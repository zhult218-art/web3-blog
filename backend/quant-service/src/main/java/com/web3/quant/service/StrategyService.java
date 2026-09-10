/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.conditions.Wrapper
 *  com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
 *  com.baomidou.mybatisplus.core.metadata.IPage
 *  com.baomidou.mybatisplus.extension.plugins.pagination.Page
 *  com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
 *  com.web3.common.core.PageResult
 *  org.springframework.stereotype.Service
 */
package com.web3.quant.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
     * 新增策略:保存实体并返回对应的 VO。
     *
     * @param strategy 待保存的策略实体(主键由 MyBatis-Plus 自动分配)
     * @return 保存成功后的策略 VO
     */
    public StrategyVO create(Strategy strategy) {
        this.save(strategy);
        return this.toVO(strategy);
    }

    /**
     * 更新策略:先校验策略是否存在,再以传入的 ID 覆盖实体主键后执行更新。
     *
     * @param id       策略 ID
     * @param strategy 更新后的策略内容
     * @return 更新后的策略 VO;策略不存在时返回 null
     */
    public StrategyVO updateStrategy(Long id, Strategy strategy) {
        Strategy exist = (Strategy)this.getById(id);
        if (exist == null) {
            return null;
        }
        strategy.setId(id);
        this.updateById(strategy);
        return this.toVO(strategy);
    }

    /**
     * 删除策略(逻辑删除,见实体上 @TableLogic 注解)。
     *
     * @param id 策略 ID
     * @throws RuntimeException 策略不存在时抛出
     */
    public void deleteStrategy(Long id) {
        Strategy exist = (Strategy)this.getById(id);
        if (exist == null) {
            throw new RuntimeException("Strategy not found");
        }
        this.removeById(id);
    }

    /**
     * 运行策略:当前实现为模拟执行,向 quant_log 表插入一条 INFO 级别的
     * "run started" 日志记录。
     *
     * @param id 策略 ID
     * @throws RuntimeException 策略不存在时抛出
     */
    public void runStrategy(Long id) {
        Strategy exist = (Strategy)this.getById(id);
        if (exist == null) {
            throw new RuntimeException("Strategy not found");
        }
        com.web3.quant.entity.QuantLog log = new QuantLog();
        log.setStrategyId(id);
        log.setMessage("Strategy [" + exist.getName() + "] run started");
        log.setLevel("INFO");
        log.setCreatedAt(LocalDateTime.now());
        this.quantLogMapper.insert(log);
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
