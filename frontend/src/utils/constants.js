import { Capacitor } from '@capacitor/core'

export const STORAGE_KEYS = {
  TOKEN: 'token',
  USER: 'user'
}

export function getApiBase() {
  if (Capacitor.isNativePlatform()) {
    return 'http://10.0.2.2:8081'
  }
  return ''
}
