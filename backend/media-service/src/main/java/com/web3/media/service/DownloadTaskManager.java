package com.web3.media.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web3.media.config.MediaDownloadProperties;
import com.web3.media.config.MediaUploadProperties;
import com.web3.media.entity.Video;
import com.web3.media.mapper.VideoMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * DownloadTaskManager —— 视频 URL 下载任务管理器
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：把 yt-dlp 下载从「阻塞 HTTP 请求」改为「后台任务 + 进度上报」，供前端轮询。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>固定大小线程池限制并发下载数，避免带宽/CPU 被单个用户打满</li>
 *   <li>用 yt-dlp 的 --progress-template 拿到结构化进度（字节数/总量/速度/剩余时间），
 *       替代逐行正则解析 [download] xx% 文本</li>
 *   <li>下载完成后落库为 video 记录，任务状态置 success 并回传 videoId</li>
 *   <li>失败时清理 .part/.ytdl 残留文件，并把 yt-dlp 的真实错误行带回前端</li>
 * </ul>
 *
 * <p><b>标题来源</b>：yt-dlp.exe（PyInstaller 构建）在中文 Windows 下把 stdout 按 GBK 输出，
 * 用 UTF-8 解码会得到乱码，且 PYTHONIOENCODING 对其无效。因此标题改从
 * yt-dlp 写出的 {@code info.json}（固定 UTF-8 编码）读取，与系统区域设置无关。</p>
 */
@Component
public class DownloadTaskManager {
    private static final Logger log = LoggerFactory.getLogger(DownloadTaskManager.class);
    /** 读取 yt-dlp info.json 用（ObjectMapper 配置后线程安全） */
    private static final ObjectMapper JSON = new ObjectMapper();

    /** 下载进度标记行前缀 */
    private static final String M_PROG = "JOBPROG|";
    /** 最终文件路径标记行前缀 */
    private static final String M_FILE = "JOBFILE|";
    /** 后处理（合并音视频等）标记行前缀 */
    private static final String M_PP = "JOBPP|";
    /** yt-dlp 中断留下的分片/临时文件（需在失败后清理） */
    private static final Pattern LEFTOVER = Pattern.compile(".*\\.(part|ytdl)$|.*\\.part-Frag\\d+$");

    private final MediaUploadProperties uploadProperties;
    private final MediaDownloadProperties downloadProperties;
    private final VideoMapper videoMapper;
    private final ExecutorService pool;
    private final Map<String, DownloadTask> tasks = new ConcurrentHashMap<>();

    /** yt-dlp 可用性探测结果缓存（避免每个请求都起进程探测） */
    private volatile Boolean ytdlpOk;

    public DownloadTaskManager(MediaUploadProperties uploadProperties,
                               MediaDownloadProperties downloadProperties,
                               VideoMapper videoMapper) {
        this.uploadProperties = uploadProperties;
        this.downloadProperties = downloadProperties;
        this.videoMapper = videoMapper;
        int n = Math.max(1, downloadProperties.getConcurrency());
        this.pool = Executors.newFixedThreadPool(n, r -> {
            Thread t = new Thread(r, "video-download-worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 下载任务状态快照（对外序列化给前端轮询）
     */
    @Getter
    public static class DownloadTask {
        /** 任务号 */
        private final String taskId;
        /** queued / downloading / merging / success / failed */
        private volatile String status = "queued";
        /** 进度百分比 0~100 */
        private volatile int percent = 0;
        /** 下载速度（已格式化，如 2.3 MB/s） */
        private volatile String speed = "";
        /** 预计剩余时间（mm:ss / h:mm:ss） */
        private volatile String eta = "";
        /** 视频标题（yt-dlp 探测） */
        private volatile String title = "";
        /** 失败原因（可读信息） */
        private volatile String error;
        /** 成功入库后的视频 ID */
        private volatile String videoId;

        public DownloadTask(String taskId) {
            this.taskId = taskId;
        }
    }

    /**
     * 创建并立即开始一个下载任务（异步执行）
     *
     * @param url      视频页面 URL
     * @param quality  best / 1080p / 720p / 480p / audio
     * @param subtitle 是否下载字幕
     * @param tags     入库标签
     * @return 任务快照（含 taskId）
     */
    public DownloadTask start(String url, String quality, boolean subtitle, String tags) {
        // URL 安全校验（阻断选项注入与内网 SSRF）优先于一切
        validateDownloadUrl(url);
        if (!ytdlpAvailable()) {
            throw new IllegalStateException("yt-dlp 不可用：" + this.downloadProperties.getYtdlpPath()
                    + "（请检查根 .env 的 YTDLP_PATH）");
        }
        String taskId = UUID.randomUUID().toString().replace("-", "");
        DownloadTask task = new DownloadTask(taskId);
        this.tasks.put(taskId, task);
        this.pool.submit(() -> this.execute(task, url, quality, subtitle, tags));
        return task;
    }

    /**
     * 下载 URL 安全校验：
     * <ul>
     *   <li>仅允许 http/https 且长度 ≤ 2048，拒绝空白/控制字符；</li>
     *   <li>拒绝本机/内网/链路本地主机（防 SSRF 探测内网与云元数据）；</li>
     *   <li>非 http(s) 前缀从根源上排除以 "-" 开头的参数注入值。</li>
     * </ul>
     * 校验失败抛 {@link IllegalArgumentException}（controller 映射为 400）。
     */
    private static void validateDownloadUrl(String url) {
        if (url == null || url.isBlank() || url.length() > 2048) {
            throw new IllegalArgumentException("URL 不合法");
        }
        String trimmed = url.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new IllegalArgumentException("仅支持 http/https 视频链接");
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < 0x20 || c == ' ' || c == '"' || c == '\'') {
                throw new IllegalArgumentException("URL 含非法字符");
            }
        }
        String host;
        try {
            host = java.net.URI.create(trimmed).getHost();
        } catch (Exception e) {
            throw new IllegalArgumentException("URL 解析失败");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL 缺少主机名");
        }
        String h = host.toLowerCase(Locale.ROOT).replace("[", "").replace("]", "");
        boolean privateHost = h.equals("localhost") || h.equals("0.0.0.0") || h.equals("::1")
                || h.startsWith("127.") || h.startsWith("10.") || h.startsWith("192.168.")
                || h.startsWith("169.254.") || h.endsWith(".local") || h.endsWith(".internal");
        if (!privateHost && h.startsWith("172.")) {
            String[] seg = h.split("\\.");
            if (seg.length > 1) {
                try {
                    int second = Integer.parseInt(seg[1]);
                    privateHost = second >= 16 && second <= 31;
                } catch (NumberFormatException ignore) {
                    // 非数字段，保持非内网判定
                }
            }
        }
        if (privateHost) {
            throw new IllegalArgumentException("不允许下载内网/本机地址");
        }
    }

    /**
     * 查询任务状态
     *
     * @param taskId 任务号
     * @return 任务快照；不存在时返回 null
     */
    public DownloadTask get(String taskId) {
        return this.tasks.get(taskId);
    }

    // ─────────────────────────── 执行体 ───────────────────────────

    private void execute(DownloadTask task, String url, String quality, boolean subtitle, String tags) {
        Path dir = Paths.get(this.uploadProperties.getDir()).toAbsolutePath().normalize();
        String base = UUID.randomUUID().toString();          // 输出文件名基（UUID.ext）
        String finalName = base + ".mp4";
        try {
            Files.createDirectories(dir);
            task.status = "downloading";

            Process proc = new ProcessBuilder(this.buildCommand(dir, base, url, quality, subtitle))
                    .redirectErrorStream(true).start();

            String lastError = null;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith(M_PROG)) {
                        this.applyProgress(task, line.substring(M_PROG.length()), dir, base);
                    } else if (line.startsWith(M_FILE)) {
                        finalName = line.substring(M_FILE.length()).trim();
                    } else if (line.startsWith(M_PP)) {
                        task.status = "merging";
                        task.percent = 99;
                        task.speed = "";
                        task.eta = "";
                    } else if (line.startsWith("ERROR")) {
                        lastError = line.trim();
                    }
                }
            }

            if (!proc.waitFor(this.downloadProperties.getTimeoutMinutes(), TimeUnit.MINUTES)) {
                proc.destroyForcibly();
                this.fail(task, "下载超时（>" + this.downloadProperties.getTimeoutMinutes() + " 分钟）", dir, base, finalName);
                return;
            }
            if (proc.exitValue() != 0) {
                this.fail(task, lastError != null ? lastError : "yt-dlp 退出码 " + proc.exitValue(), dir, base, finalName);
                return;
            }

            Path target = dir.resolve(Paths.get(finalName).getFileName()).normalize();
            if (!Files.exists(target)) {
                this.fail(task, "下载完成但文件未生成（" + finalName + "）", dir, base, finalName);
                return;
            }
            // 标题兜底：info.json 若在下载过程中未被读到，此处再读一次
            if (task.title == null || task.title.isEmpty()) {
                task.title = readInfoTitle(dir, base);
            }

            Video video = new Video();
            video.setTitle(task.title == null || task.title.isEmpty() ? "网络视频" : task.title);
            video.setUrl("/video/file/" + target.getFileName());
            video.setDescription("来源: " + url);
            video.setTags(tags != null && !tags.isEmpty() ? tags : "网络下载");
            video.setDuration(this.probeDuration(target));
            video.setCover(null);
            this.videoMapper.insert(video);

            // 标题已入库，清理 yt-dlp 中间产物
            deleteInfoJson(dir, base);
            task.videoId = String.valueOf(video.getId());
            task.percent = 100;
            task.speed = "";
            task.eta = "";
            task.status = "success";
            log.info("下载入库完成 task={} videoId={}", task.taskId, video.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.fail(task, "下载被中断", dir, base, finalName);
        } catch (Exception e) {
            this.fail(task, e.getMessage() == null ? e.toString() : e.getMessage(), dir, base, finalName);
        }
    }

    /**
     * 组装 yt-dlp 参数（借鉴 downvid-main electron/download/ytdlp.ts 的参数与进度模板设计）
     */
    private List<String> buildCommand(Path dir, String base, String url, String quality, boolean subtitle) {
        String fmt = switch (quality == null ? "best" : quality) {
            case "1080p" -> "bestvideo[height<=1080]+bestaudio/best[height<=1080]";
            case "720p" -> "bestvideo[height<=720]+bestaudio/best[height<=720]";
            case "480p" -> "bestvideo[height<=480]+bestaudio/best[height<=480]";
            case "audio" -> "bestaudio/best";
            default -> "bestvideo*+bestaudio/best";
        };
        List<String> cmd = new ArrayList<>();
        cmd.add(this.downloadProperties.getYtdlpPath());
        cmd.add("-f");
        cmd.add(fmt);
        cmd.add("--merge-output-format");
        cmd.add("mp4");
        cmd.add("-o");
        cmd.add(dir.resolve(base + ".%(ext)s").toString());
        cmd.add("--no-playlist");
        cmd.add("--playlist-items");
        cmd.add("1");
        // --print 隐含 --simulate，必须显式关闭才真正下载
        cmd.add("--no-simulate");
        cmd.add("--newline");
        cmd.add("--progress");
        // 结构化进度：下载字节 / 总量 / 速度 / 剩余秒 / 分片序号 / 分片总数
        cmd.add("--progress-template");
        cmd.add("download:" + M_PROG + "%(progress.downloaded_bytes)s|"
                + "%(progress.total_bytes,progress.total_bytes_estimate)s|"
                + "%(progress.speed)s|%(progress.eta)s|"
                + "%(progress.fragment_index)s|%(progress.fragment_count)s");
        cmd.add("--progress-template");
        cmd.add("postprocess:" + M_PP);
        // 标题从 info.json（固定 UTF-8）读取：yt-dlp 在中文 Windows 下 stdout 按 GBK 输出，
        // 用 --print 拿标题会得到乱码，故不在此处打印标题
        cmd.add("--write-info-json");
        cmd.add("--print");
        cmd.add("after_move:" + M_FILE + "%(filepath)s");
        // 分块请求：规避 CDN 对单条长连接限速（对不支持 Range 的服务器会自动退化为普通下载）
        cmd.add("--http-chunk-size");
        cmd.add("10M");
        // HLS/DASH 分片并发下载（对单文件直链无效）
        cmd.add("--concurrent-fragments");
        cmd.add("4");
        // --ffmpeg-location 需要目录而非文件，yt-dlp 会在其中同时查找 ffmpeg 与 ffprobe
        String ffmpegDir = this.downloadProperties.ffmpegDir();
        if (!ffmpegDir.isEmpty()) {
            cmd.add("--ffmpeg-location");
            cmd.add(ffmpegDir);
        }
        if (this.downloadProperties.getProxy() != null && !this.downloadProperties.getProxy().isEmpty()) {
            cmd.add("--proxy");
            cmd.add(this.downloadProperties.getProxy());
        }
        if (subtitle) {
            cmd.add("--write-subs");
            cmd.add("--write-auto-subs");
            cmd.add("--sub-langs");
            cmd.add("zh-CN,en");
            cmd.add("--sub-format");
            cmd.add("srt/best");
            cmd.add("--convert-subs");
            cmd.add("srt");
        }
        // 选项终止符：即使 URL 校验存在疏漏，也防止其被 yt-dlp 当作参数解析（选项注入）
        cmd.add("--");
        cmd.add(url);
        return cmd;
    }

    /** 解析一行结构化进度并更新任务快照 */
    private void applyProgress(DownloadTask task, String payload, Path dir, String base) {
        String[] f = payload.split("\\|", -1);
        double done = num(f, 0);
        double total = num(f, 1);
        double fragIdx = num(f, 4);
        double fragCount = num(f, 5);
        double percent = total > 0.0
                ? done / total * 100.0
                : (fragCount > 0.0 ? fragIdx / fragCount * 100.0 : 0.0);
        // 已进入后处理阶段（合并音视频）就不再回退进度条
        if (!"merging".equals(task.status)) {
            task.status = "downloading";
            task.percent = (int) Math.max(0.0, Math.min(99.0, percent));
        }
        task.speed = fmtSpeed(num(f, 2));
        task.eta = fmtEta(num(f, 3));
        // yt-dlp 在下载前就写好 info.json，首个进度行到达时即可取到正确标题
        if (task.title == null || task.title.isEmpty()) {
            task.title = readInfoTitle(dir, base);
        }
    }

    /** 标记任务失败并清理残留临时文件 */
    private void fail(DownloadTask task, String message, Path dir, String base, String keepName) {
        task.status = "failed";
        task.error = message == null ? "未知错误" : message;
        task.speed = "";
        task.eta = "";
        cleanupLeftovers(dir, base, keepName);
        deleteInfoJson(dir, base);
        log.warn("下载失败 task={}: {}", task.taskId, task.error);
    }

    /**
     * 读取 yt-dlp 写出的 info.json 中的标题
     * <p>
     * info.json 由 yt-dlp 以 UTF-8 固定编码写出，不受 Windows 区域设置影响，
     * 因此用它替代 stdout 取标题（stdout 在中文 Windows 下为 GBK，会乱码）。
     *
     * @param dir  下载目录
     * @param base 输出文件名基（UUID）
     * @return 标题；文件不存在或解析失败时返回空串
     */
    private static String readInfoTitle(Path dir, String base) {
        Path json = dir.resolve(base + ".info.json");
        if (!Files.exists(json)) {
            return "";
        }
        try {
            JsonNode node = JSON.readTree(Files.readAllBytes(json));
            String title = node.path("title").asText("").trim();
            return title;
        } catch (Exception e) {
            log.debug("读取 info.json 标题失败: {}", e.getMessage());
            return "";
        }
    }

    /** 删除 yt-dlp 中间产物 info.json */
    private static void deleteInfoJson(Path dir, String base) {
        try {
            Files.deleteIfExists(dir.resolve(base + ".info.json"));
        } catch (IOException e) {
            // 清理失败不影响任务结果
            log.debug("[download] delete info.json for {} failed: {}", base, e.getMessage());
        }
    }

    /** 删除 yt-dlp 中断留下的 .part/.ytdl 分片（保留最终成品文件） */
    private static void cleanupLeftovers(Path dir, String base, String keepName) {
        try (var stream = Files.list(dir)) {
            stream.filter(p -> {
                String name = p.getFileName().toString();
                return name.startsWith(base)
                        && !name.equals(keepName)
                        && LEFTOVER.matcher(name).matches();
            }).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    // 清理失败不影响任务结果
                    log.debug("[download] delete leftover file {} failed: {}", p.getFileName(), e.getMessage());
                }
            });
        } catch (IOException e) {
            // 目录不可读时跳过清理
            log.debug("[download] list directory for cleanup failed: {}", e.getMessage());
        }
    }

    /** ffprobe 读取时长（可选，失败返回 0） */
    private int probeDuration(Path file) {
        try {
            Process p = new ProcessBuilder(this.downloadProperties.getFfprobePath(),
                    "-v", "error", "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1", file.toString())
                    .redirectErrorStream(true).start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line.trim());
                }
            }
            p.waitFor(15, TimeUnit.SECONDS);
            return sb.length() == 0 ? 0 : (int) Double.parseDouble(sb.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /** yt-dlp 是否可用（结果缓存，仅探测一次） */
    private synchronized boolean ytdlpAvailable() {
        if (this.ytdlpOk == null) {
            this.ytdlpOk = canRun(this.downloadProperties.getYtdlpPath(), "--version");
            if (!this.ytdlpOk.booleanValue()) {
                log.warn("yt-dlp 不可用：{}", this.downloadProperties.getYtdlpPath());
            }
        }
        return this.ytdlpOk.booleanValue();
    }

    private static boolean canRun(String exe, String arg) {
        try {
            Process p = new ProcessBuilder(exe, arg).redirectErrorStream(true).start();
            try (var in = p.getInputStream()) {
                in.readAllBytes();
            }
            return p.waitFor(20, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static double num(String[] arr, int i) {
        if (i >= arr.length || arr[i] == null) {
            return 0.0;
        }
        String v = arr[i].trim();
        if (v.isEmpty() || "NA".equals(v)) {
            return 0.0;
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String fmtSpeed(double bps) {
        if (bps <= 0.0) {
            return "";
        }
        String[] units = {"B/s", "KB/s", "MB/s", "GB/s"};
        int i = 0;
        while (bps >= 1024.0 && i < units.length - 1) {
            bps /= 1024.0;
            i++;
        }
        return String.format(Locale.ROOT, "%.1f %s", bps, units[i]);
    }

    private static String fmtEta(double sec) {
        if (sec <= 0.0) {
            return "";
        }
        int s = (int) Math.round(sec);
        int h = s / 3600;
        int m = s % 3600 / 60;
        int ss = s % 60;
        return h > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, ss)
                : String.format(Locale.ROOT, "%02d:%02d", m, ss);
    }
}
