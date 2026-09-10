-- =====================================================================
-- API 中转站（AI Proxy Relay）数据库设计 v2 —— 全面版
-- 库：web3_admin    引擎：InnoDB    字符集：utf8mb4_0900_ai_ci
-- ---------------------------------------------------------------------
-- 设计原则：
--   1) 高并发安全（防死锁）：
--      - 所有额度/计数变更均为「单条原子 UPDATE」或「INSERT..ON DUPLICATE KEY UPDATE」，
--        杜绝 SELECT->UPDATE 两段式事务；
--      - 热路径不建物理外键（逻辑引用），降低行级锁交叉与级联开销，且便于后续分表；
--      - 事务内固定加锁顺序：先 token 行 -> 再 daily_stat / alert；
--      - 日志表仅追加写；request_id 为随机值，唯一键冲突概率趋近于零；
--      - 告警表以 dedup_key 唯一键 + INSERT IGNORE 实现幂等去重（天然免锁竞争）。
--   2) 安全：
--      - 上游渠道 Key 以 AES-GCM 密文(Base64(iv||ct||tag)) 存储，明文不落库；
--      - 令牌只存 SHA-256 加盐哈希与前缀，完整 Key 仅发放时一次性返回；
--      - 兑换码同样只存哈希，防拖库撞码。
--   3) 可扩展（预留 P2/P3 能力）：
--      - 渠道：多类型(type)、优先级(priority)、自动熔断(auto_disabled/fail_count)、
--        上游余额探测(balance)、JSON 扩展配置(config)；
--      - 令牌：用户分组(group_name)、IP 白名单、RPM 限速、归属用户(user_id)、发放人(created_by)；
--      - 模型：单倍率/输入输出分离(rate_type/input_rate/output_rate)、单次上限(max_tokens)、
--        全局 RPM(rpm_limit)、标签(tags)、排序(sort_order)；
--      - 用户分组倍率(proxy_user_group)、兑换码(proxy_redeem_code)、告警(proxy_alert)、
--        日汇总(proxy_daily_stat) 独立成表，随时启用不影响主链路。
-- ---------------------------------------------------------------------
-- Redis 键契约（应用层实现，见 ai-proxy-service）：
--   proxy:token:{sha256hex}      -> TokenAuth 缓存(JSON)，TTL 60s，管理端变更时 DEL
--   proxy:channel:list           -> 启用渠道列表缓存，TTL 10s
--   proxy:rr:{model}             -> round_robin 计数器(INCR)
--   proxy:rl:{tokenId}:{yyMMHHmm}-> 令牌 RPM 滑窗计数(INCR+EXPIRE 65s)，超限返回 429
--   proxy:model:all              -> 模型倍率缓存，TTL 30s
-- =====================================================================

SET NAMES utf8mb4;
USE `web3_admin`;

-- ---------------------------------------------------------------
-- 1. proxy_channel 上游渠道
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `proxy_channel` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '渠道ID',
  `name`             VARCHAR(64)     NOT NULL                COMMENT '渠道名称',
  `type`             TINYINT         NOT NULL DEFAULT 1      COMMENT '类型:1=OpenAI兼容 2=Azure 3=Anthropic(预留)',
  `base_url`         VARCHAR(512)    NOT NULL                COMMENT '上游地址',
  `api_key_cipher`   VARCHAR(2048)   NOT NULL                COMMENT 'AES-GCM密文(Base64(iv||ct||tag)),明文不落库',
  `masked_key`       VARCHAR(32)     NULL                    COMMENT '脱敏展示 sk-***abcd',
  `models`           TEXT            NULL                    COMMENT '支持模型,逗号分隔',
  `strategy`         VARCHAR(16)     NOT NULL DEFAULT 'random' COMMENT '调度:weight/random/round_robin',
  `weight`           INT             NOT NULL DEFAULT 1      COMMENT '权重(weight策略)',
  `priority`         INT             NOT NULL DEFAULT 0      COMMENT '优先级,越大越先(预留)',
  `status`           TINYINT         NOT NULL DEFAULT 1      COMMENT '0=禁用 1=启用',
  `auto_disabled`    TINYINT         NOT NULL DEFAULT 0      COMMENT '连续失败自动熔断标记(预留)',
  `fail_count`       INT             NOT NULL DEFAULT 0      COMMENT '连续失败次数(原子累加)',
  `success_count`    BIGINT          NOT NULL DEFAULT 0      COMMENT '累计成功(原子累加)',
  `avg_latency_ms`   INT             NOT NULL DEFAULT 0      COMMENT '平均延迟ms(指数平滑)',
  `balance`          DECIMAL(14,4)   NULL                    COMMENT '上游账户余额(预留探测)',
  `balance_currency` VARCHAR(8)      NULL                    COMMENT '余额币种',
  `balance_updated_at` DATETIME      NULL                    COMMENT '余额更新时间',
  `last_used_at`     DATETIME        NULL                    COMMENT '最近使用时间',
  `config`           JSON            NULL                    COMMENT '扩展配置{extraHeaders,timeoutMs,maxRetry...}',
  `remark`           VARCHAR(255)    NULL                    COMMENT '备注',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status_priority` (`status`,`priority`),
  KEY `idx_updated` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API中转站-上游渠道';

-- ---------------------------------------------------------------
-- 2. proxy_token 访问令牌
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `proxy_token` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '令牌ID',
  `name`            VARCHAR(64)     NOT NULL                COMMENT '令牌名称',
  `user_id`         BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '归属用户ID(预留,0=平台)',
  `group_name`      VARCHAR(32)     NOT NULL DEFAULT 'default' COMMENT '分组(关联proxy_user_group计倍率)',
  `token_prefix`    CHAR(11)        NOT NULL                COMMENT '前缀 sk-xxxxxxxx',
  `token_hash`      CHAR(64)        NOT NULL                COMMENT 'SHA-256(salt+token)十六进制',
  `quota`           BIGINT          NOT NULL DEFAULT 0      COMMENT '剩余额度(500000=1美元)',
  `used_quota`      BIGINT          NOT NULL DEFAULT 0      COMMENT '已用额度(原子累加)',
  `unlimited_quota` TINYINT         NOT NULL DEFAULT 0      COMMENT '0=限额 1=无限',
  `model_limit`     TEXT            NULL                    COMMENT '可用模型白名单,空=不限',
  `allow_ips`       VARCHAR(512)    NULL                    COMMENT 'IP白名单,逗号分隔,空=不限',
  `rate_limit`      INT             NULL                    COMMENT 'RPM限速,NULL=不限(Redis滑窗)',
  `expired_at`      DATETIME        NULL                    COMMENT '过期时间,NULL=永不过期',
  `status`          TINYINT         NOT NULL DEFAULT 1      COMMENT '0=停用 1=启用',
  `request_count`   BIGINT          NOT NULL DEFAULT 0      COMMENT '累计请求数(原子累加)',
  `last_used_at`    DATETIME        NULL                    COMMENT '最近使用时间',
  `created_by`      BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '发放管理员ID',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_hash` (`token_hash`),
  KEY `idx_user` (`user_id`,`status`),
  KEY `idx_group` (`group_name`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API中转站-访问令牌';

-- ---------------------------------------------------------------
-- 3. proxy_model 模型与倍率
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `proxy_model` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `model_name`      VARCHAR(128)    NOT NULL                COMMENT '模型名(请求时的model)',
  `rate_type`       TINYINT         NOT NULL DEFAULT 0      COMMENT '0=单倍率 1=输入输出分离(预留)',
  `model_rate`      DECIMAL(10,4)   NOT NULL DEFAULT 1.0000 COMMENT '基础倍率',
  `completion_rate` DECIMAL(10,4)   NOT NULL DEFAULT 1.0000 COMMENT '补全倍率(单倍率模式生效)',
  `input_rate`      DECIMAL(10,4)   NULL                    COMMENT '输入倍率(rate_type=1)',
  `output_rate`     DECIMAL(10,4)   NULL                    COMMENT '输出倍率(rate_type=1)',
  `max_tokens`      INT             NULL                    COMMENT '单次max_tokens上限,NULL=不限(预留)',
  `rpm_limit`       INT             NULL                    COMMENT '全局RPM限制,NULL=不限(预留)',
  `status`          TINYINT         NOT NULL DEFAULT 1      COMMENT '0=下架 1=上架',
  `tags`            VARCHAR(255)    NULL                    COMMENT '标签,逗号分隔(预留)',
  `sort_order`      INT             NOT NULL DEFAULT 100    COMMENT '排序',
  `remark`          VARCHAR(255)    NULL                    COMMENT '备注',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model` (`model_name`),
  KEY `idx_status_sort` (`status`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API中转站-模型倍率';

-- ---------------------------------------------------------------
-- 4. proxy_request_log 请求日志（追加写，逻辑外键）
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `proxy_request_log` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `request_id`       VARCHAR(64)     NOT NULL                COMMENT '请求追踪ID(UUID)',
  `token_id`         BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '令牌ID(逻辑引用)',
  `token_name`       VARCHAR(64)     NULL                    COMMENT '令牌名称(冗余,免JOIN)',
  `channel_id`       BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '渠道ID(0=兜底直连)',
  `channel_name`     VARCHAR(64)     NULL                    COMMENT '渠道名称(冗余)',
  `model_name`       VARCHAR(128)    NOT NULL                COMMENT '模型',
  `prompt_tokens`    INT             NOT NULL DEFAULT 0      COMMENT '提示tokens',
  `completion_tokens` INT            NOT NULL DEFAULT 0      COMMENT '补全tokens',
  `quota_cost`       BIGINT          NOT NULL DEFAULT 0      COMMENT '本次扣费额度',
  `latency_ms`       INT             NOT NULL DEFAULT 0      COMMENT '总耗时ms',
  `is_stream`        TINYINT         NOT NULL DEFAULT 0      COMMENT '是否流式',
  `status_code`      INT             NOT NULL DEFAULT 200    COMMENT '响应状态码',
  `fallback`         TINYINT         NOT NULL DEFAULT 0      COMMENT '1=无渠道兜底直连(不计费)',
  `client_ip`        VARCHAR(45)     NULL                    COMMENT '客户端IP(IPv6兼容)',
  `error_msg`        VARCHAR(1024)   NULL                    COMMENT '错误信息(截断1024)',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_id` (`request_id`),
  KEY `idx_created` (`created_at`),
  KEY `idx_token_created` (`token_id`,`created_at`),
  KEY `idx_channel_created` (`channel_id`,`created_at`),
  KEY `idx_model_created` (`model_name`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API中转站-请求日志';

-- ---------------------------------------------------------------
-- 5. proxy_user_group 用户分组（P2-T14 预留，default 组即刻生效）
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `proxy_user_group` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `group_name`  VARCHAR(32)     NOT NULL COMMENT '分组名(tokens.group_name引用)',
  `rate`        DECIMAL(8,4)    NOT NULL DEFAULT 1.0000 COMMENT '分组计费倍率',
  `description` VARCHAR(255)    NULL,
  `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '0=停用 1=启用(停用时按1计算)',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group` (`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API中转站-用户分组倍率';

-- ---------------------------------------------------------------
-- 6. proxy_redeem_code 兑换码（P2-T11 预留，只存哈希防拖库）
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `proxy_redeem_code` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `batch_no`        VARCHAR(32)     NOT NULL COMMENT '批次号',
  `code_prefix`     CHAR(8)         NOT NULL COMMENT '码前8位(展示/对账)',
  `code_hash`       CHAR(64)        NOT NULL COMMENT 'SHA-256(salt+code)',
  `quota`           BIGINT          NOT NULL COMMENT '面值额度',
  `status`          TINYINT         NOT NULL DEFAULT 0 COMMENT '0=未使用 1=已使用 2=作废',
  `used_by_token_id` BIGINT UNSIGNED NULL COMMENT '核销令牌ID',
  `used_at`         DATETIME        NULL,
  `expired_at`      DATETIME        NULL COMMENT '过期时间,NULL=长期',
  `created_by`      BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_hash` (`code_hash`),
  KEY `idx_batch` (`batch_no`,`status`),
  KEY `idx_expired` (`expired_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API中转站-额度兑换码';

-- ---------------------------------------------------------------
-- 7. proxy_alert 告警记录（dedup_key 幂等，INSERT IGNORE 防重）
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `proxy_alert` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `alert_type`  VARCHAR(32)     NOT NULL COMMENT 'LOW_BALANCE/TOKEN_EXHAUSTED/CHANNEL_FAIL/RATE_LIMIT',
  `level`       TINYINT         NOT NULL DEFAULT 2 COMMENT '1=info 2=warn 3=danger',
  `ref_id`      VARCHAR(64)     NULL COMMENT '关联对象ID',
  `message`     VARCHAR(512)    NOT NULL,
  `dedup_key`   VARCHAR(160)    NOT NULL COMMENT '去重键(如LOW_BALANCE:t:5:20260825)',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dedup` (`dedup_key`),
  KEY `idx_type_level_time` (`alert_type`,`level`,`created_at`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API中转站-告警记录';

-- ---------------------------------------------------------------
-- 8. proxy_daily_stat 日汇总（ON DUPLICATE KEY 原子累加，长历史留存，
--    日志表归档清理后统计仍完整）
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `proxy_daily_stat` (
  `stat_date`         DATE            NOT NULL,
  `user_key`          VARCHAR(64)     NOT NULL COMMENT '维度键:token前缀或user:{id}',
  `model_name`        VARCHAR(128)    NOT NULL,
  `requests`          BIGINT          NOT NULL DEFAULT 0,
  `error_count`       BIGINT          NOT NULL DEFAULT 0,
  `prompt_tokens`     BIGINT          NOT NULL DEFAULT 0,
  `completion_tokens` BIGINT          NOT NULL DEFAULT 0,
  `quota_cost`        BIGINT          NOT NULL DEFAULT 0,
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`stat_date`,`user_key`,`model_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API中转站-按日汇总';

-- ---------------------------------------------------------------
-- 种子数据（幂等）
-- ---------------------------------------------------------------
INSERT INTO `proxy_user_group` (`group_name`,`rate`,`description`)
VALUES ('default',1.0000,'默认分组')
ON DUPLICATE KEY UPDATE `description`=VALUES(`description`);

INSERT INTO `proxy_model` (`model_name`,`rate_type`,`model_rate`,`completion_rate`,`sort_order`,`remark`) VALUES
('deepseek-chat',      0, 1.0000, 1.0000, 100, 'DeepSeek V3 对话'),
('deepseek-reasoner',  0, 4.0000, 1.0000, 110, 'DeepSeek R1 推理'),
('gpt-4o',             0, 15.0000, 3.0000, 120, 'GPT-4o 多模态'),
('gpt-4o-mini',        0, 0.9000,  1.0000, 130, 'GPT-4o mini'),
('qwen-plus',          0, 1.6000,  1.0000, 140, '通义千问 Plus'),
('qwen-max',           0, 6.4000,  1.0000, 150, '通义千问 Max')
ON DUPLICATE KEY UPDATE `updated_at`=`updated_at`;
