package com.web3.media.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * NailBooking —— 美甲预约实体类
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 对应数据库表：nail_booking（美甲预约表）。
 * <p>
 * 字段说明：id 为数据库自增主键；name/phone 为预约人姓名与电话；date/timeSlot 为预约日期与时段；
 * status 为预约状态（PENDING/CONFIRMED/DONE/CANCELLED）；createdAt 插入时自动填充。
 */
@Data
@TableName(value="nail_booking")
public class NailBooking {
    @TableId(type=IdType.AUTO)
    private Long id;
    private String name;
    private String phone;
    private String service;
    private String date;
    private String timeSlot;
    private String note;
    private String status;
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
}
