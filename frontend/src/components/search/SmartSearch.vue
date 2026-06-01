<script setup>
import { ref } from 'vue'
import { searchMoments } from '../../api/ai'
import { useRouter } from 'vue-router'

const query = ref('')
const results = ref([])
const searching = ref(false)
const showResults = ref(false)
const router = useRouter()

async function doSearch() {
  const q = query.value.trim()
  if (!q) { results.value = []; showResults.value = false; return }
  searching.value = true
  try {
    const res = await searchMoments(q)
    results.value = res.data
    showResults.value = true
  } catch { results.value = [] } finally {
    searching.value = false
  }
}

function clear() {
  query.value = ''
  results.value = []
  showResults.value = false
}
</script>

<template>
  <div class="smart-search">
    <div class="search-bar">
      <input
        v-model="query" class="search-input"
        placeholder="用自然语言搜索时刻，如：奶茶第一次去医院..."
        @keyup.enter="doSearch"
        @keyup.esc="clear"
        maxlength="200"
      />
      <button class="search-btn" @click="doSearch" :disabled="searching || !query.trim()">
        {{ searching ? '搜索中...' : '搜索' }}
      </button>
      <button class="search-btn-clear" v-if="query" @click="clear">✕</button>
    </div>
    <div class="search-results" v-if="showResults">
      <div class="result-empty" v-if="results.length === 0">未找到相关时刻</div>
      <div class="result-item" v-for="r in results" :key="r.momentId">
        <div class="result-score">相关度 {{ (r.score * 100).toFixed(0) }}%</div>
        <div class="result-content">{{ r.content || '(无文字内容)' }}</div>
        <div class="result-meta">
          <span v-if="r.occurredAt">{{ r.occurredAt }}</span>
          <span class="result-badge" v-if="r.milestoneLabel">{{ r.milestoneLabel }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.smart-search { margin-bottom: 1rem; }
.search-bar { display: flex; gap: 6px; }
.search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  font-family: var(--font-body);
  font-size: 0.85rem;
  background: var(--bg-input);
  color: var(--text-primary);
  outline: none;
}
.search-input:focus { border-color: var(--accent); }
.search-btn {
  padding: 8px 14px;
  border: 1px solid var(--accent);
  background: transparent;
  color: var(--accent);
  font-family: var(--font-body);
  font-size: 0.82rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  white-space: nowrap;
}
.search-btn:hover:not(:disabled) { background: var(--accent); color: var(--text-inverse); }
.search-btn:disabled { opacity: 0.5; }
.search-btn-clear {
  padding: 8px 10px;
  border: 1px solid var(--border-default);
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: var(--radius-sm);
  font-size: 0.8rem;
}
.search-results {
  margin-top: 8px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  max-height: 260px;
  overflow-y: auto;
  background: var(--bg-surface);
}
.result-empty { padding: 1rem; text-align: center; color: var(--text-muted); font-size: 0.85rem; }
.result-item {
  padding: 10px 14px;
  border-bottom: 1px solid var(--border-subtle);
}
.result-item:last-child { border-bottom: none; }
.result-score { font-size: 0.72rem; color: var(--accent); margin-bottom: 4px; }
.result-content { font-size: 0.85rem; color: var(--text-primary); line-height: 1.5; margin-bottom: 4px; }
.result-meta { font-size: 0.75rem; color: var(--text-muted); display: flex; gap: 8px; align-items: center; }
.result-badge {
  background: var(--spot);
  color: #fff;
  padding: 0 6px;
  border-radius: var(--radius-full);
  font-size: 0.65rem;
}
</style>
