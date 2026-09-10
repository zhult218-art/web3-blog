package com.web3.gateway;

import com.web3.common.core.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;

/**
 * 类名：GatewayApplication
 * 所属模块：gateway（API 网关服务）
 * 职责：网关服务的启动入口类。
 * 关键注解：@Configuration/@EnableAutoConfiguration 为 Spring Boot 自动配置；@EnableDiscoveryClient 注册到 Nacos 服务注册中心；
 *        @EnableWebFluxSecurity 启用响应式安全；@ComponentScan 扫描 gateway 与 common 包（排除 common-core 的 GlobalExceptionHandler，因网关统一走响应式错误处理）。
 * 说明：网关负责请求路由转发、统一鉴权（JWT）、AI 接口代理。
 */
@Configuration
@EnableAutoConfiguration
@EnableDiscoveryClient
@EnableWebFluxSecurity
@ComponentScan(basePackages = {"com.web3.gateway", "com.web3.common"},
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GlobalExceptionHandler.class))
public class GatewayApplication {
    /**
     * 程序入口：启动 Spring Cloud Gateway 网关。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}