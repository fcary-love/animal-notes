<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { gsap } from 'gsap'
import { useAuth } from '../composables/useAuth'
import { usePets } from '../composables/usePets'
import { useToast } from '../composables/useToast'
import { updateProfile, changePassword, uploadAvatar } from '../api/user'
import ThemedIcon from '../components/common/ThemedIcon.vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const { currentUser, fetchMe } = useAuth()
const { pets, fetchPets } = usePets()
const { showToast: toast } = useToast()

const pageRef = ref(null)
let motionCtx = null

/* ── 头像 ── */
const avatarInput = ref(null)
const uploadingAvatar = ref(false)
const avatarHover = ref(false)

function triggerAvatarPick() {
  avatarInput.value?.click()
}

async function onAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { toast('请选择图片文件', 'warning'); return }
  if (file.size > 5 * 1024 * 1024) { toast('图片不能超过 5MB', 'warning'); return }
  uploadingAvatar.value = true
  try {
    const res = await uploadAvatar(file)
    currentUser.value = { ...currentUser.value, ...res.data }
    localStorage.setItem('user', JSON.stringify(currentUser.value))
    toast('头像已更新')
  } catch (err) {
    toast(err?.response?.data?.message || '上传失败', 'error')
  } finally {
    uploadingAvatar.value = false
    if (avatarInput.value) avatarInput.value.value = ''
  }
}

/* ── 昵称编辑 ── */
const editingNickname = ref(false)
const nicknameInput = ref('')
const savingNickname = ref(false)

function startEditNickname() {
  nicknameInput.value = currentUser.value?.nickname || ''
  editingNickname.value = true
}

async function saveNickname() {
  const val = nicknameInput.value.trim()
  if (!val) { toast('昵称不能为空', 'warning'); return }
  if (val === currentUser.value?.nickname) { editingNickname.value = false; return }
  savingNickname.value = true
  try {
    const res = await updateProfile({ nickname: val })
    currentUser.value = { ...currentUser.value, ...res.data }
    localStorage.setItem('user', JSON.stringify(currentUser.value))
    editingNickname.value = false
    toast('昵称已更新')
  } catch (err) {
    toast(err?.response?.data?.message || '更新失败', 'error')
  } finally {
    savingNickname.value = false
  }
}

function cancelNickname() { editingNickname.value = false }

/* ── 修改密码 ── */
const pwdExpanded = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdErrors = ref({})
const savingPwd = ref(false)
const pwdSuccess = ref(false)

function validatePwd() {
  const e = {}
  if (!pwdForm.value.oldPassword) e.oldPassword = '请输入当前密码'
  if (!pwdForm.value.newPassword) e.newPassword = '请输入新密码'
  else if (pwdForm.value.newPassword.length < 6) e.newPassword = '至少6个字符'
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) e.confirmPassword = '两次密码不一致'
  pwdErrors.value = e
  return Object.keys(e).length === 0
}

async function submitPassword() {
  pwdErrors.value = {}; pwdSuccess.value = false
  if (!validatePwd()) return
  savingPwd.value = true
  try {
    await changePassword({ oldPassword: pwdForm.value.oldPassword, newPassword: pwdForm.value.newPassword })
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    pwdSuccess.value = true
    toast('密码已修改')
    setTimeout(() => { pwdExpanded.value = false; pwdSuccess.value = false }, 1800)
  } catch (err) {
    pwdErrors.value = { oldPassword: err?.response?.data?.message || '修改失败' }
  } finally {
    savingPwd.value = false
  }
}

/* ── 计算属性 ── */
const joinDate = computed(() => {
  const d = currentUser.value?.createdAt
  if (!d) return '未知'
  return new Date(d).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
})

const userId = computed(() => {
  const id = currentUser.value?.id
  if (!id) return '00000'
  return String(id).padStart(5, '0')
})

const userInitial = computed(() => {
  const n = currentUser.value?.nickname || currentUser.value?.username || '?'
  return n.charAt(0).toUpperCase()
})

const daysSinceJoin = computed(() => {
  const d = currentUser.value?.createdAt
  if (!d) return 0
  return Math.max(0, Math.ceil((Date.now() - new Date(d).getTime()) / 86400000))
})

const petCount = computed(() => pets.value.length)

const accentColor = computed(() => {
  const colors = ['#5c7a5e', '#c17b60', '#7a6a5c', '#6a7a8c', '#8c6a6a']
  const id = currentUser.value?.id || 0
  return colors[id % colors.length]
})

/* ── GSAP 入场动画 ── */
function initAnimations() {
  if (!pageRef.value) return
  motionCtx?.revert()
  motionCtx = gsap.matchMedia()
  motionCtx.add(
    { reduceMotion: '(prefers-reduced-motion: reduce)' },
    (ctx) => {
      if (ctx.conditions.reduceMotion) {
        gsap.set(pageRef.value.querySelectorAll('.anim'), { autoAlpha: 1, clearProps: 'transform' })
        return
      }
      const tl = gsap.timeline({ defaults: { duration: 0.65, ease: 'power3.out' } })
      tl
        .from('.page-head', { autoAlpha: 0, y: -20 })
        .from('.hero-specimen', { autoAlpha: 0, y: 30, scale: 0.97 }, '-=0.35')
        .from('.info-field', { autoAlpha: 0, x: -16, stagger: 0.08 }, '-=0.3')
        .from('.sidebar-card', { autoAlpha: 0, y: 24, stagger: 0.1 }, '-=0.35')
        .from('.pet-mini', { autoAlpha: 0, y: 16, rotation: -3, stagger: 0.08 }, '-=0.3')
    },
    pageRef.value
  )
}

onMounted(async () => {
  try {
    await Promise.all([fetchMe(), fetchPets()])
  } catch {}
  await nextTick()
  initAnimations()
})

onBeforeUnmount(() => { motionCtx?.revert() })
</script>

<template>
  <div ref="pageRef" class="profile-page">

    <!-- ═══ 页头 ═══ -->
    <header class="page-head">
      <div class="head-flourish">
        <svg class="flourish-svg" viewBox="0 0 200 20" preserveAspectRatio="none">
          <path d="M0 10 Q50 0 100 10 T200 10" stroke="var(--border-default)" stroke-width="0.8" fill="none" opacity="0.5"/>
        </svg>
      </div>
      <div class="head-content">
        <span class="head-kicker">PERSONAL SPECIMEN ARCHIVE</span>
        <h1 class="head-title">个人档案</h1>
        <p class="head-sub">在这一页，记录关于你自己的故事。</p>
      </div>
      <div class="head-flourish">
        <svg class="flourish-svg" viewBox="0 0 200 20" preserveAspectRatio="none">
          <path d="M0 10 Q50 20 100 10 T200 10" stroke="var(--border-default)" stroke-width="0.8" fill="none" opacity="0.5"/>
        </svg>
      </div>
    </header>

    <div class="profile-layout">

      <!-- ═══ 左栏：标本卡 hero ═══ -->
      <main class="main-column">
        <section class="hero-specimen">
          <!-- 背景装饰 -->
          <div class="specimen-bg">
            <div class="bg-grid"></div>
            <div class="bg-stamp">
              <svg width="80" height="80" viewBox="0 0 80 80" opacity="0.06">
                <circle cx="40" cy="40" r="36" stroke="var(--text-primary)" stroke-width="2" fill="none" stroke-dasharray="4 3"/>
                <circle cx="40" cy="40" r="28" stroke="var(--text-primary)" stroke-width="1" fill="none"/>
                <text x="40" y="38" text-anchor="middle" font-family="var(--font-display)" font-size="8" fill="var(--text-primary)">PET</text>
                <text x="40" y="48" text-anchor="middle" font-family="var(--font-display)" font-size="6" fill="var(--text-primary)">TIMELINE</text>
              </svg>
            </div>
          </div>

          <!-- 角标 -->
          <div class="corner corner-tl"></div>
          <div class="corner corner-tr"></div>
          <div class="corner corner-bl"></div>
          <div class="corner corner-br"></div>

          <!-- 头像宝丽来 -->
          <div class="polaroid-wrap">
            <div
              class="avatar-polaroid"
              @click="triggerAvatarPick"
              @mouseenter="avatarHover = true"
              @mouseleave="avatarHover = false"
              :class="{ uploading: uploadingAvatar }"
            >
              <!-- 胶带装饰 -->
              <div class="tape tape-left"></div>
              <div class="tape tape-right"></div>

              <div class="polaroid-inner">
                <img v-if="currentUser?.avatarUrl" :src="currentUser.avatarUrl" alt="头像" class="avatar-img" />
                <div v-else class="avatar-fallback" :style="{ background: accentColor + '18' }">
                  <span class="fallback-letter" :style="{ color: accentColor }">{{ userInitial }}</span>
                </div>

                <!-- hover 蒙层 -->
                <Transition name="fade">
                  <div v-if="avatarHover || uploadingAvatar" class="avatar-hover-layer">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
                      <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z" stroke="currentColor" stroke-width="1.5"/>
                      <circle cx="12" cy="13" r="4" stroke="currentColor" stroke-width="1.5"/>
                    </svg>
                    <span>{{ uploadingAvatar ? '上传中...' : '更换头像' }}</span>
                  </div>
                </Transition>
              </div>

              <div class="polaroid-caption">
                <span class="caption-text">{{ currentUser?.nickname || currentUser?.username || '未命名' }}</span>
                <span class="caption-date">Fig. {{ userId }}</span>
              </div>
            </div>
            <input ref="avatarInput" type="file" accept="image/*" class="hidden-input" @change="onAvatarChange" />
          </div>

          <!-- 信息字段 -->
          <div class="specimen-fields">
            <div class="info-field">
              <div class="field-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" stroke="currentColor" stroke-width="1.5"/><circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="1.5"/></svg>
                <span>用户名</span>
              </div>
              <div class="field-value">
                <code class="mono-val">{{ currentUser?.username || '—' }}</code>
                <span class="field-tag">不可修改</span>
              </div>
            </div>

            <div class="info-field">
              <div class="field-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="M12 20h9M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z" stroke="currentColor" stroke-width="1.5"/></svg>
                <span>昵称</span>
              </div>
              <div class="field-value" v-if="!editingNickname">
                <span class="text-val">{{ currentUser?.nickname || currentUser?.username || '—' }}</span>
                <button class="field-edit-btn" @click="startEditNickname">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none"><path d="M15.232 5.232l3.536 3.536M9 13l-2 6 6-2 9.586-9.586a2 2 0 000-2.828l-.707-.707a2 2 0 00-2.828 0L9 13z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                  编辑
                </button>
              </div>
              <div class="field-edit" v-else>
                <input
                  v-model="nicknameInput"
                  class="inline-input"
                  placeholder="输入新昵称"
                  maxlength="50"
                  @keyup.enter="saveNickname"
                  @keyup.escape="cancelNickname"
                />
                <div class="inline-actions">
                  <button class="act-save" @click="saveNickname" :disabled="savingNickname">
                    {{ savingNickname ? '...' : '保存' }}
                  </button>
                  <button class="act-cancel" @click="cancelNickname">取消</button>
                </div>
              </div>
            </div>

            <div class="info-field">
              <div class="field-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M16 2v4M8 2v4M3 10h18" stroke="currentColor" stroke-width="1.5"/></svg>
                <span>采集日期</span>
              </div>
              <div class="field-value">
                <span class="text-val">{{ joinDate }}</span>
                <span class="field-tag subtle" v-if="daysSinceJoin > 0">第 {{ daysSinceJoin }} 天</span>
              </div>
            </div>

            <div class="info-field">
              <div class="field-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78L12 21.23l8.84-8.84a5.5 5.5 0 000-7.78z" stroke="currentColor" stroke-width="1.5"/></svg>
                <span>伙伴数量</span>
              </div>
              <div class="field-value">
                <span class="text-val">{{ petCount }} 位小伙伴</span>
                <button class="field-link" @click="router.push('/pet/create')" v-if="petCount === 0">去创建</button>
              </div>
            </div>
          </div>
        </section>

        <!-- ═══ 宠物伙伴墙 ═══ -->
        <section class="companions-section anim" v-if="pets.length">
          <div class="section-head">
            <div>
              <span class="section-kicker">COMPANIONS</span>
              <h2 class="section-title">我的伙伴</h2>
            </div>
          </div>
          <div class="companions-grid">
            <article
              v-for="(pet, i) in pets"
              :key="pet.id"
              class="pet-mini"
              :style="{ '--delay': i * 0.06 + 's', transform: `rotate(${i % 2 === 0 ? -1.2 : 1.5}deg)` }"
              @click="router.push(`/pet/${pet.id}`)"
            >
              <div class="mini-photo">
                <img v-if="pet.avatarUrl" :src="pet.avatarUrl" :alt="pet.name" />
                <span v-else class="mini-fallback">{{ pet.name?.charAt(0) || '?' }}</span>
              </div>
              <div class="mini-info">
                <strong>{{ pet.name }}</strong>
                <small>{{ pet.breed || '未知品种' }}</small>
              </div>
            </article>
          </div>
        </section>
      </main>

      <!-- ═══ 右栏 ═══ -->
      <aside class="side-column">

        <!-- 密码修改 -->
        <section class="sidebar-card pwd-card">
          <button class="card-toggle" @click="pwdExpanded = !pwdExpanded">
            <div class="toggle-left">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <rect x="3" y="11" width="18" height="11" rx="2" stroke="currentColor" stroke-width="1.5"/>
                <path d="M7 11V7a5 5 0 0110 0v4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
              <span>修改密码</span>
            </div>
            <svg class="toggle-arrow" :class="{ expanded: pwdExpanded }" width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
          <Transition name="expand">
            <form v-if="pwdExpanded" class="pwd-form" @submit.prevent="submitPassword">
              <div class="pwd-field">
                <label class="pwd-label">当前密码</label>
                <input v-model="pwdForm.oldPassword" type="password" class="pwd-input" :class="{ 'has-error': pwdErrors.oldPassword }" placeholder="输入当前密码" autocomplete="current-password" />
                <span class="pwd-err" v-if="pwdErrors.oldPassword">{{ pwdErrors.oldPassword }}</span>
              </div>
              <div class="pwd-field">
                <label class="pwd-label">新密码</label>
                <input v-model="pwdForm.newPassword" type="password" class="pwd-input" :class="{ 'has-error': pwdErrors.newPassword }" placeholder="至少6个字符" autocomplete="new-password" />
                <span class="pwd-err" v-if="pwdErrors.newPassword">{{ pwdErrors.newPassword }}</span>
              </div>
              <div class="pwd-field">
                <label class="pwd-label">确认新密码</label>
                <input v-model="pwdForm.confirmPassword" type="password" class="pwd-input" :class="{ 'has-error': pwdErrors.confirmPassword }" placeholder="再输入一次" autocomplete="new-password" />
                <span class="pwd-err" v-if="pwdErrors.confirmPassword">{{ pwdErrors.confirmPassword }}</span>
              </div>
              <p class="pwd-success" v-if="pwdSuccess">密码已更新</p>
              <button class="pwd-submit" type="submit" :disabled="savingPwd">
                {{ savingPwd ? '保存中...' : '确认修改' }}
              </button>
            </form>
          </Transition>
        </section>

        <!-- 账号信息 -->
        <section class="sidebar-card account-card">
          <div class="card-header">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" stroke="currentColor" stroke-width="1.5"/>
            </svg>
            <span>账号信息</span>
          </div>
          <div class="account-rows">
            <div class="account-row">
              <span class="a-label">用户 ID</span>
              <code class="a-val mono">{{ currentUser?.id || '—' }}</code>
            </div>
            <div class="account-row">
              <span class="a-label">用户名</span>
              <code class="a-val mono">{{ currentUser?.username || '—' }}</code>
            </div>
            <div class="account-row">
              <span class="a-label">状态</span>
              <span class="a-val">
                <span class="pulse-dot"></span>
                在线
              </span>
            </div>
          </div>
        </section>

        <!-- 手写风备注 -->
        <section class="sidebar-card memo-card">
          <div class="memo-inner">
            <span class="memo-icon">&#9997;</span>
            <p class="memo-text">
              每一次修改档案，<br/>
              都是在手帐里留下<br/>
              新的笔迹。
            </p>
          </div>
        </section>

      </aside>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  flex: 1;
  width: 100%;
  max-width: 1040px;
  margin: 0 auto;
  padding: 0.5rem 1.5rem 3rem;
}

/* ═══ 页头 ═══ */
.page-head {
  text-align: center;
  padding: 1.5rem 0 1.2rem;
  margin-bottom: 1.5rem;
}
.head-flourish { max-width: 260px; margin: 0 auto; }
.flourish-svg { width: 100%; height: 14px; }
.head-content { padding: 0.4rem 0; }
.head-kicker {
  font-family: var(--font-mono);
  font-size: 0.62rem;
  letter-spacing: 0.18em;
  color: var(--accent);
  opacity: 0.7;
  display: block;
  margin-bottom: 0.2rem;
}
.head-title {
  font-family: var(--font-display);
  font-size: 1.65rem;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: 0.06em;
  margin: 0;
}
.head-sub {
  font-size: 0.82rem;
  color: var(--text-muted);
  margin: 0.3rem 0 0;
  line-height: 1.6;
}

/* ═══ 布局 ═══ */
.profile-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 1.5rem;
  align-items: start;
}

/* ═══ 标本卡 hero ═══ */
.hero-specimen {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 2.5rem 2rem 2rem;
  position: relative;
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.specimen-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(var(--border-subtle) 1px, transparent 1px),
    linear-gradient(90deg, var(--border-subtle) 1px, transparent 1px);
  background-size: 40px 40px;
  opacity: 0.15;
}
.bg-stamp {
  position: absolute;
  top: 16px;
  right: 16px;
  transform: rotate(12deg);
}

/* 角标 */
.corner {
  position: absolute;
  width: 22px;
  height: 22px;
  border-color: var(--accent);
  border-style: solid;
  opacity: 0.2;
  z-index: 1;
}
.corner-tl { top: 10px; left: 10px; border-width: 2px 0 0 2px; }
.corner-tr { top: 10px; right: 10px; border-width: 2px 2px 0 0; }
.corner-bl { bottom: 10px; left: 10px; border-width: 0 0 2px 2px; }
.corner-br { bottom: 10px; right: 10px; border-width: 0 2px 2px 0; }

/* ═══ 宝丽来头像 ═══ */
.polaroid-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 2rem;
  position: relative;
  z-index: 1;
}
.avatar-polaroid {
  width: 200px;
  background: #fffef9;
  padding: 10px 10px 0;
  border: 1px solid var(--border-subtle);
  box-shadow: 0 8px 28px rgba(61, 50, 38, 0.12), 0 2px 6px rgba(61, 50, 38, 0.06);
  cursor: pointer;
  transition: transform 0.35s var(--ease-out), box-shadow 0.35s var(--ease-out);
  position: relative;
  transform: rotate(-1.5deg);
}
.avatar-polaroid:hover {
  transform: rotate(0deg) translateY(-4px);
  box-shadow: 0 14px 36px rgba(61, 50, 38, 0.16), 0 4px 10px rgba(61, 50, 38, 0.08);
}

/* 胶带 */
.tape {
  position: absolute;
  width: 50px;
  height: 18px;
  background: rgba(193, 123, 96, 0.15);
  border: 1px solid rgba(193, 123, 96, 0.12);
  z-index: 2;
  backdrop-filter: blur(1px);
}
.tape-left { top: -8px; left: 18px; transform: rotate(-8deg); }
.tape-right { top: -6px; right: 14px; transform: rotate(5deg); }

.polaroid-inner {
  aspect-ratio: 1;
  overflow: hidden;
  position: relative;
  background: var(--bg-aged);
  border-radius: 2px;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.fallback-letter {
  font-family: var(--font-display);
  font-size: 3.5rem;
  font-weight: 800;
  opacity: 0.6;
}
.avatar-hover-layer {
  position: absolute;
  inset: 0;
  background: rgba(61, 50, 38, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #fff;
  font-size: 0.78rem;
}

.polaroid-caption {
  padding: 10px 4px 14px;
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.caption-text {
  font-family: var(--font-display);
  font-size: 0.88rem;
  color: var(--text-primary);
  font-weight: 600;
}
.caption-date {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  color: var(--text-muted);
}

.hidden-input { display: none; }

/* ═══ 信息字段 ═══ */
.specimen-fields {
  display: flex;
  flex-direction: column;
  gap: 0;
  position: relative;
  z-index: 1;
}
.info-field {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 0.9rem 0;
  border-bottom: 1px dashed var(--border-subtle);
}
.info-field:last-child { border-bottom: none; }

.field-label {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 100px;
  flex-shrink: 0;
  font-family: var(--font-display);
  font-size: 0.78rem;
  color: var(--text-muted);
  letter-spacing: 0.04em;
  padding-top: 2px;
}
.field-label svg { color: var(--accent); opacity: 0.6; }

.field-value {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}
.mono-val {
  font-family: var(--font-mono);
  font-size: 0.88rem;
  color: var(--text-secondary);
  background: var(--bg-warm);
  padding: 2px 8px;
  border-radius: 3px;
  border: 1px solid var(--border-subtle);
}
.text-val {
  font-size: 0.92rem;
  color: var(--text-primary);
}
.field-tag {
  font-size: 0.65rem;
  color: var(--text-muted);
  background: var(--bg-warm);
  padding: 2px 7px;
  border-radius: 3px;
  letter-spacing: 0.02em;
  flex-shrink: 0;
}
.field-tag.subtle {
  background: var(--accent-surface);
  color: var(--accent);
}
.field-link {
  background: none;
  border: none;
  color: var(--accent);
  font-family: var(--font-body);
  font-size: 0.78rem;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
  flex-shrink: 0;
}

.field-edit-btn {
  background: none;
  border: 1px solid var(--border-default);
  color: var(--text-muted);
  cursor: pointer;
  padding: 3px 10px;
  border-radius: var(--radius-sm);
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: var(--font-body);
  font-size: 0.72rem;
  transition: all var(--duration-fast);
  flex-shrink: 0;
}
.field-edit-btn:hover {
  color: var(--accent);
  border-color: var(--accent);
  background: var(--accent-surface);
}

.field-edit {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.inline-input {
  padding: 8px 12px;
  border: 1px solid var(--accent);
  border-radius: var(--radius-sm);
  background: var(--bg-input);
  font-family: var(--font-body);
  font-size: 0.9rem;
  color: var(--text-primary);
  outline: none;
  transition: box-shadow var(--duration-fast);
}
.inline-input:focus {
  box-shadow: 0 0 0 3px var(--accent-surface);
}
.inline-actions { display: flex; gap: 8px; }
.act-save, .act-cancel {
  padding: 5px 16px;
  border-radius: var(--radius-sm);
  font-family: var(--font-body);
  font-size: 0.8rem;
  cursor: pointer;
  transition: all var(--duration-fast);
}
.act-save {
  background: var(--accent);
  color: var(--text-inverse);
  border: none;
}
.act-save:hover:not(:disabled) { background: var(--accent-light); }
.act-save:disabled { opacity: 0.6; cursor: not-allowed; }
.act-cancel {
  background: none;
  border: 1px solid var(--border-default);
  color: var(--text-secondary);
}
.act-cancel:hover { border-color: var(--text-muted); }

/* ═══ 宠物伙伴 ═══ */
.companions-section {
  margin-top: 1.5rem;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}
.section-kicker {
  font-family: var(--font-mono);
  font-size: 0.6rem;
  letter-spacing: 0.15em;
  color: var(--accent);
  opacity: 0.7;
  display: block;
}
.section-title {
  font-family: var(--font-display);
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0.1rem 0 0;
}
.companions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 1rem;
}
.pet-mini {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 0.6rem;
  cursor: pointer;
  transition: all 0.3s var(--ease-out);
  box-shadow: var(--shadow-sm);
}
.pet-mini:hover {
  transform: translateY(-4px) rotate(0deg) !important;
  box-shadow: var(--shadow-elevated);
}
.mini-photo {
  aspect-ratio: 1;
  overflow: hidden;
  border-radius: var(--radius-md);
  background: var(--bg-aged);
  margin-bottom: 0.5rem;
}
.mini-photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.4s var(--ease-out);
}
.pet-mini:hover .mini-photo img { transform: scale(1.06); }
.mini-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 1.8rem;
  color: var(--accent);
  opacity: 0.5;
}
.mini-info { padding: 0 2px; }
.mini-info strong {
  display: block;
  font-family: var(--font-display);
  font-size: 0.88rem;
  color: var(--text-primary);
}
.mini-info small {
  display: block;
  font-size: 0.68rem;
  color: var(--text-muted);
}

/* ═══ 侧栏 ═══ */
.side-column {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.sidebar-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

/* 密码卡 */
.card-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  background: none;
  border: none;
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.88rem;
  color: var(--text-primary);
}
.toggle-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.toggle-left svg { color: var(--accent); }
.toggle-arrow {
  transition: transform 0.25s var(--ease-out);
  color: var(--text-muted);
}
.toggle-arrow.expanded { transform: rotate(180deg); }

.pwd-form {
  padding: 0 1.25rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}
.pwd-field { display: flex; flex-direction: column; gap: 3px; }
.pwd-label { font-size: 0.72rem; color: var(--text-muted); letter-spacing: 0.03em; }
.pwd-input {
  padding: 8px 10px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  background: var(--bg-input);
  font-family: var(--font-body);
  font-size: 0.85rem;
  color: var(--text-primary);
  outline: none;
  transition: border-color var(--duration-fast);
}
.pwd-input:focus { border-color: var(--accent); }
.pwd-input.has-error { border-color: var(--danger); }
.pwd-err { font-size: 0.7rem; color: var(--danger); }
.pwd-success { font-size: 0.8rem; color: var(--success); text-align: center; }
.pwd-submit {
  padding: 9px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: var(--text-inverse);
  font-family: var(--font-body);
  font-size: 0.85rem;
  cursor: pointer;
  transition: background var(--duration-fast);
}
.pwd-submit:hover:not(:disabled) { background: var(--accent-light); }
.pwd-submit:disabled { opacity: 0.6; cursor: not-allowed; }

/* 展开动画 */
.expand-enter-active { animation: expandIn 0.3s var(--ease-out); }
.expand-leave-active { animation: expandIn 0.2s var(--ease-out) reverse; }
@keyframes expandIn {
  from { opacity: 0; max-height: 0; transform: translateY(-8px); }
  to { opacity: 1; max-height: 400px; transform: translateY(0); }
}

/* 账号信息卡 */
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 1rem 1.25rem 0.75rem;
  font-family: var(--font-display);
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.03em;
}
.card-header svg { color: var(--accent); }

.account-rows {
  padding: 0 1.25rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.account-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.a-label { font-size: 0.76rem; color: var(--text-muted); }
.a-val {
  font-size: 0.82rem;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.a-val.mono {
  font-family: var(--font-mono);
  font-size: 0.78rem;
  background: var(--bg-warm);
  padding: 1px 6px;
  border-radius: 3px;
}
.pulse-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--success);
  box-shadow: 0 0 0 2px rgba(107, 142, 107, 0.2);
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 2px rgba(107, 142, 107, 0.2); }
  50% { box-shadow: 0 0 0 5px rgba(107, 142, 107, 0.08); }
}

/* 备注卡 */
.memo-card {
  background: linear-gradient(135deg, var(--bg-warm) 0%, var(--bg-aged) 100%);
  border: 1px dashed var(--border-default);
}
.memo-inner {
  padding: 1.25rem;
  text-align: center;
}
.memo-icon {
  font-size: 1.5rem;
  display: block;
  margin-bottom: 0.5rem;
  opacity: 0.5;
}
.memo-text {
  font-family: var(--font-display);
  font-size: 0.82rem;
  color: var(--text-muted);
  line-height: 2;
  margin: 0;
  font-style: italic;
}

/* ═══ 响应式 ═══ */
@media (max-width: 760px) {
  .profile-page { padding: 0.5rem 1rem 2rem; }
  .profile-layout { grid-template-columns: 1fr; }
  .hero-specimen { padding: 2rem 1.25rem 1.5rem; }
  .head-title { font-size: 1.35rem; }
  .info-field { flex-direction: column; gap: 0.4rem; }
  .field-label { min-width: auto; }
  .companions-grid { grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); }
  .avatar-polaroid { width: 170px; }
}

@media (max-width: 480px) {
  .head-title { font-size: 1.15rem; }
  .avatar-polaroid { width: 150px; }
  .companions-grid { grid-template-columns: repeat(2, 1fr); }
}

/* ── 过渡 ── */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
