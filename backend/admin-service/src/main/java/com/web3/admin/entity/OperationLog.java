/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.annotation.IdType
 *  com.baomidou.mybatisplus.annotation.TableId
 *  com.baomidou.mybatisplus.annotation.TableLogic
 *  com.baomidou.mybatisplus.annotation.TableName
 */
package com.web3.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 类名：OperationLog
 * 所属模块：admin-service（管理后台服务）
 * 职责：管理操作日志实体类，对应数据库表 admin_operation_log，记录管理员的后台操作明细（谁在何时对什么做了什么）。
 * 关键注解：@TableName("admin_operation_log") 指定表名；@TableId(type = AUTO) 自增主键；deleted=逻辑删除标记（@TableLogic）。
 */
@TableName(value="admin_operation_log")
public class OperationLog {
    @TableId(type=IdType.AUTO)
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

    public Long getId() {
        return this.id;
    }

    public Long getOperatorId() {
        return this.operatorId;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public String getModule() {
        return this.module;
    }

    public String getAction() {
        return this.action;
    }

    public String getTargetId() {
        return this.targetId;
    }

    public String getTargetName() {
        return this.targetName;
    }

    public String getDetail() {
        return this.detail;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof OperationLog)) {
            return false;
        }
        OperationLog other = (OperationLog)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Long this$operatorId = this.getOperatorId();
        Long other$operatorId = other.getOperatorId();
        if (this$operatorId == null ? other$operatorId != null : !((Object)this$operatorId).equals(other$operatorId)) {
            return false;
        }
        Integer this$deleted = this.getDeleted();
        Integer other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !((Object)this$deleted).equals(other$deleted)) {
            return false;
        }
        String this$operatorName = this.getOperatorName();
        String other$operatorName = other.getOperatorName();
        if (this$operatorName == null ? other$operatorName != null : !this$operatorName.equals(other$operatorName)) {
            return false;
        }
        String this$module = this.getModule();
        String other$module = other.getModule();
        if (this$module == null ? other$module != null : !this$module.equals(other$module)) {
            return false;
        }
        String this$action = this.getAction();
        String other$action = other.getAction();
        if (this$action == null ? other$action != null : !this$action.equals(other$action)) {
            return false;
        }
        String this$targetId = this.getTargetId();
        String other$targetId = other.getTargetId();
        if (this$targetId == null ? other$targetId != null : !this$targetId.equals(other$targetId)) {
            return false;
        }
        String this$targetName = this.getTargetName();
        String other$targetName = other.getTargetName();
        if (this$targetName == null ? other$targetName != null : !this$targetName.equals(other$targetName)) {
            return false;
        }
        String this$detail = this.getDetail();
        String other$detail = other.getDetail();
        if (this$detail == null ? other$detail != null : !this$detail.equals(other$detail)) {
            return false;
        }
        String this$ipAddress = this.getIpAddress();
        String other$ipAddress = other.getIpAddress();
        if (this$ipAddress == null ? other$ipAddress != null : !this$ipAddress.equals(other$ipAddress)) {
            return false;
        }
        String this$userAgent = this.getUserAgent();
        String other$userAgent = other.getUserAgent();
        if (this$userAgent == null ? other$userAgent != null : !this$userAgent.equals(other$userAgent)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof OperationLog;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $operatorId = this.getOperatorId();
        result = result * 59 + ($operatorId == null ? 43 : ((Object)$operatorId).hashCode());
        Integer $deleted = this.getDeleted();
        result = result * 59 + ($deleted == null ? 43 : ((Object)$deleted).hashCode());
        String $operatorName = this.getOperatorName();
        result = result * 59 + ($operatorName == null ? 43 : $operatorName.hashCode());
        String $module = this.getModule();
        result = result * 59 + ($module == null ? 43 : $module.hashCode());
        String $action = this.getAction();
        result = result * 59 + ($action == null ? 43 : $action.hashCode());
        String $targetId = this.getTargetId();
        result = result * 59 + ($targetId == null ? 43 : $targetId.hashCode());
        String $targetName = this.getTargetName();
        result = result * 59 + ($targetName == null ? 43 : $targetName.hashCode());
        String $detail = this.getDetail();
        result = result * 59 + ($detail == null ? 43 : $detail.hashCode());
        String $ipAddress = this.getIpAddress();
        result = result * 59 + ($ipAddress == null ? 43 : $ipAddress.hashCode());
        String $userAgent = this.getUserAgent();
        result = result * 59 + ($userAgent == null ? 43 : $userAgent.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "OperationLog(id=" + this.getId() + ", operatorId=" + this.getOperatorId() + ", operatorName=" + this.getOperatorName() + ", module=" + this.getModule() + ", action=" + this.getAction() + ", targetId=" + this.getTargetId() + ", targetName=" + this.getTargetName() + ", detail=" + this.getDetail() + ", ipAddress=" + this.getIpAddress() + ", userAgent=" + this.getUserAgent() + ", createdAt=" + this.getCreatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}
