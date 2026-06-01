<script setup>
import { ref, onBeforeUnmount } from 'vue'

const props = defineProps({
  petName: { type: String, default: '猫咪' },
  pet: { type: Object, default: null }
})
const emit = defineEmits(['complete', 'back'])

const score = ref(0)
const timeLeft = ref(30)
const started = ref(false)
const running = ref(false)
const finished = ref(false)
const passed = ref(false)
const message = ref('')

const currentItem = ref(null)
const draggedItem = ref(null)
const GOAL = 12

let timer = null

const itemTypes = [
  { kind: 'fish', emoji: '🐟', label: '小鱼干', zone: 'bowl' },
  { kind: 'toy', emoji: '🎮', label: '玩具', zone: 'toybox' },
  { kind: 'box', emoji: '📦', label: '纸箱', zone: 'shelf' }
]

const zones = [
  { id: 'bowl', emoji: '🍚', label: '食碗', accept: 'fish' },
  { id: 'toybox', emoji: '🧸', label: '玩具箱', accept: 'toy' },
  { id: 'shelf', emoji: '🗄️', label: '收纳架', accept: 'box' }
]

function randomItem() {
  const t = itemTypes[Math.floor(Math.random() * itemTypes.length)]
  return { ...t, id: Date.now() + Math.random() }
}

function spawnItem() {
  currentItem.value = randomItem()
}

function onDragStart(item, e) {
  draggedItem.value = item
  e.dataTransfer.effectAllowed = 'move'
}

function onDragOver(e) {
  e.preventDefault()
  e.dataTransfer.dropEffect = 'move'
}

function onDrop(zone) {
  if (!draggedItem.value || !running.value) return
  const item = draggedItem.value
  draggedItem.value = null

  if (item.zone === zone.id) {
    score.value++
    message.value = `正确！${item.emoji} 放入了${zone.label}`
    if (score.value >= GOAL) {
      win()
      return
    }
  } else {
    timeLeft.value = Math.max(0, timeLeft.value - 2)
    message.value = `放错了！${item.emoji} 应该去 ${zones.find(z => z.id === item.zone)?.label || '别处'}`
  }

  spawnItem()
}

function clickZone(zone) {
  if (!currentItem.value || !running.value) return
  const item = currentItem.value

  if (item.zone === zone.id) {
    score.value++
    message.value = `正确！${item.emoji} 放入了${zone.label}`
    if (score.value >= GOAL) {
      win()
      return
    }
  } else {
    timeLeft.value = Math.max(0, timeLeft.value - 2)
    message.value = `放错了！${item.emoji} 应该去 ${zones.find(z => z.id === item.zone)?.label || '别处'}`
  }

  spawnItem()
}

function win() {
  if (timer) { clearInterval(timer); timer = null }
  running.value = false
  finished.value = true
  passed.value = true
  currentItem.value = null
  message.value = `${props.petName} 把房间整理得干干净净！得分 ${score.value}`
  emit('complete', { passed: true, score: score.value, task: { title: '猫窝整理' } })
}

function lose() {
  if (timer) { clearInterval(timer); timer = null }
  running.value = false
  finished.value = true
  passed.value = false
  currentItem.value = null
  const diff = GOAL - score.value
  message.value = `还差 ${diff} 个就整理完，可以再试一次。`
  emit('complete', { passed: false, score: score.value, task: { title: '猫窝整理' } })
}

function startGame() {
  started.value = true
  if (timer) { clearInterval(timer); timer = null }
  score.value = 0
  timeLeft.value = 35
  running.value = true
  finished.value = false
  passed.value = false
  message.value = '把物品拖到对应区域，或直接点击区域'
  spawnItem()

  timer = setInterval(() => {
    timeLeft.value = Math.max(0, timeLeft.value - 1)
    if (timeLeft.value <= 0) lose()
  }, 1000)
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="tidy-game">
    <div v-if="!started" class="tidy-intro">
      <div class="intro-icon">🧹</div>
      <h3>猫窝整理</h3>
      <p>把当前物品拖到对应位置，也可以直接点击正确区域完成整理。</p>
      <div class="intro-rules">
        <span>目标：35 秒内整理 {{ GOAL }} 个</span>
        <span>🐟 到食碗，🎮 到玩具箱，📦 到收纳架</span>
        <span>放错会扣 2 秒，失败会显示还差几个</span>
      </div>
      <button class="ctrl-primary" @click="startGame">开始游戏</button>
      <div class="tidy-footer">
        <button class="back-btn" @click="emit('back')">← 返回游戏盒</button>
      </div>
    </div>

    <template v-else>
    <div class="tidy-hud">
      <div class="hud-item">
        <span class="hud-label">已整理</span>
        <span class="hud-value">{{ score }}/{{ GOAL }}</span>
      </div>
      <div class="hud-item">
        <span class="hud-label">时间</span>
        <span class="hud-value" :class="{ warn: timeLeft < 8 }">{{ timeLeft }}s</span>
      </div>
    </div>

    <!-- Current item -->
    <div class="tidy-current" v-if="currentItem">
      <p>请把这件物品放到正确位置：</p>
      <div class="current-item" draggable="true" @dragstart="onDragStart(currentItem, $event)">
        <span class="item-emoji">{{ currentItem.emoji }}</span>
        <span class="item-label">{{ currentItem.label }}</span>
      </div>
    </div>
    <div class="tidy-current" v-else-if="!finished">
      <p>准备中...</p>
    </div>

    <!-- Drop zones -->
    <div class="tidy-zones">
      <div
        v-for="zone in zones"
        :key="zone.id"
        class="drop-zone"
        @dragover="onDragOver"
        @drop="onDrop(zone)"
        @click="clickZone(zone)"
      >
        <span class="zone-emoji">{{ zone.emoji }}</span>
        <span class="zone-label">{{ zone.label }}</span>
      </div>
    </div>

    <p class="tidy-msg" :class="{ success: passed, fail: finished && !passed }">{{ message }}</p>

    <div class="tidy-ctrl">
      <button class="ctrl-primary" @click="finished ? startGame() : null">
        {{ finished ? '再来一局' : '进行中...' }}
      </button>
    </div>

    <div class="tidy-footer">
      <button class="back-btn" @click="emit('back')">← 返回游戏盒</button>
    </div>
    </template>
  </div>
</template>

<style scoped>
.tidy-game {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  align-items: center;
}
.tidy-intro {
  width: 100%;
  max-width: 420px;
  min-height: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.8rem;
  text-align: center;
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-md);
  background: rgba(255, 250, 241, 0.6);
  padding: 1.5rem;
}
.intro-icon { font-size: 3rem; }
.tidy-intro h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.3rem;
  color: var(--text-primary);
}
.tidy-intro p {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.82rem;
  line-height: 1.6;
}
.intro-rules {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: var(--text-secondary);
  font-size: 0.76rem;
}
.tidy-hud {
  display: flex;
  gap: 8px;
  width: 100%;
  max-width: 420px;
}
.hud-item {
  flex: 1;
  text-align: center;
  padding: 4px 0;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  background: rgba(255, 250, 241, 0.6);
}
.hud-label {
  display: block;
  font-size: 0.6rem;
  color: var(--text-muted);
  line-height: 1;
}
.hud-value {
  display: block;
  font-family: var(--font-display);
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--accent);
  line-height: 1.3;
}
.hud-value.warn { color: var(--spot); }

.tidy-current {
  text-align: center;
  padding: 1rem;
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-md);
  background: rgba(255, 250, 241, 0.5);
  width: 100%;
  max-width: 420px;
}
.tidy-current p {
  margin: 0 0 0.6rem;
  font-size: 0.78rem;
  color: var(--text-muted);
}
.current-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 2px solid var(--accent);
  border-radius: var(--radius-md);
  background: var(--accent-surface);
  cursor: grab;
  transition: transform var(--duration-fast);
}
.current-item:hover { transform: scale(1.05); }
.item-emoji { font-size: 2rem; }
.item-label {
  font-family: var(--font-body);
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-primary);
}

.tidy-zones {
  display: flex;
  gap: 12px;
  width: 100%;
  max-width: 420px;
}
.drop-zone {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 1.2rem 0.5rem;
  border: 2px dashed var(--border-default);
  border-radius: var(--radius-md);
  background: var(--bg-warm);
  cursor: pointer;
  transition: all var(--duration-fast);
}
.drop-zone:hover {
  border-color: var(--accent);
  background: var(--accent-surface);
}
.zone-emoji { font-size: 2rem; }
.zone-label {
  font-family: var(--font-body);
  font-size: 0.78rem;
  color: var(--text-secondary);
}

.tidy-msg {
  min-height: 20px;
  margin: 0;
  font-size: 0.78rem;
  color: var(--text-muted);
  text-align: center;
}
.tidy-msg.success { color: var(--accent); font-weight: 600; }
.tidy-msg.fail { color: var(--spot); }

.tidy-ctrl { display: flex; gap: 0.8rem; }
.ctrl-primary {
  padding: 10px 24px;
  border: 1px solid var(--accent);
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: #fff;
  font-family: var(--font-body);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
}
.tidy-footer { width: 100%; max-width: 420px; }
.back-btn {
  background: none;
  border: 1px solid var(--border-default);
  padding: 5px 14px;
  border-radius: var(--radius-sm);
  font-family: var(--font-body);
  font-size: 0.78rem;
  color: var(--text-secondary);
  cursor: pointer;
}
.back-btn:hover { border-color: var(--accent); color: var(--accent); }
</style>
