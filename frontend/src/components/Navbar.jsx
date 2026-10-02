import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import SheetSwitcher from './SheetSwitcher.jsx'

function Navbar() {
  const { user, loading, logout, isAuthenticated } = useAuth()
  const mainLinks = [
    { to: '/dashboard', label: 'Dashboard' },
    { to: '/sheets', label: 'Sheets' },
  ]

  function renderLinks(onNavigate) {
    return mainLinks.map((item) => (
      <NavLink key={item.to} to={item.to} onClick={onNavigate}>
        {item.label}
      </NavLink>
    ))
  }

  return (
    <header className="site-header">
      <div className="navbar page-shell">
        <Link className="brand" to="/" aria-label="DSA Hub home">
          <span className="brand-mark" aria-hidden="true">D</span>
          <span>DSA <strong>Hub</strong></span>
        </Link>

        <nav className="desktop-nav" aria-label="Main navigation">
          {renderLinks()}
        </nav>

        <SheetSwitcher />

        <div className="navbar-actions">
          <Link className="search-shortcut" to="/sheets">
            <span aria-hidden="true">⌕</span> Search problems <kbd>/</kbd>
          </Link>
          {!loading && (isAuthenticated
            ? <div className="auth-actions"><Link to="/account">{user?.displayName || 'Account'}</Link><Link to="/settings">Settings</Link><button className="text-button" type="button" onClick={logout}>Log out</button></div>
            : <div className="auth-actions"><Link to="/login">Log in</Link><Link className="button button-small" to="/register">Create account</Link></div>)}
        </div>

        <details className="mobile-nav">
          <summary aria-label="Open navigation menu"><span /><span /><span /></summary>
          <nav aria-label="Mobile navigation">
            {renderLinks()}
            <Link to="/sheets">Search problems</Link>
            {!loading && (isAuthenticated
              ? <><Link to="/account">{user?.displayName || 'Account'}</Link><Link to="/settings">Settings</Link><button className="text-button" type="button" onClick={logout}>Log out</button></>
              : <><Link to="/login">Log in</Link><Link to="/register">Create account</Link></>)}
          </nav>
        </details>
      </div>
    </header>
  )
}

export default Navbar
