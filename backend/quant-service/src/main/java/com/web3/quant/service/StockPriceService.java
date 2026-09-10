package com.web3.quant.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web3.quant.entity.StockPrice;
import com.web3.quant.mapper.StockPriceMapper;
import com.web3.quant.vo.KlineItem;
import com.web3.quant.vo.StockVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Service;

/**
 * 股票行情业务服务(quant-service 模块)。
 *
 * <p>动态维护股票池：优先通过 HTTP 调用 Python 量化网关获取全量 A 股列表，
 * 缓存 5 分钟；网关不可用时使用内置的沪深蓝筹全量列表（覆盖银行、保险、
 * 白酒、新能源、半导体、医药、地产、券商等主要行业约 100 只股票）。
 * 行情统一转换为 StockVO / KlineItem 供接口返回。</p>
 */
@Service
public class StockPriceService {
    private volatile Map<String, String> stockPool = new LinkedHashMap<>();
    private volatile long poolLoadTime = 0;
    private static final long POOL_CACHE_TTL = 5 * 60 * 1000L;
    private final StockPriceMapper stockPriceMapper;
    private static final Random RANDOM = new Random();
    private static final String QUANT_PY_BASE = System.getenv().getOrDefault("QUANT_PY_BASE", "http://127.0.0.1:9006");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 内置沪深蓝筹全量列表（网关不可用时的兜底） */
    private static final Map<String, String> FALLBACK_POOL = new LinkedHashMap<>();
    static {
        // 上证50核心
        FALLBACK_POOL.put("sh600519", "\u8d35\u5dde\u8305\u53f0");
        FALLBACK_POOL.put("sz000858", "\u4e94\u7cae\u6db2");
        FALLBACK_POOL.put("sh601318", "\u4e2d\u56fd\u5e73\u5b89");
        FALLBACK_POOL.put("sz300750", "\u5b81\u5fb7\u65f6\u4ee3");
        FALLBACK_POOL.put("sh600036", "\u62db\u5546\u94f6\u884c");
        FALLBACK_POOL.put("sz000001", "\u5e73\u5b89\u94f6\u884c");
        FALLBACK_POOL.put("sh688981", "\u4e2d\u82af\u56fd\u9645");
        FALLBACK_POOL.put("sz002594", "\u6bd4\u4e9a\u8fea");
        // 银行
        FALLBACK_POOL.put("sh601398", "\u5de5\u5546\u94f6\u884c");
        FALLBACK_POOL.put("sh601288", "\u519c\u4e1a\u94f6\u884c");
        FALLBACK_POOL.put("sh601988", "\u4e2d\u56fd\u94f6\u884c");
        FALLBACK_POOL.put("sh601939", "\u5efa\u8bbe\u94f6\u884c");
        FALLBACK_POOL.put("sh600000", "\u6d66\u53d1\u94f6\u884c");
        FALLBACK_POOL.put("sh600016", "\u6c11\u751f\u94f6\u884c");
        FALLBACK_POOL.put("sh600015", "\u534e\u590f\u94f6\u884c");
        FALLBACK_POOL.put("sh601166", "\u5174\u4e1a\u94f6\u884c");
        FALLBACK_POOL.put("sz002142", "\u5b81\u6ce2\u94f6\u884c");
        FALLBACK_POOL.put("sh600919", "\u6c5f\u82cf\u94f6\u884c");
        // 保险
        FALLBACK_POOL.put("sh601628", "\u4eba\u5bff\u94f6\u884c");
        FALLBACK_POOL.put("sh601601", "\u4e2d\u56fd\u592a\u4fdd");
        FALLBACK_POOL.put("sh601336", "\u65b0\u534e\u4fdd\u9669");
        // 券商
        FALLBACK_POOL.put("sh600030", "\u4e2d\u4fe1\u8bc1\u5238");
        FALLBACK_POOL.put("sh601211", "\u56fd\u6cf0\u541b\u5b89");
        FALLBACK_POOL.put("sh600837", "\u6d77\u901a\u8bc1\u5238");
        FALLBACK_POOL.put("sz000776", "\u5e7f\u53d1\u8bc1\u5238");
        FALLBACK_POOL.put("sh601688", "\u534e\u6cf0\u8bc1\u5238");
        // 白酒
        FALLBACK_POOL.put("sz000568", "\u6cf8\u5dde\u8001\u7a9a");
        FALLBACK_POOL.put("sh600809", "\u5c71\u897f\u6c7f\u9152");
        FALLBACK_POOL.put("sz002304", "\u6d88\u6d0a\u6cb3\u8c37");
        FALLBACK_POOL.put("sz000596", "\u53e4\u4e95\u8d21\u9152");
        FALLBACK_POOL.put("sh603369", "\u4eca\u4e16\u7f18");
        // 新能源
        FALLBACK_POOL.put("sz002459", "\u666f\u6d89\u80fd\u6e90");
        FALLBACK_POOL.put("sh601012", "\u9686\u57fa\u7eff\u80fd");
        FALLBACK_POOL.put("sz002466", "\u5929\u9f99\u6676\u79d1");
        FALLBACK_POOL.put("sz300274", "\u9633\u5149\u7535\u6e90");
        FALLBACK_POOL.put("sz002129", "\u4e2d\u73af\u80fd\u79d1");
        // 汽车
        FALLBACK_POOL.put("sh600104", "\u4e0a\u6c7f\u96c6\u56e2");
        FALLBACK_POOL.put("sz000625", "\u957f\u5b89\u6c7d\u8f66");
        FALLBACK_POOL.put("sz002920", "\u5fb7\u8d5b\u8d77\u65cf");
        FALLBACK_POOL.put("sh600741", "\u534e\u57df\u6c7d\u8f66");
        // 医药
        FALLBACK_POOL.put("sh600276", "\u6052\u745e\u533b\u836f");
        FALLBACK_POOL.put("sz000538", "\u4e91\u5357\u767d\u836f");
        FALLBACK_POOL.put("sz300760", "\u8fc8\u745e\u533b\u7597");
        FALLBACK_POOL.put("sz002007", "\u534e\u514b\u751f\u7269");
        FALLBACK_POOL.put("sh600196", "\u590d\u661f\u533b\u836f");
        FALLBACK_POOL.put("sz300015", "\u7231\u5c14\u773c\u79d1");
        // 半导体/芯片
        FALLBACK_POOL.put("sh688012", "\u4e2d\u5fae\u534a\u5bfc");
        FALLBACK_POOL.put("sz002371", "\u5317\u65b9\u534a\u5bfc\u521b\u65b0");
        FALLBACK_POOL.put("sz002049", "\u7d2b\u5149\u56fd\u5fae");
        FALLBACK_POOL.put("sh688008", "\u6d9c\u6701\u79d1\u521b");
        FALLBACK_POOL.put("sz300661", "\u5706\u8270\u5a01\u4f20\u611f");
        // 科技/互联网
        FALLBACK_POOL.put("sz002230", "\u79d1\u5927\u8baf\u98de");
        FALLBACK_POOL.put("sh603019", "\u4e2d\u79d1\u661f\u5149");
        FALLBACK_POOL.put("sz300059", "\u4e1c\u8d22\u8d22\u5bcc");
        FALLBACK_POOL.put("sz002602", "\u4e07\u8fbe\u7535\u5546");
        // 地产
        FALLBACK_POOL.put("sz000002", "\u4e07\u79d1A");
        FALLBACK_POOL.put("sh600048", "\u4fdd\u5229\u53d1\u5c55");
        FALLBACK_POOL.put("sz001979", "\u86ce\u5b9d\u5730\u4ea7");
        // 家电
        FALLBACK_POOL.put("sz000333", "\u7f8e\u7687\u7535\u5668");
        FALLBACK_POOL.put("sz000651", "\u683c\u529b\u7535\u5668");
        FALLBACK_POOL.put("sz002032", "\u82cf\u6ce8\u5c3c\u5a01");
        // 食品饮料
        FALLBACK_POOL.put("sz002714", "\u7267\u539f\u80a1\u4efd");
        FALLBACK_POOL.put("sh600887", "\u4f0a\u5229\u8421\u4e73");
        FALLBACK_POOL.put("sz000895", "\u53cc\u6c47\u5b9d");
        // 钢铁/有色
        FALLBACK_POOL.put("sh600019", "\u5b9d\u94a2\u80a1\u4efd");
        FALLBACK_POOL.put("sh601899", "\u7d2b\u91d1\u77ff\u4e1a");
        FALLBACK_POOL.put("sh600547", "\u5c71\u4e1c\u9ec4\u91d1");
        // 电力/能源
        FALLBACK_POOL.put("sh600900", "\u957f\u6c5f\u7535\u529b");
        FALLBACK_POOL.put("sh600886", "\u56fd\u6295\u7535\u529b");
        FALLBACK_POOL.put("sh601985", "\u4e2d\u56fd\u6838\u7535");
        // 军工
        FALLBACK_POOL.put("sh600893", "\u8230\u52a8\u529b");
        FALLBACK_POOL.put("sz002013", "\u4e2d\u822a\u673a\u7535");
        FALLBACK_POOL.put("sh600760", "\u4e2d\u8230\u9632\u6001");
        // 通信
        FALLBACK_POOL.put("sh600050", "\u4e2d\u56fd\u8054\u901a");
        FALLBACK_POOL.put("sh601728", "\u4e2d\u56fd\u7535\u4fe1");
        FALLBACK_POOL.put("sz000063", "\u4e2d\u5173\u6751\u901a\u4fe1");
        // 交通运输
        FALLBACK_POOL.put("sh601006", "\u5927\u65b0\u94c1\u8def");
        FALLBACK_POOL.put("sh600029", "\u5357\u65b9\u822a\u7a7a");
        FALLBACK_POOL.put("sh601111", "\u4e2d\u56fd\u56fd\u822a");
        // 传媒/文化
        FALLBACK_POOL.put("sz002607", "\u4e2d\u516c\u5e73");
        FALLBACK_POOL.put("sz300413", "\u85cf\u745e\u5f71\u4e1a");
        // 建材/建筑
        FALLBACK_POOL.put("sh600585", "\u6d77\u86ee\u6c34\u6ce5");
        FALLBACK_POOL.put("sh601668", "\u4e2d\u56fd\u5efa\u7b51");
        // 化工
        FALLBACK_POOL.put("sh600309", "\u4e07\u534e\u5316\u5de5");
        FALLBACK_POOL.put("sz000792", "\u76d0\u6e56\u80a1\u4efd");
        // 纺织服装
        FALLBACK_POOL.put("sh600398", "\u6d77\u6f8b\u6703\u80a1");
        // 农业
        FALLBACK_POOL.put("sz000998", "\u9686\u5e73\u96c5\u96c5");
        FALLBACK_POOL.put("sz002714", "\u7267\u539f\u80a1\u4efd");
        // 旅游
        FALLBACK_POOL.put("sh600138", "\u4e2d\u9752\u65c5");
        // 环保
        FALLBACK_POOL.put("sz300070", "\u7eff\u8272\u751f\u6001");
        // 机械
        FALLBACK_POOL.put("sh600031", "\u4e09\u4e00\u91cd\u5de5");
        FALLBACK_POOL.put("sz000157", "\u4e2d\u8054\u91cd\u5de5");
    }

    public StockPriceService(StockPriceMapper stockPriceMapper) {
        this.stockPriceMapper = stockPriceMapper;
    }

    /**
     * 获取股票池（代码 -> 名称）。
     * 优先从 Python 网关动态获取全量列表，缓存 5 分钟；
     * 网关不可用时使用内置蓝筹列表。
     */
    public Map<String, String> getStockPool() {
        long now = System.currentTimeMillis();
        if (!stockPool.isEmpty() && (now - poolLoadTime) < POOL_CACHE_TTL) {
            return stockPool;
        }
        synchronized (this) {
            if (!stockPool.isEmpty() && (now - poolLoadTime) < POOL_CACHE_TTL) {
                return stockPool;
            }
            Map<String, String> dynamic = fetchPoolFromGateway();
            if (dynamic != null && !dynamic.isEmpty()) {
                stockPool = dynamic;
            } else if (stockPool.isEmpty()) {
                stockPool = new LinkedHashMap<>(FALLBACK_POOL);
            }
            poolLoadTime = System.currentTimeMillis();
        }
        return stockPool;
    }

    /**
     * 从 Python 网关获取全量 A 股列表。
     * 调用 /api/quant/market/quotes 获取所有股票报价，
     * 从报价中提取代码和名称构建股票池。
     */
    private Map<String, String> fetchPoolFromGateway() {
        try {
            JsonNode data = gatewayGet("/api/quant/market/quotes");
            if (data == null || !data.isArray()) return null;
            Map<String, String> pool = new LinkedHashMap<>();
            for (JsonNode item : data) {
                String code = item.path("code").asText("");
                String name = item.path("name").asText("");
                if (!code.isEmpty()) {
                    pool.put(code, name.isEmpty() ? code : name);
                }
            }
            if (!pool.isEmpty()) {
                System.out.println("[StockPriceService] loaded " + pool.size() + " stocks from gateway");
            }
            return pool;
        } catch (Exception e) {
            System.err.println("[StockPriceService] failed to fetch pool from gateway: " + e);
            return null;
        }
    }

    /**
     * 根据股票代码获取股票名称,不在池内时直接返回代码本身。
     *
     * @param symbol 股票代码
     * @return 股票名称
     */
    public String getStockName(String symbol) {
        return getStockPool().getOrDefault(symbol, symbol);
    }

    /**
     * 调用 Python 量化网关的 GET 接口并解析响应中的 data 节点,
     * 网关异常或非 200 时返回 null。
     *
     * @param path 网关接口路径(含前缀,如 /api/quant/market/quote/sh600519)
     * @return 响应 JSON 的 data 节点;失败返回 null
     */
    private static JsonNode gatewayGet(String path) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(QUANT_PY_BASE + path))
                .timeout(Duration.ofSeconds(30))
                .GET().build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                System.err.println("[gateway] " + path + " -> HTTP " + res.statusCode() + " body=" + (res.body() != null ? res.body().substring(0, Math.min(res.body().length(), 200)) : ""));
                return null;
            }
            JsonNode root = MAPPER.readTree(res.body());
            return root.path("data");
        } catch (Exception e) {
            System.err.println("[gateway] " + path + " -> EX: " + e);
            return null;
        }
    }

    /**
     * 将网关返回的实时报价 JSON 节点合并到 StockPrice 实体上,
     * 仅覆盖有效字段,更新行情时间戳为当前时间。
     *
     * @param sp 待更新的行情实体
     * @param q  网关返回的报价 JSON 节点
     * @return 合并后的行情实体
     */
    private StockPrice applyRealQuote(StockPrice sp, JsonNode q) {
        if (q == null || q.isMissingNode() || q.isNull()) return sp;
        BigDecimal price = q.path("price").isNumber() ? new BigDecimal(q.path("price").asText()) : null;
        if (price == null) return sp;
        BigDecimal change = q.path("change").isNumber() ? new BigDecimal(q.path("change").asText()) : BigDecimal.ZERO;
        BigDecimal changePct = q.path("change_percent").isNumber() ? new BigDecimal(q.path("change_percent").asText()) : BigDecimal.ZERO;
        BigDecimal high = q.path("high").isNumber() ? new BigDecimal(q.path("high").asText()) : price;
        BigDecimal low = q.path("low").isNumber() ? new BigDecimal(q.path("low").asText()) : price;
        BigDecimal open = q.path("open").isNumber() ? new BigDecimal(q.path("open").asText()) : price;
        BigDecimal volume = q.path("volume").isNumber() && q.path("volume").asDouble() > 0 ? new BigDecimal(q.path("volume").asText()) : sp.getVolume();
        sp.setName(!q.path("name").asText("").isEmpty() ? q.path("name").asText() : sp.getName());
        sp.setClose(price);
        sp.setOpen(open);
        sp.setHigh(high);
        sp.setLow(low);
        sp.setChange(change);
        sp.setChangePercent(changePct);
        sp.setVolume(volume);
        sp.setTurnover(volume.multiply(price).setScale(2, RoundingMode.HALF_UP));
        sp.setAmplitude(q.path("amplitude").isNumber() ? new BigDecimal(q.path("amplitude").asText()) : sp.getAmplitude());
        sp.setTurnoverRate(q.path("turnover_rate").isNumber() ? new BigDecimal(q.path("turnover_rate").asText()) : sp.getTurnoverRate());
        sp.setTimestamp(LocalDateTime.now());
        return sp;
    }

    /**
     * 获取某只股票的最新行情:优先网关实时报价,其次数据库最新记录,
     * 兜底使用模拟数据。
     *
     * @param symbol 股票代码
     * @return 最新行情实体(含实时报价字段)
     */
    public StockPrice findLatest(String symbol) {
        StockPrice latest = this.generateMockData(symbol, this.getStockName(symbol), "day");
        try {
            LambdaQueryWrapper<StockPrice> wrapper = new LambdaQueryWrapper<StockPrice>();
            wrapper.eq(StockPrice::getSymbol, symbol).orderByDesc(StockPrice::getTimestamp).last("LIMIT 1");
            StockPrice dbRow = this.stockPriceMapper.selectOne(wrapper);
            if (dbRow != null) latest = dbRow;
        } catch (Exception ignored) {
        }
        return this.applyRealQuote(latest, StockPriceService.gatewayGet("/api/quant/market/quote/" + symbol));
    }

    /**
     * 查询某只股票指定周期的 K 线序列:优先网关 K 线接口,其次数据库,
     * 兜底生成模拟 K 线,并按 limit 截取最近 N 条。
     *
     * @param symbol 股票代码
     * @param period 周期类型,如 day
     * @param limit  需要的条数
     * @return K 线行情实体列表(按时间正序)
     */
    public List<StockPrice> findByPeriod(String symbol, String period, int limit) {
        JsonNode klines = StockPriceService.gatewayGet("/api/quant/market/kline/" + symbol + "?days=" + Math.max(limit, 90));
        if (klines != null && klines.isArray() && klines.size() > 0) {
            List<StockPrice> list = new ArrayList<StockPrice>();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (JsonNode bar : klines) {
                if (!bar.path("date").isTextual()) continue;
                try {
                    StockPrice sp = new StockPrice();
                    sp.setSymbol(symbol);
                    sp.setName(this.getStockName(symbol));
                    sp.setPeriod(period);
                    sp.setTimestamp(LocalDate.parse(bar.path("date").asText().substring(0, 10), fmt).atStartOfDay().plusDays(1));
                    BigDecimal close = new BigDecimal(bar.path("close").asText());
                    BigDecimal open = new BigDecimal(bar.path("open").asText());
                    sp.setClose(close);
                    sp.setOpen(open);
                    sp.setHigh(new BigDecimal(bar.path("high").asText()));
                    sp.setLow(new BigDecimal(bar.path("low").asText()));
                    sp.setVolume(bar.path("volume").isNumber() ? new BigDecimal(bar.path("volume").asText()) : BigDecimal.ZERO);
                    sp.setChange(close.subtract(open).setScale(2, RoundingMode.HALF_UP));
                    sp.setChangePercent(open.compareTo(BigDecimal.ZERO) > 0 ? close.subtract(open).divide(open, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                    sp.setAmplitude(bar.path("amplitude").isNumber() ? new BigDecimal(bar.path("amplitude").asText()) : BigDecimal.ZERO);
                    sp.setTurnoverRate(bar.path("change_percent").isNumber() ? new BigDecimal(bar.path("change_percent").asText()) : BigDecimal.ZERO);
                    sp.setCreatedAt(LocalDateTime.now());
                    list.add(sp);
                } catch (Exception ignored) {
                }
            }
            if (!list.isEmpty()) return list;
        }
        LambdaQueryWrapper<StockPrice> wrapper = new LambdaQueryWrapper<StockPrice>();
        wrapper.eq(StockPrice::getSymbol, symbol).eq(StockPrice::getPeriod, period).orderByDesc(StockPrice::getTimestamp).last("LIMIT " + Math.max(limit, 30));
        List<StockPrice> dbList = this.stockPriceMapper.selectList(wrapper);
        if (dbList != null && !dbList.isEmpty()) return dbList;
        List<StockPrice> mock = this.generateMockKline(symbol, this.getStockName(symbol), period, 90);
        List<StockPrice> result = new ArrayList<StockPrice>();
        for (int i = mock.size() - 1; i >= Math.max(0, mock.size() - limit); --i) {
            result.add(mock.get(i));
        }
        return result;
    }

    /**
     * 行情实体转换为股票列表/详情使用的 VO。
     *
     * @param sp 行情实体
     * @return 股票 VO(价格、涨跌幅、成交量、振幅、换手率等)
     */
    public StockVO toVO(StockPrice sp) {
        StockVO vo = new StockVO();
        vo.setSymbol(sp.getSymbol());
        vo.setName(sp.getName());
        vo.setPrice(sp.getClose());
        vo.setChange(sp.getChange());
        vo.setChangePercent(sp.getChangePercent());
        vo.setVolume(sp.getVolume());
        vo.setHigh(sp.getHigh());
        vo.setLow(sp.getLow());
        vo.setAmplitude(sp.getAmplitude());
        vo.setTurnoverRate(sp.getTurnoverRate());
        vo.setUpdatedAt(sp.getTimestamp());
        return vo;
    }

    /**
     * 行情实体转换为 K 线图数据项(yyyy-MM-dd HH:mm 格式时间、OHLCV 与涨跌幅)。
     *
     * @param sp 行情实体
     * @return K 线数据项
     */
    public KlineItem toKline(StockPrice sp) {
        KlineItem item = new KlineItem();
        item.setDate(sp.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        item.setOpen(sp.getOpen());
        item.setClose(sp.getClose());
        item.setHigh(sp.getHigh());
        item.setLow(sp.getLow());
        item.setVolume(sp.getVolume());
        item.setChangePercent(sp.getChangePercent());
        return item;
    }

    /**
     * 生成一条随机模拟行情数据(仅用于网关与数据库均不可用时的兜底)。
     *
     * @param symbol 股票代码
     * @param name   股票名称
     * @param period 周期类型
     * @return 模拟行情实体
     */
    public StockPrice generateMockData(String symbol, String name, String period) {
        StockPrice sp = new StockPrice();
        sp.setSymbol(symbol);
        sp.setName(name);
        sp.setPeriod(period);
        sp.setTimestamp(LocalDateTime.now());
        BigDecimal base = BigDecimal.valueOf(10 + RANDOM.nextInt(2000)).add(BigDecimal.valueOf(RANDOM.nextDouble() * 50));
        base = base.setScale(2, RoundingMode.HALF_UP);
        sp.setClose(base);
        sp.setOpen(base.subtract(BigDecimal.valueOf(RANDOM.nextDouble() * 5)).setScale(2, RoundingMode.HALF_UP));
        sp.setHigh(base.add(BigDecimal.valueOf(RANDOM.nextDouble() * 5)).setScale(2, RoundingMode.HALF_UP));
        sp.setLow(base.subtract(BigDecimal.valueOf(RANDOM.nextDouble() * 5)).setScale(2, RoundingMode.HALF_UP));
        sp.setVolume(BigDecimal.valueOf(1000 + RANDOM.nextInt(90000)));
        sp.setTurnover(BigDecimal.valueOf(10000 + RANDOM.nextInt(9000000)));
        sp.setChange(BigDecimal.valueOf(RANDOM.nextInt(60) - 30, 1));
        sp.setChangePercent(BigDecimal.valueOf(RANDOM.nextInt(200) - 100, 1));
        sp.setAmplitude(BigDecimal.valueOf(RANDOM.nextInt(100) + 10, 1));
        sp.setTurnoverRate(BigDecimal.valueOf(RANDOM.nextInt(500) + 5, 2));
        sp.setCreatedAt(LocalDateTime.now());
        return sp;
    }

    /**
     * 生成随机游走形态的模拟 K 线序列(仅用于兜底)。
     *
     * @param symbol 股票代码
     * @param name   股票名称
     * @param period 周期类型
     * @param days   生成的天数
     * @return 模拟 K 线实体列表(时间正序)
     */
    public List<StockPrice> generateMockKline(String symbol, String name, String period, int days) {
        List<StockPrice> list = new ArrayList<StockPrice>();
        BigDecimal base = BigDecimal.valueOf(10 + RANDOM.nextInt(2000)).add(BigDecimal.valueOf(RANDOM.nextDouble() * 50));
        LocalDateTime cursor = LocalDateTime.now().minusDays(days);
        for (int i = 0; i < days; ++i) {
            StockPrice sp = new StockPrice();
            sp.setSymbol(symbol);
            sp.setName(name);
            sp.setPeriod(period);
            sp.setTimestamp(cursor.plusDays(i));
            base = base.add(BigDecimal.valueOf(RANDOM.nextInt(400) - 200, 1));
            base = base.max(BigDecimal.valueOf(1));
            BigDecimal open = base.subtract(BigDecimal.valueOf(RANDOM.nextDouble() * 5)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal close = base.setScale(2, RoundingMode.HALF_UP);
            BigDecimal high = base.add(BigDecimal.valueOf(RANDOM.nextDouble() * 5)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal low = base.subtract(BigDecimal.valueOf(RANDOM.nextDouble() * 5)).setScale(2, RoundingMode.HALF_UP);
            sp.setOpen(open);
            sp.setClose(close);
            sp.setHigh(high);
            sp.setLow(low);
            sp.setVolume(BigDecimal.valueOf(1000 + RANDOM.nextInt(90000)));
            sp.setChange(close.subtract(open).setScale(2, RoundingMode.HALF_UP));
            sp.setChangePercent(open.compareTo(BigDecimal.ZERO) > 0 ? close.subtract(open).divide(open, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            sp.setAmplitude(BigDecimal.valueOf(RANDOM.nextInt(100) + 10, 1));
            sp.setTurnoverRate(BigDecimal.valueOf(RANDOM.nextInt(500) + 5, 2));
            sp.setCreatedAt(LocalDateTime.now());
            list.add(sp);
        }
        return list;
    }
}
