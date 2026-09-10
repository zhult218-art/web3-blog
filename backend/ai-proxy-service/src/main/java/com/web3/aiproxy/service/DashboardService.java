package com.web3.aiproxy.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3.aiproxy.entity.ProxyChannel;
import com.web3.aiproxy.entity.ProxyToken;
import com.web3.aiproxy.mapper.ProxyAlertMapper;
import com.web3.aiproxy.mapper.ProxyChannelMapper;
import com.web3.aiproxy.mapper.ProxyRequestLogMapper;
import com.web3.aiproxy.mapper.ProxyTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类名：DashboardService
 * 所属模块：ai-proxy-service（中转站）
 * 职责：智能管理看板 —— 聚合总览指标、14 天趋势、模型分布、令牌排行、时段热度、
 *       最近告警流，并运行规则化洞察引擎：环比增长、错误率预警、低余额告警、
 *       渠道健康、用量激增检测、高峰时段提示。全部基于真实数据计算，无 mock。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProxyRequestLogMapper logMapper;
    private final ProxyTokenMapper tokenMapper;
    private final ProxyChannelMapper channelMapper;
    private final ProxyAlertMapper alertMapper;

    /** 看板聚合数据（一次请求全量返回） */
    public Map<String, Object> dashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime days14 = now.minusDays(13).toLocalDate().atStartOfDay();
        LocalDateTime days30 = now.minusDays(29).toLocalDate().atStartOfDay();

        Map<String, Object> result = new HashMap<>();
        result.put("overview", overview());
        result.put("trend", fillMissingDays(logMapper.trend(days14), 14));
        result.put("modelDist", logMapper.byModel(days30));
        result.put("topUsers", logMapper.byUser(days30));
        result.put("hourly", normalizeHours(logMapper.hourly(now.minusHours(24))));
        result.put("errorDist", logMapper.errorDist(now.minusHours(24)));
        try {
            result.put("alerts", alertMapper.recent(20));
        } catch (Exception ignored) {
        }
        return result;
    }

    /** 总览卡片：今日/昨日请求数·消耗·token·活跃令牌 + 令牌与渠道存量 */
    public Map<String, Object> overview() {
        Map<String, Object> today = logMapper.todayOverview();
        Map<String, Object> yesterday = logMapper.yesterdayOverview();

        Long tokenTotal = tokenMapper.selectCount(null);
        Long tokenActive = tokenMapper.selectCount(
                Wrappers.<ProxyToken>lambdaQuery().eq(ProxyToken::getStatus, 1));
        Long channelTotal = channelMapper.selectCount(null);
        Long channelEnabled = channelMapper.selectCount(
                Wrappers.<ProxyChannel>lambdaQuery().eq(ProxyChannel::getStatus, 1));

        Map<String, Object> overview = new HashMap<>();
        overview.put("todayRequests", num(today.get("requests")));
        overview.put("todayQuota", dec(today.get("quota")));
        overview.put("todayTokens", num(today.get("tokens")));
        overview.put("avgLatency", Math.round(dbl(today.get("avgLatency"))));
        overview.put("activeTokensToday", logMapper.activeTokensToday());
        overview.put("yesterdayRequests", num(yesterday.get("requests")));
        overview.put("yesterdayQuota", dec(yesterday.get("quota")));
        overview.put("tokenTotal", tokenTotal);
        overview.put("tokenActive", tokenActive);
        overview.put("channelTotal", channelTotal);
        overview.put("channelEnabled", channelEnabled);

        long tReq = num(today.get("requests"));
        long yReq = num(yesterday.get("requests"));
        overview.put("requestGrowthPct", pct(tReq - yReq, yReq));
        BigDecimal tQuota = dec(today.get("quota"));
        BigDecimal yQuota = dec(yesterday.get("quota"));
        overview.put("quotaGrowthPct", yQuota.signum() == 0 ? null :
                tQuota.subtract(yQuota).divide(yQuota, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue());
        return overview;
    }

    /**
     * 智能洞察：规则引擎输出运营建议与风险告警。
     *
     * @return [{level: success|info|warn|danger, icon, title, detail}]
     */
    public List<Map<String, Object>> insights() {
        List<Map<String, Object>> out = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> today = logMapper.todayOverview();
        long todayReq = num(today.get("requests"));
        double avgLatency = dbl(today.get("avgLatency"));

        // ① 流量环比
        if (todayReq > 0) {
            long yesterdayReq = num(logMapper.yesterdayOverview().get("requests"));
            String delta = pctText(todayReq - yesterdayReq, yesterdayReq);
            Map<String, Object> item = base(yesterdayReq <= todayReq ? "success" : "info", "📈",
                    "今日请求 " + todayReq + " 次（较昨日 " + delta + "）");
            item.put("detail", "平均响应耗时 " + Math.round(avgLatency) + " ms");
            out.add(item);
        } else {
            out.add(base("info", "🌙", "今日暂无调用", "中转站今日还没有请求记录"));
        }

        // ② 错误率预警
        List<Map<String, Object>> errors = logMapper.errorDist(now.minusHours(24));
        if (!errors.isEmpty() && todayReq > 0) {
            long fail24 = errors.stream().mapToLong(e -> num(e.get("cnt"))).sum();
            double rate = fail24 * 100.0 / Math.max(todayReq, fail24);
            StringBuilder sb = new StringBuilder();
            errors.forEach(e -> sb.append("HTTP ").append(e.get("statusCode")).append("×").append(num(e.get("cnt"))).append(" "));
            out.add(base(rate >= 20 ? "danger" : rate >= 5 ? "warn" : "info",
                    rate >= 5 ? "🚨" : "🧪",
                    "近 24h 失败率 " + String.format("%.1f%%", Math.min(rate, 100)),
                    sb.toString().trim()));
        }

        // ③ 低余额令牌（实时扫描，与 proxy_alert 幂等告警互补）
        List<ProxyToken> tokens = tokenMapper.selectList(Wrappers.<ProxyToken>lambdaQuery()
                .eq(ProxyToken::getStatus, 1)
                .eq(ProxyToken::getUnlimitedQuota, 0).isNotNull(ProxyToken::getQuota));
        List<String> lowBalance = new ArrayList<>();
        for (ProxyToken t : tokens) {
            Long quota = t.getQuota();
            long used = t.getUsedQuota() == null ? 0 : t.getUsedQuota();
            if (quota != null && quota > 0) {
                if (quota - used <= 0) {
                    lowBalance.add("#" + t.getId() + "(" + t.getName() + ") 已耗尽");
                } else if (used * 5 >= quota) {
                    lowBalance.add("#" + t.getId() + "(" + t.getName() + ") 余 " + (quota - used));
                }
            }
        }
        if (!lowBalance.isEmpty()) {
            out.add(base(lowBalance.stream().anyMatch(s -> s.contains("已耗尽")) ? "danger" : "warn",
                    "💳", lowBalance.size() + " 枚令牌余额不足 20%", String.join("；", lowBalance)));
        }

        // ④ 渠道健康（近 24h 失败分布）
        List<Map<String, Object>> chFails = logMapper.channelFailures(now.minusHours(24));
        if (!chFails.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            chFails.forEach(f -> sb.append(f.get("channelName") == null
                    ? "#" + f.get("channelId") : f.get("channelName"))
                    .append(" 失败 ").append(num(f.get("failures"))).append(" 次；"));
            out.add(base("warn", "🛰️", "渠道近 24h 存在失败转发", sb.toString()));
        }

        // ⑤ 用量激增令牌（今日 > 3 × 其 7 日日均）
        Map<Long, Double> avgMap = new HashMap<>();
        for (Map<String, Object> row : logMapper.userDailyAvg(now.minusDays(7))) {
            avgMap.put(numAsLong(row.get("tokenId")), dbl(row.get("avgDaily")));
        }
        List<String> spikes = new ArrayList<>();
        for (Map<String, Object> row : logMapper.byUserToday()) {
            Long tid = numAsLong(row.get("tokenId"));
            double todayCnt = num(row.get("requests"));
            Double avg = avgMap.get(tid);
            if (avg != null && avg >= 1 && todayCnt > avg * 3) {
                spikes.add("令牌 #" + tid + " 今日 " + (long) todayCnt + " 次（日均 " +
                        String.format("%.1f", avg) + "）");
            }
        }
        if (!spikes.isEmpty()) {
            out.add(base("warn", "🔥", "检测到用量激增令牌", String.join("；", spikes)));
        }

        // ⑥ 高峰时段
        List<Map<String, Object>> hours = normalizeHours(logMapper.hourly(now.minusHours(24)));
        int peakHour = -1;
        long peakCnt = 0;
        for (Map<String, Object> h : hours) {
            long c = num(h.get("requests"));
            if (c > peakCnt) {
                peakCnt = c;
                peakHour = ((Number) h.get("hour")).intValue();
            }
        }
        if (peakHour >= 0 && peakCnt >= 3) {
            out.add(base("info", "⏰", "高峰时段 " + peakHour + ":00–" + ((peakHour + 1) % 24) + ":00",
                    "近 24h 该时段请求最多（" + peakCnt + " 次），建议保障该时段渠道余量"));
        }

        // ⑦ 渠道容量
        Long enabled = channelMapper.selectCount(Wrappers.<ProxyChannel>lambdaQuery()
                .eq(ProxyChannel::getStatus, 1));
        if (enabled != null && enabled == 0) {
            out.add(base("warn", "🔌", "当前无启用渠道", "所有 sk- 请求将回退兜底直连地址，不计费"));
        }
        return out;
    }

    // ---------------- 工具 ----------------

    private Map<String, Object> base(String level, String icon, String title) {
        return base(level, icon, title, null);
    }

    private Map<String, Object> base(String level, String icon, String title, String detail) {
        Map<String, Object> m = new HashMap<>();
        m.put("level", level);
        m.put("icon", icon);
        m.put("title", title);
        m.put("detail", detail);
        return m;
    }

    /** 补齐趋势图中缺失日期为 0 */
    private List<Map<String, Object>> fillMissingDays(List<Map<String, Object>> rows, int days) {
        Map<String, Map<String, Object>> byDate = new HashMap<>();
        rows.forEach(r -> byDate.put(String.valueOf(r.get("date")), r));
        List<Map<String, Object>> filled = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        for (int i = 0; i < days; i++) {
            String key = start.plusDays(i).toString();
            Map<String, Object> row = byDate.get(key);
            if (row == null) {
                row = new HashMap<>();
                row.put("date", key);
                row.put("requests", 0L);
                row.put("quota", BigDecimal.ZERO);
                row.put("tokens", 0L);
            }
            filled.add(row);
        }
        return filled;
    }

    /** 时段补齐 0-23 */
    private List<Map<String, Object>> normalizeHours(List<Map<String, Object>> rows) {
        Map<Integer, Map<String, Object>> byHour = new HashMap<>();
        rows.forEach(r -> byHour.put(((Number) r.get("hour")).intValue(), r));
        List<Map<String, Object>> filled = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> row = byHour.get(h);
            if (row == null) {
                row = new HashMap<>();
                row.put("hour", h);
                row.put("requests", 0L);
                row.put("quota", BigDecimal.ZERO);
            }
            filled.add(row);
        }
        return filled;
    }

    private String pctText(long cur, long base) {
        if (base <= 0) return cur > 0 ? "新增" : "持平";
        long diff = (cur - base) * 100 / base;
        return (diff >= 0 ? "+" : "") + diff + "%";
    }

    private Double pct(long diff, long base) {
        if (base <= 0) return null;
        return Math.round(diff * 10000.0 / base) / 100.0;
    }

    private long num(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    private long numAsLong(Object o) {
        return num(o);
    }

    private BigDecimal dec(Object o) {
        if (o instanceof BigDecimal d) return d;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    private double dbl(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0d;
    }
}
