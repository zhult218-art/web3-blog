package com.web3.media.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MediaDownloadProperties —— 视频 URL 下载（yt-dlp）配置
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 绑定前缀 media.download.*。三个可执行文件路径默认取环境变量
 * YTDLP_PATH / FFMPEG_PATH / FFPROBE_PATH（见根 .env），未配置时回退到 PATH 查找。
 */
@Component
@ConfigurationProperties(prefix = "media.download")
public class MediaDownloadProperties {
    /** yt-dlp 可执行文件路径（绝对路径或 PATH 中的命令名） */
    private String ytdlpPath = "yt-dlp";
    /** ffmpeg 可执行文件路径 */
    private String ffmpegPath = "ffmpeg";
    /** ffprobe 可执行文件路径 */
    private String ffprobePath = "ffprobe";
    /** 下载代理，如 http://127.0.0.1:7890；留空则不使用代理 */
    private String proxy = "";
    /** 单个任务最长下载时间（分钟） */
    private int timeoutMinutes = 30;
    /** 同时进行的下载任务数 */
    private int concurrency = 2;

    public String getYtdlpPath() {
        return this.ytdlpPath;
    }

    public void setYtdlpPath(String ytdlpPath) {
        this.ytdlpPath = ytdlpPath;
    }

    public String getFfmpegPath() {
        return this.ffmpegPath;
    }

    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public String getFfprobePath() {
        return this.ffprobePath;
    }

    public void setFfprobePath(String ffprobePath) {
        this.ffprobePath = ffprobePath;
    }

    public String getProxy() {
        return this.proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public int getTimeoutMinutes() {
        return this.timeoutMinutes;
    }

    public void setTimeoutMinutes(int timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public int getConcurrency() {
        return this.concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    /**
     * yt-dlp 的 --ffmpeg-location 需要的是「目录」而非文件路径：
     * 传入目录后 yt-dlp 会在其中同时查找 ffmpeg 与 ffprobe。
     * 方法名刻意不用 get 前缀，避免被当成 ConfigurationProperties 的只读属性。
     *
     * @return ffmpeg 所在目录；未配置路径时返回空串（表示交给 PATH）
     */
    public String ffmpegDir() {
        Path parent = Paths.get(this.ffmpegPath).getParent();
        return parent == null ? "" : parent.toString();
    }
}
