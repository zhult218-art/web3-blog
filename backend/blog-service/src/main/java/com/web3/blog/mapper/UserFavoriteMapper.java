package com.web3.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.blog.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {
}
