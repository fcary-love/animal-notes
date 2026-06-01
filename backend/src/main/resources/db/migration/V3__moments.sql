CREATE TABLE IF NOT EXISTS moments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    content TEXT,
    photos JSON,
    occurred_at DATETIME NOT NULL,
    is_milestone TINYINT DEFAULT 0,
    milestone_label VARCHAR(30),
    location VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    INDEX idx_pet_id (pet_id),
    INDEX idx_occurred_at (occurred_at),
    INDEX idx_pet_occurred (pet_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时间线条目表';
