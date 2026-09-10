/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.mybatis.spring.annotation.MapperScan
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 */
package com.web3.forum;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages={"com.web3.forum", "com.web3.common", "com.web3.user"})
@EnableDiscoveryClient
@MapperScan(value={"com.web3.forum.mapper"})
/**
 * ForumServiceApplication —— 论坛服务启动类
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：论坛微服务的入口类，通过 main 方法启动 Spring Boot 应用。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@SpringBootApplication(scanBasePackages={"com.web3.forum","com.web3.common","com.web3.user"})：启动注解，扫描本模块、通用模块及用户模块的组件</li>
 *   <li>@EnableDiscoveryClient：注册到 Nacos 服务注册中心，供网关发现调用</li>
 *   <li>@MapperScan("com.web3.forum.mapper")：扫描本模块 MyBatis-Plus Mapper 接口</li>
 * </ul>
 */
public class ForumServiceApplication {
    /** 应用启动入口 */
    public static void main(String[] args) {
        SpringApplication.run(ForumServiceApplication.class, (String[])args);
    }
}
