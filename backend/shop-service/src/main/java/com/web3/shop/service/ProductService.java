package com.web3.shop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web3.common.core.BusinessException;
import com.web3.common.core.PageResult;
import com.web3.shop.dto.ProductCreateDTO;
import com.web3.shop.entity.Product;
import com.web3.shop.mapper.ProductMapper;
import com.web3.shop.vo.ProductVO;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * ProductService —— 商品业务服务
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：实现商品的核心业务逻辑，包括分页查询（名称关键字/分类）、详情查询、
 * 创建（初始化销量与上架状态）、原子扣减库存（供下单使用）、删除、更新（部分字段更新），
 * 以及实体到视图对象（ProductVO）的转换。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Service：声明为 Spring 业务组件</li>
 * </ul>
 */
@Service
public class ProductService {
    /** 商品 Mapper */
    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    /**
     * 分页查询商品列表，支持名称关键字与分类筛选，按创建时间倒序
     *
     * @param page     页码（从 1 开始）
     * @param size     每页条数
     * @param keyword  商品名称关键字，可为空
     * @param category 分类，可为空
     * @return 商品视图对象的分页结果
     */
    public PageResult<ProductVO> page(int page, int size, String keyword, String category) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Product::getName, (Object)keyword);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Product::getCategory, (Object)category);
        }
        wrapper.orderByDesc(Product::getCreatedAt);
        Page<Product> mpPage = new Page<Product>((long)page, (long)size);
        Page<Product> result = this.productMapper.selectPage(mpPage, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    /**
     * 查询商品详情，不存在时抛异常
     *
     * @param id 商品 ID
     * @return 商品视图对象
     */
    public ProductVO getDetail(Long id) {
        Product product = (Product)this.productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("\u5546\u54c1\u4e0d\u5b58\u5728");
        }
        return this.toVO(product);
    }

    /**
     * 创建商品（管理员操作），初始化销量为 0、状态为 ON（上架）
     *
     * @param dto 商品创建参数
     * @return 创建成功后的商品视图对象
     */
    public ProductVO create(ProductCreateDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties((Object)dto, (Object)product);
        product.setSales(0);
        product.setStatus("ON");
        this.productMapper.insert(product);
        return this.toVO(product);
    }

    /**
     * 原子扣减商品库存（下单时调用）
     * 通过带库存条件的 UPDATE 保证并发安全；受影响行数为 0 时说明商品不存在或库存不足
     *
     * @param productId 商品 ID
     * @param quantity  扣减数量
     */
    public void deductStock(Long productId, int quantity) {
        int affected = this.productMapper.deductStockAtomic(productId, quantity);
        if (affected == 0) {
            Product product = (Product)this.productMapper.selectById(productId);
            if (product == null) {
                throw new BusinessException("\u5546\u54c1\u4e0d\u5b58\u5728");
            }
            throw new BusinessException("\u5e93\u5b58\u4e0d\u8db3");
        }
    }

    /**
     * 回补库存并回退销量（订单取消时调用）
     *
     * @param productId 商品 ID
     * @param quantity  回补数量
     */
    public void restoreStock(Long productId, int quantity) {
        this.productMapper.restoreStock(productId, quantity);
    }

    /**
     * 删除商品（逻辑删除，管理员操作），不存在时抛异常
     *
     * @param id 商品 ID
     */
    public void delete(Long id) {
        Product product = (Product)this.productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("\u5546\u54c1\u4e0d\u5b58\u5728");
        }
        this.productMapper.deleteById(id);
    }

    /**
     * 更新商品（管理员操作），仅更新传入的非空且合法字段（名称/描述/价格/库存/封面/分类）
     *
     * @param id  商品 ID
     * @param dto 商品更新参数
     * @return 更新后的商品视图对象
     */
    public ProductVO update(Long id, ProductCreateDTO dto) {
        Product product = (Product)this.productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("\u5546\u54c1\u4e0d\u5b58\u5728");
        }
        if (this.notBlank(dto.getName())) {
            product.setName(dto.getName());
        }
        if (this.notBlank(dto.getDescription())) {
            product.setDescription(dto.getDescription());
        }
        if (dto.getPrice() != null && dto.getPrice().signum() > 0) {
            product.setPrice(dto.getPrice());
        }
        if (dto.getStock() != null && dto.getStock() >= 0) {
            product.setStock(dto.getStock());
        }
        if (this.notBlank(dto.getCover())) {
            product.setCover(dto.getCover());
        }
        if (this.notBlank(dto.getCategory())) {
            product.setCategory(dto.getCategory());
        }
        this.productMapper.updateById(product);
        return this.toVO(product);
    }

    /**
     * 判断字符串是否非空白（内部工具方法）
     *
     * @param s 待判断字符串
     * @return true=非空且非空白
     */
    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * 实体转视图对象（内部工具方法）
     *
     * @param product 商品实体
     * @return 商品视图对象
     */
    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties((Object)product, (Object)vo);
        return vo;
    }
}
