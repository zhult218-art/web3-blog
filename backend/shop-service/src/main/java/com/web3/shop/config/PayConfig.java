package com.web3.shop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * PayConfig —— 支付配置类
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：从环境变量读取支付宝/微信支付凭证与回调地址，统一注入到 PayService。
 * 凭证来源（.env）：
 * <ul>
 *   <li>支付宝：ALIPAY_APPID/ALIPAY_PRIVATE_KEY/ALIPAY_PUBLIC_KEY/ALIPAY_GATEWAY/ALIPAY_NOTIFY_URL/ALIPAY_RETURN_URL</li>
 *   <li>微信：WECHAT_APPID/WECHAT_MCHID/WECHAT_APIV3_KEY/WECHAT_CERT_SERIAL_NO/WECHAT_PRIVATE_KEY/WECHAT_NOTIFY_URL</li>
 * </ul>
 * <p>
 * 未配置时各字段为空字符串，PayService 会进入"未配置"降级态——
 * 调用支付接口时返回 503 提示"支付平台未配置"，避免运行时 NPE。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "pay")
public class PayConfig {
    /** 支付宝配置 */
    private Alipay alipay = new Alipay();
    /** 微信支付配置 */
    private Wechat wechat = new Wechat();

    @Data
    public static class Alipay {
        /** 应用 ID（开放平台 → 应用 → APPID） */
        private String appId = "";
        /** 应用私钥（PKCS8 格式，去除 BEGIN/END 头尾和换行） */
        private String privateKey = "";
        /** 支付宝公钥（用于验签异步回调） */
        private String publicKey = "";
        /** 网关：沙箱 https://openapi.alipaydev.com/gateway.do / 生产 https://openapi.alipay.com/gateway.do */
        private String gateway = "https://openapi.alipay.com/gateway.do";
        /** 异步回调地址（公网可达，如 https://frp-six.com:44598/api/pay/notify/alipay） */
        private String notifyUrl = "";
        /** 同步返回地址（支付完跳回，前端页面） */
        private String returnUrl = "";
        /** 签名算法类型，固定 RSA2 */
        private String signType = "RSA2";
    }

    @Data
    public static class Wechat {
        /** 应用 ID（公众号/小程序/移动应用 AppID） */
        private String appId = "";
        /** 商户号（微信支付商户平台 → 账户中心 → 商户信息） */
        private String mchId = "";
        /** APIv3 密钥（商户平台 → 账户中心 → API 安全 → 设置 APIv3 密钥） */
        private String apiV3Key = "";
        /** 商户证书序列号（商户平台 → 账户中心 → API 安全 → 管理证书） */
        private String certSerialNo = "";
        /** 商户私钥（apiclient_key.pem 内容，去除 BEGIN/END 头尾和换行） */
        private String privateKey = "";
        /** 异步回调地址（公网可达，如 https://frp-six.com:44598/api/pay/notify/wechat） */
        private String notifyUrl = "";
    }

    /** 支付宝是否已配置齐全（appId + 私钥 + 公钥 + 网关 + notifyUrl 全部非空） */
    public boolean alipayConfigured() {
        Alipay a = alipay;
        return notBlank(a.getAppId()) && notBlank(a.getPrivateKey()) && notBlank(a.getPublicKey())
            && notBlank(a.getGateway()) && notBlank(a.getNotifyUrl());
    }

    /** 微信支付是否已配置齐全 */
    public boolean wechatConfigured() {
        Wechat w = wechat;
        return notBlank(w.getAppId()) && notBlank(w.getMchId()) && notBlank(w.getApiV3Key())
            && notBlank(w.getCertSerialNo()) && notBlank(w.getPrivateKey()) && notBlank(w.getNotifyUrl());
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
