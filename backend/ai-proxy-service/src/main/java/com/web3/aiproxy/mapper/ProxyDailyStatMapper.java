package com.web3.aiproxy.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 日汇总 Mapper —— 单语句 upsert 原子累加（防死锁：复合主键行级锁一次持有）；
 * 供长历史统计查询（日志表可安全归档）。
 */
@Mapper
public interface ProxyDailyStatMapper {

    @Insert("INSERT INTO proxy_daily_stat " +
            "(stat_date, user_key, model_name, requests, error_count, prompt_tokens, completion_tokens, quota_cost) " +
            "VALUES (#{statDate}, #{userKey}, #{modelName}, #{requests}, #{errorCount}, " +
            "#{promptTokens}, #{completionTokens}, #{quotaCost}) " +
            "ON DUPLICATE KEY UPDATE requests = requests + VALUES(requests), " +
            "error_count = error_count + VALUES(error_count), " +
            "prompt_tokens = prompt_tokens + VALUES(prompt_tokens), " +
            "completion_tokens = completion_tokens + VALUES(completion_tokens), " +
            "quota_cost = quota_cost + VALUES(quota_cost)")
    int upsertIncrement(@Param("statDate") LocalDate statDate, @Param("userKey") String userKey,
                        @Param("modelName") String modelName, @Param("requests") long requests,
                        @Param("errorCount") long errorCount, @Param("promptTokens") long promptTokens,
                        @Param("completionTokens") long completionTokens, @Param("quotaCost") long quotaCost);

    /** 近 N 天日汇总趋势 */
    @Select("SELECT stat_date AS date, SUM(requests) AS requests, SUM(quota_cost) AS quota, " +
            "SUM(prompt_tokens + completion_tokens) AS tokens " +
            "FROM proxy_daily_stat WHERE stat_date >= #{since} GROUP BY stat_date ORDER BY stat_date")
    List<Map<String, Object>> trendSince(@Param("since") LocalDate since);
}
