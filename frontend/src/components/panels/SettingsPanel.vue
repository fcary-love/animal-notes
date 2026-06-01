<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { usePets } from '../../composables/usePets'
import { useAuth } from '../../composables/useAuth'
import ThemedIcon from '../common/ThemedIcon.vue'

const loaded = ref(false)
const { pets } = usePets()
const { currentUser } = useAuth()

const settings = ref({
  notify: true,
  sound: false,
  darkMode: false,
  language: 'zh',
  autoBackup: true,
})

const saveMsg = ref('')

function loadSettings() {
  try {
    const saved = JSON.parse(localStorage.getItem('animal-notes:settings') || '{}')
    settings.value = { ...settings.value, ...saved }
  } catch { /* ignore */ }
}

function saveSettings() {
  localStorage.setItem('animal-notes:settings', JSON.stringify(settings.value))
}

function toggle(key) {
  settings.value[key] = !settings.value[key]
}

function exportData() {
  const payload = {
    exportedAt: new Date().toISOString(),
    user: currentUser.value ? { username: currentUser.value.username, nickname: currentUser.value.nickname } : null,
    pets: pets.value,
    settings: settings.value
  }
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `animal-notes-export-${new Date().toISOString().slice(0, 10)}.json`
  a.click()
  URL.revokeObjectURL(url)
  saveMsg.value = '已导出当前可用数据'
}

function clearCache() {
  Object.keys(localStorage)
    .filter(key => key.startsWith('animal-notes:todos') || key === 'animal-notes:settings')
    .forEach(key => localStorage.removeItem(key))
  loadSettings()
  saveMsg.value = '本地缓存已清除'
}

onMounted(() => {
  loadSettings()
  nextTick(() => { loaded.value = true })
})
watch(settings, saveSettings, { deep: true })
</script>

<template>
  <div class="panel-content" :class="{ loaded }">
    <!-- Notification -->
    <section class="set-section">
      <div class="section-label">通知偏好</div>
      <div class="set-group">
        <div class="set-row">
          <div class="set-row-info">
            <span class="set-row-title">推送通知</span>
            <span class="set-row-desc">喂食、待办提醒等消息推送</span>
          </div>
          <button class="set-toggle" :class="{ on: settings.notify }" @click="toggle('notify')">
            <span class="set-toggle-knob"></span>
          </button>
        </div>
        <div class="set-row">
          <div class="set-row-info">
            <span class="set-row-title">提示音效</span>
            <span class="set-row-desc">操作反馈和提醒音效</span>
          </div>
          <button class="set-toggle" :class="{ on: settings.sound }" @click="toggle('sound')">
            <span class="set-toggle-knob"></span>
          </button>
        </div>
      </div>
    </section>

    <!-- Appearance -->
    <section class="set-section">
      <div class="section-label">外观设置</div>
      <div class="set-group">
        <div class="set-row">
          <div class="set-row-info">
            <span class="set-row-title">深色模式</span>
            <span class="set-row-desc">降低亮度，保护眼睛</span>
          </div>
          <button class="set-toggle" :class="{ on: settings.darkMode }" @click="toggle('darkMode')">
            <span class="set-toggle-knob"></span>
          </button>
        </div>
      </div>
    </section>

    <!-- Data -->
    <section class="set-section">
      <div class="section-label">数据管理</div>
      <div class="set-group">
        <div class="set-row">
          <div class="set-row-info">
            <span class="set-row-title">自动备份</span>
            <span class="set-row-desc">每日自动备份数据到云端</span>
          </div>
          <button class="set-toggle" :class="{ on: settings.autoBackup }" @click="toggle('autoBackup')">
            <span class="set-toggle-knob"></span>
          </button>
        </div>
        <button class="set-row clickable" @click="exportData">
          <div class="set-row-info">
            <span class="set-row-title">导出数据</span>
            <span class="set-row-desc">导出所有记录为 JSON 文件</span>
          </div>
          <span class="set-arrow">→</span>
        </button>
        <button class="set-row clickable" @click="clearCache">
          <div class="set-row-info">
            <span class="set-row-title">清除缓存</span>
            <span class="set-row-desc">清除本地缓存数据</span>
          </div>
          <span class="set-arrow">→</span>
        </button>
      </div>
      <p class="save-msg" v-if="saveMsg">{{ saveMsg }}</p>
    </section>

    <!-- About -->
    <section class="set-section">
      <div class="section-label">关于</div>
      <div class="set-about">
        <ThemedIcon name="paw" :size="32" class="set-about-logo" />
        <span class="set-about-name">宠物时光手帐</span>
        <span class="set-about-ver">v1.0.0</span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.panel-content { opacity: 0; transform: translateY(12px); transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1); }
.panel-content.loaded { opacity: 1; transform: translateY(0); }

.set-section { margin-bottom: 22px; }

.section-label {
  font-family: var(--font-display);
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--text-muted);
  letter-spacing: 0.06em;
  margin-bottom: 10px;
}

.set-group {
  background: var(--bg-warm);
  border: 1px solid var(--border-subtle);
  border-radius: 14px;
  overflow: hidden;
}
.set-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-subtle);
  transition: background var(--duration-fast);
  background: transparent;
  font-family: var(--font-body);
  border-left: none;
  border-right: none;
  border-top: none;
  text-align: left;
}
.set-row:last-child { border-bottom: none; }
.set-row.clickable { cursor: pointer; }
.set-row.clickable:hover { background: var(--accent-surface); }

.set-row-info { flex: 1; min-width: 0; }
.set-row-title { display: block; font-size: 0.82rem; color: var(--text-primary); font-weight: 500; }
.set-row-desc { display: block; font-size: 0.68rem; color: var(--text-muted); margin-top: 2px; }

.set-arrow { font-size: 0.85rem; color: var(--text-muted); }

/* Toggle switch */
.set-toggle {
  width: 42px;
  height: 24px;
  border-radius: 12px;
  border: 1.5px solid var(--border-default);
  background: var(--bg-surface);
  cursor: pointer;
  position: relative;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  flex-shrink: 0;
  padding: 0;
}
.set-toggle.on {
  background: var(--accent);
  border-color: var(--accent);
}
.set-toggle-knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.15);
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
.set-toggle.on .set-toggle-knob {
  transform: translateX(18px);
}

/* About */
.set-about {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 24px;
  background: var(--bg-warm);
  border: 1px solid var(--border-subtle);
  border-radius: 14px;
}
.set-about-logo { color: var(--accent); }
.set-about-name { font-family: var(--font-display); font-size: 0.95rem; font-weight: 700; color: var(--text-primary); }
.set-about-ver { font-family: var(--font-mono); font-size: 0.65rem; color: var(--text-muted); }
.save-msg { margin: 8px 0 0; color: var(--accent); font-size: 0.72rem; text-align: center; }
</style>
