<script setup>
import { watch } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '' },
  width: { type: String, default: '420px' }
})
const emit = defineEmits(['close'])

watch(() => props.visible, (v) => {
  document.body.style.overflow = v ? 'hidden' : ''
})
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer">
      <div v-if="visible" class="drawer-overlay" @click.self="emit('close')">
        <div class="drawer-panel" :style="{ maxWidth: width }">
          <div class="drawer-header">
            <h2 class="drawer-title">{{ title }}</h2>
            <button class="drawer-close" @click="emit('close')" aria-label="关闭">
              <svg width="18" height="18" viewBox="0 0 20 20" fill="none">
                <path d="M5 5l10 10M15 5L5 15" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
          <div class="drawer-body">
            <slot />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(30, 25, 20, 0.25);
  backdrop-filter: blur(2px);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
}
.drawer-panel {
  width: 100%;
  height: 100%;
  background: var(--bg-surface);
  box-shadow: -8px 0 40px rgba(61, 50, 38, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border-subtle);
  flex-shrink: 0;
}
.drawer-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.03em;
}
.drawer-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--text-muted);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-fast);
}
.drawer-close:hover {
  background: var(--accent-surface);
  color: var(--text-primary);
}
.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px 32px;
}

/* Transition — spring-like slide from right */
.drawer-enter-active {
  transition: opacity 0.3s ease;
}
.drawer-enter-active .drawer-panel {
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.drawer-leave-active {
  transition: opacity 0.25s ease;
}
.drawer-leave-active .drawer-panel {
  transition: transform 0.3s cubic-bezier(0.5, 0, 0.75, 0);
}
.drawer-enter-from {
  opacity: 0;
}
.drawer-enter-from .drawer-panel {
  transform: translateX(100%);
}
.drawer-leave-to {
  opacity: 0;
}
.drawer-leave-to .drawer-panel {
  transform: translateX(60%);
}
</style>
