<script setup>
import ThemedIcon from '../common/ThemedIcon.vue'

const props = defineProps({
  viewMode: { type: String, default: 'timeline' },
  milestoneOnly: { type: Boolean, default: false },
  filterMonth: { type: String, default: '' },
  filterTag: { type: String, default: '' },
  months: { type: Array, default: () => [] },
  tags: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:viewMode', 'update:milestoneOnly', 'update:filterMonth', 'update:filterTag'])
</script>

<template>
  <div class="album-filter">
    <div class="filter-row">
      <div class="view-switch">
        <button
          class="view-btn"
          :class="{ 'is-active': viewMode === 'timeline' }"
          @click="emit('update:viewMode', 'timeline')"
        >
          <ThemedIcon name="scroll" :size="13" />
          <span>时间轴</span>
        </button>
        <button
          class="view-btn"
          :class="{ 'is-active': viewMode === 'wall' }"
          @click="emit('update:viewMode', 'wall')"
        >
          <ThemedIcon name="images" :size="13" />
          <span>相册墙</span>
        </button>
      </div>

      <div class="filter-group">
        <button
          class="filter-btn"
          :class="{ 'is-active': !milestoneOnly }"
          @click="emit('update:milestoneOnly', false)"
        >全部</button>
        <button
          class="filter-btn"
          :class="{ 'is-active': milestoneOnly }"
          @click="emit('update:milestoneOnly', true)"
        >
          <ThemedIcon name="flag" :size="11" />
          里程碑
        </button>
      </div>
    </div>

    <div class="filter-row">
      <div class="filter-chips" v-if="months.length > 0">
        <span class="filter-label">月份</span>
        <button
          class="chip"
          :class="{ 'is-active': !filterMonth }"
          @click="emit('update:filterMonth', '')"
        >全部</button>
        <button
          v-for="m in months"
          :key="m.value"
          class="chip"
          :class="{ 'is-active': filterMonth === m.value }"
          @click="emit('update:filterMonth', filterMonth === m.value ? '' : m.value)"
        >{{ m.label }}</button>
      </div>
    </div>

    <div class="filter-row" v-if="tags.length > 0">
      <div class="filter-chips">
        <span class="filter-label">标签</span>
        <button
          class="chip"
          :class="{ 'is-active': !filterTag }"
          @click="emit('update:filterTag', '')"
        >全部</button>
        <button
          v-for="t in tags"
          :key="t"
          class="chip"
          :class="{ 'is-active': filterTag === t }"
          @click="emit('update:filterTag', filterTag === t ? '' : t)"
        >{{ t }}</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.album-filter {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 1.5rem;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.view-switch {
  display: flex;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  overflow: hidden;
  flex-shrink: 0;
}

.view-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 14px;
  border: none;
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.8rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast);
}

.view-btn + .view-btn {
  border-left: 1px solid var(--border-default);
}

.view-btn:hover {
  background: var(--accent-surface);
  color: var(--accent);
}

.view-btn.is-active {
  background: var(--accent);
  color: var(--text-inverse);
}

.filter-group {
  display: flex;
  gap: 4px;
}

.filter-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 4px 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-full);
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.78rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast);
}

.filter-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.filter-btn.is-active {
  background: var(--accent);
  border-color: var(--accent);
  color: var(--text-inverse);
}

.filter-chips {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 0.72rem;
  color: var(--text-muted);
  letter-spacing: 0.04em;
  flex-shrink: 0;
}

.chip {
  padding: 2px 10px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.72rem;
  color: var(--text-muted);
  cursor: pointer;
  transition: all var(--duration-fast);
}

.chip:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.chip.is-active {
  background: var(--accent);
  border-color: var(--accent);
  color: var(--text-inverse);
}

@media (max-width: 640px) {
  .filter-row {
    gap: 8px;
  }

  .filter-label {
    display: none;
  }
}
</style>
