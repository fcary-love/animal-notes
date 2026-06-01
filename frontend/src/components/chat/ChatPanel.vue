<script setup>
import { ref, nextTick } from 'vue'
import ChatMessage from './ChatMessage.vue'
import ChatInput from './ChatInput.vue'
import ThemedIcon from '../common/ThemedIcon.vue'
import { STORAGE_KEYS } from '../../utils/constants'

const collapsed = ref(true)
const messages = ref([])
const loading = ref(false)
const msgList = ref(null)

function toggle() { collapsed.value = !collapsed.value }

function scrollToBottom() {
  nextTick(() => {
    if (msgList.value) msgList.value.scrollTop = msgList.value.scrollHeight
  })
}

async function onSend(text) {
  if (!text.trim() || loading.value) return

  messages.value.push({ role: 'user', content: text })
  messages.value.push({ role: 'assistant', content: '' })
  loading.value = true
  scrollToBottom()

  const lastMsg = messages.value[messages.value.length - 1]
  const token = localStorage.getItem(STORAGE_KEYS.TOKEN)

  try {
    const resp = await fetch('/api/v1/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ question: text })
    })

    if (!resp.ok) {
      lastMsg.content = resp.status === 401
        ? '登录已过期，请重新登录'
        : `请求失败 (${resp.status})`
      return
    }

    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''

      for (const evt of events) {
        for (const line of evt.split('\n')) {
          const trimmed = line.trim()
          if (trimmed.startsWith('data:')) {
            const payload = trimmed.substring(5).trim()
            if (payload && !payload.includes('[ERROR]') && payload !== '[DONE]') {
              lastMsg.content += payload
              scrollToBottom()
            }
          }
        }
      }
    }

    if (buffer.trim()) {
      for (const line of buffer.split('\n')) {
        const trimmed = line.trim()
        if (trimmed.startsWith('data:')) {
          const payload = trimmed.substring(5).trim()
          if (payload && !payload.includes('[ERROR]') && payload !== '[DONE]') {
            lastMsg.content += payload
          }
        }
      }
    }
  } catch {
    const last = messages.value[messages.value.length - 1]
    if (!last.content) last.content = '抱歉，时光助手暂时无法响应。'
  } finally {
    loading.value = false
    scrollToBottom()
  }
}
</script>

<template>
  <div class="chat-panel" :class="{ collapsed }">
    <button class="chat-toggle" @click="toggle">
      <ThemedIcon name="quill" :size="14" />
      {{ collapsed ? '宠物管家' : '收起' }}
    </button>
    <div class="chat-body" v-if="!collapsed">
      <div class="chat-messages" ref="msgList">
        <div class="chat-welcome" v-if="messages.length === 0">
          <p><ThemedIcon name="paw" :size="14" /> 你好！我是时光助手，可以回答宠物喂养、健康、训练等问题。</p>
        </div>
        <ChatMessage v-for="(m, i) in messages" :key="i" :role="m.role" :content="m.content" />
        <div class="chat-typing" v-if="loading">
          <span class="spinner"></span> 思考中...
        </div>
      </div>
      <ChatInput @send="onSend" />
    </div>
  </div>
</template>

<style scoped>
.chat-panel {
  position: fixed; right: 20px; bottom: 20px; z-index: 1000;
  width: 400px; background: var(--bg-surface);
  border: 1px solid var(--border-default); border-radius: var(--radius-md);
  box-shadow: 0 4px 24px rgba(61, 50, 43, 0.15);
  transition: all var(--duration-fast) var(--ease-out);
}
.chat-panel.collapsed { width: auto; }
.chat-toggle {
  display: flex; align-items: center; gap: 6px;
  width: 100%; padding: 10px 16px; border: none;
  background: var(--accent); color: var(--text-inverse);
  font-family: var(--font-body); font-size: 0.88rem; font-weight: 600;
  cursor: pointer; border-radius: var(--radius-md);
  transition: background var(--duration-fast);
}
.chat-panel.collapsed .chat-toggle { border-radius: var(--radius-md); }
.chat-toggle:hover { background: var(--accent-light); }
.chat-body {
  display: flex; flex-direction: column; max-height: 420px;
}
.chat-messages {
  flex: 1; overflow-y: auto; padding: 1rem; max-height: 320px;
}
.chat-welcome {
  font-size: 0.85rem; color: var(--text-muted);
  text-align: center; padding: 1rem 0; line-height: 1.6;
}
.chat-typing {
  font-size: 0.8rem; color: var(--text-muted);
  display: flex; align-items: center; gap: 6px; padding: 0.5rem 0;
}
.spinner {
  width: 14px; height: 14px;
  border: 2px solid var(--border-default);
  border-top-color: var(--accent); border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
