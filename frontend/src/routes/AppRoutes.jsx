import { Route, Routes } from 'react-router-dom'
import AppLayout from '../layouts/AppLayout.jsx'
import Dashboard from '../pages/Dashboard.jsx'
import ProblemDetail from '../pages/ProblemDetail.jsx'
import SheetDetail from '../pages/SheetDetail.jsx'
import Sheets from '../pages/Sheets.jsx'

function PlaceholderPage({ title }) {
  return <div className="page-stack"><p className="eyebrow">DSA Hub</p><h1>{title}</h1><div className="state-panel">This page will be added in a later step.</div></div>
}

function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/sheets" element={<Sheets />} />
        <Route path="/sheets/:sheetId" element={<SheetDetail />} />
        <Route path="/problems/:problemId" element={<ProblemDetail />} />
        <Route path="/login" element={<PlaceholderPage title="Login" />} />
        <Route path="/register" element={<PlaceholderPage title="Register" />} />
        <Route path="/settings" element={<PlaceholderPage title="Settings" />} />
        <Route path="/account" element={<PlaceholderPage title="Account" />} />
        <Route path="*" element={<PlaceholderPage title="Page not found" />} />
      </Route>
    </Routes>
  )
}

export default AppRoutes
