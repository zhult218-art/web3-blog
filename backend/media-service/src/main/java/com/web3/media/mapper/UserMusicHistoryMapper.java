package com.web3.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.media.entity.UserMusicHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * UserMusicHistoryMapper —— 用户音乐播放历史表 Mapper
 */
@Mapper
public interface UserMusicHistoryMapper extends BaseMapper<UserMusicHistory> {
}
