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
 * Order —— 订单实体类
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 对应数据库表：{@code `order`}（订单表，order 为 MySQL 保留字故使用反引号）。
 * <p>
 * 字段说明：id 为雪花算法主键；amount 订单金额（单价 x 数量）；status 状态
 * （PENDING 待支付/PAID 已支付等）；orderNo 订单号；paidAt 支付时间；
 * deleted 为逻辑删除标记（{@code @TableLogic} 自动过滤）；createdAt 插入时自动填充。
 */
@Data
@TableName("`order`")
public class Order {
    @TableId(type = IdType.ASSIGN_ID)
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
    /** 第三方交易号（支付宝 trade_no / 微信 transaction_id），支付回调时回填 */
    private String tradeNo;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}
