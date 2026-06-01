<script setup>
import { ref } from 'vue'
import CatQuestPanel from '../CatQuestPanel.vue'

const props = defineProps({
  petName: { type: String, default: '猫咪' },
  pet: { type: Object, default: null }
})
const emit = defineEmits(['complete', 'back'])

const started = ref(false)
const gameKey = ref(0)

const task = {
  id: 'catch',
  title: '小鱼干接力',
  goal: 60,
  reason: '接住小鱼干和玩具，躲开纸箱，连续接到触发连击加分。'
}

function start() {
  started.value = true
  gameKey.value++
}

function onComplete(result) {
  emit('complete', result)
}

function restart() {
  started.value = false
  setTimeout(() => start(), 50)
}
</script>

<template>
  <div class="catch-game">
    <!-- 说明 overlay -->
    <div v-if="!started" class="game-intro">
      <div class="intro-content">
        <div class="intro-icon">🐟</div>
        <h3>小鱼干接力</h3>
        <p class="intro-desc">接住从上方掉落的小鱼干和玩具，躲开纸箱。连续接到正向道具触发连击加分！</p>
        <div class="intro-rules">
          <div class="rule good"><span>🐟</span> 小鱼干 +10</div>
          <div class="rule good"><span>🎮</span> 玩具 +14</div>
          <div class="rule bad"><span>📦</span> 纸箱 -8</div>
        </div>
        <div class="intro-controls">
          <span>A / D 或方向键左右移动</span>
          <span>30 秒内达到 60 分</span>
        </div>
        <button class="start-btn" @click="start">开始游戏</button>
      </div>
    </div>

    <!-- 游戏主体 -->
    <CatQuestPanel
      v-else
      :key="gameKey"
      :pet-name="petName"
      :task="task"
      mode="catch"
      large
      @complete="onComplete"
    />

    <!-- 结果区 -->
    <div v-if="!started" class="game-footer">
      <button class="back-btn" @click="emit('back')">
        < 返回游戏盒
      </button>
    </div>
  </div>
</template>

<style scoped>
.catch-game {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.game-intro {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 380px;
}
.intro-content {
  text-align: center;
  max-width: 360px;
  padding: 2rem 1.5rem;
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-lg);
  background: rgba(255, 250, 241, 0.6);
}
.intro-icon {
  font-size: 3rem;
  margin-bottom: 0.5rem;
}
.intro-content h3 {
  margin: 0 0 0.5rem;
  font-family: var(--font-display);
  font-size: 1.3rem;
  color: var(--text-primary);
}
.intro-desc {
  margin: 0 0 1rem;
  color: var(--text-muted);
  font-size: 0.82rem;
  line-height: 1.6;
}
.intro-rules {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 1rem;
}
.rule {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.78rem;
}
.rule.good { color: var(--accent); }
.rule.bad { color: var(--spot); }
.intro-controls {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 1.2rem;
  font-size: 0.72rem;
  color: var(--text-muted);
}
.start-btn {
  padding: 10px 32px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: #fff;
  font-family: var(--font-body);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background var(--duration-fast);
}
.start-btn:hover { background: var(--accent-light); }
.game-footer {
  display: flex;
  justify-content: flex-start;
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
