package com.web3.media.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * NailProfile —— 美甲小铺店主资料实体类
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 对应数据库表：nail_profile（美甲店主资料表，单行记录，固定 id=1）。
 * <p>
 * 字段说明：id 固定为 1；sign 为店铺签名；loc 为店铺位置；hours 为营业时间；
 * wx/qq/tel 为联系方式；updatedAt 由数据库 ON UPDATE 自动维护。
 */
@Data
@TableName(value="nail_profile")
public class NailProfile {
    @TableId(type=IdType.INPUT)
    private Long id;
    private String name;
    private String sign;
    private String loc;
    private String hours;
    private String wx;
    private String qq;
    private String tel;
    private LocalDateTime updatedAt;
}
