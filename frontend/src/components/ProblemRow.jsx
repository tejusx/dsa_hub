import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useProgress } from '../context/ProgressContext.jsx'

function platformKey(linkType = '') {
  const key = linkType.toLowerCase().replace(/[^a-z0-9]/g, '')
  if (key.includes('leetcode')) return 'leetcode'
  if (key.includes('geeksforgeeks') || key === 'gfg') return 'gfg'
  if (key.includes('youtube')) return 'youtube'
  if (key.includes('codingninja') || key.includes('code360')) return 'code360'
  if (key.includes('takeuforward') || key === 'tuf') return 'takeuforward'
  if (key.includes('neetcode')) return 'neetcode'
  if (key.includes('editorial')) return 'editorial'
  return key
}

function uniquePlatformLinks(links) {
  const seenPlatforms = new Set()
  return (Array.isArray(links) ? links : []).filter((link) => {
    if (typeof link?.url !== 'string' || !link.url || typeof link?.linkType !== 'string') return false
    const key = platformKey(link.linkType)
    if (!key || seenPlatforms.has(key)) return false
    seenPlatforms.add(key)
    return true
  })
}

function platformLabel(linkType) {
  const key = platformKey(linkType)
  if (key === 'leetcode') return 'LeetCode'
  if (key === 'gfg') return 'GeeksforGeeks'
  if (key === 'youtube') return 'YouTube'
  if (key === 'code360') return 'Code360'
  if (key === 'neetcode') return 'NeetCode'
  if (key === 'takeuforward') return 'Take U Forward'
  if (key === 'editorial') return 'Editorial'
  if (key === 'spoj') return 'SPOJ'
  if (key === 'discussion') return 'Discussion'
  return linkType.replace(/[_-]+/g, ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())
}

function platformMark(linkType) {
  const key = platformKey(linkType)
  if (key === 'youtube') {
    return <svg viewBox="0 0 20 14" aria-hidden="true"><rect x="1" y="1" width="18" height="12" rx="4" /><path d="m8 4 5 3-5 3z" /></svg>
  }
  if (key === 'leetcode') return <span className="platform-letters platform-letters-leetcode">LC</span>
  if (key === 'gfg') return <span className="platform-letters platform-letters-gfg">GfG</span>
  if (key === 'code360') return <span className="platform-letters platform-letters-code360">360</span>
  if (key === 'neetcode') return <span className="platform-letters platform-letters-neetcode">NC</span>
  if (key === 'takeuforward') return <span className="platform-letters platform-letters-tuf">TUF</span>
  if (key === 'editorial') return <span className="platform-letters platform-letters-editorial">Ed</span>
  if (key === 'spoj') return <span className="platform-letters">S</span>
  if (key === 'discussion') return <span className="platform-letters">D</span>
  return <span className="platform-letters">↗</span>
}

function ProblemRow({ problem, links = [], sheetId, progress: initialProgress, onProgressChange }) {
  const { getProgress, markProblemComplete, markProblemIncomplete } = useProgress()
  const [progress, setProgress] = useState(initialProgress ?? {
    problemId: problem.problemId,
    solved: false,
    isBookmarked: false,
  })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const platformLinks = uniquePlatformLinks(links)

  useEffect(() => {
    if (initialProgress !== undefined) {
      // Keep repeated appearances of the same canonical problem in sync.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setProgress(initialProgress)
      return undefined
    }

    let cancelled = false
    getProgress(problem.problemId, sheetId)
      .then((value) => {
        if (!cancelled) setProgress(value)
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message || 'Could not load progress.')
      })
    return () => { cancelled = true }
  }, [getProgress, initialProgress, problem.problemId, sheetId])

  async function handleSolvedChange(event) {
    event.stopPropagation()
    const solved = event.target.checked
    const previousProgress = progress
    const optimisticProgress = { ...previousProgress, solved }
    setProgress(optimisticProgress)
    onProgressChange?.(optimisticProgress)
    setBusy(true)
    setError('')
    try {
      const updated = solved
        ? await markProblemComplete(problem.problemId, sheetId)
        : await markProblemIncomplete(problem.problemId, sheetId)
      setProgress(updated)
      onProgressChange?.(updated)
    } catch (requestError) {
      setProgress(previousProgress)
      onProgressChange?.(previousProgress)
      setError(requestError.message || 'Progress could not be saved. Please retry.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <article className={`problem-row${progress.solved ? ' is-solved' : ''}`}>
      <label className="solved-control" onClick={(event) => event.stopPropagation()}>
        <input
          type="checkbox"
          checked={Boolean(progress.solved)}
          disabled={busy}
          onChange={handleSolvedChange}
          aria-label={`Mark ${problem.title} ${progress.solved ? 'incomplete' : 'complete'}`}
        />
        <span className="custom-checkbox" aria-hidden="true">{progress.solved ? '✓' : ''}</span>
      </label>
      <div className="problem-row-main">
        <Link className="problem-title" to={`/problems/${encodeURIComponent(problem.problemId)}`}>
          {problem.title}
        </Link>
        {error && <p className="inline-error" role="status">{error}</p>}
      </div>
      <div className="problem-row-side">
        {platformLinks.length > 0 && <div className="platform-links" aria-label="Problem platforms">
          {platformLinks.map((link) => {
            const label = platformLabel(link.linkType)
            const platform = platformKey(link.linkType)
            return <a
              key={platform}
              className={`platform-link platform-${platform}`}
              href={link.url}
              target="_blank"
              rel="noopener noreferrer"
              aria-label={`Open ${problem.title} on ${label} in a new tab`}
              title={label}
              onClick={(event) => event.stopPropagation()}
            >{platformMark(link.linkType)}</a>
          })}
        </div>}
      </div>
    </article>
  )
}

export default ProblemRow
