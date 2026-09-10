package com.web3.user.service;

import com.web3.user.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * OtpService (邮箱/短信验证码服务)
 *
 * email: 6 位数字验证码，有效期 10 分钟，同一邮箱 60 秒内只能再发一次。
 * phone: 短信验证码（smsEnabled=true 走短信网关，false 为开发模式直接返回 devCode）。
 * mailEnabled=false 时邮箱验证码走开发模式：验证码仍生成并校验，但不发 SMTP，
 * 直接写进响应体 data.devCode 方便调试，控制台打印。
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int CODE_LENGTH = 6;
    private static final int CODE_TTL_MINUTES = 10;
    private static final int RESEND_INTERVAL_SECONDS = 60;

    private static final String EMAIL_CODE_PREFIX = "otp:email:";
    private static final String EMAIL_RATE_PREFIX = "otpsent:email:";
    private static final String PHONE_CODE_PREFIX = "otp:phone:";
    private static final String PHONE_RATE_PREFIX = "otpsent:phone:";

    private final StringRedisTemplate redis;
    private final AuthProperties props;
    private final JavaMailSender mailSender;

    public OtpService(StringRedisTemplate redis, AuthProperties props,
                      @org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender) {
        this.redis = redis;
        this.props = props;
        this.mailSender = mailSender;
    }

    // ───────── 邮箱验证码 ─────────

    /** 发送邮箱验证码；devCode 仅在 mailEnabled=false 时返回，供前端显示调试 */
    public String sendEmailCode(String email) {
        rateLimit(email, EMAIL_RATE_PREFIX);
        String code = randomCode();
        String key = EMAIL_CODE_PREFIX + email.toLowerCase();
        redis.opsForValue().set(key, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("[OtpService] email={} code={}", email, code);
        if (!props.isMailEnabled()) {
            log.warn("[OtpService] mailDisabled, returning devCode only");
            return code;
        }
        sendMail(email, code);
        return null;
    }

    public boolean verifyEmailCode(String email, String code) {
        if (email == null || code == null) return false;
        String key = EMAIL_CODE_PREFIX + email.toLowerCase();
        String expected = redis.opsForValue().get(key);
        if (expected == null) return false;
        redis.delete(key);
        return expected.equals(code.trim());
    }

    // ───────── 手机短信验证码 ─────────

    /** 发送短信验证码；smsEnabled=false 返回 devCode */
    public String sendPhoneCode(String phone) {
        rateLimit(phone, PHONE_RATE_PREFIX);
        String code = randomCode();
        String key = PHONE_CODE_PREFIX + phone;
        redis.opsForValue().set(key, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("[OtpService] phone={} code={}", phone, code);
        if (!props.isSmsEnabled()) {
            log.warn("[OtpService] smsDisabled, returning devCode only");
            return code;
        }
        // TODO: 对接真实短信网关（阿里云/腾讯云），当前返回 null
        return null;
    }

    public boolean verifyPhoneCode(String phone, String code) {
        if (phone == null || code == null) return false;
        String key = PHONE_CODE_PREFIX + phone;
        String expected = redis.opsForValue().get(key);
        if (expected == null) return false;
        redis.delete(key);
        return expected.equals(code.trim());
    }

    // ───────── 内部方法 ─────────

    /** 60 秒发送频率限制（过快抛 RuntimeException） */
    private void rateLimit(String target, String prefix) {
        String key = prefix + target.toLowerCase();
        if (Boolean.TRUE.equals(redis.hasKey(key))) {
            throw new RuntimeException("发送过于频繁，请60秒后重试");
        }
        redis.opsForValue().set(key, "1", RESEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private String randomCode() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }

    private void sendMail(String to, String code) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom("noreply@web3blog.com");
            helper.setTo(to);
            helper.setSubject("Web3-Blog 登录验证码");
            helper.setText("您的登录验证码为：" + code + "（10 分钟内有效），请勿泄露给他人。", false);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("[OtpService] sendMail failed: {}", e.getMessage(), e);
            throw new RuntimeException("验证码发送失败，请稍后重试");
        }
    }
}
