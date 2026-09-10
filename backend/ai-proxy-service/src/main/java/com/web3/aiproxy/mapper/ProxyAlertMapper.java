package com.web3.aiproxy.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 告警 Mapper —— INSERT IGNORE 依赖 dedup_key 唯一键幂等去重，
 * 并发重复告警直接静默忽略，无更新锁竞争。
 */
@Mapper
public interface ProxyAlertMapper {

    @Insert("INSERT IGNORE INTO proxy_alert (alert_type, level, ref_id, message, dedup_key) " +
            "VALUES (#{alertType}, #{level}, #{refId}, #{message}, #{dedupKey})")
    int insertIgnore(@Param("alertType") String alertType, @Param("level") Integer level,
                     @Param("refId") String refId, @Param("message") String message,
                     @Param("dedupKey") String dedupKey);

    @Select("SELECT a.*, IF(a.created_at >= CURDATE(),1,0) AS today FROM proxy_alert a " +
            "ORDER BY a.id DESC LIMIT #{limit}")
    List<Map<String, Object>> recent(@Param("limit") int limit);
}
