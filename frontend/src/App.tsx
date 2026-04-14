import { BrowserRouter, Link, Outlet, Route, Routes } from 'react-router-dom'
import CataloguePage from './pages/CataloguePage'
import ItemDetailPage from './pages/ItemDetailPage'

function Layout() {
  return (
    <div style={{ fontFamily: 'sans-serif' }}>
      <header style={{ padding: '1rem', borderBottom: '1px solid #e0e0e0', display: 'flex', gap: '1rem' }}>
        <strong>TheVaultBO</strong>
        <nav style={{ display: 'flex', gap: '1rem', marginLeft: '2rem' }}>
          <Link to="/">Catalogue</Link>
          <Link to="/jobs">Jobs</Link>
        </nav>
      </header>
      <main style={{ padding: '1rem' }}>
        <Outlet />
      </main>
    </div>
  )
}

function JobsPage() {
  return <h1>Jobs</h1>
}

function NotFound() {
  return <h1>404 — Page not found</h1>
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<CataloguePage />} />
          <Route path="items/:id" element={<ItemDetailPage />} />
          <Route path="jobs" element={<JobsPage />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
