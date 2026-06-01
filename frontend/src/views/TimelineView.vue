<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { gsap } from 'gsap'
import { useAuth } from '../composables/useAuth'
import { usePets } from '../composables/usePets'
import { useMoments } from '../composables/useMoments'
import PetSwitcher from '../components/pet/PetSwitcher.vue'
import TimelineList from '../components/timeline/TimelineList.vue'
import MomentForm from '../components/timeline/MomentForm.vue'
import SmartSearch from '../components/search/SmartSearch.vue'
import ThemedIcon from '../components/common/ThemedIcon.vue'

const router = useRouter()
const { fetchMe } = useAuth()
const { pets, currentPet, fetchPets, loading, error, remove } = usePets()
const { records, total, loading: ml, error: me, sort, milestoneOnly, fetchTimeline, create, update, remove: removeMoment, toggleSort, toggleMilestone } = useMoments()

const pageRef = ref(null)
const showForm = ref(false)
const editingMoment = ref(null)
const formLoading = ref(false)
let motionMatchMedia = null

function runPageMotion() {
  if (!pageRef.value) return
  motionMatchMedia?.revert()
  motionMatchMedia = gsap.matchMedia()
  motionMatchMedia.add(
    {
      reduceMotion: '(prefers-reduced-motion: reduce)',
      isMobile: '(max-width: 760px)'
    },
    (context) => {
      const { reduceMotion, isMobile } = context.conditions
      if (reduceMotion) {
        gsap.set(pageRef.value.querySelectorAll('.tl-animate'), { autoAlpha: 1, clearProps: 'transform' })
        return undefined
      }
      const tl = gsap.timeline({ defaults: { duration: 0.62, ease: 'power3.out' } })
      tl.from('.timeline-hero', { autoAlpha: 0, y: 24 })
        .from('.journal-sidebar', { autoAlpha: 0, x: isMobile ? 0 : -18, y: isMobile ? 16 : 0 }, '-=0.35')
        .from('.story-surface', { autoAlpha: 0, y: 22, stagger: 0.08 }, '-=0.36')
      return () => tl.kill()
    },
    pageRef.value
  )
}

onMounted(async () => {
  await fetchMe()
  await fetchPets()
  await nextTick()
  runPageMotion()
})

watch(currentPet, async (pet) => {
  if (pet) {
    await fetchTimeline(pet.id)
    await nextTick()
    runPageMotion()
  }
}, { immediate: true })

watch(sort, async () => {
  if (currentPet.value) {
    await fetchTimeline(currentPet.value.id)
    await nextTick()
    runPageMotion()
  }
})

watch(milestoneOnly, async () => {
  if (currentPet.value) {
    await fetchTimeline(currentPet.value.id)
    await nextTick()
    runPageMotion()
  }
})

onBeforeUnmount(() => {
  motionMatchMedia?.revert()
})

function goCard() {
  if (currentPet.value) router.push(`/card/${currentPet.value.id}`)
}

function goReport() {
  if (currentPet.value) router.push(`/pet/${currentPet.value.id}/report`)
}

function go3D() {
  if (currentPet.value) router.push(`/pet/${currentPet.value.id}/3d`)
}

function goHealth() {
  if (currentPet.value) router.push(`/pet/${currentPet.value.id}/health`)
}

function goCreate() { router.push('/pet/create') }
function goEdit(pet) { router.push(`/pet/${pet.id}`) }

async function onDelete(pet) {
  if (!confirm(`确定删除「${pet.name}」吗？`)) return
  try { await remove(pet.id) } catch { /* handled */ }
}

function openCreateForm() {
  editingMoment.value = null
  showForm.value = true
}

function openEditForm(moment) {
  editingMoment.value = moment
  showForm.value = true
}

async function onFormSubmit(data) {
  if (!currentPet.value) return
  formLoading.value = true
  try {
    if (editingMoment.value) {
      await update(editingMoment.value.id, data)
    } else {
      await create(currentPet.value.id, data)
    }
    showForm.value = false
    editingMoment.value = null
    await fetchTimeline(currentPet.value.id)
  } catch { /* handled in composable */ } finally {
    formLoading.value = false
  }
}

async function onMomentDelete(moment) {
  try { await removeMoment(moment.id) } catch { /* handled */ }
}
</script>

<template>
  <div ref="pageRef" class="timeline-page">
    <section class="timeline-hero tl-animate">
      <div>
        <p class="kicker">时间线</p>
        <h1>{{ currentPet?.name || '宠物' }}的故事书</h1>
        <p>把每一次试探、撒娇、长大，都装订成能反复翻看的日子。</p>
      </div>
      <button class="hero-write" :disabled="!currentPet" @click="openCreateForm">
        <ThemedIcon name="quill" :size="15" /> 写下今天
      </button>
    </section>

    <div class="timeline-layout">
      <aside class="journal-sidebar tl-animate">
        <p class="sidebar-label">宠物索引</p>
        <PetSwitcher @create="goCreate" @edit="goEdit" />
      </aside>

      <main class="timeline-main">
        <div class="loading-msg" v-if="loading">
          <span class="spinner"></span>
          <span>正在翻找档案...</span>
        </div>

        <div class="error-msg story-surface" v-else-if="error">
          <p>{{ error }}</p>
          <button @click="fetchPets">重试</button>
        </div>

        <div class="empty-state story-surface" v-else-if="pets.length === 0">
          <div class="empty-illustration"><span class="empty-icon">&#9783;</span></div>
          <h2 class="empty-title">还没有宠物档案</h2>
          <p class="empty-desc">先创建一只宠物的档案，然后开始记录它的每一个重要时刻。</p>
          <button class="empty-action" @click="goCreate">创建第一份档案</button>
        </div>

        <div class="empty-state story-surface" v-else-if="!currentPet">
          <p class="empty-desc">从左侧选择一只宠物。</p>
        </div>

        <template v-else>
          <section class="story-surface pet-story-card tl-animate">
            <div class="current-pet-note">
              <div class="current-avatar">
                <img v-if="currentPet.avatarUrl" :src="currentPet.avatarUrl" :alt="currentPet.name" />
                <span v-else>{{ currentPet.name?.charAt(0) || '?' }}</span>
              </div>
              <div class="current-copy">
                <p class="kicker">正在翻阅</p>
                <h2>{{ currentPet.name }}</h2>
                <p>{{ currentPet.species }}{{ currentPet.breed ? ' · ' + currentPet.breed : '' }}</p>
                <small v-if="currentPet.bio">{{ currentPet.bio }}</small>
              </div>
            </div>
          </section>

          <section class="story-surface search-surface tl-animate">
            <SmartSearch />
          </section>

          <section class="story-surface moment-form-area tl-animate" v-if="showForm">
            <MomentForm
              :key="editingMoment ? editingMoment.id : 'new'"
              :model-value="editingMoment"
              :loading="formLoading"
              @submit="onFormSubmit"
              @cancel="showForm = false"
            />
          </section>

          <section class="story-surface timeline-book tl-animate">
            <TimelineList
              :pet-id="currentPet.id"
              :records="records"
              :total="total"
              :loading="ml"
              :error="me"
              :sort="sort"
              :milestone-only="milestoneOnly"
              @create="openCreateForm"
              @edit="openEditForm"
              @delete="onMomentDelete"
              @toggle-sort="toggleSort"
              @toggle-milestone="toggleMilestone"
              @card="goCard"
              @report="goReport"
              @d3d="go3D"
              @health="goHealth"
              @fetch="fetchTimeline(currentPet.id)"
            />
          </section>
        </template>
      </main>
    </div>
  </div>
</template>

<style scoped>
.timeline-page {
  flex: 1;
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 1.25rem 1.5rem 2rem;
}
.tl-animate,
.story-surface,
.journal-sidebar,
.timeline-hero {
  will-change: transform, opacity;
}
.kicker,
.sidebar-label {
  margin: 0 0 0.35rem;
  color: var(--accent);
  font-size: 0.76rem;
  letter-spacing: 0;
}
.timeline-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1.2rem;
  padding: 1.1rem 0 1rem;
  margin-bottom: 0.8rem;
  border-top: 2px solid var(--accent);
}
.timeline-hero > div:first-child {
  flex: 1;
  min-width: 0;
}
.timeline-hero h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.6rem;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: 0.02em;
  color: var(--text-primary);
}
.timeline-hero > div:first-child p:last-child {
  margin: 0.3rem 0 0;
  font-size: 0.82rem;
  color: var(--text-muted);
  line-height: 1.6;
}
.hero-write {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  border: 1px solid var(--accent);
  border-radius: var(--radius-sm);
  background: var(--accent);
  color: #fff;
  font-family: var(--font-body);
  font-size: 0.8rem;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}
.hero-write:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.timeline-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 1.1rem;
  align-items: start;
}
.journal-sidebar {
  position: sticky;
  top: 78px;
  padding: 1rem;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  background: rgba(255, 254, 249, 0.86);
}
.timeline-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.story-surface {
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  background: rgba(255, 254, 249, 0.86);
  box-shadow: 0 6px 18px rgba(61, 50, 38, 0.05);
}
.pet-story-card {
  padding: 1rem;
}
.current-pet-note {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.current-avatar {
  width: 76px;
  height: 76px;
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  border-radius: 38% 62% 52% 48%;
  background: var(--bg-aged);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  font-family: var(--font-display);
  font-size: 1.6rem;
}
.current-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.current-copy h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.45rem;
  color: var(--text-primary);
}
.current-copy p {
  margin: 0.2rem 0;
  color: var(--text-secondary);
}
.current-copy small {
  display: block;
  color: var(--text-muted);
  line-height: 1.6;
}
.search-surface,
.moment-form-area,
.timeline-book {
  padding: 1rem;
}
.loading-msg {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: center;
  padding: 4rem 0;
  color: var(--text-muted);
  font-size: 0.95rem;
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
.error-msg {
  text-align: center;
  padding: 4rem 0;
  color: var(--danger);
}
.error-msg button,
.empty-action {
  margin-top: 1rem;
  padding: 8px 18px;
  border: 1px solid var(--accent);
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.85rem;
  color: var(--accent);
  cursor: pointer;
  border-radius: var(--radius-sm);
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  text-align: center;
}
.empty-illustration { margin-bottom: 1.5rem; }
.empty-icon { font-size: 5rem; color: var(--accent); opacity: 0.3; }
.empty-title {
  font-family: var(--font-display);
  font-size: 1.5rem;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}
.empty-desc {
  font-size: 0.95rem;
  color: var(--text-muted);
  max-width: 340px;
  line-height: 1.7;
}

@media (max-width: 860px) {
  .timeline-layout {
    grid-template-columns: 1fr;
  }
  .journal-sidebar {
    position: static;
  }
  .timeline-hero {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 560px) {
  .timeline-page {
    padding: 1rem;
  }
  .search-surface,
  .moment-form-area,
  .timeline-book {
    padding: 0.8rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .spinner {
    animation: none;
  }
}
</style>
