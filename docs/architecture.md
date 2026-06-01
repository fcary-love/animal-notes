# 宠物时间简史 (PetTimeline) — 架构蓝图

## 1. 领域分析

### 场景还原
- 时间：碎片化时刻——晚上窝在沙发上翻手机时，带宠物去医院等叫号时，宠物生日想发朋友圈时
- 地点：大部分在家，小部分在医院/户外
- 心情：温馨、怀念、偶尔焦虑（宠物生病时）
- 设备：手机为主（拍照记录），桌面端为辅（整理和回顾）

### 用户画像
| 角色 | 核心诉求 | 优先级 |
|------|---------|--------|
| 宠物主（主要） | 记录宠物日常和重要时刻，回顾成长轨迹，分享到社交平台 | ★★★ |
| 多宠家庭 | 在一个应用里管理多只宠物的档案和时间线，能切换视角 | ★★☆ |
| 宠物救助/寄养者 | 为暂养的宠物建立临时档案，转交给领养人时一并移交 | ★☆☆ |

### 核心价值主张
宠物主在碎片时间里，用 PetTimeline 记录下宠物生命中每一个值得被记住的时刻，多年后回头看，能像翻阅一本传记一样重温它的一生。

---

## 2. 功能设计

### 用户故事
| 编号 | 故事 | MVP |
|------|------|-----|
| ⭐ S1 | 作为宠物主，我想创建宠物档案（名字、品种、生日、头像），以便为它建立专属的生命主线 | ⭐ |
| ⭐ S2 | 作为宠物主，我想在宠物时间线上添加图文时刻（照片+文字+日期），以便记录每一个值得记住的瞬间 | ⭐ |
| ⭐ S3 | 作为宠物主，我想给某些特殊时刻打上「里程碑」标记（到家日、生日、第一次打针等），以便一眼看出宠物生命中的重要节点 | ⭐ |
| ⭐ S4 | 作为宠物主，我想按时间顺序浏览宠物的完整时间线，以便回顾它的成长轨迹 | ⭐ |
| ⭐ S5 | 作为宠物主，我想生成一张包含宠物头像和时间线亮点的时间线卡片并分享，以便在社交平台上展示 | ⭐ |
| S6 | 作为宠物主，我想记录宠物的健康事件（疫苗、体检、病史），以便在看病时快速提供给兽医 | |
| S7 | 作为多宠家庭，我想在我的账号下管理多只宠物，以便每只都有独立的时间线 | |
| S8 | 作为宠物主，当我不小心删错了一条记录时，我想从回收站恢复它，以便不丢失珍贵的回忆 | |

### 功能模块树
```
宠物管理（覆盖 S1、S7）
  ├── 创建宠物档案
  ├── 编辑宠物信息
  ├── 多宠物切换
  └── 宠物头像上传

时间线（覆盖 S2、S3、S4）
  ├── 新建时间线条目（照片+文字+日期+标签）
  ├── 编辑/删除条目
  ├── 按时间正序/倒序浏览
  └── 标记/取消里程碑

卡片分享（覆盖 S5）
  ├── 生成时间线卡片（宠物头像+精选时刻）
  └── 下载/分享卡片图片

健康记录（覆盖 S6）— MVP 后扩展
回收站（覆盖 S8）— MVP 后扩展
```

### 纵向功能切片队列
| 顺序 | 切片 | 覆盖故事 | 后端 | 前端 | 依赖 |
|------|------|----------|------|------|------|
| 1 | 认证与项目骨架 | S1(部分) | 注册/登录/当前用户、JWT、全局异常、CORS | 登录页、注册页、路由守卫、AppHeader | 无 |
| 2 | 宠物档案 | S1、S7 | 宠物 CRUD + 头像上传、MinIO | PetCard、PetForm、PetSwitcher | 切片1 |
| 3 | 时间线核心 | S2、S4 | 时刻 CRUD、时间线排序分页 | TimelineList、TimelineItem、MomentForm | 切片2 |
| 4 | 里程碑与卡片 | S3、S5 | 里程碑标记/筛选、卡片生成 | MilestoneBadge、TimelineCard | 切片3 |

---

## 3. 数据模型

### 实体

**User**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | |
| username | VARCHAR(50) UNIQUE NOT NULL | 登录名 |
| password | VARCHAR(255) NOT NULL | bcrypt 加密 |
| nickname | VARCHAR(50) | 显示昵称 |
| avatar_url | VARCHAR(500) | 头像 URL |
| created_at | DATETIME DEFAULT NOW() | |
| updated_at | DATETIME ON UPDATE NOW() | |
| is_deleted | TINYINT DEFAULT 0 | 软删除 |

**Pet**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | |
| user_id | BIGINT FK → User NOT NULL | 所属用户 |
| name | VARCHAR(50) NOT NULL | 宠物名 |
| species | VARCHAR(20) NOT NULL | 猫/狗/兔/鸟/其它 |
| breed | VARCHAR(50) | 品种 |
| birthday | DATE | 生日 |
| avatar_url | VARCHAR(500) | 头像 |
| bio | VARCHAR(200) | 一句话简介 |
| created_at | DATETIME DEFAULT NOW() | |
| updated_at | DATETIME ON UPDATE NOW() | |
| is_deleted | TINYINT DEFAULT 0 | 软删除 |

**Moment**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | |
| pet_id | BIGINT FK → Pet NOT NULL | 所属宠物 |
| content | TEXT | 文字内容 |
| photos | JSON | 照片 URL 数组 |
| occurred_at | DATETIME NOT NULL | 事件真实时间 |
| is_milestone | TINYINT DEFAULT 0 | 是否里程碑 |
| milestone_label | VARCHAR(30) | 里程碑标签 |
| location | VARCHAR(100) | 地点 |
| created_at | DATETIME DEFAULT NOW() | |
| updated_at | DATETIME ON UPDATE NOW() | |
| is_deleted | TINYINT DEFAULT 0 | 软删除 |

### 实体关系
```
User 1──* Pet 1──* Moment
```

---

## 4. API 契约

### 端点

#### 认证
```
POST   /api/v1/auth/register   # 注册
POST   /api/v1/auth/login      # 登录
GET    /api/v1/auth/me         # 当前用户信息
```

#### 宠物档案
```
POST   /api/v1/pets            # 创建宠物
GET    /api/v1/pets            # 当前用户的宠物列表
GET    /api/v1/pets/{id}       # 单只宠物详情
PUT    /api/v1/pets/{id}       # 更新宠物信息
DELETE /api/v1/pets/{id}       # 删除宠物（软删除）
POST   /api/v1/pets/{id}/avatar  # 上传宠物头像
```

#### 时间线条目
```
POST   /api/v1/pets/{petId}/moments           # 创建时刻
GET    /api/v1/pets/{petId}/moments           # 时间线（occurred_at排序，分页）
GET    /api/v1/moments/{id}                    # 单条时刻详情
PUT    /api/v1/moments/{id}                    # 编辑时刻
DELETE /api/v1/moments/{id}                    # 软删除
GET    /api/v1/pets/{petId}/moments/milestones # 只查里程碑
```

#### 卡片
```
GET    /api/v1/pets/{petId}/timeline-card     # 生成时间线卡片（返回图片URL）
```

#### 文件
```
POST   /api/v1/files/upload   # 上传图片
```

### 核心接口示例

**POST /api/v1/pets/{petId}/moments**
```json
// 请求
{ "content": "奶茶今天第一次自己跳上窗台晒太阳。", "photos": ["https://oss.example.com/pets/1/moments/abc.jpg"], "occurredAt": "2026-05-28 14:30:00", "isMilestone": true, "milestoneLabel": "第一次", "location": "家里窗台" }
// 响应
{ "code": 200, "message": "ok", "data": { "id": 1, "petId": 1, "content": "...", "photos": ["..."], "occurredAt": "2026-05-28T14:30:00", "isMilestone": true, "milestoneLabel": "第一次", "location": "家里窗台", "createdAt": "2026-05-29T20:15:00" } }
```

**GET /api/v1/pets/{petId}/moments?sort=desc&page=1&size=20**
```json
{ "code": 200, "message": "ok", "data": { "total": 42, "page": 1, "size": 20, "records": [ ... ] } }
```

**GET /api/v1/pets/{petId}/timeline-card**
```json
{ "code": 200, "message": "ok", "data": { "cardUrl": "https://oss.example.com/cards/pet1_timeline.png", "generatedAt": "2026-05-29T20:30:00" } }
```

**POST /api/v1/auth/register**
```json
// 请求
{ "username": "catlover", "password": "123456", "nickname": "奶茶妈妈" }
// 响应
{ "code": 200, "message": "注册成功", "data": { "id": 1, "username": "catlover", "nickname": "奶茶妈妈", "token": "eyJhbGciOiJIUzI1NiJ9..." } }
```

### 统一响应格式
```json
{ "code": 200, "message": "ok", "data": {} }
```

### 错误码
| code | message | 场景 |
|------|---------|------|
| 401 | 未登录或登录已过期 | Token 缺失/无效/过期 |
| 403 | 无权操作该宠物 | 用户试图编辑别人的宠物 |
| 404 | 宠物不存在 / 时刻不存在 | |
| 400 | 照片不能超过9张 | |
| 413 | 图片大小不能超过10MB | |
| 415 | 仅支持JPG/PNG/WebP格式 | |
| 409 | 用户名已被注册 | |

---

## 5. 后端架构

### 技术栈
| 层面 | 推荐 | 理由 |
|------|------|------|
| 语言/框架 | Spring Boot 2.7.x | 固定技术栈 |
| 数据库 | MySQL 8.0 | 固定关系型存储 |
| ORM | MyBatis-Plus 3.5.x | 固定数据访问 |
| 数据库版本管理 | Flyway | schema 变化可追踪 |
| 对象存储 | MinIO | 头像 + 照片 + 卡片图片 |
| 认证 | JWT (jjwt) | 无状态 Token |

### 分层架构
```
controller/  ← 参数校验、路由、统一响应
service/     ← 全部业务逻辑
mapper/      ← 数据访问
model/
  ├── entity/  ← User, Pet, Moment
  ├── dto/     ← 入参对象
  └── vo/      ← 返参对象
config/      ← Security, CORS, MinIO, MyBatisPlus
exception/   ← BusinessException, GlobalExceptionHandler
utils/       ← JwtUtils, FileUtils, ImageUtils
```

### 包结构
```
com.pettimeline/
├── PetTimelineApplication.java
├── controller/  (AuthController, PetController, MomentController, TimelineCardController, FileController)
├── service/     (AuthService, PetService, MomentService, TimelineCardService, FileService + impl/)
├── mapper/      (UserMapper, PetMapper, MomentMapper)
├── model/
│   ├── entity/  (User, Pet, Moment)
│   ├── dto/     (LoginDTO, RegisterDTO, CreatePetDTO, UpdatePetDTO, CreateMomentDTO, UpdateMomentDTO)
│   └── vo/      (UserVO, PetVO, MomentVO, TimelineCardVO, PageVO)
├── config/      (SecurityConfig, CorsConfig, MinioConfig, MybatisPlusConfig)
├── exception/   (BusinessException, GlobalExceptionHandler)
└── utils/       (JwtUtils, FileUtils, ImageUtils)
```

---

## 6. 前端架构

### 技术栈
| 层面 | 推荐 | 理由 |
|------|------|------|
| 框架 | Vue3 + Vite | 固定 |
| UI | Naive UI | 现代、可定制、适合情感化应用 |
| 路由 | Vue Router 4 | 固定 |
| HTTP | Axios | 固定 |
| 状态 | composable + Pinia(auth) | 认证跨页面，其他按需 |

### 路由
```
/                    → 首页（重定向到/timeline）
/login               → 登录
/register            → 注册
/timeline            → 宠物列表 + 时间线（主页面）
/pet/:id             → 宠物详情/编辑
/pet/create          → 创建宠物
/moment/:id/edit     → 编辑时刻
/card/:petId         → 卡片预览/下载
```

### 前后端映射
| composable | API | 用途 |
|-----------|-----|------|
| useAuth() | POST/GET /auth/* | 认证 |
| usePets() | CRUD /pets | 宠物管理 |
| useMoments() | CRUD /pets/{id}/moments | 时间线 |
| useCard() | GET /pets/{id}/timeline-card | 卡片 |

---

## 7. 开发计划

| 顺序 | 切片 | 后端 | 前端 | 依赖 |
|------|------|------|------|------|
| 1 | 认证与项目骨架 | 注册/登录/me、JWT、异常处理、CORS | 登录/注册页、路由守卫、AppHeader | 无 |
| 2 | 宠物档案 | 宠物CRUD+头像上传+MinIO | PetCard、PetForm、PetSwitcher | 切片1 |
| 3 | 时间线核心 | 时刻CRUD+排序分页 | TimelineList、TimelineItem、MomentForm | 切片2 |
| 4 | 里程碑与卡片 | 里程碑标记/筛选+卡片生成 | MilestoneBadge、TimelineCard | 切片3 |

## 8. 最小部署计划
- 后端：`mvn package` → `java -jar target/pet-timeline-1.0.0.jar`
- 前端：`npm run build` → `dist/` 目录
- Nginx：静态资源 `/` → `dist/`，反向代理 `/api/` → `localhost:8080`，`/files/` → MinIO
- 环境变量：`JWT_SECRET`、`MYSQL_URL`、`MINIO_ENDPOINT`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`
