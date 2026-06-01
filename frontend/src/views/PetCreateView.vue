<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { usePets } from '../composables/usePets'
import PetAvatar from '../components/pet/PetAvatar.vue'
import PetForm from '../components/pet/PetForm.vue'

const router = useRouter()
const { create, loading, error } = usePets()
const avatarFile = ref(null)
const avatarPreview = ref('')
const avatarComp = ref(null)
const createdId = ref(null)

async function onSubmit(formData) {
  try {
    const pet = await create({
      name: formData.name,
      species: formData.species,
      breed: formData.breed || undefined,
      birthday: formData.birthday || undefined,
      bio: formData.bio || undefined
    })
    createdId.value = pet.id
    if (avatarFile.value) {
      const { uploadAvatar } = usePets()
      await uploadAvatar(pet.id, avatarFile.value)
    }
    router.push('/timeline')
  } catch { /* handled in composable */ }
}

function onAvatarUpload(file) {
  avatarFile.value = file
  avatarPreview.value = URL.createObjectURL(file)
}
</script>

<template>
  <div class="page">
    <div class="page-card">
      <div class="page-hero">
        <router-link to="/timeline" class="back-link">&larr; 返回</router-link>
        <PetAvatar :src="avatarPreview" pet-name="新" @upload="onAvatarUpload" ref="avatarComp" />
        <h1 class="page-title">创建宠物档案</h1>
      </div>

      <PetForm :modelValue="{ name: '', species: '', breed: '', birthday: '', bio: '' }" :loading="loading" @submit="onSubmit" />

      <p class="form-error" v-if="error">{{ error }}</p>
    </div>
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
</style>
