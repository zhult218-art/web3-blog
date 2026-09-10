package com.web3.tool.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.tool.entity.SiteShare;
import org.apache.ibatis.annotations.Mapper;

/**
 * SiteShareMapper —— 分享网站表 Mapper
 * <p>
 * 所属模块：tool-service（工具模块）。
 * <p>
 * @Mapper 注册为 MyBatis 映射器，继承 BaseMapper 获得 site_share 表的基础 CRUD 能力。
 */
@Mapper
public interface SiteShareMapper extends BaseMapper<SiteShare> {
}