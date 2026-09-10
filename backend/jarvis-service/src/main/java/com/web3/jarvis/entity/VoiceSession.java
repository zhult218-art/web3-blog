/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.annotation.FieldFill
 *  com.baomidou.mybatisplus.annotation.IdType
 *  com.baomidou.mybatisplus.annotation.TableField
 *  com.baomidou.mybatisplus.annotation.TableId
 *  com.baomidou.mybatisplus.annotation.TableLogic
 *  com.baomidou.mybatisplus.annotation.TableName
 */
package com.web3.jarvis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 语音会话实体类,对应数据库表 {@code voice_session}。
 *
 * <p>记录一次 Jarvis 语音对话的会话状态(会话 ID、关联用户、会话状态、
 * 最后指令与应答、上下文 JSON 等)。id 为自增主键;
 * deleted 标注 {@code @TableLogic} 逻辑删除。</p>
 */
@TableName(value="voice_session")
public class VoiceSession {
    /**
     * 主键(数据库自增)
     */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 对外会话 ID(UUID,无横线) */
    private String sessionId;
    /** 关联的用户 ID(可为 null) */
    private Long userId;
    /** 会话状态,如 active */
    private String status;
    /** 最后发出的指令文本 */
    private String lastCommand;
    /** 最后收到的应答文本 */
    private String lastResponse;
    /** 会话上下文(JSON 字符串) */
    private String context;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
    /** 逻辑删除标记(0 正常,1 已删除) */
    @TableLogic
    private Integer deleted;

    public Long getId() {
        return this.id;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getStatus() {
        return this.status;
    }

    public String getLastCommand() {
        return this.lastCommand;
    }

    public String getLastResponse() {
        return this.lastResponse;
    }

    public String getContext() {
        return this.context;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLastCommand(String lastCommand) {
        this.lastCommand = lastCommand;
    }

    public void setLastResponse(String lastResponse) {
        this.lastResponse = lastResponse;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof VoiceSession)) {
            return false;
        }
        VoiceSession other = (VoiceSession)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Long this$userId = this.getUserId();
        Long other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !((Object)this$userId).equals(other$userId)) {
            return false;
        }
        Integer this$deleted = this.getDeleted();
        Integer other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !((Object)this$deleted).equals(other$deleted)) {
            return false;
        }
        String this$sessionId = this.getSessionId();
        String other$sessionId = other.getSessionId();
        if (this$sessionId == null ? other$sessionId != null : !this$sessionId.equals(other$sessionId)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$lastCommand = this.getLastCommand();
        String other$lastCommand = other.getLastCommand();
        if (this$lastCommand == null ? other$lastCommand != null : !this$lastCommand.equals(other$lastCommand)) {
            return false;
        }
        String this$lastResponse = this.getLastResponse();
        String other$lastResponse = other.getLastResponse();
        if (this$lastResponse == null ? other$lastResponse != null : !this$lastResponse.equals(other$lastResponse)) {
            return false;
        }
        String this$context = this.getContext();
        String other$context = other.getContext();
        if (this$context == null ? other$context != null : !this$context.equals(other$context)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt)) {
            return false;
        }
        LocalDateTime this$updatedAt = this.getUpdatedAt();
        LocalDateTime other$updatedAt = other.getUpdatedAt();
        return !(this$updatedAt == null ? other$updatedAt != null : !((Object)this$updatedAt).equals(other$updatedAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof VoiceSession;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : ((Object)$userId).hashCode());
        Integer $deleted = this.getDeleted();
        result = result * 59 + ($deleted == null ? 43 : ((Object)$deleted).hashCode());
        String $sessionId = this.getSessionId();
        result = result * 59 + ($sessionId == null ? 43 : $sessionId.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $lastCommand = this.getLastCommand();
        result = result * 59 + ($lastCommand == null ? 43 : $lastCommand.hashCode());
        String $lastResponse = this.getLastResponse();
        result = result * 59 + ($lastResponse == null ? 43 : $lastResponse.hashCode());
        String $context = this.getContext();
        result = result * 59 + ($context == null ? 43 : $context.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "VoiceSession(id=" + this.getId() + ", sessionId=" + this.getSessionId() + ", userId=" + this.getUserId() + ", status=" + this.getStatus() + ", lastCommand=" + this.getLastCommand() + ", lastResponse=" + this.getLastResponse() + ", context=" + this.getContext() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}
