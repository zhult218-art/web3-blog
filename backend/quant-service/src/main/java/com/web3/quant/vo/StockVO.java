package com.web3.quant.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 股票行情展示层 VO(quant-service 模块)。
 *
 * <p>股票列表与详情接口返回给前端的模型,由
 * {@link com.web3.quant.service.StockPriceService#toVO} 从行情实体转换而来。</p>
 */
public class StockVO {
    /** 股票代码,如 sh600519 */
    private String symbol;
    /** 股票名称 */
    private String name;
    /** 当前价格 */
    private BigDecimal price;
    /** 涨跌额 */
    private BigDecimal change;
    /** 涨跌幅(百分比) */
    private BigDecimal changePercent;
    /** 成交量 */
    private BigDecimal volume;
    /** 最高价 */
    private BigDecimal high;
    /** 最低价 */
    private BigDecimal low;
    /** 振幅 */
    private BigDecimal amplitude;
    /** 换手率 */
    private BigDecimal turnoverRate;
    /** 行情更新时间 */
    private LocalDateTime updatedAt;

    public String getSymbol() {
        return this.symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getChange() {
        return this.change;
    }

    public void setChange(BigDecimal change) {
        this.change = change;
    }

    public BigDecimal getChangePercent() {
        return this.changePercent;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }

    public BigDecimal getVolume() {
        return this.volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public BigDecimal getHigh() {
        return this.high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getLow() {
        return this.low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getAmplitude() {
        return this.amplitude;
    }

    public void setAmplitude(BigDecimal amplitude) {
        this.amplitude = amplitude;
    }

    public BigDecimal getTurnoverRate() {
        return this.turnoverRate;
    }

    public void setTurnoverRate(BigDecimal turnoverRate) {
        this.turnoverRate = turnoverRate;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
