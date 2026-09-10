/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.mybatis.spring.annotation.MapperScan
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 */
package com.web3.software;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 软件下载服务启动类(software-service 模块)。
 *
 * <p>作为 Spring Boot 应用入口,负责启动软件下载服务并完成自动装配。注解说明:</p>
 * <ul>
 *   <li>{@code @SpringBootApplication}:声明为 Spring Boot 应用,扫描本服务
 *       com.web3.software 与公共模块 com.web3.common 下的组件;</li>
 *   <li>{@code @EnableDiscoveryClient}:开启服务注册发现,供网关与其他微服务调用;</li>
 *   <li>{@code @MapperScan("com.web3.software.mapper")}:扫描 MyBatis-Plus Mapper 接口。</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages={"com.web3.software", "com.web3.common"})
@EnableDiscoveryClient
@MapperScan(value={"com.web3.software.mapper"})
public class SoftwareServiceApplication {
    /**
     * 服务启动入口,通过 SpringApplication 启动软件下载服务容器。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SoftwareServiceApplication.class, (String[])args);
    }
}
