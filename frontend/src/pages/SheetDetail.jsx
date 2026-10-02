import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import ProblemRow from '../components/ProblemRow.jsx'
import ProgressBar from '../components/ProgressBar.jsx'
import SearchBar from '../components/SearchBar.jsx'
import { getSheet, getSheetProblems } from '../api/sheetApi.js'
import { useProgress } from '../context/ProgressContext.jsx'

function orderByPosition(left, right) {
  const leftPosition = Number.isFinite(left.position) ? left.position : Number.MAX_SAFE_INTEGER
  const rightPosition = Number.isFinite(right.position) ? right.position : Number.MAX_SAFE_INTEGER
  // Array#sort is stable, so equal per-section positions retain backend order.
  return leftPosition - rightPosition
}

function getSectionPath(problem) {
  const sourcePath = Array.isArray(problem.sectionPath) ? problem.sectionPath : []
  const path = sourcePath.filter((segment) => typeof segment === 'string' && segment.trim())
  if (path.length) return path

  return [problem.section, problem.topic ?? problem.subsection]
    .filter((segment, index, values) => typeof segment === 'string' && segment.trim() && values.indexOf(segment) === index)
}

function nodeDomId(nodeKey) {
  return `sheet-node-${encodeURIComponent(nodeKey)}`
}

function buildSheetLayout(problems) {
  const orderedProblems = [...problems].sort(orderByPosition)
  const hasHierarchy = orderedProblems.some((problem) => getSectionPath(problem).length > 0)
  if (!hasHierarchy) {
    return { hierarchical: false, blocks: [{ type: 'flat', key: 'flat', problems: orderedProblems }] }
  }

  const blocks = []
  const nodesByPath = new Map()
  for (const problem of orderedProblems) {
    const path = getSectionPath(problem)
    if (path.length === 0) {
      let flatBlock = blocks.at(-1)
      if (!flatBlock || flatBlock.type !== 'flat') {
        flatBlock = { type: 'flat', key: `flat-${problem.occurrenceId ?? problem.problemId}`, problems: [] }
        blocks.push(flatBlock)
      }
      flatBlock.problems.push(problem)
      continue
    }

    let parent = null
    for (let depth = 0; depth < path.length; depth += 1) {
      const nodePath = path.slice(0, depth + 1)
      const nodeKey = JSON.stringify(nodePath)
      let node = nodesByPath.get(nodeKey)
      if (!node) {
        node = { type: 'node', key: nodeKey, name: path[depth], path: nodePath, children: [], problems: [] }
        nodesByPath.set(nodeKey, node)
        if (parent) parent.children.push(node)
        else blocks.push(node)
      }
      parent = node
    }
    parent.problems.push(problem)
  }

  return { hierarchical: true, blocks }
}

function SheetDetail() {
  const { sheetId } = useParams()
  const { getProgress } = useProgress()
  const [sheet, setSheet] = useState(null)
  const [problems, setProblems] = useState([])
  const [progressById, setProgressById] = useState(new Map())
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [difficultyFilter, setDifficultyFilter] = useState('all')
  const [expandedBySheet, setExpandedBySheet] = useState({})
  const expandedPaths = expandedBySheet[sheetId] ?? new Set()
  const [mapCollapsed, setMapCollapsed] = useState(false)
  const [progressCollapsed, setProgressCollapsed] = useState(false)
  const [currentSection, setCurrentSection] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadSheet = useCallback(async () => {
    try {
      const [sheetData, sheetProblems, progress] = await Promise.all([
        getSheet(sheetId),
        getSheetProblems(sheetId),
        getProgress(null, sheetId),
      ])
      setError('')
      setSheet(sheetData)
      setProblems(sheetProblems)
      setProgressById(new Map(progress.map((item) => [item.problemId, item])))
    } catch (loadError) {
      setError(loadError.message || 'This sheet could not be loaded.')
    } finally {
      setLoading(false)
    }
  }, [getProgress, sheetId])

  useEffect(() => {
    // The loader changes state only after its asynchronous API requests settle.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadSheet()
  }, [loadSheet])

  const difficulties = useMemo(() => [...new Set(
    problems.map((problem) => problem.difficulty).filter(Boolean),
  )].sort(), [problems])

  const solvedCount = problems.filter((problem) => progressById.get(problem.problemId)?.solved).length
  const percentage = problems.length ? (solvedCount / problems.length) * 100 : 0
  const updateProblemProgress = useCallback((progress) => {
    setProgressById((current) => new Map(current).set(progress.problemId, progress))
  }, [])

  const sheetLayout = useMemo(() => buildSheetLayout(problems), [problems])
  const sectionBlocks = sheetLayout.blocks.filter((block) => block.type === 'node')
  const filteredProblems = useMemo(() => {
    const query = search.trim().toLowerCase()
    return problems.filter((problem) => {
      const matchesSearch = problem.title.toLowerCase().includes(query) ||
        (problem.leetcodeId && problem.leetcodeId.toLowerCase().includes(query))
      const solved = Boolean(progressById.get(problem.problemId)?.solved)
      const matchesStatus = statusFilter === 'all' || (statusFilter === 'solved' ? solved : !solved)
      const matchesDifficulty = difficultyFilter === 'all' || problem.difficulty === difficultyFilter
      return matchesSearch && matchesStatus && matchesDifficulty
    })
  }, [problems, progressById, search, statusFilter, difficultyFilter])
  const visibleProblems = useMemo(() => new Set(filteredProblems), [filteredProblems])

  function updateExpandedPaths(update) {
    setExpandedBySheet((current) => {
      const next = new Set(current[sheetId] ?? [])
      update(next)
      return { ...current, [sheetId]: next }
    })
  }

  useEffect(() => {
    if (!sheetLayout.hierarchical || typeof IntersectionObserver === 'undefined') return undefined
    const sections = [...document.querySelectorAll('[data-sheet-section]')]
    if (!sections.length) return undefined
    const observer = new IntersectionObserver((entries) => {
      const visible = entries.filter((entry) => entry.isIntersecting)
        .sort((left, right) => left.boundingClientRect.top - right.boundingClientRect.top)
      if (visible[0]) setCurrentSection(visible[0].target.dataset.sheetSection)
    }, { rootMargin: '-15% 0px -70% 0px', threshold: 0 })
    sections.forEach((section) => observer.observe(section))
    return () => observer.disconnect()
  }, [sheetLayout.hierarchical, filteredProblems])

  const recentSolved = useMemo(() => {
    const titleById = new Map(problems.map((problem) => [problem.problemId, problem.title]))
    return [...progressById.values()]
      .filter((item) => item.solved && titleById.has(item.problemId) && (item.updatedAt || item.solvedAt || item.lastAttemptedAt))
      .sort((left, right) => new Date(right.updatedAt || right.solvedAt || right.lastAttemptedAt)
        - new Date(left.updatedAt || left.solvedAt || left.lastAttemptedAt))
      .slice(0, 3)
      .map((item) => ({ ...item, title: titleById.get(item.problemId) }))
  }, [problems, progressById])

  function renderProblemRows(items) {
    return <div className="problem-list">{items.map((problem) => (
      <ProblemRow
        key={problem.occurrenceId ?? `${problem.problemId}-${problem.position ?? 'unpositioned'}`}
        problem={problem}
        links={problem.links}
        sheetId={sheetId}
        onProgressChange={updateProblemProgress}
        progress={progressById.get(problem.problemId) ?? {
          problemId: problem.problemId, solved: false, isBookmarked: false,
        }}
      />
    ))}</div>
  }

  const visibleProblemCount = filteredProblems.length
  const hasActiveFilter = Boolean(search.trim()) || statusFilter !== 'all' || difficultyFilter !== 'all'

  function getCounts(items) {
    const solved = items.filter((problem) => progressById.get(problem.problemId)?.solved).length
    return { solved, total: items.length, percentage: items.length ? (solved / items.length) * 100 : 0 }
  }

  function renderProgress(counts, compact = false) {
    return <div className={`roadmap-progress${compact ? ' is-compact' : ''}`}>
      <ProgressBar value={counts.percentage} label={`${counts.solved} of ${counts.total} solved`} />
      <span>{counts.solved}/{counts.total}</span>
    </div>
  }

  function sectionProblems(node) {
    return [...node.problems, ...node.children.flatMap((child) => sectionProblems(child))]
  }

  function nodeHasVisibleProblems(node) {
    return sectionProblems(node).some((problem) => visibleProblems.has(problem))
  }

  function renderNode(node, depth = 0) {
    const isRoot = depth === 0
    const expanded = expandedPaths.has(node.key) || (hasActiveFilter && nodeHasVisibleProblems(node))
    const nodeProblems = sectionProblems(node)
    const nodeCounts = getCounts(nodeProblems)
    const visible = nodeHasVisibleProblems(node)
    if (hasActiveFilter && !visible) return null
    const toggleClass = isRoot ? 'roadmap-section-toggle' : 'roadmap-subtopic-toggle'
    const sectionClass = isRoot ? 'roadmap-section' : 'roadmap-subtopic'

    return <section className={`${sectionClass} roadmap-node`} key={node.key} id={nodeDomId(node.key)}
      data-depth={depth} data-sheet-section={isRoot ? node.key : undefined}>
      <button
        className={toggleClass}
        type="button"
        aria-expanded={expanded}
        onClick={() => updateExpandedPaths((next) => {
          if (next.has(node.key)) next.delete(node.key)
          else next.add(node.key)
        })}
      >
        <span className={`roadmap-chevron${expanded ? ' is-open' : ''}`} aria-hidden="true" />
        <span className={isRoot ? 'roadmap-section-name' : 'roadmap-subtopic-name'}>{node.name}</span>
        {renderProgress(nodeCounts, !isRoot)}
      </button>
      {expanded && <div className="roadmap-subtopics roadmap-node-children">
        {node.problems.length > 0 && renderProblemRows(node.problems.filter((problem) => visibleProblems.has(problem)))}
        {node.children.map((child) => renderNode(child, depth + 1))}
      </div>}
    </section>
  }

  const hierarchyContent = sheetLayout.hierarchical
    ? <div className="roadmap-sections">{sheetLayout.blocks.map((block) => block.type === 'flat'
      ? <div className="roadmap-flat-block" key={block.key}>{renderProblemRows(block.problems.filter((problem) => visibleProblems.has(problem)))}</div>
      : renderNode(block))}</div>
    : <div className="sheet-flat-block">{renderProblemRows(filteredProblems)}</div>

  function navigateToNode(node) {
    updateExpandedPaths((next) => {
      for (let depth = 1; depth <= node.path.length; depth += 1) {
        next.add(JSON.stringify(node.path.slice(0, depth)))
      }
    })
    window.setTimeout(() => document.getElementById(nodeDomId(node.key))?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 50)
  }

  function renderMapNode(node, depth = 0) {
    const counts = getCounts(sectionProblems(node))
    const isCurrent = depth === 0 && currentSection === node.key
    return <div className={`sheet-map-section${isCurrent ? ' is-current' : ''}`} key={node.key} data-depth={depth}>
      <button type="button" className={`sheet-map-link${depth ? ' is-nested' : ''}`} onClick={() => navigateToNode(node)}
        aria-current={isCurrent ? 'location' : undefined}
        style={{ paddingInlineStart: `calc(4px + ${depth} * 12px)` }}>
        <span>{node.name}</span><small>{counts.solved}/{counts.total}</small>
      </button>
      {depth === 0 && <div className="sheet-map-meter"><span style={{ width: `${counts.percentage}%` }} /></div>}
      {node.children.length > 0 && <div className="sheet-map-topics">{node.children.map((child) => renderMapNode(child, depth + 1))}</div>}
    </div>
  }

  return (
    <div className="page-stack sheet-detail-page">
      {loading && <div className="state-panel" role="status">Loading sheet…</div>}
      {!loading && error && <div className="state-panel state-error" role="alert"><p>{error}</p><Link className="button button-secondary" to="/sheets">Back to sheets</Link><button className="button button-quiet" type="button" onClick={() => { setLoading(true); setError(''); loadSheet() }}>Retry</button></div>}
      {!loading && !error && sheet && (
        <>
          <div className="breadcrumb"><Link to="/sheets">Sheets</Link><span aria-hidden="true">/</span><span>{sheet.name}</span></div>
          <section className="detail-heading sheet-detail-heading">
            <div className="sheet-title-block"><p className="eyebrow">Practice sheet</p><h1>{sheet.name}</h1>
              <p className="muted-text">{problems.length} {problems.length === 1 ? 'problem' : 'problem appearances'} <span aria-hidden="true">·</span> {solvedCount} solved</p>
            </div>
            <div className="detail-progress sheet-progress"><strong>{Math.round(percentage)}%</strong><ProgressBar value={percentage} label={`${sheet.name} completion`} /><span className="sheet-progress-caption">{solvedCount} / {problems.length} solved</span></div>
          </section>

          <div className={`sheet-workspace${sheetLayout.hierarchical ? ' has-map' : ''}`}>
            {sheetLayout.hierarchical && <aside className={`sheet-map${mapCollapsed ? ' is-collapsed' : ''}`} aria-label="Sheet section map">
              <button className="sheet-panel-toggle sheet-map-toggle" type="button" aria-expanded={!mapCollapsed}
                onClick={() => setMapCollapsed((collapsed) => !collapsed)}>
                <span className="eyebrow">Sheet map</span><span className="sheet-panel-chevron" aria-hidden="true" />
              </button>
              <nav>
                {sectionBlocks.map((section) => renderMapNode(section))}
              </nav>
            </aside>}

          <section className="problem-list-section sheet-problem-section">
            <div className="list-toolbar">
              <SearchBar value={search} onChange={setSearch} label="Search problems in this sheet" placeholder="Search problems…" />
              <label className="filter-select">Status
                <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
                  <option value="all">All</option><option value="unsolved">Unsolved</option><option value="solved">Solved</option>
                </select>
              </label>
              {difficulties.length > 0 && <label className="filter-select">Difficulty
                <select value={difficultyFilter} onChange={(event) => setDifficultyFilter(event.target.value)}>
                  <option value="all">All levels</option>{difficulties.map((difficulty) => <option key={difficulty}>{difficulty}</option>)}
                </select>
              </label>}
            </div>
            {problems.length === 0
              ? <div className="state-panel">This sheet does not contain any active problems.</div>
              : visibleProblemCount === 0
                ? <div className="state-panel">No problems match the current search and filters.</div>
                : hierarchyContent}
          </section>
          <aside className={`sheet-study-panel${progressCollapsed ? ' is-collapsed' : ''}`} aria-label="Sheet progress">
            <button className="sheet-panel-toggle sheet-progress-toggle" type="button" aria-expanded={!progressCollapsed}
              onClick={() => setProgressCollapsed((collapsed) => !collapsed)}>
              <span className="eyebrow">Your progress</span><span className="sheet-panel-chevron" aria-hidden="true" />
            </button>
            <div className="sheet-study-content">
            <section className="study-panel-block">
              <strong className="study-panel-total">{solvedCount}<span> / {problems.length}</span></strong>
              <div className="study-panel-progress"><ProgressBar value={percentage} label={`${percentage.toFixed(1)} percent complete`} /></div>
              <p className="study-panel-remaining">{Math.max(0, problems.length - solvedCount)} remaining</p>
            </section>
            {sheetLayout.hierarchical && currentSection && (() => {
              const active = sectionBlocks.find((block) => block.key === currentSection)
              if (!active) return null
              const counts = getCounts(sectionProblems(active))
              return <section className="study-panel-block">
                <p className="eyebrow">Current section</p><strong className="study-panel-section">{active.name}</strong>
                <p className="muted-text">{counts.solved} / {counts.total} solved</p>
                <ProgressBar value={counts.percentage} label={`${active.name} progress`} />
              </section>
            })()}
            {recentSolved.length > 0 && <section className="study-panel-block">
              <p className="eyebrow">Recently solved</p>
              <ul className="study-recent-list">{recentSolved.map((item) => <li key={item.problemId}><Link to={`/problems/${encodeURIComponent(item.problemId)}`}>{item.title}</Link></li>)}</ul>
            </section>}
            </div>
          </aside>
          </div>
        </>
      )}
    </div>
  )
}

export default SheetDetail
