import { BrowserRouter, NavLink, Navigate, Outlet, Route, Routes, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { AuthProvider, useAuth } from './AuthContext'
import { Button, IconButton, Input, MaterialIcon, ToastProvider } from './components/ui'
import CataloguePage from './pages/CataloguePage'
import ConsolesPage from './pages/ConsolesPage'
import ConsoleDetailPage from './pages/ConsoleDetailPage'
import AccessoriesPage from './pages/AccessoriesPage'
import AccessoryDetailPage from './pages/AccessoryDetailPage'
import ItemDetailPage from './pages/ItemDetailPage'
import JobsPage from './pages/JobsPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import CollectionPage from './pages/CollectionPage'
import WishlistPage from './pages/WishlistPage'
import AddItemPage from './pages/AddItemPage'
import AdminLoginPage from './pages/AdminLoginPage'
import GamesPage from './pages/GamesPage'

const LOGO_SVG = `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg"><defs><linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" style="stop-color:#d8b9ff"/><stop offset="100%" style="stop-color:#7e28df"/></linearGradient></defs><path d="M20 30 L50 15 L80 30 L80 70 L50 85 L20 70 Z" fill="none" stroke="url(#g)" stroke-width="6" stroke-linejoin="round"/><path d="M35 45 L50 35 L65 45 L65 60 L50 70 L35 60 Z" fill="url(#g)"/><rect x="47" y="52" width="6" height="10" rx="3" fill="#fff" opacity="0.85"/></svg>`

interface NavItem {
  to: string
  icon: string
  label: string
  end?: boolean
}

function NavLinkItem({ item }: { item: NavItem }) {
  return (
    <NavLink
      to={item.to}
      end={item.end}
      className={({ isActive }) =>
        [
          'group relative flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-semibold transition-colors focus-ring',
          isActive
            ? 'bg-vault-primary/10 text-vault-primary'
            : 'text-vault-on-surface-variant hover:bg-vault-surface-low hover:text-vault-on-surface',
        ].join(' ')
      }
    >
      {({ isActive }) => (
        <>
          {isActive && (
            <span className="absolute left-0 top-2 bottom-2 w-0.5 rounded-full bg-vault-primary glow-purple" />
          )}
          <MaterialIcon name={item.icon} size={20} filled={isActive} />
          <span>{item.label}</span>
        </>
      )}
    </NavLink>
  )
}

function Sidebar() {
  const { user, isAdmin, logout } = useAuth()
  const navigate = useNavigate()

  const userLinks: NavItem[] = [
    { to: '/', icon: 'dashboard', label: 'Dashboard', end: true },
    { to: '/collection', icon: 'inventory_2', label: 'Collection' },
    { to: '/wishlist', icon: 'favorite', label: 'Wishlist' },
  ]

  const exploreLinks: NavItem[] = [
    { to: '/games', icon: 'stadia_controller', label: 'Games' },
    { to: '/consoles', icon: 'sports_esports', label: 'Consoles' },
    { to: '/accessories', icon: 'headphones', label: 'Accessoires' },
  ]

  const adminLinks: NavItem[] = [{ to: '/jobs', icon: 'tune', label: 'Vault Analysis' }]

  return (
    <aside className="hidden md:flex fixed top-0 left-0 h-screen w-64 flex-col bg-vault-surface-lowest border-r border-vault-outline-variant/10 z-40">
      {/* Logo block */}
      <div className="px-6 pt-6 pb-8 flex items-center gap-3">
        <div className="w-10 h-10 shrink-0" dangerouslySetInnerHTML={{ __html: LOGO_SVG }} />
        <div className="min-w-0">
          <h1 className="font-display text-lg font-extrabold text-vault-on-surface leading-tight">TheVault</h1>
          <p className="text-[10px] font-bold tracking-[0.18em] text-vault-primary uppercase">Premium Security</p>
        </div>
      </div>

      {/* Nav */}
      <nav className="flex flex-col gap-1 px-3 flex-1 overflow-y-auto">
        {user && (
          <>
            {userLinks.map((item) => (
              <NavLinkItem key={item.to} item={item} />
            ))}
            <div className="h-px bg-vault-outline-variant/30 my-3" />
          </>
        )}

        <p className="text-label-bold text-vault-on-surface-variant px-3 mb-2 mt-1">Explorer</p>
        {exploreLinks.map((item) => (
          <NavLinkItem key={item.to} item={item} />
        ))}

        {isAdmin && (
          <>
            <div className="h-px bg-vault-outline-variant/30 my-3" />
            <p className="text-label-bold text-vault-on-surface-variant px-3 mb-2">Admin</p>
            {adminLinks.map((item) => (
              <NavLinkItem key={item.to} item={item} />
            ))}
          </>
        )}
      </nav>

      {/* Add Item CTA */}
      {user && (
        <div className="px-4 pb-3">
          <Button leftIcon="add" fullWidth size="lg" onClick={() => navigate('/add')}>
            Add Item
          </Button>
        </div>
      )}

      {/* Footer */}
      <div className="px-3 pb-4 border-t border-vault-outline-variant/20 pt-3">
        {user ? (
          <>
            {!isAdmin && (
              <NavLink
                to="/admin"
                className="flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-vault-on-surface-variant hover:bg-vault-surface-low hover:text-vault-on-surface transition-colors focus-ring"
              >
                <MaterialIcon name="shield_lock" size={20} />
                <span>Admin</span>
              </NavLink>
            )}
            <NavLink
              to="/support"
              className="flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-vault-on-surface-variant hover:bg-vault-surface-low hover:text-vault-on-surface transition-colors focus-ring"
            >
              <MaterialIcon name="help" size={20} />
              <span>Support</span>
            </NavLink>
            <button
              onClick={logout}
              className="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-vault-on-surface-variant hover:bg-vault-error/10 hover:text-vault-error transition-colors focus-ring"
            >
              <MaterialIcon name="logout" size={20} />
              <span>Sign Out</span>
            </button>
          </>
        ) : (
          <div className="flex flex-col gap-2 px-1">
            <NavLink
              to="/login"
              className="text-sm font-semibold text-vault-primary hover:text-vault-primary-dim transition-colors px-2 py-1.5"
            >
              Se connecter →
            </NavLink>
            <NavLink
              to="/register"
              className="text-xs text-vault-on-surface-variant hover:text-vault-on-surface transition-colors px-2 py-1"
            >
              Créer un compte
            </NavLink>
          </div>
        )}
      </div>
    </aside>
  )
}

function HeaderSearch() {
  const [query, setQuery] = useState('')
  const navigate = useNavigate()

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (query.trim()) navigate(`/catalogue?q=${encodeURIComponent(query.trim())}`)
  }

  return (
    <form onSubmit={handleSubmit} className="flex-1 max-w-xl">
      <Input
        leftIcon="search"
        placeholder="Search your vault..."
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        glass
        size="md"
      />
    </form>
  )
}

function UserAvatar() {
  const { user } = useAuth()
  if (!user) return null
  const initial = (user.name || user.email || '?').charAt(0).toUpperCase()
  return (
    <button
      className="flex items-center gap-3 pl-2 pr-3 py-1 rounded-full hover:bg-vault-surface-low transition-colors focus-ring"
      title={user.email}
    >
      <div className="h-9 w-9 rounded-full bg-gradient-to-br from-vault-primary to-vault-primary-deep flex items-center justify-center text-sm font-bold text-vault-on-primary glow-purple">
        {initial}
      </div>
      <span className="hidden lg:inline text-sm font-semibold text-vault-on-surface">
        {user.name || user.email.split('@')[0]}
      </span>
    </button>
  )
}

function MobileTopBar() {
  const navigate = useNavigate()
  return (
    <div className="md:hidden flex items-center justify-between px-4 py-3 border-b border-vault-outline-variant/20 bg-vault-surface-lowest">
      <div className="flex items-center gap-2">
        <div className="w-8 h-8" dangerouslySetInnerHTML={{ __html: LOGO_SVG }} />
        <span className="font-display font-extrabold text-vault-on-surface">TheVault</span>
      </div>
      <IconButton icon="add" onClick={() => navigate('/add')} variant="subtle" />
    </div>
  )
}

function Layout() {
  return (
    <div className="min-h-screen bg-vault-bg">
      <Sidebar />
      <div className="md:ml-64 min-h-screen flex flex-col">
        <MobileTopBar />
        <header className="hidden md:flex sticky top-0 z-30 bg-vault-bg/85 backdrop-blur-md border-b border-vault-outline-variant/15 px-8 py-3 items-center gap-4">
          <HeaderSearch />
          <div className="flex items-center gap-2">
            <IconButton icon="notifications" badge aria-label="Notifications" />
            <IconButton icon="settings" aria-label="Settings" />
            <UserAvatar />
          </div>
        </header>
        <main className="flex-1 p-6 md:p-8 max-w-[1440px] w-full mx-auto">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

function RequireAuth({ children, fallback }: { children: React.ReactNode; fallback?: string }) {
  const { user } = useAuth()
  if (!user) return <Navigate to={fallback || '/login'} replace />
  return <>{children}</>
}

function RequireAdmin({ children }: { children: React.ReactNode }) {
  const { isAdmin } = useAuth()
  if (!isAdmin) return <Navigate to="/login" replace />
  return <>{children}</>
}

function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center h-96 text-center">
      <p className="text-display-lg text-gradient-primary">404</p>
      <p className="text-vault-on-surface-variant mt-2">Page introuvable</p>
    </div>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/admin" element={<AdminLoginPage />} />
            <Route path="/" element={<Layout />}>
              <Route index element={<RequireAuth fallback="/games"><DashboardPage /></RequireAuth>} />
              <Route path="collection" element={<RequireAuth><CollectionPage /></RequireAuth>} />
              <Route path="wishlist" element={<RequireAuth><WishlistPage /></RequireAuth>} />
              <Route path="add" element={<RequireAuth><AddItemPage /></RequireAuth>} />
              <Route path="catalogue" element={<CataloguePage />} />
              <Route path="games" element={<GamesPage />} />
              <Route path="consoles" element={<ConsolesPage />} />
              <Route path="consoles/:id" element={<ConsoleDetailPage />} />
              <Route path="accessories" element={<AccessoriesPage />} />
              <Route path="accessories/:id" element={<AccessoryDetailPage />} />
              <Route path="items/:id" element={<ItemDetailPage />} />
              <Route path="jobs" element={<RequireAdmin><JobsPage /></RequireAdmin>} />
              <Route path="*" element={<NotFound />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  )
}
