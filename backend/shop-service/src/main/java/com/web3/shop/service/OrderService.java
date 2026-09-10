package com.web3.shop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web3.common.core.BusinessException;
import com.web3.common.core.PageResult;
import com.web3.common.core.UnauthorizedException;
import com.web3.shop.dto.OrderCreateDTO;
import com.web3.shop.entity.Order;
import com.web3.shop.entity.Product;
import com.web3.shop.mapper.OrderMapper;
import com.web3.shop.mapper.ProductMapper;
import com.web3.shop.vo.OrderVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OrderService —— 订单业务服务
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：实现订单的核心业务逻辑，包括创建订单（校验商品/库存、生成订单号、事务内扣减库存）、
 * 当前用户订单分页、全量订单分页（管理员）、订单详情（含归属校验）、
 * 用户支付（原子 PENDING→PAID，防重复支付）、用户取消（原子转换并回补库存）、
 * 管理员状态更新（状态机校验），以及实体到视图对象（OrderVO）的转换。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Service：声明为 Spring 业务组件</li>
 *   <li>@Transactional：创建订单/取消订单方法开启事务，保证订单写入与库存变更原子性</li>
 * </ul>
 */
@Service
public class OrderService {
    /** 订单 Mapper */
    private final OrderMapper orderMapper;
    /** 商品 Mapper（下单校验与扣库存） */
    private final ProductMapper productMapper;
    /** 商品业务服务（库存扣减） */
    private final ProductService productService;

    /** 订单状态机允许的目标状态白名单 */
    private static final Set<String> ALLOWED_STATUS = Set.of("PENDING", "PAID", "SHIPPED", "COMPLETED", "CANCELLED");

    public OrderService(OrderMapper orderMapper, ProductMapper productMapper, ProductService productService) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.productService = productService;
    }

    /**
     * 创建订单（同一事务内扣减库存）
     * 校验商品存在性与库存充足后，生成 PENDING 状态订单并计算订单金额（单价 x 数量）
     *
     * @param dto    订单创建参数（商品 ID、数量）
     * @param userId 当前用户 ID（取自登录令牌）
     * @return 创建成功后的订单视图对象
     */
    @Transactional
    public OrderVO createOrder(OrderCreateDTO dto, Long userId) {
        if (dto.getProductId() == null || dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new BusinessException("\u5546\u54c1ID\u6216\u6570\u91cf\u4e0d\u5408\u6cd5");
        }
        Product product = (Product)this.productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new BusinessException("\u5546\u54c1\u4e0d\u5b58\u5728");
        }
        if (product.getStock() == null || product.getStock() < dto.getQuantity()) {
            throw new BusinessException("\u5e93\u5b58\u4e0d\u8db3");
        }
        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(dto.getProductId());
        order.setQuantity(dto.getQuantity());
        order.setStatus("PENDING");
        order.setAmount(product.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity().intValue())));
        order.setOrderNo(this.generateOrderNo());
        this.orderMapper.insert(order);
        this.productService.deductStock(dto.getProductId(), dto.getQuantity());
        return this.toVO(order);
    }

    /**
     * 用户支付订单（模拟渠道回调）：归属校验 + 原子 PENDING→PAID 转换
     * 并发重复支付/已取消订单均被原子 UPDATE 拒绝；成功后记录渠道与支付时间
     *
     * @param id      订单 ID
     * @param userId  当前用户 ID
     * @param channel 支付渠道（alipay/wechat）
     * @return 支付后的订单视图对象
     */
    public OrderVO payOrder(Long id, Long userId, String channel) {
        OrderVO order = this.getDetailOwned(id, userId);
        if ("PAID".equals(order.getStatus())) {
            throw new BusinessException(409, "\u8ba2\u5355\u5df2\u652f\u4ed8\uff0c\u8bf7\u52ff\u91cd\u590d\u652f\u4ed8");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(409, "\u5f53\u524d\u72b6\u6001\u4e0d\u53ef\u652f\u4ed8\uff08" + order.getStatus() + "\uff09");
        }
        int rows = this.orderMapper.markPaid(id, channel);
        if (rows == 0) {
            throw new BusinessException(409, "\u8ba2\u5355\u72b6\u6001\u5df2\u53d8\u66f4\uff0c\u652f\u4ed8\u5931\u8d25");
        }
        return this.getDetailOwned(id, userId);
    }

    /**
     * 用户取消订单：归属校验 + 原子 PENDING→CANCELLED 转换 + 回补库存
     *
     * @param id     订单 ID
     * @param userId 当前用户 ID
     * @return 取消后的订单视图对象
     */
    @Transactional
    public OrderVO cancelOwned(Long id, Long userId) {
        OrderVO order = this.getDetailOwned(id, userId);
        if ("CANCELLED".equals(order.getStatus())) {
            return order;
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(409, "\u5f53\u524d\u72b6\u6001\u4e0d\u53ef\u53d6\u6d88\uff08" + order.getStatus() + "\uff09");
        }
        int rows = this.orderMapper.markCancelled(id);
        if (rows == 0) {
            throw new BusinessException(409, "\u8ba2\u5355\u72b6\u6001\u5df2\u53d8\u66f4\uff0c\u53d6\u6d88\u5931\u8d25");
        }
        this.productService.restoreStock(order.getProductId(), order.getQuantity());
        return this.getDetailOwned(id, userId);
    }

    /**
     * 用户删除订单（逻辑删除，仅订单所有者）
     * 归属校验通过 getDetailOwned（不存在/非本人会抛异常），随后逻辑删除
     *
     * @param id     订单 ID
     * @param userId 当前用户 ID
     */
    public void deleteOwned(Long id, Long userId) {
        this.getDetailOwned(id, userId);
        this.orderMapper.deleteById(id);
    }

    /**
     * 当前用户订单分页列表，按创建时间倒序
     *
     * @param userId 当前用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 订单视图对象的分页结果
     */
    public PageResult<OrderVO> pageByUser(Long userId, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>();
        wrapper.eq(Order::getUserId, (Object)userId);
        wrapper.orderByDesc(Order::getCreatedAt);
        Page<Order> mpPage = new Page<Order>((long)page, (long)size);
        Page<Order> result = this.orderMapper.selectPage(mpPage, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    /**
     * 全量订单分页列表（管理员），可按状态筛选，按创建时间倒序
     *
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @param status 订单状态筛选（如 PENDING/PAID），可为空
     * @return 订单视图对象的分页结果
     */
    public PageResult<OrderVO> pageAll(int page, int size, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Order::getStatus, (Object)status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        Page<Order> mpPage = new Page<Order>((long)page, (long)size);
        Page<Order> result = this.orderMapper.selectPage(mpPage, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    /**
     * 查询订单详情（无归属校验，管理员使用），不存在时抛异常
     *
     * @param id 订单 ID
     * @return 订单视图对象
     */
    public OrderVO getDetail(Long id) {
        Order order = (Order)this.orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("\u8ba2\u5355\u4e0d\u5b58\u5728");
        }
        return this.toVO(order);
    }

    /**
     * 查询订单详情并校验归属（普通用户使用）
     * 订单不属于当前用户时抛 401 无权限异常
     *
     * @param id     订单 ID
     * @param userId 当前用户 ID
     * @return 订单视图对象
     */
    public OrderVO getDetailOwned(Long id, Long userId) {
        Order order = (Order)this.orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("\u8ba2\u5355\u4e0d\u5b58\u5728");
        }
        if (!userId.equals(order.getUserId())) {
            throw new UnauthorizedException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u8ba2\u5355");
        }
        return this.toVO(order);
    }

    /**
     * 更新订单状态（管理员操作），带状态机白名单与转换校验：
     * PENDING→PAID/CANCELLED、PAID→SHIPPED/CANCELLED、SHIPPED→COMPLETED；
     * PAID 转换走原子 UPDATE 防并发；取消订单自动回补库存
     *
     * @param id     订单 ID
     * @param status 目标状态（PENDING/PAID/SHIPPED/COMPLETED/CANCELLED）
     */
    public void updateStatus(Long id, String status) {
        if (status == null || !ALLOWED_STATUS.contains(status)) {
            throw new BusinessException("\u975e\u6cd5\u8ba2\u5355\u72b6\u6001: " + status);
        }
        Order order = (Order)this.orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("\u8ba2\u5355\u4e0d\u5b58\u5728");
        }
        String from = order.getStatus();
        if (from.equals(status)) {
            return;
        }
        boolean valid = switch (from == null ? "" : from) {
            case "PENDING" -> status.equals("PAID") || status.equals("CANCELLED");
            case "PAID" -> status.equals("SHIPPED") || status.equals("CANCELLED");
            case "SHIPPED" -> status.equals("COMPLETED");
            default -> false;
        };
        if (!valid) {
            throw new BusinessException("\u4e0d\u5141\u8bb8\u7684\u72b6\u6001\u8fc1\u79fb: " + from + " -> " + status);
        }
        if (status.equals("PAID")) {
            int rows = this.orderMapper.markPaid(id, "admin");
            if (rows == 0) {
                throw new BusinessException(409, "\u8ba2\u5355\u72b6\u6001\u5df2\u53d8\u66f4\uff0c\u64cd\u4f5c\u5931\u8d25");
            }
            return;
        }
        order.setStatus(status);
        this.orderMapper.updateById(order);
        // 取消订单回补库存（PENDING/PAID 均只扣过一次库存，取消恰好回补一次）
        if (status.equals("CANCELLED")) {
            this.productService.restoreStock(order.getProductId(), order.getQuantity());
        }
    }

    /**
     * 生成订单号（内部工具方法）：时间戳（yyyyMMddHHmmss）+ 8 位随机 UUID 片段
     *
     * @return 订单号字符串
     */
    private String generateOrderNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return datePart + uidPart;
    }

    /**
     * 实体转视图对象（内部工具方法）
     *
     * @param order 订单实体
     * @return 订单视图对象
     */
    private OrderVO toVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties((Object)order, (Object)vo);
        return vo;
    }
}
