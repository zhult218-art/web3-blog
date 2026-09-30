package com.web3.forum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.forum.entity.LikeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * LikeRecordMapper —— 点赞记录表数据访问接口
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：操作 like_record 点赞记录表，继承 BaseMapper 获得通用 CRUD，
 * 用于查询某用户是否已点赞某目标（如帖子）以及点赞记录的新增/删除。
 *
 * <p>关键注解作用：{@code @Mapper} 声明为 MyBatis Mapper 接口，由框架扫描并生成实现。</p>
 */
@Mapper
public interface LikeRecordMapper extends BaseMapper<LikeRecord> {
}
