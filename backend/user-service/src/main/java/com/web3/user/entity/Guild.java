package com.web3.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("guild")
public class Guild {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String description;
    private String emblem;
    private Long masterUserId;
    private Integer maxMembers;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEmblem() { return emblem; }
    public void setEmblem(String emblem) { this.emblem = emblem; }
    public Long getMasterUserId() { return masterUserId; }
    public void setMasterUserId(Long masterUserId) { this.masterUserId = masterUserId; }
    public Integer getMaxMembers() { return maxMembers; }
    public void setMaxMembers(Integer maxMembers) { this.maxMembers = maxMembers; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
