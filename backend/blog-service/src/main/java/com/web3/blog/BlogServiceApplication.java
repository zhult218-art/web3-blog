/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.mybatis.spring.annotation.MapperScan
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 */
package com.web3.blog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages={"com.web3"})
@EnableDiscoveryClient
@MapperScan(value={"com.web3.blog.mapper"})
/**
 * BlogServiceApplication —— 博客服务启动类
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 职责：博客微服务的入口类，通过 main 方法启动 Spring Boot 应用。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@SpringBootApplication(scanBasePackages="com.web3")：Spring Boot 启动注解，扫描 com.web3 包下的所有组件（含 common 模块）</li>
 *   <li>@EnableDiscoveryClient：注册到 Nacos 服务注册中心，供网关与其他服务发现调用</li>
 *   <li>@MapperScan("com.web3.blog.mapper")：扫描本模块 MyBatis-Plus Mapper 接口并生成代理实现</li>
 * </ul>
 */
public class BlogServiceApplication {
    /** 应用启动入口 */
    public static void main(String[] args) {
        SpringApplication.run(BlogServiceApplication.class, (String[])args);
    }
}
