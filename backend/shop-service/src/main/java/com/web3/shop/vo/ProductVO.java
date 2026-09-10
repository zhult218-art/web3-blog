/*
 * Decompiled with CFR 0.152.
 */
package com.web3.shop.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ProductVO —— 商品视图对象（VO）
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：对前端输出的商品数据载体，由 Product 实体转换而来。
 */
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

    public Long getId() {
        return this.id;
    }

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

    public Integer getSales() {
        return this.sales;
    }

    public String getCover() {
        return this.cover;
    }

    public String getCategory() {
        return this.category;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setSales(Integer sales) {
        this.sales = sales;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ProductVO)) {
            return false;
        }
        ProductVO other = (ProductVO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Integer this$stock = this.getStock();
        Integer other$stock = other.getStock();
        if (this$stock == null ? other$stock != null : !((Object)this$stock).equals(other$stock)) {
            return false;
        }
        Integer this$sales = this.getSales();
        Integer other$sales = other.getSales();
        if (this$sales == null ? other$sales != null : !((Object)this$sales).equals(other$sales)) {
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
        if (this$category == null ? other$category != null : !this$category.equals(other$category)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ProductVO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $stock = this.getStock();
        result = result * 59 + ($stock == null ? 43 : ((Object)$stock).hashCode());
        Integer $sales = this.getSales();
        result = result * 59 + ($sales == null ? 43 : ((Object)$sales).hashCode());
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
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "ProductVO(id=" + this.getId() + ", name=" + this.getName() + ", description=" + this.getDescription() + ", price=" + this.getPrice() + ", stock=" + this.getStock() + ", sales=" + this.getSales() + ", cover=" + this.getCover() + ", category=" + this.getCategory() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}
