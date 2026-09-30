package com.web3.shop.dto;

import lombok.Data;

/**
 * OrderCreateDTO —— 订单创建请求参数对象（DTO）
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：接收前端 POST /order 请求体，承载下单的商品 ID 与购买数量。
 */
@Data
public class OrderCreateDTO {
    private Long productId;
    private Integer quantity;
}
