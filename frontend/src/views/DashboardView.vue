<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { gsap } from 'gsap'
import { useAuth } from '../composables/useAuth'
import { usePets } from '../composables/usePets'
import { getHealthSummary } from '../api/health'
import { getAgentTasks } from '../api/agent'
import ThemedIcon from '../components/common/ThemedIcon.vue'

const router = useRouter()
const { currentUser } = useAuth()
const { pets, fetchPets } = usePets()

const pageRef = ref(null)
const loading = ref(true)
const petSummaries = ref({})
const allMoments = ref([])
const agentTasks = ref([])

let motionMatchMedia = null

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const today = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
})

const userName = computed(() => currentUser.value?.nickname || currentUser.value?.username || '朋友')

const latestMoment = computed(() => allMoments.value[0] || null)
const heroPet = computed(() => {
  if (!pets.value.length) return null
  const momentPetId = latestMoment.value?.petId
  return pets.value.find(p => p.id === Number(momentPetId)) || pets.value[0]
})
const heroImage = computed(() => heroPet.value?.avatarUrl || latestMoment.value?.imageUrl || '')
const heroLine = computed(() => {
  if (!heroPet.value) return '今天也适合为新的陪伴留一页空白。'
  const days = getPetDays(heroPet.value)
  const age = getPetAge(heroPet.value)
  return `今天是 ${heroPet.value.name} 来到家里的第 ${days} 天，${age ? `${age}，` : ''}愿它继续慢慢发光。`
})
const dailyWhisper = computed(() => {
  if (!pets.value.length) return '给未来的它留一张照片，故事就从今天开始。'
  const pet = heroPet.value || pets.value[0]
  const tips = [
    `今天适合给 ${pet.name} 梳梳毛，让爱意从指尖落下来。`,
    `${pet.name} 的小日子也许很普通，但普通里藏着很长的陪伴。`,
    `如果阳光正好，记得给 ${pet.name} 留下一张暖暖的照片。`,
    `今晚可以多陪 ${pet.name} 玩一会儿，快乐会自己长出尾巴。`
  ]
  return tips[(new Date().getDate() + pets.value.length) % tips.length]
})

const featuredMoments = computed(() => allMoments.value.slice(0, 4))
const recentMilestones = computed(() => allMoments.value.filter(m => m.milestoneLabel).slice(0, 5))
const healthAlerts = computed(() => {
  const alerts = []
  const now = new Date()
  for (const [petId, summary] of Object.entries(petSummaries.value)) {
    if (!summary) continue
    const pet = pets.value.find(p => p.id === Number(petId))
    if (!pet) continue
    if (summary.nextVaccineDate) {
      const due = new Date(summary.nextVaccineDate)
      const days = Math.ceil((due - now) / 86400000)
      if (days <= 14) alerts.push({ pet: pet.name, type: '疫苗', date: summary.nextVaccineDate, days, urgent: days <= 3 })
    }
    if (summary.nextDewormingDate) {
      const due = new Date(summary.nextDewormingDate)
      const days = Math.ceil((due - now) / 86400000)
      if (days <= 14) alerts.push({ pet: pet.name, type: '驱虫', date: summary.nextDewormingDate, days, urgent: days <= 3 })
    }
  }
  return alerts.sort((a, b) => a.days - b.days)
})

function getPetDays(pet) {
  const dateText = pet.adoptionDate || pet.createdAt || pet.birthDate
  if (!dateText) return 1
  const start = new Date(dateText)
  if (Number.isNaN(start.getTime())) return 1
  return Math.max(1, Math.ceil((Date.now() - start.getTime()) / 86400000))
}

function getPetAge(pet) {
  if (!pet.birthDate) return ''
  const birth = new Date(pet.birthDate)
  if (Number.isNaN(birth.getTime())) return ''
  const months = Math.max(0, (new Date().getFullYear() - birth.getFullYear()) * 12 + new Date().getMonth() - birth.getMonth())
  if (months < 1) return '还不到 1 个月'
  const years = Math.floor(months / 12)
  const rest = months % 12
  if (!years) return `${rest} 个月大`
  if (!rest) return `${years} 岁`
  return `${years} 岁 ${rest} 个月`
}

function momentPetName(moment) {
  const pet = pets.value.find(p => p.id === Number(moment?.petId))
  return pet?.name || heroPet.value?.name || '它'
}

function momentDate(moment) {
  return moment?.occurredAt?.substring(0, 10) || today.value
}

function momentText(moment) {
  if (!moment) return '还没有精选瞬间，今天可以写下第一句陪伴。'
  return moment.content || moment.title || '这一刻被安静地收藏了起来。'
}

function petTagline(pet, index) {
  const lines = ['今天也在认真可爱', '把家里巡视成小王国', '适合被轻轻夸奖', '正在积攒新的故事']
  return `${pet.name}：${lines[index % lines.length]}`
}

function goPet(petId) { router.push(`/pet/${petId}`) }
function goTimeline() { router.push('/timeline') }
function goHealth(petId) { router.push(`/pet/${petId}/health`) }
function goAiChat() { router.push('/ai/chat') }
function goKnowledge() { router.push('/admin/knowledge') }

function runAgentTask(task) {
  const petId = heroPet.value?.id
  if (!petId) return
  const routes = {
    health_check: `/pet/${petId}/health`,
    weight_record: `/pet/${petId}/health`,
    diet_record: `/pet/${petId}/diet`,
    add_photo: `/pet/${petId}/album`,
    growth_report: `/pet/${petId}/report`,
    play_interact: `/pet/${petId}/game`,
    daily_pet: `/pet/${petId}/game`,
    record_moment: '/timeline',
    suggest_milestone: '/timeline'
  }
  router.push(routes[task.id] || '/timeline')
}

function initAnimations() {
  if (!pageRef.value) return
  motionMatchMedia?.revert()
  motionMatchMedia = gsap.matchMedia()
  motionMatchMedia.add(
    {
      reduceMotion: '(prefers-reduced-motion: reduce)',
      isMobile: '(max-width: 720px)'
    },
    (context) => {
      const { reduceMotion, isMobile } = context.conditions
      const root = pageRef.value
      const cards = gsap.utils.toArray(root.querySelectorAll('.pet-polaroid'))

      if (reduceMotion) {
        gsap.set(root.querySelectorAll('.lookbook-animate'), { autoAlpha: 1, clearProps: 'transform' })
        return undefined
      }

      const intro = gsap.timeline({ defaults: { duration: 0.72, ease: 'power3.out' } })
      intro
        .from('.hero-lookbook', { autoAlpha: 0, y: 28 })
        .from('.time-capsule', { autoAlpha: 0, y: 24 }, '-=0.42')
        .from('.story-card', { autoAlpha: 0, y: 24, stagger: 0.08 }, '-=0.34')
        .from('.pet-polaroid', { autoAlpha: 0, y: 28, rotation: isMobile ? 0 : -2, stagger: 0.1 }, '-=0.34')
        .from('.side-note', { autoAlpha: 0, x: isMobile ? 0 : 18, y: isMobile ? 18 : 0, stagger: 0.08 }, '-=0.38')

      const cleanupFns = cards.map((card) => {
        const image = card.querySelector('.pet-photo')
        const onEnter = () => {
          gsap.to(card, { y: -8, scale: 1.025, rotation: 0, duration: 0.36, ease: 'power2.out', overwrite: 'auto' })
          if (image) gsap.to(image, { scale: 1.06, duration: 0.5, ease: 'power2.out', overwrite: 'auto' })
        }
        const onLeave = () => {
          gsap.to(card, { y: 0, scale: 1, rotation: Number(card.dataset.tilt || 0), duration: 0.42, ease: 'power2.out', overwrite: 'auto' })
          if (image) gsap.to(image, { scale: 1, duration: 0.5, ease: 'power2.out', overwrite: 'auto' })
        }
        card.addEventListener('mouseenter', onEnter)
        card.addEventListener('mouseleave', onLeave)
        return () => {
          card.removeEventListener('mouseenter', onEnter)
          card.removeEventListener('mouseleave', onLeave)
        }
      })

      return () => {
        intro.kill()
        cleanupFns.forEach(fn => fn())
      }
    },
    pageRef.value
  )
}

onMounted(async () => {
  try {
    await fetchPets()
    const momentPromises = pets.value.map(p =>
      import('../api/moment').then(m => m.getTimeline(p.id, { sort: 'desc', page: 1, size: 50 }))
        .then(res => res.data.records || [])
        .catch(() => [])
    )
    const healthPromises = pets.value.map(p =>
      getHealthSummary(p.id).then(res => ({ petId: p.id, summary: res.data })).catch(() => null)
    )

    const [momentResults, healthResults] = await Promise.all([
      Promise.all(momentPromises),
      Promise.all(healthPromises)
    ])

    allMoments.value = momentResults.flat().sort((a, b) =>
      (b.occurredAt || '').localeCompare(a.occurredAt || '')
    )

    for (const h of healthResults) {
      if (h) petSummaries.value[h.petId] = h.summary
    }

    // Fetch agent tasks for the first pet
    if (pets.value.length > 0) {
      try {
        const taskRes = await getAgentTasks(pets.value[0].id)
        agentTasks.value = taskRes.data || []
      } catch {}
    }
  } catch (e) {
    console.error('Dashboard load error:', e)
  } finally {
    loading.value = false
    await nextTick()
    initAnimations()
  }
})

onBeforeUnmount(() => {
  motionMatchMedia?.revert()
})
</script>

<template>
  <div ref="pageRef" class="home-page lookbook-page">
    <section class="hero-lookbook lookbook-animate">
      <div class="hero-copy">
        <p class="kicker">{{ greeting }}，{{ userName }} · {{ today }}</p>
        <h1>宠物时间手帐</h1>
        <p class="hero-line">{{ heroLine }}</p>
        <button class="timeline-link" @click="goTimeline">翻开今日故事 <span>→</span></button>
      </div>
      <aside class="daily-note">
        <span>今日寄语</span>
        <p>{{ dailyWhisper }}</p>
      </aside>
    </section>

    <div class="home-loading" v-if="loading">
      <span class="spinner"></span> 正在翻开手帐...
    </div>

    <template v-else>
      <section class="time-capsule lookbook-animate">
        <div class="capsule-copy">
          <p class="kicker">时间胶囊</p>
          <h2>{{ heroPet?.name || '新的小伙伴' }}的今日一页</h2>
          <p>{{ momentText(latestMoment) }}</p>
          <button class="under-link" @click="goTimeline">去时间线收藏这一刻</button>
        </div>
        <div class="capsule-photo">
          <img v-if="heroImage" :src="heroImage" :alt="heroPet?.name || '宠物照片'" />
          <div v-else class="photo-placeholder"><ThemedIcon name="paw" :size="42" /></div>
          <span>{{ momentDate(latestMoment) }}</span>
        </div>
      </section>

      <div class="lookbook-layout">
        <main class="story-column">
          <section class="story-card feature-card lookbook-animate" @click="goTimeline">
            <p class="kicker">今日故事书</p>
            <h2>把日子写得柔软一点</h2>
            <p>最新照片、碎碎念和里程碑会在这里悄悄发光。主页不再催促你看数字，只提醒你：陪伴本身已经很珍贵。</p>
            <div class="mini-gallery">
              <div v-for="(pet, index) in pets.slice(0, 3)" :key="pet.id" class="mini-photo" :class="`tilt-${index}`">
                <img v-if="pet.avatarUrl" :src="pet.avatarUrl" :alt="pet.name" />
                <span v-else>{{ pet.name?.charAt(0) || '?' }}</span>
              </div>
            </div>
          </section>

          <section class="pet-wall">
            <div class="section-header">
              <div>
                <p class="kicker">我的宠物</p>
                <h2>灵动名片墙</h2>
              </div>
              <button class="under-link" @click="router.push('/pet/create')">新增宠物</button>
            </div>
            <div class="polaroid-grid" v-if="pets.length">
              <article
                v-for="(pet, index) in pets"
                :key="pet.id"
                class="pet-polaroid lookbook-animate"
                :data-tilt="index % 2 === 0 ? -1.4 : 1.2"
                :style="{ '--pet-tint': index % 2 === 0 ? 'rgba(193, 123, 96, 0.12)' : 'rgba(92, 122, 94, 0.12)', transform: `rotate(${index % 2 === 0 ? -1.4 : 1.2}deg)` }"
                @click="goPet(pet.id)"
              >
                <div class="pet-photo">
                  <img v-if="pet.avatarUrl" :src="pet.avatarUrl" :alt="pet.name" />
                  <span v-else>{{ pet.name?.charAt(0) || '?' }}</span>
                </div>
                <div class="pet-caption">
                  <strong>{{ pet.name }}</strong>
                  <small>{{ petTagline(pet, index) }}</small>
                  <button @click.stop="goHealth(pet.id)">健康小档案</button>
                </div>
              </article>
            </div>
            <div class="empty-card" v-else>
              <p>还没有宠物名片，先创建第一位家庭成员吧。</p>
              <button class="timeline-link" @click="router.push('/pet/create')">创建宠物 <span>→</span></button>
            </div>
          </section>

          <section class="highlight-window story-card lookbook-animate">
            <div class="section-header">
              <div>
                <p class="kicker">精选瞬间</p>
                <h2>随机小确幸</h2>
              </div>
              <button class="under-link" @click="goTimeline">查看全部</button>
            </div>
            <div class="highlight-list" v-if="featuredMoments.length">
              <article v-for="moment in featuredMoments" :key="moment.id" class="highlight-item">
                <span>{{ momentDate(moment) }}</span>
                <strong>{{ momentPetName(moment) }}</strong>
                <p>{{ momentText(moment) }}</p>
              </article>
            </div>
            <p class="no-data" v-else>暂无记录。也许今晚就会有一个值得写下来的瞬间。</p>
          </section>
        </main>

        <aside class="side-column">
          <section class="side-note agent-tasks-card lookbook-animate" v-if="agentTasks.length > 0">
            <p class="kicker">AI 管家</p>
            <h3>{{ heroPet?.name || '宠物' }}的待办</h3>
            <div class="agent-task-list">
              <div v-for="task in agentTasks.slice(0, 3)" :key="task.id" class="agent-task-item">
                <div class="agent-task-head">
                  <ThemedIcon :name="task.type === 'play' ? 'collar' : task.type === 'care' ? 'heartPulse' : task.type === 'photo' ? 'image' : 'scroll'" :size="12" />
                  <strong>{{ task.title }}</strong>
                </div>
                <p>{{ task.reason }}</p>
                <button class="under-link" @click="runAgentTask(task)">去完成 →</button>
              </div>
            </div>
          </section>

          <section class="side-note lookbook-animate">
            <p class="kicker">今日关怀</p>
            <h3>今天的小提醒</h3>
            <p>{{ dailyWhisper }}</p>
            <button class="under-link" @click="goAiChat">问问时光顾问</button>
          </section>

          <section class="side-note lookbook-animate">
            <p class="kicker">最近里程碑</p>
            <div class="milestone-list" v-if="recentMilestones.length">
              <div class="ms-item" v-for="m in recentMilestones" :key="m.id">
                <span class="ms-dot"></span>
                <div>
                  <strong>{{ m.milestoneLabel }}</strong>
                  <small>{{ momentDate(m) }}</small>
                </div>
              </div>
            </div>
            <p class="no-data compact" v-else>还没有里程碑，第一枚会很珍贵。</p>
          </section>

          <section class="side-note lookbook-animate">
            <p class="kicker">健康小贴士</p>
            <div class="care-list" v-if="healthAlerts.length">
              <div class="care-item" v-for="(a, i) in healthAlerts" :key="i" :class="{ urgent: a.urgent }">
                <strong>{{ a.pet }} · {{ a.type }}</strong>
                <span v-if="a.days > 0">还有 {{ a.days }} 天</span>
                <span v-else>已过期</span>
              </div>
            </div>
            <p v-else>今天没有临近的健康事项，可以安心陪它玩一会儿。</p>
          </section>

          <section class="side-note quick-note lookbook-animate">
            <button @click="goTimeline"><ThemedIcon name="scroll" :size="18" /> 时间线</button>
            <button @click="goKnowledge"><ThemedIcon name="book" :size="18" /> 知识库</button>
            <button @click="goAiChat"><ThemedIcon name="quill" :size="18" /> 时光顾问</button>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

<style scoped>
.home-page {
  flex: 1;
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 1.25rem 1.5rem 2rem;
}
.lookbook-page {
  color: var(--text-primary);
}
.lookbook-animate,
.pet-polaroid,
.story-card,
.side-note,
.hero-lookbook,
.time-capsule {
  will-change: transform, opacity;
}
.kicker {
  margin: 0 0 0.35rem;
  color: var(--accent);
  font-size: 0.76rem;
  letter-spacing: 0;
}
.hero-lookbook {
  display: flex;
  align-items: stretch;
  gap: 1.6rem;
  padding: 1.1rem 0 1rem;
  margin-bottom: 0.8rem;
  border-top: 2px solid var(--accent);
}
.hero-copy {
  flex: 1;
  min-width: 0;
}
.hero-copy h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.6rem;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: 0.02em;
  color: var(--text-primary);
}
.hero-line {
  max-width: 480px;
  margin: 0.3rem 0 0.7rem;
  color: var(--text-muted);
  font-size: 0.82rem;
  line-height: 1.6;
}
.daily-note {
  flex-shrink: 0;
  max-width: 220px;
  padding: 0.4rem 0 0 1.2rem;
  border-left: 1px solid rgba(92, 122, 94, 0.18);
}
.daily-note span {
  display: block;
  color: var(--text-muted);
  font-size: 0.68rem;
  margin-bottom: 0.3rem;
  letter-spacing: 0.04em;
}
.daily-note p {
  margin: 0;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 0.82rem;
  line-height: 1.7;
}
.timeline-link,
.under-link {
  border: none;
  background: transparent;
  color: var(--accent);
  font-family: var(--font-body);
  cursor: pointer;
}
.timeline-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 0 4px;
  border-bottom: 1px solid currentColor;
  font-size: 0.92rem;
}
.timeline-link span {
  transition: transform var(--duration-fast);
}
.timeline-link:hover span {
  transform: translateX(4px);
}
.under-link {
  font-size: 0.82rem;
  text-decoration: underline;
  text-underline-offset: 4px;
}
.home-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 4rem 0;
  color: var(--text-muted);
}
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--border-default);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.time-capsule {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 1.4rem;
  align-items: center;
  padding: 1rem;
  margin-bottom: 1.1rem;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  background: rgba(255, 254, 249, 0.78);
}
.capsule-copy h2,
.story-card h2,
.pet-wall h2 {
  margin: 0 0 0.55rem;
  font-family: var(--font-display);
  color: var(--text-primary);
}
.capsule-copy p {
  margin: 0 0 0.7rem;
  color: var(--text-secondary);
  line-height: 1.8;
}
.capsule-photo {
  position: relative;
  aspect-ratio: 4 / 3;
  padding: 8px;
  background: #fffdf8;
  border: 1px solid var(--border-subtle);
  box-shadow: 0 8px 20px rgba(61, 50, 38, 0.08);
  transform: rotate(1.5deg);
}
.capsule-photo img,
.photo-placeholder {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: var(--bg-aged);
}
.photo-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
}
.capsule-photo span {
  position: absolute;
  left: 12px;
  bottom: 10px;
  padding: 2px 8px;
  background: rgba(255, 254, 249, 0.86);
  color: var(--text-muted);
  font-size: 0.68rem;
}
.lookbook-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(260px, 0.55fr);
  gap: 1.2rem;
  align-items: start;
}
.story-column,
.side-column {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.story-card,
.side-note,
.empty-card {
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  background: rgba(255, 254, 249, 0.86);
}
.feature-card {
  position: relative;
  min-height: 260px;
  padding: 1.6rem;
  overflow: hidden;
  cursor: pointer;
}
.feature-card p {
  max-width: 520px;
  color: var(--text-secondary);
  line-height: 1.8;
}
.mini-gallery {
  position: absolute;
  right: 1.2rem;
  bottom: 1.2rem;
  display: flex;
  align-items: end;
  gap: 8px;
}
.mini-photo {
  width: 82px;
  height: 104px;
  padding: 6px 6px 18px;
  border: 1px solid var(--border-subtle);
  background: #fffdf8;
  box-shadow: var(--shadow-card);
}
.mini-photo img,
.mini-photo span {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-aged);
  color: var(--accent);
  font-family: var(--font-display);
}
.tilt-0 { transform: rotate(-5deg); }
.tilt-1 { transform: translateY(-16px) rotate(3deg); }
.tilt-2 { transform: rotate(6deg); }
.pet-wall {
  padding: 0.2rem 0;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.75rem;
}
.polaroid-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 1rem;
}
.pet-polaroid {
  padding: 0.65rem 0.65rem 0.85rem;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  background: linear-gradient(180deg, #fffef9 0%, #fbf5e9 100%);
  box-shadow: 0 6px 18px rgba(61, 50, 38, 0.08);
  cursor: pointer;
  transform-origin: center;
}
.pet-polaroid:hover {
  background: linear-gradient(180deg, #fffef9 0%, var(--pet-tint) 100%);
}
.pet-photo {
  aspect-ratio: 1 / 0.78;
  overflow: hidden;
  border-radius: var(--radius-md);
  background: var(--bg-aged);
}
.pet-photo img,
.pet-photo span {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  font-family: var(--font-display);
  font-size: 2rem;
}
.pet-caption {
  padding-top: 0.7rem;
}
.pet-caption strong {
  display: block;
  font-family: var(--font-display);
  font-size: 1.05rem;
}
.pet-caption small {
  display: block;
  min-height: 1.4rem;
  color: var(--text-muted);
  font-size: 0.74rem;
}
.pet-caption button {
  margin-top: 0.45rem;
  border: none;
  background: transparent;
  color: var(--accent);
  font-family: var(--font-body);
  font-size: 0.74rem;
  cursor: pointer;
}
.highlight-window {
  padding: 1rem;
}
.highlight-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}
.highlight-item {
  min-height: 126px;
  padding: 1rem;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: rgba(245, 239, 224, 0.52);
}
.highlight-item span {
  display: block;
  color: var(--text-muted);
  font-size: 0.68rem;
}
.highlight-item strong {
  display: block;
  margin: 0.2rem 0;
  color: var(--accent);
  font-family: var(--font-display);
}
.highlight-item p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.78rem;
  line-height: 1.65;
}
.side-note {
  padding: 1rem;
}
.side-note h3 {
  margin: 0 0 0.45rem;
  font-family: var(--font-display);
}
.side-note p {
  margin: 0 0 0.65rem;
  color: var(--text-secondary);
  font-size: 0.82rem;
  line-height: 1.75;
}
.milestone-list {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.ms-item {
  display: flex;
  gap: 0.55rem;
  align-items: flex-start;
}
.ms-dot {
  width: 8px;
  height: 8px;
  margin-top: 0.5rem;
  border-radius: 50%;
  background: var(--spot);
  flex-shrink: 0;
}
.ms-item strong,
.ms-item small {
  display: block;
}
.ms-item strong {
  color: var(--text-primary);
  font-size: 0.82rem;
}
.ms-item small {
  color: var(--text-muted);
  font-size: 0.68rem;
}
.care-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.care-item {
  display: flex;
  justify-content: space-between;
  gap: 0.6rem;
  padding: 0.55rem;
  border: 1px solid rgba(92, 122, 94, 0.18);
  border-radius: var(--radius-md);
  background: rgba(92, 122, 94, 0.06);
  font-size: 0.76rem;
}
.care-item.urgent {
  border-color: rgba(181, 83, 61, 0.24);
  background: rgba(181, 83, 61, 0.06);
}
.quick-note {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.55rem;
}
.quick-note button {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 0.7rem;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-secondary);
  font-family: var(--font-body);
  cursor: pointer;
}
.quick-note button:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.empty-card {
  padding: 1.2rem;
  text-align: center;
}
.empty-card p,
.no-data {
  color: var(--text-muted);
  font-size: 0.82rem;
}
.no-data {
  padding: 1rem 0;
  text-align: center;
}
.no-data.compact {
  padding: 0;
  text-align: left;
}

/* Agent tasks */
.agent-task-list { display: flex; flex-direction: column; gap: 0.6rem; }
.agent-task-item { padding: 0.5rem; border: 1px solid var(--border-subtle); border-radius: var(--radius-md); transition: border-color var(--duration-fast); }
.agent-task-item:hover { border-color: var(--accent); }
.agent-task-head { display: flex; align-items: center; gap: 6px; }
.agent-task-head strong { font-size: 0.82rem; font-family: var(--font-display); }
.agent-task-item p { margin: 0.2rem 0 0.3rem; font-size: 0.74rem; color: var(--text-muted); line-height: 1.5; }

@media (max-width: 900px) {
  .hero-lookbook,
  .time-capsule,
  .lookbook-layout {
    grid-template-columns: 1fr;
  }
  .mini-gallery {
    position: static;
    margin-top: 1rem;
  }
}

@media (max-width: 620px) {
  .home-page {
    padding: 1rem;
  }
  .hero-lookbook {
    flex-direction: column;
    gap: 0.8rem;
  }
  .daily-note {
    max-width: none;
    border-left: none;
    border-top: 1px solid rgba(92, 122, 94, 0.18);
    padding: 0.6rem 0 0 0;
  }
  .time-capsule {
    grid-template-columns: 1fr;
  }
  .capsule-photo {
    transform: none;
  }
  .highlight-list {
    grid-template-columns: 1fr;
  }
  .polaroid-grid {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .spinner {
    animation: none;
  }
  .timeline-link span {
    transition: none;
  }
}
</style>
