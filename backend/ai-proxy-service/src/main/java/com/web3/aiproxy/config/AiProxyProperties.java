package com.web3.aiproxy.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 类名：AiProxyProperties
 * 所属模块：ai-proxy-service（中转站）
 * 职责：中转站配置项绑定（ai-proxy.* 前缀）—— 令牌 salt、渠道 Key 加密密钥、
 *       兜底直连地址、渠道选择策略。全部支持环境变量 / Nacos 覆盖。
 *
 * <p>2026-09-17 修订：增加 {@code @PostConstruct validateSecret()}，
 * 强制 {@code tokenSecret} 与 {@code channelKeySecret} 启动时必须注入，
 * 长度不少于 32 字符，避免历史上 "change-me-..." 默认值导致已加密的渠道 Key
 * 可被任意人解密。</p>
 *
 * <p>2026-09-18 修订：增加 {@code agentLlmProviders} 列表，
 * 支持直接配置多个 OpenAI 兼容 LLM 提供商（DeepSeek/SiliconFlow/LM Studio 等），
 * api-key 留空时跳过，填入后自动启用。Agent 在未配置 sk- 系统令牌时
 * 优先走直连模式。</p>
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
    /** Agent 直连 LLM 提供商列表（api-key 为空时跳过，填入即启用） */
    private List<LlmProvider> agentLlmProviders = new ArrayList<>();

    @Data
    public static class LlmProvider {
        /** 提供商名称（DeepSeek / SiliconFlow / LM Studio 等） */
        private String name;
        /** OpenAI 兼容 Base URL，如 https://api.deepseek.com/v1 */
        private String baseUrl;
        /** API Key（留空则跳过此提供商，填入后自动启用） */
        private String apiKey;
        /** 默认模型名 */
        private String model;
    }

    @PostConstruct
    void validateSecret() {
        requireSecret("AI_PROXY_TOKEN_SECRET", tokenSecret);
        requireSecret("AI_PROXY_CHANNEL_KEY_SECRET", channelKeySecret);
    }

    private void requireSecret(String envName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "ai-proxy 安全密钥未配置。请通过环境变量 " + envName + " 注入一个至少 32 字符的随机串。"
                + "历史默认值 'change-me-...' 已于 2026-09-17 移除，以防止渠道 Key 被解密。"
            );
        }
        if (value.length() < 32) {
            throw new IllegalStateException(envName + " 至少需要 32 字符，当前仅 " + value.length() + " 字符。");
        }
    }
}
