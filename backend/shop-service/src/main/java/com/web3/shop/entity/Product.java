package com.web3.shop.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product —— 商品实体类
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 对应数据库表：product（商品表）。
 * <p>
 * 字段说明：id 为雪花算法主键；price 单价（BigDecimal 精确小数）；stock 库存；
 * sales 销量；status 上下架状态（ON=上架）；deleted 为逻辑删除标记（{@code @TableLogic} 自动过滤）；
 * createdAt 插入时自动填充，updatedAt 插入/更新时自动填充。
 */
@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String cover;
    private String category;
    private Integer sales;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
