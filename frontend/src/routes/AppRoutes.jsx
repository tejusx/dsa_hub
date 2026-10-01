import { Route, Routes } from 'react-router-dom'

const pageHeadings = {
  dashboard: 'Dashboard',
  sheets: 'Sheets',
  sheet: 'Sheet',
  problem: 'Problem',
  login: 'Login',
  register: 'Register',
  settings: 'Settings',
  account: 'Account',
}

function PlaceholderPage({ title }) {
  return <main><h1>{title}</h1></main>
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<PlaceholderPage title={pageHeadings.dashboard} />} />
      <Route path="/dashboard" element={<PlaceholderPage title={pageHeadings.dashboard} />} />
      <Route path="/sheets" element={<PlaceholderPage title={pageHeadings.sheets} />} />
      <Route path="/sheets/:sheetId" element={<PlaceholderPage title={pageHeadings.sheet} />} />
      <Route path="/problems/:problemId" element={<PlaceholderPage title={pageHeadings.problem} />} />
      <Route path="/login" element={<PlaceholderPage title={pageHeadings.login} />} />
      <Route path="/register" element={<PlaceholderPage title={pageHeadings.register} />} />
      <Route path="/settings" element={<PlaceholderPage title={pageHeadings.settings} />} />
      <Route path="/account" element={<PlaceholderPage title={pageHeadings.account} />} />
      <Route path="*" element={<PlaceholderPage title="Page not found" />} />
    </Routes>
  )
}

export default AppRoutes
