<script setup>
import { computed } from 'vue'
import ThemedIcon from '../common/ThemedIcon.vue'

const props = defineProps({
  groupedPhotos: { type: Array, required: true },
  pet: { type: Object, default: null }
})

const emit = defineEmits(['photo-click', 'tag-click'])

const speciesEmoji = { cat: '🐱', dog: '🐶', rabbit: '🐰', bird: '🐦', hamster: '🐹', other: '🐾' }

function formatMonthLabel(monthKey) {
  const [y, m] = monthKey.split('-')
  return `${y}年${m}月`
}

function formatDateHeader(dateStr) {
  if (!dateStr) return '未知日期'
  const d = new Date(dateStr)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getFullYear()}.${m}.${day} 星期${weekdays[d.getDay()]}`
}

function calcAge(birthday, dateStr) {
  if (!birthday || !dateStr) return ''
  const birth = new Date(birthday)
  const target = new Date(dateStr)
  if (isNaN(birth.getTime()) || isNaN(target.getTime())) return ''
  let years = target.getFullYear() - birth.getFullYear()
  let months = target.getMonth() - birth.getMonth()
  let days = target.getDate() - birth.getDate()
  if (days < 0) { months--; }
  if (months < 0) { years--; months += 12; }
  if (years < 0) return ''
  const totalMonths = years * 12 + months
  if (totalMonths === 0) return '< 1个月'
  if (years === 0) return `${months}个月`
  if (months === 0) return `${years}岁`
  return `${years}岁${months}个月`
}

function photoKey(photo, idx) {
  return photo.momentId ? `${photo.momentId}-${idx}` : `photo-${idx}`
}
</script>

<template>
  <div class="album-timeline">
    <div class="timeline-month" v-for="month in groupedPhotos" :key="month.monthKey">
      <div class="month-header">
        <div class="month-stamp">
          <span class="month-text">{{ formatMonthLabel(month.monthKey) }}</span>
        </div>
        <span class="month-count">{{ month.totalCount }}张</span>
      </div>

      <div class="timeline-track">
        <div class="timeline-line"></div>

        <div class="timeline-date" v-for="dateGroup in month.dates" :key="dateGroup.date">
          <div class="date-node">
            <div class="node-dot" :class="{ 'has-milestone': dateGroup.hasMilestone }">
              <ThemedIcon v-if="dateGroup.hasMilestone" name="flag" :size="10" />
              <ThemedIcon v-else name="paw" :size="9" />
            </div>
            <div class="date-label">
              <span class="date-text">{{ formatDateHeader(dateGroup.date) }}</span>
              <span class="date-age" v-if="calcAge(pet?.birthday, dateGroup.date)">
                {{ speciesEmoji[pet?.species] || '🐾' }} {{ calcAge(pet?.birthday, dateGroup.date) }}
              </span>
            </div>
          </div>

          <div class="date-cards">
            <div
              v-for="(photo, pIdx) in dateGroup.photos"
              :key="photoKey(photo, pIdx)"
              class="polaroid"
              :class="{ 'is-milestone': photo.isMilestone }"
              @click="emit('photo-click', photo)"
            >
              <div class="polaroid-frame">
                <div class="img-placeholder" v-if="!photo._loaded">
                  <ThemedIcon name="image" :size="20" />
                </div>
                <img
                  :src="photo.photoUrl"
                  :alt="photo.content || '照片'"
                  loading="lazy"
                  @load="photo._loaded = true"
                  @error="photo._error = true"
                  :style="{ opacity: photo._loaded ? 1 : 0 }"
                />
                <div class="milestone-flag" v-if="photo.isMilestone">
                  <ThemedIcon name="flag" :size="10" />
                </div>
              </div>
              <div class="polaroid-caption">
                <div class="caption-tags">
                  <span class="milestone-tag" v-if="photo.milestoneLabel">
                    <ThemedIcon name="bookmark" :size="9" />
                    {{ photo.milestoneLabel }}
                  </span>
                  <span class="location-tag" v-if="photo.location">
                    {{ photo.location }}
                  </span>
                </div>
                <p class="caption-text" v-if="photo.content">{{ photo.content }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.album-timeline {
  position: relative;
}

/* --- Month Section --- */
.timeline-month {
  margin-bottom: 2.5rem;
}

.month-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 1.2rem;
  padding-left: 24px;
}

.month-stamp {
  display: inline-flex;
  align-items: center;
  padding: 3px 14px;
  background: var(--bg-aged);
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-sm);
}

.month-text {
  font-family: var(--font-display);
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.06em;
}

.month-count {
  font-size: 0.72rem;
  color: var(--text-muted);
}

/* --- Timeline Track --- */
.timeline-track {
  position: relative;
  padding-left: 56px;
}

.timeline-line {
  position: absolute;
  left: 18px;
  top: 0;
  bottom: 0;
  width: 1.5px;
  background: var(--border-default);
  opacity: 0.6;
}

/* --- Date Node --- */
.timeline-date {
  position: relative;
  margin-bottom: 1.8rem;
}

.date-node {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 0.8rem;
  position: relative;
}

.node-dot {
  position: absolute;
  left: -47px;
  width: 24px;
  height: 24px;
  border-radius: var(--radius-full);
  background: var(--bg-warm);
  border: 1.5px solid var(--border-default);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  z-index: 2;
  transition: all var(--duration-fast);
}

.node-dot.has-milestone {
  background: var(--spot-glow);
  border-color: var(--spot);
  color: var(--spot);
}

.date-label {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.date-text {
  font-family: var(--font-display);
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.02em;
}

.date-age {
  font-size: 0.72rem;
  color: var(--text-muted);
}

/* --- Photo Cards (Polaroid) --- */
.date-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.polaroid {
  width: 200px;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: 3px;
  padding: 8px 8px 0 8px;
  cursor: pointer;
  box-shadow: var(--shadow-card);
  transition: all var(--duration-normal) var(--ease-out);
  position: relative;
}

.polaroid:hover {
  transform: translateY(-3px) rotate(-0.5deg);
  box-shadow: var(--shadow-elevated);
}

.polaroid.is-milestone {
  border-color: var(--spot-glow);
}

.polaroid-frame {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border-radius: 2px;
  background: var(--bg-warm);
  margin-bottom: 6px;
}

.polaroid-frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: opacity 0.3s ease;
}

.img-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  opacity: 0.4;
}

.milestone-flag {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 22px;
  height: 22px;
  border-radius: var(--radius-full);
  background: var(--spot);
  color: var(--text-inverse);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 4px rgba(0,0,0,0.15);
}

.polaroid-caption {
  padding: 4px 2px 8px 2px;
  min-height: 32px;
}

.caption-tags {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
  margin-bottom: 2px;
}

.milestone-tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 6px;
  border-radius: var(--radius-full);
  background: var(--spot-glow);
  color: var(--spot);
  font-size: 0.65rem;
  font-weight: 600;
}

.location-tag {
  font-size: 0.65rem;
  color: var(--text-muted);
  padding: 1px 6px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
}

.caption-text {
  font-size: 0.78rem;
  color: var(--text-secondary);
  line-height: 1.55;
  margin: 2px 0 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* --- Responsive --- */
@media (max-width: 640px) {
  .timeline-track {
    padding-left: 36px;
  }

  .timeline-line {
    left: 10px;
  }

  .node-dot {
    left: -33px;
    width: 20px;
    height: 20px;
  }

  .polaroid {
    width: calc(50% - 7px);
    min-width: 0;
  }

  .month-header {
    padding-left: 12px;
  }
}

@media (max-width: 400px) {
  .polaroid {
    width: 100%;
  }
}
</style>
