package com.web3.user.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CaptchaService (图形验证码服务)
 *
 * 生成 AWT 图形验证码 PNG，存入 Redis captcha:{id} 有效期 5 分钟，过期自动清除。
 * GET /user/captcha 返回 {captchaId, image}（data:image/png;base64,...），
 * 登录/发验证码时校验 captchaId + captchaCode 是否匹配。
 */
@Service
public class CaptchaService {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LEN = 4;
    private static final int TTL_SECONDS = 300;
    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final String REDIS_PREFIX = "captcha:";

    private final StringRedisTemplate redis;

    public CaptchaService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 生成验证码并返回 id + Base64 PNG
     */
    public Map<String, String> generate() {
        String code = randomCode();
        String id = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(REDIS_PREFIX + id, code.toLowerCase(), TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);

        String base64 = renderImage(code);
        Map<String, String> result = new HashMap<>();
        result.put("captchaId", id);
        result.put("image", base64);
        return result;
    }

    /**
     * 校验验证码（一次性使用，通过后立即删除）
     */
    public boolean verify(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) return false;
        String key = REDIS_PREFIX + captchaId;
        String expected = redis.opsForValue().get(key);
        if (expected == null) return false;
        redis.delete(key);
        return expected.equalsIgnoreCase(captchaCode.trim());
    }

    private String randomCode() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(CHARSET.charAt(r.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

    private String renderImage(String code) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // 背景
        g.setColor(new Color(20, 25, 40));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        ThreadLocalRandom r = ThreadLocalRandom.current();

        // 干扰线
        g.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(0, 180 + r.nextInt(70), 220 + r.nextInt(35), 90));
            g.drawLine(r.nextInt(WIDTH), r.nextInt(HEIGHT), r.nextInt(WIDTH), r.nextInt(HEIGHT));
        }

        // 字符
        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(0, 220 + r.nextInt(36), 255));
            double theta = Math.toRadians(r.nextInt(20) - 10);
            AffineTransform orig = g.getTransform();
            g.rotate(theta, 28 + i * 22, 30);
            g.drawString(String.valueOf(code.charAt(i)), 20 + i * 22, 30);
            g.setTransform(orig);
        }

        // 干扰点
        for (int i = 0; i < 80; i++) {
            g.setColor(new Color(0, 200 + r.nextInt(56), 255, r.nextInt(120)));
            g.fillOval(r.nextInt(WIDTH), r.nextInt(HEIGHT), 2, 2);
        }

        g.dispose();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", bos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("captcha render failed", e);
        }
    }
}
