import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { getSheets, getSheetProblems } from '../api/sheetApi.js'
import { useProgress } from '../context/ProgressContext.jsx'

function SheetSwitcher() {
  const [sheets, setSheets] = useState([])
  const [stats, setStats] = useState({})
  const [open, setOpen] = useState(false)
  const [loadingStats, setLoadingStats] = useState(false)
  const rootRef = useRef(null)
  const navigate = useNavigate()
  const location = useLocation()
  const { getProgress } = useProgress()
  const currentSheetId = location.pathname.match(/^\/sheets\/([^/]+)/)?.[1]
  const currentSheet = sheets.find((sheet) => sheet.sheetId === decodeURIComponent(currentSheetId ?? ''))

  useEffect(() => {
    let cancelled = false
    getSheets().then((result) => {
      if (!cancelled) setSheets((result ?? []).filter((sheet) => sheet.enabled !== false))
    }).catch(() => {
      if (!cancelled) setSheets([])
    })
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    if (!open) return undefined
    function closeOnOutside(event) {
      if (!rootRef.current?.contains(event.target)) setOpen(false)
    }
    function closeOnEscape(event) {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('pointerdown', closeOnOutside)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOnOutside)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [open])

  async function toggleMenu() {
    const nextOpen = !open
    setOpen(nextOpen)
    if (!nextOpen || !sheets.length) return
    setLoadingStats(true)
    try {
      const entries = await Promise.all(sheets.map(async (sheet) => {
        const [problems, progress] = await Promise.all([
          getSheetProblems(sheet.sheetId),
          getProgress(null, sheet.sheetId),
        ])
        const solvedIds = new Set((Array.isArray(progress) ? progress : [])
          .filter((item) => item.solved).map((item) => item.problemId))
        return [sheet.sheetId, {
          total: problems.length,
          solved: problems.filter((problem) => solvedIds.has(problem.problemId)).length,
        }]
      }))
      setStats(Object.fromEntries(entries))
    } catch {
      setStats({})
    } finally {
      setLoadingStats(false)
    }
  }

  function visitSheet(sheetId) {
    setOpen(false)
    navigate(`/sheets/${encodeURIComponent(sheetId)}`)
  }

  return (
    <div className="sheet-switcher" ref={rootRef}>
      <button className={`sheet-switcher-trigger${open ? ' is-open' : ''}`} type="button"
        aria-label={`Switch practice sheet${currentSheet ? `, current sheet ${currentSheet.name}` : ''}`}
        aria-expanded={open} aria-controls="sheet-switcher-menu" onClick={toggleMenu}>
        <span className="sheet-switcher-trigger-copy">
          <small>{currentSheet ? 'Current sheet' : 'Practice sheets'}</small>
          <strong>{currentSheet?.name ?? 'Choose a sheet'}</strong>
        </span>
        <span className="sheet-switcher-chevron" aria-hidden="true">⌄</span>
      </button>
      {open && <div className="sheet-switcher-menu" id="sheet-switcher-menu" role="menu" aria-label="Your sheets">
        <p className="sheet-switcher-heading">Switch practice sheet</p>
        {sheets.length === 0
          ? <p className="sheet-switcher-empty">Sheets are unavailable right now.</p>
          : sheets.map((sheet) => {
            const stat = stats[sheet.sheetId]
            const current = decodeURIComponent(currentSheetId ?? '') === sheet.sheetId
            return <button className={`sheet-switcher-item${current ? ' is-current' : ''}`} key={sheet.sheetId}
              type="button" role="menuitem" aria-current={current ? 'page' : undefined}
              onClick={() => visitSheet(sheet.sheetId)}>
              <span className="sheet-switcher-check" aria-hidden="true">{current ? '✓' : ''}</span>
              <span className="sheet-switcher-name">{sheet.name}</span>
              <small>{loadingStats ? '…' : stat ? `${stat.solved} / ${stat.total}` : ''}</small>
            </button>
          })}
      </div>}
    </div>
  )
}

export default SheetSwitcher
