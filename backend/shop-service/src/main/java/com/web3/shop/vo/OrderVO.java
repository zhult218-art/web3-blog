/*
 * Decompiled with CFR 0.152.
 */
package com.web3.shop.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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

    public Long getId() {
        return this.id;
    }

    public Long getUserId() {
        return this.userId;
    }

    public Long getProductId() {
        return this.productId;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public String getStatus() {
        return this.status;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public String getOrderNo() {
        return this.orderNo;
    }

    public LocalDateTime getPaidAt() {
        return this.paidAt;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getPayChannel() {
        return this.payChannel;
    }

    public void setPayChannel(String payChannel) {
        this.payChannel = payChannel;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof OrderVO)) {
            return false;
        }
        OrderVO other = (OrderVO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Long this$userId = this.getUserId();
        Long other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !((Object)this$userId).equals(other$userId)) {
            return false;
        }
        Long this$productId = this.getProductId();
        Long other$productId = other.getProductId();
        if (this$productId == null ? other$productId != null : !((Object)this$productId).equals(other$productId)) {
            return false;
        }
        Integer this$quantity = this.getQuantity();
        Integer other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !((Object)this$quantity).equals(other$quantity)) {
            return false;
        }
        BigDecimal this$amount = this.getAmount();
        BigDecimal other$amount = other.getAmount();
        if (this$amount == null ? other$amount != null : !((Object)this$amount).equals(other$amount)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$orderNo = this.getOrderNo();
        String other$orderNo = other.getOrderNo();
        if (this$orderNo == null ? other$orderNo != null : !this$orderNo.equals(other$orderNo)) {
            return false;
        }
        LocalDateTime this$paidAt = this.getPaidAt();
        LocalDateTime other$paidAt = other.getPaidAt();
        if (this$paidAt == null ? other$paidAt != null : !((Object)this$paidAt).equals(other$paidAt)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof OrderVO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : ((Object)$userId).hashCode());
        Long $productId = this.getProductId();
        result = result * 59 + ($productId == null ? 43 : ((Object)$productId).hashCode());
        Integer $quantity = this.getQuantity();
        result = result * 59 + ($quantity == null ? 43 : ((Object)$quantity).hashCode());
        BigDecimal $amount = this.getAmount();
        result = result * 59 + ($amount == null ? 43 : ((Object)$amount).hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $orderNo = this.getOrderNo();
        result = result * 59 + ($orderNo == null ? 43 : $orderNo.hashCode());
        LocalDateTime $paidAt = this.getPaidAt();
        result = result * 59 + ($paidAt == null ? 43 : ((Object)$paidAt).hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "OrderVO(id=" + this.getId() + ", userId=" + this.getUserId() + ", productId=" + this.getProductId() + ", amount=" + this.getAmount() + ", status=" + this.getStatus() + ", quantity=" + this.getQuantity() + ", orderNo=" + this.getOrderNo() + ", paidAt=" + this.getPaidAt() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}
