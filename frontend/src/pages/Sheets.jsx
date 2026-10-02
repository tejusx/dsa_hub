import { useCallback, useEffect, useMemo, useState } from 'react'
import SearchBar from '../components/SearchBar.jsx'
import SheetCard from '../components/SheetCard.jsx'
import { getSheetProblems, getSheets } from '../api/sheetApi.js'
import { useProgress } from '../context/ProgressContext.jsx'

function Sheets() {
  const { getProgress } = useProgress()
  const [sheets, setSheets] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadSheets = useCallback(async () => {
    try {
      const enabledSheets = (await getSheets()).filter((sheet) => sheet.enabled)
      const sheetCards = await Promise.all(enabledSheets.map(async (sheet) => {
        const [problems, progress] = await Promise.all([
          getSheetProblems(sheet.sheetId),
          getProgress(null, sheet.sheetId),
        ])
        const solvedIds = new Set(progress.filter((item) => item.solved).map((item) => item.problemId))
        return {
          sheet,
          problemCount: problems.length,
          solvedCount: problems.filter((problem) => solvedIds.has(problem.problemId)).length,
        }
      }))
      setError('')
      setSheets(sheetCards)
    } catch (loadError) {
      setError(loadError.message || 'Practice sheets could not be loaded. Please try again.')
    } finally {
      setLoading(false)
    }
  }, [getProgress])

  useEffect(() => {
    // The loader changes state only after its asynchronous API requests settle.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadSheets()
  }, [loadSheets])

  const filteredSheets = useMemo(() => sheets.filter(({ sheet }) =>
    sheet.name.toLowerCase().includes(search.trim().toLowerCase())), [sheets, search])

  return (
    <div className="page-stack">
      <section className="page-heading-row">
        <div><p className="eyebrow">Curated practice paths</p><h1>Sheets</h1><p className="lede">Follow a structured set of problems and keep your progress in view.</p></div>
        <SearchBar value={search} onChange={setSearch} label="Search sheets" placeholder="Find a sheet…" />
      </section>

      {loading && <div className="state-panel" role="status">Loading practice sheets…</div>}
      {!loading && error && <div className="state-panel state-error" role="alert"><p>{error}</p><button className="button button-secondary" type="button" onClick={() => { setLoading(true); setError(''); loadSheets() }}>Try again</button></div>}
      {!loading && !error && filteredSheets.length === 0 && (
        <div className="state-panel">{sheets.length ? 'No sheets match your search.' : 'No enabled practice sheets are available yet.'}</div>
      )}
      {!loading && !error && filteredSheets.length > 0 && (
        <div className="sheet-grid">
          {filteredSheets.map(({ sheet, problemCount, solvedCount }) => (
            <SheetCard key={sheet.sheetId} sheet={sheet} problemCount={problemCount} solvedCount={solvedCount} />
          ))}
        </div>
      )}
    </div>
  )
}

export default Sheets
