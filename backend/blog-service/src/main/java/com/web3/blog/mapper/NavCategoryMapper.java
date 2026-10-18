package com.web3.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.blog.entity.NavCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * NavCategoryMapper —— 导航分类表数据访问接口
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 继承 MyBatis-Plus BaseMapper 获得通用 CRUD。
 */
@Mapper
public interface NavCategoryMapper
extends BaseMapper<NavCategory> {
}
