<script setup>
import { ref } from 'vue'

const props = defineProps({
  src: String,
  petName: { type: String, default: '?' }
})
const emit = defineEmits(['upload'])
const uploading = ref(false)

function onFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  uploading.value = true
  emit('upload', file)
}

function resetUpload() {
  uploading.value = false
}
defineExpose({ resetUpload })
</script>

<template>
  <div class="pet-avatar-wrap">
    <label class="avatar-label">
      <img v-if="src" :src="src" alt="" class="avatar-img" />
      <span v-else class="avatar-fallback">{{ petName.charAt(0) }}</span>
      <div class="avatar-overlay">
        <span v-if="uploading" class="upload-spinner"></span>
        <span v-else>&#128247;</span>
      </div>
      <input type="file" accept="image/*" class="avatar-input" @change="onFileChange" />
    </label>
  </div>
</template>

<style scoped>
.pet-avatar-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 1rem;
}
.avatar-label {
  position: relative;
  width: 100px; height: 100px;
  border-radius: var(--radius-full);
  overflow: hidden;
  cursor: pointer;
  border: 3px solid var(--border-subtle);
  display: flex; align-items: center; justify-content: center;
  background: var(--bg-aged);
  transition: border-color var(--duration-fast) var(--ease-out);
}
.avatar-label:hover { border-color: var(--accent-glow); }
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-fallback {
  font-family: var(--font-display);
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--accent);
}
.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(61, 50, 38, 0.3);
  display: flex; align-items: center; justify-content: center;
  font-size: 1.5rem;
  opacity: 0;
  transition: opacity var(--duration-fast) var(--ease-out);
}
.avatar-label:hover .avatar-overlay { opacity: 1; }
.avatar-input { display: none; }

.upload-spinner {
  width: 24px; height: 24px;
  border: 2px solid transparent;
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
