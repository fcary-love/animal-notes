## 切片 4 交接包：里程碑与卡片

### 本轮状态
- 状态：已完成
- 覆盖故事：S3（里程碑标记）、S5（时光卡片）
- 依赖切片：切片 1（认证）+ 切片 2（宠物档案）+ 切片 3（时间线核心）

### 后端变更

#### 新增依赖
- `net.coobird:thumbnailator:0.4.20` — 图片缩放/裁剪

#### 新增文件
| 文件 | 说明 |
|------|------|
| `service/TimelineCardService.java` | 卡片生成接口 |
| `service/impl/TimelineCardServiceImpl.java` | 卡片生成实现 — Java2D + Thumbnailator 合成图片 |
| `controller/CardController.java` | POST /api/v1/pets/{petId}/card |

#### 卡片设计
- 尺寸：800×600 px，PNG 格式
- 背景：暖纸色 (#F5F0E2)，双重装饰边框
- 宠物头像：圆形裁剪（Thumbnailator），居中顶部
- 宠物名称 + 品种 + 出生日期
- 里程碑列表（最多 5 条，含日期、标签、内容摘要）
- 底部水印「宠物时间简史 · PetTimeline」
- 支持中文字体渲染（Microsoft YaHei → SimSun → Serif fallback）

#### 新增端点

| 方法 | 路径 | Token | 说明 |
|------|------|-------|------|
| POST | /api/v1/pets/{petId}/card | 是 | 生成卡片图片，返回 URL |

### 前端变更

#### 新增文件
| 文件 | 说明 |
|------|------|
| `api/card.js` | generateCard(petId) |
| `views/CardPreviewView.vue` | /card/:petId 页面 — 生成、预览、下载 |
| `docs/slices/04-milestone-card/` | 本交接包 |

#### 修改文件
| 文件 | 变更 |
|------|------|
| `main.js` | 新增 /card/:petId 路由 + CardPreviewView 导入 |
| `composables/useMoments.js` | 新增 milestoneOnly 状态 + toggleMilestone + fetchTimeline 智能切换（里程碑模式调用 GET /milestones） |
| `components/timeline/TimelineList.vue` | 新增 milestoneOnly prop + ★里程碑筛选按钮 + 卡片按钮 |
| `views/TimelineView.vue` | 新增 goCard 导航、toggleMilestone 处理、watch milestoneOnly 自动拉取 |

#### 用户流程

1. 用户在 TimelineView 点击工具栏 **"卡片"** 按钮
2. 跳转至 `/card/:petId` → CardPreviewView
3. 页面调用 POST `/api/v1/pets/{petId}/card` 生成卡片
4. 显示 loading → 渲染卡片图片 → 点击下载

### 测试结果
- `mvn test`：16/16 通过 (Auth:4 + Pet:4 + Moment:4 + Card:0)
- `npx vite build`：117 个模块，0 错误
