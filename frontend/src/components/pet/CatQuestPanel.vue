<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import ThemedIcon from '../common/ThemedIcon.vue'

const props = defineProps({
  petName: { type: String, default: '猫咪' },
  task: { type: Object, default: null },
  large: { type: Boolean, default: false },
  mode: { type: String, default: 'catch' }
})

const emit = defineEmits(['complete', 'tick'])

const running = ref(false)
const finished = ref(false)
const lane = ref(1)
const timeLeft = ref(30)
const score = ref(0)
const combo = ref(0)
const message = ref('')
const item = ref(null)
const itemTop = ref(0)
const lastResult = ref('')

let timer = null
let serial = 0

const goal = computed(() => Number(props.task?.goal || 60))
const passed = computed(() => score.value >= goal.value)
const timeProgress = computed(() => Math.max(0, (timeLeft.value / 35) * 100))
const modeConfigs = {
  catch: {
    title: '小鱼干接力',
    rules: [
      { kind: 'fish', label: '小鱼干 +10', good: true },
      { kind: 'toy', label: '玩具 +14', good: true },
      { kind: 'box', label: '纸箱 -8', good: false }
    ],
    pool: ['fish', 'fish', 'toy', 'box'],
    tip: '接住小鱼干和玩具，避开纸箱。'
  },
  dodge: {
    title: '纸箱闪避',
    rules: [
      { kind: 'box', label: '躲开 +8', good: true },
      { kind: 'fish', label: '小鱼干 +6', good: true },
      { kind: 'box', label: '撞到 -12', good: false }
    ],
    pool: ['box', 'box', 'box', 'fish', 'toy'],
    tip: '重点躲纸箱，能接道具就顺手接。'
  },
  combo: {
    title: '玩具连击',
    rules: [
      { kind: 'toy', label: '玩具 +16', good: true },
      { kind: 'fish', label: '小鱼干 +8', good: true },
      { kind: 'box', label: '纸箱断连击', good: false }
    ],
    pool: ['toy', 'toy', 'toy', 'fish', 'box'],
    tip: '连续接玩具会更容易达标。'
  },
  feast: {
    title: '晚餐冲刺',
    rules: [
      { kind: 'fish', label: '小鱼干 +14', good: true },
      { kind: 'toy', label: '玩具 +6', good: true },
      { kind: 'box', label: '纸箱 -8', good: false }
    ],
    pool: ['fish', 'fish', 'fish', 'fish', 'toy', 'box'],
    tip: '食物更多，适合练习快速接取。'
  },
  tidy: {
    title: '整理猫窝',
    rules: [
      { kind: 'box', label: '收纳纸箱 +12', good: true },
      { kind: 'toy', label: '玩具 +8', good: true },
      { kind: 'fish', label: '小鱼干跳过', good: false }
    ],
    pool: ['box', 'box', 'toy', 'toy', 'fish'],
    tip: '这次纸箱是要收纳的目标，接住纸箱加分。'
  },
  patrol: {
    title: '夜间巡逻',
    rules: [
      { kind: 'toy', label: '巡逻点 +12', good: true },
      { kind: 'fish', label: '补给 +8', good: true },
      { kind: 'box', label: '障碍 -10', good: false }
    ],
    pool: ['toy', 'toy', 'fish', 'box', 'box'],
    tip: '障碍更密集，保持移动，巡逻到更多玩具点。'
  }
}
const config = computed(() => modeConfigs[props.mode] || modeConfigs.catch)

function randomItem() {
  const kinds = config.value.pool
  const kind = kinds[Math.floor(Math.random() * kinds.length)]
  return { id: serial++, kind, lane: Math.floor(Math.random() * 3) }
}

const itemLabel = { fish: '小鱼干', toy: '玩具球', box: '纸箱' }
const itemIcon = { fish: 'bowl', toy: 'collar', box: 'folder' }
const itemEmoji = { fish: '🐟', toy: '🎮', box: '📦' }

watch(() => props.mode, () => {
  stop()
  finished.value = false
  lane.value = 1
  timeLeft.value = 30
  score.value = 0
  combo.value = 0
  item.value = null
  itemTop.value = 0
  lastResult.value = ''
  message.value = ''
})

let stepCount = 0

function start() {
  if (running.value) return
  running.value = true
  finished.value = false
  lane.value = 1
  timeLeft.value = 35
  score.value = 0
  combo.value = 0
  itemTop.value = 0
  lastResult.value = ''
  message.value = ''
  stepCount = 0
  item.value = randomItem()
  window.addEventListener('keydown', onKeydown)
  timer = window.setInterval(step, 120)
}

function stop() {
  running.value = false
  window.removeEventListener('keydown', onKeydown)
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
}

function finish() {
  stop()
  finished.value = true
  if (passed.value) {
    lastResult.value = 'pass'
    message.value = `${props.petName} 完成训练！得分 ${score.value}`
  } else {
    lastResult.value = 'fail'
    message.value = `还差 ${Math.max(0, goal.value - score.value)} 分，再试一次！`
  }
  emit('complete', {
    passed: passed.value,
    score: score.value,
    combo: combo.value,
    task: props.task
  })
}

function step() {
  if (!running.value) return
  stepCount++
  // Gradual speed: start at 5, increase by 0.05 per tick, cap at 7.5
  const dropSpeed = Math.min(5 + stepCount * 0.025, 7.5)
  itemTop.value += dropSpeed
  if (itemTop.value >= 86) {
    resolveItem()
    item.value = randomItem()
    itemTop.value = 0
  }
  timeLeft.value = Math.max(0, timeLeft.value - 0.12)
  if (timeLeft.value <= 0) finish()
}

function resolveItem() {
  if (!item.value) return
  const caught = item.value.lane === lane.value
  if (caught && props.mode === 'tidy' && item.value.kind === 'box') {
    combo.value += 1
    const bonus = 12 + Math.min(combo.value, 3) * 2
    score.value += bonus
    message.value = combo.value > 1 ? `收纳纸箱 +${bonus} (${combo.value}连击)` : '收纳纸箱 +12'
    emit('tick', 'toy')
    return
  }
  if (caught && item.value.kind === 'box') {
    combo.value = 0
    const penalty = props.mode === 'dodge' ? 12 : props.mode === 'patrol' ? 10 : 8
    score.value = Math.max(0, score.value - penalty)
    message.value = props.mode === 'combo' ? '碰到纸箱，连击断了' : `碰到纸箱 -${penalty}`
    return
  }
  if (caught && item.value.kind === 'fish') {
    if (props.mode === 'tidy') {
      combo.value = 0
      message.value = '这局先整理猫窝，小鱼干留到饭点。'
      return
    }
    combo.value += 1
    const base = props.mode === 'feast' ? 14 : props.mode === 'dodge' ? 6 : props.mode === 'combo' ? 8 : 10
    const bonus = base + Math.min(combo.value, 3) * 2
    score.value += bonus
    message.value = combo.value > 1 ? `接住小鱼干 +${bonus} (${combo.value}连击)` : `接住小鱼干 +${base}`
    emit('tick', 'fish')
    return
  }
  if (caught && item.value.kind === 'toy') {
    combo.value += 1
    const base = props.mode === 'combo' ? 16 : props.mode === 'tidy' ? 8 : props.mode === 'patrol' ? 12 : props.mode === 'feast' ? 6 : 14
    const bonus = base + Math.min(combo.value, 4) * 2
    score.value += bonus
    message.value = combo.value > 1 ? `接到玩具 +${bonus} (${combo.value}连击)` : `接到玩具 +${base}`
    emit('tick', 'toy')
    return
  }
  if (!caught && item.value.kind === 'box') {
    if (props.mode === 'dodge') {
      combo.value += 1
      const bonus = 8 + Math.min(combo.value, 3)
      score.value += bonus
      message.value = `成功闪避 +${bonus}`
    } else {
      message.value = '成功躲开纸箱'
    }
    return
  }
  combo.value = 0
  message.value = ''
}

function moveTo(nextLane) {
  lane.value = Math.max(0, Math.min(2, nextLane))
}

function onKeydown(e) {
  if (!running.value) return
  if (e.key === 'ArrowLeft' || e.key.toLowerCase() === 'a') moveTo(lane.value - 1)
  if (e.key === 'ArrowRight' || e.key.toLowerCase() === 'd') moveTo(lane.value + 1)
}

onBeforeUnmount(stop)
</script>

<template>
  <div class="quest-panel" :class="{ large }">
    <!-- HUD: score / goal / time -->
    <div class="quest-hud">
      <div class="hud-item">
        <span class="hud-label">分数</span>
        <span class="hud-value" :class="{ hit: lastResult === 'fail' }">{{ score }}</span>
      </div>
      <div class="hud-item">
        <span class="hud-label">目标</span>
        <span class="hud-value goal">{{ goal }}</span>
      </div>
      <div class="hud-item">
        <span class="hud-label">时间</span>
        <span class="hud-value" :class="{ warn: timeLeft < 8 && running }">{{ Math.ceil(timeLeft) }}s</span>
      </div>
    </div>

    <!-- Time bar -->
    <div class="time-track">
      <span class="time-fill" :style="{ width: timeProgress + '%' }"></span>
    </div>

    <!-- Rules -->
    <div class="quest-rules">
      <span
        v-for="rule in config.rules"
        :key="rule.kind + rule.label"
        :class="rule.good ? 'rule-good' : 'rule-bad'"
      >
        <span class="rule-emoji">{{ itemEmoji[rule.kind] }}</span> {{ rule.label }}
      </span>
    </div>
    <p class="mode-tip" v-if="large">{{ config.tip }}</p>

    <!-- Game stage -->
    <div class="stage" :class="{ running }">
      <!-- Lane lines -->
      <div class="lanes">
        <div class="lane" v-for="n in 3" :key="n"></div>
      </div>

      <!-- Falling item -->
      <div
        v-if="item"
        class="fall-item"
        :class="item.kind"
        :style="{ left: `${16 + item.lane * 34}%`, top: `${itemTop}%` }"
      >
        <span class="fall-emoji">{{ itemEmoji[item.kind] }}</span>
      </div>

      <!-- Cat runner -->
      <div class="cat" :style="{ left: `${16 + lane * 34}%` }">
        <span class="cat-emoji">🐱</span>
      </div>
    </div>

    <!-- Message -->
    <p class="quest-msg" :class="{ success: lastResult === 'pass', fail: lastResult === 'fail' }">{{ message }}</p>

    <!-- Controls -->
    <div class="quest-ctrl">
      <button class="ctrl-btn" @click="moveTo(lane - 1)" :disabled="!running">
        <span>◀</span> 左移
      </button>
      <button class="ctrl-btn primary" @click="running ? finish() : start()">
        {{ running ? '结束' : finished ? '再来一局' : '开始训练' }}
      </button>
      <button class="ctrl-btn" @click="moveTo(lane + 1)" :disabled="!running">
        右移 <span>▶</span>
      </button>
    </div>

    <p class="quest-hint">A / D 键或方向键控制</p>
  </div>
</template>

<style scoped>
.quest-panel {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* HUD */
.quest-hud {
  display: flex;
  gap: 6px;
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
  font-size: 1rem;
  font-weight: 700;
  color: var(--accent);
  line-height: 1.3;
}
.hud-value.goal { color: var(--text-secondary); }
.hud-value.warn { color: var(--spot); }
.hud-value.hit { color: var(--spot); }

/* Time track */
.time-track {
  height: 4px;
  background: rgba(110, 89, 65, 0.1);
  border-radius: 2px;
  overflow: hidden;
}
.time-fill {
  display: block;
  height: 100%;
  background: var(--spot);
  transition: width 0.15s linear;
  border-radius: 2px;
}

/* Rules */
.quest-rules {
  display: flex;
  gap: 8px;
  font-size: 0.65rem;
  color: var(--text-muted);
}
.mode-tip {
  margin: -4px 0 0;
  text-align: center;
  color: var(--text-muted);
  font-size: 0.78rem;
}
.rule-good, .rule-bad {
  display: flex;
  align-items: center;
  gap: 2px;
}
.rule-good { color: var(--accent); }
.rule-bad { color: var(--spot); }
.rule-emoji { font-size: 0.75rem; }

/* Stage */
.stage {
  position: relative;
  height: 180px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  overflow: hidden;
  background:
    linear-gradient(90deg, transparent 32%, rgba(120, 96, 70, 0.08) 32%, rgba(120, 96, 70, 0.08) 34%, transparent 34%, transparent 66%, rgba(120, 96, 70, 0.08) 66%, rgba(120, 96, 70, 0.08) 68%, transparent 68%),
    linear-gradient(180deg, #fffaf1 0%, #f0e6d2 100%);
}

.quest-panel.large {
  gap: 12px;
}
.quest-panel.large .quest-hud {
  gap: 12px;
}
.quest-panel.large .hud-item {
  padding: 12px 0;
}
.quest-panel.large .hud-label {
  font-size: 0.78rem;
}
.quest-panel.large .hud-value {
  font-size: 1.8rem;
}
.quest-panel.large .time-track {
  height: 8px;
}
.quest-panel.large .quest-rules {
  justify-content: center;
  gap: 24px;
  font-size: 0.86rem;
}
.quest-panel.large .rule-emoji {
  font-size: 1rem;
}
.quest-panel.large .stage {
  height: min(54vh, 520px);
  min-height: 360px;
}
.quest-panel.large .fall-item {
  width: 58px;
  height: 58px;
}
.quest-panel.large .fall-emoji {
  font-size: 1.8rem;
}
.quest-panel.large .cat {
  width: 72px;
  height: 58px;
  bottom: 18px;
}
.quest-panel.large .cat-emoji {
  font-size: 2rem;
}
.quest-panel.large .stage.running .cat {
  animation-name: largeBounce;
}
@keyframes largeBounce {
  from { bottom: 18px; }
  to { bottom: 28px; }
}
.quest-panel.large .quest-msg {
  min-height: 26px;
  font-size: 0.92rem;
}
.quest-panel.large .quest-ctrl {
  gap: 12px;
}
.quest-panel.large .ctrl-btn {
  padding: 14px 10px;
  font-size: 0.92rem;
}
.quest-panel.large .quest-hint {
  font-size: 0.78rem;
}
.lanes {
  position: absolute;
  inset: 0;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  pointer-events: none;
}
.lane {
  border-right: 1px dashed rgba(120, 96, 70, 0.12);
}
.lane:last-child {
  border-right: none;
}

/* Falling item */
.fall-item {
  position: absolute;
  width: 36px;
  height: 36px;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #fffdf8;
  border: 1.5px solid var(--border-default);
  box-shadow: 0 4px 10px rgba(90, 68, 43, 0.1);
  transition: top 0.1s linear;
  z-index: 3;
}
.fall-emoji {
  font-size: 1.1rem;
  line-height: 1;
}
.fall-item.box {
  border-color: var(--spot);
  background: #fff5f0;
}

/* Cat */
.cat {
  position: absolute;
  bottom: 8px;
  transform: translateX(-50%);
  width: 44px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50% 50% 40% 40%;
  background: #f5d99a;
  border: 1.5px solid #d4b87a;
  box-shadow: 0 4px 12px rgba(90, 68, 43, 0.15);
  transition: left 0.12s ease;
  z-index: 4;
}
.cat-emoji {
  font-size: 1.15rem;
  line-height: 1;
}
.stage.running .cat {
  animation: bounce 0.4s ease-in-out infinite alternate;
}
@keyframes bounce {
  from { bottom: 8px; }
  to { bottom: 14px; }
}

/* Message */
.quest-msg {
  min-height: 18px;
  margin: 0;
  font-size: 0.7rem;
  color: var(--text-muted);
  text-align: center;
  line-height: 1.4;
}
.quest-msg.success { color: var(--accent); font-weight: 600; }
.quest-msg.fail { color: var(--spot); }

/* Controls */
.quest-ctrl {
  display: grid;
  grid-template-columns: 1fr 1.2fr 1fr;
  gap: 6px;
}
.ctrl-btn {
  padding: 7px 4px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-family: var(--font-body);
  font-size: 0.73rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  transition: all var(--duration-fast);
}
.ctrl-btn:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}
.ctrl-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.ctrl-btn.primary {
  border-color: var(--accent);
  background: var(--accent);
  color: #fff;
  font-weight: 600;
}
.ctrl-btn.primary:hover {
  background: var(--accent-light);
}

/* Hint */
.quest-hint {
  margin: 0;
  font-size: 0.6rem;
  color: var(--text-muted);
  text-align: center;
  opacity: 0.7;
}
</style>
