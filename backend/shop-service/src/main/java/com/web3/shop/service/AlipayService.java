package com.web3.shop.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.web3.common.core.BusinessException;
import com.web3.shop.config.PayConfig;
import com.web3.shop.entity.Order;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * AlipayService —— 支付宝支付服务
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：封装支付宝官方 SDK 调用，提供电脑网站支付（page.pay）下单与异步回调验签。
 * <p>
 * 流程：
 * <ol>
 *   <li>前端 POST /pay/alipay → 本服务调 createPagePay 生成支付表单 HTML → 前端新窗口 document.write 跳转支付宝</li>
 *   <li>用户在支付宝页面完成支付 → 支付宝 POST 回调 notify_url → 本服务 verifyNotify 验签 → 更新订单为 PAID</li>
 *   <li>支付宝收到 "success" 字符串响应后停止重试，否则会按 1m/5m/10m/... 节奏重试最多 8 次</li>
 * </ol>
 * <p>
 * 凭证未配置时 alipayClient 为 null，createPagePay 抛 503 提示填配置；verifyNotify 直接返回 false。
 */
@Service
public class AlipayService {
    private final PayConfig payConfig;
    private volatile AlipayClient alipayClient;

    public AlipayService(PayConfig payConfig) {
        this.payConfig = payConfig;
    }

    @PostConstruct
    public void init() {
        if (payConfig.alipayConfigured()) {
            PayConfig.Alipay a = payConfig.getAlipay();
            alipayClient = new DefaultAlipayClient(
                a.getGateway(), a.getAppId(), a.getPrivateKey(),
                "json", "UTF-8", a.getPublicKey(), a.getSignType()
            );
        }
    }

    /**
     * 创建电脑网站支付（page.pay），返回完整表单 HTML
     * <p>
     * 前端拿到 HTML 后用新窗口 document.write(html) 即可自动提交跳转到支付宝收银台。
     *
     * @param order 待支付订单（必须处于 PENDING 状态，amount 为元）
     * @return 支付宝支付的完整 form HTML
     */
    public String createPagePay(Order order) {
        if (alipayClient == null) {
            throw new BusinessException(503, "支付宝未配置：请在 .env 填入 ALIPAY_APPID/ALIPAY_PRIVATE_KEY/ALIPAY_PUBLIC_KEY/ALIPAY_NOTIFY_URL");
        }
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(payConfig.getAlipay().getNotifyUrl());
            request.setReturnUrl(payConfig.getAlipay().getReturnUrl());
            // out_trade_no 用订单主键（Long），支付宝要求 1-64 位；total_amount 保留 2 位小数
            String bizContent = String.format(
                "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}",
                String.valueOf(order.getId()),
                order.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                "Web3 Portal 订单 " + order.getOrderNo()
            );
            request.setBizContent(bizContent);
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            if (!response.isSuccess()) {
                throw new BusinessException(500, "支付宝下单失败: " + response.getSubMsg());
            }
            return response.getBody();
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(500, "支付宝下单异常: " + e.getMessage());
        }
    }

    /**
     * 验证异步回调签名
     * <p>
     * 支付宝回调 POST 表单参数，用支付宝公钥 RSA2 验签，防止伪造。
     *
     * @param params 回调请求的所有参数（已转为 Map<String,String>）
     * @return 验签是否通过
     */
    public boolean verifyNotify(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(
                params, payConfig.getAlipay().getPublicKey(), "UTF-8", payConfig.getAlipay().getSignType()
            );
        } catch (Exception e) {
            return false;
        }
    }
}
