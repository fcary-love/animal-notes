<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { generateCard } from '../api/card'
import { usePets } from '../composables/usePets'

const route = useRoute()
const router = useRouter()
const petId = Number(route.params.petId)
const { getPet } = usePets()

const loading = ref(true)
const error = ref('')
const cardUrl = ref('')
const pet = ref(null)

onMounted(async () => {
  try {
    pet.value = await getPet(petId)
    const res = await generateCard(petId)
    cardUrl.value = res.data.url
  } catch (e) {
    error.value = e?.response?.data?.message || e.message || '生成失败'
  } finally {
    loading.value = false
  }
})

function download() {
  const a = document.createElement('a')
  a.href = cardUrl.value
  a.download = `${pet.value?.name || 'pet'}-timeline-card.png`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

function goBack() {
  router.push('/timeline')
}
</script>

<template>
  <div class="card-page">
    <div class="card-header">
      <button class="back-btn" @click="goBack">← 返回时间线</button>
      <h1 class="page-title">时光卡片</h1>
    </div>

    <div class="card-body">
      <!-- loading -->
      <div class="card-state" v-if="loading">
        <span class="spinner"></span>
        <span>正在生成卡片...</span>
      </div>

      <!-- error -->
      <div class="card-state" v-else-if="error">
        <p class="error-text">{{ error }}</p>
        <button class="retry-btn" @click="router.go(0)">重试</button>
      </div>

      <!-- card preview -->
      <div v-else class="card-preview">
        <img :src="cardUrl" :alt="pet?.name + ' 的时光卡片'" class="card-image" />
        <div class="card-actions">
          <button class="download-btn" @click="download">下载卡片</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card-page {
  flex: 1;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
  padding: 1.5rem 2rem;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  margin-bottom: 2rem;
}
.back-btn {
  background: none;
  border: 1px solid var(--border-default);
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  font-family: var(--font-body);
  font-size: 0.85rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}
.back-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.page-title {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.5rem;
  color: var(--text-primary);
  margin: 0;
}

.card-body {
  display: flex;
  justify-content: center;
}

.card-state {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: center;
  padding: 6rem 0;
  color: var(--text-muted);
  font-size: 0.95rem;
}
.spinner {
  width: 18px; height: 18px;
  border: 2px solid var(--border-default);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.error-text { color: var(--danger); margin-bottom: 1rem; }
.retry-btn {
  padding: 6px 16px;
  border: 1px solid var(--border-default);
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.85rem;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.card-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}
.card-image {
  max-width: 100%;
  border-radius: var(--radius-md);
  box-shadow: 0 4px 24px rgba(61, 50, 43, 0.12);
  border: 1px solid var(--border-subtle);
}
.card-actions { display: flex; gap: 12px; }
.download-btn {
  padding: 10px 28px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: var(--text-inverse);
  font-family: var(--font-body);
  font-size: 0.95rem;
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-out);
}
.download-btn:hover { background: var(--accent-light); }
</style>
