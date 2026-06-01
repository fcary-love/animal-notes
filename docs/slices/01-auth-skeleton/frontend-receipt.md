## 前端回执：认证与项目骨架

### 接入状态
- 前端状态：已完成
- 对接后端交接包：`backend-handoff.md`
- 前端可以放行后端继续下一个切片：是

### 完成内容
- api 文件：`api/http.js`（axios + 拦截器）、`api/auth.js`（register/login/getMe）
- composable：`composables/useAuth.js`（登录/注册/me/登出/本地恢复）
- 页面/组件：
  - `views/LoginView.vue` — 登录页，含表单校验、loading、错误态
  - `views/RegisterView.vue` — 注册页
  - `views/TimelineView.vue` — 时间线占位空状态
  - `components/layout/AppHeader.vue` — 顶部导航栏
- 样式和交互：
  - 审美方向：自然主义科学手稿 — 暖纸色系、干叶绿主色、干花陶土点缀
  - 设计令牌全部 CSS 变量
  - 字体：Noto Serif SC + LXGW WenKai
  - 纸张纤维噪点纹理
  - 路由守卫：未登录 → `/login`，已登录 → `/timeline`

### 联调结果
- 成功场景：注册后跳转 `/timeline`，token 存储 localStorage，AppHeader 显示昵称
- 空状态：TimelineView 显示「还没有宠物档案」引导
- 错误状态：登录/注册失败显示后端返回的错误消息
- 权限/Token 状态：401 响应自动清除 token 跳转登录页，路由守卫拦截未登录用户

### 测试结果
- `npm run build`：通过 (built in 1.23s)
- 页面状态验证：登录页/注册页四种状态均覆盖
- 路由守卫：已登录用户无法访问登录/注册页，未登录用户无法访问 /timeline

### 发现的问题
无。后端 API 字段与蓝图一致。

### 给后端的反馈
- 无需修正
- 可以进入下一个切片：切片 2 — 宠物档案
