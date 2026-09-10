/*
 * Decompiled with CFR 0.152.
 */
package com.web3.shop.dto;

import java.math.BigDecimal;

/**
 * ProductCreateDTO —— 商品创建/更新请求参数对象（DTO）
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：接收前端 POST /product 与 PUT /product/{id} 请求体，
 * 承载名称、描述、价格、库存、封面、分类等提交参数。
 */
public class ProductCreateDTO {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String cover;
    private String category;

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public Integer getStock() {
        return this.stock;
    }

    public String getCover() {
        return this.cover;
    }

    public String getCategory() {
        return this.category;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ProductCreateDTO)) {
            return false;
        }
        ProductCreateDTO other = (ProductCreateDTO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$stock = this.getStock();
        Integer other$stock = other.getStock();
        if (this$stock == null ? other$stock != null : !((Object)this$stock).equals(other$stock)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        BigDecimal this$price = this.getPrice();
        BigDecimal other$price = other.getPrice();
        if (this$price == null ? other$price != null : !((Object)this$price).equals(other$price)) {
            return false;
        }
        String this$cover = this.getCover();
        String other$cover = other.getCover();
        if (this$cover == null ? other$cover != null : !this$cover.equals(other$cover)) {
            return false;
        }
        String this$category = this.getCategory();
        String other$category = other.getCategory();
        return !(this$category == null ? other$category != null : !this$category.equals(other$category));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ProductCreateDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $stock = this.getStock();
        result = result * 59 + ($stock == null ? 43 : ((Object)$stock).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        BigDecimal $price = this.getPrice();
        result = result * 59 + ($price == null ? 43 : ((Object)$price).hashCode());
        String $cover = this.getCover();
        result = result * 59 + ($cover == null ? 43 : $cover.hashCode());
        String $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : $category.hashCode());
        return result;
    }

    public String toString() {
        return "ProductCreateDTO(name=" + this.getName() + ", description=" + this.getDescription() + ", price=" + this.getPrice() + ", stock=" + this.getStock() + ", cover=" + this.getCover() + ", category=" + this.getCategory() + ")";
    }
}
