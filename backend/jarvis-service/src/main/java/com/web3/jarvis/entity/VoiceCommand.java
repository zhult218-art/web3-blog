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
 * 语音指令实体类,对应数据库表 {@code voice_command}。
 *
 * <p>一条指令定义语音触发词与对应的动作(动作类型、目标服务/URL、
 * TTS 应答文本等)。id 为自增主键;createdAt 插入时自动填充、
 * updatedAt 更新时自动填充;deleted 标注 {@code @TableLogic} 逻辑删除。</p>
 */
@TableName(value="voice_command")
public class VoiceCommand {
    /**
     * 主键(数据库自增)
     */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 指令唯一键,如 OPEN_COMMUNITY */
    private String commandKey;
    /** 指令描述 */
    private String description;
    /** 动作类型,如 NAVIGATE(跳转)/QUERY(查询) */
    private String actionType;
    /** 目标服务名 */
    private String targetService;
    /** 目标跳转 URL */
    private String targetUrl;
    /** 参数模板(预留) */
    private String paramsTemplate;
    /** 语音触发词,如 "打开社区" */
    private String voiceTrigger;
    /** TTS 语音应答文本 */
    private String ttsResponse;
    /** 排序号(升序展示) */
    private Integer sortOrder;
    /** 是否启用(1 启用,0/非 1 停用) */
    private Integer enabled;
    /** 创建时间(插入时自动填充) */
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 更新时间(更新时自动填充) */
    @TableField(fill=FieldFill.UPDATE)
    private LocalDateTime updatedAt;
    /** 逻辑删除标记(0 正常,1 已删除) */
    @TableLogic
    private Integer deleted;

    public Long getId() {
        return this.id;
    }

    public String getCommandKey() {
        return this.commandKey;
    }

    public String getDescription() {
        return this.description;
    }

    public String getActionType() {
        return this.actionType;
    }

    public String getTargetService() {
        return this.targetService;
    }

    public String getTargetUrl() {
        return this.targetUrl;
    }

    public String getParamsTemplate() {
        return this.paramsTemplate;
    }

    public String getVoiceTrigger() {
        return this.voiceTrigger;
    }

    public String getTtsResponse() {
        return this.ttsResponse;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public Integer getEnabled() {
        return this.enabled;
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

    public void setCommandKey(String commandKey) {
        this.commandKey = commandKey;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public void setParamsTemplate(String paramsTemplate) {
        this.paramsTemplate = paramsTemplate;
    }

    public void setVoiceTrigger(String voiceTrigger) {
        this.voiceTrigger = voiceTrigger;
    }

    public void setTtsResponse(String ttsResponse) {
        this.ttsResponse = ttsResponse;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
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
        if (!(o instanceof VoiceCommand)) {
            return false;
        }
        VoiceCommand other = (VoiceCommand)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Integer this$sortOrder = this.getSortOrder();
        Integer other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !((Object)this$sortOrder).equals(other$sortOrder)) {
            return false;
        }
        Integer this$enabled = this.getEnabled();
        Integer other$enabled = other.getEnabled();
        if (this$enabled == null ? other$enabled != null : !((Object)this$enabled).equals(other$enabled)) {
            return false;
        }
        Integer this$deleted = this.getDeleted();
        Integer other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !((Object)this$deleted).equals(other$deleted)) {
            return false;
        }
        String this$commandKey = this.getCommandKey();
        String other$commandKey = other.getCommandKey();
        if (this$commandKey == null ? other$commandKey != null : !this$commandKey.equals(other$commandKey)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$actionType = this.getActionType();
        String other$actionType = other.getActionType();
        if (this$actionType == null ? other$actionType != null : !this$actionType.equals(other$actionType)) {
            return false;
        }
        String this$targetService = this.getTargetService();
        String other$targetService = other.getTargetService();
        if (this$targetService == null ? other$targetService != null : !this$targetService.equals(other$targetService)) {
            return false;
        }
        String this$targetUrl = this.getTargetUrl();
        String other$targetUrl = other.getTargetUrl();
        if (this$targetUrl == null ? other$targetUrl != null : !this$targetUrl.equals(other$targetUrl)) {
            return false;
        }
        String this$paramsTemplate = this.getParamsTemplate();
        String other$paramsTemplate = other.getParamsTemplate();
        if (this$paramsTemplate == null ? other$paramsTemplate != null : !this$paramsTemplate.equals(other$paramsTemplate)) {
            return false;
        }
        String this$voiceTrigger = this.getVoiceTrigger();
        String other$voiceTrigger = other.getVoiceTrigger();
        if (this$voiceTrigger == null ? other$voiceTrigger != null : !this$voiceTrigger.equals(other$voiceTrigger)) {
            return false;
        }
        String this$ttsResponse = this.getTtsResponse();
        String other$ttsResponse = other.getTtsResponse();
        if (this$ttsResponse == null ? other$ttsResponse != null : !this$ttsResponse.equals(other$ttsResponse)) {
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
        return other instanceof VoiceCommand;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $sortOrder = this.getSortOrder();
        result = result * 59 + ($sortOrder == null ? 43 : ((Object)$sortOrder).hashCode());
        Integer $enabled = this.getEnabled();
        result = result * 59 + ($enabled == null ? 43 : ((Object)$enabled).hashCode());
        Integer $deleted = this.getDeleted();
        result = result * 59 + ($deleted == null ? 43 : ((Object)$deleted).hashCode());
        String $commandKey = this.getCommandKey();
        result = result * 59 + ($commandKey == null ? 43 : $commandKey.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $actionType = this.getActionType();
        result = result * 59 + ($actionType == null ? 43 : $actionType.hashCode());
        String $targetService = this.getTargetService();
        result = result * 59 + ($targetService == null ? 43 : $targetService.hashCode());
        String $targetUrl = this.getTargetUrl();
        result = result * 59 + ($targetUrl == null ? 43 : $targetUrl.hashCode());
        String $paramsTemplate = this.getParamsTemplate();
        result = result * 59 + ($paramsTemplate == null ? 43 : $paramsTemplate.hashCode());
        String $voiceTrigger = this.getVoiceTrigger();
        result = result * 59 + ($voiceTrigger == null ? 43 : $voiceTrigger.hashCode());
        String $ttsResponse = this.getTtsResponse();
        result = result * 59 + ($ttsResponse == null ? 43 : $ttsResponse.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "VoiceCommand(id=" + this.getId() + ", commandKey=" + this.getCommandKey() + ", description=" + this.getDescription() + ", actionType=" + this.getActionType() + ", targetService=" + this.getTargetService() + ", targetUrl=" + this.getTargetUrl() + ", paramsTemplate=" + this.getParamsTemplate() + ", voiceTrigger=" + this.getVoiceTrigger() + ", ttsResponse=" + this.getTtsResponse() + ", sortOrder=" + this.getSortOrder() + ", enabled=" + this.getEnabled() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}
