<script setup>
const emit = defineEmits(['create'])
</script>

<template>
  <div class="tl-empty">
    <div class="empty-graphic">&#9783;</div>
    <h3 class="empty-title">时间线还是空的</h3>
    <p class="empty-desc">记录下宠物的第一个重要时刻吧——不管是第一次到家、第一次打疫苗，还是今天睡了个超萌的姿势。</p>
    <button class="empty-action" @click="emit('create')">记录第一个时刻</button>
  </div>
</template>

<style scoped>
.tl-empty {
  text-align: center; padding: 3rem 2rem;
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-md);
}
.empty-graphic { font-size: 3rem; color: var(--accent); opacity: 0.3; margin-bottom: 0.75rem; }
.empty-title {
  font-family: var(--font-display); font-weight: 700; font-size: 1.15rem;
  color: var(--text-primary); margin-bottom: 0.5rem;
}
.empty-desc {
  font-size: 0.9rem; color: var(--text-muted); max-width: 360px;
  margin: 0 auto 1.25rem; line-height: 1.7;
}
.empty-action {
  padding: 8px 20px; border: 1px solid var(--accent); background: transparent;
  color: var(--accent); font-family: var(--font-body); font-size: 0.9rem;
  border-radius: var(--radius-sm); cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}
.empty-action:hover { background: var(--accent); color: var(--text-inverse); }
</style>
