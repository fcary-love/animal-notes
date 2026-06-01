<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuth } from '../../composables/useAuth'
import { usePets } from '../../composables/usePets'
import ThemedIcon from '../common/ThemedIcon.vue'

const { currentUser } = useAuth()
const { pets, fetchPets } = usePets()
const loaded = ref(false)
const copied = ref(false)

const displayName = computed(() => currentUser.value?.nickname || currentUser.value?.username || '当前用户')
const inviteCode = computed(() => {
  const seed = String(currentUser.value?.id || currentUser.value?.username || 'PET')
  return `PET-${seed.slice(0, 4).toUpperCase()}-${pets.value.length || 1}`
})
const members = computed(() => [
  { id: 'me', name: displayName.value, role: '档案创建者', active: true }
])

async function copyCode() {
  await navigator.clipboard?.writeText(inviteCode.value)
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}

onMounted(async () => {
  if (pets.value.length === 0) await fetchPets()
  loaded.value = true
})
</script>

<template>
  <div class="panel-content" :class="{ loaded }">
    <section class="fam-section">
      <div class="section-label">家庭成员</div>
      <div class="fam-list">
        <div v-for="(m, idx) in members" :key="m.id" class="fam-card" :style="{ '--i': idx }">
          <div class="fam-avatar">
            <ThemedIcon name="user" :size="22" />
            <div v-if="m.active" class="fam-online"></div>
          </div>
          <div class="fam-info">
            <span class="fam-name">{{ m.name }}</span>
            <span class="fam-role">{{ m.role }}</span>
          </div>
          <span class="fam-status active">在线</span>
        </div>
      </div>
    </section>

    <section class="fam-section">
      <div class="section-label">共同照护范围</div>
      <div class="fam-summary">
        <div><strong>{{ pets.length }}</strong><span>宠物档案</span></div>
        <div><strong>{{ members.length }}</strong><span>成员</span></div>
      </div>
    </section>

    <section class="fam-section">
      <div class="section-label">邀请家人</div>
      <div class="fam-invite">
        <div class="fam-invite-code">
          <span class="fam-code-label">邀请码</span>
          <span class="fam-code-value">{{ inviteCode }}</span>
        </div>
        <button class="fam-copy-btn" @click="copyCode">
          {{ copied ? '已复制' : '复制' }}
        </button>
      </div>
      <p class="fam-invite-hint">当前版本先提供邀请码展示与复制，成员加入接口接通后会自动显示真实成员列表。</p>
    </section>

    <section class="fam-section">
      <div class="section-label">权限说明</div>
      <div class="fam-perms">
        <div class="fam-perm"><ThemedIcon name="quill" :size="16" /><span>成员可协作记录时间线和日常照护</span></div>
        <div class="fam-perm"><ThemedIcon name="bell" :size="16" /><span>待办提醒会作为家庭照护清单同步展示</span></div>
        <div class="fam-perm"><ThemedIcon name="folder" :size="16" /><span>宠物删除与核心档案管理保留给创建者</span></div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.panel-content { opacity: 0; transform: translateY(12px); transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1); }
.panel-content.loaded { opacity: 1; transform: translateY(0); }
.fam-section { margin-bottom: 22px; }
.section-label { font-family: var(--font-display); font-size: 0.78rem; color: var(--text-muted); margin-bottom: 10px; }
.fam-list { display: flex; flex-direction: column; gap: 8px; }
.fam-card {
  display: flex; align-items: center; gap: 12px; padding: 14px 16px;
  background: var(--bg-warm); border: 1px solid var(--border-subtle); border-radius: var(--radius-lg);
}
.fam-avatar {
  width: 44px; height: 44px; border-radius: 42% 58% 50% 50%; border: 2px solid var(--accent-glow);
  background: var(--bg-surface); display: flex; align-items: center; justify-content: center; color: var(--accent); position: relative; flex-shrink: 0;
}
.fam-online { position: absolute; bottom: 0; right: 0; width: 10px; height: 10px; border-radius: 50%; background: var(--success); border: 2px solid var(--bg-surface); }
.fam-info { flex: 1; min-width: 0; }
.fam-name { display: block; font-size: 0.86rem; font-weight: 600; color: var(--text-primary); }
.fam-role { display: block; font-size: 0.68rem; margin-top: 2px; color: var(--accent); }
.fam-status { font-size: 0.65rem; color: var(--text-muted); padding: 2px 8px; border-radius: var(--radius-full); border: 1px solid var(--border-subtle); white-space: nowrap; }
.fam-status.active { color: var(--success); border-color: rgba(107, 142, 107, 0.3); background: rgba(107, 142, 107, 0.06); }
.fam-summary { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.fam-summary div { padding: 14px; text-align: center; background: var(--bg-warm); border: 1px solid var(--border-subtle); border-radius: var(--radius-lg); }
.fam-summary strong { display: block; font-family: var(--font-display); color: var(--accent); font-size: 1.3rem; }
.fam-summary span { color: var(--text-muted); font-size: 0.72rem; }
.fam-invite { display: flex; align-items: center; gap: 10px; padding: 14px 16px; background: var(--bg-warm); border: 1px solid var(--border-subtle); border-radius: var(--radius-lg); }
.fam-invite-code { flex: 1; }
.fam-code-label { display: block; font-size: 0.65rem; color: var(--text-muted); margin-bottom: 2px; }
.fam-code-value { font-family: var(--font-mono); font-size: 0.9rem; font-weight: 600; color: var(--accent); letter-spacing: 0.06em; }
.fam-copy-btn { padding: 8px 16px; border: 1px solid var(--accent); border-radius: var(--radius-full); background: transparent; color: var(--accent); font-family: var(--font-body); cursor: pointer; }
.fam-copy-btn:hover { background: var(--accent); color: #fff; }
.fam-invite-hint { margin: 8px 0 0; font-size: 0.7rem; color: var(--text-muted); line-height: 1.5; }
.fam-perms { display: flex; flex-direction: column; gap: 8px; }
.fam-perm { display: flex; align-items: center; gap: 8px; font-size: 0.76rem; color: var(--text-secondary); line-height: 1.5; }
.fam-perm svg { color: var(--accent); }
</style>
