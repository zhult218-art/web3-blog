/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.cloud.gateway.route.RouteLocator
 *  org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package com.web3.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 类名：GatewayRouteConfig
 * 所属模块：gateway（API 网关服务）
 * 职责：网关路由配置类，负责定义请求转发路由。
 * 关键注解：@Configuration 声明为 Spring 配置类。
 * 说明：当前仅注册一个空路由构建器 Bean，实际服务路由由 Nacos 服务发现（lb://service-name 动态路由）或配置文件定义。
 */
@Configuration
public class GatewayRouteConfig {
    /**
     * 构建网关 RouteLocator，此处未注册具体路由（空构建）。
     *
     * @param builder Spring Cloud Gateway 路由构建器
     * @return 路由定位器 RouteLocator
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes().build();
    }
}
