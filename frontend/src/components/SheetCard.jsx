import { Link } from 'react-router-dom'
import ProgressBar from './ProgressBar.jsx'

function SheetCard({ sheet, problemCount, solvedCount }) {
  const percentage = problemCount ? (solvedCount / problemCount) * 100 : 0

  return (
    <article className="sheet-card">
      <div className="card-kicker">Practice sheet</div>
      <h2>{sheet.name}</h2>
      <p className="muted-text">
        {problemCount} {problemCount === 1 ? 'problem' : 'problems'}
      </p>
      <div className="sheet-card-progress">
        <div className="progress-caption">
          <span>{solvedCount} solved</span>
          <span>{Math.round(percentage)}%</span>
        </div>
        <ProgressBar value={percentage} label={`${sheet.name} completion`} />
      </div>
      <Link className="button button-secondary" to={`/sheets/${encodeURIComponent(sheet.sheetId)}`}>
        Open sheet <span aria-hidden="true">→</span>
      </Link>
    </article>
  )
}

export default SheetCard
