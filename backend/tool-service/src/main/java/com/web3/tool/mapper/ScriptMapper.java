/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  org.apache.ibatis.annotations.Mapper
 */
package com.web3.tool.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.tool.entity.Script;
import org.apache.ibatis.annotations.Mapper;

/**
 * 脚本表 Mapper(tool-service 模块)。
 *
 * <p>{@code @Mapper} 注册为 MyBatis 映射器,继承
 * {@link com.baomidou.mybatisplus.core.mapper.BaseMapper} 获得
 * script 表的基础 CRUD 能力。</p>
 */
@Mapper
public interface ScriptMapper
extends BaseMapper<Script> {
}
