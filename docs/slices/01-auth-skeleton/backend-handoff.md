## 前端交接包：认证与项目骨架

### 本轮状态
- 后端状态：已完成
- 覆盖故事：S1（部分 — 宠物档案的前置认证部分）
- 依赖切片：无
- 前端可以开始接入：是

### 变更记录
与蓝图一致。唯一调整：升级到 Spring Boot 3.2.5 + jjwt 0.12.6（适配 JDK 21），API 契约不变。

### 本轮接口速查

| 方法 | 路径 | Token | 说明 |
|------|------|-------|------|
| POST | /api/v1/auth/register | 否 | 注册 |
| POST | /api/v1/auth/login | 否 | 登录 |
| GET | /api/v1/auth/me | 是 | 当前用户信息 |

### 数据库变更
- migration 文件：`V1__auth_users.sql`
- 新增表：`users` (id, username, password, nickname, avatar_url, created_at, updated_at, is_deleted)

### 请求/响应示例（实际跑过的）

**注册成功**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"catlover","password":"123456","nickname":"奶茶妈妈"}'
```
```json
{"code":200,"message":"注册成功","data":{"id":1,"username":"catlover","nickname":"奶茶妈妈","avatarUrl":null,"token":"eyJ..."}}
```

**用户名已注册**
```json
{"code":409,"message":"用户名已被注册","data":null}
```

**登录成功**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"catlover","password":"123456"}'
```
```json
{"code":200,"message":"ok","data":{"id":1,"username":"catlover","nickname":"奶茶妈妈","avatarUrl":null,"token":"eyJ..."}}
```

**密码错误**
```json
{"code":401,"message":"用户名或密码错误","data":null}
```

**Token 缺失**
```json
{"code":401,"message":"未登录或登录已过期","data":null}
```

### 测试结果
- `mvn test`：通过 (4/4)
- 关键单测：AuthServiceTest — 注册/重复用户/登录/密码错误 四个场景
- curl 冒烟：注册、登录、me 均已通过

### 前端接入建议
- 需要新增/修改的 api 文件：`api/http.js` + `api/auth.js`
- 需要新增/修改的 composable：`composables/useAuth.js`
- 需要接入的页面/组件：LoginView.vue, RegisterView.vue, router 路由守卫, AppHeader.vue 用户头像区域
- loading/empty/error 状态建议：登录/注册按钮 loading 态、表单验证错误提示、401 自动跳转登录页

### 验收标准
- [x] 注册接口：新用户注册成功，重复用户名拒绝
- [x] 登录接口：正确密码返回 token，错误密码返回 401
- [x] me 接口：有效 token 返回用户信息，无效 token 返回 401
- [ ] 前端接入后如发现字段/状态码不匹配，应反馈给后端修正

### 蓝图修正建议
- 无需修正

### 下一步
Solo 模式：切换到 frontend-builder 接入本切片，前端回执落盘后再继续切片 2（宠物档案）。
