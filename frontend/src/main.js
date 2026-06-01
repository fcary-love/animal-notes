import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import { STORAGE_KEYS } from './utils/constants'
import './styles/base.css'
import './styles/layout.css'

import LoginView from './views/LoginView.vue'
import RegisterView from './views/RegisterView.vue'
import TimelineView from './views/TimelineView.vue'
import PetCreateView from './views/PetCreateView.vue'
import PetDetailView from './views/PetDetailView.vue'
import CardPreviewView from './views/CardPreviewView.vue'
import AiChatView from './views/AiChatView.vue'
import KnowledgeManage from './views/KnowledgeManage.vue'
import PetReportView from './views/PetReportView.vue'
import Pet3DView from './views/Pet3DView.vue'
import DashboardView from './views/DashboardView.vue'
import PetHealthView from './views/PetHealthView.vue'
import HealthOverviewView from './views/HealthOverviewView.vue'
import AlbumOverviewView from './views/AlbumOverviewView.vue'
import PetAlbumView from './views/PetAlbumView.vue'
import PetDietView from './views/PetDietView.vue'
import PetGameView from './views/PetGameView.vue'
import ProfileView from './views/ProfileView.vue'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', name: 'Login', component: LoginView },
  { path: '/register', name: 'Register', component: RegisterView },
  { path: '/timeline', name: 'Timeline', component: TimelineView, meta: { requiresAuth: true } },
  { path: '/pet/create', name: 'PetCreate', component: PetCreateView, meta: { requiresAuth: true } },
  { path: '/pet/:id', name: 'PetDetail', component: PetDetailView, meta: { requiresAuth: true } },
  { path: '/card/:petId', name: 'CardPreview', component: CardPreviewView, meta: { requiresAuth: true } },
  { path: '/ai/chat', name: 'AiChat', component: AiChatView, meta: { requiresAuth: true } },
  { path: '/admin/knowledge', name: 'KnowledgeManage', component: KnowledgeManage, meta: { requiresAuth: true } },
  { path: '/pet/:id/report', name: 'PetReport', component: PetReportView, meta: { requiresAuth: true } },
  { path: '/pet/:id/3d', name: 'Pet3D', component: Pet3DView, meta: { requiresAuth: true } },
  { path: '/pet/:id/game', name: 'PetGame', component: PetGameView, meta: { requiresAuth: true, hideChat: true } },
  { path: '/dashboard', name: 'Dashboard', component: DashboardView, meta: { requiresAuth: true } },
  { path: '/health', name: 'HealthOverview', component: HealthOverviewView, meta: { requiresAuth: true } },
  { path: '/pet/:id/health', name: 'PetHealth', component: PetHealthView, meta: { requiresAuth: true } },
  { path: '/album', name: 'AlbumOverview', component: AlbumOverviewView, meta: { requiresAuth: true } },
  { path: '/pet/:id/album', name: 'PetAlbum', component: PetAlbumView, meta: { requiresAuth: true } },
  { path: '/pet/:id/diet', name: 'PetDiet', component: PetDietView, meta: { requiresAuth: true } },
  { path: '/profile', name: 'Profile', component: ProfileView, meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem(STORAGE_KEYS.TOKEN)
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    next('/timeline')
  } else {
    next()
  }
})

const app = createApp(App)
app.use(router)
app.mount('#app')
