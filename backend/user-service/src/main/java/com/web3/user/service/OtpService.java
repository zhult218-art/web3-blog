package com.web3.user.service;

import com.web3.user.config.AuthProperties;
import com.web3.common.util.LogMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * OtpService (邮箱/短信验证码服务)
 *
 * email: 6 位数字验证码，有效期 10 分钟，同一邮箱 60 秒内只能再发一次。
 * 邮箱验证码只在真实 SMTP 配置可用时发送；短信验证码在接入供应商 SDK 前明确禁用。
 * 验证码不会写入日志或响应体。
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int CODE_LENGTH = 6;
    private static final int CODE_TTL_MINUTES = 10;
    private static final int RESEND_INTERVAL_SECONDS = 60;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DefaultRedisScript<Long> CONSUME_CODE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);

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

    /**
     * 发送邮箱验证码。
     *
     * <p>2026-10-10 安全修订：dev 模式（mailEnabled=false）验证码不再回传 HTTP 响应体，
     * 不再写入日志或响应体。原先 dev 模式会把 devCode 写入
     * 响应体，配合 emailLogin 的未知邮箱自动注册，构成「对任意邮箱发码→从响应取码→登录即接管」
     * 的账户接管链路；现 dev 模式与生产模式统一返回 null，由真实 SMTP 承担交付。</p>
     *
     * <p>生产部署：设置 {@code MAIL_ENABLED=true} 并配置 {@code MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD}，
     * 验证码走真实 SMTP。</p>
     *
     * @param email 邮箱
     * @return 始终返回 null（验证码不再回传响应体；dev 模式请查服务端日志）
     */
    public String sendEmailCode(String email) {
        if (!props.isMailEnabled() || mailSender == null) {
            throw new IllegalStateException("Email OTP delivery is not configured");
        }
        rateLimit(email, EMAIL_RATE_PREFIX);
        String code = randomCode();
        String key = EMAIL_CODE_PREFIX + email.toLowerCase(java.util.Locale.ROOT);
        sendMail(email, code);
        redis.opsForValue().set(key, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("[OtpService] email code sent. email={}", LogMasker.maskEmail(email));
        return null;
    }

    public boolean verifyEmailCode(String email, String code) {
        if (email == null || code == null) return false;
        String key = EMAIL_CODE_PREFIX + email.toLowerCase(java.util.Locale.ROOT);
        return consumeCode(key, code.trim());
    }

    // ───────── 手机短信验证码 ─────────

    /**
     * 发送短信验证码。
     *
     * <p>2026-10-10 安全修订：dev 模式（smsEnabled=false）与网关未接入的 fallback 路径
     * 未实现的短信通道直接失败，避免调用方误以为验证码已下发。</p>
     *
     * <p>真实 SMS 网关接入：设置 {@code SMS_ENABLED=true} + {@code SMS_PROVIDER=aliyun|tencent}
     * + {@code SMS_KEY/SMS_SECRET/SMS_SIGN_NAME/SMS_TEMPLATE_CODE}，然后在
     * {@link #sendSmsByGateway} 填具体实现（阿里云 SDK / 腾讯云 SDK / HTTP 调用均可）。</p>
     *
     * @param phone 手机号
     * @return 始终返回 null（验证码不再回传响应体；dev/fallback 模式请查服务端日志）
     */
    public String sendPhoneCode(String phone) {
        throw new IllegalStateException("SMS OTP delivery is not implemented; use email verification");
    }

    public boolean verifyPhoneCode(String phone, String code) {
        if (phone == null || code == null) return false;
        String key = PHONE_CODE_PREFIX + phone;
        return consumeCode(key, code.trim());
    }

    // ───────── 内部方法 ─────────

    /** 60 秒发送频率限制（过快抛 RuntimeException） */
    private void rateLimit(String target, String prefix) {
        String key = prefix + target.toLowerCase(java.util.Locale.ROOT);
        Boolean acquired = redis.opsForValue().setIfAbsent(
                key, "1", RESEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new RuntimeException("发送过于频繁，请60秒后重试");
        }
    }

    private boolean consumeCode(String key, String code) {
        Long consumed = redis.execute(CONSUME_CODE_SCRIPT, Collections.singletonList(key), code);
        return Long.valueOf(1L).equals(consumed);
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) sb.append(SECURE_RANDOM.nextInt(10));
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
