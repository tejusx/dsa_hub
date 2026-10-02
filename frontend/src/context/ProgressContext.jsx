import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import * as progressApi from '../api/progressApi.js'
import { useAuth } from './AuthContext.jsx'
import {
  clearGuestProgress,
  getGuestProgress,
  updateGuestProblemProgress,
  updateGuestSyncSetting,
} from '../utils/guestStorage.js'

/* eslint-disable react-refresh/only-export-components */
const ProgressContext = createContext(null)

function toMigrationEntry(entry) {
  return {
    problemId: entry.problemId,
    solved: entry.solved,
    solvedAt: entry.solvedAt ?? null,
    notes: entry.notes ?? null,
    isBookmarked: entry.isBookmarked,
    revisionStatus: entry.revisionStatus ?? null,
    userDifficulty: entry.userDifficulty ?? null,
    lastAttemptedAt: entry.lastAttemptedAt ?? null,
  }
}

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
  const [guestSettings, setGuestSettings] = useState(() => getGuestProgress())
  const [accountSettings, setAccountSettings] = useState(null)
  const [accountSettingsUserId, setAccountSettingsUserId] = useState(null)
  const settingsLoading = Boolean(user && token && accountSettingsUserId !== user.userId)
  const settings = user && token
    ? (accountSettingsUserId === user.userId ? accountSettings : null)
    : guestSettings

  useEffect(() => {
    let cancelled = false
    if (!user || !token) {
      return undefined
    }

    progressApi.getUserSettings()
      .then((currentSettings) => {
        if (!cancelled) {
          setAccountSettings(currentSettings)
          setAccountSettingsUserId(user.userId)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setAccountSettings(null)
          setAccountSettingsUserId(user.userId)
        }
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
      setAccountSettings(updatedSettings)
      setAccountSettingsUserId(user.userId)
      return updatedSettings
    }

    const guestProgress = updateGuestSyncSetting(syncProgressAcrossSheets)
    setGuestSettings(guestProgress)
    return guestProgress
  }, [user, token])

  const migrateGuestProgress = useCallback(async () => {
    const guestProgress = getGuestProgress()
    const payload = {
      syncProgressAcrossSheets: guestProgress.syncProgressAcrossSheets,
      problemProgress: guestProgress.syncProgressAcrossSheets
        ? Object.values(guestProgress.problemProgress).map(toMigrationEntry)
        : [],
      sheetProblemProgress: guestProgress.syncProgressAcrossSheets
        ? []
        : Object.values(guestProgress.sheetProblemProgress)
          .flatMap((items) => Object.values(items))
          .map((entry) => ({ ...toMigrationEntry(entry), sheetId: entry.sheetId })),
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
