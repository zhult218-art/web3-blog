package com.web3.aiproxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 类名：AiProxyProperties
 * 所属模块：ai-proxy-service（中转站）
 * 职责：中转站配置项绑定（ai-proxy.* 前缀）—— 令牌 salt、渠道 Key 加密密钥、
 *       兜底直连地址、渠道选择策略。全部支持环境变量 / Nacos 覆盖。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-proxy")
public class AiProxyProperties {
    private String tokenSecret;
    private String channelKeySecret;
    private String fallbackBaseUrl;
    /** weight / random / round-robin */
    private String strategy;
    /** Agent 编排层使用的系统令牌（sk-...），让 Agent 以应用身份走渠道并计费 */
    private String agentSystemToken;
    /** Agent 默认模型名（可为空，由编排层按路由覆盖） */
    private String agentDefaultModel;
    /** Supabase 项目地址（Agent 工具用） */
    private String supabaseUrl;
    /** Supabase 服务端密钥（Agent 用 REST 读公开数据；浏览器端不持有） */
    private String supabaseServiceKey;
}
