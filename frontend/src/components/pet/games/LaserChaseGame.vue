<script setup>
import { ref, onBeforeUnmount } from 'vue'

const props = defineProps({
  petName: { type: String, default: '猫咪' },
  pet: { type: Object, default: null }
})
const emit = defineEmits(['complete', 'back'])

const canvasRef = ref(null)
const score = ref(0)
const timeLeft = ref(35)
const started = ref(false)
const running = ref(false)
const finished = ref(false)
const passed = ref(false)
const message = ref('')
const combo = ref(0)

let timer = null
let animFrame = null
const W = 400
const H = 320

let cat = { x: 200, y: 240, targetX: 200, targetY: 240, size: 28 }
let dot = { x: 100, y: 100, size: 16, visible: true }
let particles = []
let dotTimer = 0
let gameTime = 0
const GOAL = 12

function spawnDot() {
  dot.x = 50 + Math.random() * (W - 100)
  dot.y = 50 + Math.random() * (H - 130)
  dot.visible = true
  dotTimer = 0
}

function draw() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  canvas.width = W
  canvas.height = H

  // Background
  ctx.fillStyle = '#f5efe0'
  ctx.fillRect(0, 0, W, H)

  // Grid pattern
  ctx.strokeStyle = '#e8e0d0'
  ctx.lineWidth = 0.5
  for (let i = 0; i < W; i += 40) {
    ctx.beginPath(); ctx.moveTo(i, 0); ctx.lineTo(i, H); ctx.stroke()
  }
  for (let i = 0; i < H; i += 40) {
    ctx.beginPath(); ctx.moveTo(0, i); ctx.lineTo(W, i); ctx.stroke()
  }

  // Dot
  if (dot.visible) {
    // Outer glow
    ctx.beginPath()
    ctx.arc(dot.x, dot.y, dot.size + 6, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(255, 34, 34, 0.15)'
    ctx.fill()
    // Main dot
    ctx.beginPath()
    ctx.arc(dot.x, dot.y, dot.size, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(255, 34, 34, 0.85)'
    ctx.fill()
  }

  // Particles
  for (const p of particles) {
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.size * p.life, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(92, 122, 94, ${p.life * 0.6})`
    ctx.fill()
  }

  // Cat
  ctx.font = `${cat.size}px serif`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('🐱', cat.x, cat.y)
}

function update() {
  gameTime++

  // Cat speed: starts at 3.5, increases with combo
  const baseSpeed = 3.5
  const comboSpeed = Math.min(combo.value * 0.25, 2)
  const speed = baseSpeed + comboSpeed

  const dx = cat.targetX - cat.x
  const dy = cat.targetY - cat.y
  const dist = Math.sqrt(dx * dx + dy * dy)
  if (dist > 2) {
    cat.x += (dx / dist) * Math.min(speed, dist)
    cat.y += (dy / dist) * Math.min(speed, dist)
  }

  // Catch check — generous radius (36px total)
  if (dot.visible) {
    const ddx = cat.x - dot.x
    const ddy = cat.y - dot.y
    const catchDist = cat.size / 2 + dot.size + 8 // 8px padding
    if (Math.sqrt(ddx * ddx + ddy * ddy) < catchDist) {
      dot.visible = false
      score.value++
      combo.value++
      message.value = combo.value > 1 ? `命中！${combo.value} 连击` : '命中！'
      for (let i = 0; i < 5; i++) {
        particles.push({
          x: dot.x, y: dot.y,
          vx: (Math.random() - 0.5) * 3,
          vy: (Math.random() - 0.5) * 3,
          size: 3 + Math.random() * 3,
          life: 1
        })
      }
      if (score.value >= GOAL) {
        win()
        return
      }
      // Respawn delay: generous, decreases slightly with combo
      const delay = Math.max(300, 600 - combo.value * 30)
      setTimeout(spawnDot, delay)
    }
  }

  // Auto respawn: dot stays for ~4 seconds (240 frames at 60fps)
  if (dot.visible) {
    dotTimer++
    if (dotTimer > 240) {
      dot.visible = false
      combo.value = 0
      message.value = '光点消失了，重新引导'
      setTimeout(spawnDot, 500)
    }
  }

  // Particles
  particles = particles.filter(p => {
    p.x += p.vx
    p.y += p.vy
    p.life -= 0.025
    return p.life > 0
  })
}

function gameLoop() {
  if (!running.value) return
  update()
  draw()
  animFrame = requestAnimationFrame(gameLoop)
}

function onCanvasClick(e) {
  if (!running.value) return
  const rect = canvasRef.value.getBoundingClientRect()
  const scaleX = W / rect.width
  const scaleY = H / rect.height
  cat.targetX = (e.clientX - rect.left) * scaleX
  cat.targetY = (e.clientY - rect.top) * scaleY
}

function win() {
  running.value = false
  finished.value = true
  passed.value = true
  if (timer) { clearInterval(timer); timer = null }
  message.value = `${props.petName} 追到了所有光点！命中 ${score.value} 次`
  draw()
  emit('complete', { passed: true, score: score.value, task: { title: '逗猫棒追逐' } })
}

function lose() {
  running.value = false
  finished.value = true
  passed.value = false
  if (timer) { clearInterval(timer); timer = null }
  const diff = GOAL - score.value
  message.value = `还差 ${diff} 次就完成，可以再试一次。`
  draw()
  emit('complete', { passed: false, score: score.value, task: { title: '逗猫棒追逐' } })
}

function startGame() {
  started.value = true
  if (timer) { clearInterval(timer); timer = null }
  if (animFrame) { cancelAnimationFrame(animFrame); animFrame = null }
  cat = { x: 200, y: 240, targetX: 200, targetY: 240, size: 28 }
  score.value = 0
  timeLeft.value = 35
  combo.value = 0
  gameTime = 0
  running.value = true
  finished.value = false
  passed.value = false
  particles = []
  message.value = '点击画布引导光点，让猫咪追上它'
  spawnDot()
  draw()
  gameLoop()

  timer = setInterval(() => {
    timeLeft.value = Math.max(0, timeLeft.value - 1)
    if (timeLeft.value <= 0) lose()
  }, 1000)
}

onBeforeUnmount(() => {
  running.value = false
  if (timer) { clearInterval(timer); timer = null }
  if (animFrame) cancelAnimationFrame(animFrame)
})
</script>

<template>
  <div class="laser-game">
    <div v-if="!started" class="laser-intro">
      <div class="intro-icon">🔴</div>
      <h3>逗猫棒追逐</h3>
      <p>点击画布移动引导点，让猫咪追上红色光点。连续命中会加快猫咪速度。</p>
      <div class="intro-rules">
        <span>目标：35 秒内命中 {{ GOAL }} 次</span>
        <span>光点停留约 4 秒，消失后会刷新</span>
        <span>失败会显示还差几次</span>
      </div>
      <button class="ctrl-primary" @click="startGame">开始游戏</button>
      <div class="laser-footer">
        <button class="back-btn" @click="emit('back')">← 返回游戏盒</button>
      </div>
    </div>

    <template v-else>
    <div class="laser-hud">
      <div class="hud-item">
        <span class="hud-label">命中</span>
        <span class="hud-value">{{ score }}/{{ GOAL }}</span>
      </div>
      <div class="hud-item">
        <span class="hud-label">时间</span>
        <span class="hud-value" :class="{ warn: timeLeft < 8 }">{{ timeLeft }}s</span>
      </div>
      <div class="hud-item">
        <span class="hud-label">连击</span>
        <span class="hud-value">{{ combo }}</span>
      </div>
    </div>

    <div class="laser-stage" @click="onCanvasClick">
      <canvas ref="canvasRef"></canvas>
    </div>

    <p class="laser-msg" :class="{ success: passed, fail: finished && !passed }">{{ message }}</p>

    <div class="laser-ctrl">
      <button class="ctrl-primary" @click="finished ? startGame() : null">
        {{ finished ? '再来一局' : '进行中...' }}
      </button>
    </div>

    <p class="laser-hint">点击画布任意位置引导光点</p>

    <div class="laser-footer">
      <button class="back-btn" @click="emit('back')">← 返回游戏盒</button>
    </div>
    </template>
  </div>
</template>

<style scoped>
.laser-game {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  align-items: center;
}
.laser-intro {
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
.laser-intro h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.3rem;
  color: var(--text-primary);
}
.laser-intro p {
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
.laser-hud {
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
.laser-stage {
  border: 2px solid var(--border-default);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: crosshair;
  line-height: 0;
  max-width: 100%;
}
.laser-stage canvas {
  display: block;
  max-width: 100%;
  height: auto;
}
.laser-msg {
  min-height: 20px;
  margin: 0;
  font-size: 0.78rem;
  color: var(--text-muted);
  text-align: center;
}
.laser-msg.success { color: var(--accent); font-weight: 600; }
.laser-msg.fail { color: var(--spot); }
.laser-ctrl {
  display: flex;
  gap: 0.8rem;
}
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
.laser-hint {
  margin: 0;
  font-size: 0.65rem;
  color: var(--text-muted);
  opacity: 0.7;
}
.laser-footer {
  width: 100%;
  max-width: 420px;
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
