package com.web3.quant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 股票行情实体类,对应数据库表 {@code stock_price}。
 *
 * <p>保存股票各周期的 OHLCV(开高低收、成交量)、涨跌幅、振幅、换手率等行情
 * 数据,也用于 K 线序列的存储。id 为自增主键;change 字段因与 SQL 保留字
 * 冲突,通过 {@code @TableField("`change`")} 转义映射。</p>
 */
@TableName(value="stock_price")
public class StockPrice {
    /**
     * 主键(数据库自增)
     */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 股票代码,如 sh600519 */
    private String symbol;
    /** 股票名称 */
    private String name;
    /** 周期类型,如 day(日线) */
    private String period;
    /** 行情时间戳 */
    private LocalDateTime timestamp;
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
    /** 成交额 */
    private BigDecimal turnover;
    /** 振幅 */
    private BigDecimal amplitude;
    /** 涨跌额(change 为 SQL 保留字,需转义) */
    @com.baomidou.mybatisplus.annotation.TableField(value = "`change`")
    private BigDecimal change;
    /** 涨跌幅(百分比) */
    private BigDecimal changePercent;
    /** 换手率 */
    private BigDecimal turnoverRate;
    /** 记录创建时间 */
    private LocalDateTime createdAt;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPeriod() {
        return this.period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public LocalDateTime getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    public BigDecimal getTurnover() {
        return this.turnover;
    }

    public void setTurnover(BigDecimal turnover) {
        this.turnover = turnover;
    }

    public BigDecimal getAmplitude() {
        return this.amplitude;
    }

    public void setAmplitude(BigDecimal amplitude) {
        this.amplitude = amplitude;
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

    public BigDecimal getTurnoverRate() {
        return this.turnoverRate;
    }

    public void setTurnoverRate(BigDecimal turnoverRate) {
        this.turnoverRate = turnoverRate;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
