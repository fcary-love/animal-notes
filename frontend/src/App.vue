<script setup>
import AppHeader from './components/layout/AppHeader.vue'
import ChatPanel from './components/chat/ChatPanel.vue'
import Toast from './components/common/Toast.vue'
import { useRoute } from 'vue-router'
import { computed, ref } from 'vue'
import { useToast } from './composables/useToast'

const route = useRoute()
const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')
const showChat = computed(() => !isAuthPage.value && !route.meta.hideChat)
const toastEl = ref(null)
const { setRef } = useToast()

function onToastReady() {
  if (toastEl.value) setRef(toastEl.value)
}
</script>

<template>
  <div class="app-shell">
    <AppHeader v-if="!isAuthPage" />
    <main class="app-main" :class="{ 'is-auth': isAuthPage }">
      <router-view />
    </main>
    <ChatPanel v-if="showChat" />
    <Toast ref="toastEl" @vue:mounted="onToastReady" />
  </div>
</template>
