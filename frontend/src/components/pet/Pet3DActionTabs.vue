<script setup>
import { ref } from 'vue'
import ThemedIcon from '../common/ThemedIcon.vue'

const props = defineProps({
  petId: { type: Number, required: true },
  petName: { type: String, default: '宠物' },
  agentTasks: { type: Array, default: () => [] },
  tasksLoading: { type: Boolean, default: false },
  agentMessage: { type: String, default: '' }
})

const emit = defineEmits(['selectTask', 'runTask'])

const activeTab = ref('agent')
const tabs = [
  { key: 'agent', label: 'Agent 任务', icon: 'quill' },
  { key: 'train', label: '小训练', icon: 'collar' },
  { key: 'tools', label: '成长工具', icon: 'chart' }
]
</script>

<template>
  <div class="action-tabs">
    <div class="tab-bar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-btn"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        <ThemedIcon :name="tab.icon" :size="13" />
        <span>{{ tab.label }}</span>
      </button>
    </div>

    <div class="tab-content">
      <!-- Agent 任务 -->
      <div v-if="activeTab === 'agent'" class="tab-pane">
        <div class="agent-tasks" v-if="agentTasks.length > 0">
          <div
            v-for="task in agentTasks.slice(0, 3)"
            :key="task.id"
            class="task-row"
          >
            <div class="task-info">
              <ThemedIcon :name="task.type === 'play' ? 'collar' : task.type === 'care' ? 'heartPulse' : task.type === 'photo' ? 'image' : 'scroll'" :size="12" />
              <span class="task-title">{{ task.title }}</span>
            </div>
            <p class="task-reason" v-if="task.reason">{{ task.reason }}</p>
            <button class="task-run" @click="emit('runTask', task)">
              <ThemedIcon name="paw" :size="11" /> 执行
            </button>
          </div>
        </div>
        <div class="tab-empty" v-else-if="tasksLoading">
          <span class="spinner-sm"></span> 分析中...
        </div>
        <div class="tab-empty" v-else>
          <ThemedIcon name="quill" :size="18" />
          <p>暂无推荐任务</p>
        </div>
        <p class="agent-msg" v-if="agentMessage">{{ agentMessage }}</p>
      </div>

      <!-- 小训练 -->
      <div v-if="activeTab === 'train'" class="tab-pane">
        <div class="train-entry">
          <div class="train-visual">
            <ThemedIcon name="paw" :size="34" />
          </div>
          <h3>{{ petName }}的小训练</h3>
          <p>进入独立训练场，画面更大，操作更清楚。训练结果只在游戏页显示，不会写入成长时间线。</p>
          <router-link :to="`/pet/${props.petId}/game`" class="train-link">
            进入训练场
          </router-link>
        </div>
      </div>

      <!-- 成长工具 -->
      <div v-if="activeTab === 'tools'" class="tab-pane">
        <div class="tools-grid">
          <router-link :to="`/pet/${petId}/report`" class="tool-item">
            <ThemedIcon name="file" :size="16" />
            <span>成长报告</span>
          </router-link>
          <router-link :to="`/card/${petId}`" class="tool-item">
            <ThemedIcon name="image" :size="16" />
            <span>时光卡片</span>
          </router-link>
          <router-link :to="`/pet/${petId}/album`" class="tool-item">
            <ThemedIcon name="images" :size="16" />
            <span>成长相册</span>
          </router-link>
          <router-link :to="`/pet/${petId}/health`" class="tool-item">
            <ThemedIcon name="heartPulse" :size="16" />
            <span>健康档案</span>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.action-tabs {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.tab-bar {
  display: flex;
  gap: 2px;
  padding: 0 0 8px;
  border-bottom: 1px solid var(--border-subtle);
  flex-shrink: 0;
}
.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 6px 4px;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-muted);
  font-family: var(--font-body);
  font-size: 0.72rem;
  cursor: pointer;
  transition: all var(--duration-fast);
}
.tab-btn:hover {
  color: var(--text-secondary);
  background: var(--bg-warm);
}
.tab-btn.active {
  color: var(--accent);
  border-color: var(--accent);
  background: var(--accent-surface);
  font-weight: 600;
}
.tab-content {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.tab-pane {
  height: 100%;
  overflow-y: auto;
  padding-top: 8px;
}
.tab-pane--slot {
  overflow: visible;
}

.train-entry {
  min-height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.7rem;
  text-align: center;
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-md);
  background: rgba(255, 250, 241, 0.55);
  padding: 1.2rem;
}
.train-visual {
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: var(--accent);
  background: var(--accent-surface);
  border: 1px solid rgba(92, 122, 94, 0.25);
}
.train-entry h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.05rem;
  color: var(--text-primary);
}
.train-entry p {
  max-width: 260px;
  margin: 0;
  color: var(--text-muted);
  font-size: 0.76rem;
  line-height: 1.65;
}
.train-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 150px;
  padding: 9px 18px;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: #fff;
  text-decoration: none;
  font-size: 0.84rem;
  font-family: var(--font-body);
  font-weight: 600;
}
.train-link:hover {
  background: var(--accent-light);
}

/* Agent tasks */
.agent-tasks {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.task-row {
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  padding: 7px 9px;
  transition: border-color var(--duration-fast);
}
.task-row:hover {
  border-color: var(--accent);
}
.task-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
}
.task-title {
  font-family: var(--font-body);
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--text-primary);
}
.task-reason {
  margin: 4px 0 0;
  font-size: 0.68rem;
  color: var(--text-muted);
  line-height: 1.5;
}
.task-run {
  margin-top: 6px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border: 1px solid var(--accent);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--accent);
  font-family: var(--font-body);
  font-size: 0.7rem;
  cursor: pointer;
  transition: all var(--duration-fast);
}
.task-run:hover {
  background: var(--accent);
  color: #fff;
}
.agent-msg {
  margin: 6px 0 0;
  color: var(--accent);
  font-size: 0.68rem;
  line-height: 1.5;
}

/* Tools grid */
.tools-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}
.tool-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 6px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  text-decoration: none;
  font-family: var(--font-body);
  font-size: 0.72rem;
  transition: all var(--duration-fast);
}
.tool-item:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-surface);
}

/* Empty state */
.tab-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 1.5rem 0;
  color: var(--text-muted);
  font-size: 0.76rem;
}
.spinner-sm {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 1.5px solid var(--border-default);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
