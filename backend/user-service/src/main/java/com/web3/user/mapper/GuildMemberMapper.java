package com.web3.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.user.entity.GuildMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GuildMemberMapper extends BaseMapper<GuildMember> {
}
