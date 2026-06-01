ALTER TABLE pets
    ADD COLUMN model_config JSON NULL COMMENT '3D宠物外观配置',
    ADD COLUMN model_updated_at DATETIME NULL COMMENT '3D模型最近更新时间';
