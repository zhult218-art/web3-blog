/*
 * MediaUploadProperties - upload configuration
 */
package com.web3.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MediaUploadProperties —— 上传配置属性类
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：绑定 application.yml 中 media.upload.* 配置项，
 * 提供上传文件保存目录与大小上限，供 VideoService 及安全校验使用。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Component：声明为 Spring 组件，注入到容器中</li>
 *   <li>@ConfigurationProperties(prefix="media.upload")：将配置文件前缀 media.upload 的键值绑定到本类属性</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix="media.upload")
public class MediaUploadProperties {
    /** 上传文件保存目录（相对路径，相对应用运行目录） */
    private String dir = "runtime/uploads";
    /** 上传文件大小上限（字节），默认 500MB */
    private long maxSize = 524288000L;

    public String getDir() {
        return this.dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public long getMaxSize() {
        return this.maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }
}
