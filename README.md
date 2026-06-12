 # 动物笔记 / 宠物时间简史

  一个面向宠物陪伴、成长记录与日常照护的全栈应用。

  项目希望把传统"宠物管理系统"做得更像一本温暖的手帐：记录宠物的成长时刻、健康数据、饮食体重、相册回忆，也提供知识库、时
  光顾问、3D 宠物房间和独立的猫咪游戏盒。

  ## 功能特性

  - 宠物档案：管理多只宠物的基础信息
  - 成长时间线：记录照片、故事、健康、报告、3D 等时刻
  - 健康档案：管理疫苗、驱虫、体检等健康事件
  - 饮食与体重：记录日常饮食和体重变化
  - 成长相册：按宠物聚合照片回忆
  - 知识库：维护宠物相关知识内容
  - 时光顾问：基于宠物上下文进行问答和照护建议
  - 3D 宠物房间：提供宠物陪伴入口和互动空间
  - 猫咪游戏盒：独立娱乐游戏中心，游戏结果不写入成长时间线或正式健康数据

  ## 技术栈

  **前端**
  - Vue 3 + Vite
  - Vue Router
  - Three.js
  - GSAP
  - Naive UI
  - Axios
  - Capacitor（Android 支持）

  **后端**
  - Spring Boot 3 + Java 21
  - MySQL + MyBatis Plus
  - Flyway 数据库迁移
  - JWT 认证
  - Redis
  - LangChain4j（AI 时光顾问）
    
   ## 项目结构

      backend/                        Spring Boot 后端
        src/main/java/
          ai/                         AI 模块 RAG、Agent、Tools
          config/                     配置类 CORS、JWT、MyBatis Plus
          controller/                 REST 接口
          mapper/                     MyBatis Plus 数据访问
          model/                      实体与 DTO
          service/                    业务逻辑层
        src/main/resources/
          db/migration/               Flyway 迁移脚本

      frontend/                       Vue 3 前端
        src/
          api/                        接口请求
          components/                 通用组件
          views/                      页面视图
          styles/                     样式系统
        android/                      Capacitor Android 构建

          ## 快速开始

  ### 环境要求

  - JDK 21+
  - Node.js 18+
  - MySQL 8.0
  - Redis

  ### 1. 克隆项目

  ```bash
  git clone https://github.com/fcary-love/animal-notes.git
  cd animal-notes
