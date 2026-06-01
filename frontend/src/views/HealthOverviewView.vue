<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePets } from '../composables/usePets'
import { getHealthSummary } from '../api/health'
import ThemedIcon from '../components/common/ThemedIcon.vue'

const router = useRouter()
const { pets, fetchPets, loading: petsLoading } = usePets()

const loading = ref(true)
const petCards = ref([])

onMounted(async () => {
  try {
    await fetchPets()
    if (pets.value.length === 1) {
      router.replace(`/pet/${pets.value[0].id}/health`)
      return
    }
    // Fetch health summary for each pet
    const results = await Promise.allSettled(
      pets.value.map(async (pet) => {
        try {
          const res = await getHealthSummary(pet.id)
          return { pet, summary: res.data }
        } catch {
          return { pet, summary: null }
        }
      })
    )
    petCards.value = results.map(r => r.value || r.reason)
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
})

function goHealth(petId) {
  router.push(`/pet/${petId}/health`)
}

function goCreate() {
  router.push('/pet/create')
}

const speciesEmoji = { cat: '🐱', dog: '🐶', rabbit: '🐰', bird: '🐦', other: '🐾' }
</script>

<template>
  <div class="overview-page">
    <div class="overview-header">
      <h1 class="page-title">
        <ThemedIcon name="heartPulse" :size="22" class="title-icon" />
        健康档案
      </h1>
      <p class="page-desc">记录每一次疫苗、体检和成长，守护毛孩子的健康。</p>
    </div>

    <div class="overview-loading" v-if="loading">
      <span class="loading-dot">·</span>
      <span class="loading-dot">·</span>
      <span class="loading-dot">·</span>
    </div>

    <template v-else>
      <div class="pet-grid" v-if="petCards.length > 0">
        <div
          class="pet-card"
          v-for="item in petCards"
          :key="item.pet.id"
          @click="goHealth(item.pet.id)"
        >
          <div class="pet-card-head">
            <div class="pet-avatar-wrap">
              <img v-if="item.pet.avatarUrl" :src="item.pet.avatarUrl" class="pet-avatar" />
              <span v-else class="pet-avatar-fallback">{{ speciesEmoji[item.pet.species] || '🐾' }}</span>
            </div>
            <div class="pet-info">
              <span class="pet-name">{{ item.pet.name }}</span>
              <span class="pet-species">{{ item.pet.breed || item.pet.species }}</span>
            </div>
            <ThemedIcon name="heartPulse" :size="18" class="card-heart" />
          </div>

          <div class="pet-card-body" v-if="item.summary">
            <div class="stat-row">
              <div class="stat-item">
                <span class="stat-num">{{ item.summary.totalEvents }}</span>
                <span class="stat-label">健康事件</span>
              </div>
              <div class="stat-item">
                <span class="stat-num">{{ item.summary.vaccineCompleted }}/{{ item.summary.vaccineCount }}</span>
                <span class="stat-label">疫苗完成</span>
              </div>
            </div>
            <div class="reminder-row" v-if="item.summary.nextVaccineDate || item.summary.nextDewormingDate">
              <div class="reminder-item" v-if="item.summary.nextVaccineDate">
                <ThemedIcon name="stethoscope" :size="13" />
                <span>下次疫苗 {{ item.summary.nextVaccineDate }}</span>
              </div>
              <div class="reminder-item" v-if="item.summary.nextDewormingDate">
                <ThemedIcon name="bell" :size="13" />
                <span>下次驱虫 {{ item.summary.nextDewormingDate }}</span>
              </div>
            </div>
          </div>
          <div class="pet-card-body pet-card-empty" v-else>
            <span>暂无健康记录</span>
          </div>

          <div class="pet-card-foot">
            <span class="foot-link">查看详情 &rarr;</span>
          </div>
        </div>
      </div>

      <div class="empty-overview" v-else>
        <ThemedIcon name="heartPulse" :size="48" class="empty-icon" />
        <p class="empty-title">还没有宠物档案</p>
        <p class="empty-desc">先创建一只宠物，再开始记录它的健康故事。</p>
        <button class="empty-btn" @click="goCreate">创建宠物档案</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.overview-page {
  flex: 1;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
  padding: 2rem;
}

.overview-header {
  margin-bottom: 2rem;
}
.page-title {
  font-family: var(--font-display);
  font-size: 1.5rem;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 0.5rem;
}
.title-icon { color: var(--accent); }
.page-desc {
  font-size: 0.88rem;
  color: var(--text-muted);
  margin: 0;
}

/* Loading */
.overview-loading {
  text-align: center;
  padding: 4rem 0;
  color: var(--text-muted);
  font-size: 1.5rem;
  letter-spacing: 0.5em;
}
.loading-dot {
  animation: blink 1.2s infinite;
}
.loading-dot:nth-child(2) { animation-delay: 0.2s; }
.loading-dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 60%, 100% { opacity: 0.2; }
  30% { opacity: 1; }
}

/* Pet grid */
.pet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}

.pet-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  cursor: pointer;
  transition: border-color var(--duration-fast), box-shadow var(--duration-fast);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.pet-card:hover {
  border-color: var(--accent-glow);
  box-shadow: var(--shadow-card);
}

.pet-card-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.pet-avatar-wrap {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-full);
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-warm);
  flex-shrink: 0;
}
.pet-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.pet-avatar-fallback {
  font-size: 1.3rem;
}
.pet-info {
  flex: 1;
  min-width: 0;
}
.pet-name {
  display: block;
  font-family: var(--font-display);
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--text-primary);
}
.pet-species {
  font-size: 0.78rem;
  color: var(--text-muted);
}
.card-heart {
  color: var(--accent-glow);
  flex-shrink: 0;
}

/* Stats */
.stat-row {
  display: flex;
  gap: 1.5rem;
}
.stat-item {
  display: flex;
  flex-direction: column;
}
.stat-num {
  font-family: var(--font-display);
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--accent);
}
.stat-label {
  font-size: 0.72rem;
  color: var(--text-muted);
}

/* Reminders */
.reminder-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.reminder-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.78rem;
  color: var(--text-secondary);
}
.reminder-item svg { color: var(--spot); flex-shrink: 0; }

/* Empty card */
.pet-card-empty {
  font-size: 0.82rem;
  color: var(--text-muted);
  text-align: center;
  padding: 0.5rem 0;
}

/* Card footer */
.pet-card-foot {
  border-top: 1px solid var(--border-subtle);
  padding-top: 0.75rem;
}
.foot-link {
  font-size: 0.82rem;
  color: var(--accent);
  transition: color var(--duration-fast);
}
.pet-card:hover .foot-link { color: var(--accent-light); }

/* Empty state */
.empty-overview {
  text-align: center;
  padding: 4rem 2rem;
}
.empty-icon { color: var(--accent-glow); margin-bottom: 1rem; }
.empty-title {
  font-family: var(--font-display);
  font-size: 1.1rem;
  color: var(--text-primary);
  margin: 0 0 0.5rem;
}
.empty-desc {
  font-size: 0.88rem;
  color: var(--text-muted);
  margin: 0 0 1.5rem;
}
.empty-btn {
  padding: 8px 24px;
  border: 1px solid var(--accent);
  background: var(--accent);
  color: var(--text-inverse);
  font-family: var(--font-body);
  font-size: 0.9rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background var(--duration-fast);
}
.empty-btn:hover { background: var(--accent-light); }

@media (max-width: 640px) {
  .overview-page { padding: 1.5rem 1rem; }
  .pet-grid { grid-template-columns: 1fr; }
}
</style>
