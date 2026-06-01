---
name: frontend-builder
description: 根据项目架构蓝图、docs/mvp-progress.md 和后端交接包，实现完整可运行的 Vue3 前端项目。分两阶段执行：第一阶段基于 API 契约和全局 mock 数据完成美学选型、布局骨架、组件结构和样式；第二阶段按纵向切片接入真实 API、联调、测试并写入前端回执。当用户说"帮我实现前端""搭建前端项目""写XX页面的前端代码"时使用。
argument-hint: "[架构蓝图 + 可选：实际API文档]"
model: opus
effort: high
---

## 目标

拿到架构蓝图、`docs/mvp-progress.md` 和后端交接包，分阶段生成**完整可运行 + 有设计感**的 Vue3 前端项目。默认按"后端一个功能切片 → 前端接入并完善 → 写入回执 → 后端继续下一个切片"的节奏推进。整个项目有两条并行线程：

- **功能线程**：API 调用 → 状态管理 → 数据流 → DOM 渲染。保证"能用"。
- **审美线程**：美学方向 → 设计令牌 → 字体/配色/动效 → 氛围细节。保证"好用且记得住"。

两条线程各有自己的执行节奏。功能线程按蓝图逐步推进，审美线程**在每个步骤中同时介入**。

---

## 双线程模型

本技能内部包含了完整的审美能力。但如果你希望得到更极致、更多样的视觉设计，可以在**第 1 步**时显式加载 `frontend-design-cn` 技能来主导审美决策。

```
本技能（frontend-builder）
├── 功能线程 ← 本技能直接处理
│   ├── API 对接
│   ├── 状态管理
│   ├── 组件逻辑
│   └── 路由 & 导航
│
└── 审美线程 ← 优先委托 frontend-design-cn 技能
    ├── 美学方向选择
    ├── 字体 & 配色
    ├── 空间构成
    ├── 动效设计
    └── 氛围纹理
```

**执行规则**：当本技能进行到与"视觉、样式、动效、配色、字体、氛围"相关的决策时，优先调用 `frontend-design-cn` 技能的设计原则来做判断。功能线程（API 对接、数据流、路由）由本技能独立完成。

---

## 核心原则

1. **数据接入和 UI 渲染分两层写**：修正接口字段时只改 api/*.js 和 composable，不动组件模板和样式
2. **先定契约和 mock 数据，再做 UI**：第一阶段用契约一致的 mock 数据驱动页面，第二阶段只替换真实 API 和字段映射
3. **每个页面处理四种状态**：正常、加载中、空数据、错误
4. **设计决策先于代码**：在写第一个组件样式之前，审美方向必须已经定下来
5. **从 mock 数据起步**：第一阶段 mock 数据的结构必须与蓝图 §4 的 API 响应格式一致，保证第二阶段换数据源时字段名不用大面积改
6. **审美优先委托**：涉及视觉决策时，以 `frontend-design-cn` 为准，本技能不做二次判断
7. **只有一个数据解包点**：API 响应的 `{code, data}` 结构**只在 composable 中解包**，组件通过 composable 拿到的就是最终数据。组件代码里绝不出现 `res.data` 或 `(res.data || res)` 这样的解包逻辑
8. **一次只接一个后端切片**：收到后端交接包后，只完成该切片涉及的 api/composable/页面/组件和联调反馈，不主动扩展下一个切片
9. **交付物必须落盘**：前端回执、联调结果和进度状态必须写入 `docs/slices/` 与 `docs/mvp-progress.md`
10. **全局 mock 开关**：不要在每个 api 模块散落 `USE_MOCK`，统一从 `api/mock/index.js` 或环境变量读取

---

## 前置：读取并理解输入

开始编码前，先解析以下输入：

**必有：架构蓝图**
- §4 API 契约：全部端点、请求/响应格式
- §6 前端架构：路由表、组件树、状态策略、API 映射表

**可选：后端交接包 / 实际 API 文档**（后端已就绪时提供）
- 与蓝图有差异的端点列表
- 实际跑过的响应 JSON
- 本轮切片名称、验收标准、前端接入建议
- `docs/slices/NN-[slice-name]/backend-handoff.md`
- `docs/slices/NN-[slice-name]/api-examples.md`

如果只有蓝图没有后端交接包，进入第一阶段（mock 模式）。如果有本轮后端交接包，优先只接入该切片；仍先建立或修正 api/composable 数据层，再实现组件与样式。

---

## 执行流程

### 第 0 步：确定本轮前端切片

开始编码前，先确定本轮范围：

- 如果收到后端交接包，切片名称、端点、字段和验收标准以交接包为准
- 如果只有架构蓝图，选择功能切片队列中的第一个前端可做切片，用 mock 数据实现
- 如果用户指定功能名，只做该功能涉及的前端范围

输出本轮范围：切片名称、页面/组件、api 文件、composable、验收标准、回执保存路径。后续代码不要越出这个范围。

**模式检测**：读取 `docs/mvp-progress.md` 的"开发模式"字段。若为 Solo，回执落盘后自动提示切换到 backend-builder 进入下一个切片；若为协作，停止等待后端下一轮交接包。

本轮持久化路径固定为：

```text
docs/slices/NN-[slice-name]/
├── backend-handoff.md
├── frontend-receipt.md
└── api-examples.md
```

### 第一阶段：契约驱动 UI 先行（mock 模式，不依赖后端）

---

#### 第 1 步：使用 frontend-design-cn 主导审美决策

这一步**不写代码**，只做设计决策。如果 `frontend-design-cn` 已加载，按它的"设计思考"流程执行；如果不可用，则按本节的精简规则完成以下输出：

**1.1 理解项目场景**（从架构蓝图 §1 中拿）

- 这个界面解决什么问题？谁在用？
- 用户使用时的情绪状态是什么？

**1.2 选定美学基调**（从 `frontend-design-cn` 的基调列表中选一个极端方向）

```
选项示例（不止这些）：
极致极简 / 繁复主义 / 复古未来 / 自然有机 / 奢华精致
杂志编辑风 / 粗野主义 / 装饰艺术 / 柔和粉彩 / 工业实用风
深空观测 / 赛博禅意 / 胶片暗房 / 水墨留白 / 机械蓝图
```

选定一个方向，并找**一个实体世界的参照物**（不是网页），用它锁定色彩倾向和质感。

**1.3 定字体搭配**（遵循 `frontend-design-cn` 的字体指南）

- 展示字体（大标题用）：要有辨识度，不能用 Inter/Roboto/Arial
- 正文字体（大段阅读）：保证可读性，中文优先思源/霞鹜文楷/阿里系列
- 等宽字体（代码/数据展示）：JetBrains Mono 或同类

**1.4 定配色方案**（遵循 `frontend-design-cn` 的色彩指南）

- 背景色阶（3~4 个层级）
- 文字色阶（主要/次要/禁用）
- 主色调 + 点缀色（只用一种点缀色）
- 语义色（成功/警告/错误）

**1.5 定空间策略**（遵循 `frontend-design-cn` 的空间构成指南）

- 留白策略：大面积留白 还是 高密度控制
- 卡片风格：直角 / 小圆角 / 大圆角 / 混合
- 阴影策略：无阴影扁平 / 柔和投影 / 强烈层次

这一步的输出是一个明确的**设计决策卡片**，写在 `base.css` 文件头部注释中，后续所有样式决策都回溯到这里。

---

#### 第 2 步：创建设计令牌（base.css）

把第 1 步的审美决策转化为 CSS 变量。**变量命名和结构由 `frontend-design-cn` 的色彩与主题原则指导。**

```css
/* ============================================
   [项目名] — [美学方向名称]
   参照物：[实体参照物]
   三个关键词：[词1]、[词2]、[词3]
   ============================================ */

:root {
  /* 背景色阶 */
  --bg-abyss: ...;
  --bg-deep: ...;
  --bg-base: ...;
  --bg-surface: ...;
  --bg-card: ...;
  --bg-input: ...;

  /* 文字色阶 */
  --text-primary: ...;
  --text-secondary: ...;
  --text-muted: ...;
  --text-inverse: ...;

  /* 主色 + 点缀 */
  --accent: ...;
  --accent-glow: ...;
  --accent-surface: ...;

  /* 语义色 */
  --success: ...;
  --warning: ...;
  --danger: ...;

  /* 边框 */
  --border-subtle: ...;
  --border-default: ...;
  --border-strong: ...;

  /* 阴影 */
  --shadow-sm: ...;
  --shadow-card: ...;
  --shadow-elevated: ...;

  /* 圆角 */
  --radius-sm: ...;
  --radius-md: ...;
  --radius-lg: ...;
  --radius-full: 999px;

  /* 字体（来自第 1 步的决策） */
  --font-display: ...;
  --font-body: ...;
  --font-mono: ...;

  /* 动效 */
  --ease-out: cubic-bezier(0.16, 1, 0.3, 1);
  --duration-fast: 0.15s;
  --duration-normal: 0.25s;
}
```

**关键规则**：后续所有 `.css` 文件只引用这些变量，**绝不出现硬编码颜色值**。这保证了如果审美方向要调整，改一个文件即可。

---

#### 第 3 步：创建项目脚手架

```bash
npm create vite@latest frontend -- --template vue
```

安装依赖：
```bash
npm install vue-router axios [UI库]
```

配置 `vite.config.js`：dev server 端口、`/api` 和 `/files` 代理到后端地址。

配置 `index.html`：加载第 1 步选定的 Google Fonts / 字体 CDN。

---

#### 第 4 步：搭布局骨架

按蓝图 §6 的路由表创建：

- `router/index.js`：全部路由配置，先用空白占位页面
- `App.vue`：全局布局壳子（导航栏 + `<router-view>` + 侧栏/底部栏）
- `components/layout/` 下的全局布局组件

此时浏览器应能看到一个**风格完整的空壳**——导航栏、侧栏、页面切换动画都已就绪，只是内容区是空白的。

---

#### 第 5 步：创建 API 调用层和 Mock 数据

**5.1 axios 实例（`api/http.js`）**

- baseURL 从环境变量或 proxy 配置推导
- **响应拦截器必须做 `res => res.data`**：所有 API 调用返回的是 axios 的 `response` 对象（`{data: {code, data}}`），拦截器直接返回 `response.data`（`{code, data}`），这样 mock 和真实 API 的返回格式完全一致
- 请求拦截器：**统一从 `localStorage.getItem('token')` 读取**。不要出现两种 token key（如 `app_token` vs `token`）。如果项目有多个端，用同一个 key 存储，或用常量文件统一定义
- 401 拦截：清 token → 跳对应登录页

**5.2 全局常量文件（`utils/constants.js`）**

新建此文件，定义所有 localStorage key：

```js
// utils/constants.js
export const STORAGE_KEYS = {
  TOKEN: 'token',
  USER: 'user'
}
```

所有 composable 和组件中需要读写 localStorage 时，必须用 `STORAGE_KEYS.TOKEN`，绝不手写字符串 `'token'`。

> 这个项目踩过的坑：APP 端用 `app_token`，管理端用 `token`，http 请求拦截器只读了 `token`，导致 APP 端登录后接口请求不带 Token，全部 401。统一用一个 key 后解决。

**5.3 API 调用函数**

按蓝图 §6 的 API 映射表，每个后端资源组创建一个 `api/[模块].js`。每个函数签名与蓝图 §4 的端点一一对应。

mock 开关必须集中管理，不要在每个模块文件里各自定义。创建 `api/mock/index.js`：

```js
// api/mock/index.js
// 按资源控制 mock 状态，联调时逐资源关闭
// 未列出的资源默认走 mock
export const mockEnabled = {
  auth: true,
  // orders: false,   // 已接入真实 API，关闭 mock
  // products: true,  // 仍用 mock
}

// 查询某资源是否启用 mock
export const useMock = (resource) => {
  return mockEnabled[resource] ?? true
}
```

各 `api/[模块].js` 按自己资源名调用 `useMock('[资源名]')` 判断。全局环境变量 `VITE_USE_MOCK=false` 作为兜底——设为 false 时强制关闭所有 mock。mock 和真实 API 返回的结构必须一致，所以真实 API 调用不要做额外处理。

**5.4 Mock 数据**

在 `api/mock/` 下，按蓝图 §4 的响应 JSON 格式创建 mock 数据。数据结构必须与蓝图一致——字段名、嵌套层级、分页格式，保证后续从 mock 切换到真实 API 时只改 `mockEnabled` 中的对应资源开关。

#### 第 6 步：创建 composable（状态管理层）

每个数据域一个 composable，统一封装 `{ data, loading, error, execute() }`：

```js
// composables/useXxx.js
import { ref } from 'vue'
import { fetchXxxList } from '../api/xxx'

export function useXxx() {
  const data = ref(null)
  const loading = ref(false)
  const error = ref('')

  async function fetch(params) {
    loading.value = true
    error.value = ''
    try {
      const res = await fetchXxxList(params)
      // ⚠️ 数据解包只在 composable 里做，组件不碰 res
      data.value = res.data  // axios 拦截器已做第一层解包，这里只取 data 字段
    } catch (e) {
      error.value = e?.response?.data?.message || e.message || '加载失败'
    } finally {
      loading.value = false
    }
  }

  return { data, loading, error, fetch }
}
```

**关键规则**：`res.data` 这一行**只在 composable 中出现**。组件代码中绝不写 `res.data`、`(res.data || res)` 或任何解包逻辑。组件只从 composable 拿 `data`，拿到的就是最终数据。

composable 不做 DOM 操作，不引入 UI 依赖。它只有数据和状态。

---

#### 第 7 步：逐个实现组件（审美线程全程介入）

**实现顺序**（自底向上）：

```
叶子组件（按钮、输入框、标签、徽章）
  → 组合组件（卡片、列表项、表单区、对话气泡）
    → 页面组件（组装 + 布局）
```

**每个组件实现时，审美线程同步执行**：

| 功能线程做的事 | 审美线程做的事 |
|--------------|--------------|
| 写 template 结构 | 定 DOM 元素的视觉层级（氛围层→容器层→内容层→行动层） |
| 写 script 逻辑 | — |
| — | 写 `<style>` 或 CSS，只用 token 变量 |
| — | 处理四种状态的视觉差异（加载态不能只写"加载中"三个字） |
| — | 空状态要有完整布局：图标 + 标题 + 说明 + 引导按钮 |
| — | 微交互细节：hover / active / focus 的过渡动画 |

每个页面落地后对照 `frontend-design-cn` 检查：
- [ ] 没有用 Inter / Roboto / Arial / 系统默认字体
- [ ] 颜色来自 CSS 变量，没有硬编码
- [ ] 有明确的美学主张，不是"通用 AI 风格"
- [ ] 至少有一个让人记住的细节（加载动画 / hover 效果 / 空状态设计）

---

#### 第 8 步：全局样式收尾

所有组件写完后，统一处理：
- 滚动条样式
- UI 库组件覆盖（Element Plus 的输入框、弹窗、下拉等与项目风格对齐）
- 响应式断点
- 氛围纹理（噪点叠加 / 点阵网格 / 柔光渐变——选一种即可）

---

### 第二阶段：数据接入（后端就绪后）

#### 第 9 步：接入本轮真实 API

拿到后端产出的**本轮交接包**后：

1. 对比实际文档和蓝图 §4，找出差异点（路径变化、字段名变化、类型变化）
2. 只修正本轮切片涉及的 `api/*.js` 调用参数
3. 只修正本轮切片涉及的 composable 数据提取路径
4. 将本轮涉及的 mock 切换为真实 API；保留其他切片的 mock 数据，不要删除整个 `api/mock/`，直到所有切片都完成真实 API 接入

**关键**：只改 api 层和 composable 的数据提取，**不动组件的 template 和 style**。

#### 第 10 步：本轮边界状态联调

真实数据下过一遍本轮页面/组件的四种状态：

- **正常态**：数据正确展示
- **加载态**：接口慢时 loading/spinner 正常
- **空态**：新用户无数据时，空状态引导是否合理
- **错误态**：后端报错时提示清晰、可重试

额外检查：token 过期跳登录、防重复提交、文件上传大小/格式提示。

#### 第 10.5 步：本轮测试门槛

每个切片回执前至少完成：

- `npm run build` 可通过；如果失败，回执必须记录失败原因和剩余风险
- 关键 composable 的核心数据转换逻辑有 vitest 单测
- 本轮页面至少验证正常、加载、空、错误四种状态（手动走一遍，截图或记录）
- 认证相关切片必须验证未登录跳转、登录成功、Token 过期
- 文件上传切片必须验证大小、格式、进度/错误提示

#### 第 11 步：输出前端回执

本轮切片接入完成后，必须写入 `docs/slices/NN-[slice-name]/frontend-receipt.md`，供后端判断是否继续下一个切片：

```markdown
## 前端回执：[切片名称]

### 接入状态
- 前端状态：已完成 / 部分完成 / 阻塞
- 对接后端交接包：
- 前端可以放行后端继续下一个切片：是 / 否

### 完成内容
- api 文件：
- composable：
- 页面/组件：
- 样式和交互：

### 联调结果
- 成功场景：
- 空状态：
- 错误状态：
- 权限/Token 状态：

### 测试结果
- `npm run build`：
- composable 测试/手动验证：
- 页面状态验证：

### 发现的问题
| 类型 | 位置 | 现象 | 建议 |
|------|------|------|------|
| API字段/状态码/业务逻辑/UI | | | |

### 给后端的反馈
- 需要后端修正：
- 可以进入下一个切片：
```

如果发现后端字段、状态码或业务语义不匹配，不要在前端硬兼容掩盖问题；在回执中明确反馈，让后端修正后再继续。

回执写入后更新 `docs/mvp-progress.md`：
- 前端完成：前端状态改为 `frontend_done`
- 联调通过：联调状态改为 `integrated`，是否放行下一个改为 `是`
- 有问题：联调状态改为 `blocked`，写清阻塞问题

---

## 代码质量标准

### 文件结构（从蓝图推导，不用固定模板）

```
src/
├── api/
│   ├── http.js
│   ├── mock/
│   │   └── index.js     ← 全局 mock 开关与 mock 数据出口
│   └── [模块].js        ← 每个后端资源组对应一个文件
├── composables/
│   └── use[Xxx].js      ← 每个数据域一个
├── components/
│   ├── layout/
│   └── [功能模块]/      ← 按蓝图 §2 的功能模块组织
├── views/
│   └── [页面].vue
├── router/
│   └── index.js
├── styles/
│   ├── base.css         ← 设计令牌（来自第 2 步）
│   ├── layout.css
│   ├── [模块].css
│   ├── element-overrides.css
│   └── responsive.css
├── App.vue
└── main.js
```

### 组件检查清单

- [ ] 四种状态都有对应的 UI 处理
- [ ] 只用 CSS 变量，不用硬编码颜色
- [ ] 空状态有完整的图标+文字+操作引导
- [ ] composable 中没有任何 DOM 操作
- [ ] 没有 `style="color: #xxx"` 内联样式
- [ ] 没有 console.log 残留
- [ ] 符合 `frontend-design-cn` 的反 AI 味检查（没有 Inter 字体、没有白底紫渐变、没有 cookie-cutter 布局）
- [ ] `npm run build` 已执行，结果写入前端回执
- [ ] `frontend-receipt.md` 已写入本轮 `docs/slices/NN-[slice-name]/`
- [ ] `docs/mvp-progress.md` 已更新

### 严禁事项

- 不要在组件 `<script>` 中直接调 axios
- 不要在 composable 中操作 DOM
- 不要内联样式
- 不要遗漏空状态和错误状态
- 不要在审美决策上"自己猜"——优先走 frontend-design-cn 的原则
- 不要在组件里写 `res.data` 或 `(res.data || res)` —— 数据解包只在 composable
- 不要在每个 api 模块各自定义 mock 开关，必须集中在 `api/mock/index.js` 的 `mockEnabled` 中管理

### 修改基础模块后的操作

修改了以下文件后，**必须重启 Vite dev server**（`Ctrl+C` 后重新 `npm run dev`）：
- `api/http.js`（axios 实例、拦截器）
- `main.js`（入口文件）
- `router/index.js`（路由配置）
- `vite.config.js`

这些文件不在 Vue 组件的 HMR 范围内，Vite 不会自动热更新。忽略这步会导致反复排查"明明代码改了为什么不生效"的问题。

### MVP 完成后的最小交付说明

最后一个切片联调通过后，补一份最小部署说明：

- 前端：`npm run build`，产物目录，Nginx 静态资源路径
- 后端：`mvn package`，jar 名称，启动命令
- 环境变量：API 地址、数据库、Redis/MinIO（如用到）、JWT 密钥
- Nginx：静态资源配置和 `/api/` 反向代理
