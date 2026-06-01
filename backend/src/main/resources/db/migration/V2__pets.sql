CREATE TABLE IF NOT EXISTS pets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    species VARCHAR(20) NOT NULL,
    breed VARCHAR(50),
    birthday DATE,
    avatar_url VARCHAR(500),
    bio VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_species (species)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物档案表';
