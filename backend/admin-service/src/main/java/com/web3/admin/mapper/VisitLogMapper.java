package com.web3.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.admin.entity.VisitLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 类名：VisitLogMapper
 * 所属模块：admin-service（管理后台服务）
 * 职责：访问日志表（visit_log）的 MyBatis-Plus Mapper 接口，提供访问记录的写入、分页查询与统计能力。
 * 关键注解：@Mapper 声明为 MyBatis 映射接口，由启动类 @MapperScan("com.web3.admin.mapper") 扫描注册。
 */
@Mapper
public interface VisitLogMapper extends BaseMapper<VisitLog> {
}