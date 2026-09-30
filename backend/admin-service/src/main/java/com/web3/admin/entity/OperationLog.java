package com.web3.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类名：OperationLog
 * 所属模块：admin-service（管理后台服务）
 * 职责：管理操作日志实体类，对应数据库表 {@code admin_operation_log}，
 *       记录管理员的后台操作明细（谁在何时对什么做了什么）。
 *
 * <p>2026-09-17 重写样板（CFR 反编译 244 行 → Lombok @Data 30 行）：</p>
 * <ul>
 *   <li>{@code @Data} 自动生成 getter/setter/equals/hashCode/toString；
 *       原 CFR 反编译手写的 equals/hashCode/toString 全部移除，行为等价。</li>
 *   <li>{@code @TableId(type = AUTO)} 主键自增；{@code @TableLogic} 标记 deleted 字段
 *       为逻辑删除字段。</li>
 *   <li>数据库表 2026-09-17 同步对齐：原 SQL 建表 {@code operation_log}（无 admin_ 前缀），
 *       与实体 @TableName 不一致；已改 SQL 为 {@code admin_operation_log}，
 *       见 [10-web3_admin.sql](../../deploy/mysql/init/10-web3_admin.sql) 与
 *       迁移 [migrate-202609-admin-log.sql](../../deploy/mysql/migrate-202609-admin-log.sql)。</li>
 * </ul>
 *
 * <p>历史沿革：本类原为 jar 反编译产物（244 行，含手写 equals/hashCode/toString 与
 * 反编译头注释），2026-09-17 重写为 Lombok {@code @Data} 样板。反编译历史与其余文件的
 * 重写进度统一记录在 backend/DECOMPILED-SOURCES.md。
 */
@Data
@TableName("admin_operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private String operatorName;
    private String module;
    private String action;
    private String targetId;
    private String targetName;
    private String detail;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}
