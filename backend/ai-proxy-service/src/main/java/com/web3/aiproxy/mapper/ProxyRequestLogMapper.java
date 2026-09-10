package com.web3.aiproxy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.aiproxy.entity.ProxyRequestLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 请求日志 Mapper（只追加）+ 看板聚合 SQL —— 全部基于 proxy_request_log 实时聚合，
 * 统计口径排除兜底直连流量（fallback = 0）。
 */
@Mapper
public interface ProxyRequestLogMapper extends BaseMapper<ProxyRequestLog> {

    @Select("SELECT COUNT(*) AS requests, COALESCE(SUM(quota_cost),0) AS quota, " +
            "COALESCE(SUM(prompt_tokens + completion_tokens),0) AS tokens, " +
            "COALESCE(AVG(latency_ms),0) AS avgLatency " +
            "FROM proxy_request_log WHERE created_at >= CURDATE()")
    Map<String, Object> todayOverview();

    @Select("SELECT COUNT(*) AS requests, COALESCE(SUM(quota_cost),0) AS quota, " +
            "COALESCE(SUM(prompt_tokens + completion_tokens),0) AS tokens, " +
            "COALESCE(AVG(latency_ms),0) AS avgLatency " +
            "FROM proxy_request_log WHERE created_at >= CURDATE() - INTERVAL 1 DAY AND created_at < CURDATE()")
    Map<String, Object> yesterdayOverview();

    @Select("SELECT DATE(created_at) AS date, COUNT(*) AS requests, " +
            "COALESCE(SUM(quota_cost),0) AS quota, COALESCE(SUM(prompt_tokens+completion_tokens),0) AS tokens " +
            "FROM proxy_request_log WHERE created_at >= #{since} " +
            "GROUP BY DATE(created_at) ORDER BY date")
    List<Map<String, Object>> trend(@Param("since") LocalDateTime since);

    @Select("SELECT COALESCE(model_name,'unknown') AS model, COUNT(*) AS requests, " +
            "COALESCE(SUM(quota_cost),0) AS quota, COALESCE(SUM(prompt_tokens+completion_tokens),0) AS tokens " +
            "FROM proxy_request_log WHERE created_at >= #{since} " +
            "GROUP BY model_name ORDER BY quota DESC LIMIT 12")
    List<Map<String, Object>> byModel(@Param("since") LocalDateTime since);

    @Select("SELECT token_id AS tokenId, token_name AS tokenName, COUNT(*) AS requests, " +
            "COALESCE(SUM(quota_cost),0) AS quota, COALESCE(SUM(prompt_tokens+completion_tokens),0) AS tokens " +
            "FROM proxy_request_log WHERE created_at >= #{since} AND token_id IS NOT NULL " +
            "GROUP BY token_id, token_name ORDER BY quota DESC LIMIT 10")
    List<Map<String, Object>> byUser(@Param("since") LocalDateTime since);

    @Select("SELECT token_id AS tokenId, COUNT(*) AS requests, COALESCE(SUM(quota_cost),0) AS quota " +
            "FROM proxy_request_log WHERE created_at >= CURDATE() AND token_id IS NOT NULL GROUP BY token_id")
    List<Map<String, Object>> byUserToday();

    @Select("SELECT token_id AS tokenId, AVG(c) AS avgDaily FROM (" +
            "SELECT token_id, DATE(created_at) AS d, COUNT(*) AS c FROM proxy_request_log " +
            "WHERE created_at >= #{since} AND token_id IS NOT NULL GROUP BY token_id, DATE(created_at)) t " +
            "GROUP BY token_id")
    List<Map<String, Object>> userDailyAvg(@Param("since") LocalDateTime since);

    @Select("SELECT HOUR(created_at) AS hour, COUNT(*) AS requests, COALESCE(SUM(quota_cost),0) AS quota " +
            "FROM proxy_request_log WHERE created_at >= #{since} GROUP BY HOUR(created_at)")
    List<Map<String, Object>> hourly(@Param("since") LocalDateTime since);

    @Select("SELECT status_code AS statusCode, COUNT(*) AS cnt FROM proxy_request_log " +
            "WHERE created_at >= #{since} AND status_code <> 200 GROUP BY status_code ORDER BY cnt DESC")
    List<Map<String, Object>> errorDist(@Param("since") LocalDateTime since);

    @Select("SELECT COUNT(DISTINCT token_id) AS cnt FROM proxy_request_log " +
            "WHERE created_at >= CURDATE() AND token_id IS NOT NULL")
    Long activeTokensToday();

    @Select("SELECT channel_id AS channelId, channel_name AS channelName, COUNT(*) AS failures " +
            "FROM proxy_request_log WHERE created_at >= #{since} AND status_code <> 200 " +
            "AND channel_id IS NOT NULL AND channel_id > 0 GROUP BY channel_id, channel_name ORDER BY failures DESC")
    List<Map<String, Object>> channelFailures(@Param("since") LocalDateTime since);
}
