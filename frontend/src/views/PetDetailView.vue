<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePets } from '../composables/usePets'
import PetAvatar from '../components/pet/PetAvatar.vue'
import PetForm from '../components/pet/PetForm.vue'
import ThemedIcon from '../components/common/ThemedIcon.vue'

const route = useRoute()
const router = useRouter()
const id = ref(Number(route.params.id))
const pet = ref(null)
const { pets, getPet, update, remove, uploadAvatar, loading, error } = usePets()

onMounted(async () => {
  // try local state first
  const found = pets.value.find(p => p.id === id.value)
  if (found) {
    pet.value = found
  } else {
    try {
      pet.value = await getPet(id.value)
    } catch { router.push('/timeline') }
  }
})

async function onSubmit(formData) {
  await update(id.value, {
    name: formData.name,
    species: formData.species,
    breed: formData.breed || undefined,
    birthday: formData.birthday || undefined,
    bio: formData.bio || undefined
  })
  router.push('/timeline')
}

async function onAvatarUpload(file) {
  await uploadAvatar(id.value, file)
}

async function onDelete() {
  if (!confirm('确定要删除这只宠物吗？删除后可以在30天内恢复。')) return
  try {
    await remove(id.value)
    router.push('/timeline')
  } catch { /* handled */ }
}
</script>

<template>
  <div class="page" v-if="pet">
    <div class="page-card">
      <div class="page-hero">
        <router-link to="/timeline" class="back-link">&larr; 返回时间线</router-link>
        <PetAvatar :src="pet.avatarUrl" :pet-name="pet.name" @upload="onAvatarUpload" />
        <h1 class="page-title">{{ pet.name }}</h1>
      </div>

      <PetForm :modelValue="pet" :loading="loading" @submit="onSubmit" />

      <p class="form-error" v-if="error">{{ error }}</p>

      <div class="quick-links">
        <router-link :to="`/pet/${id}/health`" class="quick-link-btn"><ThemedIcon name="stethoscope" :size="15" /> 健康管理</router-link>
        <router-link :to="`/pet/${id}/report`" class="quick-link-btn"><ThemedIcon name="file" :size="15" /> 成长报告</router-link>
        <router-link :to="`/pet/${id}/3d`" class="quick-link-btn"><ThemedIcon name="cube" :size="15" /> 3D宠物</router-link>
      </div>

      <div class="danger-zone">
        <button class="btn-delete" @click="onDelete">删除这份档案</button>
      </div>
    </div>
  </div>

  <div class="page loading-center" v-else>
    <span class="loading-text">加载中...</span>
  </div>
</template>

<style scoped>
.page {
  flex: 1;
  max-width: 520px;
  margin: 0 auto;
  padding: 2rem;
  width: 100%;
}
.page-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 2rem;
  box-shadow: var(--shadow-sm);
}
.page-hero {
  text-align: center;
  margin-bottom: 1.5rem;
}
.back-link {
  display: inline-block;
  margin-bottom: 1rem;
  font-size: 0.85rem;
  color: var(--text-muted);
  text-decoration: none;
}
.back-link:hover { color: var(--accent); }
.page-title {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.4rem;
  color: var(--text-primary);
}
.form-error { text-align: center; font-size: 0.85rem; color: var(--danger); margin-top: 1rem; }

.quick-links {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;
  margin-top: 1.5rem; padding-top: 1.5rem; border-top: 1px solid var(--border-subtle);
}
.quick-link-btn {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 10px; text-align: center;
  border: 1px solid var(--border-default); border-radius: var(--radius-sm);
  text-decoration: none; font-size: 0.85rem; color: var(--text-secondary);
  transition: all var(--duration-fast);
}
.quick-link-btn:hover { border-color: var(--accent); color: var(--accent); background: var(--bg-input); }

.danger-zone {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--border-subtle);
  text-align: center;
}
.btn-delete {
  padding: 8px 20px;
  border: 1px solid var(--danger);
  background: transparent;
  color: var(--danger);
  font-family: var(--font-body);
  font-size: 0.85rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}
.btn-delete:hover { background: var(--danger); color: #fff; }

.loading-center {
  display: flex; align-items: center; justify-content: center;
}
.loading-text { font-size: 0.95rem; color: var(--text-muted); }
</style>
