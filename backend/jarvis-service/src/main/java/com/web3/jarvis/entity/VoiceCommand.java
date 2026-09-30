package com.web3.jarvis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 语音指令实体类,对应数据库表 {@code voice_command}。
 *
 * <p>所属模块：jarvis-service（语音助手模块）。</p>
 *
 * <p>一条指令定义语音触发词与对应的动作(动作类型、目标服务/URL、
 * TTS 应答文本等)。id 为自增主键;createdAt 插入时自动填充、
 * updatedAt 更新时自动填充;deleted 标注 {@code @TableLogic} 逻辑删除。</p>
 *
 * <p>关键注解作用：
 * <ul>
 *   <li>{@code @Data} 自动生成 getter/setter/equals/hashCode/toString；</li>
 *   <li>{@code @TableId(type = AUTO)} 数据库自增主键；{@code @TableLogic} 逻辑删除；{@code @TableField(fill=...)} 自动填充。</li>
 * </ul>
 */
@Data
@TableName(value = "voice_command")
public class VoiceCommand {
    /** 主键(数据库自增) */
    @TableId(type = IdType.AUTO)
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 更新时间(更新时自动填充) */
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
    /** 逻辑删除标记(0 正常,1 已删除) */
    @TableLogic
    private Integer deleted;
}
