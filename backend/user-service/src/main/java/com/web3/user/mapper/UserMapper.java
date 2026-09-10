/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  org.apache.ibatis.annotations.Mapper
 */
package com.web3.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 类名：UserMapper
 * 所属模块：user-service（用户服务）
 * 职责：用户表（user）的 MyBatis-Plus Mapper 接口，继承 BaseMapper 获得用户表 CRUD 与分页能力。
 * 关键注解：@Mapper 声明为 MyBatis 映射接口，由启动类 @MapperScan("com.web3.user.mapper") 扫描注册。
 */
@Mapper
public interface UserMapper
extends BaseMapper<User> {
}
