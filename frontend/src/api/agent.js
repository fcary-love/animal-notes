import http from './http'

export function getAgentTasks(petId) {
  return http.get(`/pets/${petId}/agent/tasks`)
}

export function completeAgentTask(petId, taskId) {
  return http.post(`/pets/${petId}/agent/tasks/${taskId}/complete`)
}

export function getEnhancedReport(petId) {
  return http.get(`/pets/${petId}/report/enhanced`)
}
