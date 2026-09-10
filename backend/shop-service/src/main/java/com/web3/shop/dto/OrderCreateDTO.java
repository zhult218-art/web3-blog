/*
 * Decompiled with CFR 0.152.
 */
package com.web3.shop.dto;

/**
 * OrderCreateDTO —— 订单创建请求参数对象（DTO）
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：接收前端 POST /order 请求体，承载下单的商品 ID 与购买数量。
 */
public class OrderCreateDTO {
    private Long productId;
    private Integer quantity;

    public Long getProductId() {
        return this.productId;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof OrderCreateDTO)) {
            return false;
        }
        OrderCreateDTO other = (OrderCreateDTO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$productId = this.getProductId();
        Long other$productId = other.getProductId();
        if (this$productId == null ? other$productId != null : !((Object)this$productId).equals(other$productId)) {
            return false;
        }
        Integer this$quantity = this.getQuantity();
        Integer other$quantity = other.getQuantity();
        return !(this$quantity == null ? other$quantity != null : !((Object)this$quantity).equals(other$quantity));
    }

    protected boolean canEqual(Object other) {
        return other instanceof OrderCreateDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $productId = this.getProductId();
        result = result * 59 + ($productId == null ? 43 : ((Object)$productId).hashCode());
        Integer $quantity = this.getQuantity();
        result = result * 59 + ($quantity == null ? 43 : ((Object)$quantity).hashCode());
        return result;
    }

    public String toString() {
        return "OrderCreateDTO(productId=" + this.getProductId() + ", quantity=" + this.getQuantity() + ")";
    }
}
