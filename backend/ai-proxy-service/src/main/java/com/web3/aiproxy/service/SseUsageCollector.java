package com.web3.aiproxy.service;

import java.nio.charset.StandardCharsets;

/**
 * 类名：SseUsageCollector
 * 所属模块：ai-proxy-service（中转站）
 * 职责：流式响应的 usage 收集器 —— 在字节级缓冲 SSE 行（以 0x0A 切行，UTF-8 安全），
 *       解析每个 data 块：累计 delta 内容长度用于估算、捕获末尾 usage 精确值。
 *       上游未返回 usage 时按 chars/4 估算（文档 §7.4 流式乘数估算）。
 */
public class SseUsageCollector {

    private byte[] pending = new byte[0];
    private int promptTokens = -1;
    private int completionTokens = -1;
    private int contentChars = 0;

    /** 追加一段原始字节并处理其中已完成的行 */
    public synchronized void feed(byte[] chunk, int len) {
        byte[] merged = new byte[pending.length + len];
        System.arraycopy(pending, 0, merged, 0, pending.length);
        System.arraycopy(chunk, 0, merged, pending.length, len);
        int start = 0;
        for (int i = 0; i < merged.length; i++) {
            if (merged[i] == '\n') {
                int lineLen = i - start;
                if (lineLen > 0) processLine(new String(merged, start, lineLen, StandardCharsets.UTF_8));
                start = i + 1;
            }
        }
        pending = new byte[merged.length - start];
        System.arraycopy(merged, start, pending, 0, pending.length);
    }

    private void processLine(String line) {
        String trimmed = line.trim();
        if (!trimmed.startsWith("data:")) return;
        String payload = trimmed.substring(5).trim();
        if (payload.isEmpty() || "[DONE]".equals(payload)) return;
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
            com.fasterxml.jackson.databind.JsonNode usage = node.get("usage");
            if (usage != null && usage.isObject()) {
                if (usage.hasNonNull("prompt_tokens")) promptTokens = usage.get("prompt_tokens").asInt();
                if (usage.hasNonNull("completion_tokens")) completionTokens = usage.get("completion_tokens").asInt();
            }
            com.fasterxml.jackson.databind.JsonNode choices = node.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                com.fasterxml.jackson.databind.JsonNode delta = choices.get(0).get("delta");
                if (delta != null && delta.hasNonNull("content")) {
                    contentChars += delta.get("content").asText("").length();
                }
            }
        } catch (Exception ignored) {
            // 非完整 JSON 的心跳/注释行，忽略
        }
    }

    /** 是否捕获到上游精确 usage */
    public boolean hasUsage() {
        return promptTokens >= 0 && completionTokens >= 0;
    }

    public int getPromptTokens() {
        return promptTokens >= 0 ? promptTokens : Math.max(1, contentChars / 8);
    }

    public int getCompletionTokens() {
        if (completionTokens >= 0) return completionTokens;
        return Math.max(1, contentChars * 3 / 8); // 中文占比高时按 ~2.7 chars/token 估算
    }
}
