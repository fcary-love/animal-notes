<script setup>
import { computed, onMounted, ref } from 'vue'
import { usePets } from '../../composables/usePets'
import { getTimeline } from '../../api/moment'
import ThemedIcon from '../common/ThemedIcon.vue'

const { pets, fetchPets } = usePets()
const loaded = ref(false)
const moments = ref([])

const anniversaries = computed(() => {
  const petDates = pets.value.flatMap(p => {
    const items = []
    if (p.birthDate || p.birthday) {
      items.push({ id: `birth-${p.id}`, petName: p.name, title: `${p.name} 的生日`, date: p.birthDate || p.birthday, desc: `${p.name} 来到世界的那一天。`, icon: 'cake' })
    }
    if (p.adoptionDate || p.createdAt) {
      items.push({ id: `home-${p.id}`, petName: p.name, title: `${p.name} 到家纪念`, date: p.adoptionDate || p.createdAt, desc: '家里从这一天开始多了一份柔软。', icon: 'heart' })
    }
    return items
  })
  const milestoneItems = moments.value
    .filter(m => m.milestoneLabel || m.isMilestone)
    .map(m => ({
      id: `moment-${m.id}`,
      petName: pets.value.find(p => p.id === Number(m.petId))?.name || '宠物',
      title: m.milestoneLabel || '里程碑',
      date: m.occurredAt,
      desc: m.content || '一个值得珍藏的成长瞬间。',
      icon: 'bookmark'
    }))
  return [...petDates, ...milestoneItems]
    .filter(i => i.date)
    .sort((a, b) => new Date(b.date) - new Date(a.date))
    .slice(0, 8)
})

function daysSince(dateStr) {
  const d = new Date(dateStr)
  if (Number.isNaN(d.getTime())) return 0
  return Math.max(0, Math.floor((new Date() - d) / 86400000))
}

function formatDate(dateStr) {
  const d = new Date(dateStr)
  if (Number.isNaN(d.getTime())) return dateStr?.slice(0, 10) || ''
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}

onMounted(async () => {
  if (pets.value.length === 0) await fetchPets()
  const results = await Promise.all(pets.value.map(p =>
    getTimeline(p.id, { sort: 'desc', page: 1, size: 50 })
      .then(res => res.data?.records || [])
      .catch(() => [])
  ))
  moments.value = results.flat()
  loaded.value = true
})
</script>

<template>
  <div class="panel-content" :class="{ loaded }">
    <div class="ann-grid" v-if="anniversaries.length">
      <div v-for="(item, idx) in anniversaries" :key="item.id" class="ann-card" :class="{ wide: idx % 3 === 0 }" :style="{ '--i': idx }">
        <div class="ann-photo">
          <ThemedIcon :name="item.icon" :size="34" />
          <div class="ann-photo-tape"></div>
        </div>
        <div class="ann-date-badge">
          <span class="ann-date-absolute">{{ formatDate(item.date) }}</span>
          <span class="ann-date-relative">{{ daysSince(item.date) }} 天前</span>
        </div>
        <h3 class="ann-title">{{ item.title }}</h3>
        <p class="ann-desc">{{ item.desc }}</p>
      </div>
    </div>

    <div v-else class="ann-empty">
      <ThemedIcon name="image" :size="36" />
      <p>{{ loaded ? '还没有纪念日记录' : '正在整理纪念日...' }}</p>
    </div>
  </div>
</template>

<style scoped>
.panel-content { opacity: 0; transform: translateY(12px); transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1); }
.panel-content.loaded { opacity: 1; transform: translateY(0); }
.ann-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.ann-card {
  background: var(--bg-warm); border: 1px solid var(--border-subtle); border-radius: var(--radius-lg);
  padding: 14px; opacity: 0; transform: translateY(10px) rotate(-0.5deg); transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}
.loaded .ann-card { opacity: 1; transform: translateY(0) rotate(0); transition-delay: calc(0.08s * var(--i)); }
.ann-card.wide { grid-column: 1 / -1; }
.ann-card:hover { transform: rotate(0) translateY(-2px); box-shadow: 0 6px 20px rgba(61, 50, 38, 0.08); }
.ann-photo {
  width: 100%; aspect-ratio: 4 / 3; background: linear-gradient(135deg, var(--bg-aged), var(--bg-warm));
  border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; margin-bottom: 10px;
  position: relative; overflow: hidden; border: 1px solid var(--border-subtle); color: var(--accent);
}
.ann-photo-tape {
  position: absolute; top: -4px; left: 50%; transform: translateX(-50%) rotate(-2deg);
  width: 48px; height: 16px; background: rgba(255, 240, 200, 0.6); border: 1px solid rgba(200, 180, 140, 0.3);
}
.ann-date-badge { display: flex; align-items: baseline; gap: 6px; margin-bottom: 6px; }
.ann-date-absolute {
  font-family: var(--font-mono); font-size: 0.65rem; color: var(--accent);
  padding: 2px 7px; border: 1px solid var(--accent); border-radius: var(--radius-full); opacity: 0.7;
}
.ann-date-relative { font-size: 0.62rem; color: var(--text-muted); }
.ann-title { margin: 0 0 4px; font-family: var(--font-display); font-size: 0.88rem; color: var(--text-primary); line-height: 1.4; }
.ann-desc { margin: 0; font-size: 0.75rem; color: var(--text-muted); line-height: 1.6; }
.ann-empty { text-align: center; padding: 40px 0; color: var(--text-muted); }
.ann-empty p { margin: 8px 0 0; font-size: 0.82rem; }
</style>
