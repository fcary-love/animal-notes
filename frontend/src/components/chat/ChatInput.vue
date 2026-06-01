<script setup>
import { ref, onBeforeUnmount } from 'vue'

const emit = defineEmits(['send', 'sendImage'])
const text = ref('')

// Image state
const imageFile = ref(null)
const imagePreview = ref(null)

// Voice recognition state
const recording = ref(false)
const voiceSupported = typeof window !== 'undefined' && ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window)
let recognition = null

function send() {
  if (imageFile.value) {
    emit('sendImage', { file: imageFile.value, text: text.value.trim() })
    clearImage()
    text.value = ''
    return
  }
  const val = text.value.trim()
  if (!val) return
  emit('send', val)
  text.value = ''
}

function onImageSelect(e) {
  const file = e.target.files?.[0]
  if (!file) return
  imageFile.value = file
  const reader = new FileReader()
  reader.onload = (evt) => { imagePreview.value = evt.target.result }
  reader.readAsDataURL(file)
  e.target.value = ''
}

function clearImage() {
  imageFile.value = null
  imagePreview.value = null
}

function toggleVoice() {
  if (!voiceSupported) return
  if (recording.value) stopVoice()
  else startVoice()
}

function startVoice() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.continuous = false
  recognition.interimResults = true
  recognition.onresult = (event) => {
    let transcript = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      transcript += event.results[i][0].transcript
    }
    text.value = transcript
  }
  recognition.onerror = () => { recording.value = false }
  recognition.onend = () => { recording.value = false }
  recognition.start()
  recording.value = true
}

function stopVoice() {
  if (recognition) { recognition.stop(); recognition = null }
  recording.value = false
}

onBeforeUnmount(() => { stopVoice() })
</script>

<template>
  <div class="chat-input-container">
    <!-- Image preview -->
    <div class="image-preview-bar" v-if="imagePreview">
      <div class="image-preview-thumb">
        <img :src="imagePreview" />
        <button class="image-preview-remove" @click="clearImage">&times;</button>
      </div>
      <span class="image-preview-hint">可输入问题后发送</span>
    </div>

    <form class="chat-input-wrap" @submit.prevent="send">
      <label class="chat-image-btn" title="发送图片">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
          <circle cx="8.5" cy="8.5" r="1.5"/>
          <polyline points="21 15 16 10 5 21"/>
        </svg>
        <input type="file" accept="image/*" hidden @change="onImageSelect" />
      </label>
      <input v-model="text" class="chat-input" :placeholder="imageFile ? '描述一下这张照片...' : '输入你的宠物问题...'" maxlength="500" />
      <button
        v-if="voiceSupported"
        type="button"
        class="chat-mic"
        :class="{ recording }"
        @click="toggleVoice"
        :title="recording ? '点击停止' : '点击说话'"
      >
        <svg v-if="!recording" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
          <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
          <line x1="12" y1="19" x2="12" y2="23"/>
          <line x1="8" y1="23" x2="16" y2="23"/>
        </svg>
        <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
          <rect x="6" y="6" width="12" height="12" rx="2"/>
        </svg>
      </button>
      <button type="submit" class="chat-send" :disabled="!text.trim() && !imageFile">发送</button>
    </form>
  </div>
</template>

<style scoped>
.chat-input-container {
  border-top: 1px solid var(--border-subtle);
  background: rgba(255, 254, 249, 0.92);
}
.image-preview-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px 0;
}
.image-preview-thumb {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  flex-shrink: 0;
}
.image-preview-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.image-preview-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(0,0,0,0.55);
  border: none;
  color: #fff;
  font-size: 0.75rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}
.image-preview-hint {
  font-size: 0.72rem;
  color: var(--text-muted);
}
.chat-input-wrap {
  display: flex; gap: 6px; padding: 10px 12px;
  overflow: hidden;
}
.chat-image-btn {
  width: 36px;
  height: 36px;
  border: 1px solid var(--border-default);
  border-radius: 50%;
  background: transparent;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--duration-fast);
}
.chat-image-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.chat-input {
  flex: 1;
  min-width: 0;
  padding: 8px 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-full);
  font-family: var(--font-body);
  font-size: 0.85rem;
  background: var(--bg-input);
  color: var(--text-primary);
  outline: none;
}
.chat-input:focus { border-color: var(--accent); }
.chat-mic {
  width: 36px;
  height: 36px;
  border: 1px solid var(--border-default);
  border-radius: 50%;
  background: transparent;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--duration-fast);
}
.chat-mic:hover { border-color: var(--accent); color: var(--accent); }
.chat-mic.recording {
  border-color: var(--danger);
  color: var(--danger);
  animation: pulse 1.2s ease-in-out infinite;
  background: rgba(181, 83, 61, 0.06);
}
@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}
.chat-send {
  padding: 8px 16px;
  border: none;
  border-radius: var(--radius-full);
  background: var(--accent);
  color: var(--text-inverse);
  font-family: var(--font-body);
  font-size: 0.85rem;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: background var(--duration-fast);
}
.chat-send:hover:not(:disabled) { background: var(--accent-light); }
.chat-send:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
