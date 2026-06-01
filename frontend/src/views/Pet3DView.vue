<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePets } from '../composables/usePets'
import { useMoments } from '../composables/useMoments'
import ThreePet from '../components/pet/ThreePet.vue'
import Pet3DActionTabs from '../components/pet/Pet3DActionTabs.vue'
import ThemedIcon from '../components/common/ThemedIcon.vue'
import { updatePetModelConfig, uploadFile } from '../api/pet'
import { getAgentTasks } from '../api/agent'
import { getPetPhotos } from '../api/moment'

const route = useRoute()
const router = useRouter()
const petId = Number(route.params.id)
const { getPet } = usePets()
const { records, fetchTimeline } = useMoments()

const pet = ref(null)
const loading = ref(true)
const loadError = ref('')
const interactions = ref(0)
const feedingCount = ref(0)
const playingCount = ref(0)
const recentLabel = ref('')
const pet3d = ref(null)
const modelSaving = ref(false)
const modelMessage = ref('')
const selectedTaskId = ref('')
const agentMessage = ref('')
const agentTasks = ref([])
const tasksLoading = ref(false)
const laserMode = ref(false)
const laserCatches = ref(0)
const albumPhotoUrls = ref([])

const speciesColors = { '猫': '#E8A359', '狗': '#C49A6C', '兔子': '#E8D5C0', '仓鼠': '#D4B896' }
const petColor = computed(() => speciesColors[pet.value?.species] || '#E8A359')
const avatarUrl = computed(() => pet.value?.avatarUrl || '')
const modelConfig = computed(() => {
  if (!pet.value?.modelConfig) return null
  try {
    return typeof pet.value.modelConfig === 'string'
      ? JSON.parse(pet.value.modelConfig)
      : pet.value.modelConfig
  } catch {
    return null
  }
})
const modelKey = computed(() => `${pet.value?.id || 'pet'}-${pet.value?.modelUpdatedAt || 'default'}`)

onMounted(async () => {
  try {
    pet.value = await getPet(petId)
    await Promise.all([fetchTimeline(petId), fetchTasks(), fetchAlbumPhotos()])
    if (records.value.length > 0) {
      recentLabel.value = records.value[0].milestoneLabel || '日常'
    }
  } catch (e) {
    loadError.value = e?.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
})

async function fetchAlbumPhotos() {
  try {
    const res = await getPetPhotos(petId, { page: 1, size: 3 })
    albumPhotoUrls.value = (res.data.records || []).map(p => p.photoUrl).filter(Boolean)
  } catch {
    albumPhotoUrls.value = []
  }
}

async function fetchTasks() {
  tasksLoading.value = true
  try {
    const res = await getAgentTasks(petId)
    agentTasks.value = res.data || []
    if (agentTasks.value.length > 0 && !selectedTaskId.value) {
      selectedTaskId.value = agentTasks.value[0].id
    }
  } catch {
    agentTasks.value = []
  } finally {
    tasksLoading.value = false
  }
}

function onInteraction(type) {
  interactions.value++
}

function feed() {
  feedingCount.value++
  pet3d.value?.feed()
}

function play() {
  playingCount.value++
  pet3d.value?.play()
}

function goBack() { router.push('/timeline') }

function rgbToHex({ r, g, b }) {
  return '#' + [r, g, b].map(v => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, '0')).join('')
}

function rgbToHsl({ r, g, b }) {
  r /= 255; g /= 255; b /= 255
  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  let h = 0
  let s = 0
  const l = (max + min) / 2
  if (max !== min) {
    const d = max - min
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min)
    if (max === r) h = (g - b) / d + (g < b ? 6 : 0)
    else if (max === g) h = (b - r) / d + 2
    else h = (r - g) / d + 4
    h /= 6
  }
  return { h: h * 360, s, l }
}

function naturalizeCatColor(avg, warmRatio, darkRatio, whiteRatio) {
  const hsl = rgbToHsl(avg)
  const unnatural = hsl.s > 0.32 && hsl.h > 75 && hsl.h < 270
  if (whiteRatio > 0.38) {
    return { r: 226, g: 218, b: 199 }
  }
  if (unnatural) {
    if (darkRatio > 0.28) return { r: 92, g: 84, b: 74 }
    if (warmRatio > 0.18) return { r: 174, g: 134, b: 86 }
    return { r: 166, g: 150, b: 128 }
  }
  return {
    r: avg.r * 0.88 + 232 * 0.12,
    g: avg.g * 0.88 + 210 * 0.12,
    b: avg.b * 0.88 + 178 * 0.12
  }
}

function analyzeCatPhoto(file) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const size = 80
      canvas.width = size
      canvas.height = size
      const ctx = canvas.getContext('2d', { willReadFrequently: true })
      ctx.drawImage(img, 0, 0, size, size)
      const data = ctx.getImageData(0, 0, size, size).data
      let r = 0, g = 0, b = 0, weightSum = 0, dark = 0, warm = 0, white = 0
      for (let i = 0; i < data.length; i += 4) {
        const alpha = data[i + 3]
        if (alpha < 80) continue
        const cr = data[i], cg = data[i + 1], cb = data[i + 2]
        const brightness = (cr + cg + cb) / 3
        if (brightness > 242 || brightness < 18) continue
        const px = (i / 4) % size
        const py = Math.floor((i / 4) / size)
        const dx = (px - size / 2) / (size / 2)
        const dy = (py - size / 2) / (size / 2)
        const centerWeight = Math.max(0.08, 1 - Math.sqrt(dx * dx + dy * dy))
        const greenBackground = cg > cr * 1.08 && cg > cb * 1.12 && brightness > 75
        const skyOrBlue = cb > cr * 1.12 && cb > cg * 1.05
        const w = centerWeight * (greenBackground || skyOrBlue ? 0.12 : 1)
        r += cr * w; g += cg * w; b += cb * w; weightSum += w
        if (brightness < 95) dark += w
        if (cr > cg && cg > cb) warm += w
        if (brightness > 195 && Math.abs(cr - cg) < 28 && Math.abs(cg - cb) < 35) white += w
      }
      if (!weightSum) {
        resolve({ baseColor: petColor.value, accentColor: '#9A6A45', pattern: 'soft' })
        return
      }
      const avg = { r: r / weightSum, g: g / weightSum, b: b / weightSum }
      const base = naturalizeCatColor(avg, warm / weightSum, dark / weightSum, white / weightSum)
      const accent = {
        r: base.r * 0.58,
        g: base.g * 0.52,
        b: base.b * 0.46
      }
      let pattern = 'soft'
      if (dark / weightSum > 0.26 && white / weightSum > 0.22) pattern = 'tuxedo'
      else if (warm / weightSum > 0.2 || dark / weightSum > 0.16) pattern = 'tabby'
      resolve({
        baseColor: rgbToHex(base),
        accentColor: rgbToHex(accent),
        pattern,
        whiteMarkings: white / weightSum > 0.16
      })
    }
    img.onerror = () => reject(new Error('照片读取失败'))
    img.src = URL.createObjectURL(file)
  })
}

async function onModelPhotoChange(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file || !pet.value) return
  modelSaving.value = true
  modelMessage.value = '正在根据照片生成 3D 外观...'
  try {
    const appearance = await analyzeCatPhoto(file)
    const uploaded = await uploadFile(file)
    const nextConfig = {
      ...appearance,
      sourcePhotoUrl: uploaded.data.url,
      generatedFrom: 'photo',
      generatedAt: new Date().toISOString()
    }
    const res = await updatePetModelConfig(petId, JSON.stringify(nextConfig))
    pet.value = res.data
    modelMessage.value = '已保存新的 3D 猫外观'
  } catch (e2) {
    modelMessage.value = e2?.response?.data?.message || e2.message || '生成失败'
  } finally {
    modelSaving.value = false
  }
}

const stats = computed(() => {
  if (!records.value.length) return { total: 0, milestones: 0, thisMonth: 0 }
  const now = new Date()
  return {
    total: records.value.length,
    milestones: records.value.filter(r => r.milestoneLabel).length,
    thisMonth: records.value.filter(r => {
      const d = new Date(r.occurredAt)
      return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear()
    }).length
  }
})

function toggleLaser() {
  if (laserMode.value) {
    pet3d.value?.stopLaser()
    laserMode.value = false
    agentMessage.value = ''
  } else {
    pet3d.value?.startLaser()
    laserMode.value = true
    laserCatches.value = 0
    agentMessage.value = '逗猫模式：点击地面移动红点，引导猫咪追逐！'
  }
}

function onLaserCatch(count) {
  laserCatches.value = count
  agentMessage.value = `追逐成功 ${count}/5，继续引导！`
}

function onLaserComplete() {
  laserMode.value = false
  pet3d.value?.play()
  agentMessage.value = `逗猫训练完成，本次追逐 ${laserCatches.value} 次。结果仅用于当前互动，不会写入成长时间线。`
}

function onSelectTask(taskId) {
  selectedTaskId.value = taskId
}

function onRunTask(task) {
  selectedTaskId.value = task.id
  const routes = {
    health_check: `/pet/${petId}/health`,
    weight_record: `/pet/${petId}/health`,
    diet_record: `/pet/${petId}/diet`,
    add_photo: `/pet/${petId}/album`,
    growth_report: `/pet/${petId}/report`,
    play_interact: `/pet/${petId}/game`,
    daily_pet: `/pet/${petId}/game`,
    record_moment: '/timeline',
    suggest_milestone: '/timeline'
  }
  const target = routes[task.id] || (task.type === 'play' ? `/pet/${petId}/game` : '/timeline')
  router.push(target)
}
</script>

<template>
  <div class="pet3d-page">
    <!-- Header -->
    <div class="p3d-header">
      <button class="back-btn" @click="goBack">
        <ThemedIcon name="paw" :size="12" /> 返回
      </button>
      <h1 class="p3d-title">{{ pet?.name || '3D宠物' }}</h1>
      <span class="pet-badge" v-if="pet">{{ pet.species }}</span>
    </div>

    <!-- Loading -->
    <div class="p3d-loading" v-if="loading">
      <span class="spinner"></span> 加载中...
    </div>

    <!-- Error -->
    <div class="p3d-error" v-else-if="loadError">
      <p>{{ loadError }}</p>
      <button class="back-btn" @click="router.push('/timeline')">返回时间线</button>
    </div>

    <!-- Workspace -->
    <div class="p3d-workspace" v-else>
      <!-- Left: 3D Stage -->
      <div class="p3d-stage">
        <div class="stage-container">
          <ThreePet
            :key="modelKey"
            ref="pet3d"
            :species="pet?.species || '猫'"
            :color="petColor"
            :model-config="modelConfig"
            :photo-url="avatarUrl"
            :pet-name="pet?.name || ''"
            :album-photos="albumPhotoUrls"
            @interaction="onInteraction"
            @laser-catch="onLaserCatch"
            @laser-complete="onLaserComplete"
          />
        </div>
        <p class="p3d-hint" v-if="laserMode">点击地面移动红点，引导猫咪追逐（{{ laserCatches }}/5）</p>
        <p class="p3d-hint" v-else>拖拽旋转 · 滚轮缩放 · 点击宠物抚摸</p>
      </div>

      <!-- Right: Panel -->
      <div class="p3d-panel">
        <!-- Overview -->
        <div class="panel-overview">
          <div class="ov-pet">
            <img v-if="pet?.avatarUrl" :src="pet.avatarUrl" class="ov-avatar" alt="" />
            <div v-else class="ov-avatar-ph"><ThemedIcon name="paw" :size="18" /></div>
            <div class="ov-info">
              <h3>{{ pet?.name }}</h3>
              <p>{{ pet?.species }} · {{ pet?.breed || '未知品种' }}</p>
            </div>
          </div>
          <div class="ov-stats">
            <span class="ov-stat"><b>{{ stats.total }}</b>时刻</span>
            <span class="ov-stat"><b>{{ stats.milestones }}</b>碑</span>
            <span class="ov-stat"><b>{{ stats.thisMonth }}</b>本月</span>
            <span class="ov-stat"><b>{{ interactions }}</b>抚摸</span>
          </div>
        </div>

        <!-- Actions -->
        <div class="panel-actions">
          <button class="act-btn" @click="feed" title="喂食">
            <ThemedIcon name="bowl" :size="14" />
            <span>喂食</span>
            <small v-if="feedingCount">×{{ feedingCount }}</small>
          </button>
          <button class="act-btn" @click="play" title="玩耍">
            <ThemedIcon name="collar" :size="14" />
            <span>玩耍</span>
            <small v-if="playingCount">×{{ playingCount }}</small>
          </button>
          <button class="act-btn" :class="{ active: laserMode }" @click="toggleLaser" title="逗猫">
            <ThemedIcon name="paw" :size="14" />
            <span>{{ laserMode ? '结束逗猫' : '逗猫棒' }}</span>
          </button>
          <label class="act-btn upload" :class="{ disabled: modelSaving }" title="上传照片生成3D外观">
            <ThemedIcon name="image" :size="14" />
            <span>{{ modelSaving ? '生成中...' : '照片建模' }}</span>
            <input type="file" accept="image/*" hidden :disabled="modelSaving" @change="onModelPhotoChange" />
          </label>
        </div>
        <p class="model-msg" v-if="modelMessage">{{ modelMessage }}</p>

        <!-- Tabs -->
        <Pet3DActionTabs
          :pet-id="petId"
          :pet-name="pet?.name || '宠物'"
          :agent-tasks="agentTasks"
          :tasks-loading="tasksLoading"
          :agent-message="agentMessage"
          @select-task="onSelectTask"
          @run-task="onRunTask"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.pet3d-page {
  flex: 1;
  max-width: 1280px;
  margin: 0 auto;
  width: 100%;
  padding: 0.8rem 1.5rem 1.5rem;
}

/* Header */
.p3d-header {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  margin-bottom: 0.8rem;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: 1px solid var(--border-default);
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  font-family: var(--font-body);
  font-size: 0.78rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast);
}
.back-btn:hover { border-color: var(--accent); color: var(--accent); }
.p3d-title {
  font-family: var(--font-display);
  font-size: 1.3rem;
  color: var(--text-primary);
  margin: 0;
  flex: 1;
}
.pet-badge {
  font-size: 0.72rem;
  background: var(--spot);
  color: #fff;
  padding: 2px 12px;
  border-radius: var(--radius-full);
  font-weight: 600;
}

/* Loading & Error */
.p3d-loading, .p3d-error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 6rem 0;
  color: var(--text-muted);
  font-size: 0.92rem;
  flex-direction: column;
}
.p3d-error { color: var(--spot); }
.p3d-error .back-btn { margin-top: 1rem; }
.spinner {
  width: 18px; height: 18px;
  border: 2px solid var(--border-default);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Workspace Grid */
.p3d-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
  align-items: start;
}

/* Left: 3D Stage */
.p3d-stage {
  position: sticky;
  top: 72px;
  min-width: 0;
}
.stage-container {
  height: calc(100vh - 160px);
  min-height: 480px;
  max-height: 680px;
}
.p3d-hint {
  text-align: center;
  font-size: 0.7rem;
  color: var(--text-muted);
  margin: 6px 0 0;
}

/* Right: Panel */
.p3d-panel {
  height: calc(100vh - 160px);
  min-height: 480px;
  max-height: 680px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: hidden;
}

/* Overview */
.panel-overview {
  flex-shrink: 0;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 10px 12px;
}
.ov-pet {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.ov-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border-subtle);
}
.ov-avatar-ph {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--bg-input);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}
.ov-info { min-width: 0; }
.ov-info h3 {
  font-family: var(--font-display);
  font-size: 0.95rem;
  color: var(--text-primary);
  margin: 0 0 1px;
}
.ov-info p {
  font-size: 0.7rem;
  color: var(--text-muted);
  margin: 0;
}
.ov-stats {
  display: flex;
  gap: 4px;
}
.ov-stat {
  flex: 1;
  text-align: center;
  padding: 3px 0;
  border: 1px solid rgba(139, 115, 85, 0.12);
  border-radius: 6px;
  background: rgba(255, 250, 241, 0.5);
  font-size: 0.62rem;
  color: var(--text-muted);
  line-height: 1.3;
}
.ov-stat b {
  display: block;
  font-family: var(--font-display);
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--accent);
}

/* Actions */
.panel-actions {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}
.act-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 7px 2px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-family: var(--font-body);
  font-size: 0.68rem;
  cursor: pointer;
  transition: all var(--duration-fast);
}
.act-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-surface);
}
.act-btn span { font-weight: 600; }
.act-btn small { font-size: 0.6rem; color: var(--text-muted); }
.act-btn.upload {
  border-color: rgba(92, 122, 94, 0.3);
  color: var(--accent);
}
.act-btn.upload.disabled {
  opacity: 0.5;
  cursor: wait;
}
.act-btn.active {
  border-color: var(--spot);
  background: var(--spot);
  color: #fff;
}
.model-msg {
  flex-shrink: 0;
  margin: -4px 0 0;
  font-size: 0.66rem;
  color: var(--text-muted);
  line-height: 1.4;
}

/* Responsive: Mobile */
@media (max-width: 860px) {
  .p3d-workspace {
    grid-template-columns: 1fr;
  }
  .p3d-stage {
    position: static;
  }
  .stage-container {
    height: 380px;
    min-height: 320px;
    max-height: 420px;
  }
  .p3d-panel {
    height: auto;
    max-height: none;
    min-height: 0;
  }
  .panel-actions {
    grid-template-columns: repeat(4, 1fr);
  }
}
</style>
