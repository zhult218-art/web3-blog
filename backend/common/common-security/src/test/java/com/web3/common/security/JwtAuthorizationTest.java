package com.web3.common.security;

import com.web3.common.core.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void missingTokenIsRejected() {
        UnauthorizedException error = assertThrows(
                UnauthorizedException.class,
                () -> AuthUtils.requireUserId(null, jwtUtil, properties));

        assertEquals(401, error.getCode());
    }
}
