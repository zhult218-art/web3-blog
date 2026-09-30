package com.web3.jarvis.vo;

import lombok.Data;

/**
 * 语音指令展示层 VO(jarvis-service 模块)。
 *
 * <p>语音指令接口返回给前端的模型,由
 * {@link com.web3.jarvis.service.JarvisService} 从
 * {@link com.web3.jarvis.entity.VoiceCommand} 转换而来,不包含
 * 创建/更新时间与逻辑删除标记。</p>
 *
 * <p>关键注解作用：{@code @Data} 自动生成 getter/setter/equals/hashCode/toString。</p>
 */
@Data
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
}
