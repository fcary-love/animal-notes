import http from './http'

export function updateProfile(data) {
  return http.put('/users/profile', data)
}

export function changePassword(data) {
  return http.put('/users/password', data)
}

export function uploadAvatar(file) {
  const fd = new FormData()
  fd.append('file', file)
  return http.post('/users/avatar', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
