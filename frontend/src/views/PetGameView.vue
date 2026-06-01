<script setup>
import { computed, onMounted, ref, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePets } from '../composables/usePets'
import { getAgentTasks } from '../api/agent'
import ThemedIcon from '../components/common/ThemedIcon.vue'

import CatchFishGame from '../components/pet/games/CatchFishGame.vue'
import BoxMazeGame from '../components/pet/games/BoxMazeGame.vue'
import LaserChaseGame from '../components/pet/games/LaserChaseGame.vue'
import NestTidyGame from '../components/pet/games/NestTidyGame.vue'
import DinnerMatchGame from '../components/pet/games/DinnerMatchGame.vue'
import NightPatrolGame from '../components/pet/games/NightPatrolGame.vue'

const route = useRoute()
const router = useRouter()
const petId = Number(route.params.id)
const { getPet } = usePets()

const pet = ref(null)
const tasks = ref([])
const loading = ref(true)
const loadError = ref('')
const activeGameId = ref('')
const resultText = ref('')
const feedbackLog = ref([])

const gameComponents = {
  catch: CatchFishGame,
  maze: BoxMazeGame,
  laser: LaserChaseGame,
  tidy: NestTidyGame,
  dinner: DinnerMatchGame,
  patrol: NightPatrolGame
}

const games = [
  {
    id: 'catch',
    name: '小鱼干接力',
    icon: 'bowl',
    difficulty: '入门',
    goal: '60分',
    type: '反应力',
    desc: '接住小鱼干和玩具，躲开纸箱，连续接到触发连击加分。',
    emoji: '🐟'
  },
  {
    id: 'maze',
    name: '纸箱迷宫',
    icon: 'folder',
    difficulty: '挑战',
    goal: '10关',
    type: '解谜闯关',
    desc: '连续挑战 10 关纸箱迷宫，解机关、答问题、躲开巡逻影子。',
    emoji: '🗺️'
  },
  {
    id: 'laser',
    name: '逗猫棒追逐',
    icon: 'paw',
    difficulty: '入门',
    goal: '12次',
    type: '陪伴互动',
    desc: '点击引导光点，猫咪追逐，连续命中加速。',
    emoji: '🔴'
  },
  {
    id: 'tidy',
    name: '猫窝整理',
    icon: 'heart',
    difficulty: '休闲',
    goal: '12个',
    type: '习惯养成',
    desc: '把小鱼干放碗里、玩具放箱子、纸箱放架子，分类整理。',
    emoji: '🧹'
  },
  {
    id: 'dinner',
    name: '晚餐配餐',
    icon: 'bowl',
    difficulty: '休闲',
    goal: '80%',
    type: '饮食科普',
    desc: '根据猫咪状态选择食物组合，匹配度越高越好。',
    emoji: '🍽️'
  },
  {
    id: 'patrol',
    name: '夜间巡逻',
    icon: 'collar',
    difficulty: '挑战',
    goal: '保持生命',
    type: '趣味闯关',
    desc: '猫咪夜里巡逻房间，躲避障碍物坚持到天亮，收集物品加分。',
    emoji: '🌙'
  }
]

const activeGame = computed(() => games.find(g => g.id === activeGameId.value))
const activeComponent = computed(() => gameComponents[activeGameId.value] || null)

const recommendation = computed(() => {
  if (!pet.value) return null
  // Simple agent recommendation logic based on available data
  if (tasks.value.length > 0) {
    const playTask = tasks.value.find(t => t.type === 'play')
    if (playTask) {
      return { gameId: 'catch', reason: `Agent 建议：${playTask.reason || '先玩小鱼干接力练习反应力'}` }
    }
  }
  if (!pet.value.breed || pet.value.breed === '未知品种') {
    return { gameId: 'dinner', reason: '还不太了解宠物的饮食偏好，先试试晚餐配餐吧。' }
  }
  return { gameId: 'laser', reason: '逗猫棒追逐能增进互动陪伴，试试看？' }
})

onMounted(async () => {
  try {
    pet.value = await getPet(petId)
    const res = await getAgentTasks(petId)
    tasks.value = res.data || []
  } catch (e) {
    loadError.value = e?.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
})

function enterGame(gameId) {
  activeGameId.value = gameId
  resultText.value = ''
  feedbackLog.value = []
}

function backToBox() {
  activeGameId.value = ''
  feedbackLog.value = []
}

function goBack() {
  router.push(`/pet/${petId}/3d`)
}

function onGameComplete(result) {
  const game = activeGame.value
  if (result.passed) {
    resultText.value = `「${game?.name}」完成，得分 ${result.score}。结果仅作为娱乐反馈，不会写入时间线。`
  } else {
    resultText.value = `「${game?.name}」结束，得分 ${result.score}，未达目标。可以再来一局。`
  }
  feedbackLog.value.unshift({
    id: Date.now(),
    text: `${game?.name}: ${result.passed ? '通过' : '未通过'} (${result.score}分)`
  })
  feedbackLog.value = feedbackLog.value.slice(0, 5)
}
</script>

<template>
  <div class="game-page">
    <!-- Header -->
    <div class="game-header">
      <button class="back-btn" @click="goBack">
        <ThemedIcon name="paw" :size="13" /> 返回 3D 房间
      </button>
      <div class="title-block">
        <p>猫咪游戏盒</p>
        <h1>{{ pet?.name || '猫咪' }}的游戏盒</h1>
      </div>
      <span class="data-badge">不写入时间线</span>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="game-state">
      <span class="spinner"></span>
      <p>正在加载游戏盒...</p>
    </div>

    <!-- Error -->
    <div v-else-if="loadError" class="game-state error">
      <p>{{ loadError }}</p>
      <button class="back-btn" @click="goBack">返回</button>
    </div>

    <!-- Main layout -->
    <div v-else class="game-layout">
      <!-- Sidebar -->
      <aside class="game-side">
        <!-- Pet info -->
        <div class="pet-card">
          <img v-if="pet?.avatarUrl" :src="pet.avatarUrl" alt="" />
          <div v-else class="avatar-ph"><ThemedIcon name="paw" :size="22" /></div>
          <div>
            <h2>{{ pet?.name }}</h2>
            <p>{{ pet?.species }} · {{ pet?.breed || '未知品种' }}</p>
          </div>
        </div>

        <!-- Agent recommendation -->
        <section class="rec-card" v-if="recommendation && !activeGameId">
          <div class="rec-head">
            <ThemedIcon name="quill" :size="14" />
            <span>Agent 推荐</span>
          </div>
          <p class="rec-reason">{{ recommendation.reason }}</p>
          <button class="rec-btn" @click="enterGame(recommendation.gameId)">
            去试试
          </button>
        </section>

        <!-- Data boundary -->
        <section class="boundary-card">
          <h3>数据边界</h3>
          <ul>
            <li>游戏结果不写入成长时间线</li>
            <li>不影响成长报告和健康统计</li>
            <li>仅作为娱乐互动反馈</li>
          </ul>
          <div v-if="resultText" class="result-text">
            <p>{{ resultText }}</p>
          </div>
          <div v-if="feedbackLog.length" class="feedback-log">
            <span v-for="item in feedbackLog" :key="item.id">{{ item.text }}</span>
          </div>
        </section>
      </aside>

      <!-- Main content -->
      <main class="game-main">
        <!-- Game box home -->
        <div v-if="!activeGameId" class="game-box">
          <div class="box-head">
            <p>选择一个小游戏开始</p>
            <h2>{{ pet?.name }}的猫咪游戏盒</h2>
            <span>共 {{ games.length }} 个玩法不同的小游戏，结果不会进入成长报告。</span>
          </div>
          <div class="game-grid">
            <button
              v-for="game in games"
              :key="game.id"
              class="game-card"
              @click="enterGame(game.id)"
            >
              <span class="game-icon">{{ game.emoji }}</span>
              <span class="game-info">
                <strong>{{ game.name }}</strong>
                <small>{{ game.desc }}</small>
              </span>
              <span class="game-tags">
                <em class="tag-type">{{ game.type }}</em>
                <em class="tag-diff">{{ game.difficulty }}</em>
              </span>
            </button>
          </div>
        </div>

        <!-- Active game -->
        <div v-else class="game-panel">
          <div class="play-head">
            <button class="back-box" @click="backToBox">
              <ThemedIcon name="folder" :size="13" /> 返回游戏盒
            </button>
            <div>
              <p>正在游玩</p>
              <h2>{{ activeGame?.name }}</h2>
            </div>
            <span class="play-goal">目标 {{ activeGame?.goal }}</span>
          </div>
          <component
            :is="activeComponent"
            :pet-name="pet?.name || '猫咪'"
            :pet="pet"
            @complete="onGameComplete"
            @back="backToBox"
          />
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.game-page {
  flex: 1;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0.8rem 1.5rem 1.5rem;
}

/* Header */
.game-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.8rem;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  padding: 5px 12px;
  font-family: var(--font-body);
  font-size: 0.78rem;
  cursor: pointer;
}
.back-btn:hover { border-color: var(--accent); color: var(--accent); }
.title-block { flex: 1; }
.title-block p {
  margin: 0 0 2px;
  color: var(--accent);
  font-size: 0.72rem;
}
.title-block h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.3rem;
  color: var(--text-primary);
}
.data-badge {
  border: 1px solid rgba(92, 122, 94, 0.35);
  border-radius: var(--radius-full);
  padding: 4px 12px;
  color: var(--accent);
  background: rgba(92, 122, 94, 0.06);
  font-size: 0.72rem;
  white-space: nowrap;
}

/* Loading/Error */
.game-state {
  min-height: 40vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  gap: 0.8rem;
}
.game-state.error { color: var(--spot); }
.spinner {
  width: 18px; height: 18px;
  border: 2px solid var(--border-default);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Layout */
.game-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 1rem;
  align-items: start;
}
.game-side {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.game-main { min-width: 0; }

/* Cards */
.pet-card,
.rec-card,
.boundary-card,
.game-box,
.game-panel {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

/* Pet card */
.pet-card {
  display: flex;
  gap: 0.7rem;
  align-items: center;
  padding: 0.8rem;
}
.pet-card img,
.avatar-ph {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-input);
  color: var(--text-muted);
}
.pet-card h2 {
  margin: 0 0 2px;
  font-family: var(--font-display);
  font-size: 0.95rem;
}
.pet-card p {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.72rem;
}

/* Recommendation */
.rec-card {
  padding: 0.8rem;
}
.rec-head {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--accent);
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 0.85rem;
  margin-bottom: 0.4rem;
}
.rec-reason {
  margin: 0 0 0.6rem;
  font-size: 0.74rem;
  color: var(--text-muted);
  line-height: 1.55;
}
.rec-btn {
  padding: 5px 14px;
  border: 1px solid var(--accent);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--accent);
  font-family: var(--font-body);
  font-size: 0.76rem;
  cursor: pointer;
}
.rec-btn:hover { background: var(--accent); color: #fff; }

/* Boundary */
.boundary-card {
  padding: 0.8rem;
}
.boundary-card h3 {
  margin: 0 0 0.5rem;
  font-family: var(--font-display);
  font-size: 0.85rem;
  color: var(--text-primary);
}
.boundary-card ul {
  margin: 0;
  padding: 0 0 0 1rem;
  font-size: 0.72rem;
  color: var(--text-muted);
  line-height: 1.8;
}
.result-text {
  margin-top: 0.6rem;
  padding-top: 0.6rem;
  border-top: 1px solid var(--border-subtle);
}
.result-text p {
  margin: 0;
  font-size: 0.72rem;
  color: var(--accent);
  line-height: 1.55;
}
.feedback-log {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 0.4rem;
}
.feedback-log span {
  font-size: 0.68rem;
  color: var(--text-muted);
}

/* Game box */
.game-box {
  padding: 1.2rem;
}
.box-head {
  text-align: center;
  margin-bottom: 1.2rem;
}
.box-head p {
  margin: 0 0 4px;
  color: var(--accent);
  font-size: 0.75rem;
}
.box-head h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.4rem;
  color: var(--text-primary);
}
.box-head span {
  display: block;
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 0.78rem;
}

/* Game grid */
.game-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}
.game-card {
  position: relative;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 0.7rem;
  padding: 0.9rem;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  background: rgba(255, 250, 241, 0.6);
  text-align: left;
  font-family: var(--font-body);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast);
}
.game-card:hover {
  border-color: var(--accent);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(88, 68, 43, 0.08);
}
.game-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.6rem;
  border: 1px solid rgba(92, 122, 94, 0.15);
  border-radius: var(--radius-sm);
  background: rgba(92, 122, 94, 0.06);
}
.game-info { min-width: 0; }
.game-info strong {
  display: block;
  font-family: var(--font-display);
  font-size: 0.92rem;
  color: var(--text-primary);
  margin-bottom: 4px;
}
.game-info small {
  display: block;
  font-size: 0.72rem;
  color: var(--text-muted);
  line-height: 1.5;
}
.game-tags {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
}
.game-tags em {
  font-style: normal;
  font-size: 0.62rem;
  padding: 2px 6px;
  border-radius: var(--radius-full);
}
.tag-type {
  border: 1px solid rgba(92, 122, 94, 0.25);
  color: var(--accent);
  background: rgba(92, 122, 94, 0.06);
}
.tag-diff {
  border: 1px solid rgba(193, 123, 96, 0.25);
  color: var(--spot);
  background: rgba(193, 123, 96, 0.06);
}

/* Game panel */
.game-panel {
  padding: 1rem;
}
.play-head {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}
.play-head > div { flex: 1; text-align: center; }
.play-head p {
  margin: 0 0 2px;
  color: var(--accent);
  font-size: 0.72rem;
}
.play-head h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.2rem;
  color: var(--text-primary);
}
.play-goal {
  border: 1px solid rgba(92, 122, 94, 0.28);
  border-radius: var(--radius-full);
  padding: 4px 10px;
  color: var(--accent);
  background: rgba(92, 122, 94, 0.06);
  font-size: 0.72rem;
  white-space: nowrap;
}
.back-box {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  padding: 5px 10px;
  font-family: var(--font-body);
  font-size: 0.74rem;
  cursor: pointer;
}
.back-box:hover { border-color: var(--accent); color: var(--accent); }

/* Responsive */
@media (max-width: 860px) {
  .game-layout {
    grid-template-columns: 1fr;
  }
  .game-grid {
    grid-template-columns: 1fr;
  }
  .game-header {
    flex-wrap: wrap;
    gap: 0.5rem;
  }
  .data-badge {
    order: 3;
  }
}
</style>
