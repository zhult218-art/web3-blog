/*
 * Decompiled with CFR 0.152.
 */
package com.web3.jarvis.vo;

/**
 * 语音指令展示层 VO(jarvis-service 模块)。
 *
 * <p>语音指令接口返回给前端的模型,由
 * {@link com.web3.jarvis.service.JarvisService} 从
 * {@link com.web3.jarvis.entity.VoiceCommand} 转换而来,不包含
 * 创建/更新时间与逻辑删除标记。</p>
 */
public class VoiceCommandVO {
    /** 指令唯一键 */
    private String commandKey;
    /** 指令描述 */
    private String description;
    /** 动作类型 */
    private String actionType;
    /** 目标服务名 */
    private String targetService;
    /** 目标跳转 URL */
    private String targetUrl;
    /** 语音触发词 */
    private String voiceTrigger;
    /** TTS 语音应答文本 */
    private String ttsResponse;
    /** 排序号 */
    private Integer sortOrder;
    /** 是否启用(1 启用) */
    private Integer enabled;

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

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof VoiceCommandVO)) {
            return false;
        }
        VoiceCommandVO other = (VoiceCommandVO)o;
        if (!other.canEqual(this)) {
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
        String this$voiceTrigger = this.getVoiceTrigger();
        String other$voiceTrigger = other.getVoiceTrigger();
        if (this$voiceTrigger == null ? other$voiceTrigger != null : !this$voiceTrigger.equals(other$voiceTrigger)) {
            return false;
        }
        String this$ttsResponse = this.getTtsResponse();
        String other$ttsResponse = other.getTtsResponse();
        return !(this$ttsResponse == null ? other$ttsResponse != null : !this$ttsResponse.equals(other$ttsResponse));
    }

    protected boolean canEqual(Object other) {
        return other instanceof VoiceCommandVO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $sortOrder = this.getSortOrder();
        result = result * 59 + ($sortOrder == null ? 43 : ((Object)$sortOrder).hashCode());
        Integer $enabled = this.getEnabled();
        result = result * 59 + ($enabled == null ? 43 : ((Object)$enabled).hashCode());
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
        String $voiceTrigger = this.getVoiceTrigger();
        result = result * 59 + ($voiceTrigger == null ? 43 : $voiceTrigger.hashCode());
        String $ttsResponse = this.getTtsResponse();
        result = result * 59 + ($ttsResponse == null ? 43 : $ttsResponse.hashCode());
        return result;
    }

    public String toString() {
        return "VoiceCommandVO(commandKey=" + this.getCommandKey() + ", description=" + this.getDescription() + ", actionType=" + this.getActionType() + ", targetService=" + this.getTargetService() + ", targetUrl=" + this.getTargetUrl() + ", voiceTrigger=" + this.getVoiceTrigger() + ", ttsResponse=" + this.getTtsResponse() + ", sortOrder=" + this.getSortOrder() + ", enabled=" + this.getEnabled() + ")";
    }
}
