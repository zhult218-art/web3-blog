package com.web3.resource.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 类名：ResourceProperties
 * 所属模块：resource-service（资源服务）
 * 职责：绑定 application.yml 中 resource.* 前缀的配置项到本类字段（如 resource.upload-dir 上传目录）。
 * 关键注解：@Component 注册为 Spring 组件；@ConfigurationProperties(prefix = "resource") 绑定配置；
 *           @Data 自动生成 getter/setter。
 */
@Data
@Component
@ConfigurationProperties(prefix = "resource")
public class ResourceProperties {
    /** 文件上传存储目录（相对路径，默认 runtime/uploads） */
    private String uploadDir = "runtime/uploads";
    /** 单文件上传大小上限（字节，默认 200MB） */
    private long maxUploadSize = 200L * 1024 * 1024;
}
