/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.mybatis.spring.annotation.MapperScan
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 *  org.springframework.scheduling.annotation.EnableScheduling
 */
package com.web3.jarvis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Jarvis 语音助手服务启动类(jarvis-service 模块)。
 *
 * <p>作为 Spring Boot 应用入口,负责启动 Jarvis 服务并完成自动装配。注解说明:</p>
 * <ul>
 *   <li>{@code @SpringBootApplication}:声明为 Spring Boot 应用,扫描本服务
 *       com.web3.jarvis 与公共模块 com.web3.common 下的组件;</li>
 *   <li>{@code @EnableDiscoveryClient}:开启服务注册发现,供网关与其他微服务调用;</li>
 *   <li>{@code @MapperScan("com.web3.jarvis.mapper")}:扫描 MyBatis-Plus Mapper 接口;</li>
 *   <li>{@code @EnableScheduling}:启用 Spring 定时任务能力。</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages={"com.web3.jarvis", "com.web3.common"})
@EnableDiscoveryClient
@MapperScan(value={"com.web3.jarvis.mapper"})
@EnableScheduling
public class JarvisServiceApplication {
    /**
     * 服务启动入口,通过 SpringApplication 启动 Jarvis 语音助手服务容器。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(JarvisServiceApplication.class, (String[])args);
    }
}
