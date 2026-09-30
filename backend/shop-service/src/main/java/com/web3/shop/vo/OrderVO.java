package com.web3.shop.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OrderVO —— 订单视图对象（VO）
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：对前端输出的订单数据载体，由 Order 实体转换而来。
 * id / productId 使用 ToStringSerializer 序列化为字符串，避免雪花 ID 超出
 * JS Number 安全范围导致精度丢失（前端回传取消/支付时得到错误 ID 而报"订单不存在"）。
 */
@Data
public class OrderVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long userId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;
    private BigDecimal amount;
    private String status;
    private Integer quantity;
    private String orderNo;
    private LocalDateTime paidAt;
    /** 支付渠道（alipay/wechat） */
    private String payChannel;
    private LocalDateTime createdAt;
}
