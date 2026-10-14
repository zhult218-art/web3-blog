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
     * 只写入服务端日志（明文，便于本地开发从控制台读取）。原先 dev 模式会把 devCode 写入
     * 响应体，配合 emailLogin 的未知邮箱自动注册，构成「对任意邮箱发码→从响应取码→登录即接管」
     * 的账户接管链路；现 dev 模式与生产模式统一返回 null，由日志承担开发可见性。</p>
     *
     * <p>生产部署：设置 {@code MAIL_ENABLED=true} 并配置 {@code MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD}，
     * 验证码走真实 SMTP。</p>
     *
     * @param email 邮箱
     * @return 始终返回 null（验证码不再回传响应体；dev 模式请查服务端日志）
     */
    public String sendEmailCode(String email) {
        rateLimit(email, EMAIL_RATE_PREFIX);
        String code = randomCode();
        String key = EMAIL_CODE_PREFIX + email.toLowerCase();
        redis.opsForValue().set(key, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        if (!props.isMailEnabled()) {
            // dev 模式：验证码只写日志，不再回传响应体（防账户接管）
            log.warn("[OtpService] mailDisabled, code logged for local dev. email={} code={}", LogMasker.maskEmail(email), code);
            return null;
        }
        log.info("[OtpService] email code sent. email={}", LogMasker.maskEmail(email));
        sendMail(email, code);
        return null;
    }

    public boolean verifyEmailCode(String email, String code) {
        if (email == null || code == null) return false;
        String key = EMAIL_CODE_PREFIX + email.toLowerCase();
        return consumeCode(key, code.trim());
    }

    // ───────── 手机短信验证码 ─────────

    /**
     * 发送短信验证码。
     *
     * <p>2026-10-10 安全修订：dev 模式（smsEnabled=false）与网关未接入的 fallback 路径
     * 均不再回传 devCode，只写服务端日志（明文）。原先 dev/fallback 回传 devCode 配合
     * phoneLogin 自动注册构成账户接管链路，现统一返回 null。</p>
     *
     * <p>真实 SMS 网关接入：设置 {@code SMS_ENABLED=true} + {@code SMS_PROVIDER=aliyun|tencent}
     * + {@code SMS_KEY/SMS_SECRET/SMS_SIGN_NAME/SMS_TEMPLATE_CODE}，然后在
     * {@link #sendSmsByGateway} 填具体实现（阿里云 SDK / 腾讯云 SDK / HTTP 调用均可）。</p>
     *
     * @param phone 手机号
     * @return 始终返回 null（验证码不再回传响应体；dev/fallback 模式请查服务端日志）
     */
    public String sendPhoneCode(String phone) {
        rateLimit(phone, PHONE_RATE_PREFIX);
        String code = randomCode();
        String key = PHONE_CODE_PREFIX + phone;
        redis.opsForValue().set(key, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        if (!props.isSmsEnabled()) {
            // dev 模式：验证码只写日志，不再回传响应体（防账户接管）
            log.warn("[OtpService] smsDisabled, code logged for local dev. phone={} code={}", LogMasker.maskPhone(phone), code);
            return null;
        }
        // 生产模式：尝试调网关；网关未接入时回退日志模式（不回传 devCode，提示用户改用邮箱）
        boolean sent = sendSmsByGateway(phone, code);
        if (sent) {
            log.info("[OtpService] sms code sent. phone={}", LogMasker.maskPhone(phone));
            return null;
        }
        log.warn("[OtpService] sms gateway unavailable, code logged as fallback. phone={} code={}", LogMasker.maskPhone(phone), code);
        return null;
    }

    /**
     * 真实短信网关下发（生产模式调用）。
     *
     * <p>2026-09-30 修订：项目改为「仅邮件 OTP」方案——短信通道未接入第三方网关，
     * 此方法不再抛 RuntimeException 阻塞流程，而是记录 warn 日志并返回 false，
     * 由 {@link #sendPhoneCode} 自动回退到 dev 模式（回传 devCode），
     * 让 Controller 提示用户改用邮箱验证码完成登录/绑定。</p>
     *
     * <p>如未来需要接入真实短信：在 {@code application.yml} 配置
     * {@code SMS_PROVIDER=aliyun|tencent} + 凭据后，在此方法内按 provider 分发到
     * 阿里云 dysmsapi20170525 SDK 或腾讯云 tencentcloud-sdk-java-sms 即可，
     * 返回 true 表示已真实下发。</p>
     *
     * @param phone 手机号
     * @param code 验证码
     * @return true 表示已真实下发；false 表示网关未接入，调用方应回退 dev 模式
     */
    private boolean sendSmsByGateway(String phone, String code) {
        String provider = System.getenv().getOrDefault("SMS_PROVIDER", "");
        if (provider.isBlank()) {
            log.warn("[OtpService] 短信网关未配置：请改用邮箱验证码。phone={}", LogMasker.maskPhone(phone));
            return false;
        }
        // 短信 SDK 未接入：保留接入位（aliyun/tencent），当前一律回退
        log.warn("[OtpService] 短信网关 {} 未接入 SDK：请改用邮箱验证码。phone={}", provider, LogMasker.maskPhone(phone));
        return false;
    }

    public boolean verifyPhoneCode(String phone, String code) {
        if (phone == null || code == null) return false;
        String key = PHONE_CODE_PREFIX + phone;
        return consumeCode(key, code.trim());
    }

    // ───────── 内部方法 ─────────

    /** 60 秒发送频率限制（过快抛 RuntimeException） */
    private void rateLimit(String target, String prefix) {
        String key = prefix + target.toLowerCase();
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
