package com.web3.jarvis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web3.jarvis.entity.VoiceCommand;
import com.web3.jarvis.entity.VoiceSession;
import com.web3.jarvis.mapper.VoiceCommandMapper;
import com.web3.jarvis.mapper.VoiceSessionMapper;
import com.web3.jarvis.vo.VoiceCommandVO;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * Jarvis 语音指令业务服务(jarvis-service 模块)。
 *
 * <p>负责语音指令库的查询与匹配、语音会话的创建/查询/更新,以及将
 * 匹配到的指令通过 RabbitMQ(交换机 jarvis.exchange)发布给下游消费者
 * 执行。{@code @Service} 注解使其被 Spring 容器托管,供 JarvisController
 * 注入使用。</p>
 */
@Service
public class JarvisService {
    /** 语音指令表 Mapper */
    private final VoiceCommandMapper commandMapper;
    /** 语音会话表 Mapper */
    private final VoiceSessionMapper sessionMapper;
    /** RabbitMQ 消息发送模板 */
    private final AmqpTemplate amqpTemplate;
    /** 会话缓存键前缀(预留) */
    private static final String SESSION_PREFIX = "jarvis:session:";
    /** 指令消息交换机名称 */
    private static final String EXCHANGE = "jarvis.exchange";
    /** 指令消息路由键 */
    private static final String ROUTING_KEY = "jarvis.command";

    public JarvisService(VoiceCommandMapper commandMapper, VoiceSessionMapper sessionMapper, AmqpTemplate amqpTemplate) {
        this.commandMapper = commandMapper;
        this.sessionMapper = sessionMapper;
        this.amqpTemplate = amqpTemplate;
    }

    /**
     * 查询全部未删除的语音指令,按排序号升序。
     *
     * @return 语音指令 VO 列表
     */
    public List<VoiceCommandVO> getAllCommands() {
        LambdaQueryWrapper<VoiceCommand> qw = new LambdaQueryWrapper<VoiceCommand>();
        qw.eq(VoiceCommand::getDeleted, (Object)0).orderByAsc(VoiceCommand::getSortOrder);
        List<VoiceCommand> list = this.commandMapper.selectList(qw);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 将用户语音文本(小写后)与指令库的语音触发词做包含匹配,
     * 返回第一个启用的匹配指令。
     *
     * @param text 用户语音文本
     * @return 匹配到的指令 VO;无匹配时返回 null
     */
    public VoiceCommandVO matchCommand(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String lower = text.toLowerCase();
        List<VoiceCommandVO> commands = this.getAllCommands();
        for (VoiceCommandVO cmd : commands) {
            if (cmd.getEnabled() != null && cmd.getEnabled() != 1 || cmd.getVoiceTrigger() == null || !lower.contains(cmd.getVoiceTrigger().toLowerCase())) continue;
            return cmd;
        }
        return null;
    }

    /**
     * 创建新的语音会话:生成无横线的 UUID 作为 sessionId,状态置为 active。
     *
     * @param userId 关联的用户 ID(可为 null)
     * @return 已落库的会话实体
     */
    public VoiceSession createSession(Long userId) {
        VoiceSession session = new VoiceSession();
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(userId);
        session.setStatus("active");
        session.setContext("{}");
        this.sessionMapper.insert(session);
        return session;
    }

    /**
     * 按 sessionId 查询会话(最多返回一条)。
     *
     * @param sessionId 会话 ID
     * @return 会话实体;不存在时返回 null
     */
    public VoiceSession getSession(String sessionId) {
        return this.sessionMapper.selectOne(new LambdaQueryWrapper<VoiceSession>().eq(VoiceSession::getSessionId, (Object)sessionId).last("LIMIT 1"));
    }

    /**
     * 更新会话的最后指令与应答记录。
     *
     * @param sessionId 会话 ID
     * @param command   用户最后发出的指令文本
     * @param response  系统最后的应答文本
     */
    public void updateSession(String sessionId, String command, String response) {
        VoiceSession session = this.getSession(sessionId);
        if (session == null) {
            return;
        }
        session.setLastCommand(command);
        session.setLastResponse(response);
        session.setUpdatedAt(LocalDateTime.now());
        this.sessionMapper.updateById(session);
    }

    /**
     * 将匹配到的指令打包为 JSON 消息,通过 RabbitMQ 发布到
     * jarvis.exchange / jarvis.command,交由下游消费者执行。
     *
     * @param cmd       匹配到的指令
     * @param sessionId 会话 ID
     * @param userId    用户 ID(可为 null)
     * @param context   会话上下文(JSON 字符串)
     * @throws RuntimeException 消息序列化失败时抛出
     */
    public void executeCommand(VoiceCommandVO cmd, String sessionId, Long userId, String context) {
        Map<String, Object> payloadMap = new HashMap<String, Object>();
        payloadMap.put("commandKey", cmd.getCommandKey());
        payloadMap.put("userId", userId);
        payloadMap.put("sessionId", sessionId);
        payloadMap.put("context", context);
        payloadMap.put("timestamp", LocalDateTime.now().toString());
        try {
            this.amqpTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, (Object)new ObjectMapper().writeValueAsString(payloadMap));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize jarvis command payload", e);
        }
    }

    /**
     * 语音指令识别的核心流程:匹配指令 -> 发布 MQ 消息 -> 更新会话 ->
     * 组装应答结果。未匹配时返回兜底的提示语。
     *
     * @param text      用户语音文本
     * @param sessionId 会话 ID
     * @param userId    用户 ID(可为 null)
     * @return 含 sessionId、response、action、targetUrl、commandKey 的 Map
     */
    public Map<String, Object> handleCommand(String text, String sessionId, Long userId) {
        VoiceCommandVO matched = this.matchCommand(text);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("sessionId", sessionId);
        if (matched == null) {
            result.put("response", "抱歉，我没有理解这个指令。你可以试试说“打开社区”、“打开商城”、“现在几点”等。");
            result.put("action", null);
            result.put("targetUrl", null);
            return result;
        }
        String context = "{}";
        try {
            this.executeCommand(matched, sessionId, userId, context);
        }
        catch (RuntimeException e) {
            // MQ 不可用时不影响语音应答
        }
        Object response = matched.getTtsResponse() != null ? matched.getTtsResponse() : "Done: " + matched.getDescription();
        this.updateSession(sessionId, text, (String)response);
        result.put("response", response);
        result.put("action", matched.getActionType());
        result.put("targetUrl", matched.getTargetUrl());
        result.put("commandKey", matched.getCommandKey());
        return result;
    }

    /**
     * 指令实体转换为展示层 VO(同名属性拷贝)。
     *
     * @param entity 指令实体
     * @return 指令 VO
     */
    private VoiceCommandVO toVO(VoiceCommand entity) {
        VoiceCommandVO vo = new VoiceCommandVO();
        BeanUtils.copyProperties((Object)entity, (Object)vo);
        return vo;
    }
}
