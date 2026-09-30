package com.web3.shop.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ProductVO —— 商品视图对象（VO）
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：对前端输出的商品数据载体，由 Product 实体转换而来。
 */
@Data
public class ProductVO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Integer sales;
    private String cover;
    private String category;
    private LocalDateTime createdAt;
}
