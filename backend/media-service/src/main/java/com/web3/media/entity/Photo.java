package com.web3.media.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Photo —— 相册照片实体类
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 对应数据库表：photo（相册照片表）。
 * <p>
 * 字段说明：id 为数据库自增主键；url 为图片访问地址（/photo/file/{filename}）；
 * width/height 为图片尺寸；likes 为点赞数；uploader 为上传者用户名；
 * deleted 为逻辑删除标记（@TableLogic 自动过滤）；createdAt 插入时自动填充。
 */
@Data
@TableName(value="photo")
public class Photo {
    @TableId(type=IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String url;
    private Integer width;
    private Integer height;
    private Integer likes;
    private String uploader;
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}
