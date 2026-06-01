import http from './http'

export function chat(question, petId) {
  return http.post('/ai/chat', { question, petId })
}

export function searchMoments(query) {
  return http.post('/ai/search', { query })
}

export function milestoneSuggestions(petId) {
  return http.post(`/pets/${petId}/milestone-suggestions`)
}

export function generateReport(petId) {
  return http.post(`/pets/${petId}/report`)
}

export function downloadReportPdf(petId) {
  return http.post(`/pets/${petId}/report/pdf`)
}

export function analyzePhoto(file) {
  const fd = new FormData()
  fd.append('file', file)
  return http.post('/ai/vision', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}

