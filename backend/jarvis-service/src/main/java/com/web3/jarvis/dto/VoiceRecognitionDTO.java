/*
 * Decompiled with CFR 0.152.
 */
package com.web3.jarvis.dto;

/**
 * 语音识别请求 DTO(jarvis-service 模块)。
 *
 * <p>语音识别接口的入参模型(预留),包含会话 ID、识别文本、用户 ID
 * 与语言类型。</p>
 */
public class VoiceRecognitionDTO {
    /** 会话 ID */
    private String sessionId;
    /** 识别出的语音文本 */
    private String text;
    /** 用户 ID(可为 null) */
    private Long userId;
    /** 语言类型,如 zh-CN */
    private String language;

    public String getSessionId() {
        return this.sessionId;
    }

    public String getText() {
        return this.text;
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getLanguage() {
        return this.language;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof VoiceRecognitionDTO)) {
            return false;
        }
        VoiceRecognitionDTO other = (VoiceRecognitionDTO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$userId = this.getUserId();
        Long other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !((Object)this$userId).equals(other$userId)) {
            return false;
        }
        String this$sessionId = this.getSessionId();
        String other$sessionId = other.getSessionId();
        if (this$sessionId == null ? other$sessionId != null : !this$sessionId.equals(other$sessionId)) {
            return false;
        }
        String this$text = this.getText();
        String other$text = other.getText();
        if (this$text == null ? other$text != null : !this$text.equals(other$text)) {
            return false;
        }
        String this$language = this.getLanguage();
        String other$language = other.getLanguage();
        return !(this$language == null ? other$language != null : !this$language.equals(other$language));
    }

    protected boolean canEqual(Object other) {
        return other instanceof VoiceRecognitionDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : ((Object)$userId).hashCode());
        String $sessionId = this.getSessionId();
        result = result * 59 + ($sessionId == null ? 43 : $sessionId.hashCode());
        String $text = this.getText();
        result = result * 59 + ($text == null ? 43 : $text.hashCode());
        String $language = this.getLanguage();
        result = result * 59 + ($language == null ? 43 : $language.hashCode());
        return result;
    }

    public String toString() {
        return "VoiceRecognitionDTO(sessionId=" + this.getSessionId() + ", text=" + this.getText() + ", userId=" + this.getUserId() + ", language=" + this.getLanguage() + ")";
    }
}
