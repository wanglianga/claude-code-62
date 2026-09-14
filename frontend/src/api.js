const BASE = '/api'

export function getToken() {
  return localStorage.getItem('funeral_token') || ''
}
export function setSession(token, user) {
  localStorage.setItem('funeral_token', token)
  localStorage.setItem('funeral_user', JSON.stringify(user))
}
export function getUser() {
  try { return JSON.parse(localStorage.getItem('funeral_user') || 'null') } catch { return null }
}
export function clearSession() {
  localStorage.removeItem('funeral_token')
  localStorage.removeItem('funeral_user')
}

async function request(method, url, body) {
  const opt = {
    method,
    headers: { 'Content-Type': 'application/json', 'X-Auth-Token': getToken() }
  }
  if (body !== undefined) opt.body = JSON.stringify(body)
  const res = await fetch(BASE + url, opt)
  let data = null
  try { data = await res.json() } catch { /* ignore */ }
  if (!res.ok || (data && data.code !== 0)) {
    const msg = data?.message || ('请求失败 (' + res.status + ')')
    const err = new Error(msg)
    err.status = res.status
    throw err
  }
  return data?.data
}

export const api = {
  get: (u) => request('GET', u),
  post: (u, b) => request('POST', u, b ?? {}),
  put: (u, b) => request('PUT', u, b ?? {}),
  del: (u) => request('DELETE', u)
}
