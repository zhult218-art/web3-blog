/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.boot.context.properties.ConfigurationProperties
 *  org.springframework.stereotype.Component
 */
package com.web3.resource.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 资源服务配置属性类(resource-service 模块)。
 *
 * <p>{@code @Component} 声明为 Spring 组件,{@code @ConfigurationProperties}
 * 将配置文件(application.yml)中 {@code resource.*} 前缀的配置项绑定到本类字段,
 * 例如 {@code resource.upload-dir} 对应上传目录。</p>
 */
@Component
@ConfigurationProperties(prefix="resource")
public class ResourceProperties {
    /**
     * 文件上传存储目录(相对路径,默认 runtime/uploads)
     */
    private String uploadDir = "runtime/uploads";

    /**
     * 获取上传目录。
     *
     * @return 上传目录路径
     */
    public String getUploadDir() {
        return this.uploadDir;
    }

    /**
     * 设置上传目录(由配置绑定调用)。
     *
     * @param uploadDir 上传目录路径
     */
    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
}
