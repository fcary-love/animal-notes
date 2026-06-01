## 前端交接包：宠物档案

### 本轮状态
- 后端状态：已完成
- 覆盖故事：S1（创建宠物档案）、S7（多宠物管理）
- 依赖切片：切片 1（认证完成）
- 前端可以开始接入：是

### 变更记录
与蓝图一致。文件存储从 MinIO 变更为本地文件系统（`/files/` 路径），API 路径和字段不变。

### 本轮接口速查

| 方法 | 路径 | Token | 说明 |
|------|------|-------|------|
| POST | /api/v1/pets | 是 | 创建宠物 |
| GET | /api/v1/pets | 是 | 当前用户的宠物列表 |
| GET | /api/v1/pets/{id} | 是 | 单只宠物详情 |
| PUT | /api/v1/pets/{id} | 是 | 更新宠物信息 |
| DELETE | /api/v1/pets/{id} | 是 | 删除宠物（软删除） |
| POST | /api/v1/pets/{id}/avatar | 是 | 上传宠物头像（multipart file） |
| POST | /api/v1/files/upload | 是 | 通用文件上传 |

### 数据库变更
- migration 文件：`V2__pets.sql`
- 新增表：`pets` (id, user_id, name, species, breed, birthday, avatar_url, bio, created_at, updated_at, is_deleted)

### 请求/响应示例（实际跑过的）

**创建宠物**
```bash
curl -X POST http://localhost:8080/api/v1/pets \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"奶茶","species":"猫","breed":"英短","birthday":"2025-01-15","bio":"一只爱吃鸡胸肉的懒猫"}'
```
```json
{"code":200,"message":"ok","data":{"id":1,"userId":1,"name":"奶茶","species":"猫","breed":"英短","birthday":"2025-01-15","avatarUrl":null,"bio":"一只爱吃鸡胸肉的懒猫","createdAt":"2026-05-29T18:05:00"}}
```

**获取宠物列表**
```bash
curl http://localhost:8080/api/v1/pets -H "Authorization: Bearer <token>"
```
```json
{"code":200,"message":"ok","data":[{"id":1,"userId":1,"name":"奶茶","species":"猫","breed":"英短","birthday":"2025-01-15","avatarUrl":null,"bio":"一只爱吃鸡胸肉的懒猫","createdAt":"2026-05-29T18:05:00"}]}
```

**更新宠物**
```json
{"code":200,"message":"ok","data":{"id":1,"userId":1,"name":"珍珠","species":"猫","breed":"英短","birthday":"2025-01-15","avatarUrl":null,"bio":"还是那只懒猫","createdAt":"2026-05-29T18:05:00"}}
```

**上传头像**
```bash
curl -X POST http://localhost:8080/api/v1/pets/1/avatar \
  -H "Authorization: Bearer <token>" \
  -F "file=@cat.jpg"
```
```json
{"code":200,"message":"ok","data":{"id":1,"...","avatarUrl":"/files/avatars/uuid123.jpg",...}}
```

**不存在的宠物**
```json
{"code":404,"message":"宠物不存在","data":null}
```

**名字为空**
```json
{"code":400,"message":"name: 宠物名不能为空","data":null}
```

**无 Token**
```json
{"code":401,"message":"未登录或登录已过期","data":null}
```

### 测试结果
- `mvn test`：通过 (8/8) — AuthServiceTest(4) + PetServiceTest(4)
- 关键单测：PetServiceTest — 创建/列表/更新/权限校验
- curl 冒烟：创建、列表、更新、删除、头像上传均已通过

### 前端接入建议
- 需要新增/修改的 api 文件：`api/pet.js`（宠物 CRUD + 头像上传）
- 需要新增/修改的 composable：`composables/usePets.js`
- 需要接入的页面/组件：PetCreateView.vue、PetDetailView.vue、PetCard.vue、PetSwitcher.vue、PetForm.vue、PetAvatar.vue
- loading/empty/error 状态建议：创建中 loading、宠物列表为空引导创建、删除确认弹窗

### 验收标准
- [x] 创建宠物：必填字段 name/species，选填 breed/birthday/bio
- [x] 宠物列表：按用户过滤，按创建时间倒序
- [x] 更新宠物：不允许跨用户修改（403）
- [x] 删除宠物：软删除
- [x] 头像上传：支持 JPG/PNG/WebP，限制 10MB
- [ ] 前端接入后如发现字段/状态码不匹配，应反馈给后端修正

### 蓝图修正建议
- 文件存储方案从 MinIO 改为本地文件系统 + `/files/` 路径映射，更简单可直接开始
- 403 权限校验：蓝图设计了 403，但当前实现用 404 统一处理"无权/不存在"以保护隐私。如需显式区分，可后续调整

### 下一步
Solo 模式：切换到 frontend-builder 接入本切片，前端回执落盘后再继续切片 3（时间线核心）。
