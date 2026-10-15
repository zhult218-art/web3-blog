package com.web3.aiproxy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.web3.aiproxy.config.AiProxyProperties;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * AgentFlowService
 * (zh) Supabase 驱动的多步技能工作流引擎：数据图表 / 表格整理 / 文档写作 / 海报绘画。
 *
 * 工作流定义存放在 Supabase 表 agent_workflows（后端 60s 缓存），拉取失败/为空时
 * 回退到内置默认工作流，保证功能开箱可用。执行时优先调用 ai-proxy 中转引擎
 * （需配置 AI_PROXY_AGENT_TOKEN 对应 sk- 令牌与启用渠道）；未配置或上游失败时
 * 使用本地确定性生成器，仍能产出图表/表格/Word/海报。
 *
 * SSE 事件：
 *   data: {"type":"state","state":"working"}
 *   data: {"type":"text","content":"..."}
 *   data: {"type":"artifact","format":"chart|table|word|image","payload":{...}}
 *   data: {"type":"meta","llm":bool,"title":"...","saved":bool}
 *   data: [DONE]
 */
@Service
public class AgentFlowService {

    private static final Logger log = LoggerFactory.getLogger(AgentFlowService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long CACHE_MS = 60_000L;

    private static final Pattern CHART_P = Pattern.compile("(图|图表|柱状|折线|饼图|环形|echarts|chart|可视化|统计图|趋势图|做图|画图|占比图)");
    private static final Pattern TABLE_P = Pattern.compile("(表格|数据表|做表|整理成表|报表|清单|对比表|行列|excel|csv)");
    private static final Pattern WORD_P = Pattern.compile("(word|文档|写文档|doc|docx|报告|周报|工作总结|述职|文书|信函|通知|方案书|计划书|商务邮件)");
    private static final Pattern IMAGE_P = Pattern.compile("(海报|贺卡|邀请函|宣传图|插画|封面图|设计图|画一幅|画一张|卡片)");

    private final ProxyEngineService engineService;
    private final AiProxyProperties props;
    private final HttpClient httpClient;

    private volatile List<JsonNode> flowCache;
    private volatile long flowCacheTs;

    public AgentFlowService(ProxyEngineService engineService, AiProxyProperties props) {
        this.engineService = engineService;
        this.props = props;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(4000)).build();
    }

    // ─────────────── 对外：列表 / 匹配 / 执行 ───────────────

    public String listJson() {
        try {
            ArrayNode arr = MAPPER.createArrayNode();
            for (JsonNode wf : loadFlows()) {
                ObjectNode m = MAPPER.createObjectNode();
                m.put("slug", t(wf, "slug"));
                m.put("name", t(wf, "name"));
                m.put("description", t(wf, "description"));
                m.put("icon", t(wf, "icon"));
                m.put("color", t(wf, "color"));
                m.put("sample", t(wf, "sample"));
                m.put("outputFormat", t(wf, "output_format"));
                m.put("stepCount", wf.path("steps").isArray() ? wf.path("steps").size() : 0);
                arr.add(m);
            }
            return MAPPER.writeValueAsString(arr);
        } catch (Exception e) {
            return "[]";
        }
    }

    public String matchJson(String message) {
        try {
            JsonNode wf = match(message);
            ObjectNode out = MAPPER.createObjectNode();
            if (wf == null) {
                out.put("matched", false);
                return MAPPER.writeValueAsString(out);
            }
            out.put("matched", true);
            ObjectNode m = MAPPER.createObjectNode();
            m.put("slug", t(wf, "slug"));
            m.put("name", t(wf, "name"));
            m.put("description", t(wf, "description"));
            m.put("icon", t(wf, "icon"));
            m.put("color", t(wf, "color"));
            m.put("sample", t(wf, "sample"));
            m.put("outputFormat", t(wf, "output_format"));
            m.set("steps", wf.path("steps"));
            out.set("workflow", m);
            return MAPPER.writeValueAsString(out);
        } catch (Exception e) {
            return "{\"matched\":false}";
        }
    }

    public ResponseEntity<StreamingResponseBody> execute(String slug, String answersJson, String clientIp) {
        JsonNode flow = findBySlug(slug);
        if (flow == null) {
            return errorEvent("未找到该技能（" + (slug == null ? "" : slug) + "），请稍后重试。");
        }
        Map<String, String> answers = parseAnswers(answersJson);
        String format = t(flow, "output_format");
        String system = t(flow, "system_prompt");
        if (system == null || system.isBlank()) {
            system = "你是 Web3 门户的智能助手，根据用户的回答生成内容。";
        }
        String user = describeAnswers(flow, answers);

        if ("text".equals(format)) {
            String out = llmRaw(system, user, clientIp);
            if (out == null) {
                out = "已收到你的需求：\n" + user.stripTrailing()
                        + "\n\n（尚未配置 LLM 上游 AI_PROXY_AGENT_TOKEN，暂以要点回显代替智能输出；在管理后台添加 OpenAI 兼容渠道并配置令牌后即可生成完整内容。）";
            }
            String finalOut = out;
            return sse(w -> {
                w.write("data: {\"type\":\"state\",\"state\":\"working\"}\n\n");
                emitText(w, finalOut);
                w.write("data: [DONE]\n\n");
            });
        }

        // 产物类：优先 LLM 结构化 JSON，失败走本地生成器
        JsonNode payload = null;
        boolean llm = false;
        String raw = llmRaw(system, user, clientIp);
        if (raw != null) {
            JsonNode node = parseStructured(raw);
            if (node != null) {
                payload = node;
                llm = true;
            }
        }
        if (payload == null) {
            payload = builder(format, answers);
        }
        if (payload == null || payload.isNull()) {
            payload = MAPPER.createObjectNode().put("error", "生成失败，请重试");
        }
        final JsonNode finalPayload = payload;

        String title = answers.getOrDefault("title", answers.getOrDefault("topic", t(flow, "name")));
        boolean saved = persistArtifact(t(flow, "slug"), title, format, payload);
        final boolean finalLlm = llm;
        final String artifactEvent = stringify(envelope("artifact", format, payload));
        final String metaEvent = stringify(metaNode(title, finalLlm, saved));
        String loading = "已收到 4 步信息，正在生成" + formatLabel(format) + "…";

        return sse(w -> {
            w.write("data: {\"type\":\"state\",\"state\":\"working\"}\n\n");
            emitText(w, loading);
            w.write("data: " + artifactEvent + "\n\n");
            w.write("data: " + metaEvent + "\n\n");
            w.write("data: [DONE]\n\n");
        });
    }

    // ─────────────── Supabase 工作流加载 ───────────────

    private List<JsonNode> loadFlows() {
        List<JsonNode> cached = flowCache;
        if (cached != null && System.currentTimeMillis() - flowCacheTs < CACHE_MS) {
            return cached;
        }
        List<JsonNode> loaded = null;
        String json = fetchSupabase("/rest/v1/agent_workflows?select=*&enabled=eq.true&order=sort_order.asc");
        if (json != null) {
            try {
                JsonNode arr = MAPPER.readTree(json);
                if (arr.isArray() && arr.size() > 0) {
                    loaded = new ArrayList<>();
                    arr.forEach(loaded::add);
                }
            } catch (Exception e) {
                log.warn("[AgentFlow] parse workflows failed: {}", e.getMessage());
            }
        }
        if (loaded == null || loaded.isEmpty()) {
            loaded = builtinFlows();
        }
        flowCache = loaded;
        flowCacheTs = System.currentTimeMillis();
        return loaded;
    }

    private JsonNode match(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String m = message.toLowerCase();
        JsonNode best = null;
        int score = 0;
        for (JsonNode wf : loadFlows()) {
            JsonNode triggers = wf.path("triggers");
            if (!triggers.isArray()) continue;
            int s = 0;
            for (JsonNode tg : triggers) {
                String k = tg.asText("");
                if (!k.isBlank() && m.contains(k.toLowerCase())) s++;
            }
            if (s > score) {
                score = s;
                best = wf;
            }
        }
        if (best == null) {
            if (CHART_P.matcher(message).find()) best = findBySlug("chart");
            else if (TABLE_P.matcher(message).find()) best = findBySlug("table");
            else if (WORD_P.matcher(message).find()) best = findBySlug("word");
            else if (IMAGE_P.matcher(message).find()) best = findBySlug("image");
        }
        return best;
    }

    private JsonNode findBySlug(String slug) {
        for (JsonNode wf : loadFlows()) {
            if (slug.equals(t(wf, "slug"))) return wf;
        }
        for (JsonNode wf : builtinFlows()) {
            if (slug.equals(t(wf, "slug"))) return wf;
        }
        return null;
    }

    // ─────────────── 内部工具 ───────────────

    private String describeAnswers(JsonNode flow, Map<String, String> answers) {
        StringBuilder sb = new StringBuilder();
        JsonNode steps = flow.path("steps");
        if (steps.isArray()) {
            for (JsonNode s : steps) {
                String id = t(s, "id");
                String q = t(s, "question");
                String v = answers.get(id);
                if (q == null || q.isBlank()) continue;
                sb.append("· ").append(q).append("：").append(v == null ? "" : v).append("\n");
            }
        }
        return sb.toString();
    }

    private Map<String, String> parseAnswers(String json) {
        Map<String, String> out = new LinkedHashMap<>();
        try {
            JsonNode n = MAPPER.readTree(json);
            if (n.isObject()) {
                n.fields().forEachRemaining(e -> {
                    JsonNode v = e.getValue();
                    out.put(e.getKey(), v.isNull() ? "" : (v.isTextual() ? v.asText() : v.toString()));
                });
            } else if (n.isArray()) {
                for (JsonNode a : n) {
                    String id = t(a, "id");
                    if (id != null && !id.isBlank()) out.put(id, t(a, "value"));
                }
            }
        } catch (Exception e) {
            log.warn("[AgentFlow] parse answers JSON failed: {}", e.getMessage());
        }
        return out;
    }

    /** 调用中转引擎（非流式）。未配置 token / 上游失败返回 null。响应计入 token 计费与日志。 */
    private String llmRaw(String system, String user, String clientIp) {
        String token = props.getAgentSystemToken();
        if (token == null || token.isBlank()) return null;
        try {
            ObjectNode body = MAPPER.createObjectNode();
            body.put("model", defaultModel());
            body.put("stream", false);
            body.put("temperature", 0.7);
            ArrayNode msgs = body.putArray("messages");
            msgs.addObject().put("role", "system").put("content", system);
            msgs.addObject().put("role", "user").put("content", user);
            Object resp = engineService.chatCompletions(
                    MAPPER.writeValueAsString(body), "Bearer " + token.trim(), clientIp);
            if (!(resp instanceof ResponseEntity<?> re)) return null;
            int sc = re.getStatusCode().value();
            if (sc < 200 || sc >= 300) {
                log.warn("[AgentFlow] engine non-200: {}", sc);
                return null;
            }
            Object b = re.getBody();
            if (b == null) return null;
            String json = b instanceof byte[] bb ? new String(bb, StandardCharsets.UTF_8) : b.toString();
            JsonNode root = MAPPER.readTree(json);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.isMissingNode() || content.isNull() ? null : content.asText();
        } catch (Exception e) {
            log.warn("[AgentFlow] engine call failed: {}", e.getMessage());
            return null;
        }
    }

    /** 从 LLM 输出稳健抽取首个 JSON 值（抗代码块/前后缀）。 */
    private JsonNode parseStructured(String raw) {
        if (raw == null) return null;
        String s = raw.replace("```json", "").replace("```", "").trim();
        int bs = s.indexOf('{');
        int as = s.indexOf('[');
        int from = -1;
        char open, close;
        if (bs >= 0 && (as < 0 || bs <= as)) {
            from = bs;
            open = '{';
            close = '}';
        } else if (as >= 0) {
            from = as;
            open = '[';
            close = ']';
        } else {
            return null;
        }
        int depth = 0;
        boolean inStr = false;
        char quote = 0;
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (c == '\\') { i++; continue; }
                if (c == quote) inStr = false;
                continue;
            }
            if (c == '"' || c == '\'') { inStr = true; quote = c; continue; }
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) {
                    try {
                        return MAPPER.readTree(s.substring(from, i + 1));
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    // ─────────────── 本地确定性生成器 ───────────────

    private JsonNode builder(String format, Map<String, String> a) {
        return switch (format) {
            case "chart" -> buildChart(a);
            case "table" -> buildTable(a);
            case "word" -> buildWord(a);
            case "image" -> buildImage(a);
            default -> null;
        };
    }

    private JsonNode buildChart(Map<String, String> a) {
        String typeRaw = a.getOrDefault("chartType", "柱状图");
        String title = a.getOrDefault("title", "数据图表");
        String dataRaw = a.getOrDefault("data", "");
        String theme = a.getOrDefault("theme", "科技冷色");
        boolean pie = typeRaw.contains("饼") || typeRaw.contains("环");
        String seriesType = typeRaw.contains("折线") ? "line" : (pie ? "pie" : "bar");

        List<String> cats = new ArrayList<>();
        List<Double> vals = new ArrayList<>();
        for (String part : dataRaw.split("[,，;；\n]")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            String[] kv = p.split("[=＝:]", 2);
            if (kv.length == 2) {
                try {
                    vals.add(Double.parseDouble(kv[1].trim().replace(",", "")));
                } catch (NumberFormatException ignored) {
                    continue;
                }
            }
            cats.add(kv[0].trim());
        }
        if (vals.size() != cats.size()) {
            while (vals.size() < cats.size()) vals.add(0d);
        }

        String[] palette = switch (theme) {
            case "暖色渐变" -> new String[]{"#ffb45e", "#ff8c5a", "#ff5e62"};
            case "青绿清爽" -> new String[]{"#6ee7b7", "#34d399", "#10b981"};
            default -> new String[]{"#7dd3fc", "#60a5fa", "#818cf8"};
        };

        ObjectNode opt = MAPPER.createObjectNode();
        opt.put("backgroundColor", "transparent");
        opt.put("title", title);
        opt.putObject("textStyle").put("color", "#cfd6e6");

        if (pie) {
            ObjectNode s = opt.putArray("series").addObject();
            s.put("name", title);
            s.put("type", "pie");
            if (typeRaw.contains("环")) {
                ArrayNode radius = s.putArray("radius");
                radius.add("45%");
                radius.add("72%");
            }
            ArrayNode data = s.putArray("data");
            for (int i = 0; i < Math.min(cats.size(), vals.size()); i++) {
                ObjectNode item = data.addObject();
                item.put("name", cats.get(i));
                item.put("value", vals.get(i));
                item.putObject("itemStyle").put("color", palette[i % palette.length]);
            }
            opt.putArray("legend").addObject().put("top", "bottom")
                    .putObject("textStyle").put("color", "#9aa3b5");
        } else {
            ObjectNode tooltip = opt.putArray("tooltip").addObject();
            tooltip.put("trigger", "axis");
            ObjectNode xa = opt.putArray("xAxis").addObject();
            ArrayNode xs = xa.putArray("data");
            cats.forEach(xs::add);
            xa.putObject("axisLabel").put("color", "#9aa3b5");
            xa.putObject("axisLine").put("lineStyle", MAPPER.createObjectNode().put("color", "#2c3652"));
            ObjectNode ya = opt.putArray("yAxis").addObject();
            ya.putObject("splitLine").put("lineStyle", MAPPER.createObjectNode().put("color", "#1c2338"));
            ObjectNode s = opt.putArray("series").addObject();
            s.put("name", "数值");
            s.put("type", seriesType);
            s.put("smooth", true);
            s.put("barWidth", "46%");
            ArrayNode sd = s.putArray("data");
            vals.forEach(v -> sd.add(v));
            s.putObject("itemStyle").put("color", palette[0]);
            if ("line".equals(seriesType)) {
                s.putObject("areaStyle").put("opacity", 0.22);
                s.putObject("lineStyle").put("color", palette[0]).put("width", 3);
            }
        }

        ObjectNode out = MAPPER.createObjectNode();
        out.set("option", opt);
        return out;
    }

    private JsonNode buildTable(Map<String, String> a) {
        String title = a.getOrDefault("title", "数据表格");
        ArrayNode cols = MAPPER.createArrayNode();
        for (String c : a.getOrDefault("columns", "").split("[,，、|/]")) {
            String x = c.trim();
            if (!x.isEmpty()) cols.add(x);
        }
        if (cols.size() == 0) cols.add("数值");
        ArrayNode rows = MAPPER.createArrayNode();
        for (String line : (a.getOrDefault("rows", "") + "\n").split("[;；\n]")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            ArrayNode row = MAPPER.createArrayNode();
            for (String cell : line.split("[,，、|/]")) row.add(cell.trim());
            rows.add(row);
        }
        ObjectNode out = MAPPER.createObjectNode();
        out.put("title", title);
        out.set("columns", cols);
        out.set("rows", rows);
        return out;
    }

    private JsonNode buildWord(Map<String, String> a) {
        String title = a.getOrDefault("title", "文档");
        String style = a.getOrDefault("style", "工作总结");
        String length = a.getOrDefault("length", "标准（约800字）");
        String content = a.getOrDefault("content", "");
        ArrayNode sections = MAPPER.createArrayNode();
        ObjectNode sec = MAPPER.createObjectNode();
        sec.put("heading", style);
        ArrayNode paras = sec.putArray("paragraphs");
        if (!content.isBlank()) {
            for (String line : content.split("\n|\r\n")) {
                String x = line.trim();
                if (!x.isEmpty()) paras.add(x);
            }
        } else {
            paras.add("本篇文章围绕「" + title + "」展开，以下按「" + style + "」的体例进行组织。");
            paras.add("（这是本地默认生成内容。配置 LLM 上游 AI_PROXY_AGENT_TOKEN 后，将由智能模型根据你的要点生成完整正文。）");
        }
        sec.putArray("bullets");
        sec.put("note", "篇幅约定：" + length);
        sections.add(sec);
        ObjectNode out = MAPPER.createObjectNode();
        out.put("title", title);
        out.set("sections", sections);
        out.put("footer", "Web3 Portal · Agent 自动生成");
        return out;
    }

    private JsonNode buildImage(Map<String, String> a) {
        String topic = a.getOrDefault("topic", "");
        String style = a.getOrDefault("style", "科技未来");
        String text = a.getOrDefault("text", "");
        String palette = a.getOrDefault("palette", "青紫霓虹");

        String[] cols = switch (palette) {
            case "暖橙渐变" -> new String[]{"#FF8E53", "#FFB86C", "#2B1B0F"};
            case "玉石青绿" -> new String[]{"#34D399", "#6EE7B7", "#0B1F17"};
            case "黑白高级" -> new String[]{"#EAEAEA", "#808080", "#0A0A0A"};
            default -> new String[]{"#7F5AF0", "#2CB67D", "#14101F"};
        };
        String t1 = escapeXml(topic.isBlank() ? "AI 绘画" : topic);
        String t2 = escapeXml(style) + " · " + escapeXml(palette);
        String body = text.isBlank() ? "" :
                "<text x='360' y='340' text-anchor='middle' font-family='PingFang SC,sans-serif' font-size='46' font-weight='600' fill='#ffffff'>" + escapeXml(text) + "</text>";

        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='720' height='540' viewBox='0 0 720 540'>"
                + "<defs><linearGradient id='bg' x1='0' y1='0' x2='1' y2='1'>"
                + "<stop offset='0' stop-color='" + cols[0] + "'/><stop offset='1' stop-color='" + cols[2] + "'/></linearGradient>"
                + "<radialGradient id='glow' cx='0.5' cy='0.4' r='0.7'><stop offset='0' stop-color='" + cols[1]
                + "' stop-opacity='0.9'/><stop offset='1' stop-color='" + cols[1] + "' stop-opacity='0'/></radialGradient></defs>"
                + "<rect width='720' height='540' fill='url(#bg)'/>"
                + "<circle cx='560' cy='120' r='180' fill='url(#glow)'/><circle cx='120' cy='440' r='160' fill='url(#glow)' opacity='0.6'/>"
                + "<circle cx='640' cy='420' r='90' fill='" + cols[1] + "' opacity='0.3'/>"
                + "<circle cx='90' cy='100' r='46' fill='" + cols[1] + "' opacity='0.25'/>"
                + "<g stroke='#ffffff' stroke-opacity='0.12' stroke-width='1' fill='none'>"
                + "<rect x='40' y='40' width='640' height='460' rx='30'/><circle cx='360' cy='270' r='120'/></g>"
                + "<text x='360' y='190' text-anchor='middle' font-family='PingFang SC,sans-serif' font-size='64' font-weight='700' fill='#ffffff' letter-spacing='4'>" + t1 + "</text>"
                + "<text x='360' y='232' text-anchor='middle' font-family='PingFang SC,sans-serif' font-size='22' fill='#ffffff' opacity='0.85'>" + t2 + "</text>"
                + body
                + "<text x='360' y='498' text-anchor='middle' font-family='PingFang SC,sans-serif' font-size='15' fill='#ffffff' opacity='0.5'>Web3 Portal · Agent 生成</text>"
                + "</svg>";
        ObjectNode out = MAPPER.createObjectNode();
        out.put("svg", svg);
        return out;
    }

    // ─────────────── Supabase 持久化 ───────────────

    private boolean persistArtifact(String slug, String title, String format, JsonNode payload) {
        try {
            ObjectNode body = MAPPER.createObjectNode();
            body.put("workflow_slug", slug == null ? "" : slug);
            body.put("title", title == null ? "" : title);
            body.put("format", format);
            body.set("payload", payload);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(strip(props.getSupabaseUrl()) + "/rest/v1/agent_artifacts"))
                    .timeout(Duration.ofMillis(5000))
                    .header("Content-Type", "application/json")
                    .header("apikey", props.getSupabaseServiceKey())
                    .header("Authorization", "Bearer " + props.getSupabaseServiceKey())
                    .header("Prefer", "return=minimal")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() >= 200 && resp.statusCode() < 300;
        } catch (Exception e) {
            log.warn("[AgentFlow] persist artifact skipped: {}", e.getMessage());
            return false;
        }
    }

    private String fetchSupabase(String path) {
        try {
            String key = props.getSupabaseServiceKey();
            if (key == null || key.isBlank()) return null;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(strip(props.getSupabaseUrl()) + path))
                    .timeout(Duration.ofMillis(5000))
                    .header("apikey", key)
                    .header("Authorization", "Bearer " + key)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) return resp.body();
            return null;
        } catch (Exception e) {
            log.warn("[AgentFlow] fetch supabase {} failed: {}", path, e.getMessage());
            return null;
        }
    }

    // ─────────────── 内置默认工作流（Supabase 不可用时兜底） ───────────────

    private List<JsonNode> builtinFlows() {
        List<JsonNode> list = new ArrayList<>();
        list.add(flow("chart", "数据图表", "📊", "#6ee7b7",
                "帮我画一张 2026 年各季度销售趋势折线图",
                new String[]{"图", "图表", "柱状", "折线", "饼图", "环形", "可视化", "统计图", "趋势图", "做图", "画图", "echarts", "chart", "占比图"},
                List.of(
                        step("chartType", "你想做哪种类型的图表？", "select", List.of("柱状图", "折线图", "饼图", "环形图")),
                        step("title", "给这张图表起个标题吧", "text", "例如：2026 年各季度销售额"),
                        step("data", "提供数据：类别=数值，多个用逗号分隔", "textarea", "例如：一月=120, 二月=150, 三月=98"),
                        step("theme", "希望用哪种配色风格？", "select", List.of("科技冷色", "暖色渐变", "青绿清爽"))
                ),
                "你是资深数据可视化工程师。根据用户回答输出一张 ECharts 配置。严格只输出一个 JSON 对象，不要代码块与解释：{\"title\":标题,\"xAxis\":{\"data\":[类别]},\"series\":[{\"name\":\"数值\",\"type\":\"bar|line|pie\",\"data\":[数值]}]}。饼图/环形图改用 series[0].data=[{name,value}] 且省略 xAxis。",
                "chart"));
        list.add(flow("table", "表格整理", "📋", "#67e8f9",
                "帮我把这几个城市的人口数据整理成一张表",
                new String[]{"表格", "数据表", "做表", "整理成表", "报表", "清单", "对比表", "行列", "excel", "csv"},
                List.of(
                        step("title", "表格的名称或标题是什么？", "text", "例如：主要城市人口对比"),
                        step("columns", "表头有哪些列？用逗号分隔", "text", "例如：城市,人口(万),地区"),
                        step("rows", "逐行给数据：每行各列用逗号分隔，行之间用分号分隔", "textarea", "例如：北京,2188,华北;上海,2487,华东;广州,1868,华南")
                ),
                "你是数据分析助手。根据用户回答输出一张结构化表格。严格只输出一个 JSON 对象：{\"title\":标题,\"columns\":[\"列名\",...],\"rows\":[[\"值\",...],...]}，保证列数与每行单元格一致。",
                "table"));
        list.add(flow("word", "文档写作", "📄", "#fcd34d",
                "写一篇 800 字的项目季度工作总结",
                new String[]{"word", "文档", "写文档", "doc", "docx", "报告", "周报", "工作总结", "述职", "文书", "信函", "通知", "方案书", "计划书", "商务邮件"},
                List.of(
                        step("title", "文档标题或主题是什么？", "text", "例如：2026 年 Q3 项目工作总结"),
                        step("style", "文档属于哪类文体？", "select", List.of("工作总结", "工作周报", "方案书", "通知公告", "商务邮件", "学习计划")),
                        step("length", "篇幅要多长？", "select", List.of("精简（约300字）", "标准（约800字）", "详细（约1500字）")),
                        step("content", "需要包含哪些内容要点？（可选，可留空）", "textarea", "例如：上线了新功能、用户量翻倍、下一步计划")
                ),
                "你是专业的文字工作者。根据用户的文体与篇幅写出结构完整的文档正文。严格只输出一个 JSON 对象：{\"title\":标题,\"sections\":[{\"heading\":小节标题,\"paragraphs\":[\"段落\",...],\"bullets\":[\"要点\"]}],\"footer\":\"落款可选\"}。",
                "word"));
        list.add(flow("image", "海报绘画", "🎨", "#e0aaff",
                "画一张科技风的新年贺卡",
                new String[]{"海报", "贺卡", "邀请函", "宣传图", "插画", "封面图", "设计图", "画一幅", "画一张", "卡片"},
                List.of(
                        step("topic", "画面主题是什么？想表达什么主体内容？", "text", "例如：宇宙飞船穿过星云"),
                        step("style", "选择一种绘画风格", "select", List.of("极简色块", "科技未来", "中国风", "赛博霓虹")),
                        step("text", "画面上要放一段主文字（可留空）", "text", "例如：新春快乐"),
                        step("palette", "整体色调？", "select", List.of("青紫霓虹", "暖橙渐变", "玉石青绿", "黑白高级"))
                ),
                "你是平面设计师。根据用户回答绘制一张 SVG 海报。严格只输出一个 JSON 对象：{\"svg\":\"<svg width='720' height='540' 的完整 SVG XML>\"}。要求：美丽渐变背景、分层几何装饰、主题元素、居中排版主文字；只用 rect/circle/path/text/defs/linearGradient/radialGradient；不要外部资源。",
                "image"));
        return list;
    }

    private JsonNode flow(String slug, String name, String icon, String color, String sample,
                          String[] triggers, List<ObjectNode> steps, String systemPrompt, String format) {
        ObjectNode wf = MAPPER.createObjectNode();
        wf.put("slug", slug);
        wf.put("name", name);
        wf.put("description", name + "：分步收集需求，自动为你产出成品。");
        wf.put("icon", icon);
        wf.put("color", color);
        wf.put("sample", sample);
        ArrayNode tr = wf.putArray("triggers");
        for (String tgr : triggers) tr.add(tgr);
        ArrayNode st = wf.putArray("steps");
        steps.forEach(st::add);
        wf.put("system_prompt", systemPrompt);
        wf.put("output_format", format);
        wf.put("enabled", true);
        return wf;
    }

    private ObjectNode step(String id, String question, String type, Object option) {
        ObjectNode s = MAPPER.createObjectNode();
        s.put("id", id);
        s.put("question", question);
        s.put("type", type);
        if (option instanceof String placeholder) {
            s.put("placeholder", placeholder);
        } else if (option instanceof List<?> list) {
            ArrayNode arr = s.putArray("options");
            for (Object o : list) arr.add(String.valueOf(o));
        }
        return s;
    }

    // ─────────────── SSE 工具 ───────────────

    private ResponseEntity<StreamingResponseBody> sse(SseWriter writer) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "event-stream", StandardCharsets.UTF_8))
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(out -> {
                    try (OutputStreamWriter w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                        writer.write(w);
                        w.flush();
                    } catch (IOException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new IOException("sse write failed", e);
                    }
                });
    }

    @FunctionalInterface
    interface SseWriter {
        void write(OutputStreamWriter w) throws Exception;
    }

    private ResponseEntity<StreamingResponseBody> errorEvent(String message) {
        return sse(w -> {
            w.write("data: {\"type\":\"error\",\"content\":\"" + escapeJson(message) + "\"}\n\n");
            w.write("data: [DONE]\n\n");
        });
    }

    private void emitText(OutputStreamWriter w, String content) throws Exception {
        w.write("data: {\"type\":\"text\",\"content\":\"" + escapeJson(content) + "\"}\n\n");
    }

    private ObjectNode envelope(String type, String format, JsonNode payload) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("type", type);
        n.put("format", format);
        n.set("payload", payload);
        return n;
    }

    private ObjectNode metaNode(String title, boolean llm, boolean saved) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("type", "meta");
        n.put("llm", llm);
        n.put("title", title == null ? "" : title);
        n.put("saved", saved);
        return n;
    }

    private String stringify(JsonNode n) {
        try {
            return MAPPER.writeValueAsString(n);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String formatLabel(String format) {
        return switch (format) {
            case "chart" -> "图表";
            case "table" -> "表格";
            case "word" -> "Word 文档";
            case "image" -> "海报";
            default -> "内容";
        };
    }

    private String defaultModel() {
        String m = props.getAgentDefaultModel();
        return m == null || m.isBlank() ? "gpt-4o-mini" : m;
    }

    private String t(JsonNode n, String field) {
        JsonNode v = n == null ? null : n.get(field);
        if (v == null || v.isNull()) return "";
        return v.isObject() || v.isArray() ? v.toString() : v.asText();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "").replace("\t", "\\t");
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String strip(String url) {
        url = url == null ? "" : url.trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}