package com.web3.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.media.entity.UserMusicLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * UserMusicLikeMapper —— 用户音乐收藏关系表 Mapper
 */
@Mapper
public interface UserMusicLikeMapper extends BaseMapper<UserMusicLike> {
}
