/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.annotation.FieldFill
 *  com.baomidou.mybatisplus.annotation.IdType
 *  com.baomidou.mybatisplus.annotation.TableField
 *  com.baomidou.mybatisplus.annotation.TableId
 *  com.baomidou.mybatisplus.annotation.TableLogic
 *  com.baomidou.mybatisplus.annotation.TableName
 */
package com.web3.shop.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order —— 订单实体类
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 对应数据库表：`order`（订单表，order 为 MySQL 保留字故使用反引号）。
 * <p>
 * 字段说明：id 为雪花算法主键；amount 订单金额（单价 x 数量）；status 状态
 * （PENDING 待支付/PAID 已支付等）；orderNo 订单号；paidAt 支付时间；
 * deleted 为逻辑删除标记（@TableLogic 自动过滤）；createdAt 插入时自动填充。
 */
@TableName(value="`order`")
public class Order {
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long productId;
    private BigDecimal amount;
    private String status;
    private Integer quantity;
    private String orderNo;
    private LocalDateTime paidAt;
    /** 支付渠道（alipay/wechat），支付成功时记录 */
    private String payChannel;
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;

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

    public Integer getDeleted() {
        return this.deleted;
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

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Order)) {
            return false;
        }
        Order other = (Order)o;
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
        Integer this$deleted = this.getDeleted();
        Integer other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !((Object)this$deleted).equals(other$deleted)) {
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
        return other instanceof Order;
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
        Integer $deleted = this.getDeleted();
        result = result * 59 + ($deleted == null ? 43 : ((Object)$deleted).hashCode());
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
        return "Order(id=" + this.getId() + ", userId=" + this.getUserId() + ", productId=" + this.getProductId() + ", amount=" + this.getAmount() + ", status=" + this.getStatus() + ", quantity=" + this.getQuantity() + ", orderNo=" + this.getOrderNo() + ", paidAt=" + this.getPaidAt() + ", createdAt=" + this.getCreatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}
