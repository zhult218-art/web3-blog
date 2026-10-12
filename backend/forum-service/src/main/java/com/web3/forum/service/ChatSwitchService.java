package com.web3.forum.service;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 群聊开关服务：管理员可一键关闭/恢复群聊。
 * 状态持久化在 chat_config 表（key=chatEnabled），内存 volatile 缓存；
 * 关闭后 WebSocket 新连接被拒绝、已有连接发消息返回错误提示，前端同步隐藏聊天入口。
 */
@Service
public class ChatSwitchService {

    private static final Logger log = LoggerFactory.getLogger(ChatSwitchService.class);
    private static final String KEY = "chatEnabled";

    private final JdbcTemplate jdbc;
    private volatile boolean enabled = true;

    public ChatSwitchService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 启动时从数据库加载开关状态（表由 ChatInitRunner 创建，默认开启） */
    public void load() {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT `value` FROM chat_config WHERE `key` = ?", KEY);
            if (!rows.isEmpty()) {
                this.enabled = !"0".equals(String.valueOf(rows.get(0).get("value")));
            }
        } catch (Exception e) {
            log.warn("[chat-switch] load failed, default enabled: {}", e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** 设置开关并持久化（true=开启, false=关闭） */
    public void setEnabled(boolean value) {
        this.enabled = value;
        try {
            jdbc.update("UPDATE chat_config SET `value` = ? WHERE `key` = ?",
                    value ? "1" : "0", KEY);
        } catch (Exception e) {
            // 兼容无表场景：尝试插入
            try {
                jdbc.update("INSERT INTO chat_config (`key`, `value`) VALUES (?, ?)",
                        KEY, value ? "1" : "0");
            } catch (Exception ignored) {}
        }
        log.info("[chat-switch] chat {}", value ? "ENABLED" : "DISABLED");
    }
}
