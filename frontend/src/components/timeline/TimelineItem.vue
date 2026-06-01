<script setup>
defineProps({
  moment: { type: Object, required: true }
})
const emit = defineEmits(['edit', 'delete'])

function formatDate(dt) {
  const d = new Date(dt)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}.${m}.${day}  ${h}:${min}`
}
</script>

<template>
  <div class="timeline-item" :class="{ 'is-milestone': moment.isMilestone }">
    <!-- 时间线左侧：时间点 + 线 -->
    <div class="tl-left">
      <div class="tl-dot" :class="{ 'is-milestone': moment.isMilestone }"></div>
      <div class="tl-line"></div>
    </div>

    <!-- 内容区 -->
    <div class="tl-content">
      <div class="tl-header">
        <time class="tl-time">{{ formatDate(moment.occurredAt) }}</time>
        <span class="tl-milestone-badge" v-if="moment.isMilestone && moment.milestoneLabel">
          {{ moment.milestoneLabel }}
        </span>
        <span class="tl-location" v-if="moment.location">&#9906; {{ moment.location }}</span>
      </div>

      <p class="tl-text" v-if="moment.content">{{ moment.content }}</p>

      <div class="tl-photos" v-if="moment.photos && moment.photos.length > 0">
        <img
          v-for="(photo, i) in moment.photos" :key="i"
          :src="photo" :alt="'照片' + (i + 1)"
          class="tl-photo"
        />
      </div>

      <div class="tl-actions">
        <button class="tl-action-btn" @click="emit('edit', moment)">编辑</button>
        <button class="tl-action-btn tl-action-danger" @click="emit('delete', moment)">删除</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.timeline-item {
  display: flex;
  gap: 0;
  position: relative;
}

/* 左侧时间轴 */
.tl-left {
  width: 32px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.tl-dot {
  width: 12px; height: 12px;
  border-radius: var(--radius-full);
  background: #fffdf8;
  border: 2px solid var(--accent);
  margin-top: 18px;
  flex-shrink: 0;
  transition: all var(--duration-fast) var(--ease-out);
}
.tl-dot.is-milestone {
  width: 16px; height: 16px;
  background: var(--spot);
  border-color: #fffdf8;
  box-shadow: 0 0 0 3px var(--spot-glow);
}
.tl-line {
  flex: 1;
  width: 1px;
  background: var(--border-subtle);
  margin-top: 4px;
  min-height: 20px;
}
.timeline-item:last-child .tl-line { display: none; }

/* 内容 */
.tl-content {
  flex: 1;
  padding: 0.9rem 1rem 1rem;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(180deg, rgba(255, 254, 249, 0.96), rgba(250, 247, 240, 0.88));
  box-shadow: 0 6px 16px rgba(61, 50, 38, 0.05);
  transform: rotate(-0.2deg);
  transition: transform var(--duration-fast), box-shadow var(--duration-fast), border-color var(--duration-fast);
}
.is-milestone .tl-content {
  background:
    linear-gradient(135deg, rgba(232, 180, 160, 0.18), rgba(255, 254, 249, 0.94));
  border-color: rgba(193, 123, 96, 0.28);
}
.timeline-item:hover .tl-content {
  transform: translateY(-2px) rotate(0deg);
  border-color: var(--accent-glow);
  box-shadow: 0 10px 22px rgba(61, 50, 38, 0.09);
}
.tl-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}
.tl-time {
  font-family: var(--font-mono);
  font-size: 0.82rem;
  color: var(--text-muted);
  letter-spacing: -0.02em;
}
.tl-milestone-badge {
  font-size: 0.7rem;
  background: rgba(193, 123, 96, 0.12);
  color: var(--spot);
  border: 1px solid rgba(193, 123, 96, 0.22);
  padding: 1px 8px;
  border-radius: var(--radius-full);
  font-weight: 600;
  letter-spacing: 0.04em;
}
.tl-location {
  font-size: 0.78rem;
  color: var(--text-muted);
}
.tl-text {
  font-size: 0.95rem;
  color: var(--text-primary);
  line-height: 1.8;
  margin-bottom: 10px;
}
.tl-photos {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.tl-photo {
  width: 132px; height: 132px;
  object-fit: cover;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-subtle);
  padding: 4px;
  background: #fffdf8;
}
.tl-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity var(--duration-fast);
}
.timeline-item:hover .tl-actions { opacity: 1; }
.tl-action-btn {
  background: none;
  border: none;
  font-family: var(--font-body);
  font-size: 0.8rem;
  color: var(--text-muted);
  cursor: pointer;
  padding: 0;
}
.tl-action-btn:hover { color: var(--accent); }
.tl-action-danger:hover { color: var(--danger); }
</style>
