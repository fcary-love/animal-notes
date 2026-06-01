<script setup>
import { ref, onBeforeUnmount } from 'vue'

const props = defineProps({
  petName: { type: String, default: '猫咪' },
  pet: { type: Object, default: null }
})
const emit = defineEmits(['complete', 'back'])

const score = ref(0)
const round = ref(0)
const totalRounds = 3
const timeLeft = ref(60)
const started = ref(false)
const running = ref(false)
const finished = ref(false)
const passed = ref(false)
const message = ref('')
const selected = ref([])
const currentState = ref(null)
const showResult = ref(false)
const roundScore = ref(0)
let timer = null

const statusOptions = [
  { label: '活力低', key: 'energy', low: true },
  { label: '饱腹低', key: 'hunger', low: true },
  { label: '心情低', key: 'mood', low: true },
  { label: '活力高', key: 'energy', low: false },
  { label: '饱腹高', key: 'hunger', low: false },
  { label: '需要补水', key: 'water', low: true }
]

const foods = [
  { id: 'fish', emoji: '🐟', name: '小鱼干', effects: { hunger: 3, energy: 1, mood: 1, water: 0 } },
  { id: 'kibble', emoji: '🥣', name: '猫粮', effects: { hunger: 2, energy: 1, mood: 0, water: 0 } },
  { id: 'water', emoji: '💧', name: '水碗', effects: { hunger: 0, energy: 0, mood: 0, water: 3 } },
  { id: 'treat', emoji: '🍬', name: '零食', effects: { hunger: 1, energy: 0, mood: 3, water: 0 } },
  { id: 'toy', emoji: '🎮', name: '玩具', effects: { hunger: 0, energy: 2, mood: 2, water: 0 } },
  { id: 'vitamin', emoji: '💊', name: '营养膏', effects: { hunger: 1, energy: 2, mood: 1, water: 1 } },
  { id: 'wet', emoji: '🥫', name: '湿粮', effects: { hunger: 3, energy: 0, mood: 1, water: 2 } },
  { id: 'milk', emoji: '🥛', name: '羊奶', effects: { hunger: 1, energy: 1, mood: 1, water: 2 } }
]

function randomState() {
  const count = 2 + Math.floor(Math.random() * 2)
  const shuffled = [...statusOptions].sort(() => Math.random() - 0.5)
  return shuffled.slice(0, count)
}

function calculateMatch() {
  if (!currentState.value || selected.value.length === 0) return 0
  let matchPoints = 0
  let totalNeeded = 0

  for (const status of currentState.value) {
    const need = status.low ? 2 : 1
    totalNeeded += need

    for (const foodId of selected.value) {
      const food = foods.find(f => f.id === foodId)
      if (!food) continue
      const effect = food.effects[status.key] || 0
      if (status.low && effect >= 2) matchPoints += need
      else if (!status.low && effect >= 1) matchPoints += need
      else matchPoints += effect * 0.5
    }
  }

  return Math.min(100, Math.round((matchPoints / Math.max(totalNeeded, 1)) * 100))
}

function toggleFood(foodId) {
  if (!running.value || showResult.value) return
  const idx = selected.value.indexOf(foodId)
  if (idx >= 0) {
    selected.value.splice(idx, 1)
  } else if (selected.value.length < 3) {
    selected.value.push(foodId)
  }
}

function submit() {
  if (!running.value || selected.value.length === 0) return
  const match = calculateMatch()
  roundScore.value = match
  score.value += match
  showResult.value = true

  if (match >= 80) {
    message.value = `匹配度 ${match}%！搭配得很好`
  } else if (match >= 50) {
    message.value = `匹配度 ${match}%，还可以更精准`
  } else {
    message.value = `匹配度 ${match}%，不太合适哦`
  }
}

function nextRound() {
  if (!running.value) return
  round.value++
  if (round.value >= totalRounds) {
    finishGame()
    return
  }
  selected.value = []
  showResult.value = false
  roundScore.value = 0
  currentState.value = randomState()
  message.value = ''
}

function clearTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function finishGame(forceFail = false) {
  clearTimer()
  running.value = false
  finished.value = true
  const completedRounds = forceFail ? Math.max(round.value + (showResult.value ? 1 : 0), 0) : totalRounds
  const avg = completedRounds > 0 ? Math.round(score.value / completedRounds) : 0
  passed.value = !forceFail && avg >= 60
  if (passed.value) {
    message.value = `${props.petName} 的配餐平均匹配度 ${avg}%，很棒！`
  } else if (forceFail) {
    const diff = Math.max(0, totalRounds - completedRounds)
    message.value = `时间到！还差 ${diff} 轮完成配餐，可以再试一次。`
  } else {
    message.value = `平均匹配度 ${avg}%，距离 60% 目标还差 ${Math.max(0, 60 - avg)}%。`
  }
  emit('complete', { passed: passed.value, score: avg, task: { title: '晚餐配餐' } })
}

function startGame() {
  started.value = true
  clearTimer()
  score.value = 0
  round.value = 0
  timeLeft.value = 60
  running.value = true
  finished.value = false
  passed.value = false
  selected.value = []
  showResult.value = false
  roundScore.value = 0
  currentState.value = randomState()
  message.value = '根据猫咪状态，选择 3 种食物搭配一餐'
  timer = setInterval(() => {
    timeLeft.value = Math.max(0, timeLeft.value - 1)
    if (timeLeft.value <= 0) finishGame(true)
  }, 1000)
}

onBeforeUnmount(clearTimer)
</script>

<template>
  <div class="dinner-game">
    <div v-if="!started" class="dinner-intro">
      <div class="intro-icon">🍽️</div>
      <h3>晚餐配餐</h3>
      <p>根据猫咪当前状态选择最多 3 种食物，完成 3 轮配餐后按平均匹配度结算。</p>
      <div class="intro-rules">
        <span>目标：60 秒内完成 3 轮</span>
        <span>平均匹配度达到 60% 即通过</span>
        <span>结果只用于当前游戏反馈</span>
      </div>
      <button class="ctrl-primary" @click="startGame">开始游戏</button>
      <div class="dinner-footer">
        <button class="back-btn" @click="emit('back')">← 返回游戏盒</button>
      </div>
    </div>

    <template v-else>
    <div class="dinner-hud">
      <div class="hud-item">
        <span class="hud-label">轮次</span>
        <span class="hud-value">{{ round + 1 }}/{{ totalRounds }}</span>
      </div>
      <div class="hud-item">
        <span class="hud-label">总分</span>
        <span class="hud-value">{{ score }}</span>
      </div>
      <div class="hud-item">
        <span class="hud-label">时间</span>
        <span class="hud-value" :class="{ bad: timeLeft < 10 }">{{ timeLeft }}s</span>
      </div>
      <div class="hud-item" v-if="showResult">
        <span class="hud-label">本轮</span>
        <span class="hud-value" :class="{ good: roundScore >= 80, bad: roundScore < 50 }">{{ roundScore }}%</span>
      </div>
    </div>

    <!-- Status card -->
    <div class="state-card" v-if="currentState">
      <p class="state-title">{{ petName }}的当前状态</p>
      <div class="state-tags">
        <span v-for="s in currentState" :key="s.key + s.low" class="state-tag" :class="{ low: s.low }">
          {{ s.label }}
        </span>
      </div>
    </div>

    <!-- Food selection -->
    <div class="food-grid">
      <button
        v-for="food in foods"
        :key="food.id"
        class="food-btn"
        :class="{ selected: selected.includes(food.id), disabled: !selected.includes(food.id) && selected.length >= 3 }"
        @click="toggleFood(food.id)"
      >
        <span class="food-emoji">{{ food.emoji }}</span>
        <span class="food-name">{{ food.name }}</span>
      </button>
    </div>

    <p class="dinner-hint">已选 {{ selected.length }}/3 种食物</p>

    <!-- Submit / Next -->
    <div class="dinner-ctrl">
      <button v-if="!showResult" class="ctrl-primary" :disabled="selected.length === 0" @click="submit">
        提交配餐
      </button>
      <button v-else class="ctrl-primary" @click="nextRound">
        {{ round + 1 >= totalRounds ? '查看结果' : '下一轮' }}
      </button>
    </div>

    <p class="dinner-msg" :class="{ success: passed && finished, fail: finished && !passed }">{{ message }}</p>

    <div class="dinner-footer" v-if="finished">
      <button class="ctrl-primary" @click="startGame">再来一轮</button>
      <button class="back-btn" @click="emit('back')">← 返回游戏盒</button>
    </div>
    <div class="dinner-footer" v-else>
      <button class="back-btn" @click="emit('back')">← 返回游戏盒</button>
    </div>
    </template>
  </div>
</template>

<style scoped>
.dinner-game {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  align-items: center;
}
.dinner-intro {
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
.dinner-intro h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.3rem;
  color: var(--text-primary);
}
.dinner-intro p {
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
.dinner-hud {
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
.hud-value.good { color: var(--accent); }
.hud-value.bad { color: var(--spot); }

.state-card {
  width: 100%;
  max-width: 420px;
  padding: 1rem;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  text-align: center;
}
.state-title {
  margin: 0 0 0.6rem;
  font-family: var(--font-display);
  font-size: 0.95rem;
  color: var(--text-primary);
}
.state-tags {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}
.state-tag {
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 0.78rem;
  font-family: var(--font-body);
  border: 1px solid var(--accent);
  color: var(--accent);
  background: var(--accent-surface);
}
.state-tag.low {
  border-color: var(--spot);
  color: var(--spot);
  background: rgba(193, 123, 96, 0.08);
}

.food-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  width: 100%;
  max-width: 420px;
}
.food-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 4px;
  border: 1.5px solid var(--border-default);
  border-radius: var(--radius-md);
  background: transparent;
  cursor: pointer;
  transition: all var(--duration-fast);
  font-family: var(--font-body);
}
.food-btn:hover:not(.disabled) {
  border-color: var(--accent);
  background: var(--accent-surface);
}
.food-btn.selected {
  border-color: var(--accent);
  background: var(--accent-surface);
  box-shadow: 0 0 0 2px rgba(92, 122, 94, 0.2);
}
.food-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.food-emoji { font-size: 1.5rem; }
.food-name {
  font-size: 0.7rem;
  color: var(--text-secondary);
}

.dinner-hint {
  margin: 0;
  font-size: 0.72rem;
  color: var(--text-muted);
}
.dinner-ctrl { display: flex; gap: 0.8rem; }
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
.ctrl-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.dinner-msg {
  min-height: 20px;
  margin: 0;
  font-size: 0.78rem;
  color: var(--text-muted);
  text-align: center;
}
.dinner-msg.success { color: var(--accent); font-weight: 600; }
.dinner-msg.fail { color: var(--spot); }
.dinner-footer {
  display: flex;
  gap: 0.8rem;
  align-items: center;
}
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
