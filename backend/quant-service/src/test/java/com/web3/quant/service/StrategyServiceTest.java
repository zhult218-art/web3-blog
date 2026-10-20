package com.web3.quant.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web3.common.core.UnauthorizedException;
import com.web3.quant.entity.QuantLog;
import com.web3.quant.entity.Strategy;
import com.web3.quant.mapper.QuantLogMapper;
import com.web3.quant.mapper.StrategyMapper;
import com.web3.quant.vo.StrategyVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * StrategyService 单元测试（2026-10-20 补全）。
 *
 * <p>重点覆盖 2026-09-17 修复的横向越权问题：update / delete / run 三个写操作
 * 在只校验登录态时，任意登录用户都能改、删、跑别人的策略。修复后统一走
 * {@code ensureOwnership}，本测试即为该逻辑的回归防线。</p>
 *
 * <p>覆盖矩阵：
 * <ul>
 *   <li>本人策略 → 允许</li>
 *   <li>他人策略 → 403 拒绝，且不得落库</li>
 *   <li>公共策略（user_id 为 null）→ 非管理员 403，管理员放行</li>
 *   <li>管理员 → 可操作任意策略</li>
 *   <li>策略不存在 → update 返回 null / delete、run 抛异常</li>
 *   <li>update 不得篡改归属字段</li>
 *   <li>create 必须写入归属用户</li>
 * </ul>
 */
class StrategyServiceTest {

    /** 策略表 Mapper，同时作为 ServiceImpl.baseMapper 注入 */
    @Mock
    private StrategyMapper strategyMapper;

    /** 执行日志 Mapper，构造器注入 */
    @Mock
    private QuantLogMapper quantLogMapper;

    @InjectMocks
    private StrategyService strategyService;

    /** 策略归属人 */
    private static final Long OWNER_ID = 11L;
    /** 其他普通用户 */
    private static final Long OTHER_ID = 22L;
    /** 管理员 */
    private static final Long ADMIN_ID = 99L;
    /** 被测策略 ID */
    private static final Long STRATEGY_ID = 100L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // ServiceImpl.baseMapper 为 protected 字段，需反射注入
        ReflectionTestUtils.setField(strategyService, "baseMapper", strategyMapper);
    }

    /** 构造一条归属 OWNER_ID 的策略 */
    private Strategy ownedStrategy() {
        Strategy s = new Strategy();
        s.setId(STRATEGY_ID);
        s.setUserId(OWNER_ID);
        s.setName("双均线");
        s.setStatus("draft");
        return s;
    }

    /** 构造一条公共策略（无归属） */
    private Strategy publicStrategy() {
        Strategy s = new Strategy();
        s.setId(STRATEGY_ID);
        s.setUserId(null);
        s.setName("平台公共策略");
        return s;
    }

    // ───────────── update ─────────────

    @Test
    void update_owner_shouldSucceed() {
        when(strategyMapper.selectById(STRATEGY_ID)).thenReturn(ownedStrategy());
        when(strategyMapper.updateById(any(Strategy.class))).thenReturn(1);

        Strategy patch = new Strategy();
        patch.setName("改名后");

        StrategyVO vo = strategyService.updateStrategy(STRATEGY_ID, patch, OWNER_ID, false);

        assertNotNull(vo);
        assertEquals("改名后", vo.getName());
        verify(strategyMapper).updateById(any(Strategy.class));
    }

    @Test
    void update_otherUserStrategy_shouldThrow403() {
        when(strategyMapper.selectById(STRATEGY_ID)).thenReturn(ownedStrategy());

        Strategy patch = new Strategy();
        patch.setName("劫持");

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> strategyService.updateStrategy(STRATEGY_ID, patch, OTHER_ID, false));

        assertEquals(403, ex.getCode());
        // 越权时绝不允许落库
        verify(strategyMapper, never()).updateById(any(Strategy.class));
    }

    @Test
    void update_publicStrategy_byNonAdmin_shouldThrow403() {
        when(strategyMapper.selectById(STRATEGY_ID)).thenReturn(publicStrategy());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> strategyService.updateStrategy(STRATEGY_ID, new Strategy(), OTHER_ID, false));

        assertEquals(403, ex.getCode());
        verify(strategyMapper, never()).updateById(any(Strategy.class));
    }

    @Test
    void update_publicStrategy_byAdmin_shouldSucceed() {
        when(strategyMapper.selectById(STRATEGY_ID)).thenReturn(publicStrategy());
        when(strategyMapper.updateById(any(Strategy.class))).thenReturn(1);

        StrategyVO vo = strategyService.updateStrategy(STRATEGY_ID, new Strategy(), ADMIN_ID, true);

        assertNotNull(vo);
        verify(strategyMapper).updateById(any(Strategy.class));
    }

    @Test
    void update_shouldNotAllowChangingOwner() {
        when(strategyMapper.selectById(STRATEGY_ID)).thenReturn(ownedStrategy());
        when(strategyMapper.updateById(any(Strategy.class))).thenReturn(1);

        // 恶意请求体试图把归属改成 OTHER_ID
        Strategy patch = new Strategy();
        patch.setUserId(OTHER_ID);
        patch.setName("偷走");

        strategyService.updateStrategy(STRATEGY_ID, patch, OWNER_ID, false);

        ArgumentCaptor<Strategy> captor = ArgumentCaptor.forClass(Strategy.class);
        verify(strategyMapper).updateById(captor.capture());
        assertEquals(OWNER_ID, captor.getValue().getUserId(),
                "update 必须保留原归属，不能通过请求体篡改 user_id");
        assertEquals(STRATEGY_ID, captor.getValue().getId());
    }

    @Test
    void update_notExist_shouldReturnNull() {
        when(strategyMapper.selectById(STRATEGY_ID)).thenReturn(null);

        assertNull(strategyService.updateStrategy(STRATEGY_ID, new Strategy(), OWNER_ID, false));
        verify(strategyMapper, never()).updateById(any(Strategy.class));
    }

    // ───────────── delete ─────────────

    @Test
    void delete_owner_shouldSucceed() {
        when(strategyMapper.selectById(STRATEGY_ID)).thenReturn(ownedStrategy());
        when(strategyMapper.deleteById(STRATEGY_ID)).thenReturn(1);

        strategyService.deleteStrategy(STRATEGY_ID, OWNER_ID, false);

        verify(strategyMapper).deleteById(STRATEGY_ID);
    }

    @Test
    void delete_otherUserStrategy_shouldThrow403AndKeepData() {
        when(strategyMapper.selectById(STRATEGY_ID)).thenReturn(ownedStrategy());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> strategyService.deleteStrategy(STRATEGY_ID, OTHER_ID, false));

        assertEquals(403, ex.getCode());
        verify(strategyMapper, never()).deleteById(anyLong());
    }

    @Test
    void delete_byAdmin_canDeleteOthers() {
        when(strategyMapper.selectById(STRATEGY_ID)).thenReturn(ownedStrategy());
        when(strategyMapper.deleteById(STRATEGY_ID)).thenReturn(1);

        strategyService.deleteStrategy(STRATEGY_ID, ADMIN_ID, true);

        verify(strategyMapper).deleteById(STRATEGY_ID);
    }

    @Test
    void delete_notExist_shouldThrow() {
        when(strategyMapper.selectById(STRATEGY_ID)).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> strategyService.deleteStrategy(STRATEGY_ID, OWNER_ID, false));
        verify(strategyMapper, never()).deleteById(anyLong());
    }

    // ───────────── run ─────────────

    @Test
    void run_owner_shouldWriteLog() {
        when(strategyMapper.selectById(STRATEGY_ID)).thenReturn(ownedStrategy());
        when(quantLogMapper.insert(any(QuantLog.class))).thenReturn(1);

        strategyService.runStrategy(STRATEGY_ID, OWNER_ID, false);

        ArgumentCaptor<QuantLog> captor = ArgumentCaptor.forClass(QuantLog.class);
        verify(quantLogMapper).insert(captor.capture());
        QuantLog log = captor.getValue();
        assertEquals(STRATEGY_ID, log.getStrategyId());
        assertEquals("INFO", log.getLevel());
        assertTrue(log.getMessage().contains("双均线"), "日志应带策略名，便于排查");
    }

    @Test
    void run_otherUserStrategy_shouldThrow403AndNotRun() {
        when(strategyMapper.selectById(STRATEGY_ID)).thenReturn(ownedStrategy());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> strategyService.runStrategy(STRATEGY_ID, OTHER_ID, false));

        assertEquals(403, ex.getCode());
        verify(quantLogMapper, never()).insert(any(QuantLog.class));
    }

    @Test
    void run_notExist_shouldThrow() {
        when(strategyMapper.selectById(STRATEGY_ID)).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> strategyService.runStrategy(STRATEGY_ID, OWNER_ID, false));
        verify(quantLogMapper, never()).insert(any(QuantLog.class));
    }

    // ───────────── create ─────────────

    @Test
    void create_shouldBindOwner() {
        when(strategyMapper.insert(any(Strategy.class))).thenReturn(1);

        Strategy input = new Strategy();
        input.setName("新策略");

        StrategyVO vo = strategyService.create(input, OWNER_ID);

        assertEquals(OWNER_ID, vo.getUserId(), "新建策略必须写入当前登录用户为归属");
    }

    // ───────────── page ─────────────

    @Test
    void page_shouldDelegateToMapper() {
        when(strategyMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<>());

        // 直接调用：若抛异常本用例即失败，无需 assertDoesNotThrow（避免与 ThrowingSupplier 重载歧义）
        strategyService.page(1, 10);

        verify(strategyMapper).selectPage(any(Page.class), any(Wrapper.class));
    }
}
