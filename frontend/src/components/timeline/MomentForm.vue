<script setup>
import { ref } from 'vue'
import { analyzePhoto } from '../../api/ai'
import { uploadPhoto as uploadPhotoFile } from '../../api/moment'
import { useToast } from '../../composables/useToast'
import ThemedIcon from '../common/ThemedIcon.vue'

const { showToast } = useToast()

// 时间范围：今天 ~ 过去7天
function pad(n) { return String(n).padStart(2, '0') }
function toLocalDatetime(d) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}
const now = new Date()
const maxDate = toLocalDatetime(now)
const minDateObj = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
const minDate = toLocalDatetime(minDateObj)

const emptyMoment = {
  content: '',
  photos: [],
  occurredAt: '',
  isMilestone: false,
  milestoneLabel: '',
  location: ''
}

const props = defineProps({
  modelValue: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})
const emit = defineEmits(['submit', 'cancel'])
const initialValue = props.modelValue || emptyMoment

const form = ref({
  content: initialValue.content || '',
  photos: [...(initialValue.photos || [])],
  occurredAt: initialValue.occurredAt ? initialValue.occurredAt.substring(0, 16) : maxDate,
  isMilestone: initialValue.isMilestone || false,
  milestoneLabel: initialValue.milestoneLabel || '',
  location: initialValue.location || ''
})

const fieldErrors = ref({})
const analyzing = ref(false)
const uploading = ref(false)

function validate() {
  const e = {}
  if (!form.value.occurredAt) {
    e.occurredAt = '请选择事件时间'
  } else {
    const val = form.value.occurredAt
    if (val < minDate) e.occurredAt = '最早只能记录7天前'
    if (val > maxDate) e.occurredAt = '不能选择未来时间'
  }
  fieldErrors.value = e
  return Object.keys(e).length === 0
}

function submit() {
  fieldErrors.value = {}
  if (!validate()) return
  emit('submit', {
    content: form.value.content || undefined,
    photos: form.value.photos.length > 0 ? form.value.photos : undefined,
    occurredAt: form.value.occurredAt.replace('T', ' ') + ':00',
    isMilestone: form.value.isMilestone || undefined,
    milestoneLabel: form.value.isMilestone ? form.value.milestoneLabel || undefined : undefined,
    location: form.value.location || undefined
  })
}

async function onPhotoUpload(e) {
  const files = e.target.files
  if (!files || files.length === 0) return
  uploading.value = true
  try {
    for (const file of files) {
      if (form.value.photos.length >= 9) break
      const url = await uploadPhotoFile(file)
      form.value.photos.push(url)
    }
  } catch {} finally {
    uploading.value = false
    e.target.value = ''
  }
}

function removePhoto(index) {
  form.value.photos.splice(index, 1)
}

async function onPhotoAnalyze(e) {
  const file = e.target.files?.[0]
  if (!file) return
  analyzing.value = true
  try {
    const [res, url] = await Promise.all([
      analyzePhoto(file),
      uploadPhotoFile(file)
    ])
    form.value.content = res.data.description
    form.value.isMilestone = true
    form.value.milestoneLabel = res.data.label
    if (url && form.value.photos.length < 9) {
      form.value.photos.push(url)
    }
  } catch (err) {
    const msg = err?.response?.data?.message || '图片分析失败'
    showToast(msg, 'error')
  } finally {
    analyzing.value = false
    e.target.value = ''
  }
}

const milestoneLabels = ['到家日', '生日', '第一次打疫苗', '第一次洗澡', '绝育', '第一次', '特别纪念']
</script>

<template>
  <form class="moment-form" @submit.prevent="submit">
    <div class="field">
      <label class="field-label">事件时间 *</label>
      <input v-model="form.occurredAt" type="datetime-local" class="field-input" :min="minDate" :max="maxDate" :class="{ 'has-error': fieldErrors.occurredAt }" />
      <span class="field-err" v-if="fieldErrors.occurredAt">{{ fieldErrors.occurredAt }}</span>
    </div>

    <div class="field">
      <label class="field-label">
        写点什么
        <span class="ai-photo-label" v-if="analyzing"><ThemedIcon name="quill" :size="12" /> AI 正在看图...</span>
        <label class="ai-photo-btn" v-else>
          AI 识图创建
          <input type="file" accept="image/*" hidden @change="onPhotoAnalyze" />
        </label>
      </label>
      <textarea v-model="form.content" class="field-input field-textarea" placeholder="比如：奶茶第一次跳上窗台晒太阳，在阳光里打了个滚..." rows="3" maxlength="2000"></textarea>
      <span class="field-hint" v-if="form.content && form.isMilestone">AI 已根据照片自动填写，可手动修改</span>
    </div>

    <!-- 照片上传 -->
    <div class="field">
      <label class="field-label">
        照片
        <span class="photo-count" v-if="form.photos.length > 0">{{ form.photos.length }}/9</span>
      </label>
      <div class="photo-area">
        <div class="photo-thumb" v-for="(url, i) in form.photos" :key="i">
          <img :src="url" />
          <button type="button" class="photo-remove" @click="removePhoto(i)">&times;</button>
        </div>
        <label class="photo-add" v-if="form.photos.length < 9">
          <span v-if="uploading" class="photo-add-loading">
            <span class="spinner-sm"></span>
          </span>
          <span v-else class="photo-add-icon">+</span>
          <input type="file" accept="image/*" multiple hidden @change="onPhotoUpload" :disabled="uploading" />
        </label>
      </div>
    </div>

    <div class="field">
      <label class="field-label">地点</label>
      <input v-model="form.location" type="text" class="field-input" placeholder="比如：家里窗台" maxlength="100" />
    </div>

    <div class="field">
      <label class="field-checkbox">
        <input type="checkbox" v-model="form.isMilestone" />
        <span>标记为里程碑</span>
      </label>
    </div>

    <div class="field" v-if="form.isMilestone">
      <label class="field-label">里程碑标签</label>
      <div class="label-grid">
        <button
          v-for="label in milestoneLabels" :key="label"
          type="button"
          class="label-btn"
          :class="{ 'is-active': form.milestoneLabel === label }"
          @click="form.milestoneLabel = label"
        >{{ label }}</button>
      </div>
    </div>

    <div class="form-actions">
      <button type="button" class="btn-cancel" @click="emit('cancel')">取消</button>
      <button type="submit" class="btn-submit" :disabled="loading">
        <span v-if="loading" class="spinner"></span>
        <span v-else>保存</span>
      </button>
    </div>
  </form>
</template>

<style scoped>
.moment-form { display: flex; flex-direction: column; gap: 1rem; max-width: 480px; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field-label { font-size: 0.8rem; color: var(--text-secondary); letter-spacing: 0.04em; display: flex; align-items: center; gap: 12px; }
.ai-photo-btn {
  font-size: 0.75rem;
  color: var(--spot);
  cursor: pointer;
  padding: 2px 8px;
  border: 1px solid var(--spot);
  border-radius: var(--radius-full);
  transition: all var(--duration-fast);
}
.ai-photo-btn:hover { background: var(--spot); color: #fff; }
.ai-photo-label { font-size: 0.75rem; color: var(--text-muted); }
.photo-count { font-size: 0.72rem; color: var(--text-muted); margin-left: auto; }

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

/* 照片上传区 */
.photo-area {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.photo-thumb {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--border-subtle);
}
.photo-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.photo-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  border-radius: var(--radius-full);
  background: rgba(0, 0, 0, 0.55);
  border: none;
  color: #fff;
  font-size: 0.75rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  opacity: 0;
  transition: opacity var(--duration-fast);
}
.photo-thumb:hover .photo-remove { opacity: 1; }

.photo-add {
  width: 72px;
  height: 72px;
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color var(--duration-fast), background var(--duration-fast);
}
.photo-add:hover {
  border-color: var(--accent);
  background: var(--accent-surface);
}
.photo-add-icon {
  font-size: 1.4rem;
  color: var(--text-muted);
  line-height: 1;
}
.photo-add-loading {
  display: flex;
  align-items: center;
  justify-content: center;
}
.spinner-sm {
  width: 14px;
  height: 14px;
  border: 2px solid transparent;
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

.field-checkbox {
  display: flex; align-items: center; gap: 8px;
  font-size: 0.9rem; color: var(--text-secondary); cursor: pointer;
}
.field-checkbox input[type="checkbox"] { accent-color: var(--spot); }

.label-grid { display: flex; flex-wrap: wrap; gap: 6px; }
.label-btn {
  padding: 4px 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-full);
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.8rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}
.label-btn:hover { border-color: var(--spot); color: var(--spot); }
.label-btn.is-active {
  background: var(--spot);
  border-color: var(--spot);
  color: #fff;
}

.field-err { font-size: 0.78rem; color: var(--danger); }
.field-hint { font-size: 0.72rem; color: var(--accent); margin-top: 2px; }

.form-actions {
  display: flex; justify-content: flex-end; gap: 10px; margin-top: 0.5rem;
}
.btn-cancel {
  padding: 8px 20px;
  border: 1px solid var(--border-default);
  background: transparent;
  font-family: var(--font-body); font-size: 0.9rem;
  color: var(--text-secondary); border-radius: var(--radius-sm); cursor: pointer;
}
.btn-submit {
  padding: 8px 20px;
  border: none; border-radius: var(--radius-sm);
  background: var(--accent); color: var(--text-inverse);
  font-family: var(--font-body); font-size: 0.9rem; cursor: pointer;
  display: flex; align-items: center; gap: 8px;
  transition: background var(--duration-fast) var(--ease-out);
}
.btn-submit:hover:not(:disabled) { background: var(--accent-light); }
.btn-submit:disabled { opacity: 0.6; cursor: not-allowed; }

.spinner {
  width: 16px; height: 16px;
  border: 2px solid transparent; border-top-color: var(--text-inverse);
  border-radius: 50%; animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
