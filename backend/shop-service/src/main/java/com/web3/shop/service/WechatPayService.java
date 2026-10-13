package com.web3.shop.service;

import com.web3.common.core.BusinessException;
import com.web3.shop.config.PayConfig;
import com.web3.shop.entity.Order;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * WechatPayService —— 微信支付服务
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：封装微信支付官方新一代 SDK（wechatpay-java）调用，提供 Native 扫码下单与异步回调验签解密。
 * <p>
 * 流程：
 * <ol>
 *   <li>前端 POST /pay/wechat → 本服务调 createNativePay 下单 → 返回 code_url（weixin://wxpay/bizpayurl?pr=xxx）</li>
 *   <li>前端用 code_url 生成二维码展示 → 用户微信扫码支付</li>
 *   <li>微信支付完成后 POST 回调 notify_url（JSON body + Wechatpay-Signature 等头） → 本服务 parseNotify 验签解密 → 更新订单为 PAID</li>
 *   <li>返回 200 + {"code":"SUCCESS"}，微信停止重试；返回非 200 微信按 15s/15s/30s/...重试</li>
 * </ol>
 * <p>
 * 凭证未配置时 config 为 null，createNativePay 抛 503；parseNotify 抛 400。
 */
@Service
public class WechatPayService {
    private final PayConfig payConfig;
    private volatile Config config;
    private volatile NativePayService nativePayService;
    private volatile NotificationParser notificationParser;

    public WechatPayService(PayConfig payConfig) {
        this.payConfig = payConfig;
    }

    @PostConstruct
    public void init() {
        if (payConfig.wechatConfigured()) {
            PayConfig.Wechat w = payConfig.getWechat();
            // RSAAutoCertificateConfig 会自动下载微信支付平台证书（基于 APIv3 密钥）
            config = new RSAAutoCertificateConfig.Builder()
                .merchantId(w.getMchId())
                .privateKey(w.getPrivateKey())
                .merchantSerialNumber(w.getCertSerialNo())
                .apiV3Key(w.getApiV3Key())
                .build();
            nativePayService = new NativePayService.Builder().config(config).build();
            notificationParser = new NotificationParser(
                (com.wechat.pay.java.core.notification.NotificationConfig) config
            );
        }
    }

    /**
     * 创建 Native 扫码支付订单，返回 code_url
     * <p>
     * 前端用 code_url 生成二维码（推荐用 qrcode.js / vue-qrcode 等组件）。
     *
     * @param order 待支付订单（必须处于 PENDING 状态，amount 为元）
     * @return 微信支付 code_url（weixin://wxpay/bizpayurl?pr=xxx）
     */
    public String createNativePay(Order order) {
        if (nativePayService == null) {
            throw new BusinessException(503, "微信支付未配置：请在 .env 填入 WECHAT_APPID/WECHAT_MCHID/WECHAT_APIV3_KEY/WECHAT_CERT_SERIAL_NO/WECHAT_PRIVATE_KEY/WECHAT_NOTIFY_URL");
        }
        try {
            PayConfig.Wechat w = payConfig.getWechat();
            PrepayRequest request = new PrepayRequest();
            request.setAppid(w.getAppId());
            request.setMchid(w.getMchId());
            request.setDescription("Web3 Portal 订单 " + order.getOrderNo());
            request.setOutTradeNo(String.valueOf(order.getId()));
            request.setNotifyUrl(w.getNotifyUrl());
            Amount amount = new Amount();
            // 微信支付金额单位为分，元 × 100 四舍五入
            int totalCents = order.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
            amount.setTotal(totalCents);
            request.setAmount(amount);
            PrepayResponse response = nativePayService.prepay(request);
            return response.getCodeUrl();
        } catch (Exception e) {
            throw new BusinessException(500, "微信下单异常: " + e.getMessage());
        }
    }

    /**
     * 解析并验证异步回调
     * <p>
     * 微信回调是 POST JSON body + Wechatpay-Signature/Serial/Nonce/Timestamp 请求头，
     * SDK 内部用平台证书公钥验签 + APIv3 密钥解密 resource.ciphertext，返回 Transaction。
     *
     * @param serialNumber 请求头 Wechatpay-Serial
     * @param nonce         请求头 Wechatpay-Nonce
     * @param signature     请求头 Wechatpay-Signature
     * @param timestamp     请求头 Wechatpay-Timestamp
     * @param body          请求体（JSON 字符串）
     * @return 解析后的 Transaction（含 outTradeNo, transactionId, tradeState）
     */
    public Transaction parseNotify(String serialNumber, String nonce, String signature, String timestamp, String body) {
        if (notificationParser == null) {
            throw new BusinessException(503, "微信支付未配置");
        }
        try {
            RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(serialNumber)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .body(body)
                .build();
            return notificationParser.parse(requestParam, Transaction.class);
        } catch (Exception e) {
            throw new BusinessException(400, "微信回调验签失败: " + e.getMessage());
        }
    }
}
