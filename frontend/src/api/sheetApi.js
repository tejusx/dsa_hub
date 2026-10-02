import { get } from './client.js'

export const getSheets = () => get('/api/sheets')
export const getSheet = (sheetId) => get(`/api/sheets/${encodeURIComponent(sheetId)}`)
export const getSheetProblems = (sheetId) =>
  get(`/api/sheets/${encodeURIComponent(sheetId)}/problems`)
export const getSheetProblemOccurrences = (sheetId) =>
  get(`/api/sheets/${encodeURIComponent(sheetId)}/problem-occurrences`)
