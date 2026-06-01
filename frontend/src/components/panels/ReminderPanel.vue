<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { usePets } from '../../composables/usePets'
import { getHealthSummary } from '../../api/health'
import ThemedIcon from '../common/ThemedIcon.vue'

const { pets, currentPet, fetchPets } = usePets()
const loaded = ref(false)
const loading = ref(false)
const newTodo = ref('')
const localTodos = ref([])
const healthTodos = ref([])

const storageKey = computed(() => `animal-notes:todos:${currentPet.value?.id || 'all'}`)
const reminders = computed(() => [...healthTodos.value, ...localTodos.value])
const doneCount = computed(() => reminders.value.filter(r => r.done).length)

function saveLocalTodos() {
  localStorage.setItem(storageKey.value, JSON.stringify(localTodos.value))
}

function loadLocalTodos() {
  try {
    localTodos.value = JSON.parse(localStorage.getItem(storageKey.value) || '[]')
  } catch {
    localTodos.value = []
  }
}

function formatDue(dateText) {
  if (!dateText) return ''
  const days = Math.ceil((new Date(dateText) - new Date()) / 86400000)
  if (days < 0) return `已过期 ${Math.abs(days)} 天`
  if (days === 0) return '今天到期'
  return `还有 ${days} 天`
}

async function loadHealthTodos() {
  const pet = currentPet.value || pets.value[0]
  healthTodos.value = []
  if (!pet) {
    loaded.value = true
    return
  }
  loading.value = true
  try {
    const res = await getHealthSummary(pet.id)
    const summary = res.data || {}
    const items = []
    if (summary.nextVaccineDate) {
      items.push({ id: `vaccine-${pet.id}`, text: `${pet.name} 疫苗提醒`, done: false, tag: '健康', due: formatDue(summary.nextVaccineDate), locked: true })
    }
    if (summary.nextDewormingDate) {
      items.push({ id: `deworm-${pet.id}`, text: `${pet.name} 驱虫提醒`, done: false, tag: '健康', due: formatDue(summary.nextDewormingDate), locked: true })
    }
    healthTodos.value = items
  } finally {
    loading.value = false
    loaded.value = true
  }
}

function addTodo() {
  const text = newTodo.value.trim()
  if (!text) return
  localTodos.value.unshift({ id: Date.now(), text, done: false, tag: '手记', due: '' })
  newTodo.value = ''
  saveLocalTodos()
}

function toggleTodo(item) {
  if (item.locked) return
  item.done = !item.done
  saveLocalTodos()
}

function removeTodo(item) {
  if (item.locked) return
  localTodos.value = localTodos.value.filter(r => r.id !== item.id)
  saveLocalTodos()
}

function addDailyCarePlan() {
  const pet = currentPet.value || pets.value[0]
  const name = pet?.name || '宠物'
  const templates = [
    `给 ${name} 换一碗新鲜水`,
    `陪 ${name} 玩 15 分钟`,
    `检查 ${name} 的食碗和猫砂盆`
  ]
  const existing = new Set(localTodos.value.map(t => t.text))
  const additions = templates
    .filter(text => !existing.has(text))
    .map((text, i) => ({ id: Date.now() + i, text, done: false, tag: '日常', due: '今天' }))
  localTodos.value = [...additions, ...localTodos.value]
  saveLocalTodos()
}

onMounted(async () => {
  if (pets.value.length === 0) await fetchPets()
  loadLocalTodos()
  await loadHealthTodos()
})

watch(currentPet, async () => {
  loadLocalTodos()
  await loadHealthTodos()
})
</script>

<template>
  <div class="panel-content" :class="{ loaded }">
    <section class="rem-section">
      <div class="rem-progress">
        <div class="rem-progress-bar">
          <div class="rem-progress-fill" :style="{ width: reminders.length ? (doneCount / reminders.length * 100) + '%' : '0%' }"></div>
        </div>
        <span class="rem-progress-text">{{ doneCount }} / {{ reminders.length }} 已完成</span>
      </div>
    </section>

    <section class="rem-section">
      <form class="rem-add" @submit.prevent="addTodo">
        <input v-model="newTodo" class="rem-input" placeholder="添加新的待办..." maxlength="60" />
        <button type="submit" class="rem-add-btn" :disabled="!newTodo.trim()">+</button>
      </form>
    </section>

    <section class="rem-section">
      <TransitionGroup name="todo" tag="div" class="rem-list">
        <div v-for="(item, idx) in reminders" :key="item.id" class="rem-item" :class="{ done: item.done, locked: item.locked }" :style="{ '--i': idx }">
          <button class="rem-check" @click="toggleTodo(item)" :disabled="item.locked">
            <ThemedIcon v-if="item.done" name="bookmark" :size="12" />
          </button>
          <span class="rem-text">{{ item.text }}</span>
          <span v-if="item.due" class="rem-due">{{ item.due }}</span>
          <span class="rem-tag">{{ item.tag }}</span>
          <button class="rem-remove" @click="removeTodo(item)" title="删除" :disabled="item.locked">×</button>
        </div>
      </TransitionGroup>
      <div v-if="!reminders.length" class="rem-empty">{{ loading ? '同步提醒中...' : '暂无待办提醒' }}</div>
    </section>

    <section class="rem-section">
      <button class="care-plan-btn" @click="addDailyCarePlan">
        <ThemedIcon name="quill" :size="16" />
        生成今日照护清单
      </button>
    </section>
  </div>
</template>

<style scoped>
.panel-content { opacity: 0; transform: translateY(12px); transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1); }
.panel-content.loaded { opacity: 1; transform: translateY(0); }
.rem-section { margin-bottom: 18px; }
.rem-progress { display: flex; align-items: center; gap: 10px; }
.rem-progress-bar { flex: 1; height: 6px; background: var(--bg-aged); border-radius: 3px; overflow: hidden; }
.rem-progress-fill { height: 100%; background: linear-gradient(90deg, var(--accent), var(--accent-light)); border-radius: 3px; transition: width 0.5s cubic-bezier(0.16, 1, 0.3, 1); }
.rem-progress-text { font-size: 0.7rem; color: var(--text-muted); white-space: nowrap; }
.rem-add { display: flex; gap: 8px; }
.rem-input {
  flex: 1; padding: 10px 14px; border: 1px solid var(--border-subtle); border-radius: var(--radius-full);
  background: var(--bg-input); font-family: var(--font-body); font-size: 0.82rem; color: var(--text-primary); outline: none;
}
.rem-input:focus { border-color: var(--accent); }
.rem-add-btn {
  width: 40px; height: 40px; border-radius: 50%; border: 1px solid var(--accent); background: var(--accent); color: #fff;
  font-size: 1.2rem; cursor: pointer; line-height: 1;
}
.rem-add-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.rem-list { display: flex; flex-direction: column; gap: 7px; }
.rem-item {
  display: flex; align-items: center; gap: 8px; padding: 11px 12px; background: var(--bg-warm);
  border: 1px solid var(--border-subtle); border-radius: var(--radius-lg);
}
.rem-item.done { background: var(--bg-surface); }
.rem-item.done .rem-text { color: var(--text-muted); text-decoration: line-through; }
.rem-item.locked { border-color: rgba(193, 123, 96, 0.24); }
.rem-check {
  width: 22px; height: 22px; border-radius: 50%; border: 1.5px solid var(--border-default); background: var(--bg-surface);
  cursor: pointer; display: flex; align-items: center; justify-content: center; color: var(--accent); flex-shrink: 0;
}
.rem-check:disabled { cursor: default; opacity: 0.55; }
.rem-text { flex: 1; font-size: 0.82rem; color: var(--text-secondary); line-height: 1.5; }
.rem-due, .rem-tag {
  font-size: 0.62rem; padding: 2px 7px; border: 1px solid var(--border-subtle); border-radius: var(--radius-full); white-space: nowrap; color: var(--text-muted);
}
.rem-tag { color: var(--accent); border-color: rgba(92, 122, 94, 0.24); }
.rem-remove { width: 22px; height: 22px; border: none; background: transparent; color: var(--text-muted); cursor: pointer; border-radius: 50%; }
.rem-remove:hover:not(:disabled) { background: rgba(181, 83, 61, 0.08); color: var(--danger); }
.rem-remove:disabled { visibility: hidden; }
.rem-empty { text-align: center; color: var(--text-muted); font-size: 0.78rem; padding: 14px 0; }
.care-plan-btn {
  width: 100%; display: flex; align-items: center; justify-content: center; gap: 8px;
  padding: 12px 14px; border: 1px solid var(--accent); border-radius: var(--radius-full);
  background: transparent; color: var(--accent); font-family: var(--font-body); cursor: pointer;
}
.care-plan-btn:hover { background: var(--accent); color: #fff; }
.todo-enter-active, .todo-leave-active { transition: all 0.25s ease; }
.todo-enter-from, .todo-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
