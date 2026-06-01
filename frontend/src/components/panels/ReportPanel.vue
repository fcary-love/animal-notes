<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { usePets } from '../../composables/usePets'
import { getDiets, getHealthEvents, getHealthSummary, getWeights } from '../../api/health'
import { getTimeline } from '../../api/moment'
import ThemedIcon from '../common/ThemedIcon.vue'

const router = useRouter()
const { pets, currentPet, fetchPets } = usePets()
const loaded = ref(false)
const weights = ref([])
const diets = ref([])
const healthEvents = ref([])
const moments = ref([])
const summary = ref(null)

const latestWeight = computed(() => Number(weights.value[0]?.weight || weights.value[weights.value.length - 1]?.weight || 0))
const prevWeight = computed(() => Number(weights.value[1]?.weight || latestWeight.value))
const weightDiff = computed(() => (latestWeight.value - prevWeight.value).toFixed(1))
const todayCalories = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return diets.value.filter(d => d.recordedAt?.startsWith(today)).reduce((sum, d) => sum + (Number(d.calories) || 0), 0)
})
const reportNote = computed(() => {
  const notes = []
  if (latestWeight.value) notes.push(`最近体重 ${latestWeight.value}kg`)
  if (todayCalories.value) notes.push(`今日饮食约 ${todayCalories.value}kcal`)
  if (summary.value?.nextVaccineDate) notes.push(`下次疫苗 ${summary.value.nextVaccineDate}`)
  return notes.length ? notes.join('，') : '暂无足够数据，继续记录后会生成更完整的观察摘要。'
})

const chartW = 340
const chartH = 140
const padX = 20
const padY = 16
const chartData = computed(() => [...weights.value].reverse().slice(-8))
const minW = computed(() => chartData.value.length ? Math.min(...chartData.value.map(d => Number(d.weight))) - 0.3 : 0)
const maxW = computed(() => chartData.value.length ? Math.max(...chartData.value.map(d => Number(d.weight))) + 0.3 : 1)
const rangeW = computed(() => Math.max(0.1, maxW.value - minW.value))

function getX(i) {
  if (chartData.value.length <= 1) return chartW / 2
  return padX + (i / (chartData.value.length - 1)) * (chartW - padX * 2)
}
function getY(w) {
  return padY + (1 - (Number(w) - minW.value) / rangeW.value) * (chartH - padY * 2)
}
const linePath = computed(() => {
  const pts = chartData.value.map((d, i) => ({ x: getX(i), y: getY(d.weight) }))
  if (pts.length < 2) return ''
  return pts.reduce((path, p, i) => `${path}${i === 0 ? 'M' : ' L'} ${p.x} ${p.y}`, '')
})
const dotPositions = computed(() => chartData.value.map((d, i) => ({ x: getX(i), y: getY(d.weight), ...d })))

async function loadReport() {
  const pet = currentPet.value || pets.value[0]
  if (!pet) {
    loaded.value = true
    return
  }
  loaded.value = false
  const [w, d, h, s, m] = await Promise.all([
    getWeights(pet.id, 8).then(res => res.data || []).catch(() => []),
    getDiets(pet.id, 30).then(res => res.data || []).catch(() => []),
    getHealthEvents(pet.id, { page: 1, size: 20 }).then(res => res.data?.records || res.data || []).catch(() => []),
    getHealthSummary(pet.id).then(res => res.data || null).catch(() => null),
    getTimeline(pet.id, { sort: 'desc', page: 1, size: 30 }).then(res => res.data?.records || []).catch(() => [])
  ])
  weights.value = w
  diets.value = d
  healthEvents.value = h
  summary.value = s
  moments.value = m
  loaded.value = true
}

function goFullReport() {
  const pet = currentPet.value || pets.value[0]
  if (pet) router.push(`/pet/${pet.id}/report`)
}

onMounted(async () => {
  if (pets.value.length === 0) await fetchPets()
  await loadReport()
})
watch(currentPet, loadReport)
</script>

<template>
  <div class="panel-content" :class="{ loaded }">
    <section class="rpt-section">
      <div class="section-label">体重趋势</div>
      <div class="rpt-chart-card">
        <div class="rpt-chart-head">
          <div class="rpt-weight-now">
            <span class="rpt-weight-val">{{ latestWeight || '--' }}</span>
            <span class="rpt-weight-unit">kg</span>
          </div>
          <div class="rpt-weight-diff" :class="{ up: weightDiff > 0, down: weightDiff < 0 }">
            {{ latestWeight ? `${weightDiff > 0 ? '+' : ''}${weightDiff}kg` : '暂无' }}
          </div>
        </div>
        <svg v-if="chartData.length >= 2" :viewBox="`0 0 ${chartW} ${chartH}`" class="rpt-chart" preserveAspectRatio="xMidYMid meet">
          <line v-for="i in 3" :key="i" :x1="padX" :x2="chartW - padX" :y1="padY + (i - 1) * ((chartH - padY * 2) / 2)" :y2="padY + (i - 1) * ((chartH - padY * 2) / 2)" stroke="var(--border-subtle)" stroke-width="0.5" stroke-dasharray="4 4" opacity="0.5" />
          <path :d="linePath" fill="none" stroke="var(--accent)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
          <circle v-for="(d, i) in dotPositions" :key="i" :cx="d.x" :cy="d.y" r="3.5" fill="var(--bg-surface)" stroke="var(--accent)" stroke-width="2" />
        </svg>
        <p v-else class="empty-note">记录两次以上体重后会显示趋势线。</p>
      </div>
    </section>

    <section class="rpt-section">
      <div class="section-label">真实数据概览</div>
      <div class="rpt-stats">
        <div class="rpt-stat"><ThemedIcon name="bowl" :size="20" /><span class="rpt-stat-val">{{ diets.length }}</span><span class="rpt-stat-label">饮食记录</span></div>
        <div class="rpt-stat"><ThemedIcon name="stethoscope" :size="20" /><span class="rpt-stat-val">{{ healthEvents.length }}</span><span class="rpt-stat-label">健康事件</span></div>
        <div class="rpt-stat"><ThemedIcon name="quill" :size="20" /><span class="rpt-stat-val">{{ moments.length }}</span><span class="rpt-stat-label">时刻记录</span></div>
        <div class="rpt-stat"><ThemedIcon name="trend" :size="20" /><span class="rpt-stat-val">{{ weights.length }}</span><span class="rpt-stat-label">体重记录</span></div>
      </div>
    </section>

    <section class="rpt-section">
      <div class="section-label">观察摘要</div>
      <div class="rpt-note-card">
        <ThemedIcon name="book" :size="22" />
        <p>{{ reportNote }}</p>
      </div>
    </section>

    <button class="panel-link" @click="goFullReport" :disabled="!currentPet">打开完整报告</button>
  </div>
</template>

<style scoped>
.panel-content { opacity: 0; transform: translateY(12px); transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1); }
.panel-content.loaded { opacity: 1; transform: translateY(0); }
.rpt-section { margin-bottom: 22px; }
.section-label { font-family: var(--font-display); font-size: 0.78rem; color: var(--text-muted); margin-bottom: 10px; }
.rpt-chart-card { background: var(--bg-warm); border: 1px solid var(--border-subtle); border-radius: var(--radius-lg); padding: 16px 16px 8px; }
.rpt-chart-head { display: flex; align-items: baseline; gap: 10px; margin-bottom: 12px; }
.rpt-weight-now { display: flex; align-items: baseline; gap: 3px; }
.rpt-weight-val { font-family: var(--font-display); font-size: 1.6rem; font-weight: 700; color: var(--text-primary); line-height: 1; }
.rpt-weight-unit { font-size: 0.75rem; color: var(--text-muted); }
.rpt-weight-diff { font-family: var(--font-mono); font-size: 0.72rem; padding: 2px 8px; border-radius: var(--radius-full); color: var(--text-muted); background: rgba(92, 122, 94, 0.08); }
.rpt-weight-diff.up { color: var(--spot); background: rgba(193, 123, 96, 0.08); }
.rpt-weight-diff.down { color: var(--accent); background: rgba(92, 122, 94, 0.08); }
.rpt-chart { width: 100%; height: auto; display: block; }
.empty-note { margin: 0; color: var(--text-muted); font-size: 0.78rem; text-align: center; padding: 24px 0; }
.rpt-stats { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.rpt-stat {
  display: flex; flex-direction: column; align-items: center; gap: 5px; padding: 14px 8px;
  background: var(--bg-warm); border: 1px solid var(--border-subtle); border-radius: var(--radius-lg); color: var(--accent);
}
.rpt-stat-val { font-family: var(--font-display); font-size: 1.2rem; font-weight: 700; color: var(--text-primary); line-height: 1; }
.rpt-stat-label { font-size: 0.68rem; color: var(--text-muted); }
.rpt-note-card {
  display: flex; gap: 12px; padding: 16px; background: linear-gradient(135deg, rgba(255, 254, 249, 0.8), rgba(245, 239, 224, 0.6));
  border: 1px solid var(--border-subtle); border-radius: var(--radius-lg); color: var(--accent);
}
.rpt-note-card p { margin: 0; font-size: 0.78rem; color: var(--text-secondary); line-height: 1.7; }
.panel-link { width: 100%; padding: 10px; border: 1px solid var(--accent); border-radius: var(--radius-full); background: transparent; color: var(--accent); font-family: var(--font-body); cursor: pointer; }
.panel-link:hover:not(:disabled) { background: var(--accent); color: #fff; }
.panel-link:disabled { opacity: 0.45; cursor: not-allowed; }
</style>
