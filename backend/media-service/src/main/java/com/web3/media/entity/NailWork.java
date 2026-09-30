package com.web3.media.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * NailWork —— 美甲作品实体类
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 对应数据库表：nail_work（美甲作品表）。
 * <p>
 * 字段说明：id 为数据库自增主键；tags 为标签（逗号分隔字符串）；price 为价格；
 * img 为作品图（dataURL 或 URL，MEDIUMTEXT 存储）；likes 为点赞数；owner 为发布者用户名；
 * deleted 为逻辑删除标记（@TableLogic 自动过滤）；createdAt 插入时自动填充。
 */
@Data
@TableName(value="nail_work")
public class NailWork {
    @TableId(type=IdType.AUTO)
    private Long id;
    private String title;
    private String tags;
    private BigDecimal price;
    private String img;
    private Integer likes;
    private String owner;
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}
