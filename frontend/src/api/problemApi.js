import { get } from './client.js'

export const getProblems = () => get('/api/problems')
export const getProblem = (problemId) => get(`/api/problems/${encodeURIComponent(problemId)}`)
export const getProblemSheets = (problemId) =>
  get(`/api/problems/${encodeURIComponent(problemId)}/sheets`)
