package com.web3.jarvis.dto;

import lombok.Data;

/**
 * 语音识别请求 DTO(jarvis-service 模块)。
 *
 * <p>语音识别接口的入参模型(预留),包含会话 ID、识别文本、用户 ID
 * 与语言类型。</p>
 *
 * <p>关键注解作用：{@code @Data} 自动生成 getter/setter/equals/hashCode/toString。</p>
 */
@Data
public class VoiceRecognitionDTO {
    /** 会话 ID */
    private String sessionId;
    /** 识别出的语音文本 */
    private String text;
    /** 用户 ID(可为 null) */
    private Long userId;
    /** 语言类型,如 zh-CN */
    private String language;
}
