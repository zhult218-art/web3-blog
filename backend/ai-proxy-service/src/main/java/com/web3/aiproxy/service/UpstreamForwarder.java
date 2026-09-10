package com.web3.aiproxy.service;

import io.netty.channel.ChannelOption;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;

/**
 * 类名：UpstreamForwarder
 * 所属模块：ai-proxy-service（中转站）
 * 职责：上游 HTTP 转发器（基于 WebClient）——
 *       ① 非流式：阻塞式转发并返回上游原始响应；
 *       ② 流式：SSE 字节级透传到 Servlet 输出流，同时喂给 SseUsageCollector 统计 usage。
 *       读超时 300s，满足长上下文/长报告生成不被掐断（需求 §7.3）。
 */
@Slf4j
@Service
public class UpstreamForwarder {

    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(300);

    /** 按目标地址构建带超时的 WebClient */
    private WebClient client(String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(RESPONSE_TIMEOUT);
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * 非流式阻塞转发。
     *
     * @param baseUrl     上游 Base URL（渠道或兜底直连）
     * @param apiKey      上游 Key（兜底模式可传 null，由调用方决定 Authorization）
     * @param rawAuth     兜底透传的原始 Authorization 头（渠道模式下忽略）
     * @param body        请求体原文
     * @return 上游响应实体
     */
    public ResponseEntity<byte[]> forwardBlocking(String baseUrl, String apiKey, String rawAuth, String body) {
        WebClient.RequestHeadersSpec<?> spec = client(baseUrl).post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
        if (apiKey != null && !apiKey.isBlank()) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        } else if (rawAuth != null && !rawAuth.isBlank()) {
            spec.header(HttpHeaders.AUTHORIZATION, rawAuth);
        }
        return spec.retrieve().toEntity(byte[].class).block(Duration.ofSeconds(310));
    }

    /**
     * 流式 SSE 透传：把上游 DataBuffer 流逐块写入输出流并喂给收集器；阻塞至流结束。
     *
     * @throws org.springframework.web.reactive.function.client.WebClientResponseException 上游非 2xx
     */
    public void forwardStreaming(String baseUrl, String apiKey, String body,
                                 OutputStream out, SseUsageCollector collector) throws IOException {
        WebClient.RequestHeadersSpec<?> spec = client(baseUrl).post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body);
        if (apiKey != null && !apiKey.isBlank()) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }

        spec.retrieve()
                .onStatus(status -> status.isError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(b -> new UpstreamStatusException(resp.statusCode().value(), b)))
                .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
                .doOnNext(buffer -> {
                    try {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        org.springframework.core.io.buffer.DataBufferUtils.release(buffer);
                        out.write(bytes);
                        out.flush();
                        collector.feed(bytes, bytes.length);
                    } catch (IOException e) {
                        throw new IllegalStateException("client aborted", e);
                    }
                })
                .blockLast(Duration.ofSeconds(310));
    }

    /** 渠道连通性测试：GET /models 最小探针 */
    public ResponseEntity<byte[]> probeModels(String baseUrl, String apiKey) {
        return client(baseUrl).get()
                .uri("/models")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .retrieve().toEntity(byte[].class)
                .block(Duration.ofSeconds(20));
    }

    /** 上游错误状态异常（携带状态码，供引擎判断是否降级下一候选） */
    public static class UpstreamStatusException extends RuntimeException {
        @Getter
        private final int statusCode;

        public UpstreamStatusException(int statusCode, String body) {
            super("upstream " + statusCode + ": " + body);
            this.statusCode = statusCode;
        }
    }
}
