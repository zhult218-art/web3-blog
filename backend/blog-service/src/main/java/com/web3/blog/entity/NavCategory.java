package com.web3.blog.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * NavCategory —— 导航分类实体类
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 对应数据库表：nav_category（导航分类表），服务于「冒险者招募亭 /jobs」与
 * 「赏金猎人 /earn」两个导航页，用 page 字段区分所属页面。
 * <p>
 * 字段说明：page 为页面标识（jobs/earn）；catKey 为分类键，与 nav_link.catKey 关联；
 * icon 为分类图标（emoji）；description 为该分类的说明文案；sort 控制展示顺序；enabled 为启用开关。
 */
@Data
@TableName(value = "nav_category")
public class NavCategory {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 所属页面：jobs（招募亭） / earn（赏金猎人） */
    private String page;
    /** 分类键，如 central / bounty */
    private String catKey;
    /** 分类名称 */
    private String name;
    /** 分类图标（emoji） */
    private String icon;
    /** 分类说明 */
    private String description;
    /** 排序值，越小越靠前 */
    private Integer sort;
    /** 是否启用：1=启用，0=下架 */
    private Integer enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
