package com.web3.quant.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web3.common.core.UnauthorizedException;
import com.web3.common.core.PageResult;
import com.web3.quant.entity.QuantLog;
import com.web3.quant.entity.Strategy;
import com.web3.quant.mapper.QuantLogMapper;
import com.web3.quant.mapper.StrategyMapper;
import com.web3.quant.vo.StrategyVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 策略业务服务(quant-service 模块)。
 *
 * <p>继承 MyBatis-Plus 的 {@link com.baomidou.mybatisplus.extension.service.impl.ServiceImpl},
 * 提供量化策略的分页查询、详情、创建、更新、删除与"运行"逻辑
 * (运行即向 quant_log 表写入一条执行日志)。{@code @Service} 注解使其
 * 被 Spring 容器托管,供 StrategyController 注入使用。</p>
 *
 * <p><b>归属校验（2026-09-17 修订）</b>：update/delete/run 等写操作除了校验登录态外，
 * 还会校验当前用户是否为策略归属人（{@code strategy.user_id}）。ADMIN 角色可操作任意
 * 策略（含公共策略）；公共策略（user_id 为 NULL）对非 ADMIN 用户一律拒绝；
 * 他人策略（user_id 与当前用户不一致）同样拒绝。这修复了
 * 历史上"任意登录用户可改/删/跑他人策略"的横向越权问题。</p>
 */
@Service
public class StrategyService
extends ServiceImpl<StrategyMapper, Strategy> {
    private final QuantLogMapper quantLogMapper;

    public StrategyService(QuantLogMapper quantLogMapper) {
        this.quantLogMapper = quantLogMapper;
    }
    /**
     * 分页查询策略列表,按创建时间倒序。
     *
     * @param pageNum  页码,从 1 开始
     * @param pageSize 每页条数
     * @return 包含当前页策略 VO 记录与总数等分页信息的 PageResult
     */
    public PageResult<StrategyVO> page(int pageNum, int pageSize) {
        Page<Strategy> p = new Page<Strategy>((long)pageNum, (long)pageSize);
        IPage<Strategy> result = this.page((IPage<Strategy>)p, new LambdaQueryWrapper<Strategy>().orderByDesc(Strategy::getCreatedAt));
        List<StrategyVO> records = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    /**
     * 按 ID 查询策略详情并转换为 VO。
     *
     * @param id 策略 ID
     * @return 策略 VO;不存在时返回 null
     */
    public StrategyVO findById(Long id) {
        Strategy entity = (Strategy)this.getById(id);
        if (entity == null) {
            return null;
        }
        return this.toVO(entity);
    }

    /**
     * 新增策略:保存实体（含归属用户 ID）并返回对应的 VO。
     *
     * @param strategy 待保存的策略实体(主键由 MyBatis-Plus 自动分配)
     * @param userId   当前登录用户 ID，写入 strategy.user_id 作为归属
     * @return 保存成功后的策略 VO
     */
    public StrategyVO create(Strategy strategy, Long userId) {
        strategy.setUserId(userId);
        this.save(strategy);
        return this.toVO(strategy);
    }

    /**
     * 更新策略:先校验策略是否存在,再以传入的 ID 覆盖实体主键后执行更新。
     * 同时校验策略归属：非归属用户禁止更新（公共策略 user_id=null 仅允许 ADMIN）。
     *
     * @param id       策略 ID
     * @param strategy 更新后的策略内容
     * @param userId   当前登录用户 ID
     * @return 更新后的策略 VO;策略不存在时返回 null
     * @throws UnauthorizedException 当前用户无权操作该策略时抛出
     */
    public StrategyVO updateStrategy(Long id, Strategy strategy, Long userId, boolean isAdmin) {
        Strategy exist = (Strategy)this.getById(id);
        if (exist == null) {
            return null;
        }
        ensureOwnership(exist, userId, isAdmin);
        strategy.setId(id);
        // 不允许通过 update 修改归属
        strategy.setUserId(exist.getUserId());
        this.updateById(strategy);
        return this.toVO(strategy);
    }

    /**
     * 删除策略(逻辑删除,见实体上 @TableLogic 注解)。
     * 同时校验策略归属：非归属用户禁止删除（公共策略 user_id=null 仅允许 ADMIN）。
     *
     * @param id     策略 ID
     * @param userId 当前登录用户 ID
     * @throws RuntimeException 策略不存在时抛出
     * @throws UnauthorizedException 当前用户无权操作该策略时抛出
     */
    public void deleteStrategy(Long id, Long userId, boolean isAdmin) {
        Strategy exist = (Strategy)this.getById(id);
        if (exist == null) {
            throw new RuntimeException("Strategy not found");
        }
        ensureOwnership(exist, userId, isAdmin);
        this.removeById(id);
    }

    /**
     * 运行策略:当前实现为模拟执行,向 quant_log 表插入一条 INFO 级别的
     * "run started" 日志记录。同时校验策略归属。
     *
     * @param id     策略 ID
     * @param userId 当前登录用户 ID
     * @throws RuntimeException 策略不存在时抛出
     * @throws UnauthorizedException 当前用户无权操作该策略时抛出
     */
    public void runStrategy(Long id, Long userId, boolean isAdmin) {
        Strategy exist = (Strategy)this.getById(id);
        if (exist == null) {
            throw new RuntimeException("Strategy not found");
        }
        ensureOwnership(exist, userId, isAdmin);
        com.web3.quant.entity.QuantLog log = new QuantLog();
        log.setStrategyId(id);
        log.setMessage("Strategy [" + exist.getName() + "] run started");
        log.setLevel("INFO");
        log.setCreatedAt(LocalDateTime.now());
        this.quantLogMapper.insert(log);
    }

    /**
     * 校验当前用户是否有权操作该策略。
     *
     * <p>规则：</p>
     * <ul>
     *   <li>ADMIN 角色：可操作任意策略（含公共策略），直接放行；</li>
     *   <li>strategy.user_id 为 NULL（公共策略）：非 ADMIN 一律拒绝（403 公共策略仅管理员可操作）；</li>
     *   <li>strategy.user_id 不为 NULL：必须与当前 userId 一致，否则 403 无权操作他人策略。</li>
     * </ul>
     *
     * @param strategy 策略实体
     * @param userId   当前用户 ID
     * @param isAdmin  当前用户是否为 ADMIN 角色
     * @throws UnauthorizedException 无权操作时抛出
     */
    private void ensureOwnership(Strategy strategy, Long userId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        Long owner = strategy.getUserId();
        if (owner == null) {
            throw new UnauthorizedException(403, "公共策略仅管理员可操作");
        }
        if (!owner.equals(userId)) {
            throw new UnauthorizedException(403, "无权操作他人策略");
        }
    }

    /**
     * 将策略实体转换为展示层 VO(字段同名拷贝)。
     *
     * @param s 策略实体
     * @return 策略 VO
     */
    private StrategyVO toVO(Strategy s) {
        StrategyVO vo = new StrategyVO();
        vo.setId(s.getId());
        vo.setUserId(s.getUserId());
        vo.setName(s.getName());
        vo.setDescription(s.getDescription());
        vo.setStatus(s.getStatus());
        vo.setCode(s.getCode());
        vo.setReturns(s.getReturns());
        vo.setRiskLevel(s.getRiskLevel());
        vo.setTags(s.getTags());
        vo.setBacktestData(s.getBacktestData());
        vo.setCreatedAt(s.getCreatedAt());
        return vo;
    }
}
