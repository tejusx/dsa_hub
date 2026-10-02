export const TOKEN_STORAGE_KEY = 'dsa_hub_token'

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
  }
}

function isPublicGet(path, method) {
  if (method.toUpperCase() !== 'GET') return false

  const pathname = path.split('?')[0].replace(/\/$/, '')
  if (['/api/health', '/api/sheets', '/api/problems'].includes(pathname)) return true

  const segments = pathname.split('/')
  if (segments.length === 4 && segments[1] === 'api') {
    return segments[2] === 'sheets' || segments[2] === 'problems'
  }

  return segments.length === 5 && segments[1] === 'api' && (
    (segments[2] === 'sheets' && ['problems', 'problem-occurrences'].includes(segments[4])) ||
    (segments[2] === 'problems' && ['sheets', 'links', 'topics'].includes(segments[4]))
  )
}

/** Send a JSON request to the Vite-proxied backend API. */
export async function apiRequest(path, { method = 'GET', body, headers = {}, ...options } = {}) {
  if (!path.startsWith('/api/')) {
    throw new Error('API paths must begin with /api/')
  }

  const requestHeaders = new Headers(headers)
  if (body !== undefined && !requestHeaders.has('Content-Type')) {
    requestHeaders.set('Content-Type', 'application/json')
  }

  const token = localStorage.getItem(TOKEN_STORAGE_KEY)
  if (token && !isPublicGet(path, method) && !requestHeaders.has('Authorization')) {
    requestHeaders.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(path, {
    ...options,
    method,
    headers: requestHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  const responseText = await response.text()
  let data = null
  if (responseText) {
    try {
      data = JSON.parse(responseText)
    } catch {
      data = responseText
    }
  }

  if (!response.ok) {
    const message =
      data?.message || data?.error || (typeof data === 'string' ? data : '') ||
      `Request failed with status ${response.status}`
    throw new ApiError(message, response.status, data)
  }

  return data
}

export const get = (path, options) => apiRequest(path, { ...options, method: 'GET' })
export const post = (path, body, options) => apiRequest(path, { ...options, method: 'POST', body })
export const put = (path, body, options) => apiRequest(path, { ...options, method: 'PUT', body })
export const patch = (path, body, options) => apiRequest(path, { ...options, method: 'PATCH', body })
export const del = (path, options) => apiRequest(path, { ...options, method: 'DELETE' })
