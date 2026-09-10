/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  org.apache.ibatis.annotations.Mapper
 */
package com.web3.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.shop.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * ProductMapper —— 商品表数据访问接口
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：操作 product 商品表。继承 BaseMapper 获得通用 CRUD，
 * 额外提供原子扣减库存方法（带充足库存条件更新，并发安全，扣减同时累计销量）。
 */
@Mapper
public interface ProductMapper
extends BaseMapper<Product> {
    /** 原子扣减库存并累计销量：仅在库存充足（stock >= quantity）且非逻辑删除时生效，返回受影响行数 */
    @Update("UPDATE product SET stock = stock - #{quantity}, sales = COALESCE(sales, 0) + #{quantity} WHERE id = #{id} AND deleted = 0 AND stock >= #{quantity}")
    int deductStockAtomic(@Param("id") Long id, @Param("quantity") int quantity);

    /** 回补库存并回退销量：取消订单时调用，返回受影响行数 */
    @Update("UPDATE product SET stock = stock + #{quantity}, sales = CASE WHEN COALESCE(sales, 0) >= #{quantity} THEN sales - #{quantity} ELSE 0 END WHERE id = #{id} AND deleted = 0")
    int restoreStock(@Param("id") Long id, @Param("quantity") int quantity);
}
