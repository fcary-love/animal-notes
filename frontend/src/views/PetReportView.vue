<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getEnhancedReport } from '../api/agent'
import { generateReport, downloadReportPdf } from '../api/ai'
import { usePets } from '../composables/usePets'
import ThemedIcon from '../components/common/ThemedIcon.vue'

const route = useRoute()
const router = useRouter()
const petId = Number(route.params.id)
const { getPet } = usePets()

const loading = ref(true)
const error = ref('')
const pet = ref(null)
const report = ref(null)
const aiReport = ref('')
const showFullReport = ref(false)
const pdfLoading = ref(false)

onMounted(async () => {
  try {
    pet.value = await getPet(petId)
    const res = await getEnhancedReport(petId)
    report.value = res.data
  } catch (e) {
    error.value = e?.response?.data?.message || e.message || '生成失败'
  } finally {
    loading.value = false
  }
})

async function loadFullReport() {
  showFullReport.value = true
  try {
    const res = await generateReport(petId)
    aiReport.value = res.data.report
  } catch {}
}

async function downloadPdf() {
  pdfLoading.value = true
  try {
    const res = await downloadReportPdf(petId)
    const a = document.createElement('a')
    a.href = res.data.url
    a.download = `${pet.value?.name || 'pet'}-report.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
  } catch {} finally {
    pdfLoading.value = false
  }
}

function goBack() { router.push('/timeline') }

function formatWeightChange(change) {
  if (change == null) return ''
  const sign = change > 0 ? '+' : ''
  return `${sign}${change.toFixed(1)}kg`
}
</script>

<template>
  <div class="report-page">
    <div class="report-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <h1 class="page-title">
        <ThemedIcon name="file" :size="20" />
        {{ pet?.name || '...' }}的成长报告
      </h1>
    </div>

    <div class="report-state" v-if="loading">
      <span class="spinner"></span>
      <span>正在生成报告...</span>
    </div>

    <div class="report-state" v-else-if="error">
      <p class="error-text">{{ error }}</p>
      <button class="retry-btn" @click="router.go(0)">重试</button>
    </div>

    <template v-else-if="report">
      <!-- Period header -->
      <div class="period-header">
        <span class="period-badge">{{ report.period }}</span>
        <span class="period-pet">{{ report.petName }} · {{ report.species }}</span>
      </div>

      <!-- Stats grid -->
      <div class="stats-section">
        <h2 class="section-title">
          <ThemedIcon name="chart" :size="15" />
          数据概览
        </h2>
        <div class="stats-grid">
          <div class="stat-card">
            <span class="stat-num">{{ report.periodMoments }}</span>
            <span class="stat-label">本月记录</span>
            <span class="stat-sub">累计 {{ report.totalMoments }}</span>
          </div>
          <div class="stat-card">
            <span class="stat-num">{{ report.periodPhotos }}</span>
            <span class="stat-label">本月照片</span>
            <span class="stat-sub">累计 {{ report.totalPhotos }}</span>
          </div>
          <div class="stat-card">
            <span class="stat-num">{{ report.periodMilestones }}</span>
            <span class="stat-label">本月里程碑</span>
            <span class="stat-sub">累计 {{ report.totalMilestones }}</span>
          </div>
          <div class="stat-card">
            <span class="stat-num">{{ report.interactionDays }}</span>
            <span class="stat-label">互动天数</span>
            <span class="stat-sub">最近30天</span>
          </div>
        </div>
      </div>

      <!-- Activity breakdown -->
      <div class="section">
        <h2 class="section-title">
          <ThemedIcon name="scroll" :size="15" />
          本月活动
        </h2>
        <div class="activity-list">
          <div class="activity-item">
            <ThemedIcon name="heartPulse" :size="14" />
            <span>健康记录</span>
            <span class="activity-count">{{ report.healthEvents }}条</span>
          </div>
          <div class="activity-item">
            <ThemedIcon name="trend" :size="14" />
            <span>体重记录</span>
            <span class="activity-count">{{ report.weightRecords }}条</span>
          </div>
          <div class="activity-item">
            <ThemedIcon name="bowl" :size="14" />
            <span>饮食记录</span>
            <span class="activity-count">{{ report.dietRecords }}条</span>
          </div>
        </div>
      </div>

      <!-- Weight trend -->
      <div class="section" v-if="report.latestWeight != null">
        <h2 class="section-title">
          <ThemedIcon name="trend" :size="15" />
          体重趋势
        </h2>
        <div class="weight-info">
          <span class="weight-current">{{ report.latestWeight }}kg</span>
          <span class="weight-change" v-if="report.weightChange != null"
            :class="{ up: report.weightChange > 0, down: report.weightChange < 0 }">
            {{ formatWeightChange(report.weightChange) }}
          </span>
        </div>
      </div>

      <!-- Health reminders -->
      <div class="section" v-if="report.nextVaccine || report.nextDeworming || report.lastCheckup">
        <h2 class="section-title">
          <ThemedIcon name="bell" :size="15" />
          健康提醒
        </h2>
        <div class="reminder-list">
          <div class="reminder-item" v-if="report.nextVaccine">
            <ThemedIcon name="stethoscope" :size="13" />
            <span>下次疫苗</span>
            <span class="reminder-date">{{ report.nextVaccine }}</span>
          </div>
          <div class="reminder-item" v-if="report.nextDeworming">
            <ThemedIcon name="bell" :size="13" />
            <span>下次驱虫</span>
            <span class="reminder-date">{{ report.nextDeworming }}</span>
          </div>
          <div class="reminder-item" v-if="report.lastCheckup">
            <ThemedIcon name="heartPulse" :size="13" />
            <span>上次体检</span>
            <span class="reminder-date">{{ report.lastCheckup }}</span>
          </div>
        </div>
      </div>

      <!-- Milestones -->
      <div class="section" v-if="report.recentMilestones && report.recentMilestones.length > 0">
        <h2 class="section-title">
          <ThemedIcon name="flag" :size="15" />
          里程碑
        </h2>
        <div class="milestone-list">
          <div class="milestone-item" v-for="(m, i) in report.recentMilestones" :key="i">
            <ThemedIcon name="flag" :size="11" />
            <span>{{ m }}</span>
          </div>
        </div>
      </div>

      <!-- Insight Summary -->
      <div class="section ai-section" v-if="report.aiSummary">
        <h2 class="section-title">
          <ThemedIcon name="quill" :size="15" />
          观察总结
        </h2>
        <p class="ai-text">{{ report.aiSummary }}</p>
      </div>

      <!-- Suggestion -->
      <div class="section suggestion-section" v-if="report.nextWeekSuggestion">
        <h2 class="section-title">
          <ThemedIcon name="bookmark" :size="15" />
          下周建议
        </h2>
        <p class="suggestion-text">{{ report.nextWeekSuggestion }}</p>
      </div>

      <!-- Full report -->
      <div class="section" v-if="showFullReport && aiReport">
        <h2 class="section-title">
          <ThemedIcon name="scroll" :size="15" />
          完整报告
        </h2>
        <div class="full-report" v-text="aiReport"></div>
      </div>

      <!-- Actions -->
      <div class="report-actions">
        <button class="action-btn" @click="loadFullReport" v-if="!showFullReport">
          <ThemedIcon name="scroll" :size="14" />
          生成完整观察报告
        </button>
        <button class="action-btn" @click="downloadPdf" :disabled="pdfLoading">
          <ThemedIcon name="file" :size="14" />
          {{ pdfLoading ? '导出中...' : '导出PDF' }}
        </button>
        <button class="action-btn" @click="router.push(`/pet/${petId}/3d`)">
          <ThemedIcon name="cube" :size="14" />
          去3D房间互动
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.report-page {
  flex: 1;
  max-width: 760px;
  margin: 0 auto;
  width: 100%;
  padding: 1.5rem 2rem 3rem;
}

.report-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}
.back-btn {
  background: none;
  border: 1px solid var(--border-default);
  padding: 5px 12px;
  border-radius: var(--radius-sm);
  font-family: var(--font-body);
  font-size: 0.82rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast);
}
.back-btn:hover { border-color: var(--accent); color: var(--accent); }
.page-title {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.3rem;
  color: var(--text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.report-state {
  display: flex; align-items: center; gap: 10px;
  justify-content: center; padding: 6rem 0;
  color: var(--text-muted); font-size: 0.95rem;
}
.spinner {
  width: 18px; height: 18px;
  border: 2px solid var(--border-default);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.error-text { color: var(--danger); margin-bottom: 1rem; }
.retry-btn {
  padding: 6px 16px;
  border: 1px solid var(--border-default);
  background: transparent;
  font-family: var(--font-body); font-size: 0.85rem;
  color: var(--text-secondary);
  border-radius: var(--radius-sm); cursor: pointer;
}

.period-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--border-subtle);
}
.period-badge {
  font-size: 0.72rem;
  background: var(--accent);
  color: var(--text-inverse);
  padding: 3px 12px;
  border-radius: var(--radius-full);
  font-weight: 600;
}
.period-pet {
  font-size: 0.82rem;
  color: var(--text-muted);
}

/* Sections */
.section, .stats-section {
  margin-bottom: 1.5rem;
}
.section-title {
  font-family: var(--font-display);
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 0.75rem;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* Stats grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.stat-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 0.8rem;
  text-align: center;
}
.stat-num {
  display: block;
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--accent);
}
.stat-label {
  display: block;
  font-size: 0.72rem;
  color: var(--text-secondary);
  margin: 2px 0;
}
.stat-sub {
  display: block;
  font-size: 0.65rem;
  color: var(--text-muted);
}

/* Activity */
.activity-list {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.activity-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0.6rem 0.8rem;
  font-size: 0.82rem;
  color: var(--text-secondary);
}
.activity-item + .activity-item {
  border-top: 1px solid var(--border-subtle);
}
.activity-count {
  margin-left: auto;
  font-weight: 600;
  color: var(--accent);
}

/* Weight */
.weight-info {
  display: flex;
  align-items: baseline;
  gap: 10px;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 0.8rem 1rem;
}
.weight-current {
  font-family: var(--font-display);
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--text-primary);
}
.weight-change {
  font-size: 0.82rem;
  font-weight: 600;
}
.weight-change.up { color: var(--danger); }
.weight-change.down { color: var(--accent); }

/* Reminders */
.reminder-list {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.reminder-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0.55rem 0.8rem;
  font-size: 0.82rem;
  color: var(--text-secondary);
}
.reminder-item + .reminder-item {
  border-top: 1px solid var(--border-subtle);
}
.reminder-date {
  margin-left: auto;
  font-weight: 600;
  color: var(--spot);
}

/* Milestones */
.milestone-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.milestone-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0.4rem 0.6rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  font-size: 0.8rem;
  color: var(--text-secondary);
}

/* AI */
.ai-section {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 1rem 1.2rem;
}
.ai-section .section-title { margin-bottom: 0.5rem; }
.ai-text {
  font-size: 0.88rem;
  color: var(--text-primary);
  line-height: 1.7;
  margin: 0;
}

.suggestion-section {
  background: var(--accent-surface);
  border: 1px solid var(--accent-glow);
  border-radius: var(--radius-md);
  padding: 1rem 1.2rem;
}
.suggestion-section .section-title { margin-bottom: 0.5rem; }
.suggestion-text {
  font-size: 0.88rem;
  color: var(--text-primary);
  line-height: 1.7;
  margin: 0;
}

/* Full report */
.full-report {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 1.5rem;
  font-size: 0.9rem;
  line-height: 1.8;
  color: var(--text-primary);
  white-space: pre-wrap;
}

/* Actions */
.report-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--border-subtle);
}
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border: 1px solid var(--border-default);
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.85rem;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--duration-fast);
}
.action-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .report-page { padding: 1rem; }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .report-actions { flex-direction: column; }
  .action-btn { width: 100%; justify-content: center; }
}
</style>
