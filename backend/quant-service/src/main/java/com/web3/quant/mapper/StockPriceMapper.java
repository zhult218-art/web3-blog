package com.web3.quant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.quant.entity.StockPrice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 股票行情表 Mapper(quant-service 模块)。
 *
 * <p>{@code @Mapper} 注册为 MyBatis 映射器,继承
 * {@link com.baomidou.mybatisplus.core.mapper.BaseMapper} 获得
 * stock_price 表的基础 CRUD 能力。</p>
 */
@Mapper
public interface StockPriceMapper
extends BaseMapper<StockPrice> {
}
