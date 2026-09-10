package com.web3.aiproxy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.aiproxy.entity.ProxyRedeemCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 兑换码 Mapper —— 核销为条件原子 UPDATE（status=0 且未过期才可核销），
 * 并发抢码只有一个成功。
 */
@Mapper
public interface ProxyRedeemCodeMapper extends BaseMapper<ProxyRedeemCode> {

    /** 原子核销：返回 0 = 已用/作废/过期/不存在 */
    @Update("UPDATE proxy_redeem_code SET status = 1, used_by_token_id = #{tokenId}, used_at = NOW() " +
            "WHERE code_hash = #{codeHash} AND status = 0 " +
            "AND (expired_at IS NULL OR expired_at > NOW())")
    int redeem(@Param("codeHash") String codeHash, @Param("tokenId") Long tokenId);

    /** 批次作废 */
    @Update("UPDATE proxy_redeem_code SET status = 2 WHERE batch_no = #{batchNo} AND status = 0")
    int voidBatch(@Param("batchNo") String batchNo);
}
