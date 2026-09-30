package com.web3.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.media.entity.Photo;
import org.apache.ibatis.annotations.Mapper;

/**
 * PhotoMapper —— 相册照片表数据访问接口
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：操作 photo 相册照片表，继承 BaseMapper 获得通用 CRUD。
 */
@Mapper
public interface PhotoMapper extends BaseMapper<Photo> {
}
