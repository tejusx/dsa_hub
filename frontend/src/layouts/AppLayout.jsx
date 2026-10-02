import { Outlet } from 'react-router-dom'
import Navbar from '../components/Navbar.jsx'

function AppLayout() {
  return (
    <div className="app-shell">
      <Navbar />
      <main className="page-shell page-content"><Outlet /></main>
      <footer className="page-shell site-footer">DSA Hub <span>·</span> Practice with purpose.</footer>
    </div>
  )
}

export default AppLayout
