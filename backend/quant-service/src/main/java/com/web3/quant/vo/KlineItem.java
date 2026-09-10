package com.web3.quant.vo;

import java.math.BigDecimal;

/**
 * K 线图数据项 VO(quant-service 模块)。
 *
 * <p>K 线接口({@code /stock/{symbol}/kline})返回的单根 K 线数据,
 * 包含时间、开高低收四价、成交量与涨跌幅,供前端绘制 K 线图。</p>
 */
public class KlineItem {
    /** K 线时间(yyyy-MM-dd HH:mm) */
    private String date;
    /** 开盘价 */
    private BigDecimal open;
    /** 收盘价 */
    private BigDecimal close;
    /** 最高价 */
    private BigDecimal high;
    /** 最低价 */
    private BigDecimal low;
    /** 成交量 */
    private BigDecimal volume;
    /** 涨跌幅(百分比) */
    private BigDecimal changePercent;

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getOpen() {
        return this.open;
    }

    public void setOpen(BigDecimal open) {
        this.open = open;
    }

    public BigDecimal getClose() {
        return this.close;
    }

    public void setClose(BigDecimal close) {
        this.close = close;
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

    public BigDecimal getVolume() {
        return this.volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public BigDecimal getChangePercent() {
        return this.changePercent;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }
}
