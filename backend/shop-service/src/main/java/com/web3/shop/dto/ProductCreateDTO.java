package com.web3.shop.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * ProductCreateDTO —— 商品创建/更新请求参数对象（DTO）
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：接收前端 POST /product 与 PUT /product/{id} 请求体，
 * 承载名称、描述、价格、库存、封面、分类等提交参数。
 */
@Data
public class ProductCreateDTO {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String cover;
    private String category;
}
