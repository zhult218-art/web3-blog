package com.web3.jarvis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 语音会话实体类,对应数据库表 {@code voice_session}。
 *
 * <p>所属模块：jarvis-service（语音助手模块）。</p>
 *
 * <p>记录一次 Jarvis 语音对话的会话状态(会话 ID、关联用户、会话状态、
 * 最后指令与应答、上下文 JSON 等)。id 为自增主键;
 * deleted 标注 {@code @TableLogic} 逻辑删除。</p>
 *
 * <p>关键注解作用：
 * <ul>
 *   <li>{@code @Data} 自动生成 getter/setter/equals/hashCode/toString；</li>
 *   <li>{@code @TableId(type = AUTO)} 数据库自增主键；{@code @TableLogic} 逻辑删除。</li>
 * </ul>
 */
@Data
@TableName(value = "voice_session")
public class VoiceSession {
    /** 主键(数据库自增) */
    @TableId(type = IdType.AUTO)
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
}
