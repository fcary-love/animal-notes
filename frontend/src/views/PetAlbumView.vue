<script setup>
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePets } from '../composables/usePets'
import { getPetPhotos } from '../api/moment'
import ThemedIcon from '../components/common/ThemedIcon.vue'
import AlbumFilterBar from '../components/album/AlbumFilterBar.vue'
import AlbumTimeline from '../components/album/AlbumTimeline.vue'

const route = useRoute()
const router = useRouter()
const petId = Number(route.params.id)
const { getPet } = usePets()

const speciesEmoji = { cat: '🐱', dog: '🐶', rabbit: '🐰', bird: '🐦', hamster: '🐹', other: '🐾' }

const pet = ref(null)
const loading = ref(true)
const photos = ref([])

// filters
const viewMode = ref('timeline')
const milestoneOnly = ref(false)
const filterMonth = ref('')
const filterTag = ref('')

// lightbox
const previewPhoto = ref(null)

// --- Data fetching ---

async function fetchAllPhotos() {
  let all = []
  let page = 1
  const size = 100
  let total = Infinity
  while (all.length < total) {
    const res = await getPetPhotos(petId, { milestoneOnly: milestoneOnly.value, page, size })
    const data = res.data
    total = data.total
    all.push(...data.records.map(p => reactive({ ...p, _loaded: false, _error: false })))
    if (data.records.length < size) break
    page++
  }
  photos.value = all
}

onMounted(async () => {
  try {
    const [petData] = await Promise.all([getPet(petId), fetchAllPhotos()])
    pet.value = petData
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

// --- Watch milestone filter ---
let prevMilestone = milestoneOnly.value
const checkMilestone = setInterval(() => {
  if (milestoneOnly.value !== prevMilestone) {
    prevMilestone = milestoneOnly.value
    loading.value = true
    fetchAllPhotos().finally(() => { loading.value = false })
  }
}, 200)
onUnmounted(() => clearInterval(checkMilestone))

// --- Computed ---

const totalCount = computed(() => photos.value.length)
const milestoneCount = computed(() => photos.value.filter(p => p.isMilestone).length)

const monthCovered = computed(() => {
  const set = new Set()
  for (const p of photos.value) {
    if (p.occurredAt) set.add(p.occurredAt.substring(0, 7))
  }
  return set.size
})

const availableMonths = computed(() => {
  const set = new Set()
  for (const p of photos.value) {
    if (p.occurredAt) set.add(p.occurredAt.substring(0, 7))
  }
  return [...set].sort().reverse().map(v => {
    const [y, m] = v.split('-')
    return { value: v, label: `${y}年${m}月` }
  })
})

const availableTags = computed(() => {
  const set = new Set()
  for (const p of photos.value) {
    if (p.milestoneLabel) set.add(p.milestoneLabel)
  }
  return [...set]
})

const filteredPhotos = computed(() => {
  let list = photos.value
  if (filterMonth.value) {
    list = list.filter(p => p.occurredAt && p.occurredAt.startsWith(filterMonth.value))
  }
  if (filterTag.value) {
    list = list.filter(p => p.milestoneLabel === filterTag.value)
  }
  return list
})

const groupedByTimeline = computed(() => {
  const months = new Map()
  for (const photo of filteredPhotos.value) {
    const monthKey = photo.occurredAt ? photo.occurredAt.substring(0, 7) : 'unknown'
    if (!months.has(monthKey)) months.set(monthKey, new Map())
    const dateKey = photo.occurredAt ? photo.occurredAt.substring(0, 10) : 'unknown'
    if (!months.get(monthKey).has(dateKey)) months.get(monthKey).set(dateKey, [])
    months.get(monthKey).get(dateKey).push(photo)
  }
  const result = []
  for (const [monthKey, dateMap] of months) {
    const dates = []
    let totalCount = 0
    for (const [date, datePhotos] of dateMap) {
      dates.push({
        date,
        photos: datePhotos,
        hasMilestone: datePhotos.some(p => p.isMilestone)
      })
      totalCount += datePhotos.length
    }
    result.push({ monthKey, dates, totalCount })
  }
  return result
})

// --- Actions ---

function goBack() { router.push('/album') }

function openPreview(photo) {
  previewPhoto.value = photo
}

function closePreview() {
  previewPhoto.value = null
}

function onKeydown(e) {
  if (!previewPhoto.value) return
  if (e.key === 'Escape') closePreview()
}
document.addEventListener('keydown', onKeydown)
onUnmounted(() => document.removeEventListener('keydown', onKeydown))

function onImageLoad(photo) {
  photo._loaded = true
}

function onImageError(photo) {
  photo._error = true
  photo._loaded = true
}

function formatDate(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}.${m}.${day}`
}
</script>

<template>
  <div class="album-page">
    <!-- Breadcrumb -->
    <div class="breadcrumb">
      <span class="crumb" @click="goBack">成长相册</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">{{ pet?.name || '...' }}</span>
    </div>

    <!-- Header -->
    <div class="album-header" v-if="pet">
      <div class="pet-avatar-wrap">
        <img v-if="pet.avatarUrl" :src="pet.avatarUrl" class="pet-avatar" />
        <span v-else class="pet-avatar-fallback">{{ speciesEmoji[pet.species] || '🐾' }}</span>
      </div>
      <div class="pet-info">
        <h1 class="pet-name">{{ pet.name }}的成长相册</h1>
        <div class="pet-stats">
          <span class="stat">
            <ThemedIcon name="image" :size="12" />
            {{ totalCount }}张照片
          </span>
          <span class="stat-dot">·</span>
          <span class="stat">
            <ThemedIcon name="flag" :size="12" />
            {{ milestoneCount }}个里程碑
          </span>
          <span class="stat-dot">·</span>
          <span class="stat">跨越{{ monthCovered }}个月</span>
        </div>
      </div>
    </div>

    <!-- Filter Bar -->
    <AlbumFilterBar
      v-model:viewMode="viewMode"
      v-model:milestoneOnly="milestoneOnly"
      v-model:filterMonth="filterMonth"
      v-model:filterTag="filterTag"
      :months="availableMonths"
      :tags="availableTags"
    />

    <!-- Loading -->
    <div class="album-loading" v-if="loading">
      <span class="loading-dot">·</span>
      <span class="loading-dot">·</span>
      <span class="loading-dot">·</span>
    </div>

    <template v-else>
      <!-- Timeline View -->
      <template v-if="viewMode === 'timeline'">
        <AlbumTimeline
          v-if="groupedByTimeline.length > 0"
          :groupedPhotos="groupedByTimeline"
          :pet="pet"
          @photo-click="openPreview"
        />
        <div class="empty-state" v-else>
          <ThemedIcon name="images" :size="40" class="empty-icon" />
          <p class="empty-title" v-if="filterMonth || filterTag || milestoneOnly">没有找到匹配的照片</p>
          <p class="empty-title" v-else>还没有照片</p>
          <p class="empty-desc" v-if="filterMonth || filterTag || milestoneOnly">
            试试调整筛选条件，看看其他瞬间。
          </p>
          <p class="empty-desc" v-else>
            在时间线中为时刻添加照片，记录宠物成长的每个瞬间。
          </p>
          <button class="empty-action" @click="router.push('/timeline')">
            <ThemedIcon name="scroll" :size="14" />
            去时间线记录
          </button>
        </div>
      </template>

      <!-- Wall View -->
      <template v-if="viewMode === 'wall'">
        <div class="photo-wall" v-if="filteredPhotos.length > 0">
          <div
            class="wall-polaroid"
            v-for="(photo, i) in filteredPhotos"
            :key="photo.momentId + '-' + i"
            @click="openPreview(photo)"
          >
            <div class="wall-frame">
              <div class="img-placeholder" v-if="!photo._loaded">
                <ThemedIcon name="image" :size="20" />
              </div>
              <img
                :src="photo.photoUrl"
                :alt="photo.content || '照片'"
                loading="lazy"
                @load="onImageLoad(photo)"
                @error="onImageError(photo)"
                :style="{ opacity: photo._loaded ? 1 : 0 }"
              />
              <div class="wall-flag" v-if="photo.isMilestone">
                <ThemedIcon name="flag" :size="10" />
              </div>
            </div>
            <div class="wall-caption">
              <span class="wall-date">{{ formatDate(photo.occurredAt) }}</span>
              <span class="wall-label" v-if="photo.milestoneLabel">{{ photo.milestoneLabel }}</span>
            </div>
            <p class="wall-text" v-if="photo.content">{{ photo.content }}</p>
          </div>
        </div>
        <div class="empty-state" v-else>
          <ThemedIcon name="images" :size="40" class="empty-icon" />
          <p class="empty-title">没有找到匹配的照片</p>
          <p class="empty-desc">试试调整筛选条件。</p>
        </div>
      </template>
    </template>

    <!-- Photo Preview Modal -->
    <Teleport to="body">
      <Transition name="preview">
        <div v-if="previewPhoto" class="preview-overlay" @click.self="closePreview">
          <button class="preview-close" @click="closePreview">&times;</button>
          <div class="preview-content">
            <div class="preview-polaroid">
              <img :src="previewPhoto.photoUrl" class="preview-img" />
            </div>
            <div class="preview-info">
              <div class="preview-meta">
                <span class="preview-date">{{ formatDate(previewPhoto.occurredAt) }}</span>
                <span class="preview-milestone" v-if="previewPhoto.isMilestone">
                  <ThemedIcon name="flag" :size="12" />
                  {{ previewPhoto.milestoneLabel || '里程碑' }}
                </span>
                <span class="preview-location" v-if="previewPhoto.location">
                  {{ previewPhoto.location }}
                </span>
              </div>
              <p class="preview-desc" v-if="previewPhoto.content">{{ previewPhoto.content }}</p>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.album-page {
  flex: 1;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
  padding: 1.5rem 2rem 3rem;
}

/* --- Breadcrumb --- */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 1rem;
  font-size: 0.82rem;
}
.crumb { color: var(--text-muted); cursor: pointer; transition: color var(--duration-fast); }
.crumb:hover { color: var(--accent); }
.crumb-sep { color: var(--border-default); }
.crumb-current { color: var(--text-secondary); font-weight: 600; }

/* --- Header --- */
.album-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 1.25rem;
}
.pet-avatar-wrap {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-warm);
  flex-shrink: 0;
}
.pet-avatar { width: 100%; height: 100%; object-fit: cover; }
.pet-avatar-fallback { font-size: 1.4rem; }
.pet-info { flex: 1; }
.pet-name {
  font-family: var(--font-display);
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px;
}
.pet-stats {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.stat {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 0.78rem;
  color: var(--text-muted);
}
.stat-dot {
  color: var(--border-default);
  font-size: 0.6rem;
}

/* --- Loading --- */
.album-loading {
  text-align: center;
  padding: 4rem 0;
  color: var(--text-muted);
  font-size: 1.5rem;
  letter-spacing: 0.5em;
}
.loading-dot { animation: blink 1.2s infinite; }
.loading-dot:nth-child(2) { animation-delay: 0.2s; }
.loading-dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 60%, 100% { opacity: 0.2; }
  30% { opacity: 1; }
}

/* --- Photo Wall View --- */
.photo-wall {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.wall-polaroid {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: 3px;
  padding: 8px 8px 0 8px;
  cursor: pointer;
  box-shadow: var(--shadow-card);
  transition: all var(--duration-normal) var(--ease-out);
}

.wall-polaroid:hover {
  transform: translateY(-3px) rotate(-0.5deg);
  box-shadow: var(--shadow-elevated);
}

.wall-frame {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border-radius: 2px;
  background: var(--bg-warm);
  margin-bottom: 6px;
}

.wall-frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: opacity 0.3s ease;
}

.img-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  opacity: 0.4;
}

.wall-flag {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 22px;
  height: 22px;
  border-radius: var(--radius-full);
  background: var(--spot);
  color: var(--text-inverse);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 4px rgba(0,0,0,0.15);
}

.wall-caption {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 2px 4px;
}

.wall-date {
  font-size: 0.72rem;
  color: var(--text-muted);
  font-family: var(--font-display);
}

.wall-label {
  font-size: 0.65rem;
  padding: 1px 6px;
  border-radius: var(--radius-full);
  background: var(--spot-glow);
  color: var(--spot);
  font-weight: 600;
}

.wall-text {
  font-size: 0.78rem;
  color: var(--text-secondary);
  line-height: 1.5;
  margin: 0 2px 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* --- Empty State --- */
.empty-state {
  text-align: center;
  padding: 3rem;
}
.empty-icon { color: var(--accent-glow); margin-bottom: 0.75rem; }
.empty-title {
  font-family: var(--font-display);
  font-size: 1.05rem;
  color: var(--text-primary);
  margin: 0 0 0.5rem;
}
.empty-desc {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin: 0 0 1.5rem;
}
.empty-action {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 20px;
  border: 1px solid var(--accent);
  background: transparent;
  color: var(--accent);
  font-family: var(--font-body);
  font-size: 0.9rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--duration-fast);
}
.empty-action:hover { background: var(--accent); color: var(--text-inverse); }

/* --- Preview Modal --- */
.preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(30, 25, 20, 0.92);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.preview-close {
  position: absolute;
  top: 16px;
  right: 20px;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  font-size: 2rem;
  cursor: pointer;
  z-index: 10;
  line-height: 1;
  padding: 4px 8px;
  transition: color var(--duration-fast);
}
.preview-close:hover { color: #fff; }

.preview-content {
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.preview-polaroid {
  background: var(--bg-surface);
  padding: 10px;
  border-radius: 3px;
  box-shadow: var(--shadow-elevated);
  max-width: 85vw;
}

.preview-img {
  max-width: 80vw;
  max-height: 65vh;
  object-fit: contain;
  display: block;
  border-radius: 2px;
}

.preview-info {
  text-align: center;
  max-width: 600px;
}

.preview-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.preview-date {
  font-size: 0.82rem;
  color: rgba(255, 255, 255, 0.6);
  font-family: var(--font-display);
}

.preview-milestone {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 0.78rem;
  color: var(--spot-glow);
}

.preview-location {
  font-size: 0.78rem;
  color: rgba(255, 255, 255, 0.5);
}

.preview-desc {
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.7;
  margin: 0;
}

/* Preview animation */
.preview-enter-active { transition: opacity 0.2s ease; }
.preview-leave-active { transition: opacity 0.15s ease; }
.preview-enter-from, .preview-leave-to { opacity: 0; }

/* --- Responsive --- */
@media (max-width: 640px) {
  .album-page { padding: 1rem; }
  .photo-wall { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .preview-polaroid { padding: 6px; }
  .preview-img { max-width: 95vw; max-height: 55vh; }
  .preview-meta { gap: 8px; }
}
</style>
