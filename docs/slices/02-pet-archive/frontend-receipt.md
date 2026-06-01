## 前端回执：宠物档案

### 接入状态
- 前端状态：已完成
- 对接后端交接包：`backend-handoff.md`
- 前端可以放行后端继续下一个切片：是

### 完成内容
- api 文件：`api/pet.js`（createPet, getPetList, getPet, updatePet, deletePet, uploadPetAvatar, uploadFile）
- composable：`composables/usePets.js`（pets 列表、currentPet 选中、CRUD + 头像上传，loading/error 状态）
- 页面/组件：
  - `views/PetCreateView.vue` — 创建宠物页，含头像上传 + PetForm
  - `views/PetDetailView.vue` — 宠物详情/编辑页，含头像、表单、删除确认
  - `views/TimelineView.vue` — 重构为侧栏+主内容布局，显示宠物列表和时间线
  - `components/pet/PetCard.vue` — 宠物信息卡片（头像、名字、品种、年龄、操作按钮）
  - `components/pet/PetSwitcher.vue` — 侧栏宠物切换器
  - `components/pet/PetForm.vue` — 宠物创建/编辑表单（物种按钮组、日期选择、简介）
  - `components/pet/PetAvatar.vue` — 头像上传组件（圆形头像+覆盖层+loading）
- 样式和交互：
  - 全部使用 CSS 变量
  - 物种选择用按钮组（猫/狗/兔/鸟/仓鼠/鱼/其它）
  - PetCard 计算年龄（天/月/岁）
  - 删除确认弹窗
  - 四种状态：正常、加载中、空数据、错误

### 联调结果
- 成功场景：创建宠物自动选中，列表同步更新，头像上传后即时刷新
- 空状态：无宠物时显示引导创建
- 错误状态：后端错误文本展示、表单校验错误中文提示
- 权限/Token 状态：401 跳转登录

### 测试结果
- `npm run build`：通过 (104 modules, built in 1.11s)
- 页面状态验证：创建页/详情页/时间线主页 四种状态覆盖
- 组件自底向上：PetAvatar → PetCard → PetForm → PetSwitcher → 页面

### 发现的问题
无。后端字段命名使用 camelCase，前端直接映射。

### 给后端的反馈
- 无需修正
- 可以进入下一个切片：切片 3 — 时间线核心
