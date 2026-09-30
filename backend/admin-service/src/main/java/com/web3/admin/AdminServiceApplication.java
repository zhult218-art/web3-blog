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
 * 关键注解：{@code @SpringBootApplication} 扫描 com.web3.admin 与 com.web3.common；
 *           {@code @EnableDiscoveryClient} 注册到 Nacos；
 *           {@code @MapperScan} 扫描 com.web3.admin.mapper；
 *           {@code @EnableRedisRepositories} 启用 Redis 数据仓库支持。
 */
@SpringBootApplication(scanBasePackages = {"com.web3.admin", "com.web3.common"})
@EnableDiscoveryClient
@MapperScan("com.web3.admin.mapper")
@EnableRedisRepositories
public class AdminServiceApplication {
    /**
     * 程序入口：启动管理后台服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
