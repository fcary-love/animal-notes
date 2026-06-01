<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { usePets } from '../../composables/usePets'
import { createDiet, getDiets } from '../../api/health'
import ThemedIcon from '../common/ThemedIcon.vue'

const router = useRouter()
const { pets, currentPet, fetchPets } = usePets()
const loaded = ref(false)
const loading = ref(false)
const error = ref('')
const todayLogs = ref([])

const mealTypes = [
  { key: 'dry', icon: 'bowl', label: '猫粮', foodType: '干粮', calories: 45, amount: 30, mealType: '早餐' },
  { key: 'wet', icon: 'file', label: '主食罐', foodType: '湿粮', calories: 80, amount: 80, mealType: '午餐' },
  { key: 'snack', icon: 'paw', label: '小鱼干', foodType: '零食', calories: 12, amount: 8, mealType: '加餐' },
  { key: 'treat', icon: 'heart', label: '零食', foodType: '零食', calories: 20, amount: 10, mealType: '加餐' }
]

const todayCalories = computed(() => todayLogs.value.reduce((sum, d) => sum + (Number(d.calories) || 0), 0))
const todayMeals = computed(() => todayLogs.value.length)
const petName = computed(() => currentPet.value?.name || pets.value[0]?.name || '宠物')

function isToday(recordedAt) {
  return recordedAt?.slice(0, 10) === new Date().toISOString().slice(0, 10)
}

async function loadDiets() {
  const pet = currentPet.value || pets.value[0]
  if (!pet) {
    todayLogs.value = []
    loaded.value = true
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await getDiets(pet.id, 30)
    todayLogs.value = (res.data || []).filter(d => isToday(d.recordedAt))
  } catch (e) {
    error.value = e?.response?.data?.message || '饮食记录加载失败'
  } finally {
    loading.value = false
    loaded.value = true
  }
}

async function addMeal(type) {
  const pet = currentPet.value || pets.value[0]
  const mt = mealTypes.find(m => m.key === type)
  if (!pet || !mt) return
  loading.value = true
  error.value = ''
  try {
    await createDiet(pet.id, {
      foodType: mt.foodType,
      foodName: mt.label,
      amount: mt.amount,
      calories: mt.calories,
      mealType: mt.mealType,
      recordedAt: new Date().toISOString().slice(0, 16),
      note: '来自更多菜单快捷记录'
    })
    await loadDiets()
  } catch (e) {
    error.value = e?.response?.data?.message || '保存失败'
    loading.value = false
  }
}

function goFullDiet() {
  const pet = currentPet.value || pets.value[0]
  if (pet) router.push(`/pet/${pet.id}/diet`)
}

onMounted(async () => {
  if (pets.value.length === 0) await fetchPets()
  await loadDiets()
})

watch(currentPet, loadDiets)
</script>

<template>
  <div class="panel-content" :class="{ loaded }">
    <section class="diet-section hero-card">
      <div>
        <span class="section-label">今日饮食</span>
        <h3>{{ petName }}</h3>
        <p>{{ todayMeals }} 条记录 · {{ todayCalories }} kcal</p>
      </div>
      <ThemedIcon name="bowl" :size="34" />
    </section>

    <p v-if="error" class="panel-error">{{ error }}</p>

    <section class="diet-section">
      <div class="section-label">快捷记录</div>
      <div class="meal-grid">
        <button
          v-for="(mt, idx) in mealTypes"
          :key="mt.key"
          class="meal-btn"
          :style="{ '--i': idx }"
          :disabled="loading || !currentPet"
          @click="addMeal(mt.key)"
        >
          <ThemedIcon :name="mt.icon" :size="20" />
          <span class="meal-label">{{ mt.label }}</span>
          <span class="meal-kcal">{{ mt.calories }}kcal</span>
        </button>
      </div>
    </section>

    <section class="diet-section">
      <div class="section-label">今天已经记录</div>
      <div class="log-list" v-if="todayLogs.length">
        <div v-for="log in todayLogs" :key="log.id" class="log-item">
          <ThemedIcon name="bowl" :size="18" />
          <div class="log-detail">
            <span class="log-note">{{ log.foodName }}</span>
            <span class="log-time">{{ log.mealType }} · {{ log.recordedAt?.slice(11, 16) }} · {{ log.calories || 0 }}kcal</span>
          </div>
        </div>
      </div>
      <div v-else class="log-empty">{{ loading ? '同步中...' : '今天还没有饮食记录' }}</div>
    </section>

    <button class="panel-link" @click="goFullDiet" :disabled="!currentPet">打开完整饮食页</button>
  </div>
</template>

<style scoped>
.panel-content { opacity: 0; transform: translateY(12px); transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1); }
.panel-content.loaded { opacity: 1; transform: translateY(0); }
.diet-section { margin-bottom: 22px; }
.section-label {
  display: block;
  font-family: var(--font-display);
  font-size: 0.78rem;
  color: var(--text-muted);
  margin-bottom: 8px;
}
.hero-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: var(--bg-warm);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  color: var(--accent);
}
.hero-card h3 { margin: 0; font-family: var(--font-display); color: var(--text-primary); }
.hero-card p { margin: 4px 0 0; color: var(--text-muted); font-size: 0.82rem; }
.meal-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.meal-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: 14px 8px;
  background: var(--bg-warm);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  cursor: pointer;
  color: var(--text-secondary);
  font-family: var(--font-body);
}
.meal-btn:hover:not(:disabled) { border-color: var(--accent); color: var(--accent); transform: translateY(-1px); }
.meal-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.meal-label { font-size: 0.78rem; font-weight: 600; }
.meal-kcal { font-size: 0.66rem; color: var(--text-muted); }
.log-list { display: flex; flex-direction: column; gap: 7px; }
.log-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--bg-warm);
  border-radius: var(--radius-md);
}
.log-detail { flex: 1; min-width: 0; }
.log-note { display: block; font-size: 0.82rem; color: var(--text-secondary); }
.log-time { display: block; font-size: 0.68rem; color: var(--text-muted); }
.log-empty, .panel-error { font-size: 0.78rem; color: var(--text-muted); text-align: center; padding: 14px 0; }
.panel-error { color: var(--danger); }
.panel-link {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--accent);
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--accent);
  font-family: var(--font-body);
  cursor: pointer;
}
.panel-link:hover:not(:disabled) { background: var(--accent); color: #fff; }
.panel-link:disabled { opacity: 0.45; cursor: not-allowed; }
</style>
