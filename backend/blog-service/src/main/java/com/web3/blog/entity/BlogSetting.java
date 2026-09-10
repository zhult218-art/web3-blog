package com.web3.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * BlogSetting —— 博客站点配置实体类
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 对应数据库表：blog_setting（键值配置表），settingValue 为 JSON 字符串。
 */
@TableName(value="blog_setting")
public class BlogSetting {
    @TableId(type=IdType.INPUT)
    private String settingKey;
    private String settingValue;
    private LocalDateTime updatedAt;

    public String getSettingKey() {
        return this.settingKey;
    }

    public String getSettingValue() {
        return this.settingValue;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
