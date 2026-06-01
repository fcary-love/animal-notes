import http from './http'

export function createPet(data) {
  return http.post('/pets', data)
}

export function getPetList() {
  return http.get('/pets')
}

export function getPet(id) {
  return http.get(`/pets/${id}`)
}

export function updatePet(id, data) {
  return http.put(`/pets/${id}`, data)
}

export function deletePet(id) {
  return http.delete(`/pets/${id}`)
}

export function uploadPetAvatar(id, file) {
  const form = new FormData()
  form.append('file', file)
  return http.post(`/pets/${id}/avatar`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function updatePetModelConfig(id, modelConfig) {
  return http.put(`/pets/${id}/model-config`, { modelConfig })
}

export function uploadFile(file) {
  const form = new FormData()
  form.append('file', file)
  return http.post('/files/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
