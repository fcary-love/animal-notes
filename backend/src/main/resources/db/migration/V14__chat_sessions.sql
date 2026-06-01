CREATE TABLE ai_chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pet_id BIGINT,
    title VARCHAR(100) DEFAULT '新对话',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE ai_conversations ADD COLUMN session_id BIGINT;
ALTER TABLE ai_conversations ADD INDEX idx_session_id (session_id);
