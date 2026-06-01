<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePets } from '../composables/usePets'
import { getDiets, createDiet, updateDiet, deleteDiet, getDietsByDateRange } from '../api/health'
import ThemedIcon from '../components/common/ThemedIcon.vue'

const route = useRoute()
const router = useRouter()
const petId = Number(route.params.id)
const { getPet } = usePets()

const pet = ref(null)
const loading = ref(true)
const diets = ref([])
const showForm = ref(false)
const editingDiet = ref(null)
const formLoading = ref(false)
const formError = ref('')

// 日期筛选
const filterMode = ref('recent') // recent | range
const startDate = ref('')
const endDate = ref('')
const limit = ref(20)

const foodTypes = [
  { value: '干粮', label: '干粮' },
  { value: '湿粮', label: '湿粮' },
  { value: '零食', label: '零食' },
  { value: '自制', label: '自制' },
  { value: '其他', label: '其他' }
]

const mealTypes = [
  { value: '早餐', label: '早餐' },
  { value: '午餐', label: '午餐' },
  { value: '晚餐', label: '晚餐' },
  { value: '加餐', label: '加餐' }
]

const emptyForm = {
  foodType: '干粮',
  foodName: '',
  amount: '',
  calories: '',
  mealType: '早餐',
  recordedAt: new Date().toISOString().slice(0, 16),
  note: ''
}

const form = ref({ ...emptyForm })

// 统计信息
const todayCalories = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return diets.value
    .filter(d => d.recordedAt && d.recordedAt.startsWith(today))
    .reduce((sum, d) => sum + (Number(d.calories) || 0), 0)
})

const todayMeals = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return diets.value.filter(d => d.recordedAt && d.recordedAt.startsWith(today)).length
})

onMounted(async () => {
  try {
    pet.value = await getPet(petId)
    await fetchDiets()
  } catch (e) {
    formError.value = e?.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
})

async function fetchDiets() {
  try {
    if (filterMode.value === 'range' && startDate.value && endDate.value) {
      const res = await getDietsByDateRange(petId, startDate.value, endDate.value)
      diets.value = res.data || []
    } else {
      const res = await getDiets(petId, limit.value)
      diets.value = res.data || []
    }
  } catch {
    diets.value = []
  }
}

function onFilterChange(mode) {
  filterMode.value = mode
  fetchDiets()
}

function openCreate() {
  editingDiet.value = null
  form.value = { ...emptyForm }
  formError.value = ''
  showForm.value = true
}

function openEdit(diet) {
  editingDiet.value = diet
  form.value = {
    foodType: diet.foodType || '干粮',
    foodName: diet.foodName || '',
    amount: diet.amount || '',
    calories: diet.calories || '',
    mealType: diet.mealType || '早餐',
    recordedAt: diet.recordedAt ? diet.recordedAt.slice(0, 16) : '',
    note: diet.note || ''
  }
  formError.value = ''
  showForm.value = true
}

async function onSubmit() {
  if (!form.value.foodName.trim()) { formError.value = '请输入食物名称'; return }
  if (!form.value.recordedAt) { formError.value = '请选择记录时间'; return }
  formLoading.value = true
  formError.value = ''
  try {
    const payload = {
      ...form.value,
      amount: form.value.amount ? Number(form.value.amount) : undefined,
      calories: form.value.calories ? Number(form.value.calories) : undefined,
      note: form.value.note || undefined
    }
    if (editingDiet.value) {
      await updateDiet(petId, editingDiet.value.id, payload)
    } else {
      await createDiet(petId, payload)
    }
    showForm.value = false
    await fetchDiets()
  } catch (e) {
    formError.value = e?.response?.data?.message || e.message || '保存失败'
  } finally {
    formLoading.value = false
  }
}

async function onDelete(diet) {
  if (!confirm(`确定删除这条饮食记录吗？`)) return
  try {
    await deleteDiet(petId, diet.id)
    await fetchDiets()
  } catch { /* handled */ }
}

function formatDateTime(dt) {
  if (!dt) return ''
  const date = new Date(dt)
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${month}月${day}日 ${hours}:${minutes}`
}

function getMealIcon(mealType) {
  switch (mealType) {
    case '早餐': return 'sunrise'
    case '午餐': return 'sun'
    case '晚餐': return 'moon'
    case '加餐': return 'cookie'
    default: return 'bowl'
  }
}

const speciesLabel = { cat: '猫', dog: '狗', rabbit: '兔', bird: '鸟', other: '其他' }
const speciesEmoji = { cat: '🐱', dog: '🐶', rabbit: '🐰', bird: '🐦', other: '🐾' }

function goBack() { router.push('/health') }
</script>

<template>
  <div class="diet-page">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="crumb" @click="goBack">健康档案</span>
      <span class="crumb-sep">/</span>
      <span class="crumb" @click="router.push(`/pet/${petId}/health`)">{{ pet?.name || '...' }}</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">饮食记录</span>
    </div>

    <!-- 宠物头部 -->
    <div class="pet-header" v-if="pet">
      <div class="pet-avatar-wrap">
        <img v-if="pet.avatarUrl" :src="pet.avatarUrl" class="pet-avatar" />
        <span v-else class="pet-avatar-fallback">{{ speciesEmoji[pet.species] || '🐾' }}</span>
      </div>
      <div class="pet-info">
        <h1 class="pet-name">{{ pet.name }}的饮食记录</h1>
        <span class="pet-meta">{{ speciesLabel[pet.species] || pet.species }}<span v-if="pet.breed"> · {{ pet.breed }}</span></span>
      </div>
      <button class="add-btn" @click="openCreate">
        <ThemedIcon name="bowl" :size="14" />
        <span>记录饮食</span>
      </button>
    </div>

    <div class="diet-loading" v-if="loading">
      <span class="loading-dot">·</span>
      <span class="loading-dot">·</span>
      <span class="loading-dot">·</span>
    </div>
    <div class="diet-error" v-else-if="formError && !showForm">{{ formError }}</div>

    <template v-else>
      <!-- 今日摘要 -->
      <div class="summary-section">
        <div class="summary-grid">
          <div class="sum-card">
            <ThemedIcon name="bowl" :size="18" class="sum-icon" />
            <span class="sum-num">{{ todayMeals }}</span>
            <span class="sum-label">今日餐次</span>
          </div>
          <div class="sum-card">
            <ThemedIcon name="flame" :size="18" class="sum-icon" />
            <span class="sum-num">{{ todayCalories }}</span>
            <span class="sum-label">今日卡路里</span>
          </div>
          <div class="sum-card">
            <ThemedIcon name="scroll" :size="18" class="sum-icon" />
            <span class="sum-num">{{ diets.length }}</span>
            <span class="sum-label">总记录数</span>
          </div>
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <button class="filter-btn" :class="{ 'is-active': filterMode === 'recent' }" @click="onFilterChange('recent')">最近记录</button>
        <button class="filter-btn" :class="{ 'is-active': filterMode === 'range' }" @click="onFilterChange('range')">按日期筛选</button>
      </div>

      <div class="date-filter" v-if="filterMode === 'range'">
        <div class="date-fields">
          <div class="date-field">
            <label class="date-label">开始日期</label>
            <input v-model="startDate" type="date" class="date-input" @change="fetchDiets" />
          </div>
          <div class="date-field">
            <label class="date-label">结束日期</label>
            <input v-model="endDate" type="date" class="date-input" @change="fetchDiets" />
          </div>
        </div>
      </div>

      <!-- 表单 -->
      <div class="form-area" v-if="showForm">
        <h3 class="form-title">{{ editingDiet ? '编辑饮食记录' : '新建饮食记录' }}</h3>
        <form @submit.prevent="onSubmit" class="diet-form">
          <div class="field-row">
            <div class="field">
              <label class="field-label">食物类型 *</label>
              <div class="type-grid">
                <button v-for="t in foodTypes" :key="t.value" type="button"
                  class="type-btn" :class="{ 'is-active': form.foodType === t.value }"
                  @click="form.foodType = t.value"
                >{{ t.label }}</button>
              </div>
            </div>
            <div class="field">
              <label class="field-label">餐次 *</label>
              <div class="type-grid">
                <button v-for="t in mealTypes" :key="t.value" type="button"
                  class="type-btn" :class="{ 'is-active': form.mealType === t.value }"
                  @click="form.mealType = t.value"
                >{{ t.label }}</button>
              </div>
            </div>
          </div>
          <div class="field">
            <label class="field-label">食物名称 *</label>
            <input v-model="form.foodName" class="field-input" placeholder="如：皇家猫粮、鸡胸肉" maxlength="100" />
          </div>
          <div class="field-row">
            <div class="field">
              <label class="field-label">份量 (克)</label>
              <input v-model="form.amount" type="number" step="0.01" min="0" class="field-input" placeholder="50" />
            </div>
            <div class="field">
              <label class="field-label">卡路里</label>
              <input v-model="form.calories" type="number" step="0.01" min="0" class="field-input" placeholder="200" />
            </div>
          </div>
          <div class="field">
            <label class="field-label">记录时间 *</label>
            <input v-model="form.recordedAt" type="datetime-local" class="field-input" />
          </div>
          <div class="field">
            <label class="field-label">备注</label>
            <textarea v-model="form.note" class="field-input field-textarea" rows="2" placeholder="食欲情况、特殊说明..." maxlength="500"></textarea>
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

      <!-- 饮食列表 -->
      <div class="diet-list" v-if="diets.length > 0">
        <div class="diet-item" v-for="d in diets" :key="d.id">
          <div class="diet-icon">
            <ThemedIcon :name="getMealIcon(d.mealType)" :size="20" />
          </div>
          <div class="diet-main">
            <div class="diet-row">
              <span class="diet-name">{{ d.foodName }}</span>
              <span class="diet-meal-tag">{{ d.mealType }}</span>
              <span class="diet-type-tag">{{ d.foodType }}</span>
            </div>
            <div class="diet-meta">
              <span v-if="d.amount">{{ d.amount }}克</span>
              <span v-if="d.calories"> · {{ d.calories }}卡</span>
              <span> · {{ formatDateTime(d.recordedAt) }}</span>
            </div>
            <p class="diet-note" v-if="d.note">{{ d.note }}</p>
          </div>
          <div class="diet-actions">
            <button class="act-btn" @click="openEdit(d)">编辑</button>
            <button class="act-btn act-danger" @click="onDelete(d)">删除</button>
          </div>
        </div>
      </div>
      <div class="empty-state" v-else>
        <ThemedIcon name="bowl" :size="40" class="empty-icon" />
        <p class="empty-title">暂无饮食记录</p>
        <p class="empty-desc">记录每日饮食，关注毛孩子的营养健康。</p>
        <button class="empty-action" @click="openCreate">记录第一餐</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.diet-page {
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
.diet-loading {
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
.diet-error { text-align: center; padding: 4rem 0; color: var(--danger); }

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
.sum-icon { color: var(--accent); margin-bottom: 2px; }
.sum-num {
  font-family: var(--font-display);
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--accent);
}
.sum-label { font-size: 0.72rem; color: var(--text-muted); }

/* 筛选 */
.filter-bar { display: flex; gap: 6px; margin-bottom: 1rem; flex-wrap: wrap; }
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

.date-filter {
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}
.date-fields { display: flex; gap: 1rem; flex-wrap: wrap; }
.date-field { display: flex; flex-direction: column; gap: 4px; }
.date-label { font-size: 0.8rem; color: var(--text-secondary); }
.date-input {
  padding: 8px 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  background: var(--bg-input);
  font-family: var(--font-body);
  font-size: 0.9rem;
  color: var(--text-primary);
  outline: none;
  transition: border-color var(--duration-fast);
}
.date-input:focus { border-color: var(--accent); }

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
.diet-form { display: flex; flex-direction: column; gap: 1rem; }
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

/* 饮食列表 */
.diet-list { display: flex; flex-direction: column; gap: 10px; }
.diet-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 1rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  transition: border-color var(--duration-fast);
}
.diet-item:hover { border-color: var(--accent-glow); }
.diet-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  background: var(--bg-warm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  flex-shrink: 0;
}
.diet-main { flex: 1; min-width: 0; }
.diet-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; flex-wrap: wrap; }
.diet-name { font-size: 0.95rem; font-weight: 600; color: var(--text-primary); }
.diet-meal-tag {
  font-size: 0.7rem;
  padding: 1px 8px;
  border-radius: var(--radius-full);
  background: #E8F5E9;
  color: #2E7D32;
  font-weight: 600;
}
.diet-type-tag {
  font-size: 0.7rem;
  padding: 1px 8px;
  border-radius: var(--radius-full);
  background: var(--bg-aged);
  color: var(--text-secondary);
}
.diet-meta { font-size: 0.8rem; color: var(--text-muted); margin-bottom: 4px; }
.diet-note { font-size: 0.85rem; color: var(--text-secondary); line-height: 1.6; margin: 0; }
.diet-actions { display: flex; gap: 6px; flex-shrink: 0; }
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
  .diet-page { padding: 1rem; }
  .pet-header { flex-wrap: wrap; }
  .add-btn { width: 100%; justify-content: center; }
  .field-row { flex-direction: column; gap: 1rem; }
  .date-fields { flex-direction: column; }
  .diet-item { flex-direction: column; }
  .diet-actions { align-self: flex-end; }
}
</style>
