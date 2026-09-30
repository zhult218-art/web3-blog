package com.web3.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类名：DashboardStat
 * 所属模块：admin-service（管理后台服务）
 * 职责：控制台统计项实体类，对应数据库表 {@code admin_dashboard_stat}，存储各项统计指标（键值对形式）。
 * 关键注解：{@code @TableName("admin_dashboard_stat")} 指定表名；{@code @TableId(type = AUTO)} 自增主键；
 *           {@code @TableLogic} 标记 deleted 字段为逻辑删除字段。
 * 字段说明：statKey=指标名（如 totalUsers，service:* 前缀为各服务状态），statValue=指标值（字符串），
 *           category=分类，statDate=统计日期。
 */
@Data
@TableName("admin_dashboard_stat")
public class DashboardStat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String statKey;
    private String statValue;
    private String category;
    private LocalDateTime statDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
