import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getProblem, getProblemLinks, getProblemSheets, getProblemTopics } from '../api/problemApi.js'
import { useProgress } from '../context/ProgressContext.jsx'

function ProblemDetail() {
  const { problemId } = useParams()
  const { getProgress, markProblemComplete, markProblemIncomplete } = useProgress()
  const [problem, setProblem] = useState(null)
  const [sheets, setSheets] = useState([])
  const [links, setLinks] = useState([])
  const [topics, setTopics] = useState([])
  const [progress, setProgress] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [progressError, setProgressError] = useState('')
  const [relatedErrors, setRelatedErrors] = useState([])
  const [saving, setSaving] = useState(false)

  const loadProblem = useCallback(async () => {
    try {
      const problemData = await getProblem(problemId)
      setError('')
      setProgressError('')
      setRelatedErrors([])
      setProblem(problemData)
      const [sheetsResult, linksResult, topicsResult, progressResult] = await Promise.allSettled([
        getProblemSheets(problemId),
        getProblemLinks(problemId),
        getProblemTopics(problemId),
        getProgress(problemId),
      ])

      const failures = []
      if (sheetsResult.status === 'fulfilled') setSheets(sheetsResult.value)
      else failures.push('sheets')
      if (linksResult.status === 'fulfilled') setLinks(linksResult.value)
      else failures.push('links')
      if (topicsResult.status === 'fulfilled') setTopics(topicsResult.value.map((item) => item.topic))
      else failures.push('topics')
      if (progressResult.status === 'fulfilled') setProgress(progressResult.value)
      else setProgressError(progressResult.reason.message || 'Progress could not be loaded.')
      setRelatedErrors(failures)
    } catch (loadError) {
      setProblem(null)
      setError(loadError.message || 'This problem could not be loaded.')
    } finally {
      setLoading(false)
    }
  }, [getProgress, problemId])

  useEffect(() => {
    // The loader changes state only after its asynchronous API requests settle.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadProblem()
  }, [loadProblem])

  async function toggleSolved(event) {
    const solved = event.target.checked
    const previous = progress
    setProgress({ ...previous, solved })
    setSaving(true)
    setProgressError('')
    try {
      const updated = solved
        ? await markProblemComplete(problemId)
        : await markProblemIncomplete(problemId)
      setProgress(updated)
    } catch (saveError) {
      setProgress(previous)
      setProgressError(saveError.message || 'Progress could not be saved. Please retry.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="page-stack">
      {loading && <div className="state-panel" role="status">Loading problem…</div>}
      {!loading && error && <div className="state-panel state-error" role="alert"><p>{error}</p><Link className="button button-secondary" to="/sheets">Browse sheets</Link><button className="button button-quiet" type="button" onClick={() => { setLoading(true); setError(''); loadProblem() }}>Retry</button></div>}
      {!loading && !error && problem && (
        <>
          <div className="breadcrumb"><Link to="/sheets">Sheets</Link><span aria-hidden="true">/</span><span>Problem</span></div>
          <section className="problem-detail-header">
            <div>
              <p className="eyebrow">Problem</p>
              <h1>{problem.title}</h1>
              <div className="problem-meta detail-meta">
                {problem.difficulty && <span className={`difficulty difficulty-${problem.difficulty.toLowerCase()}`}>{problem.difficulty}</span>}
                {problem.leetcodeId && <span>LeetCode #{problem.leetcodeId}</span>}
                {problem.leetcodePaidOnly && <span className="topic-tag">Premium</span>}
              </div>
            </div>
            <label className="solved-toggle">
              <input type="checkbox" checked={Boolean(progress?.solved)} disabled={saving || !progress} onChange={toggleSolved} />
              <span>{progress?.solved ? 'Solved' : 'Mark as solved'}</span>
            </label>
          </section>

          {progressError && <p className="inline-error" role="status">{progressError}</p>}
          {relatedErrors.length > 0 && <div className="state-panel state-warning" role="status">Some related {relatedErrors.join(', ')} could not be loaded.</div>}

          <div className="problem-detail-grid">
            <section className="overview-card detail-panel">
              <div className="section-heading"><div><p className="eyebrow">Problem library</p><h2>Topics</h2></div></div>
              {topics.length ? <div className="tag-list">{topics.map((topic) => <span className="topic-tag" key={topic}>{topic}</span>)}</div> : <p className="muted-text">No topics are listed for this problem.</p>}
            </section>
            <section className="overview-card detail-panel">
              <div className="section-heading"><div><p className="eyebrow">Study paths</p><h2>Appears in</h2></div><span className="muted-text">{sheets.length}</span></div>
              {sheets.length
                ? <ul className="related-list">{sheets.map((sheet) => <li key={sheet.sheetId}><Link to={`/sheets/${encodeURIComponent(sheet.sheetId)}`}>{sheet.name}<span aria-hidden="true">→</span></Link></li>)}</ul>
                : <p className="muted-text">This problem is not currently listed in an enabled sheet.</p>}
            </section>
          </div>

          <section className="overview-card detail-panel">
            <div className="section-heading"><div><p className="eyebrow">External resources</p><h2>Problem links</h2></div></div>
            {links.length
              ? <ul className="external-link-list">{links.map((link) => <li key={`${link.linkType}-${link.url}`}><a href={link.url} target="_blank" rel="noreferrer"><span>{link.linkType}</span><span className="external-url">{link.url}</span><span aria-hidden="true">↗</span></a></li>)}</ul>
              : <p className="muted-text">No external links are available for this problem.</p>}
          </section>
        </>
      )}
    </div>
  )
}

export default ProblemDetail
