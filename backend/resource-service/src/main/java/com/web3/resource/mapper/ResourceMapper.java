package com.web3.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.resource.entity.Resource;
import org.apache.ibatis.annotations.Mapper;

/**
 * 类名：ResourceMapper
 * 所属模块：resource-service（资源服务）
 * 职责：资源表 resource 的 MyBatis-Plus Mapper 接口，提供基础 CRUD 能力。
 * 关键注解：@Mapper 声明为 MyBatis 映射接口，由启动类 @MapperScan 扫描注册。
 */
@Mapper
public interface ResourceMapper extends BaseMapper<Resource> {
}
