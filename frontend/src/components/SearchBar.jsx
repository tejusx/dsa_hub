function SearchBar({ value, onChange, placeholder = 'Search', label = 'Search' }) {
  return (
    <label className="search-field">
      <span className="search-icon" aria-hidden="true">⌕</span>
      <span className="visually-hidden">{label}</span>
      <input
        type="search"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
      />
    </label>
  )
}

export default SearchBar
