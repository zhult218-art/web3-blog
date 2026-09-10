/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  org.apache.ibatis.annotations.Mapper
 */
package com.web3.quant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.quant.entity.QuantLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 量化日志表 Mapper(quant-service 模块)。
 *
 * <p>{@code @Mapper} 注册为 MyBatis 映射器,继承
 * {@link com.baomidou.mybatisplus.core.mapper.BaseMapper} 获得
 * quant_log 表的基础 CRUD 能力。</p>
 */
@Mapper
public interface QuantLogMapper
extends BaseMapper<QuantLog> {
}
