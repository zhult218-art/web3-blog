/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  org.apache.ibatis.annotations.Mapper
 */
package com.web3.jarvis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.jarvis.entity.VoiceCommand;
import org.apache.ibatis.annotations.Mapper;

/**
 * 语音指令表 Mapper(jarvis-service 模块)。
 *
 * <p>{@code @Mapper} 注册为 MyBatis 映射器,继承
 * {@link com.baomidou.mybatisplus.core.mapper.BaseMapper} 获得
 * voice_command 表的基础 CRUD 能力。</p>
 */
@Mapper
public interface VoiceCommandMapper
extends BaseMapper<VoiceCommand> {
}
