import http from './http'

export function createSession(petId) {
  return http.post('/ai/chat/sessions', { petId })
}

export function getSessions(petId) {
  return http.get('/ai/chat/sessions', { params: petId ? { petId } : {} })
}

export function getSessionMessages(sessionId) {
  return http.get(`/ai/chat/sessions/${sessionId}/messages`)
}

export function deleteSession(sessionId) {
  return http.delete(`/ai/chat/sessions/${sessionId}`)
}

export function updateSession(id, title) {
  return http.put(`/ai/chat/sessions/${id}`, { title })
}
