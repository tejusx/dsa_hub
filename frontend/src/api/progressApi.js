import { get, patch, post, put } from './client.js'

export const getProgress = (problemId) =>
  get(problemId ? `/api/progress/${encodeURIComponent(problemId)}` : '/api/progress')

export const updateProgress = (problemId, progress) =>
  put(`/api/progress/${encodeURIComponent(problemId)}`, progress)

export const getSheetProgress = (sheetId, problemId) => {
  const path = `/api/sheets/${encodeURIComponent(sheetId)}/progress`
  return get(problemId ? `${path}/${encodeURIComponent(problemId)}` : path)
}

export const updateSheetProgress = (sheetId, problemId, progress) =>
  put(
    `/api/sheets/${encodeURIComponent(sheetId)}/progress/${encodeURIComponent(problemId)}`,
    progress,
  )

export const getUserSettings = () => get('/api/settings')
export const updateUserSettings = (settings) => patch('/api/settings', settings)

export const createAttempt = (problemId, attempt) =>
  post(`/api/problems/${encodeURIComponent(problemId)}/attempts`, attempt)

export const migrateGuestProgress = (progress) => post('/api/progress/migrate-guest', progress)
