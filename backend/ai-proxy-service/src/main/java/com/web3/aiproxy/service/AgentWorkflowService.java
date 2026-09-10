package com.web3.aiproxy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.web3.aiproxy.config.AiProxyProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

    private final ProxyEngineService engineService;
    private final AiProxyProperties props;
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

    public AgentWorkflowService(ProxyEngineService engineService, AiProxyProperties props) {
        this.engineService = engineService;
        this.props = props;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(4000))
                .build();
    }

    /**
     * Agent 对话统一入口：路由 + 建上下文 + 交给中转引擎（SSE）。
     *
     * @param messages 型如 [{"role":"user"|"assistant","content":"..."}]
     * @param clientIp 客户端 IP（透传给中转引擎用于限额/IP白名单）
     * @return ProxyEngineService 的流式/非流式 ResponseEntity
     */
    public Object chat(String messagesJson, String clientIp) {
        JsonNode rawMessages;
        try {
            rawMessages = MAPPER.readTree(messagesJson);
        } catch (Exception e) {
            return engineService.chatCompletions("{\"error\":\"bad request\"}", null, clientIp);
        }
        if (!rawMessages.isArray() || rawMessages.isEmpty()) {
            return engineService.chatCompletions(emptyBody(), null, clientIp);
        }

        // 最近一条 user 输入用于路由
        String userInput = lastUserText(rawMessages);

        // 工具上下文（按需，失败不影响主流程）
        String toolContext = buildToolContext(userInput);

        // 组装带 system 的完整消息
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", defaultModel());
        body.put("stream", true);
        body.put("temperature", 0.7);
        ArrayNode finalMessages = body.putArray("messages");

        ObjectNode system = MAPPER.createObjectNode();
        system.put("role", "system");
        system.put("content", buildSystemPrompt(userInput, toolContext));
        finalMessages.add(system);
        finalMessages.addAll((ArrayNode) rawMessages);

        String jsonBody;
        try {
            jsonBody = MAPPER.writeValueAsString(body);
        } catch (Exception e) {
            jsonBody = emptyBody();
        }

        // 用应用级系统令牌代表 Agent 调用（走渠道 + 计费）
        String auth = "Bearer " + (props.getAgentSystemToken() == null ? "" : props.getAgentSystemToken().trim());
        return engineService.chatCompletions(jsonBody, auth, clientIp);
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
