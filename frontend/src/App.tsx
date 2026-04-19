import { BrowserRouter, NavLink, Outlet, Route, Routes } from 'react-router-dom'
import CataloguePage from './pages/CataloguePage'
import HomePage from './pages/HomePage'
import ItemDetailPage from './pages/ItemDetailPage'
import JobsPage from './pages/JobsPage'

function Sidebar() {
  return (
    <aside className="fixed top-0 left-0 h-screen w-64 flex flex-col bg-vault-surface-low border-r border-vault-outline-variant z-10">
      <div className="px-6 py-6 border-b border-vault-outline-variant">
        <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-1">Backoffice</p>
        <h1 className="text-lg font-bold text-vault-primary">TheVaultBO</h1>
      </div>
      <nav className="flex flex-col gap-1 px-3 py-4 flex-1">
        {[
          { to: '/',          end: true,  icon: '🎮', label: 'Collection'  },
          { to: '/catalogue', end: false, icon: '📦', label: 'Catalogue'   },
          { to: '/jobs',      end: false, icon: '⚙️', label: 'Jobs'        },
        ].map(({ to, end, icon, label }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-vault-surface text-vault-primary'
                  : 'text-vault-on-surface-variant hover:bg-vault-surface hover:text-vault-on-surface'
              }`
            }
          >
            <span>{icon}</span>
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="px-6 py-4 border-t border-vault-outline-variant">
        <p className="text-xs text-vault-on-surface-variant">The Vault — Admin</p>
      </div>
    </aside>
  )
}

function Layout() {
  return (
    <div className="min-h-screen bg-vault-bg">
      <Sidebar />
      <main className="ml-64 min-h-screen p-8">
        <Outlet />
      </main>
    </div>
  )
}

function NotFound() {
  return (
    <div className="flex items-center justify-center h-64">
      <p className="text-vault-on-surface-variant text-lg">404 — Page not found</p>
    </div>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="catalogue" element={<CataloguePage />} />
          <Route path="items/:id" element={<ItemDetailPage />} />
          <Route path="jobs" element={<JobsPage />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
