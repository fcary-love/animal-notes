<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { gsap } from 'gsap'
import ChatMessage from '../components/chat/ChatMessage.vue'
import ChatInput from '../components/chat/ChatInput.vue'
import ThemedIcon from '../components/common/ThemedIcon.vue'
import { STORAGE_KEYS, getApiBase } from '../utils/constants'
import { usePets } from '../composables/usePets'
import { createSession, getSessions, getSessionMessages, deleteSession } from '../api/chat'
import { analyzePhoto } from '../api/ai'
import { getAgentTasks } from '../api/agent'

const { pets, fetchPets } = usePets()
const selectedPetId = ref(null)
const selectedPet = computed(() => pets.value.find(p => p.id === selectedPetId.value) || pets.value[0])
const petName = computed(() => selectedPet.value?.name || '它')
const quickQuestions = computed(() => [
  `${petName.value}多久打一次疫苗比较好？`,
  `${petName.value}的日常饮食要注意什么？`,
  `怎么判断${petName.value}是否健康？`,
  `${petName.value}最近不爱吃饭怎么办？`
])

async function switchPet(petId) {
  if (selectedPetId.value === petId) return
  selectedPetId.value = petId
  if (ttsSupported) speechSynthesis.cancel()
  messages.value = []
  await fetchSessions()
  if (sessions.value.length > 0) {
    await selectSession(sessions.value[0])
  } else {
    await onCreateSession()
    generateProactiveMessage()
  }
}

const messages = ref([])
const loading = ref(false)
const msgList = ref(null)
const pageRef = ref(null)
let motionMatchMedia = null

// TTS
const ttsEnabled = ref(false)
const ttsSupported = typeof window !== 'undefined' && 'speechSynthesis' in window
let currentUtterance = null

function toggleTts() {
  ttsEnabled.value = !ttsEnabled.value
  if (!ttsEnabled.value && ttsSupported) {
    speechSynthesis.cancel()
  }
}

function speakText(text) {
  if (!ttsEnabled.value || !ttsSupported || !text) return
  speechSynthesis.cancel()
  const cleaned = text.replace(/[*#`>\-\[\]()]/g, '').replace(/\n+/g, '。').trim()
  if (!cleaned) return
  currentUtterance = new SpeechSynthesisUtterance(cleaned)
  currentUtterance.lang = 'zh-CN'
  currentUtterance.rate = 1.0
  currentUtterance.pitch = 1.0
  const zhVoice = speechSynthesis.getVoices().find(v => v.lang.startsWith('zh'))
  if (zhVoice) currentUtterance.voice = zhVoice
  speechSynthesis.speak(currentUtterance)
}

async function generateProactiveMessage() {
  if (messages.value.length > 0 || !selectedPet.value) return
  try {
    const res = await getAgentTasks(selectedPet.value.id)
    const tasks = res.data || []
    if (tasks.length === 0) return

    const name = selectedPet.value?.name || '你的宠物'
    const topTask = tasks[0]
    const greetings = {
      record_moment: `好久没记录了，最近${name}还好吗？来写一笔吧~`,
      add_photo: `${name}最近没有新照片了，拍一张发给我看看？`,
      health_check: `该带${name}去做健康检查了哦，记得安排一下~`,
      weight_record: `${name}有一阵没称体重了，记录一下看看有没有变化？`,
      diet_record: `最近${name}的饮食怎么样？来记一下今天吃了什么吧`,
      play_interact: `${name}该活动活动了！有空陪它玩一会儿~`,
      suggest_milestone: `看了${name}的记录，发现有几个值得标记为里程碑的时刻，去看看？`,
      growth_report: `${name}的记录积累了不少，要不要生成一份成长报告看看？`,
      daily_pet: `今天也是${name}可爱的一天呢~`
    }
    const greeting = greetings[topTask.id] || `嗨！关于${name}，我有些小建议想和你聊聊~`

    messages.value.push({ role: 'assistant', content: greeting })
    scrollToBottom()
    speakText(greeting)
  } catch {}
}

// Session management
const sessions = ref([])
const currentSession = ref(null)
const menuOpen = ref(null)

async function fetchSessions() {
  try {
    const res = await getSessions(selectedPet.value?.id)
    sessions.value = res.data || []
  } catch {}
}

async function selectSession(session) {
  currentSession.value = session
  menuOpen.value = null
  if (ttsSupported) speechSynthesis.cancel()
  try {
    const res = await getSessionMessages(session.id)
    messages.value = (res.data || []).map(m => ({ role: m.role, content: m.content }))
  } catch {
    messages.value = []
  }
}

async function onCreateSession() {
  try {
    const res = await createSession(selectedPet.value?.id)
    const session = res.data
    sessions.value.unshift(session)
    currentSession.value = session
    messages.value = []
  } catch {}
}

async function onDeleteSession(session) {
  try {
    await deleteSession(session.id)
    sessions.value = sessions.value.filter(s => s.id !== session.id)
    if (currentSession.value?.id === session.id) {
      if (sessions.value.length > 0) {
        selectSession(sessions.value[0])
      } else {
        onCreateSession()
      }
    }
    menuOpen.value = null
  } catch {}
}

function onExportSession(session) {
  getSessionMessages(session.id).then(res => {
    const msgs = res.data || []
    const text = msgs.map(m => {
      const role = m.role === 'user' ? '我' : '时光助手'
      return `[${role}]\n${m.content}`
    }).join('\n\n---\n\n')
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${session.title || '对话'}.txt`
    a.click()
    URL.revokeObjectURL(url)
    menuOpen.value = null
  })
}

function toggleMenu(sessionId) {
  menuOpen.value = menuOpen.value === sessionId ? null : sessionId
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diffMs = now - d
  const diffDays = Math.floor(diffMs / 86400000)
  if (diffDays === 0) {
    return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
  }
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays}天前`
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function initMotion() {
  if (!pageRef.value) return
  motionMatchMedia?.revert()
  motionMatchMedia = gsap.matchMedia()
  motionMatchMedia.add(
    { reduceMotion: '(prefers-reduced-motion: reduce)' },
    (context) => {
      if (context.conditions.reduceMotion) {
        gsap.set(pageRef.value.querySelectorAll('.chat-animate'), { autoAlpha: 1, clearProps: 'transform' })
        return undefined
      }
      const tl = gsap.timeline({ defaults: { duration: 0.62, ease: 'power3.out' } })
      tl.from('.chat-hero', { autoAlpha: 0, y: 24 })
        .from('.chat-main', { autoAlpha: 0, y: 22 }, '-=0.34')
        .from('.quick-btn', { autoAlpha: 0, y: 12, stagger: 0.05 }, '-=0.28')
      return () => tl.kill()
    },
    pageRef.value
  )
}

function scrollToBottom() {
  nextTick(() => {
    if (msgList.value) {
      msgList.value.scrollTop = msgList.value.scrollHeight
    }
  })
}

async function onSend(text) {
  if (!text.trim() || loading.value) return
  if (!currentSession.value) await onCreateSession()

  messages.value.push({ role: 'user', content: text })
  messages.value.push({ role: 'assistant', content: '' })
  loading.value = true
  scrollToBottom()

  const lastMsg = messages.value[messages.value.length - 1]
  const token = localStorage.getItem(STORAGE_KEYS.TOKEN)

  try {
    const resp = await fetch(`${getApiBase()}/api/v1/ai/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        question: text,
        petId: selectedPet.value?.id,
        sessionId: currentSession.value?.id
      })
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

    // Refresh session title after first message
    fetchSessions()
  } catch (e) {
    if (!lastMsg.content) {
      lastMsg.content = '抱歉，时光助手暂时无法响应。请稍后再试。'
    }
  } finally {
    loading.value = false
    scrollToBottom()
    if (lastMsg.content && !lastMsg.content.includes('暂时无法响应')) {
      speakText(lastMsg.content)
    }
  }
}

async function onSendImage({ file, text: userText }) {
  if (!currentSession.value) await onCreateSession()
  if (!currentSession.value) return

  loading.value = true

  // Add user message with image indicator
  const questionText = userText || '看看这张照片'
  messages.value.push({ role: 'user', content: `📷 ${questionText}` })
  messages.value.push({ role: 'assistant', content: '' })
  scrollToBottom()

  const lastMsg = messages.value[messages.value.length - 1]

  try {
    // Step 1: Analyze image with the photo service
    const visionRes = await analyzePhoto(file)
    const visionDesc = visionRes.data?.description || '一张宠物照片'
    const visionLabel = visionRes.data?.label || ''

    // Step 2: Send combined question to chat
    const combinedQuestion = userText
      ? `[用户发了一张照片，照片识别结果：${visionDesc}${visionLabel ? '，标签：' + visionLabel : ''}] 用户问题：${userText}`
      : `[用户发了一张照片] 请根据照片内容聊聊：${visionDesc}`

    const token = localStorage.getItem(STORAGE_KEYS.TOKEN)
    const resp = await fetch(`${getApiBase()}/api/v1/ai/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        question: combinedQuestion,
        petId: selectedPet.value?.id,
        sessionId: currentSession.value?.id
      })
    })

    if (!resp.ok) {
      lastMsg.content = resp.status === 401 ? '登录已过期' : `请求失败 (${resp.status})`
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

    fetchSessions()
  } catch (e) {
    if (!lastMsg.content) lastMsg.content = '图片分析失败，请重试。'
  } finally {
    loading.value = false
    scrollToBottom()
    if (lastMsg.content && !lastMsg.content.includes('失败')) {
      speakText(lastMsg.content)
    }
  }
}

// Close menu on outside click
function handleDocClick(e) {
  if (!e.target.closest('.session-menu-wrap')) {
    menuOpen.value = null
  }
}

onMounted(async () => {
  await fetchPets()
  if (pets.value.length > 0) selectedPetId.value = pets.value[0].id
  await fetchSessions()
  if (sessions.value.length > 0) {
    await selectSession(sessions.value[0])
  } else {
    await onCreateSession()
  }
  initMotion()
  document.addEventListener('click', handleDocClick)
  // Proactive greeting for empty sessions
  if (messages.value.length === 0) {
    generateProactiveMessage()
  }
})
onBeforeUnmount(() => {
  motionMatchMedia?.revert()
  document.removeEventListener('click', handleDocClick)
})
</script>

<template>
  <div ref="pageRef" class="chat-page">
    <section class="chat-hero chat-animate">
      <div>
        <p class="kicker">时光顾问</p>
        <h1><ThemedIcon name="quill" :size="22" /> 时光顾问</h1>
        <p>把喂养、健康、训练这些问题，整理成一封可以慢慢读的回信。</p>
      </div>
      <div class="hero-actions">
        <button
          v-if="ttsSupported"
          class="tts-btn"
          :class="{ active: ttsEnabled }"
          @click="toggleTts"
          :title="ttsEnabled ? '关闭语音播报' : '开启语音播报'"
        >
          <svg v-if="ttsEnabled" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/>
            <path d="M19.07 4.93a10 10 0 0 1 0 14.14"/>
            <path d="M15.54 8.46a5 5 0 0 1 0 7.07"/>
          </svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/>
            <line x1="23" y1="9" x2="17" y2="15"/>
            <line x1="17" y1="9" x2="23" y2="15"/>
          </svg>
        </button>
      </div>
    </section>

    <!-- Pet tabs -->
    <div class="pet-tabs chat-animate" v-if="pets.length > 1">
      <button
        v-for="pet in pets"
        :key="pet.id"
        class="pet-tab"
        :class="{ active: selectedPet?.id === pet.id }"
        @click="switchPet(pet.id)"
      >
        <img v-if="pet.avatarUrl" :src="pet.avatarUrl" class="pet-tab-avatar" />
        <span v-else class="pet-tab-avatar pet-tab-placeholder">{{ pet.name?.charAt(0) }}</span>
        <span>{{ pet.name }}</span>
      </button>
    </div>

    <section class="chat-main chat-animate">
      <!-- Session sidebar -->
      <aside class="session-sidebar">
        <button class="new-chat-btn" @click="onCreateSession">
          <ThemedIcon name="quill" :size="14" /> 新对话
        </button>
        <div class="session-list">
          <div
            v-for="s in sessions"
            :key="s.id"
            class="session-item"
            :class="{ active: currentSession?.id === s.id }"
            @click="selectSession(s)"
          >
            <div class="session-info">
              <span class="session-title">{{ s.title || '新对话' }}</span>
              <span class="session-time">{{ formatDate(s.updatedAt || s.createdAt) }}</span>
            </div>
            <div class="session-menu-wrap">
              <button class="session-menu-btn" @click.stop="toggleMenu(s.id)">...</button>
              <div class="session-menu" v-if="menuOpen === s.id">
                <button @click.stop="onExportSession(s)">导出</button>
                <button @click.stop="onDeleteSession(s)" class="menu-danger">删除</button>
              </div>
            </div>
          </div>
          <div class="session-empty" v-if="sessions.length === 0">
            <p>暂无对话</p>
          </div>
        </div>
      </aside>

      <!-- Chat area -->
      <div class="chat-area">
        <div class="chat-messages" ref="msgList">
          <div class="chat-welcome" v-if="messages.length === 0">
            <div class="welcome-icon"><ThemedIcon name="paw" :size="42" /></div>
            <h3>你好，我在这里陪你一起照顾它</h3>
            <p>可以问我关于宠物喂养、健康护理、行为训练等问题。</p>
            <div class="quick-questions">
              <button
                v-for="q in quickQuestions"
                :key="q"
                class="quick-btn"
                @click="onSend(q)"
              >{{ q }}</button>
            </div>
          </div>
          <ChatMessage
            v-for="(m, i) in messages"
            :key="i"
            :role="m.role"
            :content="m.content"
          />
          <div class="chat-typing" v-if="loading">
            <span class="spinner"></span>
            <span>正在翻阅知识库...</span>
          </div>
        </div>
        <ChatInput @send="onSend" @send-image="onSendImage" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.chat-page {
  flex: 1;
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding: 1.45rem 1.5rem 2rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.chat-animate,
.chat-hero,
.chat-main {
  will-change: transform, opacity;
}
.kicker {
  margin: 0 0 0.35rem;
  color: var(--accent);
  font-size: 0.76rem;
}
.chat-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.2rem;
  padding: 1.1rem 0 1.15rem;
  margin-bottom: 0.65rem;
  border-top: 2px solid var(--accent);
  border-bottom: 1px solid var(--border-subtle);
}
.chat-hero > div:first-child { flex: 1; min-width: 0; }
.chat-hero h1 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.7rem;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: 0;
  color: var(--text-primary);
}
.chat-hero p:last-child {
  margin: 0.3rem 0 0;
  font-size: 0.82rem;
  color: var(--text-muted);
  line-height: 1.6;
}
.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.tts-btn {
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
  transition: all var(--duration-fast);
}
.tts-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.tts-btn.active {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-surface);
}

/* Pet tabs */
.pet-tabs {
  display: flex;
  gap: 6px;
  padding: 0 0 0.6rem;
  overflow-x: auto;
}
.pet-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--text-secondary);
  font-family: var(--font-body);
  font-size: 0.82rem;
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--duration-fast);
}
.pet-tab:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.pet-tab.active {
  border-color: var(--accent);
  background: var(--accent-surface);
  color: var(--accent);
  font-weight: 600;
}
.pet-tab-avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  object-fit: cover;
}
.pet-tab-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-surface);
  color: var(--accent);
  font-size: 0.72rem;
  font-weight: 600;
}

/* Main layout */
.chat-main {
  min-height: min(680px, calc(100vh - 260px));
  display: flex;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  background: linear-gradient(180deg, rgba(255, 254, 249, 0.92), rgba(250, 247, 240, 0.82));
  box-shadow: 0 10px 26px rgba(61, 50, 38, 0.07);
  overflow: hidden;
}

/* Session sidebar */
.session-sidebar {
  width: 240px;
  flex-shrink: 0;
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  background: rgba(255, 254, 249, 0.5);
}
.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 12px;
  padding: 9px 0;
  border: 1px dashed var(--accent);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--accent);
  font-family: var(--font-body);
  font-size: 0.82rem;
  cursor: pointer;
  transition: background var(--duration-fast);
}
.new-chat-btn:hover { background: var(--accent-surface); }
.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
}
.session-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 9px 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background var(--duration-fast);
  position: relative;
}
.session-item:hover { background: rgba(92, 122, 94, 0.06); }
.session-item.active {
  background: var(--accent-surface);
  border: 1px solid rgba(92, 122, 94, 0.18);
}
.session-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.session-title {
  font-size: 0.82rem;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.session-time {
  font-size: 0.68rem;
  color: var(--text-muted);
}
.session-menu-wrap {
  position: relative;
  flex-shrink: 0;
}
.session-menu-btn {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-muted);
  font-size: 0.9rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity var(--duration-fast), background var(--duration-fast);
}
.session-item:hover .session-menu-btn { opacity: 1; }
.session-menu-btn:hover { background: rgba(92, 122, 94, 0.1); }
.session-menu {
  position: absolute;
  right: 0;
  top: 100%;
  z-index: 10;
  min-width: 80px;
  padding: 4px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  background: var(--bg-surface);
  box-shadow: 0 4px 12px rgba(61, 50, 38, 0.12);
}
.session-menu button {
  display: block;
  width: 100%;
  padding: 6px 10px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-family: var(--font-body);
  font-size: 0.78rem;
  text-align: left;
  cursor: pointer;
}
.session-menu button:hover { background: rgba(92, 122, 94, 0.06); }
.menu-danger { color: var(--danger); }
.menu-danger:hover { background: rgba(181, 83, 61, 0.06); }
.session-empty {
  text-align: center;
  padding: 2rem 0;
  color: var(--text-muted);
  font-size: 0.78rem;
}

/* Chat area */
.chat-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.chat-messages {
  flex: 1;
  padding: 1.5rem;
  overflow-y: auto;
  max-height: calc(100vh - 310px);
}
.chat-welcome {
  text-align: center;
  padding: 3rem 1rem;
}
.welcome-icon {
  width: 74px;
  height: 74px;
  margin: 0 auto 0.9rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(92, 122, 94, 0.22);
  border-radius: 42% 58% 50% 50%;
  color: var(--accent);
  background: rgba(92, 122, 94, 0.06);
}
.chat-welcome h3 {
  margin: 0 0 0.5rem;
  font-family: var(--font-display);
  color: var(--text-primary);
}
.chat-welcome p {
  color: var(--text-muted);
  font-size: 0.9rem;
  margin-bottom: 1.4rem;
}
.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}
.quick-btn {
  padding: 8px 15px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-full);
  background: rgba(255, 254, 249, 0.78);
  font-family: var(--font-body);
  font-size: 0.82rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: border-color var(--duration-fast), color var(--duration-fast), transform var(--duration-fast);
}
.quick-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
  transform: translateY(-1px);
}
.chat-typing {
  font-size: 0.82rem;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0.5rem 0;
}
.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--border-default);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 720px) {
  .session-sidebar { width: 180px; }
  .chat-messages { padding: 1rem; }
}
@media (max-width: 620px) {
  .chat-page { padding: 1rem; }
  .chat-main { flex-direction: column; }
  .session-sidebar {
    width: 100%;
    max-height: 160px;
    border-right: none;
    border-bottom: 1px solid var(--border-subtle);
  }
  .session-list { display: flex; gap: 4px; overflow-x: auto; padding: 0 8px 8px; }
  .session-item { flex-shrink: 0; min-width: 120px; }
}
@media (prefers-reduced-motion: reduce) {
  .spinner { animation: none; }
}
</style>
