<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  petName: { type: String, default: '猫咪' },
  pet: { type: Object, default: null }
})
const emit = defineEmits(['complete', 'back'])

const COLS = 10
const ROWS = 10
const CELL = 40

const TILE = {
  EMPTY: '.',
  WALL: '#',
  START: 'S',
  FISH: 'F',
  KEY: 'K',
  EXIT: 'E',
  SWITCH: 'B',
  DOOR: 'D',
  QUIZ: 'Q',
  MONSTER: 'M',
  TRAP: 'T'
}

const levels = [
  {
    name: '纸箱入口',
    tag: '热身',
    time: 70,
    hint: '找到钥匙，再走到出口。',
    map: [
      '##########',
      '#S..F...E#',
      '#.####.#.#',
      '#....#.#.#',
      '####.#.#.#',
      '#....#...#',
      '#.######.#',
      '#.....K..#',
      '#........#',
      '##########'
    ]
  },
  {
    name: '绕路找钥匙',
    tag: '路径',
    time: 68,
    hint: '钥匙在远端，出口在另一侧。',
    map: [
      '##########',
      '#S..#...E#',
      '#.#.#.##.#',
      '#.#.#....#',
      '#.#.####.#',
      '#.#....#.#',
      '#.####.#.#',
      '#....#K#.#',
      '#F.......#',
      '##########'
    ]
  },
  {
    name: '鱼干岔路',
    tag: '收集',
    time: 66,
    hint: '鱼干加分，但别被岔路拖太久。',
    map: [
      '##########',
      '#S....#E.#',
      '#.####.#.#',
      '#....#.#.#',
      '#.##.#.#.#',
      '#F#..#...#',
      '#.#.####.#',
      '#.#....K.#',
      '#...F....#',
      '##########'
    ]
  },
  {
    name: '纸箱机关',
    tag: '机关',
    time: 64,
    hint: '先踩按钮，纸箱门才会打开。',
    map: [
      '##########',
      '#S..#...E#',
      '#.#.#.##D#',
      '#.#.#....#',
      '#.#.####.#',
      '#.#....#.#',
      '#.####.#.#',
      '#....#K#.#',
      '#B..F....#',
      '##########'
    ]
  },
  {
    name: '知识门',
    tag: '问答',
    time: 62,
    hint: '答对知识门的问题，才适合继续前进。',
    quiz: {
      question: '猫咪正常情况下更适合频繁喝哪一种？',
      options: ['清水', '加糖饮料', '浓茶'],
      answer: 0
    },
    map: [
      '##########',
      '#S..#...E#',
      '#.#.#.##.#',
      '#.#.#..Q.#',
      '#.#.####.#',
      '#.#....#.#',
      '#.####.#.#',
      '#....#K#.#',
      '#F.......#',
      '##########'
    ]
  },
  {
    name: '影子巡逻',
    tag: '躲避',
    time: 60,
    hint: '影子会巡逻，碰到会扣时间并回到起点。',
    monsters: [{ r: 5, c: 7, dir: -1, axis: 'h', min: 5, max: 7, speed: 4 }],
    map: [
      '##########',
      '#S..#...E#',
      '#.#.#.##.#',
      '#.#.#....#',
      '#.#.####.#',
      '#.#....M.#',
      '#.####.#.#',
      '#....#K#.#',
      '#F.......#',
      '##########'
    ]
  },
  {
    name: '按钮与谜题',
    tag: '综合',
    time: 58,
    hint: '踩按钮、答题、拿钥匙，顺序别乱。',
    quiz: {
      question: '刚到家的猫咪更需要什么？',
      options: ['安静适应', '立刻洗澡', '强行抱很久'],
      answer: 0
    },
    map: [
      '##########',
      '#S..#..QE#',
      '#.#.#.##D#',
      '#.#.#....#',
      '#.#.####.#',
      '#.#....#.#',
      '#.####.#.#',
      '#B...#K#.#',
      '#F.......#',
      '##########'
    ]
  },
  {
    name: '双影回廊',
    tag: '反应',
    time: 56,
    hint: '两只影子错开移动，观察节奏再走。',
    monsters: [
      { r: 3, c: 5, dir: 1, axis: 'h', min: 5, max: 8, speed: 3 },
      { r: 7, c: 2, dir: 1, axis: 'v', min: 6, max: 8, speed: 4 }
    ],
    map: [
      '##########',
      '#S.....#E#',
      '#.#####..#',
      '#....M...#',
      '####.###.#',
      '#....#...#',
      '#..#.#K#.#',
      '#.M#.....#',
      '#F.......#',
      '##########'
    ]
  },
  {
    name: '陷阱仓库',
    tag: '判断',
    time: 54,
    hint: '星号陷阱会扣时间，能绕就绕。',
    monsters: [{ r: 6, c: 6, dir: -1, axis: 'h', min: 4, max: 7, speed: 3 }],
    map: [
      '##########',
      '#S..T..#E#',
      '#.###T.#.#',
      '#...#..#.#',
      '###.#.##.#',
      '#...#....#',
      '#.###.M#.#',
      '#...T#K#.#',
      '#F.......#',
      '##########'
    ]
  },
  {
    name: '奶茶终章',
    tag: '终章',
    time: 52,
    hint: '机关、知识门、巡逻影子都会出现。',
    quiz: {
      question: '猫咪突然不吃饭、精神差时，最稳妥的做法是？',
      options: ['观察并及时就医', '只给零食', '强行加大运动'],
      answer: 0
    },
    monsters: [
      { r: 3, c: 5, dir: 1, axis: 'h', min: 5, max: 8, speed: 3 },
      { r: 7, c: 2, dir: 1, axis: 'v', min: 6, max: 8, speed: 3 },
      { r: 5, c: 7, dir: -1, axis: 'h', min: 5, max: 7, speed: 4 }
    ],
    map: [
      '##########',
      '#S..#..QE#',
      '#.#.#.##D#',
      '#.#.#M...#',
      '#.#.####.#',
      '#.#....M.#',
      '#.####.#.#',
      '#BM..#K#T#',
      '#F.......#',
      '##########'
    ]
  }
]

const grid = ref([])
const monsters = ref([])
const levelIndex = ref(0)
const playerPos = ref({ r: 1, c: 1 })
const startPos = ref({ r: 1, c: 1 })
const hasKey = ref(false)
const switchOn = ref(false)
const score = ref(0)
const timeLeft = ref(levels[0].time)
const started = ref(false)
const running = ref(false)
const finished = ref(false)
const passed = ref(false)
const message = ref('')
const quizOpen = ref(false)
const quizAnswered = ref(false)
const canvasRef = ref(null)

let timer = null
let monsterTimer = null
let monsterTick = 0

const currentLevel = computed(() => levels[levelIndex.value])
const levelNo = computed(() => levelIndex.value + 1)
const totalLevels = computed(() => levels.length)
const objective = computed(() => {
  const parts = []
  if (!hasKey.value) parts.push('钥匙')
  if (currentLevel.value.map.some(row => row.includes(TILE.SWITCH)) && !switchOn.value) parts.push('按钮')
  if (currentLevel.value.quiz && !quizAnswered.value) parts.push('知识门')
  return parts.length ? `还需：${parts.join('、')}` : '前往出口'
})

const colors = {
  bg: '#f5efe0',
  wall: '#8B7355',
  wallBorder: '#6B5335',
  empty: '#faf7f0',
  switch: '#d6b36c',
  door: '#9f7f52',
  quiz: '#6d8f8b',
  trap: '#c17b60',
  grid: '#e8e0d0'
}

function parseLevel(level) {
  const next = level.map.map(row => row.split(''))
  const nextMonsters = []
  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < COLS; c++) {
      if (next[r][c] === TILE.START) {
        startPos.value = { r, c }
        playerPos.value = { r, c }
        next[r][c] = TILE.EMPTY
      }
      if (next[r][c] === TILE.MONSTER) {
        nextMonsters.push({ r, c, dir: 1, axis: 'h', min: c, max: c, speed: 4 })
        next[r][c] = TILE.EMPTY
      }
    }
  }
  monsters.value = (level.monsters || nextMonsters).map(m => ({ ...m }))
  grid.value = next
}

function isDoorOpen() {
  return switchOn.value
}

function canEnter(tile) {
  if (tile === TILE.WALL) return false
  if (tile === TILE.DOOR && !isDoorOpen()) return false
  return true
}

function stopTimers() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  if (monsterTimer) {
    clearInterval(monsterTimer)
    monsterTimer = null
  }
}

function stopGame() {
  running.value = false
  stopTimers()
}

function startTimers() {
  stopTimers()
  timer = setInterval(() => {
    if (!running.value || quizOpen.value) return
    timeLeft.value = Math.max(0, timeLeft.value - 1)
    if (timeLeft.value <= 0) failGame()
  }, 1000)
  monsterTimer = setInterval(() => {
    if (!running.value || quizOpen.value || finished.value) return
    moveMonsters()
  }, 420)
}

function beginLevel(index) {
  levelIndex.value = index
  hasKey.value = false
  switchOn.value = false
  quizOpen.value = false
  quizAnswered.value = false
  timeLeft.value = currentLevel.value.time
  finished.value = false
  passed.value = false
  running.value = true
  monsterTick = 0
  parseLevel(currentLevel.value)
  message.value = `第 ${levelNo.value} 关：${currentLevel.value.hint}`
  draw()
  startTimers()
}

function startGame() {
  started.value = true
  score.value = 0
  beginLevel(0)
}

function restartGame() {
  score.value = 0
  beginLevel(0)
}

function drawTile(ctx, tile, x, y) {
  if (tile === TILE.WALL) {
    ctx.fillStyle = colors.wall
    ctx.fillRect(x, y, CELL, CELL)
    ctx.strokeStyle = colors.wallBorder
    ctx.lineWidth = 1
    ctx.strokeRect(x + 0.5, y + 0.5, CELL - 1, CELL - 1)
    return
  }

  ctx.fillStyle = colors.empty
  ctx.fillRect(x, y, CELL, CELL)
  ctx.strokeStyle = colors.grid
  ctx.lineWidth = 0.5
  ctx.strokeRect(x, y, CELL, CELL)

  ctx.font = '20px serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  if (tile === TILE.FISH) ctx.fillText('🐟', x + CELL / 2, y + CELL / 2)
  if (tile === TILE.KEY) ctx.fillText('🔑', x + CELL / 2, y + CELL / 2)
  if (tile === TILE.EXIT) ctx.fillText(hasKey.value ? '🚪' : '🔒', x + CELL / 2, y + CELL / 2)
  if (tile === TILE.SWITCH) {
    ctx.fillStyle = switchOn.value ? '#dfead5' : '#fff4ce'
    ctx.fillRect(x + 7, y + 7, CELL - 14, CELL - 14)
    ctx.fillStyle = '#6b5335'
    ctx.fillText(switchOn.value ? '✓' : '🔘', x + CELL / 2, y + CELL / 2)
  }
  if (tile === TILE.DOOR) {
    ctx.fillStyle = isDoorOpen() ? '#e7f0de' : colors.door
    ctx.fillRect(x + 4, y + 4, CELL - 8, CELL - 8)
    ctx.fillStyle = '#6b5335'
    ctx.fillText(isDoorOpen() ? '□' : '🚪', x + CELL / 2, y + CELL / 2)
  }
  if (tile === TILE.QUIZ) ctx.fillText(quizAnswered.value ? '✓' : '❓', x + CELL / 2, y + CELL / 2)
  if (tile === TILE.TRAP) ctx.fillText('✦', x + CELL / 2, y + CELL / 2)
}

function draw() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  canvas.width = COLS * CELL
  canvas.height = ROWS * CELL

  ctx.fillStyle = colors.bg
  ctx.fillRect(0, 0, canvas.width, canvas.height)

  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < COLS; c++) {
      drawTile(ctx, grid.value[r]?.[c] ?? TILE.WALL, c * CELL, r * CELL)
    }
  }

  for (const monster of monsters.value) {
    ctx.font = '22px serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('👾', monster.c * CELL + CELL / 2, monster.r * CELL + CELL / 2)
  }

  ctx.font = '24px serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('🐱', playerPos.value.c * CELL + CELL / 2, playerPos.value.r * CELL + CELL / 2)
}

function handleHazard(text, penalty = 8) {
  timeLeft.value = Math.max(0, timeLeft.value - penalty)
  playerPos.value = { ...startPos.value }
  message.value = `${text}，扣 ${penalty} 秒并回到起点。`
  if (timeLeft.value <= 0) {
    failGame()
    return true
  }
  draw()
  return false
}

function checkMonsterCollision() {
  if (monsters.value.some(m => m.r === playerPos.value.r && m.c === playerPos.value.c)) {
    return handleHazard('被巡逻影子碰到了', 8)
  }
  return false
}

function moveMonsters() {
  monsterTick++
  monsters.value = monsters.value.map(monster => {
    if (monsterTick % monster.speed !== 0) return monster
    const next = { ...monster }
    const key = monster.axis === 'v' ? 'r' : 'c'
    next[key] += monster.dir
    if (next[key] < monster.min || next[key] > monster.max || !canEnter(grid.value[next.r]?.[next.c] ?? TILE.WALL)) {
      next.dir = -monster.dir
      next[key] = monster[key] + next.dir
    }
    return next
  })
  checkMonsterCollision()
  draw()
}

function openQuiz() {
  if (!currentLevel.value.quiz || quizAnswered.value) return
  quizOpen.value = true
  running.value = false
  message.value = '知识门开启：答对后继续前进。'
  draw()
}

function answerQuiz(index) {
  const quiz = currentLevel.value.quiz
  if (!quiz) return
  quizOpen.value = false
  running.value = true
  if (index === quiz.answer) {
    quizAnswered.value = true
    score.value += 15
    message.value = '答对了！知识门放行，得分 +15。'
  } else {
    timeLeft.value = Math.max(0, timeLeft.value - 10)
    message.value = '答错了，扣 10 秒。再想想题目答案。'
    if (timeLeft.value <= 0) {
      failGame()
      return
    }
  }
  draw()
}

function completeLevel() {
  score.value += 25 + Math.max(0, timeLeft.value)
  if (levelIndex.value >= levels.length - 1) {
    winGame()
    return
  }
  beginLevel(levelIndex.value + 1)
}

function winGame() {
  stopGame()
  finished.value = true
  passed.value = true
  message.value = `${props.petName} 完成 10 关纸箱迷宫！总分 ${score.value}`
  draw()
  emit('complete', { passed: true, score: score.value, task: { title: '纸箱迷宫 10 关' } })
}

function failGame() {
  stopGame()
  finished.value = true
  passed.value = false
  const left = totalLevels.value - levelIndex.value
  message.value = `挑战结束，停在第 ${levelNo.value} 关，还差 ${left} 关完成全部挑战。`
  draw()
  emit('complete', { passed: false, score: score.value, task: { title: '纸箱迷宫 10 关' } })
}

function movePlayer(dr, dc) {
  if (!running.value || finished.value || quizOpen.value) return
  const nr = playerPos.value.r + dr
  const nc = playerPos.value.c + dc
  const tile = grid.value[nr]?.[nc] ?? TILE.WALL
  if (!canEnter(tile)) {
    message.value = tile === TILE.DOOR ? '纸箱门还锁着，先找按钮。' : '这里被纸箱墙挡住了。'
    draw()
    return
  }

  playerPos.value = { r: nr, c: nc }

  if (checkMonsterCollision()) return
  if (tile === TILE.FISH) {
    score.value += 10
    grid.value[nr][nc] = TILE.EMPTY
    message.value = '找到小鱼干 +10。'
  } else if (tile === TILE.KEY) {
    hasKey.value = true
    grid.value[nr][nc] = TILE.EMPTY
    message.value = '拿到钥匙！可以去出口了。'
  } else if (tile === TILE.SWITCH) {
    switchOn.value = true
    grid.value[nr][nc] = TILE.EMPTY
    score.value += 8
    message.value = '踩下按钮，纸箱门打开了。'
  } else if (tile === TILE.QUIZ && currentLevel.value.quiz && !quizAnswered.value) {
    openQuiz()
    return
  } else if (tile === TILE.TRAP) {
    if (handleHazard('踩到陷阱', 6)) return
  } else if (tile === TILE.EXIT) {
    if (!hasKey.value) {
      message.value = '出口锁着，先找到钥匙。'
    } else if (currentLevel.value.quiz && !quizAnswered.value) {
      message.value = '还没通过知识门，先去解谜。'
    } else {
      completeLevel()
      return
    }
  } else {
    message.value = objective.value
  }

  draw()
}

function onKeydown(e) {
  if (!running.value || quizOpen.value) return
  if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'w', 'a', 's', 'd', 'W', 'A', 'S', 'D'].includes(e.key)) {
    e.preventDefault()
  }
  if (e.key === 'ArrowUp' || e.key.toLowerCase() === 'w') movePlayer(-1, 0)
  if (e.key === 'ArrowDown' || e.key.toLowerCase() === 's') movePlayer(1, 0)
  if (e.key === 'ArrowLeft' || e.key.toLowerCase() === 'a') movePlayer(0, -1)
  if (e.key === 'ArrowRight' || e.key.toLowerCase() === 'd') movePlayer(0, 1)
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  stopGame()
})
</script>

<template>
  <div class="maze-game">
    <div v-if="!started" class="maze-intro">
      <div class="intro-icon">🗺️</div>
      <h3>纸箱迷宫 10 关</h3>
      <p>从热身迷宫开始，逐步加入按钮机关、知识问答、陷阱和巡逻影子。完成第 10 关才算总通关。</p>
      <div class="intro-rules">
        <span>🔑 拿钥匙后才能开出口</span>
        <span>🔘 按钮会打开纸箱门</span>
        <span>❓ 答对知识门问题继续前进</span>
        <span>👾 避开巡逻影子，碰到会扣时间</span>
      </div>
      <div class="maze-ctrl">
        <button class="ctrl-primary" @click="startGame">开始第 1 关</button>
      </div>
      <div class="maze-footer">
        <button class="back-btn" @click="emit('back')">← 返回游戏盒</button>
      </div>
    </div>

    <template v-else>
      <div class="level-strip">
        <span
          v-for="(_, index) in levels"
          :key="index"
          :class="{ current: index === levelIndex, done: index < levelIndex }"
        >
          {{ index + 1 }}
        </span>
      </div>

      <div class="maze-hud">
        <div class="hud-item">
          <span class="hud-label">关卡</span>
          <span class="hud-value">{{ levelNo }}/{{ totalLevels }}</span>
        </div>
        <div class="hud-item">
          <span class="hud-label">分数</span>
          <span class="hud-value">{{ score }}</span>
        </div>
        <div class="hud-item">
          <span class="hud-label">时间</span>
          <span class="hud-value" :class="{ warn: timeLeft < 12 }">{{ timeLeft }}s</span>
        </div>
        <div class="hud-item">
          <span class="hud-label">状态</span>
          <span class="hud-value small">{{ objective }}</span>
        </div>
      </div>

      <div class="level-title">
        <strong>第 {{ levelNo }} 关 · {{ currentLevel.name }}</strong>
        <span>{{ currentLevel.tag }}</span>
      </div>

      <div class="maze-stage">
        <canvas ref="canvasRef"></canvas>
        <div v-if="quizOpen" class="quiz-modal">
          <div class="quiz-box">
            <p>{{ currentLevel.quiz.question }}</p>
            <button
              v-for="(option, index) in currentLevel.quiz.options"
              :key="option"
              @click="answerQuiz(index)"
            >
              {{ option }}
            </button>
          </div>
        </div>
      </div>

      <p class="maze-msg" :class="{ success: passed, fail: finished && !passed }">{{ message }}</p>

      <div class="maze-ctrl">
        <div class="ctrl-dpad">
          <button @click="movePlayer(-1, 0)" :disabled="!running">▲</button>
          <div class="ctrl-row">
            <button @click="movePlayer(0, -1)" :disabled="!running">◀</button>
            <button @click="movePlayer(1, 0)" :disabled="!running">▼</button>
            <button @click="movePlayer(0, 1)" :disabled="!running">▶</button>
          </div>
        </div>
        <button class="ctrl-primary" @click="finished ? restartGame() : null">
          {{ finished ? '从第 1 关再来' : '闯关中...' }}
        </button>
      </div>

      <p class="maze-hint">方向键或 WASD 控制移动</p>

      <div class="maze-footer">
        <button class="back-btn" @click="emit('back')">← 返回游戏盒</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.maze-game {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  align-items: center;
}
.maze-intro {
  width: 100%;
  max-width: 480px;
  min-height: 390px;
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
.maze-intro h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.3rem;
  color: var(--text-primary);
}
.maze-intro p {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.82rem;
  line-height: 1.6;
}
.intro-rules {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px 12px;
  color: var(--text-secondary);
  font-size: 0.76rem;
}
.level-strip {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 4px;
  width: 100%;
  max-width: 520px;
}
.level-strip span {
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  color: var(--text-muted);
  font-size: 0.68rem;
}
.level-strip span.done {
  background: rgba(92, 122, 94, 0.1);
  color: var(--accent);
}
.level-strip span.current {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}
.maze-hud {
  display: flex;
  gap: 8px;
  width: 100%;
  max-width: 560px;
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
.hud-value.small {
  font-size: 0.7rem;
  white-space: nowrap;
}
.hud-value.warn { color: var(--spot); }
.level-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 0.95rem;
}
.level-title span {
  border: 1px solid rgba(92, 122, 94, 0.25);
  border-radius: var(--radius-full);
  padding: 2px 8px;
  color: var(--accent);
  font-family: var(--font-body);
  font-size: 0.68rem;
}
.maze-stage {
  position: relative;
  border: 2px solid var(--border-default);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--bg-warm);
  line-height: 0;
}
.maze-stage canvas {
  display: block;
  max-width: 100%;
  height: auto;
}
.quiz-modal {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(250, 247, 240, 0.78);
  line-height: 1.4;
}
.quiz-box {
  width: min(320px, calc(100% - 32px));
  padding: 1rem;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  box-shadow: 0 10px 24px rgba(88, 68, 43, 0.14);
}
.quiz-box p {
  margin: 0 0 0.8rem;
  color: var(--text-primary);
  font-size: 0.86rem;
  line-height: 1.5;
}
.quiz-box button {
  display: block;
  width: 100%;
  margin-top: 6px;
  padding: 8px 10px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-family: var(--font-body);
  cursor: pointer;
}
.quiz-box button:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.maze-msg {
  min-height: 20px;
  margin: 0;
  font-size: 0.78rem;
  color: var(--text-muted);
  text-align: center;
}
.maze-msg.success { color: var(--accent); font-weight: 600; }
.maze-msg.fail { color: var(--spot); }
.maze-ctrl {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}
.ctrl-dpad {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.ctrl-dpad > button,
.ctrl-row button {
  width: 40px;
  height: 36px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-size: 0.9rem;
  cursor: pointer;
  transition: all var(--duration-fast);
}
.ctrl-dpad > button:hover:not(:disabled),
.ctrl-row button:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}
.ctrl-dpad > button:disabled,
.ctrl-row button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.ctrl-row {
  display: flex;
  gap: 4px;
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
.maze-hint {
  margin: 0;
  font-size: 0.65rem;
  color: var(--text-muted);
  opacity: 0.7;
}
.maze-footer {
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

@media (max-width: 560px) {
  .maze-hud {
    flex-wrap: wrap;
  }
  .hud-item {
    min-width: calc(50% - 4px);
  }
  .intro-rules {
    grid-template-columns: 1fr;
  }
}
</style>
