package com.web3.aiproxy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.aiproxy.entity.ProxyChannel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 渠道 Mapper —— 成功/失败统计均为单条原子 UPDATE（行内自算，无事务交叉）；
 * avg_latency 指数平滑：avg = avg*0.7 + latency*0.3。
 */
@Mapper
public interface ProxyChannelMapper extends BaseMapper<ProxyChannel> {

    /** 记录一次成功：清零连续失败、累加成功数、平滑平均延迟 */
    @Update("UPDATE proxy_channel SET success_count = success_count + 1, fail_count = 0, " +
            "avg_latency_ms = FLOOR(avg_latency_ms * 0.7 + #{latencyMs} * 0.3), last_used_at = NOW() " +
            "WHERE id = #{channelId}")
    int markSuccess(@Param("channelId") Long channelId, @Param("latencyMs") int latencyMs);

    /** 记录一次失败：累加连续失败次数（熔断判定留给管理端/后续定时任务） */
    @Update("UPDATE proxy_channel SET fail_count = fail_count + 1 WHERE id = #{channelId}")
    int markFailure(@Param("channelId") Long channelId);
}
