package com.web3.aiproxy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.web3.aiproxy.config.AiProxyProperties;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * AgentWorkflowService
 * (zh) Agent + Workflow 编排层（规则路由 + 多专用 Agent + 工具调用）。
 *
 * 工作流：
 *   1. 接收用户输入与历史消息
 *   2. 规则路由 -> 选定一个专用 Agent（写文章 / 查音乐 / 查数据 / 通用对话）
 *   3. 按需调用工具：查 Supabase（music/blog/demo）、网易云搜索、服务健康
 *   4. 组装 system prompt + 上下文 + 历史消息
 *   5. 交给 ProxyEngineService.chatCompletions（SSE）以大模型身份输出
 *
 * 说明：Agent 不面向最终用户暴露 sk- 令牌，而是用 ai-proxy.agent-system-token
 *       代表应用调用并计费。工具调用用 JDK HttpClient 直连 Supabase REST（服务端密钥）。
 */
@Service
public class AgentWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkflowService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 模型可用性探活：单条 8s 超时、整体 10s 上限，并发 8 条（避免把上游瞬时打满）
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration PROBE_TOTAL_TIMEOUT = Duration.ofSeconds(10);
    private static final ExecutorService PROBE_POOL = Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r, "agent-model-probe");
        t.setDaemon(true);
        return t;
    });

    private final ProxyEngineService engineService;
    private final AiProxyProperties props;
    private final UpstreamForwarder forwarder;
    private final HttpClient httpClient;

    // 规则关键词（中文）
    private static final List<Pattern> MUSIC_PATTERNS = List.of(
            Pattern.compile("(歌|音乐|播放|唱|曲|网易云|歌单|topic|music)"),
            Pattern.compile("(放个|听一下|来一首|推荐.{0,4}歌)"));
    private static final List<Pattern> ARTICLE_PATTERNS = List.of(
            Pattern.compile("(写|写一篇|写个|作文|文章|博客|文案|post|blog|markdown)"),
            Pattern.compile("(发布|投稿|标题|大纲|润色)"));
    private static final List<Pattern> DATA_PATTERNS = List.of(
            Pattern.compile("(数据|统计|查询|查一下|数据库|supabase|记录|table|报告|多少)"),
            Pattern.compile("(demo|music_tracks|blog_articles|service|健康|服务|启动)"));

    public AgentWorkflowService(ProxyEngineService engineService, AiProxyProperties props,
                                UpstreamForwarder forwarder) {
        this.engineService = engineService;
        this.props = props;
        this.forwarder = forwarder;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(4000))
                .build();
    }

    /**
     * Agent 对话统一入口：路由 + 建上下文 + 交给中转引擎（SSE）。
     *
     * @param messages 型如 [{"role":"user"|"assistant","content":"..."}]
     * @param model    会话台模型选择器选定的模型；为空则回退提供商配置的默认模型
     * @param clientIp 客户端 IP（透传给中转引擎用于限额/IP白名单）
     * @return 流式 ResponseEntity（SSE 格式）
     */
    @SuppressWarnings("unchecked")
    public ResponseEntity<StreamingResponseBody> chat(String messagesJson, String model, String clientIp) {
        JsonNode rawMessages;
        try {
            rawMessages = MAPPER.readTree(messagesJson);
        } catch (Exception e) {
            return fallbackSse("消息格式似乎有问题，请换一种说法再试。");
        }
        if (!rawMessages.isArray() || rawMessages.isEmpty()) {
            return fallbackSse("你好，我是 Web3 门户助手。请问有什么可以帮你？");
        }

        // 最近一条 user 输入用于路由
        String userInput = lastUserText(rawMessages);

        // 工具上下文（按需，失败不影响主流程）
        String toolContext = buildToolContext(userInput);

        // 组装带 system 的完整消息
        ObjectNode body = MAPPER.createObjectNode();
        body.put("stream", true);
        body.put("temperature", 0.7);
        ArrayNode finalMessages = body.putArray("messages");

        ObjectNode system = MAPPER.createObjectNode();
        system.put("role", "system");
        system.put("content", buildSystemPrompt(userInput, toolContext));
        finalMessages.add(system);
        finalMessages.addAll((ArrayNode) rawMessages);

        // 优先使用 sk- 系统令牌走渠道引擎（支持计费/限额/降级）
        String token = props.getAgentSystemToken();
        if (token != null && !token.isBlank()) {
            body.put("model", resolveModel(model, defaultModel()));
            String jsonBody;
            try {
                jsonBody = MAPPER.writeValueAsString(body);
            } catch (Exception e) {
                jsonBody = emptyBody();
            }
            String auth = "Bearer " + token.trim();
            Object resp = engineService.chatCompletions(jsonBody, auth, clientIp);
            if (resp instanceof ResponseEntity<?> re
                    && (re.getStatusCode().isError() || re.getBody() == null)) {
                return fallbackSse(fallbackHint(userInput));
            }
            return (ResponseEntity<StreamingResponseBody>) resp;
        }

        // 未配置系统令牌 → 尝试直连 LLM 提供商（api-key 非空的第一个）
        AiProxyProperties.LlmProvider provider = firstAvailableProvider();
        if (provider != null) {
            body.put("model", resolveModel(model, provider.getModel()));
            String jsonBodyTemp;
            try {
                jsonBodyTemp = MAPPER.writeValueAsString(body);
            } catch (Exception e) {
                jsonBodyTemp = emptyBody();
            }
            final String jsonBody = jsonBodyTemp;
            final String pName = provider.getName();
            final String pBaseUrl = provider.getBaseUrl();
            final String pKey = provider.getApiKey();
            log.info("[Agent] 使用直连 LLM: {} ({})", pName, provider.getModel());
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .header("Cache-Control", "no-cache")
                    .header("X-Accel-Buffering", "no")
                    .body(out -> {
                        try {
                            forwarder.forwardStreaming(pBaseUrl, pKey, jsonBody, out,
                                    new SseUsageCollector());
                        } catch (Exception e) {
                            log.error("[Agent] 直连 LLM 失败 ({}): {}", pName, e.getMessage());
                            try (OutputStreamWriter w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                                w.write("data: {\"choices\":[{\"delta\":{\"content\":\"[LLM 调用失败: "
                                        + (e.getMessage() == null ? "unknown" : e.getMessage().replace("\"", "'"))
                                        + "]\"}}]}\n\n");
                                w.write("data: [DONE]\n\n");
                                w.flush();
                            }
                        }
                    });
        }

        // 既无系统令牌，也无可用直连提供商 → 本地兜底
        return fallbackSse(fallbackHint(userInput));
    }

    /**
     * 解析本次请求实际使用的模型：会话台选择器选了就用用户选的，
     * 没选（null/空白）则回退到提供商配置的默认模型。
     */
    private String resolveModel(String requested, String fallback) {
        return (requested != null && !requested.isBlank()) ? requested.trim() : fallback;
    }

    /**
     * 列出当前生效 LLM 提供商可用的模型（供会话台模型选择器使用）。
     * 取第一个 api-key 非空的直连提供商，调用其 GET /models，并裁剪为
     * id / ownedBy / contextWindow 三项后回传 —— 不回传密钥，也不透传上游细节。
     * 每个模型再并发探活并附 ok 标记：会话台据此把上游拒答（403/429/超时）的
     * 模型标注为不可用。提供商不可用或请求失败时返回空列表，前端退化为「仅默认模型」。
     *
     * @return {"provider":名称|null,"default":默认模型,"models":[{id,ownedBy,contextWindow,ok}]}
     */
    public String listModels() {
        ObjectNode out = MAPPER.createObjectNode();
        ArrayNode arr = out.putArray("models");
        AiProxyProperties.LlmProvider provider = firstAvailableProvider();
        if (provider == null) {
            out.putNull("provider");
            return out.toString();
        }
        out.put("provider", provider.getName());
        // 提供商配置的默认模型：供前端选择器做初始选中项，避免前端硬编码模型名
        out.put("default", provider.getModel());

        // 先收集模型元数据，再统一并发探活
        List<ObjectNode> items = new ArrayList<>();
        try {
            ResponseEntity<byte[]> resp = forwarder.probeModels(provider.getBaseUrl(), provider.getApiKey());
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode root = MAPPER.readTree(resp.getBody());
                for (JsonNode m : root.path("data")) {
                    if (!m.isObject()) continue;
                    String id = m.path("id").asText("");
                    if (id.isBlank()) continue;
                    ObjectNode item = MAPPER.createObjectNode();
                    item.put("id", id);
                    item.put("ownedBy", m.path("owned_by").asText(""));
                    if (m.hasNonNull("context_window")) {
                        item.put("contextWindow", m.path("context_window").asLong());
                    }
                    items.add(item);
                }
            }
        } catch (Exception e) {
            log.warn("[Agent] 拉取模型列表失败 ({}): {}", provider.getName(), e.getMessage());
        }

        Map<String, Boolean> health = probeHealth(provider, items);
        for (ObjectNode item : items) {
            // 探活未返回的按不可用处理，避免前端把不可用模型当成可用
            item.put("ok", health.getOrDefault(item.path("id").asText(""), Boolean.FALSE));
            arr.add(item);
        }
        return out.toString();
    }

    /**
     * 并发探活各模型：单条最多 {@link #PROBE_TIMEOUT}，整体最多 {@link #PROBE_TOTAL_TIMEOUT}。
     * 探活只影响「可用性标注」，失败或超时一律视为不可用，不阻断列表返回。
     */
    private Map<String, Boolean> probeHealth(AiProxyProperties.LlmProvider provider, List<ObjectNode> items) {
        Map<String, Boolean> health = new ConcurrentHashMap<>();
        if (items.isEmpty()) return health;
        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        for (ObjectNode item : items) {
            String id = item.path("id").asText("");
            tasks.add(CompletableFuture.runAsync(() -> health.put(id,
                    forwarder.probeChat(provider.getBaseUrl(), provider.getApiKey(), id, PROBE_TIMEOUT)),
                    PROBE_POOL));
        }
        try {
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                    .get(PROBE_TOTAL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("[Agent] 模型探活未在 {}s 内全部返回，未返回的按不可用标注", PROBE_TOTAL_TIMEOUT.toSeconds());
        }
        return health;
    }

    /** 返回第一个 api-key 非空的 LLM 提供商，无则 null */
    private AiProxyProperties.LlmProvider firstAvailableProvider() {
        if (props.getAgentLlmProviders() == null) return null;
        for (AiProxyProperties.LlmProvider p : props.getAgentLlmProviders()) {
            if (p.getApiKey() != null && !p.getApiKey().isBlank()
                    && p.getBaseUrl() != null && !p.getBaseUrl().isBlank()) {
                return p;
            }
        }
        return null;
    }

    /**
     * 本地兜底（OpenAI 兼容 SSE 格式）：LLM 上游未配置/不可用时保证 Q&A 可用，
     * 前端 consumeAgentStream 无需改动即可按增量文本渲染。
     */
    private ResponseEntity<StreamingResponseBody> fallbackSse(String text) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(out -> {
                    try (OutputStreamWriter w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                        ArrayNode choices = MAPPER.createArrayNode();
                        choices.addObject().putObject("delta").put("content", text);
                        String payload = MAPPER.createObjectNode().set("choices", choices).toString();
                        w.write("data: " + payload + "\n\n");
                        w.write("data: [DONE]\n\n");
                        w.flush();
                    }
                });
    }

    private String fallbackHint(String userInput) {
        // 前缀 [NO_LLM_KEY] 作为结构化错误标识，前端据此显示更友好的提示
        return "[NO_LLM_KEY] 大模型上游暂未连通。请在 ai-proxy-service 的 application.yml 或环境变量中配置至少一个 LLM 提供商的 API Key。\n"
                + "支持的提供商：DeepSeek (DEEPSEEK_API_KEY)、SiliconFlow (SILICONFLOW_API_KEY)、"
                + "LM Studio (LMSTUDIO_API_KEY)、Groq (GROQ_API_KEY)、Doubao (DOUBAO_API_KEY)、"
                + "Together AI (TOGETHER_API_KEY)、Fireworks (FIREWORKS_API_KEY)、"
                + "OpenRouter (OPENROUTER_API_KEY)、Ollama (OLLAMA_API_KEY)。\n"
                + "填入任意一个 API Key 并重启服务即可恢复智能回复。\n\n"
                + "收到你的需求：\n" + (userInput == null ? "" : userInput);
    }

    // ───────── 规则路由 ─────────

    private String route(String input) {
        if (anyMatch(input, MUSIC_PATTERNS)) return "music";
        if (anyMatch(input, ARTICLE_PATTERNS)) return "article";
        if (anyMatch(input, DATA_PATTERNS)) return "data";
        return "general";
    }

    private boolean anyMatch(String input, List<Pattern> patterns) {
        for (Pattern p : patterns) {
            if (p.matcher(input).find()) return true;
        }
        return false;
    }

    private String buildSystemPrompt(String input, String toolContext) {
        String agent = route(input);
        String base = switch (agent) {
            case "music" -> "你是「音乐馆 Agent」。帮用户找歌、推荐、解答音乐相关问题；能用中文简洁回答，需要时可调用查询歌单/搜索能力。";
            case "article" -> "你是「写作 Agent」。帮用户起草/润色/排版文章（支持 Markdown），输出结构清晰、有感染力，中文为主。";
            case "data" -> "你是「数据 Agent」。回答关于本站数据、服务状态的问题，引用下方提供的实时数据，准确简洁。";
            default -> "你是「Web3 门户助手」。友好、专业地解答用户关于网站、功能、使用等各类问题。";
        };
        StringBuilder sb = new StringBuilder();
        sb.append(base).append("\n\n【当前路由 Agent】：").append(agent);
        if (toolContext != null && !toolContext.isBlank()) {
            sb.append("\n\n【工具上下文（仅作参考，不要声称来源不确定的信息）】：\n").append(toolContext);
        }
        sb.append("\n\n回复保持简洁、自然，中文为主。");
        return sb.toString();
    }

    // ───────── 工具调用（Supabase / 网易云 / 健康） ─────────

    private String buildToolContext(String input) {
        StringBuilder ctx = new StringBuilder();
        String agent = route(input);
        try {
            if ("music".equals(agent)) {
                String music = fetchJson(props.getSupabaseUrl(), "/rest/v1/music_tracks?select=title,artist,album,category&limit=8");
                ctx.append("Supabase 曲库示例：").append(music).append("\n");
            } else if ("data".equals(agent)) {
                String demo = fetchJson(props.getSupabaseUrl(), "/rest/v1/demo_items?select=name,category&limit=8");
                String blog = fetchJson(props.getSupabaseUrl(), "/rest/v1/blog_articles?select=title,summary&published=eq.true&limit=8");
                ctx.append("Supabase 示例数据：").append(demo).append("\n");
                ctx.append("Supabase 博客文章：").append(blog).append("\n");
            }
        } catch (Exception e) {
            log.warn("[Agent] 工具调用失败（跳过上下文）：{}", e.getMessage());
        }
        return ctx.toString();
    }

    private String fetchJson(String baseUrl, String path) throws Exception {
        String url = stripTrailingSlash(baseUrl) + path;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(5000))
                .header("apikey", props.getSupabaseServiceKey())
                .header("Authorization", "Bearer " + props.getSupabaseServiceKey())
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        return "HTTP " + resp.statusCode() + " " + truncate(resp.body(), 1500);
    }

    // ───────── 工具 ─────────

    private String lastUserText(JsonNode arr) {
        for (int i = arr.size() - 1; i >= 0; i--) {
            JsonNode m = arr.get(i);
            if (m.path("role").asText().equals("user")) {
                return m.path("content").asText("");
            }
        }
        return "";
    }

    private String defaultModel() {
        return props.getAgentDefaultModel() == null || props.getAgentDefaultModel().isBlank()
                ? "gpt-4o-mini" : props.getAgentDefaultModel();
    }

    private String emptyBody() {
        return "{\"model\":\"" + defaultModel() + "\",\"stream\":true,\"messages\":[{\"role\":\"user\",\"content\":\"你好\"}]}";
    }

    private String stripTrailingSlash(String s) {
        s = s == null ? "" : s.trim();
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
