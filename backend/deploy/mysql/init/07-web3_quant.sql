USE web3_quant;

CREATE TABLE IF NOT EXISTS `strategy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(200) NOT NULL,
  `description` TEXT,
  `status` VARCHAR(32) DEFAULT 'DRAFT',
  `code` LONGTEXT,
  `returns` DECIMAL(10,4) DEFAULT 0.0000,
  `risk_level` VARCHAR(16) DEFAULT 'MEDIUM',
  `tags` VARCHAR(255) DEFAULT NULL,
  `backtest_data` LONGTEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quant_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `strategy_id` BIGINT NOT NULL,
  `message` TEXT,
  `level` VARCHAR(16) DEFAULT 'INFO',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_strategy` (`strategy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `stock_price` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `symbol` VARCHAR(32) NOT NULL,
  `name` VARCHAR(64) DEFAULT NULL,
  `period` VARCHAR(16) DEFAULT 'day',
  `timestamp` DATETIME NOT NULL,
  `open` DECIMAL(16,4) DEFAULT NULL,
  `close` DECIMAL(16,4) DEFAULT NULL,
  `high` DECIMAL(16,4) DEFAULT NULL,
  `low` DECIMAL(16,4) DEFAULT NULL,
  `volume` DECIMAL(20,4) DEFAULT NULL,
  `turnover` DECIMAL(20,4) DEFAULT NULL,
  `amplitude` DECIMAL(10,4) DEFAULT NULL,
  `change` DECIMAL(16,4) DEFAULT NULL,
  `change_percent` DECIMAL(10,4) DEFAULT NULL,
  `turnover_rate` DECIMAL(10,4) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_symbol_period_ts` (`symbol`, `period`, `timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `strategy` (`name`, `description`, `status`, `returns`, `risk_level`, `tags`) VALUES
('Dual Moving Average', 'Classic dual MA crossover strategy.', 'ACTIVE', 12.5, 'LOW', 'trend,moving-average'),
('Grid Trading', 'Grid trading strategy for range markets.', 'DRAFT', 0.0, 'MEDIUM', 'grid,range');
