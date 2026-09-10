package com.web3.aiproxy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.aiproxy.entity.ProxyToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 令牌 Mapper —— 计费热路径单语句原子化（防死锁核心）：
 * 扣减 + 用量计数 + 最近使用时间合并为一条 UPDATE，行级锁一次持有即释放。
 */
@Mapper
public interface ProxyTokenMapper extends BaseMapper<ProxyToken> {

    /**
     * 原子扣费：仅当令牌启用且（无限额 或 剩余充足）时执行
     * used_quota += amount、request_count += 1、last_used_at = NOW()。
     * 返回 0 = 余额不足或状态异常。
     */
    @Update("UPDATE proxy_token SET used_quota = used_quota + #{amount}, " +
            "request_count = request_count + 1, last_used_at = NOW() " +
            "WHERE id = #{tokenId} AND status = 1 " +
            "AND (unlimited_quota = 1 OR quota >= used_quota + #{amount})")
    int deductQuota(@Param("tokenId") Long tokenId, @Param("amount") long amount);

    /** 管理员充值/兑换码核销：直接增加总额度 */
    @Update("UPDATE proxy_token SET quota = COALESCE(quota, 0) + #{amount} WHERE id = #{tokenId}")
    int addQuota(@Param("tokenId") Long tokenId, @Param("amount") long amount);
}
