import { ref } from 'vue'
import { useRouter } from 'vue-router'
import * as authApi from '../api/auth'
import { STORAGE_KEYS } from '../utils/constants'

const currentUser = ref(null)
const isLoggedIn = ref(false)

export function useAuth() {
  const router = useRouter()
  const loading = ref(false)
  const error = ref('')

  async function doRegister({ username, password, nickname }) {
    loading.value = true
    error.value = ''
    try {
      const res = await authApi.register({ username, password, nickname })
      setSession(res.data)
      router.push('/timeline')
    } catch (e) {
      error.value = e?.response?.data?.message || e.message || '注册失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function doLogin({ username, password }) {
    loading.value = true
    error.value = ''
    try {
      const res = await authApi.login({ username, password })
      setSession(res.data)
      router.push('/timeline')
    } catch (e) {
      error.value = e?.response?.data?.message || e.message || '登录失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchMe() {
    try {
      const res = await authApi.getMe()
      currentUser.value = res.data
      isLoggedIn.value = true
    } catch {
      logout()
    }
  }

  function syncLocal(user) {
    currentUser.value = { ...currentUser.value, ...user }
    localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(currentUser.value))
  }

  function setSession(user) {
    currentUser.value = user
    isLoggedIn.value = true
    localStorage.setItem(STORAGE_KEYS.TOKEN, user.token)
    localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user))
  }

  function logout() {
    currentUser.value = null
    isLoggedIn.value = false
    localStorage.removeItem(STORAGE_KEYS.TOKEN)
    localStorage.removeItem(STORAGE_KEYS.USER)
    router.push('/login')
  }

  function restoreFromStorage() {
    const token = localStorage.getItem(STORAGE_KEYS.TOKEN)
    const user = localStorage.getItem(STORAGE_KEYS.USER)
    if (token && user) {
      try {
        currentUser.value = JSON.parse(user)
        isLoggedIn.value = true
      } catch {
        logout()
      }
    }
  }

  return { currentUser, isLoggedIn, loading, error, doRegister, doLogin, fetchMe, logout, restoreFromStorage, syncLocal }
}
