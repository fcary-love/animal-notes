import http from './http'

export function generateCard(petId) {
  return http.post(`/pets/${petId}/card`)
}
