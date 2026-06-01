import http from './http'

// 知识文档（只读）
export function listKnowledge() {
  return http.get('/knowledge')
}

// 收藏
export function getFavoriteIds() {
  return http.get('/knowledge/favorites/ids')
}

export function listFavorites() {
  return http.get('/knowledge/favorites')
}

export function addFavorite(docId) {
  return http.post(`/knowledge/${docId}/favorite`)
}

export function removeFavorite(docId) {
  return http.delete(`/knowledge/${docId}/favorite`)
}

// 管理端（保留）
export function addKnowledge(title, content, category, source) {
  return http.post('/admin/knowledge', { title, content, category, source })
}

export function deleteKnowledge(id) {
  return http.delete(`/admin/knowledge/${id}`)
}

export function reindexKnowledge() {
  return http.post('/admin/knowledge/reindex')
}
