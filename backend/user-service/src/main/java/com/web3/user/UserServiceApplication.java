package com.web3.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 类名：UserServiceApplication
 * 所属模块：user-service（用户服务）
 * 职责：用户服务的启动入口类。
 * 关键注解：{@code @SpringBootApplication} 扫描 com.web3.user 与 com.web3.common（加载通用 JWT/异常处理等组件）；
 *           {@code @EnableDiscoveryClient} 注册到 Nacos；
 *           {@code @MapperScan} 扫描 com.web3.user.mapper 下的 MyBatis-Plus Mapper 接口。
 */
@SpringBootApplication(scanBasePackages = {"com.web3.user", "com.web3.common"})
@EnableDiscoveryClient
@MapperScan("com.web3.user.mapper")
public class UserServiceApplication {
    /**
     * 程序入口：启动用户服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
