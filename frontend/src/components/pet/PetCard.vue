<script setup>
import { computed } from 'vue'

const props = defineProps({
  pet: { type: Object, required: true },
  compact: { type: Boolean, default: false }
})

const emit = defineEmits(['select', 'edit', 'delete'])

const ageText = computed(() => {
  if (!props.pet.birthday) return ''
  const bd = new Date(props.pet.birthday)
  const now = new Date()
  const years = now.getFullYear() - bd.getFullYear()
  const months = now.getMonth() - bd.getMonth()
  if (years > 0) return `${years}岁${months > 0 ? months + '个月' : ''}`
  if (months > 0) return `${months}个月大`
  const days = Math.floor((now - bd) / (1000 * 60 * 60 * 24))
  return `${days}天`
})

const initial = computed(() => props.pet.name?.charAt(0) || '?')
</script>

<template>
  <div class="pet-card"
    :class="{ 'is-compact': compact }"
    @click="emit('select', pet)">
    <div class="pet-avatar" v-if="!compact || !pet.avatarUrl">
      <img v-if="pet.avatarUrl" :src="pet.avatarUrl" :alt="pet.name" />
      <span v-else class="avatar-placeholder">{{ initial }}</span>
    </div>
    <div class="pet-info">
      <div class="pet-name-row">
        <span class="pet-name">{{ pet.name }}</span>
        <span class="pet-species" v-if="pet.species">{{ pet.species }}</span>
      </div>
      <div class="pet-meta" v-if="pet.breed || ageText">
        <span v-if="pet.breed">{{ pet.breed }}</span>
        <span v-if="pet.breed && ageText" class="dot">·</span>
        <span v-if="ageText">{{ ageText }}</span>
      </div>
      <p class="pet-bio" v-if="pet.bio && !compact">{{ pet.bio }}</p>
    </div>
    <div class="pet-actions" v-if="!compact">
      <button class="action-btn" @click.stop="emit('edit', pet)" title="编辑">&#9998;</button>
      <button class="action-btn action-danger" @click.stop="emit('delete', pet)" title="删除">&#10005;</button>
    </div>
  </div>
</template>

<style scoped>
.pet-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: rgba(255, 254, 249, 0.86);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}
.pet-card:hover { border-color: var(--accent-glow); box-shadow: var(--shadow-card); transform: translateY(-1px); }
.pet-card.is-compact { padding: 8px 12px; }

.pet-avatar {
  width: 52px; height: 52px;
  border-radius: 38% 62% 52% 48%;
  overflow: hidden;
  flex-shrink: 0;
  border: 2px solid var(--border-subtle);
  background: var(--bg-aged);
  display: flex; align-items: center; justify-content: center;
}
.is-compact .pet-avatar { width: 36px; height: 36px; }
.pet-avatar img { width: 100%; height: 100%; object-fit: cover; }
.avatar-placeholder {
  font-family: var(--font-display);
  font-size: 1.25rem;
  color: var(--accent);
  font-weight: 700;
}
.is-compact .avatar-placeholder { font-size: 0.9rem; }

.pet-info { flex: 1; min-width: 0; }
.pet-name-row { display: flex; align-items: center; gap: 8px; }
.pet-name {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.05rem;
  color: var(--text-primary);
}
.pet-species {
  font-size: 0.75rem;
  color: var(--accent);
  background: var(--accent-surface);
  padding: 1px 8px;
  border-radius: var(--radius-full);
}
.pet-meta { font-size: 0.8rem; color: var(--text-muted); margin-top: 2px; }
.dot { margin: 0 4px; }
.pet-bio {
  font-size: 0.85rem; color: var(--text-secondary);
  margin-top: 4px; line-height: 1.5;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.pet-actions { display: flex; gap: 4px; flex-shrink: 0; }
.action-btn {
  width: 32px; height: 32px;
  border: 1px solid var(--border-subtle);
  background: transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 0.9rem;
  display: flex; align-items: center; justify-content: center;
  transition: all var(--duration-fast) var(--ease-out);
}
.action-btn:hover { border-color: var(--accent); color: var(--accent); }
.action-danger:hover { border-color: var(--danger); color: var(--danger); }
</style>
