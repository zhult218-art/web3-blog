package com.web3.resource;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 类名：ResourceServiceApplication
 * 所属模块：resource-service（资源服务）
 * 职责：资源服务启动入口，启动 Spring Boot 容器并完成自动装配、服务注册发现与 Mapper 扫描。
 * 关键注解：@SpringBootApplication 扫描 com.web3.resource 与 com.web3.common；
 *           @EnableDiscoveryClient 注册到服务发现；@MapperScan 扫描 com.web3.resource.mapper。
 */
@SpringBootApplication(scanBasePackages = {"com.web3.resource", "com.web3.common"})
@EnableDiscoveryClient
@MapperScan("com.web3.resource.mapper")
public class ResourceServiceApplication {
    /**
     * 服务启动入口：通过 SpringApplication 启动资源服务容器。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ResourceServiceApplication.class, args);
    }
}
