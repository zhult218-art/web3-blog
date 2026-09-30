package com.web3.tool.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实用脚本实体类,对应数据库表 {@code script}。
 *
 * <p>保存网站"工具"页面的脚本内容(名称、描述、源码、分类、语言、版本等)。
 * id 主键由 MyBatis-Plus 自动分配;createdAt 插入时自动填充;
 * deleted 标注 {@code @TableLogic} 逻辑删除。</p>
 */
@Data
@TableName(value="script")
public class Script {
    /**
     * 脚本主键(雪花 ID,自动分配)
     */
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    /** 脚本名称 */
    private String name;
    /** 脚本描述 */
    private String description;
    /** 脚本源码内容 */
    private String content;
    /** 脚本分类 */
    private String category;
    /** 脚本语言,如 Python/Shell */
    private String language;
    /** 脚本版本号 */
    private String version;
    /** 下载次数 */
    private Long downloadCount;
    /** 脚本标签 */
    private String tags;
    /** 创建时间(插入时自动填充) */
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 逻辑删除标记(0 正常,1 已删除) */
    @TableLogic
    private Integer deleted;
}
