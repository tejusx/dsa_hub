function ProgressBar({ value = 0, label = 'Progress' }) {
  const percentage = Math.min(100, Math.max(0, Number(value) || 0))

  return (
    <div
      className="progress-track"
      role="progressbar"
      aria-label={label}
      aria-valuemin="0"
      aria-valuemax="100"
      aria-valuenow={Math.round(percentage)}
    >
      <span className="progress-fill" style={{ width: `${percentage}%` }} />
    </div>
  )
}

export default ProgressBar
