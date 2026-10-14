package com.web3.user.service;

import com.web3.user.config.AuthProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpServiceTest {

    @Test
    void emailOtpFailsClosedWhenSmtpIsNotConfigured() {
        OtpService service = new OtpService(null, new AuthProperties(), null);

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> service.sendEmailCode("user@example.com"));

        assertTrue(error.getMessage().contains("not configured"));
    }

    @Test
    void phoneOtpFailsClosedUntilSmsProviderIsImplemented() {
        OtpService service = new OtpService(null, new AuthProperties(), null);

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> service.sendPhoneCode("+6591234567"));

        assertTrue(error.getMessage().contains("not implemented"));
    }
}
