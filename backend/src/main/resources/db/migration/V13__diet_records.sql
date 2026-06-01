CREATE TABLE IF NOT EXISTS diet_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    food_type VARCHAR(50) NOT NULL COMMENT '食物类型：干粮/湿粮/零食/自制/其他',
    food_name VARCHAR(100) NOT NULL COMMENT '食物名称',
    amount DECIMAL(8,2) COMMENT '份量（克）',
    calories DECIMAL(8,2) COMMENT '卡路里',
    meal_type VARCHAR(20) NOT NULL COMMENT '餐次：早餐/午餐/晚餐/加餐',
    recorded_at DATETIME NOT NULL COMMENT '记录时间',
    note VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pet_date (pet_id, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
