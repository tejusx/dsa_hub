import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import * as progressApi from '../api/progressApi.js'
import { useAuth } from './AuthContext.jsx'
import {
  clearGuestProgress,
  getGuestProgress,
  saveGuestProgress,
  updateGuestProblemProgress,
  updateGuestSyncSetting,
} from '../utils/guestStorage.js'

const ProgressContext = createContext(null)

function emptyProgress(problemId) {
  return {
    problemId,
    solved: false,
    solvedAt: null,
    notes: null,
    isBookmarked: false,
    revisionStatus: null,
    userDifficulty: null,
    lastAttemptedAt: null,
  }
}

function getGuestEntry(problemId, sheetId, guestProgress) {
  if (guestProgress.syncProgressAcrossSheets) {
    return guestProgress.problemProgress[problemId] ?? emptyProgress(problemId)
  }
  return guestProgress.sheetProblemProgress[sheetId]?.[problemId] ?? emptyProgress(problemId)
}

export function ProgressProvider({ children }) {
  const { user, token } = useAuth()
  const [settings, setSettings] = useState(() => getGuestProgress())
  const [settingsLoading, setSettingsLoading] = useState(false)

  useEffect(() => {
    let cancelled = false
    if (!user || !token) {
      setSettings(getGuestProgress())
      setSettingsLoading(false)
      return undefined
    }

    setSettingsLoading(true)
    progressApi.getUserSettings()
      .then((currentSettings) => {
        if (!cancelled) setSettings(currentSettings)
      })
      .finally(() => {
        if (!cancelled) setSettingsLoading(false)
      })
      .catch(() => {
        if (!cancelled) setSettings(null)
      })

    return () => {
      cancelled = true
    }
  }, [user, token])

  const getProgress = useCallback(async (problemId, sheetId) => {
    if (user && token) {
      if (problemId && sheetId) return progressApi.getSheetProgress(sheetId, problemId)
      if (sheetId) return progressApi.getSheetProgress(sheetId)
      return progressApi.getProgress(problemId)
    }

    const guestProgress = getGuestProgress()
    if (problemId) return getGuestEntry(problemId, sheetId, guestProgress)
    if (sheetId && !guestProgress.syncProgressAcrossSheets) {
      return Object.values(guestProgress.sheetProblemProgress[sheetId] ?? {})
    }
    return Object.values(guestProgress.problemProgress)
  }, [user, token])

  const updateProgress = useCallback(async (problemId, updates, sheetId) => {
    if (!problemId) throw new Error('problemId is required')
    const current = await getProgress(problemId, sheetId)
    const solved = updates.solved ?? current.solved
    const next = {
      solved,
      solvedAt: solved ? (updates.solvedAt ?? current.solvedAt) : null,
      notes: updates.notes ?? current.notes,
      isBookmarked: updates.isBookmarked ?? current.isBookmarked,
      revisionStatus: updates.revisionStatus ?? current.revisionStatus,
      userDifficulty: updates.userDifficulty ?? current.userDifficulty,
      lastAttemptedAt: updates.lastAttemptedAt ?? current.lastAttemptedAt,
    }

    if (user && token) {
      return sheetId
        ? progressApi.updateSheetProgress(sheetId, problemId, next)
        : progressApi.updateProgress(problemId, next)
    }

    updateGuestProblemProgress(problemId, next, sheetId)
    return getGuestEntry(problemId, sheetId, getGuestProgress())
  }, [getProgress, user, token])

  const markProblemComplete = useCallback((problemId, sheetId) =>
    updateProgress(problemId, { solved: true, solvedAt: new Date().toISOString() }, sheetId),
  [updateProgress])

  const markProblemIncomplete = useCallback((problemId, sheetId) =>
    updateProgress(problemId, { solved: false }, sheetId),
  [updateProgress])

  const updateSyncSetting = useCallback(async (syncProgressAcrossSheets) => {
    if (user && token) {
      const updatedSettings = await progressApi.updateUserSettings({ syncProgressAcrossSheets })
      setSettings(updatedSettings)
      return updatedSettings
    }

    const guestProgress = updateGuestSyncSetting(syncProgressAcrossSheets)
    setSettings(guestProgress)
    return guestProgress
  }, [user, token])

  const migrateGuestProgress = useCallback(async () => {
    const guestProgress = getGuestProgress()
    const payload = {
      syncProgressAcrossSheets: guestProgress.syncProgressAcrossSheets,
      problemProgress: guestProgress.syncProgressAcrossSheets
        ? Object.values(guestProgress.problemProgress)
        : [],
      sheetProblemProgress: guestProgress.syncProgressAcrossSheets
        ? []
        : Object.values(guestProgress.sheetProblemProgress).flatMap((items) => Object.values(items)),
    }
    const response = await progressApi.migrateGuestProgress(payload)
    clearGuestProgress()
    return response
  }, [])

  const value = useMemo(() => ({
    settings,
    settingsLoading,
    getProgress,
    updateProgress,
    markProblemComplete,
    markProblemIncomplete,
    updateSyncSetting,
    migrateGuestProgress,
  }), [settings, settingsLoading, getProgress, updateProgress,
    markProblemComplete, markProblemIncomplete, updateSyncSetting, migrateGuestProgress])

  return <ProgressContext.Provider value={value}>{children}</ProgressContext.Provider>
}

export function useProgress() {
  const context = useContext(ProgressContext)
  if (!context) throw new Error('useProgress must be used inside ProgressProvider')
  return context
}
