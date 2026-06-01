<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  petName: { type: String, default: '猫咪' },
  pet: { type: Object, default: null }
})
const emit = defineEmits(['complete', 'back'])

const canvasRef = ref(null)
const score = ref(0)
const hp = ref(5)
const timeLeft = ref(45)
const started = ref(false)
const running = ref(false)
const finished = ref(false)
const passed = ref(false)
const message = ref('')
const difficultyLevel = ref(1)

const MAX_HP = 5
const GAME_DURATION = 45 // seconds
const W = 480
const H = 300
const GROUND = H - 55
const CAT_X = 80

let timer = null
let animFrame = null
let scrollX = 0
let gameTime = 0
let spawnTick = 0
let invincible = 0
let lastItemX = 0
let lastObsX = 0

// Cat
let cat = { x: CAT_X, y: GROUND, vy: 0, jumping: false, w: 32, h: 32 }

// World objects
let items = []
let obstacles = []
let bgStars = []
let floatTexts = []
let particles = []

// Obstacle templates
const OBS_TEMPLATES = [
  { emoji: '📦', w: 24, h: 28, name: '纸箱' },
  { emoji: '🧹', w: 20, h: 34, name: '扫帚' },
  { emoji: '🪑', w: 26, h: 30, name: '椅子' },
  { emoji: '🧱', w: 22, h: 26, name: '砖块' },
  { emoji: '🪣', w: 22, h: 28, name: '水桶' },
]

// Item templates — 🟢 is a heal item
const ITEM_TEMPLATES = [
  { emoji: '🐟', name: '小鱼干', pts: 1, heal: false },
  { emoji: '⭐', name: '星星', pts: 1, heal: false },
  { emoji: '🎮', name: '游戏机', pts: 2, heal: false },
  { emoji: '🧶', name: '毛线球', pts: 1, heal: false },
  { emoji: '🎀', name: '蝴蝶结', pts: 2, heal: false },
  { emoji: '🌙', name: '月亮', pts: 3, heal: false },
  { emoji: '💚', name: '生命之心', pts: 0, heal: true },
]

function generateWorld() {
  items = []
  obstacles = []
  bgStars = []
  floatTexts = []
  particles = []
  spawnTick = 0
  lastItemX = 0
  lastObsX = 0

  // Initial items — spaced out
  for (let i = 0; i < 6; i++) {
    const x = 260 + i * 160
    spawnItem(x)
    lastItemX = x
  }

  // Initial obstacles
  for (let i = 0; i < 2; i++) {
    const x = 800 + i * 400
    spawnObstacle(x)
    lastObsX = x
  }

  // Background stars
  for (let i = 0; i < 40; i++) {
    bgStars.push({
      x: Math.random() * W * 4,
      y: Math.random() * (GROUND - 50),
      size: 1 + Math.random() * 2.5,
      twinkle: Math.random() * Math.PI * 2
    })
  }
}

function spawnItem(baseX) {
  const tpl = ITEM_TEMPLATES[Math.floor(Math.random() * ITEM_TEMPLATES.length)]
  const canBeHigh = Math.random() > 0.5
  items.push({
    x: baseX + Math.random() * 20,
    y: canBeHigh ? GROUND - 40 - Math.random() * 55 : GROUND - 20,
    ...tpl,
    collected: false
  })
}

function spawnObstacle(baseX) {
  const tpl = OBS_TEMPLATES[Math.floor(Math.random() * OBS_TEMPLATES.length)]
  obstacles.push({
    x: baseX + Math.random() * 30,
    ...tpl,
    hit: false
  })
}

function addFloatText(x, y, text, color = '#5c7a5e') {
  floatTexts.push({ x, y, text, life: 1, color })
}

function addHitParticles(x, y) {
  for (let i = 0; i < 8; i++) {
    const angle = Math.random() * Math.PI * 2
    const speed = 1 + Math.random() * 3
    particles.push({
      x, y,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed - 2,
      life: 1,
      color: Math.random() > 0.5 ? '#e85d5d' : '#f0a0a0'
    })
  }
}

function draw() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  canvas.width = W
  canvas.height = H

  // Night sky
  const grad = ctx.createLinearGradient(0, 0, 0, H)
  grad.addColorStop(0, '#1a1530')
  grad.addColorStop(0.5, '#2a2440')
  grad.addColorStop(0.85, '#3d3555')
  grad.addColorStop(1, '#4a3f60')
  ctx.fillStyle = grad
  ctx.fillRect(0, 0, W, H)

  // Moon
  const moonX = ((320 - scrollX * 0.05) % (W + 60) + W + 60) % (W + 60) - 30
  ctx.font = '28px serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('🌙', moonX, 35)

  // Background stars
  for (const s of bgStars) {
    const sx = ((s.x - scrollX * 0.15) % (W + 20) + W + 20) % (W + 20) - 10
    const alpha = 0.4 + Math.sin(gameTime * 0.03 + s.twinkle) * 0.3
    ctx.fillStyle = `rgba(255, 255, 200, ${alpha})`
    ctx.beginPath()
    ctx.arc(sx, s.y, s.size, 0, Math.PI * 2)
    ctx.fill()
  }

  // Ground
  ctx.fillStyle = '#3a3328'
  ctx.fillRect(0, GROUND, W, H - GROUND)
  ctx.fillStyle = '#5a4f3a'
  ctx.fillRect(0, GROUND, W, 4)

  // Obstacles — solid, clear
  for (const obs of obstacles) {
    const ox = obs.x - scrollX
    if (ox < -60 || ox > W + 60) continue
    // Shadow
    ctx.fillStyle = 'rgba(0,0,0,0.25)'
    ctx.beginPath()
    ctx.ellipse(ox + obs.w / 2, GROUND + 2, obs.w / 2 + 4, 5, 0, 0, Math.PI * 2)
    ctx.fill()
    // Body emoji — large and opaque
    ctx.font = `${Math.max(22, Math.min(obs.w, obs.h))}px serif`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'bottom'
    ctx.globalAlpha = 1
    ctx.fillText(obs.emoji, ox + obs.w / 2, GROUND)
  }

  // Items — solid, clear glow
  for (const item of items) {
    if (item.collected) continue
    const ix = item.x - scrollX
    if (ix < -30 || ix > W + 30) continue
    // Subtle glow ring (not too transparent)
    const pulse = 0.25 + Math.sin(gameTime * 0.05 + item.x) * 0.1
    ctx.fillStyle = item.heal
      ? `rgba(100, 220, 120, ${pulse})`
      : `rgba(255, 230, 150, ${pulse})`
    ctx.beginPath()
    ctx.arc(ix, item.y, 14, 0, Math.PI * 2)
    ctx.fill()
    // Emoji — fully opaque
    ctx.globalAlpha = 1
    ctx.font = '22px serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(item.emoji, ix, item.y)
  }

  // Cat — fully opaque, with invincibility flash
  const cx = cat.x - scrollX
  if (invincible > 0 && Math.floor(invincible / 4) % 2 === 0) {
    ctx.globalAlpha = 0.45
  } else {
    ctx.globalAlpha = 1
  }
  // Cat shadow
  ctx.fillStyle = 'rgba(0,0,0,0.25)'
  ctx.beginPath()
  ctx.ellipse(cx + cat.w / 2, GROUND + 2, cat.w / 2 + 3, 4, 0, 0, Math.PI * 2)
  ctx.fill()
  // Cat emoji
  ctx.font = `${cat.h}px serif`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('🐱', cx + cat.w / 2, cat.y + cat.h / 2)
  ctx.globalAlpha = 1

  // Hit particles
  for (const p of particles) {
    const px = p.x - scrollX
    const rgb = p.color === '#e85d5d' ? '232,93,93' : '240,160,160'
    ctx.fillStyle = `rgba(${rgb}, ${p.life})`
    ctx.beginPath()
    ctx.arc(px, p.y, 2 + p.life * 2, 0, Math.PI * 2)
    ctx.fill()
  }

  // Floating texts (fade out ok)
  ctx.font = 'bold 14px sans-serif'
  for (const ft of floatTexts) {
    const fx = ft.x - scrollX
    // Parse color and apply life as alpha
    if (ft.color.startsWith('#')) {
      const r = parseInt(ft.color.slice(1, 3), 16)
      const g = parseInt(ft.color.slice(3, 5), 16)
      const b = parseInt(ft.color.slice(5, 7), 16)
      ctx.fillStyle = `rgba(${r},${g},${b},${ft.life})`
    } else {
      ctx.fillStyle = ft.color.replace(')', `,${ft.life})`).replace('rgb', 'rgba')
    }
    ctx.textAlign = 'center'
    ctx.fillText(ft.text, fx, ft.y - (1 - ft.life) * 35)
  }

  // On-canvas HP bar (top-left) — removed, using HUD instead
}

function update() {
  gameTime++
  spawnTick++

  // Difficulty progression
  const elapsedSec = gameTime / 60
  const level = Math.min(10, 1 + Math.floor(elapsedSec / 5))
  if (level !== difficultyLevel.value) {
    difficultyLevel.value = level
  }

  // Scroll speed ramps up
  const baseSpeed = 1.1
  const rampSpeed = Math.min(level * 0.18, 1.8)
  const scrollSpeed = baseSpeed + rampSpeed
  scrollX += scrollSpeed

  // Spawn items — generous spacing
  const itemGap = Math.max(140, 220 - level * 8)
  if (scrollX > lastItemX + itemGap - W) {
    const newX = scrollX + W + 60 + Math.random() * 80
    if (newX - lastItemX > 120) {
      spawnItem(newX)
      lastItemX = newX
    }
  }

  // Spawn obstacles — start later, wide spacing
  if (elapsedSec > 5) {
    const obsGap = Math.max(250, 450 - level * 20)
    if (scrollX > lastObsX + obsGap - W) {
      const newX = scrollX + W + 80 + Math.random() * 100
      if (newX - lastObsX > 220) {
        spawnObstacle(newX)
        lastObsX = newX
      }
    }
  }

  // Gravity
  cat.vy += 0.36
  cat.y += cat.vy
  if (cat.y >= GROUND) {
    cat.y = GROUND
    cat.vy = 0
    cat.jumping = false
  }

  cat.x = scrollX + CAT_X

  // Invincibility countdown
  if (invincible > 0) invincible--

  // Item collection
  for (const item of items) {
    if (item.collected) continue
    const dx = Math.abs((cat.x + cat.w / 2) - item.x)
    const dy = Math.abs((cat.y + cat.h / 2) - item.y)
    if (dx < 34 && dy < 36) {
      item.collected = true
      if (item.heal) {
        if (hp.value < MAX_HP) {
          hp.value++
          message.value = `捡到${item.name}，回复 1 ❤️`
          addFloatText(item.x, item.y, '+1 ❤️', '#4caf50')
        } else {
          message.value = `捡到${item.name}，生命已满`
          addFloatText(item.x, item.y, '满血', '#4caf50')
        }
      } else {
        score.value += item.pts
        message.value = `收集${item.name} +${item.pts}`
        addFloatText(item.x, item.y, `+${item.pts}`, '#5c7a5e')
      }
    }
  }

  // Obstacle collision
  if (invincible <= 0) {
    for (const obs of obstacles) {
      if (obs.hit) continue
      const catPad = 8
      const obsPad = 4
      const catLeft = cat.x + catPad
      const catRight = cat.x + cat.w - catPad
      const catTop = cat.y + catPad
      const catBottom = cat.y + cat.h - 4
      const obsLeft = obs.x + obsPad
      const obsRight = obs.x + obs.w - obsPad
      const obsTop = GROUND - obs.h

      if (catRight > obsLeft && catLeft < obsRight && catBottom > obsTop && catTop < GROUND) {
        obs.hit = true
        hp.value--
        invincible = 60
        message.value = `撞到${obs.name}！❤️ -1`
        addFloatText(obs.x, obsTop, '-1 ❤️', '#d04848')
        addHitParticles(obs.x + obs.w / 2, obsTop)

        if (hp.value <= 0) {
          lose()
          return
        }
      }
    }
  }

  // Time's up — survive = win
  if (timeLeft.value <= 0) {
    win()
    return
  }

  // Update floating texts
  floatTexts = floatTexts.filter(ft => {
    ft.life -= 0.018
    return ft.life > 0
  })

  // Update particles
  particles = particles.filter(p => {
    p.x += p.vx
    p.y += p.vy
    p.vy += 0.12
    p.life -= 0.025
    return p.life > 0
  })
}

function jump() {
  if (!cat.jumping && running.value) {
    cat.vy = -8.8
    cat.jumping = true
  }
}

function gameLoop() {
  if (!running.value) return
  update()
  draw()
  animFrame = requestAnimationFrame(gameLoop)
}

function onKeydown(e) {
  if (!running.value) return
  if (e.key === ' ' || e.key === 'ArrowUp' || e.key.toLowerCase() === 'w') {
    e.preventDefault()
    jump()
  }
}

function onClick() {
  jump()
}

function win() {
  running.value = false
  finished.value = true
  passed.value = true
  if (timer) { clearInterval(timer); timer = null }
  message.value = `夜间巡逻完成！剩余 ${hp.value} 颗生命，得分 ${score.value}。`
  draw()
  emit('complete', { passed: true, score: score.value, task: { title: '夜间巡逻' } })
}

function lose() {
  running.value = false
  finished.value = true
  passed.value = false
  if (timer) { clearInterval(timer); timer = null }
  message.value = `生命值耗尽，巡逻失败，可以再来一局。`
  draw()
  emit('complete', { passed: false, score: score.value, task: { title: '夜间巡逻' } })
}

function startGame() {
  started.value = true
  if (timer) { clearInterval(timer); timer = null }
  if (animFrame) { cancelAnimationFrame(animFrame); animFrame = null }
  score.value = 0
  hp.value = MAX_HP
  timeLeft.value = GAME_DURATION
  running.value = true
  finished.value = false
  passed.value = false
  scrollX = 0
  gameTime = 0
  invincible = 0
  difficultyLevel.value = 1
  cat = { x: CAT_X, y: GROUND, vy: 0, jumping: false, w: 32, h: 32 }
  message.value = '躲避障碍物，坚持到天亮！'
  generateWorld()
  draw()
  gameLoop()

  timer = setInterval(() => {
    timeLeft.value = Math.max(0, timeLeft.value - 1)
    if (timeLeft.value <= 0 && running.value) {
      win()
    }
  }, 1000)
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  running.value = false
  window.removeEventListener('keydown', onKeydown)
  if (timer) { clearInterval(timer); timer = null }
  if (animFrame) cancelAnimationFrame(animFrame)
})
</script>

<template>
  <div class="patrol-game">
    <div v-if="!started" class="patrol-intro">
      <div class="intro-icon">🌙</div>
      <h3>夜间巡逻</h3>
      <p>夜晚降临，{{ petName }} 出发巡逻！躲避障碍物，坚持到天亮。</p>
      <div class="intro-rules">
        <span>❤️ 生命值：{{ MAX_HP }} 点，碰到障碍物 -1</span>
        <span>🏆 胜利条件：生命值大于 0，坚持 {{ GAME_DURATION }} 秒即成功</span>
        <span>🐟 小鱼干/⭐ 星星/🎮 游戏机/🧶 毛线球/🎀 蝴蝶结/🌙 月亮 — 收集加分</span>
        <span>💚 生命之心 — 回复 1 点生命</span>
        <span>📦 纸箱/🧹 扫帚/🪑 椅子/🧱 砖块/🪣 水桶 — 碰到会受伤</span>
        <span>难度随时间递增：速度加快，障碍物更密集</span>
      </div>
      <button class="ctrl-primary" @click="startGame">开始巡逻</button>
      <div class="patrol-footer">
        <button class="back-btn" @click="emit('back')">← 返回游戏盒</button>
      </div>
    </div>

    <template v-else>
    <div class="patrol-hud">
      <div class="hud-item">
        <span class="hud-label">生命</span>
        <span class="hud-value hp-val" :class="{ warn: hp <= 2, danger: hp <= 1 }">
          <span v-for="i in MAX_HP" :key="i" class="hp-heart">{{ i <= hp ? '❤️' : '🖤' }}</span>
        </span>
      </div>
      <div class="hud-item">
        <span class="hud-label">时间</span>
        <span class="hud-value" :class="{ warn: timeLeft <= 10 }">{{ timeLeft }}s</span>
      </div>
      <div class="hud-item">
        <span class="hud-label">得分</span>
        <span class="hud-value">{{ score }}</span>
      </div>
      <div class="hud-item">
        <span class="hud-label">区域</span>
        <span class="hud-value">{{ difficultyLevel }}</span>
      </div>
    </div>

    <div class="patrol-stage" @click="onClick">
      <canvas ref="canvasRef"></canvas>
    </div>

    <p class="patrol-msg" :class="{ success: passed, fail: finished && !passed }">{{ message }}</p>

    <div class="patrol-ctrl">
      <button class="ctrl-jump" @click="jump" :disabled="!running">跳跃</button>
      <button class="ctrl-primary" @click="finished ? startGame() : null">
        {{ finished ? '再来一局' : '巡逻中...' }}
      </button>
    </div>

    <p class="patrol-hint">空格 / 点击屏幕跳跃</p>

    <div class="patrol-footer">
      <button class="back-btn" @click="emit('back')">← 返回游戏盒</button>
    </div>
    </template>
  </div>
</template>

<style scoped>
.patrol-game {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  align-items: center;
}
.patrol-intro {
  width: 100%;
  max-width: 500px;
  min-height: 400px;
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
.patrol-intro h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.3rem;
  color: var(--text-primary);
}
.patrol-intro p {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.82rem;
  line-height: 1.6;
}
.intro-rules {
  display: flex;
  flex-direction: column;
  gap: 5px;
  color: var(--text-secondary);
  font-size: 0.76rem;
  text-align: left;
  background: rgba(255,255,255,0.4);
  padding: 10px 14px;
  border-radius: var(--radius-sm);
}
.patrol-hud {
  display: flex;
  gap: 8px;
  width: 100%;
  max-width: 500px;
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
.hud-value.warn { color: #e0b040; }
.hud-value.danger { color: #d04848; }
.hp-val { font-size: 0.72rem; letter-spacing: 1px; }
.hp-heart { font-size: 0.7rem; }
.patrol-stage {
  border: 2px solid var(--border-default);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  line-height: 0;
  max-width: 100%;
}
.patrol-stage canvas {
  display: block;
  max-width: 100%;
  height: auto;
}
.patrol-msg {
  min-height: 20px;
  margin: 0;
  font-size: 0.78rem;
  color: var(--text-muted);
  text-align: center;
}
.patrol-msg.success { color: var(--accent); font-weight: 600; }
.patrol-msg.fail { color: var(--spot); }
.patrol-ctrl {
  display: flex;
  gap: 0.8rem;
}
.ctrl-jump {
  padding: 10px 24px;
  border: 1px solid var(--spot);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--spot);
  font-family: var(--font-body);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
}
.ctrl-jump:hover:not(:disabled) {
  background: var(--spot);
  color: #fff;
}
.ctrl-jump:disabled { opacity: 0.4; cursor: not-allowed; }
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
.patrol-hint {
  margin: 0;
  font-size: 0.65rem;
  color: var(--text-muted);
  opacity: 0.7;
}
.patrol-footer { width: 100%; max-width: 500px; }
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
