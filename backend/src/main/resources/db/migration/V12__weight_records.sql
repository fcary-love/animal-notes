CREATE TABLE IF NOT EXISTS weight_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    weight DECIMAL(6,2) NOT NULL,
    recorded_at DATE NOT NULL,
    note VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pet_date (pet_id, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
