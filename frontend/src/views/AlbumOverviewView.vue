<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePets } from '../composables/usePets'
import { getPetPhotos } from '../api/moment'
import ThemedIcon from '../components/common/ThemedIcon.vue'

const router = useRouter()
const { pets, fetchPets, loading: petsLoading } = usePets()

const loading = ref(true)
const petCards = ref([])

onMounted(async () => {
  try {
    await fetchPets()
    if (pets.value.length === 1) {
      router.replace(`/pet/${pets.value[0].id}/album`)
      return
    }
    const results = await Promise.allSettled(
      pets.value.map(async (pet) => {
        try {
          const res = await getPetPhotos(pet.id, { page: 1, size: 1 })
          const latestPhoto = res.data.records.length > 0 ? res.data.records[0] : null
          return { pet, totalPhotos: res.data.total, latestPhoto }
        } catch {
          return { pet, totalPhotos: 0, latestPhoto: null }
        }
      })
    )
    petCards.value = results.map(r => r.value || r.reason)
  } catch {
    // handled
  } finally {
    loading.value = false
  }
})

function goAlbum(petId) {
  router.push(`/pet/${petId}/album`)
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
        <ThemedIcon name="images" :size="22" class="title-icon" />
        成长相册
      </h1>
      <p class="page-desc">收集每一个值得珍藏的瞬间，翻阅毛孩子的成长故事。</p>
    </div>

    <div class="overview-loading" v-if="loading">
      <span class="loading-dot">·</span>
      <span class="loading-dot">·</span>
      <span class="loading-dot">·</span>
    </div>

    <template v-else>
      <div class="pet-grid" v-if="petCards.length > 0">
        <div class="pet-card" v-for="item in petCards" :key="item.pet.id" @click="goAlbum(item.pet.id)">
          <div class="card-preview">
            <img v-if="item.latestPhoto" :src="item.latestPhoto.photoUrl" class="preview-img" />
            <div v-else class="preview-empty">
              <ThemedIcon name="images" :size="32" />
            </div>
          </div>
          <div class="card-info">
            <div class="card-head">
              <div class="pet-avatar-wrap">
                <img v-if="item.pet.avatarUrl" :src="item.pet.avatarUrl" class="pet-avatar" />
                <span v-else class="pet-avatar-fallback">{{ speciesEmoji[item.pet.species] || '🐾' }}</span>
              </div>
              <div class="pet-text">
                <span class="pet-name">{{ item.pet.name }}</span>
                <span class="pet-meta">{{ item.totalPhotos }} 张照片</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="empty-overview" v-else>
        <ThemedIcon name="images" :size="48" class="empty-icon" />
        <p class="empty-title">还没有宠物档案</p>
        <p class="empty-desc">先创建一只宠物，开始记录它的成长瞬间。</p>
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

.overview-header { margin-bottom: 2rem; }
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

.overview-loading {
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

.pet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1rem;
}

.pet-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: border-color var(--duration-fast), box-shadow var(--duration-fast);
}
.pet-card:hover {
  border-color: var(--accent-glow);
  box-shadow: var(--shadow-card);
}

.card-preview {
  width: 100%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: var(--bg-warm);
  display: flex;
  align-items: center;
  justify-content: center;
}
.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}
.pet-card:hover .preview-img { transform: scale(1.03); }
.preview-empty { color: var(--border-default); }

.card-info { padding: 0.75rem 1rem; }
.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pet-avatar-wrap {
  width: 32px;
  height: 32px;
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
.pet-avatar-fallback { font-size: 1rem; }
.pet-text { flex: 1; min-width: 0; }
.pet-name {
  display: block;
  font-family: var(--font-display);
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-primary);
}
.pet-meta { font-size: 0.75rem; color: var(--text-muted); }

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
