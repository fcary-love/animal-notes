import http from './http'

export async function uploadPhoto(file) {
  const formData = new FormData()
  formData.append('file', file)
  const res = await http.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000
  })
  return res.data.url
}

export function createMoment(petId, data) {
  return http.post(`/pets/${petId}/moments`, data)
}

export function getTimeline(petId, { sort = 'desc', page = 1, size = 20 } = {}) {
  return http.get(`/pets/${petId}/moments`, { params: { sort, page, size } })
}

export function getMilestones(petId) {
  return http.get(`/pets/${petId}/moments/milestones`)
}

export function getMoment(id) {
  return http.get(`/moments/${id}`)
}

export function updateMoment(id, data) {
  return http.put(`/moments/${id}`, data)
}

export function deleteMoment(id) {
  return http.delete(`/moments/${id}`)
}

export function getPetPhotos(petId, { milestoneOnly = false, page = 1, size = 30 } = {}) {
  return http.get(`/pets/${petId}/photos`, { params: { milestoneOnly, page, size } })
}
