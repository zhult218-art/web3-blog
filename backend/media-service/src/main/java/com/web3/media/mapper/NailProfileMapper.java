package com.web3.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.media.entity.NailProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * NailProfileMapper —— 美甲店主资料表数据访问接口
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：操作 nail_profile 美甲店主资料表，继承 BaseMapper 获得通用 CRUD。
 */
@Mapper
public interface NailProfileMapper extends BaseMapper<NailProfile> {
}
