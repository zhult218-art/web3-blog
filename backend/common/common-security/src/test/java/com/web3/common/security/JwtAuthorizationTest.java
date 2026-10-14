package com.web3.common.security;

import com.web3.common.core.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthorizationTest {
    private JwtUtil jwtUtil;
    private JwtProperties properties;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret("unit-test-secret-with-at-least-32-characters");
        jwtUtil = new JwtUtil(properties);
    }

    @Test
    void regularUserCannotPassAdminAuthorization() {
        String token = jwtUtil.generateToken("reader", 7L, "USER");

        UnauthorizedException error = assertThrows(
                UnauthorizedException.class,
                () -> AuthUtils.requireAdmin("Bearer " + token, jwtUtil, properties));

        assertEquals(403, error.getCode());
    }

    @Test
    void adminTokenPassesAdminAuthorization() {
        String token = jwtUtil.generateToken("operator", 8L, "ADMIN");

        assertEquals("operator", AuthUtils.requireAdmin("Bearer " + token, jwtUtil, properties));
    }

    @Test
    void revokedRedisSessionInvalidatesOtherwiseValidJwt() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        JwtUtil sessionJwtUtil = new JwtUtil(properties, redis);
        String token = sessionJwtUtil.generateToken("reader", 7L, "USER");
        when(values.get("token:7")).thenReturn(token).thenReturn(null);

        assertTrue(sessionJwtUtil.validate(token));
        assertFalse(sessionJwtUtil.validate(token));
    }
    @Test
    void missingTokenIsRejected() {
        UnauthorizedException error = assertThrows(
                UnauthorizedException.class,
                () -> AuthUtils.requireUserId(null, jwtUtil, properties));

        assertEquals(401, error.getCode());
    }
}
