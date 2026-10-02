import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProblems } from '../api/problemApi.js'
import { getSheets, getSheetProblems } from '../api/sheetApi.js'
import ProgressBar from '../components/ProgressBar.jsx'
import SheetCard from '../components/SheetCard.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { useProgress } from '../context/ProgressContext.jsx'

function collectUniqueProgress(groups) {
  const byProblem = new Map()
  for (const progress of groups.flat()) {
    const current = byProblem.get(progress.problemId)
    if (!current) {
      byProblem.set(progress.problemId, progress)
      continue
    }
    byProblem.set(progress.problemId, {
      ...current,
      ...progress,
      solved: current.solved || progress.solved,
      updatedAt: [current.updatedAt, progress.updatedAt].filter(Boolean).sort().at(-1),
    })
  }
  return [...byProblem.values()]
}

function Dashboard() {
  const { user, isAuthenticated, loading: authLoading } = useAuth()
  const { settings, settingsLoading, getProgress } = useProgress()
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadDashboard = useCallback(async () => {
    try {
      const [problems, sheets] = await Promise.all([getProblems(), getSheets()])
      setError('')
      const activeSheets = sheets.filter((sheet) => sheet.enabled)
      let progressGroups

      if (settings?.syncProgressAcrossSheets === false) {
        progressGroups = await Promise.all(activeSheets.map((sheet) => getProgress(null, sheet.sheetId)))
      } else {
        progressGroups = [await getProgress()]
      }

      const sheetsWithProgress = await Promise.all(activeSheets.map(async (sheet, index) => {
        const [sheetProblems, sheetProgress] = await Promise.all([
          getSheetProblems(sheet.sheetId),
          progressGroups.length === activeSheets.length
            ? progressGroups[index]
            : getProgress(null, sheet.sheetId),
        ])
        const solvedProblemIds = new Set(sheetProgress.filter((item) => item.solved).map((item) => item.problemId))
        return {
          ...sheet,
          problemCount: sheetProblems.length,
          solvedCount: sheetProblems.filter((problem) => solvedProblemIds.has(problem.problemId)).length,
        }
      }))

      setDashboard({ problems, sheets: sheetsWithProgress, progress: collectUniqueProgress(progressGroups) })
    } catch (loadError) {
      setError(loadError.message || 'The dashboard data could not be loaded. Please try again.')
    } finally {
      setLoading(false)
    }
  }, [getProgress, settings?.syncProgressAcrossSheets])

  useEffect(() => {
    if (authLoading) return
    if (isAuthenticated && settingsLoading) return
    // The loader changes state only after its asynchronous API requests settle.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadDashboard()
  }, [authLoading, isAuthenticated, settingsLoading, loadDashboard])

  const problemById = useMemo(() => new Map(
    (dashboard?.problems ?? []).map((problem) => [problem.problemId, problem]),
  ), [dashboard?.problems])
  const solvedCount = dashboard?.progress.filter((progress) =>
    progress.solved && problemById.has(progress.problemId),
  ).length ?? 0
  const totalProblems = dashboard?.problems.length ?? 0
  const remainingCount = Math.max(0, totalProblems - solvedCount)
  const percentage = totalProblems ? (solvedCount / totalProblems) * 100 : 0
  const recentProblems = (dashboard?.progress ?? [])
    .filter((progress) => problemById.has(progress.problemId))
    .sort((a, b) => new Date(b.updatedAt || b.lastAttemptedAt || b.solvedAt || 0)
      - new Date(a.updatedAt || a.lastAttemptedAt || a.solvedAt || 0))
    .slice(0, 5)

  return (
    <div className="page-stack">
      <section className="welcome-section">
        <div>
          <p className="eyebrow">Your practice workspace</p>
          <h1>{user?.displayName ? `Welcome back, ${user.displayName}` : 'Make your next problem count.'}</h1>
          <p className="lede">A focused place to work through data structures and algorithms, one problem at a time.</p>
        </div>
        {!authLoading && !isAuthenticated && (
          <aside className="guest-note">
            <span className="guest-indicator" />
            <div><strong>Guest progress is saved on this device.</strong><p>Create an account when you’re ready to sync across devices.</p></div>
            <Link to="/register">Create account <span aria-hidden="true">→</span></Link>
          </aside>
        )}
      </section>

      {loading && <div className="state-panel" role="status">Loading your practice data…</div>}
      {!loading && error && (
        <div className="state-panel state-error" role="alert">
          <p>{error}</p><button className="button button-secondary" type="button" onClick={() => { setLoading(true); setError(''); loadDashboard() }}>Try again</button>
        </div>
      )}

      {!loading && !error && dashboard && (
        <>
          <section className="stats-grid" aria-label="Practice statistics">
            <article className="stat-card"><span className="stat-label">Problems available</span><strong>{totalProblems.toLocaleString()}</strong><span className="stat-foot">Across the problem library</span></article>
            <article className="stat-card stat-mint"><span className="stat-label">Solved</span><strong>{solvedCount.toLocaleString()}</strong><span className="stat-foot">Problems marked complete</span></article>
            <article className="stat-card"><span className="stat-label">Remaining</span><strong>{remainingCount.toLocaleString()}</strong><span className="stat-foot">Ready when you are</span></article>
            <article className="stat-card stat-violet"><span className="stat-label">Available sheets</span><strong>{dashboard.sheets.length}</strong><span className="stat-foot">Curated practice paths</span></article>
          </section>

          <section className="overview-card">
            <div className="section-heading"><div><p className="eyebrow">At a glance</p><h2>Overall progress</h2></div><strong className="percentage-value">{Math.round(percentage)}%</strong></div>
            <ProgressBar value={percentage} label="Overall problem completion" />
            <p className="muted-text">{solvedCount.toLocaleString()} of {totalProblems.toLocaleString()} problems solved</p>
          </section>

          <section className="section-block">
            <div className="section-heading"><div><p className="eyebrow">Choose a path</p><h2>Practice sheets</h2></div><Link className="quiet-link" to="/sheets">View all <span aria-hidden="true">→</span></Link></div>
            {dashboard.sheets.length === 0
              ? <div className="state-panel">No enabled practice sheets are available yet.</div>
              : <div className="sheet-grid">{dashboard.sheets.slice(0, 3).map((sheet) => (
                <SheetCard key={sheet.sheetId} sheet={sheet} problemCount={sheet.problemCount} solvedCount={sheet.solvedCount} />
              ))}</div>}
          </section>

          <section className="section-block">
            <div className="section-heading"><div><p className="eyebrow">Pick up where you left off</p><h2>Recently worked</h2></div></div>
            {recentProblems.length === 0
              ? <div className="state-panel">Your recent problems will appear here after you start practicing.</div>
              : <div className="recent-list">{recentProblems.map((progress) => {
                const problem = problemById.get(progress.problemId)
                return <Link className="recent-item" key={problem.problemId} to={`/problems/${encodeURIComponent(problem.problemId)}`}>
                  <span className={`recent-status${progress.solved ? ' is-solved' : ''}`} aria-hidden="true">{progress.solved ? '✓' : '·'}</span>
                  <span><strong>{problem.title}</strong>{problem.difficulty && <small>{problem.difficulty}</small>}</span>
                  <span className="recent-arrow" aria-hidden="true">→</span>
                </Link>
              })}</div>}
          </section>
        </>
      )}
    </div>
  )
}

export default Dashboard
