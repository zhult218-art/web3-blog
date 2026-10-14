package com.web3.quant.controller;

import com.web3.common.core.UnauthorizedException;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.quant.entity.Strategy;
import com.web3.quant.mapper.QuantLogMapper;
import com.web3.quant.mapper.StrategyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuantLogControllerSecurityTest {
    private JwtProperties properties;
    private JwtUtil jwtUtil;
    private StrategyMapper strategyMapper;
    private QuantLogController controller;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret("unit-test-secret-with-at-least-32-characters");
        jwtUtil = new JwtUtil(properties);
        strategyMapper = mock(StrategyMapper.class);
        controller = new QuantLogController(mock(QuantLogMapper.class), strategyMapper, jwtUtil, properties);
    }

    @Test
    void anonymousCannotReadStrategyLogs() {
        UnauthorizedException error = assertThrows(
                UnauthorizedException.class, () -> controller.logs(1L, null));
        assertEquals(401, error.getCode());
    }

    @Test
    void userCannotReadAnotherUsersStrategyLogs() {
        Strategy strategy = new Strategy();
        strategy.setId(1L);
        strategy.setUserId(99L);
        when(strategyMapper.selectById(1L)).thenReturn(strategy);
        String token = jwtUtil.generateToken("reader", 7L, "USER");

        UnauthorizedException error = assertThrows(
                UnauthorizedException.class, () -> controller.logs(1L, "Bearer " + token));

        assertEquals(403, error.getCode());
    }
}
