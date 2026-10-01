import { get, post } from './client.js'

export const register = (request) => post('/api/auth/register', request)
export const login = (request) => post('/api/auth/login', request)
export const getCurrentUser = () => get('/api/auth/me')
