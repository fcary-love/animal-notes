<script setup>
import ThemedIcon from '../common/ThemedIcon.vue'

defineProps({
  role: { type: String, required: true },
  content: { type: String, required: true }
})
</script>

<template>
  <div class="chat-msg" :class="role">
    <div class="msg-avatar">
      <ThemedIcon v-if="role === 'assistant'" name="quill" :size="14" />
      <ThemedIcon v-else name="user" :size="14" />
    </div>
    <div class="msg-bubble">
      <p class="msg-text" v-text="content"></p>
    </div>
  </div>
</template>

<style scoped>
.chat-msg { display: flex; gap: 10px; margin-bottom: 1rem; }
.chat-msg.user { flex-direction: row-reverse; }
.msg-avatar {
  width: 32px; height: 32px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  background: #fffdf8;
  border: 1px solid var(--border-subtle);
  color: var(--accent);
}
.chat-msg.user .msg-avatar { color: var(--text-muted); }
.msg-bubble {
  max-width: 75%;
  padding: 11px 15px;
  border-radius: var(--radius-lg);
  font-size: 0.9rem;
  line-height: 1.7;
  box-shadow: 0 5px 14px rgba(61, 50, 38, 0.05);
}
.chat-msg.assistant .msg-bubble {
  background: rgba(255, 254, 249, 0.94);
  border: 1px solid var(--border-subtle);
  color: var(--text-primary);
}
.chat-msg.user .msg-bubble {
  background: rgba(92, 122, 94, 0.12);
  border: 1px solid rgba(92, 122, 94, 0.24);
  color: var(--text-primary);
}
.msg-text { margin: 0; white-space: pre-wrap; word-break: break-word; }
</style>
