import axios from 'axios'
import { Capacitor } from '@capacitor/core'
import { STORAGE_KEYS } from '../utils/constants'

// 检测运行环境，设置 API 基础地址
function getBaseURL() {
  if (Capacitor.isNativePlatform()) {
    // Android App: 用电脑的局域网 IP 访问后端
    return 'http://10.0.2.2:8081/api/v1'
  }
  // 浏览器: 相对路径（走 Vite proxy 或同源）
  return '/api/v1'
}

const http = axios.create({
  baseURL: getBaseURL(),
  timeout: 15000
})

http.interceptors.request.use(config => {
  const token = localStorage.getItem(STORAGE_KEYS.TOKEN)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  res => {
    const body = res.data
    // 后端统一格式 {code, message, data}，HTTP 200 但业务码非 200 时也应 reject
    if (body && typeof body.code === 'number' && body.code !== 200) {
      const err = new Error(body.message || '请求失败')
      err.response = { status: body.code, data: body }
      return Promise.reject(err)
    }
    return body
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem(STORAGE_KEYS.TOKEN)
      localStorage.removeItem(STORAGE_KEYS.USER)
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default http
