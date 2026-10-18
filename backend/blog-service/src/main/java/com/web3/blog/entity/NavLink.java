package com.web3.blog.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * NavLink —— 导航条目实体类
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 对应数据库表：nav_link（导航条目表），一条记录代表招募亭里的一家企业或
 * 赏金猎人里的一个赚钱平台。description 为鼠标悬停时展示的详细介绍；
 * iconUrl 为平台图标地址，为空时由前端按官网域名自动取 favicon，取不到再回退首字母色块。
 */
@Data
@TableName(value = "nav_link")
public class NavLink {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 所属页面：jobs（招募亭） / earn（赏金猎人） */
    private String page;
    /** 所属分类键，对应 nav_category.catKey */
    private String catKey;
    /** 名称（企业名 / 平台名） */
    private String name;
    /** 官网链接 */
    private String url;
    /** 短标签，如「电网」「提交漏洞获赏金」 */
    private String tag;
    /** 详细介绍：鼠标悬停卡片时展示 */
    private String description;
    /** 图标地址，可为空（空则前端自动取官网 favicon） */
    private String iconUrl;
    /** 排序值，越小越靠前 */
    private Integer sort;
    /** 是否启用：1=启用，0=下架 */
    private Integer enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
