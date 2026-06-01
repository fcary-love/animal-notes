<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePets } from '../composables/usePets'
import { getHealthEvents, createHealthEvent, updateHealthEvent, deleteHealthEvent, getHealthSummary, getWeights, createWeight, deleteWeight } from '../api/health'
import ThemedIcon from '../components/common/ThemedIcon.vue'

const route = useRoute()
const router = useRouter()
const petId = Number(route.params.id)
const { getPet } = usePets()

const pet = ref(null)
const loading = ref(true)
const events = ref([])
const total = ref(0)
const page = ref(1)
const filterType = ref('')
const summary = ref(null)
const showForm = ref(false)
const editingEvent = ref(null)
const formLoading = ref(false)
const formError = ref('')

// 体重相关
const weights = ref([])
const showWeightForm = ref(false)
const weightForm = ref({ weight: '', recordedAt: new Date().toISOString().slice(0, 10), note: '' })
const weightFormLoading = ref(false)
const weightFormError = ref('')

const eventTypes = [
  { value: 'VACCINE', label: '疫苗', icon: 'stethoscope' },
  { value: 'CHECKUP', label: '体检', icon: 'heartPulse' },
  { value: 'DEWORMING', label: '驱虫', icon: 'bell' },
  { value: 'NEUTERING', label: '绝育', icon: 'collar' },
  { value: 'SURGERY', label: '手术', icon: 'flag' },
  { value: 'MEDICATION', label: '用药', icon: 'bowl' },
  { value: 'OTHER', label: '其他', icon: 'bookmark' }
]

const typeLabels = Object.fromEntries(eventTypes.map(t => [t.value, t.label]))

const emptyForm = {
  eventType: 'VACCINE',
  title: '',
  description: '',
  eventDate: '',
  nextDate: '',
  veterinarian: '',
  cost: '',
  status: 'COMPLETED'
}

const form = ref({ ...emptyForm })

// SVG 体重趋势图
const trendPoints = computed(() => {
  if (weights.value.length < 2) return ''
  const sorted = [...weights.value].reverse()
  const vals = sorted.map(w => Number(w.weight))
  const min = Math.min(...vals) - 0.5
  const max = Math.max(...vals) + 0.5
  const range = max - min || 1
  const w = 240
  const h = 60
  const step = w / (sorted.length - 1)
  return sorted.map((item, i) => {
    const x = i * step
    const y = h - ((Number(item.weight) - min) / range) * h
    return `${x},${y}`
  }).join(' ')
})

onMounted(async () => {
  try {
    pet.value = await getPet(petId)
    await Promise.all([fetchEvents(), fetchSummary(), fetchWeights()])
  } catch (e) {
    formError.value = e?.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
})

async function fetchEvents() {
  const res = await getHealthEvents(petId, { eventType: filterType.value || undefined, page: page.value, size: 20 })
  events.value = res.data.records
  total.value = res.data.total
}

async function fetchSummary() {
  try {
    const res = await getHealthSummary(petId)
    summary.value = res.data
  } catch { /* optional */ }
}

async function fetchWeights() {
  try {
    const res = await getWeights(petId, 10)
    weights.value = res.data || []
  } catch { /* optional */ }
}

function onFilterType(type) {
  filterType.value = filterType.value === type ? '' : type
  page.value = 1
  fetchEvents()
}

function openCreate() {
  editingEvent.value = null
  form.value = { ...emptyForm }
  formError.value = ''
  showForm.value = true
}

function openEdit(event) {
  editingEvent.value = event
  form.value = {
    eventType: event.eventType,
    title: event.title,
    description: event.description || '',
    eventDate: event.eventDate || '',
    nextDate: event.nextDate || '',
    veterinarian: event.veterinarian || '',
    cost: event.cost || '',
    status: event.status || 'COMPLETED'
  }
  formError.value = ''
  showForm.value = true
}

async function onSubmit() {
  if (!form.value.title.trim()) { formError.value = '请输入标题'; return }
  if (!form.value.eventDate) { formError.value = '请选择事件日期'; return }
  formLoading.value = true
  formError.value = ''
  try {
    const payload = {
      ...form.value,
      cost: form.value.cost ? Number(form.value.cost) : undefined,
      nextDate: form.value.nextDate || undefined,
      description: form.value.description || undefined,
      veterinarian: form.value.veterinarian || undefined
    }
    if (editingEvent.value) {
      await updateHealthEvent(petId, editingEvent.value.id, payload)
    } else {
      await createHealthEvent(petId, payload)
    }
    showForm.value = false
    await Promise.all([fetchEvents(), fetchSummary()])
  } catch (e) {
    formError.value = e?.response?.data?.message || e.message || '保存失败'
  } finally {
    formLoading.value = false
  }
}

async function onDelete(event) {
  if (!confirm(`确定删除「${event.title}」吗？`)) return
  try {
    await deleteHealthEvent(petId, event.id)
    await Promise.all([fetchEvents(), fetchSummary()])
  } catch { /* handled */ }
}

// 体重相关
async function onWeightSubmit() {
  if (!weightForm.value.weight) { weightFormError.value = '请输入体重'; return }
  if (!weightForm.value.recordedAt) { weightFormError.value = '请选择日期'; return }
  weightFormLoading.value = true
  weightFormError.value = ''
  try {
    await createWeight(petId, {
      weight: Number(weightForm.value.weight),
      recordedAt: weightForm.value.recordedAt,
      note: weightForm.value.note || undefined
    })
    showWeightForm.value = false
    weightForm.value = { weight: '', recordedAt: new Date().toISOString().slice(0, 10), note: '' }
    await Promise.all([fetchWeights(), fetchSummary()])
  } catch (e) {
    weightFormError.value = e?.response?.data?.message || e.message || '记录失败'
  } finally {
    weightFormLoading.value = false
  }
}

async function onDeleteWeight(w) {
  if (!confirm('删除这条体重记录？')) return
  try {
    await deleteWeight(petId, w.id)
    await Promise.all([fetchWeights(), fetchSummary()])
  } catch { /* handled */ }
}

const speciesLabel = { cat: '猫', dog: '狗', rabbit: '兔', bird: '鸟', other: '其他' }
const speciesEmoji = { cat: '🐱', dog: '🐶', rabbit: '🐰', bird: '🐦', other: '🐾' }

function goBack() { router.push('/health') }
</script>

<template>
  <div class="health-page">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="crumb" @click="goBack">健康档案</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">{{ pet?.name || '...' }}</span>
    </div>

    <!-- 宠物头部 -->
    <div class="pet-header" v-if="pet">
      <div class="pet-avatar-wrap">
        <img v-if="pet.avatarUrl" :src="pet.avatarUrl" class="pet-avatar" />
        <span v-else class="pet-avatar-fallback">{{ speciesEmoji[pet.species] || '🐾' }}</span>
      </div>
      <div class="pet-info">
        <h1 class="pet-name">{{ pet.name }}</h1>
        <span class="pet-meta">{{ speciesLabel[pet.species] || pet.species }}<span v-if="pet.breed"> · {{ pet.breed }}</span></span>
      </div>
      <button class="add-btn" @click="openCreate">
        <ThemedIcon name="pawPlus" :size="14" />
        <span>记录事件</span>
      </button>
    </div>

    <div class="health-loading" v-if="loading">
      <span class="loading-dot">·</span>
      <span class="loading-dot">·</span>
      <span class="loading-dot">·</span>
    </div>
    <div class="health-error" v-else-if="formError && !showForm">{{ formError }}</div>

    <template v-else>
      <!-- 摘要卡片 -->
      <div class="summary-section" v-if="summary">
        <div class="summary-grid">
          <div class="sum-card">
            <ThemedIcon name="scroll" :size="18" class="sum-icon" />
            <span class="sum-num">{{ summary.totalEvents }}</span>
            <span class="sum-label">总事件</span>
          </div>
          <div class="sum-card">
            <ThemedIcon name="stethoscope" :size="18" class="sum-icon" />
            <span class="sum-num">{{ summary.vaccineCompleted }}/{{ summary.vaccineCount }}</span>
            <span class="sum-label">疫苗完成</span>
          </div>
          <div class="sum-card sum-card--warn" v-if="summary.nextVaccineDate">
            <ThemedIcon name="bell" :size="18" class="sum-icon" />
            <span class="sum-num date-text">{{ summary.nextVaccineDate }}</span>
            <span class="sum-label">下次疫苗</span>
          </div>
          <div class="sum-card sum-card--warn" v-if="summary.nextDewormingDate">
            <ThemedIcon name="bell" :size="18" class="sum-icon" />
            <span class="sum-num date-text">{{ summary.nextDewormingDate }}</span>
            <span class="sum-label">下次驱虫</span>
          </div>
        </div>
      </div>

      <!-- 体重趋势 -->
      <div class="weight-section">
        <div class="section-head">
          <h2 class="section-title">
            <ThemedIcon name="trend" :size="16" class="section-icon" />
            体重记录
          </h2>
          <button class="link-btn" @click="showWeightForm = !showWeightForm">
            {{ showWeightForm ? '收起' : '+ 记录体重' }}
          </button>
        </div>

        <div class="weight-form-area" v-if="showWeightForm">
          <form @submit.prevent="onWeightSubmit" class="weight-form">
            <div class="wf-row">
              <div class="wf-field">
                <label class="wf-label">体重 (kg)</label>
                <input v-model="weightForm.weight" type="number" step="0.01" min="0" class="wf-input" placeholder="4.5" />
              </div>
              <div class="wf-field">
                <label class="wf-label">日期</label>
                <input v-model="weightForm.recordedAt" type="date" class="wf-input" />
              </div>
              <div class="wf-field wf-field--wide">
                <label class="wf-label">备注</label>
                <input v-model="weightForm.note" class="wf-input" placeholder="可选" maxlength="200" />
              </div>
              <button type="submit" class="wf-submit" :disabled="weightFormLoading">
                {{ weightFormLoading ? '...' : '保存' }}
              </button>
            </div>
            <p class="wf-error" v-if="weightFormError">{{ weightFormError }}</p>
          </form>
        </div>

        <div class="weight-content" v-if="weights.length > 0">
          <!-- SVG 趋势图 -->
          <div class="trend-chart" v-if="weights.length >= 2">
            <svg viewBox="0 0 240 60" class="trend-svg">
              <polyline :points="trendPoints" fill="none" stroke="var(--accent)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
              <circle v-for="(w, i) in [...weights].reverse()" :key="w.id"
                :cx="i * (240 / (weights.length - 1))"
                :cy="60 - ((Number(w.weight) - Math.min(...weights.map(x=>Number(x.weight))) + 0.5) / (Math.max(...weights.map(x=>Number(x.weight))) - Math.min(...weights.map(x=>Number(x.weight))) + 1)) * 60"
                r="2.5" fill="var(--bg-surface)" stroke="var(--accent)" stroke-width="1.2"
              />
            </svg>
          </div>

          <!-- 体重列表 -->
          <div class="weight-list">
            <div class="weight-item" v-for="w in weights" :key="w.id">
              <span class="weight-val">{{ w.weight }} kg</span>
              <span class="weight-date">{{ w.recordedAt }}</span>
              <span class="weight-note" v-if="w.note">{{ w.note }}</span>
              <button class="weight-del" @click="onDeleteWeight(w)" title="删除">&times;</button>
            </div>
          </div>
        </div>
        <div class="weight-empty" v-else-if="!showWeightForm">
          <span class="weight-empty-text">暂无体重记录，点击上方按钮开始记录。</span>
        </div>
      </div>

      <!-- 饮食记录入口 -->
      <div class="diet-entry">
        <div class="section-head">
          <h2 class="section-title">
            <ThemedIcon name="bowl" :size="16" class="section-icon" />
            饮食记录
          </h2>
          <button class="link-btn" @click="router.push(`/pet/${petId}/diet`)">
            查看详情 &rarr;
          </button>
        </div>
        <p class="diet-entry-desc">记录每日饮食，关注营养摄入。</p>
      </div>

      <!-- 筛选 -->
      <div class="filter-bar">
        <button class="filter-btn" :class="{ 'is-active': !filterType }" @click="filterType = ''; page = 1; fetchEvents()">全部</button>
        <button
          v-for="t in eventTypes" :key="t.value"
          class="filter-btn" :class="{ 'is-active': filterType === t.value }"
          @click="onFilterType(t.value)"
        >{{ t.label }}</button>
      </div>

      <!-- 表单 -->
      <div class="form-area" v-if="showForm">
        <h3 class="form-title">{{ editingEvent ? '编辑健康事件' : '新建健康事件' }}</h3>
        <form @submit.prevent="onSubmit" class="health-form">
          <div class="field">
            <label class="field-label">事件类型</label>
            <div class="type-grid">
              <button v-for="t in eventTypes" :key="t.value" type="button"
                class="type-btn" :class="{ 'is-active': form.eventType === t.value }"
                @click="form.eventType = t.value"
              >
                <ThemedIcon :name="t.icon" :size="13" />
                <span>{{ t.label }}</span>
              </button>
            </div>
          </div>
          <div class="field">
            <label class="field-label">标题 *</label>
            <input v-model="form.title" class="field-input" placeholder="如：猫三联第二针" maxlength="100" />
          </div>
          <div class="field-row">
            <div class="field">
              <label class="field-label">事件日期 *</label>
              <input v-model="form.eventDate" type="date" class="field-input" />
            </div>
            <div class="field">
              <label class="field-label">下次日期</label>
              <input v-model="form.nextDate" type="date" class="field-input" />
            </div>
          </div>
          <div class="field-row">
            <div class="field">
              <label class="field-label">兽医/医院</label>
              <input v-model="form.veterinarian" class="field-input" placeholder="如：宠爱动物医院" maxlength="50" />
            </div>
            <div class="field">
              <label class="field-label">费用(元)</label>
              <input v-model="form.cost" type="number" step="0.01" class="field-input" placeholder="0.00" />
            </div>
          </div>
          <div class="field">
            <label class="field-label">描述</label>
            <textarea v-model="form.description" class="field-input field-textarea" rows="3" placeholder="详细描述..." maxlength="2000"></textarea>
          </div>
          <div class="field">
            <label class="field-label">状态</label>
            <select v-model="form.status" class="field-input">
              <option value="COMPLETED">已完成</option>
              <option value="SCHEDULED">已预约</option>
              <option value="CANCELLED">已取消</option>
            </select>
          </div>
          <p class="form-error" v-if="formError">{{ formError }}</p>
          <div class="form-actions">
            <button type="button" class="btn-cancel" @click="showForm = false">取消</button>
            <button type="submit" class="btn-submit" :disabled="formLoading">
              {{ formLoading ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>

      <!-- 事件列表 -->
      <div class="event-list" v-if="events.length > 0">
        <div class="event-item" v-for="e in events" :key="e.id">
          <div class="event-main">
            <div class="event-row">
              <span class="event-type-tag" :class="'type-' + e.eventType.toLowerCase()">{{ typeLabels[e.eventType] || e.eventType }}</span>
              <span class="event-title">{{ e.title }}</span>
              <span class="event-status" :class="'status-' + (e.status || 'completed').toLowerCase()">{{ e.status === 'SCHEDULED' ? '已预约' : e.status === 'CANCELLED' ? '已取消' : '已完成' }}</span>
            </div>
            <div class="event-meta">
              <span>{{ e.eventDate }}</span>
              <span v-if="e.nextDate"> &rarr; 下次: {{ e.nextDate }}</span>
              <span v-if="e.veterinarian"> · {{ e.veterinarian }}</span>
              <span v-if="e.cost"> · ¥{{ e.cost }}</span>
            </div>
            <p class="event-desc" v-if="e.description">{{ e.description }}</p>
            <div class="event-photos" v-if="e.photos && e.photos.length > 0">
              <img v-for="(photo, i) in e.photos" :key="i" :src="photo" class="event-photo" />
            </div>
          </div>
          <div class="event-actions">
            <button class="act-btn" @click="openEdit(e)">编辑</button>
            <button class="act-btn act-danger" @click="onDelete(e)">删除</button>
          </div>
        </div>
      </div>
      <div class="empty-state" v-else>
        <ThemedIcon name="stethoscope" :size="40" class="empty-icon" />
        <p class="empty-title">暂无健康事件</p>
        <p class="empty-desc">记录疫苗、体检、驱虫等健康信息，守护毛孩子的每一天。</p>
        <button class="empty-action" @click="openCreate">记录第一条健康事件</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.health-page {
  flex: 1;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
  padding: 1.5rem 2rem 3rem;
}

/* 面包屑 */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 1rem;
  font-size: 0.82rem;
}
.crumb {
  color: var(--text-muted);
  cursor: pointer;
  transition: color var(--duration-fast);
}
.crumb:hover { color: var(--accent); }
.crumb-sep { color: var(--border-default); }
.crumb-current { color: var(--text-secondary); font-weight: 600; }

/* 宠物头部 */
.pet-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 1.5rem;
}
.pet-avatar-wrap {
  width: 50px;
  height: 50px;
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
.pet-avatar-fallback { font-size: 1.5rem; }
.pet-info { flex: 1; min-width: 0; }
.pet-name {
  font-family: var(--font-display);
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}
.pet-meta { font-size: 0.82rem; color: var(--text-muted); }
.add-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 18px;
  border: 1px solid var(--accent);
  background: var(--accent);
  color: var(--text-inverse);
  font-family: var(--font-body);
  font-size: 0.85rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background var(--duration-fast);
  flex-shrink: 0;
}
.add-btn:hover { background: var(--accent-light); }

/* Loading */
.health-loading {
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
.health-error { text-align: center; padding: 4rem 0; color: var(--danger); }

/* 摘要卡片 */
.summary-section { margin-bottom: 1.5rem; }
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 0.75rem;
}
.sum-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 1rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.sum-card--warn { border-color: var(--spot-glow); }
.sum-icon { color: var(--accent); margin-bottom: 2px; }
.sum-card--warn .sum-icon { color: var(--spot); }
.sum-num {
  font-family: var(--font-display);
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--accent);
}
.sum-num.date-text { font-size: 0.9rem; font-family: var(--font-body); font-weight: 400; }
.sum-label { font-size: 0.72rem; color: var(--text-muted); }

/* 体重区 */
.weight-section {
  margin-bottom: 1.5rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 1.25rem;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}
.section-title {
  font-family: var(--font-display);
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
}
.section-icon { color: var(--accent); }
.link-btn {
  background: none;
  border: none;
  font-family: var(--font-body);
  font-size: 0.82rem;
  color: var(--accent);
  cursor: pointer;
  padding: 0;
}
.link-btn:hover { color: var(--accent-light); }

/* 体重表单 */
.weight-form-area {
  margin-bottom: 1rem;
  padding: 1rem;
  background: var(--bg-warm);
  border-radius: var(--radius-sm);
}
.weight-form { display: flex; flex-direction: column; gap: 8px; }
.wf-row { display: flex; gap: 10px; align-items: flex-end; flex-wrap: wrap; }
.wf-field { display: flex; flex-direction: column; gap: 3px; }
.wf-field--wide { flex: 1; min-width: 120px; }
.wf-label { font-size: 0.72rem; color: var(--text-muted); letter-spacing: 0.03em; }
.wf-input {
  padding: 7px 10px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  background: var(--bg-input);
  font-family: var(--font-body);
  font-size: 0.88rem;
  color: var(--text-primary);
  outline: none;
  transition: border-color var(--duration-fast);
}
.wf-input:focus { border-color: var(--accent); }
.wf-submit {
  padding: 7px 16px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: var(--text-inverse);
  font-family: var(--font-body);
  font-size: 0.85rem;
  cursor: pointer;
  transition: background var(--duration-fast);
  flex-shrink: 0;
}
.wf-submit:hover:not(:disabled) { background: var(--accent-light); }
.wf-submit:disabled { opacity: 0.6; cursor: not-allowed; }
.wf-error { font-size: 0.8rem; color: var(--danger); margin: 0; }

/* 趋势图 */
.trend-chart {
  margin-bottom: 0.75rem;
  padding: 0.5rem;
  background: var(--bg-warm);
  border-radius: var(--radius-sm);
}
.trend-svg {
  width: 100%;
  height: auto;
  max-height: 80px;
}

/* 体重列表 */
.weight-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.weight-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  border-bottom: 1px solid var(--border-subtle);
  font-size: 0.85rem;
}
.weight-item:last-child { border-bottom: none; }
.weight-val {
  font-family: var(--font-display);
  font-weight: 600;
  color: var(--text-primary);
  min-width: 60px;
}
.weight-date {
  font-size: 0.78rem;
  color: var(--text-muted);
  min-width: 80px;
}
.weight-note {
  flex: 1;
  font-size: 0.78rem;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.weight-del {
  background: none;
  border: none;
  font-size: 1.1rem;
  color: var(--text-muted);
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
  opacity: 0;
  transition: opacity var(--duration-fast), color var(--duration-fast);
}
.weight-item:hover .weight-del { opacity: 1; }
.weight-del:hover { color: var(--danger); }

.weight-empty { padding: 0.5rem 0; }
.weight-empty-text { font-size: 0.82rem; color: var(--text-muted); }

/* 筛选 */
.filter-bar { display: flex; gap: 6px; margin-bottom: 1.5rem; flex-wrap: wrap; }
.filter-btn {
  padding: 4px 12px;
  border: 1px solid var(--border-default);
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.8rem;
  color: var(--text-secondary);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--duration-fast);
}
.filter-btn:hover { border-color: var(--accent); color: var(--accent); }
.filter-btn.is-active { background: var(--accent); border-color: var(--accent); color: var(--text-inverse); }

/* 表单 */
.form-area {
  margin-bottom: 1.5rem;
  padding: 1.5rem;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
}
.form-title {
  font-family: var(--font-display);
  font-size: 1.1rem;
  color: var(--text-primary);
  margin: 0 0 1rem;
}
.health-form { display: flex; flex-direction: column; gap: 1rem; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field-row { display: flex; gap: 1rem; }
.field-row .field { flex: 1; }
.field-label { font-size: 0.8rem; color: var(--text-secondary); letter-spacing: 0.04em; }
.field-input {
  padding: 10px 14px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  background: var(--bg-input);
  font-family: var(--font-body);
  font-size: 0.95rem;
  color: var(--text-primary);
  outline: none;
  transition: border-color var(--duration-fast);
}
.field-input:focus { border-color: var(--accent); }
.field-textarea { resize: vertical; }
.form-error { font-size: 0.85rem; color: var(--danger); margin: 0; }

.type-grid { display: flex; flex-wrap: wrap; gap: 6px; }
.type-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-full);
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.8rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast);
}
.type-btn:hover { border-color: var(--accent); color: var(--accent); }
.type-btn.is-active { background: var(--accent); border-color: var(--accent); color: var(--text-inverse); }

.form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 0.5rem; }
.btn-cancel {
  padding: 8px 20px;
  border: 1px solid var(--border-default);
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.9rem;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  cursor: pointer;
}
.btn-submit {
  padding: 8px 20px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: var(--text-inverse);
  font-family: var(--font-body);
  font-size: 0.9rem;
  cursor: pointer;
  transition: background var(--duration-fast);
}
.btn-submit:hover:not(:disabled) { background: var(--accent-light); }
.btn-submit:disabled { opacity: 0.6; cursor: not-allowed; }

/* 事件列表 */
.event-list { display: flex; flex-direction: column; gap: 10px; }
.event-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  transition: border-color var(--duration-fast);
}
.event-item:hover { border-color: var(--accent-glow); }
.event-main { flex: 1; min-width: 0; }
.event-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; flex-wrap: wrap; }
.event-type-tag {
  font-size: 0.7rem;
  padding: 1px 8px;
  border-radius: var(--radius-full);
  font-weight: 600;
  letter-spacing: 0.02em;
}
.type-vaccine { background: #E8F5E9; color: #2E7D32; }
.type-checkup { background: #E3F2FD; color: #1565C0; }
.type-deworming { background: #FFF3E0; color: #E65100; }
.type-neutering { background: #F3E5F5; color: #7B1FA2; }
.type-surgery { background: #FFEBEE; color: #C62828; }
.type-medication { background: #E0F7FA; color: #00695C; }
.type-other { background: var(--bg-aged); color: var(--text-secondary); }
.event-title { font-size: 0.95rem; font-weight: 600; color: var(--text-primary); }
.event-status {
  font-size: 0.7rem;
  padding: 1px 8px;
  border-radius: var(--radius-full);
  margin-left: auto;
}
.status-completed { background: #E8F5E9; color: #2E7D32; }
.status-scheduled { background: #FFF3E0; color: #E65100; }
.status-cancelled { background: #FFEBEE; color: #C62828; }
.event-meta { font-size: 0.8rem; color: var(--text-muted); margin-bottom: 4px; }
.event-desc { font-size: 0.85rem; color: var(--text-secondary); line-height: 1.6; margin: 0; }
.event-photos { display: flex; gap: 6px; margin-top: 8px; flex-wrap: wrap; }
.event-photo {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-subtle);
}
.event-actions { display: flex; gap: 6px; flex-shrink: 0; }
.act-btn {
  background: none;
  border: none;
  font-family: var(--font-body);
  font-size: 0.8rem;
  color: var(--text-muted);
  cursor: pointer;
  padding: 0;
}
.act-btn:hover { color: var(--accent); }
.act-danger:hover { color: var(--danger); }

/* 饮食记录入口 */
.diet-entry {
  margin-bottom: 1.5rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 1.25rem;
}
.diet-entry-desc {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin: 0;
}

/* 空状态 */
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

/* 响应式 */
@media (max-width: 640px) {
  .health-page { padding: 1rem; }
  .pet-header { flex-wrap: wrap; }
  .add-btn { width: 100%; justify-content: center; }
  .field-row { flex-direction: column; gap: 1rem; }
  .wf-row { flex-direction: column; align-items: stretch; }
  .wf-submit { width: 100%; }
  .event-item { flex-direction: column; }
  .event-actions { align-self: flex-end; }
}
</style>
