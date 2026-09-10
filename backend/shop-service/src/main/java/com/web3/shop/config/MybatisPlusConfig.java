package com.web3.shop.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MybatisPlusConfig —— MyBatis-Plus 配置类
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：注册 MyBatis-Plus 拦截器，启用 MySQL 数据库的分页插件，
 * 并限制单次分页查询的最大条数（200），防止超大分页请求拖垮数据库。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Configuration：声明为 Spring 配置类</li>
 *   <li>@Bean：将分页拦截器注册为 Spring Bean</li>
 * </ul>
 */
@Configuration
public class MybatisPlusConfig {
    /**
     * 注册 MyBatis-Plus 分页拦截器
     *
     * @return 配置完成的分页拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(200L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}