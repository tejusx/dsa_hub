export const GUEST_PROGRESS_STORAGE_KEY = 'dsa_hub_guest_progress'

function emptyGuestProgress() {
  return {
    version: 1,
    syncProgressAcrossSheets: true,
    problemProgress: {},
    sheetProblemProgress: {},
  }
}

/** Read the version-1 guest progress document from localStorage. */
export function getGuestProgress() {
  try {
    const stored = localStorage.getItem(GUEST_PROGRESS_STORAGE_KEY)
    if (!stored) return emptyGuestProgress()

    const progress = JSON.parse(stored)
    if (progress?.version !== 1) return emptyGuestProgress()

    return {
      ...emptyGuestProgress(),
      ...progress,
      problemProgress: progress.problemProgress ?? {},
      sheetProblemProgress: progress.sheetProblemProgress ?? {},
    }
  } catch {
    return emptyGuestProgress()
  }
}

/** Save the version-1 guest progress document. */
export function saveGuestProgress(progress) {
  const value = { ...emptyGuestProgress(), ...progress, version: 1 }
  localStorage.setItem(GUEST_PROGRESS_STORAGE_KEY, JSON.stringify(value))
  return value
}

/** Update one global problem entry or one problem entry inside a sheet. */
export function updateGuestProblemProgress(problemId, updates, sheetId) {
  const progress = getGuestProgress()
  const entry = { problemId, ...updates }

  if (progress.syncProgressAcrossSheets) {
    progress.problemProgress[problemId] = entry
  } else {
    if (!sheetId) throw new Error('sheetId is required when guest sync is disabled')
    progress.sheetProblemProgress[sheetId] ??= {}
    progress.sheetProblemProgress[sheetId][problemId] = { ...entry, sheetId }
  }

  return saveGuestProgress(progress)
}

export function updateGuestSyncSetting(syncProgressAcrossSheets) {
  const progress = getGuestProgress()
  progress.syncProgressAcrossSheets = Boolean(syncProgressAcrossSheets)
  return saveGuestProgress(progress)
}

export function clearGuestProgress() {
  localStorage.removeItem(GUEST_PROGRESS_STORAGE_KEY)
}
