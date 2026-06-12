<script setup>
import { useAuth } from '../../composables/useAuth'
import { onMounted, ref, computed, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { usePets } from '../../composables/usePets'
import { getTimeline } from '../../api/moment'
import { getDiets, getHealthSummary, getWeights } from '../../api/health'
import ThemedIcon from '../common/ThemedIcon.vue'
import SlideDrawer from '../common/SlideDrawer.vue'
import DietPanel from '../panels/DietPanel.vue'
import ReminderPanel from '../panels/ReminderPanel.vue'
import AnniversaryPanel from '../panels/AnniversaryPanel.vue'
import FamilyPanel from '../panels/FamilyPanel.vue'
import ReportPanel from '../panels/ReportPanel.vue'
import SettingsPanel from '../panels/SettingsPanel.vue'

const { currentUser, isLoggedIn, logout, restoreFromStorage } = useAuth()
const { pets, currentPet, fetchPets } = usePets()
const router = useRouter()
onMounted(async () => {
  restoreFromStorage()
  if (isLoggedIn.value && pets.value.length === 0) {
    await fetchPets()
  }
})

// Drawer state
const drawerVisible = ref(false)
const activePanel = ref('')
const drawerTitle = ref('')

const panelMap = {
  diet: { component: DietPanel, title: '饮食记录' },
  reminder: { component: ReminderPanel, title: '待办提醒' },
  anniversary: { component: AnniversaryPanel, title: '纪念日' },
  family: { component: FamilyPanel, title: '家庭成员' },
  report: { component: ReportPanel, title: '数据报告' },
  settings: { component: SettingsPanel, title: '设置' },
}
const activeComponent = computed(() => panelMap[activePanel.value]?.component || null)

function openPanel(key) {
  activePanel.value = key
  drawerTitle.value = panelMap[key]?.title || ''
  drawerVisible.value = true
  moreOpen.value = false
}
function closeDrawer() {
  drawerVisible.value = false
}

// 移动端菜单
const mobileMenuOpen = ref(false)
function toggleMobileMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value
}
function closeMobileMenu() {
  mobileMenuOpen.value = false
}

// 更多菜单
const moreOpen = ref(false)
const moreLoading = ref(false)
const moreLoaded = ref(false)
const moreOverview = ref({
  diets: 0,
  anniversaries: 0,
  reminders: 0,
  reportItems: 0,
  familyMembers: 0
})
let closeTimer = null
function openMore() {
  clearTimeout(closeTimer)
  moreOpen.value = true
  loadMoreOverview()
}
function scheduleClose() {
  closeTimer = setTimeout(() => { moreOpen.value = false }, 180)
}
function handleMoreClick(item) {
  moreOpen.value = false
  openPanel(item.key)
}

const moreItems = computed(() => [
  { key: 'diet', label: '饮食记录', icon: 'bowl', meta: currentPet.value ? `${moreOverview.value.diets} 条` : '先选择宠物' },
  { key: 'anniversary', label: '纪念日', icon: 'cake', meta: `${moreOverview.value.anniversaries} 个` },
  { key: 'reminder', label: '待办提醒', icon: 'bell', meta: `${moreOverview.value.reminders} 项` },
  { key: 'report', label: '数据报告', icon: 'chart', meta: `${moreOverview.value.reportItems} 项数据` },
  { key: 'family', label: '家庭成员', icon: 'users', meta: `${moreOverview.value.familyMembers} 位` },
  { key: 'settings', label: '设置', icon: 'gear', meta: '本地偏好' },
])

async function loadMoreOverview() {
  if (moreLoaded.value || moreLoading.value) return
  moreLoading.value = true
  try {
    if (pets.value.length === 0) await fetchPets()
    const pet = currentPet.value || pets.value[0]
    const dietPromise = pet ? getDiets(pet.id, 20).then(res => res.data?.length || 0).catch(() => 0) : Promise.resolve(0)
    const weightPromise = pet ? getWeights(pet.id, 10).then(res => res.data?.length || 0).catch(() => 0) : Promise.resolve(0)
    const summaryPromise = pet ? getHealthSummary(pet.id).then(res => res.data || null).catch(() => null) : Promise.resolve(null)
    const momentPromise = Promise.all(pets.value.map(p =>
      getTimeline(p.id, { sort: 'desc', page: 1, size: 50 })
        .then(res => res.data?.records || [])
        .catch(() => [])
    ))

    const [dietCount, weightCount, summary, momentResults] = await Promise.all([
      dietPromise,
      weightPromise,
      summaryPromise,
      momentPromise
    ])
    const moments = momentResults.flat()
    const dueCount = [summary?.nextVaccineDate, summary?.nextDewormingDate].filter(Boolean).length
    moreOverview.value = {
      diets: dietCount,
      anniversaries: moments.filter(m => m.milestoneLabel || m.isMilestone).length,
      reminders: dueCount,
      reportItems: [dietCount, weightCount, moments.length, dueCount].filter(n => n > 0).length,
      familyMembers: currentUser.value ? 1 : 0
    }
    moreLoaded.value = true
  } finally {
    moreLoading.value = false
  }
}
</script>

<template>
  <header class="app-header">
    <div class="header-inner">
      <router-link to="/dashboard" class="header-logo">
        <svg class="logo-svg" width="26" height="26" viewBox="0 0 24 24" fill="none">
          <!-- paw pads -->
          <ellipse cx="8" cy="6.5" rx="2.2" ry="2.5" fill="var(--accent)" opacity="0.8"/>
          <ellipse cx="13" cy="5.2" rx="1.8" ry="2.1" fill="var(--accent)" opacity="0.8"/>
          <ellipse cx="17.5" cy="8" rx="1.6" ry="1.9" fill="var(--accent)" opacity="0.8"/>
          <ellipse cx="4.8" cy="10.5" rx="1.6" ry="1.9" fill="var(--accent)" opacity="0.8"/>
          <!-- main pad + pen nib -->
          <path d="M9.5 14c-.8 1.8-1.3 3.2-1.3 4 0 1.1.7 1.7 1.7 1.7s1.8-.6 1.8-1.7c0-.9-.4-2-1-3.3" fill="var(--accent)" opacity="0.7"/>
          <!-- pen nib overlay -->
          <path d="M14.5 13.5l-1.8 3.2c-.1.2 0 .4.2.5l3-1.5c1.5-1.2 2.8-2.5 3.3-3.3.2-.2.2-.5 0-.6-.2-.2-.5-.2-.6 0l-4.1 1.7z" fill="var(--spot)" opacity="0.85"/>
          <line x1="12.5" y1="17" x2="11" y2="19.5" stroke="var(--spot)" stroke-width="1.2" stroke-linecap="round" opacity="0.7"/>
        </svg>
        <span class="logo-text">宠物时光手帐</span>
      </router-link>
      <!-- 移动端汉堡按钮 -->
      <button class="mobile-menu-btn" v-if="isLoggedIn && currentUser" @click="toggleMobileMenu">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <line x1="3" y1="6" x2="21" y2="6"/>
          <line x1="3" y1="12" x2="21" y2="12"/>
          <line x1="3" y1="18" x2="21" y2="18"/>
        </svg>
      </button>
      <nav class="header-nav" v-if="isLoggedIn && currentUser">
        <router-link to="/dashboard" class="nav-link">主页</router-link>
        <router-link to="/timeline" class="nav-link">时间线</router-link>
        <router-link to="/ai/chat" class="nav-link nav-with-icon">
          <ThemedIcon name="quill" :size="14" />
          <span>时光顾问</span>
        </router-link>
        <router-link to="/admin/knowledge" class="nav-link">知识库</router-link>
        <span class="nav-divider"></span>
        <router-link to="/health" class="nav-link nav-with-icon">
          <ThemedIcon name="heartPulse" :size="14" />
          <span>健康档案</span>
        </router-link>
        <router-link to="/album" class="nav-link nav-with-icon">
          <ThemedIcon name="images" :size="14" />
          <span>成长相册</span>
        </router-link>
        <div class="nav-dropdown" @mouseenter="openMore" @mouseleave="scheduleClose">
          <button class="nav-link nav-placeholder nav-dropdown-trigger">
            <ThemedIcon name="dots" :size="14" />
            <span>更多</span>
          </button>
          <Transition name="dropdown">
            <div v-if="moreOpen" class="dropdown-menu" @mouseenter="openMore" @mouseleave="scheduleClose">
              <button
                v-for="item in moreItems"
                :key="item.key"
                class="dropdown-item"
                @click="handleMoreClick(item)"
              >
                <ThemedIcon :name="item.icon" :size="15" />
                <span class="dropdown-copy">
                  <span>{{ item.label }}</span>
                  <small>{{ moreLoading ? '同步中...' : item.meta }}</small>
                </span>
              </button>
            </div>
          </Transition>
        </div>
        <router-link to="/profile" class="nav-greeting">{{ currentUser.nickname || currentUser.username }}</router-link>
        <button class="nav-logout" @click="logout">离开</button>
      </nav>
    </div>
  </header>

  <!-- Mobile menu -->
  <Transition name="mobile-menu">
    <div class="mobile-menu-overlay" v-if="mobileMenuOpen" @click.self="closeMobileMenu">
      <div class="mobile-menu-panel">
        <div class="mobile-menu-header">
          <span>{{ currentUser?.nickname || currentUser?.username }}</span>
          <button class="mobile-menu-close" @click="closeMobileMenu">&times;</button>
        </div>
        <nav class="mobile-menu-nav">
          <router-link to="/dashboard" @click="closeMobileMenu">主页</router-link>
          <router-link to="/timeline" @click="closeMobileMenu">时间线</router-link>
          <router-link to="/ai/chat" @click="closeMobileMenu">宠物管家</router-link>
          <router-link to="/health" @click="closeMobileMenu">健康档案</router-link>
          <router-link to="/album" @click="closeMobileMenu">成长相册</router-link>
          <router-link to="/admin/knowledge" @click="closeMobileMenu">知识库</router-link>
          <router-link to="/profile" @click="closeMobileMenu">个人资料</router-link>
        </nav>
        <button class="mobile-menu-logout" @click="logout">退出登录</button>
      </div>
    </div>
  </Transition>

  <!-- Drawer -->
  <SlideDrawer :visible="drawerVisible" :title="drawerTitle" @close="closeDrawer">
    <component :is="activeComponent" />
  </SlideDrawer>
</template>

<style scoped>
.app-header {
  padding: 0 2rem;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--bg-surface);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(8px);
}
.header-inner {
  max-width: 960px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 56px;
}
.header-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.25rem;
  color: var(--text-primary);
  text-decoration: none;
  flex-shrink: 0;
}
.logo-svg { flex-shrink: 0; }
.logo-text { letter-spacing: 0.05em; }
.header-nav {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  min-width: 0;
}
.nav-link {
  font-size: 0.82rem;
  color: var(--text-muted);
  text-decoration: none;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: color var(--duration-fast);
  white-space: nowrap;
  flex-shrink: 0;
}
.nav-link:hover { color: var(--accent); }
.nav-link.router-link-active { color: var(--accent); font-weight: 600; }

/* 占位按钮 — 和普通链接视觉一致 */
.nav-placeholder {
  background: none;
  border: none;
  cursor: pointer;
  font-family: var(--font-body);
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--text-muted);
  padding: 4px 7px;
  border-radius: var(--radius-sm);
  transition: color var(--duration-fast);
}
.nav-placeholder:hover { color: var(--accent); }

/* 带图标的导航链接 */
.nav-with-icon {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 4px 7px;
}
.nav-with-icon.router-link-active { color: var(--accent); font-weight: 600; }

.nav-divider {
  width: 1px;
  height: 16px;
  background: var(--border-subtle);
  margin: 0 4px;
  flex-shrink: 0;
}

/* 下拉菜单 */
.nav-dropdown { position: relative; }
.nav-dropdown-trigger { gap: 2px; }
.dropdown-menu {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  min-width: 178px;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: 4px 0;
  z-index: 200;
}
.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 7px 14px;
  background: none;
  border: none;
  font-family: var(--font-body);
  font-size: 0.82rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: background var(--duration-fast), color var(--duration-fast);
}
.dropdown-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.35;
}
.dropdown-copy small {
  color: var(--text-muted);
  font-size: 0.66rem;
}
.dropdown-item:hover .dropdown-copy small {
  color: var(--accent);
  opacity: 0.72;
}
.dropdown-item:hover {
  background: var(--accent-surface);
  color: var(--accent);
}

/* 下拉动画 */
.dropdown-enter-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.dropdown-leave-active { transition: opacity 0.1s ease, transform 0.1s ease; }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(-4px); }

.nav-greeting {
  font-size: 0.82rem;
  color: var(--text-muted);
  margin-left: 4px;
  white-space: nowrap;
  flex-shrink: 0;
  text-decoration: none;
  transition: color var(--duration-fast);
  cursor: pointer;
}
.nav-greeting:hover {
  color: var(--accent);
  text-decoration: none;
}
.nav-logout {
  background: none;
  border: 1px solid var(--border-default);
  color: var(--text-secondary);
  font-family: var(--font-body);
  font-size: 0.8rem;
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  flex-shrink: 0;
}
.nav-logout:hover {
  border-color: var(--accent);
  color: var(--accent);
}

/* 汉堡按钮 - 默认隐藏 */
.mobile-menu-btn {
  display: none;
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 6px;
}

/* 移动端菜单 */
.mobile-menu-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.3);
  z-index: 500;
}
.mobile-menu-panel {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 260px;
  background: var(--bg-surface);
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 20px rgba(0,0,0,0.1);
}
.mobile-menu-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-subtle);
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 1rem;
}
.mobile-menu-close {
  background: none;
  border: none;
  font-size: 1.4rem;
  color: var(--text-muted);
  cursor: pointer;
}
.mobile-menu-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 8px 0;
  overflow-y: auto;
}
.mobile-menu-nav a {
  display: block;
  padding: 12px 20px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 0.92rem;
  transition: background var(--duration-fast);
}
.mobile-menu-nav a:hover,
.mobile-menu-nav a.router-link-active {
  background: var(--accent-surface);
  color: var(--accent);
}
.mobile-menu-logout {
  margin: 12px 20px 20px;
  padding: 10px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-family: var(--font-body);
  font-size: 0.88rem;
  cursor: pointer;
}
.mobile-menu-logout:hover {
  border-color: var(--danger);
  color: var(--danger);
}
.mobile-menu-enter-active { transition: opacity 0.2s ease; }
.mobile-menu-leave-active { transition: opacity 0.15s ease; }
.mobile-menu-enter-from, .mobile-menu-leave-to { opacity: 0; }
.mobile-menu-enter-active .mobile-menu-panel { transition: transform 0.2s ease; }
.mobile-menu-leave-active .mobile-menu-panel { transition: transform 0.15s ease; }
.mobile-menu-enter-from .mobile-menu-panel { transform: translateX(100%); }
.mobile-menu-leave-to .mobile-menu-panel { transform: translateX(100%); }

/* 窄屏适配 */
@media (max-width: 768px) {
  .app-header { padding: 0 0.8rem; }
  .header-inner { height: 50px; }
  .header-nav { display: none; }
  .mobile-menu-btn { display: flex; }
  .logo-text { font-size: 1rem; }
}
</style>
