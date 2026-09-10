/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  org.apache.ibatis.annotations.Mapper
 */
package com.web3.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.resource.entity.Resource;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资源表 Mapper(resource-service 模块)。
 *
 * <p>{@code @Mapper} 注册为 MyBatis 映射器,继承
 * {@link com.baomidou.mybatisplus.core.mapper.BaseMapper} 获得
 * resource 表的基础 CRUD 能力。</p>
 */
@Mapper
public interface ResourceMapper
extends BaseMapper<Resource> {
}
