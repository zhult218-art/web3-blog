package com.web3.forum.config;

import com.web3.forum.service.ChatService;
import com.web3.forum.service.ChatSwitchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 群聊模块启动初始化：
 * 1. 自动建表（chat_room / chat_room_member / chat_message / chat_config）
 * 2. 确保存在默认群"极光广场"
 * 3. 加载聊天开关状态
 */
@Component
public class ChatInitRunner implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final ChatService chatService;
    private final ChatSwitchService chatSwitchService;

    public ChatInitRunner(JdbcTemplate jdbc, ChatService chatService, ChatSwitchService chatSwitchService) {
        this.jdbc = jdbc;
        this.chatService = chatService;
        this.chatSwitchService = chatSwitchService;
    }

    @Override
    public void run(String... args) {
        try {
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS chat_room (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    description VARCHAR(500),
                    owner_id BIGINT,
                    require_approval TINYINT DEFAULT 1,
                    created_at DATETIME,
                    updated_at DATETIME
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS chat_room_member (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    room_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    username VARCHAR(100),
                    nickname VARCHAR(100),
                    avatar VARCHAR(500),
                    role VARCHAR(20) DEFAULT 'member',
                    status VARCHAR(20) DEFAULT 'pending',
                    created_at DATETIME,
                    updated_at DATETIME,
                    INDEX idx_room_user (room_id, user_id),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS chat_message (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    room_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    username VARCHAR(100),
                    nickname VARCHAR(100),
                    avatar VARCHAR(500),
                    msg_type VARCHAR(20) DEFAULT 'text',
                    content TEXT,
                    created_at DATETIME,
                    INDEX idx_room (room_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        } catch (Exception e) {
            // 表已存在或无 DDL 权限时忽略
        }
        // 聊天开关配置表（管理员可关闭群聊）
        try {
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS chat_config (
                    `key` VARCHAR(50) PRIMARY KEY,
                    `value` VARCHAR(200),
                    updated_at DATETIME
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            jdbc.update("INSERT IGNORE INTO chat_config (`key`, `value`, updated_at) VALUES ('chatEnabled', '1', NOW())");
        } catch (Exception ignored) {}
        try {
            chatService.ensureDefaultRoom();
        } catch (Exception ignored) {}
        // 增量列：头像冗余（已存在时忽略 Duplicate column 报错）
        try { jdbc.execute("ALTER TABLE chat_room_member ADD COLUMN avatar VARCHAR(500) NULL AFTER nickname"); } catch (Exception ignored) {}
        try { jdbc.execute("ALTER TABLE chat_message ADD COLUMN avatar VARCHAR(500) NULL AFTER nickname"); } catch (Exception ignored) {}
        // 固定单群策略：默认群免审批，历史 pending/rejected 成员全部转为 approved
        try {
            jdbc.update("UPDATE chat_room SET require_approval = 0 WHERE id = (SELECT MIN(id) FROM (SELECT id FROM chat_room) t)");
            jdbc.update("UPDATE chat_room_member SET status = 'approved' WHERE status IN ('pending','rejected')");
        } catch (Exception ignored) {}
        // 建表完成后加载聊天开关状态
        try { chatSwitchService.load(); } catch (Exception ignored) {}
    }
}
