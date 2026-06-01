<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { gsap } from 'gsap'
import { listKnowledge, getFavoriteIds, addFavorite, removeFavorite } from '../api/knowledge'
import ThemedIcon from '../components/common/ThemedIcon.vue'

const docs = ref([])
const favoriteIds = ref(new Set())
const loading = ref(true)
const error = ref('')
const activeCategory = ref('')
const searchQuery = ref('')
const expandedId = ref(null)
const showFavoritesOnly = ref(false)
const pageRef = ref(null)
let motionMatchMedia = null

const categories = ['喂养', '健康', '行为', '训练', '新手', '品种', '季节', '日常', '老年', '通用']

const categoryIcons = {
  '喂养': 'heart', '健康': 'stethoscope', '行为': 'paw', '训练': 'flag',
  '新手': 'pawPlus', '品种': 'collar', '季节': 'scroll', '日常': 'book',
  '老年': 'bookmark', '通用': 'folder'
}

const filteredDocs = computed(() => {
  let list = docs.value
  if (showFavoritesOnly.value) {
    list = list.filter(d => favoriteIds.value.has(d.id))
  }
  if (activeCategory.value) {
    list = list.filter(d => d.category === activeCategory.value)
  }
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(d =>
      d.title.toLowerCase().includes(q) ||
      (d.content || '').toLowerCase().includes(q)
    )
  }
  return list
})

const categoryCounts = computed(() => {
  const counts = {}
  docs.value.forEach(d => { counts[d.category] = (counts[d.category] || 0) + 1 })
  return counts
})

const favoriteCount = computed(() => favoriteIds.value.size)

function initMotion() {
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
      if (reduceMotion) {
        gsap.set(pageRef.value.querySelectorAll('.kb-animate'), { autoAlpha: 1, clearProps: 'transform' })
        return undefined
      }
      const tl = gsap.timeline({ defaults: { duration: 0.6, ease: 'power3.out' } })
      tl.from('.kb-hero', { autoAlpha: 0, y: 24 })
        .from('.kb-toolbar', { autoAlpha: 0, y: 18 }, '-=0.34')
        .from('.kb-doc', { autoAlpha: 0, y: 18, stagger: 0.05 }, '-=0.28')
      return () => tl.kill()
    },
    pageRef.value
  )
}

onMounted(async () => {
  try {
    const [docRes, favRes] = await Promise.all([listKnowledge(), getFavoriteIds()])
    docs.value = docRes.data || []
    favoriteIds.value = new Set(favRes.data || [])
  } catch (e) {
    error.value = e?.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
    await nextTick()
    initMotion()
  }
})

watch(filteredDocs, async () => {
  await nextTick()
  initMotion()
})

onBeforeUnmount(() => {
  motionMatchMedia?.revert()
})

async function toggleFavorite(docId) {
  const isFav = favoriteIds.value.has(docId)
  try {
    if (isFav) {
      await removeFavorite(docId)
      favoriteIds.value.delete(docId)
    } else {
      await addFavorite(docId)
      favoriteIds.value.add(docId)
    }
    favoriteIds.value = new Set(favoriteIds.value)
  } catch (e) {
    error.value = e?.response?.data?.message || '操作失败'
  }
}

function toggleExpand(id) {
  expandedId.value = expandedId.value === id ? null : id
}

function filterCategory(cat) {
  activeCategory.value = activeCategory.value === cat ? '' : cat
}

function clearFilters() {
  searchQuery.value = ''
  activeCategory.value = ''
  showFavoritesOnly.value = false
}
</script>

<template>
  <div ref="pageRef" class="kb-page">
    <section class="kb-hero kb-animate">
      <div>
        <p class="kicker">知识库</p>
        <h1><ThemedIcon name="book" :size="24" /> 宠物护理手册</h1>
        <p>把喂养、健康、行为和训练知识，整理成一本温柔可查的家庭小百科。</p>
      </div>
      <button class="kb-fav-toggle" :class="{ 'is-active': showFavoritesOnly }" @click="showFavoritesOnly = !showFavoritesOnly">
        <ThemedIcon name="bookmark" :size="16" />
        {{ showFavoritesOnly ? '查看全部' : '我的收藏' }}
        <span class="kb-fav-count" v-if="favoriteCount">{{ favoriteCount }}</span>
      </button>
    </section>

    <section class="kb-toolbar kb-animate">
      <div class="kb-search">
        <ThemedIcon name="chat" :size="14" class="kb-search-icon" />
        <input v-model="searchQuery" class="kb-search-input" placeholder="搜索标题或正文..." maxlength="100" />
        <button v-if="searchQuery" class="kb-search-clear" @click="searchQuery = ''">×</button>
      </div>
      <div class="kb-categories">
        <button
          v-for="cat in categories"
          :key="cat"
          class="kb-cat-btn"
          :class="{ 'is-active': activeCategory === cat && !showFavoritesOnly }"
          @click="showFavoritesOnly = false; filterCategory(cat)"
        >
          <ThemedIcon :name="categoryIcons[cat]" :size="12" />
          {{ cat }}
          <span class="kb-cat-count" v-if="categoryCounts[cat]">{{ categoryCounts[cat] }}</span>
        </button>
      </div>
    </section>

    <div class="kb-state" v-if="loading">
      <span class="spinner"></span> 正在翻找护理手册...
    </div>
    <div class="kb-state kb-error" v-else-if="error">
      <p>{{ error }}</p>
    </div>

    <template v-else>
      <div class="kb-layout">
        <div class="kb-list" v-if="filteredDocs.length > 0">
            <article class="kb-doc kb-animate" v-for="doc in filteredDocs" :key="doc.id" :class="{ 'is-expanded': expandedId === doc.id }">
              <div class="kb-doc-header" @click="toggleExpand(doc.id)">
                <div class="kb-doc-left">
                  <span class="kb-doc-icon-wrap">
                    <ThemedIcon :name="categoryIcons[doc.category] || 'book'" :size="16" class="kb-doc-icon" />
                  </span>
                  <div class="kb-doc-info">
                    <span class="kb-doc-title">{{ doc.title }}</span>
                    <span class="kb-doc-meta">
                      <span class="kb-doc-cat">{{ doc.category }}</span>
                      <span>{{ doc.chunkCount || 0 }} 个分块</span>
                      <span v-if="doc.source">· {{ doc.source }}</span>
                    </span>
                  </div>
                </div>
                <div class="kb-doc-right">
                  <button
                    class="kb-fav-btn"
                    :class="{ 'is-fav': favoriteIds.has(doc.id) }"
                    :title="favoriteIds.has(doc.id) ? '取消收藏' : '收藏'"
                    @click.stop="toggleFavorite(doc.id)"
                  >
                    <ThemedIcon name="bookmark" :size="14" />
                  </button>
                  <span class="kb-expand-icon">{{ expandedId === doc.id ? '−' : '+' }}</span>
                </div>
              </div>
              <div class="kb-doc-body" v-if="expandedId === doc.id">
                <div class="kb-doc-content" v-text="doc.content || '暂无内容'"></div>
              </div>
            </article>
          </div>

          <div class="kb-empty kb-animate" v-else>
            <ThemedIcon name="book" :size="40" class="kb-empty-icon" />
            <p v-if="showFavoritesOnly">还没有收藏的文档</p>
            <p v-else-if="searchQuery || activeCategory">没有找到匹配的文档</p>
            <p v-else>知识库暂无文档</p>
            <button class="kb-btn" @click="clearFilters">清除筛选</button>
          </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.kb-page {
  flex: 1;
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 1.25rem 1.5rem 2rem;
}
.kb-animate,
.kb-hero,
.kb-toolbar,
.kb-doc {
  will-change: transform, opacity;
}
.kicker {
  margin: 0 0 0.35rem;
  color: var(--accent);
  font-size: 0.76rem;
}
.kb-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.2rem;
  padding: 1.1rem 0 1rem;
  margin-bottom: 0.8rem;
  border-top: 2px solid var(--accent);
}
.kb-hero > div:first-child {
  flex: 1;
  min-width: 0;
}
.kb-hero h1 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.6rem;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: 0.02em;
  color: var(--text-primary);
}
.kb-hero > div:first-child p:last-child {
  margin: 0.3rem 0 0;
  font-size: 0.82rem;
  color: var(--text-muted);
  line-height: 1.6;
}
.kb-fav-toggle {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  border: 1px solid var(--spot);
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.8rem;
  color: var(--spot);
  border-radius: var(--radius-sm);
  cursor: pointer;
  flex-shrink: 0;
}
.kb-fav-toggle:hover,
.kb-fav-toggle.is-active {
  background: var(--spot);
  color: #fff;
}
.kb-fav-count {
  font-size: 0.72rem;
  background: rgba(255, 255, 255, 0.25);
  padding: 1px 7px;
  border-radius: var(--radius-full);
}
.kb-toolbar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 1rem;
  padding: 1rem;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  background: rgba(255, 254, 249, 0.86);
}
.kb-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-full);
  background: var(--bg-input);
}
.kb-search:focus-within { border-color: var(--accent); }
.kb-search-icon { color: var(--text-muted); flex-shrink: 0; }
.kb-search-input {
  flex: 1;
  padding: 9px 0;
  border: none;
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.88rem;
  color: var(--text-primary);
  outline: none;
}
.kb-search-input::placeholder { color: var(--text-muted); }
.kb-search-clear {
  background: none;
  border: none;
  font-size: 1.1rem;
  color: var(--text-muted);
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}
.kb-categories {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.kb-cat-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 11px;
  border: 1px solid var(--border-default);
  background: rgba(255, 254, 249, 0.7);
  font-family: var(--font-body);
  font-size: 0.78rem;
  color: var(--text-secondary);
  border-radius: var(--radius-full);
  cursor: pointer;
}
.kb-cat-btn:hover,
.kb-cat-btn.is-active {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-surface);
}
.kb-cat-count {
  font-size: 0.68rem;
  color: var(--text-muted);
  background: var(--bg-aged);
  padding: 0 5px;
  border-radius: var(--radius-full);
}
.kb-layout {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.kb-list {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}
.kb-doc {
  background: rgba(255, 254, 249, 0.9);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 6px 16px rgba(61, 50, 38, 0.05);
}
.kb-doc:hover { border-color: var(--accent-glow); }
.kb-doc.is-expanded { border-color: var(--accent); }
.kb-doc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 13px 16px;
  cursor: pointer;
  gap: 12px;
}
.kb-doc-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}
.kb-doc-icon-wrap {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(92, 122, 94, 0.2);
  border-radius: 42% 58% 50% 50%;
  background: rgba(92, 122, 94, 0.06);
}
.kb-doc-icon { color: var(--accent); }
.kb-doc-info { flex: 1; min-width: 0; }
.kb-doc-title {
  display: block;
  font-family: var(--font-display);
  font-size: 0.98rem;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kb-doc-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 2px;
}
.kb-doc-cat {
  font-size: 0.68rem;
  color: var(--accent);
  background: var(--accent-surface);
  padding: 0 6px;
  border-radius: var(--radius-full);
}
.kb-doc-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.kb-fav-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 4px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
}
.kb-fav-btn:hover,
.kb-fav-btn.is-fav {
  color: var(--spot);
}
.kb-expand-icon {
  font-size: 1.1rem;
  color: var(--text-muted);
  width: 20px;
  text-align: center;
  font-family: var(--font-mono);
}
.kb-doc-body {
  padding: 0 16px 16px;
  border-top: 1px solid var(--border-subtle);
  background: rgba(245, 239, 224, 0.28);
}
.kb-doc-content {
  font-size: 0.88rem;
  line-height: 1.8;
  color: var(--text-secondary);
  white-space: pre-wrap;
  padding-top: 12px;
}
.kb-state,
.kb-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 4rem 2rem;
  color: var(--text-muted);
  text-align: center;
}
.kb-error { color: var(--danger); }
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--border-default);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.kb-btn {
  padding: 7px 14px;
  border: 1px solid var(--border-default);
  background: var(--bg-surface);
  font-family: var(--font-body);
  font-size: 0.82rem;
  color: var(--text-secondary);
  border-radius: var(--radius-full);
  cursor: pointer;
}
.kb-btn:hover { border-color: var(--accent); color: var(--accent); }

@media (max-width: 860px) {
  .kb-hero {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 600px) {
  .kb-page { padding: 1rem; }
  .kb-categories {
    overflow-x: auto;
    flex-wrap: nowrap;
    padding-bottom: 4px;
  }
  .kb-cat-btn {
    white-space: nowrap;
  }
}

@media (prefers-reduced-motion: reduce) {
  .spinner {
    animation: none;
  }
}
</style>
