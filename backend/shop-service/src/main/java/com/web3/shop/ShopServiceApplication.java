package com.web3.shop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * ShopServiceApplication —— 商城服务启动类
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：商城微服务的入口类，通过 main 方法启动 Spring Boot 应用。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>{@code @SpringBootApplication(scanBasePackages={"com.web3.shop","com.web3.common"})}：启动注解，扫描本模块及通用模块组件</li>
 *   <li>{@code @EnableDiscoveryClient}：注册到 Nacos 服务注册中心，供网关发现调用</li>
 *   <li>{@code @MapperScan("com.web3.shop.mapper")}：扫描本模块 MyBatis-Plus Mapper 接口</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = {"com.web3.shop", "com.web3.common"})
@EnableDiscoveryClient
@MapperScan("com.web3.shop.mapper")
public class ShopServiceApplication {
    /** 应用启动入口 */
    public static void main(String[] args) {
        SpringApplication.run(ShopServiceApplication.class, args);
    }
}
