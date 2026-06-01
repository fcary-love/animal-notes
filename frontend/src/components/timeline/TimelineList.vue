<script setup>
import TimelineItem from './TimelineItem.vue'
import TimelineEmpty from './TimelineEmpty.vue'
import ThemedIcon from '../common/ThemedIcon.vue'

defineProps({
  petId: { type: Number, required: true },
  records: { type: Array, default: () => [] },
  total: { type: Number, default: 0 },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  sort: { type: String, default: 'desc' },
  milestoneOnly: { type: Boolean, default: false }
})

const emit = defineEmits(['create', 'edit', 'delete', 'toggle-sort', 'fetch', 'toggle-milestone', 'card', 'report', 'd3d', 'health'])

function onDelete(moment) {
  if (!confirm('确定删除这个时刻吗？')) return
  emit('delete', moment)
}
</script>

<template>
  <div class="timeline-list-wrap">
    <!-- 工具栏 -->
    <div class="tl-toolbar" v-if="records.length > 0 || total > 0">
      <div class="tl-toolbar-top">
        <span class="tl-count">{{ total }} 个时刻</span>
        <button class="tl-primary-btn" @click="emit('create')">
          <ThemedIcon name="quill" :size="14" /> 记一笔
        </button>
      </div>
      <div class="tl-toolbar-groups">
        <div class="tl-group">
          <button class="tl-btn" @click="emit('toggle-milestone')" :class="{ 'is-active': milestoneOnly }">
            <ThemedIcon name="bookmark" :size="13" /> 里程碑
          </button>
          <button class="tl-btn" @click="emit('toggle-sort')">
            <ThemedIcon name="trend" :size="13" /> {{ sort === 'desc' ? '最新' : '最早' }}
          </button>
        </div>
        <span class="tl-divider"></span>
        <div class="tl-group">
          <button class="tl-btn" @click="emit('health')">
            <ThemedIcon name="stethoscope" :size="13" /> 健康
          </button>
          <button class="tl-btn" @click="emit('card')">
            <ThemedIcon name="image" :size="13" /> 卡片
          </button>
          <button class="tl-btn" @click="emit('report')">
            <ThemedIcon name="file" :size="13" /> 报告
          </button>
          <button class="tl-btn" @click="emit('d3d')">
            <ThemedIcon name="cube" :size="13" /> 3D
          </button>
        </div>
      </div>
    </div>

    <!-- loading -->
    <div class="tl-loading" v-if="loading">
      <span class="spinner"></span> 加载中...
    </div>

    <!-- error -->
    <div class="tl-error" v-else-if="error">
      <p>{{ error }}</p>
      <button @click="emit('fetch')">重试</button>
    </div>

    <!-- 空 -->
    <TimelineEmpty v-else-if="records.length === 0" @create="emit('create')" />

    <!-- 时间线 -->
    <div class="tl-list" v-else>
      <TimelineItem
        v-for="moment in records" :key="moment.id"
        :moment="moment"
        @edit="emit('edit', moment)"
        @delete="onDelete(moment)"
      />
    </div>
  </div>
</template>

<style scoped>
.timeline-list-wrap { width: 100%; }

.tl-toolbar {
  margin-bottom: 1rem;
  padding: 0.75rem;
  border-bottom: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: rgba(245, 239, 224, 0.46);
}
.tl-toolbar-top {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 0.75rem;
}
.tl-count {
  font-family: var(--font-display); font-size: 0.85rem;
  color: var(--text-muted); letter-spacing: 0.02em;
}
.tl-primary-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 18px;
  border: 1px solid var(--accent); background: var(--accent);
  color: var(--text-inverse);
  font-family: var(--font-body); font-size: 0.85rem; font-weight: 600;
  border-radius: var(--radius-full); cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  box-shadow: var(--shadow-sm);
}
.tl-primary-btn:hover { background: var(--accent-light); border-color: var(--accent-light); }

.tl-toolbar-groups {
  display: flex; align-items: center; gap: 0; flex-wrap: wrap;
}
.tl-group { display: flex; gap: 4px; }
.tl-divider {
  width: 1px; height: 20px; margin: 0 10px;
  background: var(--border-default); flex-shrink: 0;
}
.tl-btn {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 5px 11px;
  border: 1px solid var(--border-default); background: var(--bg-surface);
  color: var(--text-secondary);
  font-family: var(--font-body); font-size: 0.78rem;
  border-radius: var(--radius-full); cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  white-space: nowrap;
}
.tl-btn:hover {
  border-color: var(--accent); color: var(--accent);
  background: var(--accent-surface);
}
.tl-btn.is-active {
  border-color: var(--spot); color: var(--spot);
  background: rgba(193, 123, 96, 0.1); font-weight: 600;
}

.tl-list {
  display: flex;
  flex-direction: column;
  gap: 0.95rem;
}

@media (max-width: 600px) {
  .tl-toolbar-groups { gap: 6px; }
  .tl-divider { display: none; }
  .tl-group { flex-wrap: wrap; }
  .tl-btn { padding: 5px 9px; font-size: 0.75rem; }
  .tl-primary-btn { padding: 6px 14px; font-size: 0.82rem; }
}

.tl-loading {
  text-align: center; padding: 4rem 0; color: var(--text-muted); font-size: 0.9rem;
  display: flex; align-items: center; justify-content: center; gap: 10px;
}
.spinner {
  width: 18px; height: 18px; border: 2px solid var(--border-default);
  border-top-color: var(--accent); border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.tl-error {
  text-align: center; padding: 4rem 0; color: var(--danger);
}
.tl-error button {
  margin-top: 1rem; padding: 6px 16px;
  border: 1px solid var(--border-default); background: transparent;
  font-family: var(--font-body); font-size: 0.85rem;
  color: var(--text-secondary); cursor: pointer; border-radius: var(--radius-sm);
}
</style>
