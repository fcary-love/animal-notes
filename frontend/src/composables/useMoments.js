import { ref } from 'vue'
import * as momentApi from '../api/moment'

export function useMoments() {
  const records = ref([])
  const total = ref(0)
  const loading = ref(false)
  const error = ref('')
  const sort = ref('desc')
  const milestoneOnly = ref(false)

  async function fetchTimeline(petId, page = 1) {
    loading.value = true
    error.value = ''
    try {
      if (milestoneOnly.value) {
        const res = await momentApi.getMilestones(petId)
        records.value = res.data
        total.value = res.data.length
      } else {
        const res = await momentApi.getTimeline(petId, { sort: sort.value, page, size: 20 })
        records.value = res.data.records
        total.value = res.data.total
      }
    } catch (e) {
      error.value = e?.response?.data?.message || e.message || '加载失败'
    } finally {
      loading.value = false
    }
  }

  async function create(petId, data) {
    loading.value = true
    error.value = ''
    try {
      const res = await momentApi.createMoment(petId, data)
      records.value.unshift(res.data)
      total.value++
      return res.data
    } catch (e) {
      error.value = e?.response?.data?.message || e.message || '创建失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function update(id, data) {
    loading.value = true
    error.value = ''
    try {
      const res = await momentApi.updateMoment(id, data)
      const idx = records.value.findIndex(m => m.id === id)
      if (idx >= 0) records.value[idx] = res.data
      return res.data
    } catch (e) {
      error.value = e?.response?.data?.message || e.message || '更新失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function remove(id) {
    loading.value = true
    error.value = ''
    try {
      await momentApi.deleteMoment(id)
      records.value = records.value.filter(m => m.id !== id)
      total.value--
    } catch (e) {
      error.value = e?.response?.data?.message || e.message || '删除失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  function toggleSort() {
    sort.value = sort.value === 'desc' ? 'asc' : 'desc'
  }

  function toggleMilestone() {
    milestoneOnly.value = !milestoneOnly.value
  }

  return { records, total, loading, error, sort, milestoneOnly, fetchTimeline, create, update, remove, toggleSort, toggleMilestone }
}
