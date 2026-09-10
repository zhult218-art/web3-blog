package com.web3.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.shop.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * OrderMapper —— 订单表数据访问接口
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：操作 `order` 订单表，继承 BaseMapper 获得通用 CRUD；
 * 额外提供原子状态转换语句（仅当当前状态匹配时更新），保证支付/取消并发安全。
 */
@Mapper
public interface OrderMapper
extends BaseMapper<Order> {

    /**
     * 原子支付转换：仅当订单处于 PENDING 时置为 PAID 并记录渠道与支付时间，
     * 返回受影响行数（0 = 订单不存在或状态不允许支付）。
     */
    @Update("UPDATE `order` SET status = 'PAID', pay_channel = #{channel}, paid_at = NOW() " +
            "WHERE id = #{id} AND deleted = 0 AND status = 'PENDING'")
    int markPaid(@Param("id") Long id, @Param("channel") String channel);

    /**
     * 原子取消转换：仅当订单处于 PENDING 时置为 CANCELLED，
     * 返回受影响行数（0 = 订单不存在或已支付/已取消等不可取消状态）。
     */
    @Update("UPDATE `order` SET status = 'CANCELLED' " +
            "WHERE id = #{id} AND deleted = 0 AND status = 'PENDING'")
    int markCancelled(@Param("id") Long id);
}
