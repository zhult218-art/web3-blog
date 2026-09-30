package com.web3.media.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web3.common.core.PageResult;
import com.web3.media.config.MediaUploadProperties;
import com.web3.media.entity.Video;
import com.web3.media.mapper.VideoMapper;
import com.web3.media.security.FileSecurityChecker;
import com.web3.media.vo.VideoVO;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * VideoService —— 视频业务服务
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：实现视频资源的核心业务逻辑，包括分页查询、详情查询、上传文件落盘并建记录、
 * 本地文件安全读取、创建/更新/删除（管理员操作），以及实体到视图对象（VideoVO）的转换。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Service：声明为 Spring 业务组件</li>
 *   <li>继承 ServiceImpl&lt;VideoMapper, Video&gt;：获得 MyBatis-Plus 内置 CRUD 能力</li>
 * </ul>
 */
@Service
public class VideoService
extends ServiceImpl<VideoMapper, Video> {
    /** 上传目录与大小限制配置 */
    private final MediaUploadProperties uploadProperties;

    public VideoService(MediaUploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    /**
     * 分页查询视频列表，支持标题/描述关键字筛选，按创建时间倒序
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param keyword 标题/描述关键字，可为空
     * @return 视频视图对象的分页结果
     */
    public PageResult<VideoVO> page(int page, int size, String keyword) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Video::getTitle, (Object)keyword).or().like(Video::getDescription, (Object)keyword);
        }
        wrapper.orderByDesc(Video::getCreatedAt);
        Page<Video> mpPage = new Page<Video>((long)page, (long)size);
        Page<Video> result = this.baseMapper.selectPage(mpPage, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    /**
     * 查询视频详情，不存在时抛异常
     *
     * @param id 视频 ID
     * @return 视频视图对象
     */
    public VideoVO getDetail(Long id) {
        Video video = (Video)this.baseMapper.selectById(id);
        if (video == null) {
            throw new RuntimeException("\u89c6\u9891\u4e0d\u5b58\u5728");
        }
        return this.toVO(video);
    }

    /**
     * 创建视频（管理员操作）
     *
     * @param video 视频实体
     * @return 创建成功后的视频视图对象
     */
    public VideoVO create(Video video) {
        this.baseMapper.insert(video);
        return this.toVO(video);
    }

    /**
     * 上传视频文件到本地磁盘并创建视频记录
     * 文件名校验、大小校验后以 UUID.ext 命名写入上传目录
     *
     * @param file        上传文件
     * @param title       标题（缺省用原文件名）
     * @param description 描述（缺省用文件 MIME 类型）
     * @param tags        标签
     * @param cover       封面地址
     * @return 上传成功后的视频视图对象
     * @throws IllegalArgumentException 文件类型/大小不合法
     * @throws IOException              文件写入失败
     */
    public VideoVO upload(MultipartFile file, String title, String description, String tags, String cover) throws IOException {
        FileSecurityChecker.validate(file, this.uploadProperties.getMaxSize());
        String ext = FileSecurityChecker.extractExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString() + "." + ext;
        Path dir = Paths.get(this.uploadProperties.getDir(), new String[0]).toAbsolutePath().normalize();
        if (!Files.exists(dir, new LinkOption[0])) {
            Files.createDirectories(dir, new FileAttribute[0]);
        }
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw new IllegalArgumentException("Invalid filename");
        }
        file.transferTo(target.toFile());

        Video video = new Video();
        video.setTitle(title != null && !title.isEmpty() ? title : file.getOriginalFilename());
        video.setUrl("/video/file/" + filename);
        video.setDescription(description != null && !description.isEmpty() ? description : file.getContentType());
        video.setTags(tags);
        video.setCover(cover);
        video.setDuration(0);
        this.baseMapper.insert(video);
        return this.toVO(video);
    }

    /**
     * 通过 yt-dlp 从 URL 下载视频（YouTube/B站/抖音等 1800+ 平台）
     * 下载到上传目录后建 Video 记录，直接出现在视频列表中
     *
     * @param url       视频 URL
     * @param quality   清晰度 best/1080p/720p/480p
     * @param subtitle  是否下载字幕
     * @param tags      标签
     * @return 视频视图对象
     */
    public VideoVO downloadFromUrl(String url, String quality, boolean subtitle, String tags) throws IOException, InterruptedException {
        String ext = "mp4";
        String filename = UUID.randomUUID().toString() + "." + ext;
        Path dir = Paths.get(this.uploadProperties.getDir(), new String[0]).toAbsolutePath().normalize();
        if (!Files.exists(dir, new LinkOption[0])) {
            Files.createDirectories(dir, new FileAttribute[0]);
        }
        Path target = dir.resolve(filename).normalize();

        // 构造 yt-dlp 命令
        String fmt = switch (quality == null ? "best" : quality) {
            case "1080p" -> "bestvideo[height<=1080]+bestaudio/best[height<=1080]";
            case "720p" -> "bestvideo[height<=720]+bestaudio/best[height<=720]";
            case "480p" -> "bestvideo[height<=480]+bestaudio/best[height<=480]";
            default -> "bestvideo*+bestaudio/best";
        };
        List<String> cmd = new ArrayList<>();
        cmd.add("yt-dlp");
        cmd.add("-f"); cmd.add(fmt);
        cmd.add("--merge-output-format"); cmd.add("mp4");
        cmd.add("-o"); cmd.add(target.toString());
        cmd.add("--no-playlist");
        cmd.add("--sponsorblock-remove"); cmd.add("all");
        if (subtitle) {
            cmd.add("--write-subs");
            cmd.add("--write-auto-subs");
            cmd.add("--sub-langs"); cmd.add("zh-CN,en");
            cmd.add("--sub-format"); cmd.add("srt/best");
            cmd.add("--convert-subs"); cmd.add("srt");
        }
        cmd.add(url);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        String title = "网络视频";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                // yt-dlp 会输出 [Metadata] 标题行，尝试提取
                if (line.contains("Merging formats") || line.contains("Destination")) {
                    // 忽略
                }
            }
        }
        boolean finished = proc.waitFor(30, TimeUnit.MINUTES);
        if (!finished) {
            proc.destroyForcibly();
            throw new IOException("yt-dlp 下载超时（>30min）");
        }
        if (proc.exitValue() != 0) {
            throw new IOException("yt-dlp 下载失败 (exit " + proc.exitValue() + ")");
        }
        if (!Files.exists(target, new LinkOption[0])) {
            throw new IOException("下载完成但文件未生成");
        }

        // 用 ffprobe 探测时长（可选，失败则 0）
        int duration = 0;
        try {
            ProcessBuilder fb = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries",
                    "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", target.toString());
            Process fp = fb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(fp.getInputStream()))) {
                String d = br.readLine();
                if (d != null) duration = (int) Double.parseDouble(d.trim());
            }
            fp.waitFor(10, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        // 尝试从 yt-dlp 获取标题（重新探测仅取标题）
        try {
            ProcessBuilder tb = new ProcessBuilder("yt-dlp", "--print", "%(title)s", "--skip-download", url);
            Process tp = tb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(tp.getInputStream()))) {
                String t = br.readLine();
                if (t != null && !t.isEmpty()) title = t.trim();
            }
            tp.waitFor(30, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        Video video = new Video();
        video.setTitle(title);
        video.setUrl("/video/file/" + filename);
        video.setDescription("来源: " + url);
        video.setTags(tags != null ? tags : "网络下载");
        video.setCover(null);
        video.setDuration(duration);
        this.baseMapper.insert(video);
        return this.toVO(video);
    }

    /**
     * 安全解析视频文件路径（防目录穿越）
     * 仅允许 "UUID格式.扩展名" 的文件名，并校验路径必须位于上传目录内
     *
     * @param filename 文件名
     * @return 文件在磁盘上的绝对路径；文件名不合法或文件不存在时返回 null
     */
    public Path getFile(String filename) {
        if (!filename.matches("[a-f0-9-]{36}\\.[a-z0-9]{2,5}")) {
            return null;
        }
        Path dir = Paths.get(this.uploadProperties.getDir(), new String[0]).toAbsolutePath().normalize();
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir) || !Files.exists(target, new LinkOption[0])) {
            return null;
        }
        return target;
    }

    /**
     * 更新视频（管理员操作），不存在时抛异常
     *
     * @param id    视频 ID
     * @param video 视频实体（含需更新的字段）
     * @return 更新后的视频视图对象
     */
    public VideoVO update(Long id, Video video) {
        Video exist = (Video)this.baseMapper.selectById(id);
        if (exist == null) {
            throw new RuntimeException("\u89c6\u9891\u4e0d\u5b58\u5728");
        }
        video.setId(id);
        this.baseMapper.updateById(video);
        return this.toVO((Video)this.baseMapper.selectById(id));
    }

    /**
     * 删除视频（管理员操作）
     *
     * @param id 视频 ID
     */
    public void delete(Long id) {
        this.baseMapper.deleteById(id);
    }

    /**
     * 实体转视图对象（内部工具方法）
     *
     * @param video 视频实体
     * @return 视频视图对象
     */
    private VideoVO toVO(Video video) {
        VideoVO vo = new VideoVO();
        BeanUtils.copyProperties((Object)video, (Object)vo);
        return vo;
    }
}
