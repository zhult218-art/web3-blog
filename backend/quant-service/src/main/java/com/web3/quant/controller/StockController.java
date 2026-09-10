package com.web3.quant.controller;

import com.web3.common.core.ApiResponse;
import com.web3.quant.service.StockPriceService;
import com.web3.quant.vo.KlineItem;
import com.web3.quant.vo.StockVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 股票行情控制器(quant-service 模块)。
 *
 * <p>负责对外暴露股票池实时行情、个股详情、K 线行情与大盘仪表盘等查询接口,
 * 所有接口统一以 {@code /stock} 为前缀。数据优先来自 Python 量化网关服务
 * (QUANT_PY_BASE),异常时回退到数据库历史数据或模拟数据。</p>
 */
@RestController
@RequestMapping(value={"/stock"})
public class StockController {
    private final StockPriceService stockPriceService;

    public StockController(StockPriceService stockPriceService) {
        this.stockPriceService = stockPriceService;
    }

    /**
     * 查询股票池全部股票的最新行情(前端路由 GET /stock/list)。
     *
     * @return 股票行情 VO 列表,包含代码、名称、现价、涨跌幅等信息
     */
    @GetMapping(value={"/list"})
    public ApiResponse<List<StockVO>> list() {
        List<StockVO> result = new ArrayList<StockVO>();
        for (String symbol : this.stockPriceService.getStockPool().keySet()) {
            result.add(this.stockPriceService.toVO(this.stockPriceService.findLatest(symbol)));
        }
        return ApiResponse.ok(result);
    }

    /**
     * 查询单只股票的实时详情(前端路由 GET /stock/{symbol}/detail)。
     *
     * @param symbol 股票代码,如 sh600519(路径参数)
     * @return 该股票的行情 VO
     */
    @GetMapping(value={"/{symbol}/detail"})
    public ApiResponse<StockVO> detail(@PathVariable String symbol) {
        return ApiResponse.ok(this.stockPriceService.toVO(this.stockPriceService.findLatest(symbol)));
    }

    /**
     * 查询股票的 K 线行情(前端路由 GET /stock/{symbol}/kline),
     * 根据周期参数返回 OHLCV 序列,用于前端绘制 K 线图。
     *
     * @param symbol 股票代码
     * @param period 周期类型,如 day(日线),默认 day
     * @param limit  返回条数,默认 120
     * @return K 线数据项列表
     */
    @GetMapping(value={"/{symbol}/kline"})
    public ApiResponse<List<KlineItem>> kline(@PathVariable String symbol, @RequestParam(defaultValue="day") String period, @RequestParam(defaultValue="120") int limit) {
        List<KlineItem> result = new ArrayList<KlineItem>();
        for (Object o : this.stockPriceService.findByPeriod(symbol, period, limit)) {
            result.add(this.stockPriceService.toKline((com.web3.quant.entity.StockPrice)o));
        }
        return ApiResponse.ok(result);
    }

    /**
     * 查询大盘仪表盘统计(前端路由 GET /stock/dashboard),
     * 汇总股票池全部股票的涨跌家数、平均涨跌幅等行情概况。
     *
     * @return 包含 hotStocks(热门股票列表)、avgChangePercent(平均涨跌幅)、
     *         upCount(上涨家数)、downCount(下跌家数)、total(股票总数)的 Map
     */
    @GetMapping(value={"/dashboard"})
    public ApiResponse<Map<String, Object>> dashboard() {
        List<StockVO> hotStocks = new ArrayList<StockVO>();
        BigDecimal sum = BigDecimal.ZERO;
        int up = 0;
        int down = 0;
        Map<String, String> pool = this.stockPriceService.getStockPool();
        for (String symbol : pool.keySet()) {
            StockVO vo = this.stockPriceService.toVO(this.stockPriceService.findLatest(symbol));
            hotStocks.add(vo);
            sum = sum.add(vo.getChangePercent() == null ? BigDecimal.ZERO : vo.getChangePercent());
            if (vo.getChangePercent() == null || vo.getChangePercent().compareTo(BigDecimal.ZERO) < 0) {
                ++down;
            } else {
                ++up;
            }
        }
        BigDecimal avg = pool.isEmpty() ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(pool.size()), 2, RoundingMode.HALF_UP);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("hotStocks", hotStocks);
        data.put("avgChangePercent", avg);
        data.put("upCount", up);
        data.put("downCount", down);
        data.put("total", pool.size());
        return ApiResponse.ok(data);
    }
}
