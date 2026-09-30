package com.web3.software.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.software.entity.Software;
import org.apache.ibatis.annotations.Mapper;

/**
 * 类名：SoftwareMapper
 * 所属模块：software-service（软件下载服务）
 * 职责：软件表 software 的 MyBatis-Plus Mapper 接口，提供基础 CRUD 能力。
 * 关键注解：@Mapper 声明为 MyBatis 映射接口，由启动类 @MapperScan 扫描注册。
 */
@Mapper
public interface SoftwareMapper extends BaseMapper<Software> {
}
