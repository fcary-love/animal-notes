## 前端回执：时间线核心

### 完成状态
- 前端状态：已完成
- 构建验证：`npx vite build` — 通过 ✓（114 modules, 0 errors）

### 文件清单

| 文件 | 状态 | 说明 |
|------|------|------|
| `api/moment.js` | 已创建 | 6 个端点：createMoment, getTimeline, getMilestones, getMoment, updateMoment, deleteMoment |
| `composables/useMoments.js` | 已创建 | records/total/loading/error/sort 状态 + CRUD + toggleSort |
| `components/timeline/TimelineItem.vue` | 已创建 | 时间轴点+线、日期、里程碑徽章、地点、内容、照片网格、hover显示编辑/删除 |
| `components/timeline/TimelineEmpty.vue` | 已创建 | 空时间线引导 "记录第一个时刻" |
| `components/timeline/TimelineList.vue` | 已重构 | 改为 props/emits 模式，接收 records/total/loading/error/sort，emit create/edit/delete/toggle-sort/fetch |
| `components/timeline/MomentForm.vue` | 已创建+修复 | 创建/编辑表单，datetime-local、内容、地点、里程碑checkbox+标签按钮组；修复了取消按钮的 v-if 表达式 |
| `views/TimelineView.vue` | 已重构 | 集成 useMoments，watch currentPet/sort 自动拉取时间线，MomentForm 内联在主区域上方，TimelineList 展示时间线 |

### 数据流

```
useMoments (TimelineView 持有)
  ├─ records, total, loading, error, sort  → TimelineList (props)
  ├─ create/update/remove                  → MomentForm / TimelineItem 事件
  ├─ fetchTimeline                         → watch(currentPet) + watch(sort) 自动触发
  └─ toggleSort                             → TimelineList @toggle-sort → watch(sort) → fetchTimeline
```

### MomentForm 复用策略
- 使用 `:key="editingMoment ? editingMoment.id : 'new'"` 强制重建组件
- 创建模式：editingMoment = null，表单初始化为空
- 编辑模式：editingMoment = { ... }，表单从 modelValue 初始化

### 修复记录
1. **TimelineList**: 移除独立 useMoments() 调用和 broken Options API script 块，改为纯 props/emits
2. **MomentForm**: 修复 `v-if="emit('cancel') !== undefined || true"` → 始终显示取消按钮
3. **排序联动**: 新增 `watch(sort, ...)` 确保切换排序后自动重新拉取

### 已知未覆盖
- 照片上传功能（MomentForm 暂无照片选择器，后续切片补充）
- 里程碑筛选视图（MilestoneBadge 已含于 TimelineItem，GET /milestones API 已就绪）

### 验收核对
- [x] TimelineView 在选中宠物后展示 TimelineList
- [x] 创建时刻表单（内联，位于时间线上方）
- [x] 编辑时刻（复用 MomentForm，填充已有数据）
- [x] 删除时刻（confirm 弹窗）
- [x] 排序切换（最新在前/最早在前），切换后自动重新拉取
- [x] 空时间线引导
- [x] loading / error 状态
- [x] 构建通过
