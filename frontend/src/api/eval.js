import http from './http'

export function runEval() {
  return http.post('/eval/run')
}
