<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Object, required: true },
  loading: { type: Boolean, default: false },
})
const emit = defineEmits(['submit'])

const form = ref({
  name: '', species: '', breed: '', birthday: '', bio: ''
})

const fieldErrors = ref({})

watch(() => props.modelValue, (val) => {
  if (val) {
    form.value = {
      name: val.name || '',
      species: val.species || '',
      breed: val.breed || '',
      birthday: val.birthday || '',
      bio: val.bio || ''
    }
  }
}, { immediate: true })

function validate() {
  const e = {}
  if (!form.value.name.trim()) e.name = '请输入宠物名'
  if (!form.value.species.trim()) e.species = '请输入物种'
  fieldErrors.value = e
  return Object.keys(e).length === 0
}

function submit() {
  fieldErrors.value = {}
  if (!validate()) return
  emit('submit', { ...form.value })
}

const speciesOptions = ['猫', '狗', '兔', '鸟', '仓鼠', '鱼', '其它']
</script>

<template>
  <form class="pet-form" @submit.prevent="submit">
    <div class="field">
      <label class="field-label">宠物名 *</label>
      <input v-model="form.name" type="text" class="field-input" :class="{ 'has-error': fieldErrors.name }" placeholder="比如：奶茶、Lucky" maxlength="50" />
      <span class="field-err" v-if="fieldErrors.name">{{ fieldErrors.name }}</span>
    </div>

    <div class="field">
      <label class="field-label">物种 *</label>
      <div class="species-grid">
        <button
          v-for="s in speciesOptions" :key="s"
          type="button"
          class="species-btn"
          :class="{ 'is-active': form.species === s }"
          @click="form.species = s"
        >{{ s }}</button>
      </div>
      <span class="field-err" v-if="fieldErrors.species">{{ fieldErrors.species }}</span>
    </div>

    <div class="field">
      <label class="field-label">品种</label>
      <input v-model="form.breed" type="text" class="field-input" placeholder="比如：英短、柯基" maxlength="50" />
    </div>

    <div class="field">
      <label class="field-label">生日</label>
      <input v-model="form.birthday" type="date" class="field-input" />
    </div>

    <div class="field">
      <label class="field-label">简介</label>
      <textarea v-model="form.bio" class="field-input field-textarea" placeholder="用一句话介绍它吧" maxlength="200" rows="2"></textarea>
    </div>

    <button class="btn-submit" type="submit" :disabled="loading">
      <span v-if="loading" class="spinner"></span>
      <span v-else>保存</span>
    </button>
  </form>
</template>

<style scoped>
.pet-form { display: flex; flex-direction: column; gap: 1.25rem; }
.field { display: flex; flex-direction: column; gap: 4px; }
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
  transition: border-color var(--duration-fast) var(--ease-out);
}
.field-input:focus { border-color: var(--accent); }
.field-input.has-error { border-color: var(--danger); }
.field-textarea { resize: vertical; }

.species-grid { display: flex; flex-wrap: wrap; gap: 6px; }
.species-btn {
  padding: 6px 14px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-full);
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.85rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}
.species-btn:hover { border-color: var(--accent); color: var(--accent); }
.species-btn.is-active {
  background: var(--accent);
  border-color: var(--accent);
  color: var(--text-inverse);
}

.field-err { font-size: 0.78rem; color: var(--danger); }

.btn-submit {
  padding: 12px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: var(--text-inverse);
  font-family: var(--font-body);
  font-size: 1rem;
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-out);
  display: flex; justify-content: center; align-items: center; min-height: 44px;
}
.btn-submit:hover:not(:disabled) { background: var(--accent-light); }
.btn-submit:disabled { opacity: 0.6; cursor: not-allowed; }

.spinner {
  width: 18px; height: 18px;
  border: 2px solid transparent;
  border-top-color: var(--text-inverse);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
