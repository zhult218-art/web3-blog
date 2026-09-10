/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  org.apache.ibatis.annotations.Mapper
 */
package com.web3.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.admin.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 类名：OperationLogMapper
 * 所属模块：admin-service（管理后台服务）
 * 职责：操作日志表（admin_operation_log）的 MyBatis-Plus Mapper 接口，提供操作日志的写入与查询能力。
 * 关键注解：@Mapper 声明为 MyBatis 映射接口，由启动类 @MapperScan("com.web3.admin.mapper") 扫描注册。
 */
@Mapper
public interface OperationLogMapper
extends BaseMapper<OperationLog> {
}
