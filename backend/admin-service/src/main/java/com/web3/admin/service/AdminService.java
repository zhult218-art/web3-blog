/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.conditions.Wrapper
 *  com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 */
package com.web3.admin.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web3.admin.entity.DashboardStat;
import com.web3.admin.entity.OperationLog;
import com.web3.admin.entity.VisitLog;
import com.web3.admin.mapper.DashboardStatMapper;
import com.web3.admin.mapper.OperationLogMapper;
import com.web3.admin.mapper.VisitLogMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 类名：AdminService
 * 所属模块：admin-service（管理后台服务）
 * 职责：管理后台业务逻辑服务，负责控制台概览统计聚合、管理操作日志记录与查询、站点访问记录上报/分页查询/汇总统计等业务实现。
 * 关键注解：@Service 注册为 Spring 业务 Bean。
 * 说明：概览数据来自当日（自然日零点后）写入的 DashboardStat 统计项；访问统计基于 visit_log 表。
 */
@Service
public class AdminService {
    @Autowired
    private DashboardStatMapper statMapper;
    @Autowired
    private OperationLogMapper logMapper;
    @Autowired
    private VisitLogMapper visitLogMapper;
    private static final String DASHBOARD_CACHE_KEY = "admin:dashboard:stats:";
    private static final String OPERATION_LOG_PREFIX = "admin:logs:";

    /**
     * 统计最近 10 分钟内有访问记录的去重 IP 数（真实在线人数）。
     * @return Map 包含 onlineCount(Long)
     */
    public Map<String, Object> countOnlineUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        LambdaQueryWrapper<VisitLog> qw = new LambdaQueryWrapper<VisitLog>();
        qw.ge(VisitLog::getCreatedAt, (Object)cutoff);
        List<VisitLog> recent = this.visitLogMapper.selectList(qw);
        long online = recent.stream()
            .map(VisitLog::getIp)
            .filter(ip -> ip != null && !ip.isEmpty())
            .distinct()
            .count();
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("onlineCount", online);
        return result;
    }

    /**
     * 组装控制台概览：查询今日 DashboardStat -> 按固定键读取各项指标 -> 汇总服务状态（service: 前缀）。
     *
     * @return 概览 Map（totalUsers/totalArticles/totalPosts/totalOrders/totalRevenue/activeUsers/todayVisits/systemHealth/serviceStatus）
     */
    public Map<String, Object> getDashboardOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<DashboardStat> qw = new LambdaQueryWrapper<DashboardStat>();
        qw.gt(DashboardStat::getStatDate, (Object)todayStart);
        List<DashboardStat> todayStats = this.statMapper.selectList(qw);
        HashMap<String, String> keyMap = new HashMap<String, String>();
        for (Object o : todayStats) {
            DashboardStat st = (DashboardStat)o;
            keyMap.put(st.getStatKey(), st.getStatValue());
        }
        HashMap<String, Object> overview = new HashMap<String, Object>();
        overview.put("totalUsers", statLong(keyMap, "totalUsers"));
        overview.put("totalArticles", statLong(keyMap, "totalArticles"));
        overview.put("totalPosts", statLong(keyMap, "totalPosts"));
        overview.put("totalOrders", statLong(keyMap, "totalOrders"));
        overview.put("totalRevenue", statDouble(keyMap, "totalRevenue"));
        overview.put("activeUsers", statLong(keyMap, "activeUsers"));
        overview.put("todayVisits", statLong(keyMap, "todayVisits"));
        overview.put("systemHealth", keyMap.getOrDefault("systemHealth", "unknown"));
        HashMap<String, Object> serviceStatus = new HashMap<String, Object>();
        for (Map.Entry<String, String> entry : keyMap.entrySet()) {
            if (entry.getKey() == null || !entry.getKey().startsWith("service:")) continue;
            serviceStatus.put(entry.getKey().substring("service:".length()), entry.getValue());
        }
        overview.put("serviceStatus", serviceStatus);
        return overview;
    }

    /**
     * 将统计 Map 中的字符串值安全解析为 long，缺失或非法时返回 0。
     *
     * @param keyMap 统计键值 Map
     * @param key    指标键
     * @return 解析后的 long 值
     */
    private static long statLong(Map<String, String> keyMap, String key) {
        String value = keyMap.get(key);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 将统计 Map 中的字符串值安全解析为 double，缺失或非法时返回 0.0。
     *
     * @param keyMap 统计键值 Map
     * @param key    指标键
     * @return 解析后的 double 值
     */
    private static double statDouble(Map<String, String> keyMap, String key) {
        String value = keyMap.get(key);
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * 记录一条管理操作日志。
     *
     * @param operatorId   操作人用户 ID
     * @param operatorName 操作人用户名
     * @param module       所属模块
     * @param action       操作动作
     * @param targetId     目标对象 ID（可为 null）
     * @param targetName   目标对象名称（可为 null）
     * @param detail       操作详情（可为 null）
     * @param ip           操作人 IP（可为 null）
     * @param ua           操作人 UA（可为 null）
     */
    public void logOperation(Long operatorId, String operatorName, String module, String action, String targetId, String targetName, String detail, String ip, String ua) {
        OperationLog log = new OperationLog();
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setModule(module);
        log.setAction(action);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setDetail(detail);
        log.setIpAddress(ip);
        log.setUserAgent(ua);
        log.setCreatedAt(LocalDateTime.now());
        this.logMapper.insert(log);
    }

    /**
     * 查询最近的操作日志（按创建时间倒序，最多 100 条）。
     *
     * @param limit 查询条数（内部截断为 1~100）
     * @return 操作日志列表
     */
    public List<OperationLog> getRecentLogs(int limit) {
        LambdaQueryWrapper<OperationLog> qw = new LambdaQueryWrapper<OperationLog>();
        qw.orderByDesc(OperationLog::getCreatedAt).last("LIMIT " + Math.min(limit, 100));
        return this.logMapper.selectList(qw);
    }

    /**
     * 上报一次页面访问：解析请求体中的访问信息并入库（UA 超 512 字符截断）。
     *
     * @param body 访问数据 Map（ip/country/region/city/isp/latitude/longitude/userAgent/pagePath）
     * @return 结果 Map，含新记录 id
     */
    public Map<String, Object> recordVisit(Map<String, Object> body) {
        VisitLog log = new VisitLog();
        log.setIp(asStr(body.get("ip")));
        log.setCountry(asStr(body.get("country")));
        log.setRegion(asStr(body.get("region")));
        log.setCity(asStr(body.get("city")));
        log.setIsp(asStr(body.get("isp")));
        log.setLatitude(asBigDecimal(body.get("latitude")));
        log.setLongitude(asBigDecimal(body.get("longitude")));
        log.setUserAgent(asStr(body.get("userAgent")));
        if (log.getUserAgent() == null || log.getUserAgent().length() > 512) {
            log.setUserAgent(log.getUserAgent() == null ? null : log.getUserAgent().substring(0, 512));
        }
        log.setPagePath(asStr(body.get("pagePath")));
        log.setCreatedAt(LocalDateTime.now());
        this.visitLogMapper.insert(log);
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("id", log.getId());
        return result;
    }

    /**
     * 分页查询访问记录（按创建时间倒序，每页上限 100 条）。
     *
     * @param page 页码（最小 1）
     * @param size 每页条数（1~100）
     * @return 结果 Map，含 total 与 records
     */
    public Map<String, Object> listVisits(int page, int size) {
        Page<VisitLog> p = new Page<VisitLog>(Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        LambdaQueryWrapper<VisitLog> qw = new LambdaQueryWrapper<VisitLog>();
        qw.orderByDesc(VisitLog::getCreatedAt);
        this.visitLogMapper.selectPage(p, qw);
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("total", p.getTotal());
        result.put("records", p.getRecords());
        return result;
    }

    /**
     * 访问汇总统计：总访问量、今日访问量、近 7 天独立 IP 数、近 7 天已定位（含经纬度）条数。
     *
     * @return 汇总统计 Map（total/today/uniqueIps7d/geoLocated7d）
     */
    public Map<String, Object> visitSummary() {
        long total = this.visitLogMapper.selectCount(null);
        long today = this.visitLogMapper.selectCount(new LambdaQueryWrapper<VisitLog>()
                .ge(VisitLog::getCreatedAt, LocalDate.now().atStartOfDay()));
        List<VisitLog> all = this.visitLogMapper.selectList(new LambdaQueryWrapper<VisitLog>()
                .select(VisitLog::getIp, VisitLog::getLatitude, VisitLog::getLongitude)
                .ge(VisitLog::getCreatedAt, LocalDate.now().minusDays(7).atStartOfDay()));
        long uniqueIps = all.stream().map(VisitLog::getIp).filter(ip -> ip != null && !ip.isEmpty()).distinct().count();
        long geoLocated = all.stream().filter(v -> v.getLatitude() != null && v.getLongitude() != null).count();
        HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("total", total);
        result.put("today", today);
        result.put("uniqueIps7d", uniqueIps);
        result.put("geoLocated7d", geoLocated);
        return result;
    }

    /**
     * 对象安全转字符串。
     *
     * @param o 任意对象（可为 null）
     * @return toString 结果或 null
     */
    private static String asStr(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    /**
     * 对象安全转 BigDecimal（经纬度解析用），无法解析时返回 null。
     *
     * @param o 任意对象（可为 null）
     * @return BigDecimal 值或 null
     */
    private static BigDecimal asBigDecimal(Object o) {
        if (o == null) return null;
        try {
            if (o instanceof BigDecimal) return (BigDecimal) o;
            return new BigDecimal(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
