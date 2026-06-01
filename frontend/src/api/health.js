import http from './http'

export function createHealthEvent(petId, data) {
  return http.post(`/pets/${petId}/health-events`, data)
}

export function getHealthEvents(petId, { eventType, page = 1, size = 20 } = {}) {
  const params = { page, size }
  if (eventType) params.eventType = eventType
  return http.get(`/pets/${petId}/health-events`, { params })
}

export function getHealthEvent(petId, id) {
  return http.get(`/pets/${petId}/health-events/${id}`)
}

export function updateHealthEvent(petId, id, data) {
  return http.put(`/pets/${petId}/health-events/${id}`, data)
}

export function deleteHealthEvent(petId, id) {
  return http.delete(`/pets/${petId}/health-events/${id}`)
}

export function getHealthSummary(petId) {
  return http.get(`/pets/${petId}/health-summary`)
}

// ===== 体重记录 =====

export function createWeight(petId, data) {
  return http.post(`/pets/${petId}/weights`, data)
}

export function getWeights(petId, limit = 10) {
  return http.get(`/pets/${petId}/weights`, { params: { limit } })
}

export function deleteWeight(petId, id) {
  return http.delete(`/pets/${petId}/weights/${id}`)
}

// ===== 饮食记录 =====

export function createDiet(petId, data) {
  return http.post(`/pets/${petId}/diets`, data)
}

export function getDiets(petId, limit = 10) {
  return http.get(`/pets/${petId}/diets`, { params: { limit } })
}

export function getDietsByDateRange(petId, startDate, endDate) {
  return http.get(`/pets/${petId}/diets/range`, { params: { startDate, endDate } })
}

export function updateDiet(petId, id, data) {
  return http.put(`/pets/${petId}/diets/${id}`, data)
}

export function deleteDiet(petId, id) {
  return http.delete(`/pets/${petId}/diets/${id}`)
}
