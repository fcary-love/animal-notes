<script setup>
import { ref, onUnmounted } from 'vue'

const visible = ref(false)
const message = ref('')
const type = ref('info') // info | success | error
let timer = null

function show(msg, t = 'info', duration = 2500) {
  message.value = msg
  type.value = t
  visible.value = true
  clearTimeout(timer)
  timer = setTimeout(() => { visible.value = false }, duration)
}

function hide() {
  visible.value = false
  clearTimeout(timer)
}

onUnmounted(() => clearTimeout(timer))

defineExpose({ show, hide })
</script>

<template>
  <Teleport to="body">
    <Transition name="toast">
      <div v-if="visible" class="toast" :class="'toast--' + type" @click="hide">
        <span class="toast-dot"></span>
        <span class="toast-text">{{ message }}</span>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.toast {
  position: fixed;
  top: 72px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--text-primary);
  color: var(--text-inverse);
  font-family: var(--font-body);
  font-size: 0.82rem;
  padding: 8px 20px 8px 14px;
  border-radius: var(--radius-full);
  box-shadow: var(--shadow-elevated);
  z-index: 9999;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  pointer-events: auto;
  white-space: nowrap;
  max-width: 90vw;
}

.toast-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.toast--info .toast-dot { background: var(--accent); }
.toast--success .toast-dot { background: #66bb6a; }
.toast--error .toast-dot { background: var(--spot); }

.toast-text {
  line-height: 1.4;
}

.toast-enter-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.toast-leave-active {
  transition: opacity 0.35s ease, transform 0.35s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-6px);
}
</style>
