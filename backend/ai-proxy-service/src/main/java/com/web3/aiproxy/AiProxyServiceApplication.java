package com.web3.aiproxy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 类名：AiProxyServiceApplication
 * 所属模块：ai-proxy-service（API Token 中转站，独立微服务）
 * 职责：OpenAI 兼容中转引擎 —— 令牌鉴权、额度计费、渠道路由、上游转发(含 SSE 流式)、
 *       请求日志落库；同时提供管理端渠道/令牌/模型倍率 CRUD 与智能用量统计看板接口。
 * 关键注解：@SpringBootApplication 扫描 com.web3.aiproxy 与 com.web3.common；
 *          @MapperScan 扫描本服务 Mapper；@EnableAsync 异步写日志；@EnableDiscoveryClient 注册 Nacos。
 */
@SpringBootApplication(scanBasePackages = {"com.web3.aiproxy", "com.web3.common"})
@MapperScan("com.web3.aiproxy.mapper")
@EnableAsync
@EnableDiscoveryClient
public class AiProxyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiProxyServiceApplication.class, args);
    }
}
