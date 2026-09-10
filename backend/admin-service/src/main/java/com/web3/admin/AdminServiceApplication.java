/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.mybatis.spring.annotation.MapperScan
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 *  org.springframework.data.redis.repository.configuration.EnableRedisRepositories
 */
package com.web3.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * 类名：AdminServiceApplication
 * 所属模块：admin-service（管理后台服务）
 * 职责：管理后台服务的启动入口类。
 * 关键注解：@SpringBootApplication 扫描 com.web3.admin 与 com.web3.common；@EnableDiscoveryClient 注册到 Nacos；
 *        @MapperScan 扫描 com.web3.admin.mapper；@EnableRedisRepositories 启用 Redis 数据仓库支持。
 */
@SpringBootApplication(scanBasePackages={"com.web3.admin", "com.web3.common"})
@EnableDiscoveryClient
@MapperScan(value={"com.web3.admin.mapper"})
@EnableRedisRepositories
public class AdminServiceApplication {
    /**
     * 程序入口：启动管理后台服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, (String[])args);
    }
}
