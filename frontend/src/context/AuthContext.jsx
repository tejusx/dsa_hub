import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import * as authApi from '../api/authApi.js'
import { ApiError, TOKEN_STORAGE_KEY } from '../api/client.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_STORAGE_KEY))
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    if (!token) {
      setUser(null)
      setLoading(false)
      return undefined
    }

    setLoading(true)
    authApi.getCurrentUser()
      .then((currentUser) => {
        if (!cancelled) setUser(currentUser)
      })
      .catch((error) => {
        if (cancelled) return
        setUser(null)
        if (error instanceof ApiError && error.status === 401) {
          localStorage.removeItem(TOKEN_STORAGE_KEY)
          setToken(null)
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [token])

  const login = useCallback(async (credentials) => {
    const response = await authApi.login(credentials)
    localStorage.setItem(TOKEN_STORAGE_KEY, response.accessToken)
    setToken(response.accessToken)
    const currentUser = await authApi.getCurrentUser()
    setUser(currentUser)
    return currentUser
  }, [])

  const register = useCallback(async (details) => {
    await authApi.register(details)
    // Registration returns the new user; login supplies the JWT used by protected APIs.
    return login({ email: details.email, password: details.password })
  }, [login])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    setToken(null)
    setUser(null)
  }, [])

  const value = useMemo(() => ({
    user,
    token,
    loading,
    login,
    register,
    logout,
    isAuthenticated: Boolean(user && token),
  }), [user, token, loading, login, register, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
