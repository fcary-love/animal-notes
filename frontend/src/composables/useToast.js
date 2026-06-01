import { ref } from 'vue'

const toastRef = ref(null)

export function useToast() {
  function setRef(el) {
    toastRef.value = el
  }

  function showToast(msg, type = 'info', duration = 2500) {
    toastRef.value?.show(msg, type, duration)
  }

  return { setRef, showToast }
}
