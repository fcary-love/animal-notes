import { ref } from 'vue'
import * as petApi from '../api/pet'

const pets = ref([])
const currentPet = ref(null)
const loading = ref(false)
const error = ref('')

export function usePets() {
  async function fetchPets() {
    loading.value = true
    error.value = ''
    try {
      const res = await petApi.getPetList()
      pets.value = res.data
      if (!currentPet.value && pets.value.length > 0) {
        currentPet.value = pets.value[0]
      }
    } catch (e) {
      error.value = e?.response?.data?.message || e.message || '加载失败'
    } finally {
      loading.value = false
    }
  }

  async function create(data) {
    loading.value = true
    error.value = ''
    try {
      const res = await petApi.createPet(data)
      pets.value.unshift(res.data)
      currentPet.value = res.data
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
      const res = await petApi.updatePet(id, data)
      const idx = pets.value.findIndex(p => p.id === id)
      if (idx >= 0) pets.value[idx] = res.data
      if (currentPet.value?.id === id) currentPet.value = res.data
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
      await petApi.deletePet(id)
      pets.value = pets.value.filter(p => p.id !== id)
      if (currentPet.value?.id === id) {
        currentPet.value = pets.value.length > 0 ? pets.value[0] : null
      }
    } catch (e) {
      error.value = e?.response?.data?.message || e.message || '删除失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function uploadAvatar(id, file) {
    try {
      const res = await petApi.uploadPetAvatar(id, file)
      const idx = pets.value.findIndex(p => p.id === id)
      if (idx >= 0) pets.value[idx] = res.data
      if (currentPet.value?.id === id) currentPet.value = res.data
      return res.data
    } catch (e) {
      throw e
    }
  }

  async function getPet(id) {
    try {
      const res = await petApi.getPet(id)
      return res.data
    } catch (e) {
      throw e
    }
  }

  function selectPet(pet) {
    currentPet.value = pet
  }

  return { pets, currentPet, loading, error, fetchPets, create, update, remove, getPet, uploadAvatar, selectPet }
}
