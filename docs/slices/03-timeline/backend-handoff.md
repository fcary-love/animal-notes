## 前端交接包：时间线核心

### 本轮状态
- 后端状态：已完成
- 覆盖故事：S2（记录时刻）、S4（浏览时间线）
- 依赖切片：切片 1（认证）+ 切片 2（宠物档案）
- 前端可以开始接入：是

### 变更记录
与蓝图一致。photos 字段以 JSON 数组存储，返回时自动解析为字符串数组。

### 本轮接口速查

| 方法 | 路径 | Token | 说明 |
|------|------|-------|------|
| POST | /api/v1/pets/{petId}/moments | 是 | 创建时间线条目 |
| GET | /api/v1/pets/{petId}/moments | 是 | 时间线分页（sort=asc/desc, page, size） |
| GET | /api/v1/pets/{petId}/moments/milestones | 是 | 只查里程碑条目 |
| GET | /api/v1/moments/{id} | 是 | 单条时刻详情 |
| PUT | /api/v1/moments/{id} | 是 | 编辑时刻的文字/照片/里程碑 |
| DELETE | /api/v1/moments/{id} | 是 | 软删除 |

### 数据库变更
- migration 文件：`V3__moments.sql`
- 新增表：`moments` (id, pet_id, content, photos JSON, occurred_at, is_milestone, milestone_label, location, created_at, updated_at, is_deleted)

### 请求/响应示例

**创建时刻**
```bash
curl -X POST http://localhost:8080/api/v1/pets/1/moments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"content":"奶茶第一次跳上窗台晒太阳。","photos":[],"occurredAt":"2026-05-28 14:30:00","isMilestone":true,"milestoneLabel":"第一次","location":"家里窗台"}'
```
```json
{"code":200,"message":"ok","data":{"id":1,"petId":1,"content":"奶茶第一次跳上窗台晒太阳。","photos":[],"occurredAt":"2026-05-28T14:30:00","isMilestone":true,"milestoneLabel":"第一次","location":"家里窗台","createdAt":"2026-05-29T18:20:00"}}
```

**时间线分页**
```bash
curl "http://localhost:8080/api/v1/pets/1/moments?sort=desc&page=1&size=20" \
  -H "Authorization: Bearer <token>"
```
```json
{"code":200,"message":"ok","data":{"total":1,"page":1,"size":20,"records":[...]}}
```

**里程碑列表**
```json
{"code":200,"message":"ok","data":[{"id":1,...,"isMilestone":true,"milestoneLabel":"第一次"}]}
```

### 测试结果
- `mvn test`：12/12 通过 (Auth:4 + Pet:4 + Moment:4)
- 关键单测：MomentServiceTest — 创建/权限/分页/删除
- curl 冒烟：创建、时间线列表、里程碑筛选、更新、删除

### 前端接入建议
- 新增 api 文件：`api/moment.js`（时刻 CRUD + 里程碑查询）
- 新增 composable：`composables/useMoments.js`
- 新增/修改页面组件：
  - `views/TimelineView.vue` — 在选中宠物的主区域渲染时间线列表
  - `components/timeline/TimelineList.vue` — 时间线列表容器（含排序切换）
  - `components/timeline/TimelineItem.vue` — 单条时间线条目
  - `components/timeline/MomentForm.vue` — 创建/编辑时刻表单
  - `components/timeline/TimelineEmpty.vue` — 空时间线引导
- loading/empty/error: 时间线加载骨架屏、空时间线引导「记录第一个时刻」、error 重试按钮

### 验收标准
- [x] 创建时刻：content/photos/occurredAt/isMilestone/milestoneLabel/location
- [x] 时间线分页：支持 sort asc/desc，默认 desc
- [x] 里程碑筛选：GET milestones 只返回 is_milestone=1 的条目
- [x] 权限校验：跨用户操作宠物返回 403
- [ ] 前端接入后如发现字段不匹配，反馈给后端修正

### 下一步
Solo 模式 → frontend-builder 接入本切片，回执落盘后进入切片 4（里程碑与卡片）。
