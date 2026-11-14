import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import { AuthProvider, useAuth } from './AuthContext'
import { ToastProvider } from './components/ui'
import AppLayout from './components/layout/AppLayout'
import CataloguePage from './pages/CataloguePage'
import ConsoleDetailPage from './pages/ConsoleDetailPage'
import AccessoryDetailPage from './pages/AccessoryDetailPage'
import ItemDetailPage from './pages/ItemDetailPage'
import AdminDataPage from './pages/admin/AdminDataPage'
import AdminSyncPage from './pages/admin/AdminSyncPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import SharePage from './pages/SharePage'
import DashboardPage from './pages/DashboardPage'
import CollectionPage from './pages/CollectionPage'
import WishlistPage from './pages/WishlistPage'
import AddItemPage from './pages/AddItemPage'
import { EmptyState } from './components/ui'

function RequireAuth({ children, fallback = '/login' }: { children: ReactNode; fallback?: string }) {
  const { user } = useAuth()
  return user ? <>{children}</> : <Navigate to={fallback} replace />
}

function RequireAdmin({ children }: { children: ReactNode }) {
  const { user, isAdmin } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (!isAdmin) {
    return (
      <EmptyState
        icon="shield_lock"
        title="Accès réservé aux administrateurs"
        description="Votre compte n'a pas le rôle admin. Il est attribué via la variable ADMIN_EMAILS du backend."
      />
    )
  }
  return <>{children}</>
}

function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center h-96 text-center">
      <p className="text-display-lg text-vault-primary">404</p>
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
            <Route path="/share/:token" element={<SharePage />} />
            <Route path="/" element={<AppLayout />}>
              <Route index element={<RequireAuth fallback="/catalogue"><DashboardPage /></RequireAuth>} />
              <Route path="collection" element={<RequireAuth><CollectionPage /></RequireAuth>} />
              <Route path="wishlist" element={<RequireAuth><WishlistPage /></RequireAuth>} />
              <Route path="add" element={<RequireAuth><AddItemPage /></RequireAuth>} />
              <Route path="catalogue" element={<CataloguePage />} />
              <Route path="games" element={<Navigate to="/catalogue?type=game" replace />} />
              <Route path="consoles" element={<Navigate to="/catalogue?type=console" replace />} />
              <Route path="consoles/:id" element={<ConsoleDetailPage />} />
              <Route path="accessories" element={<Navigate to="/catalogue?type=accessory" replace />} />
              <Route path="accessories/:id" element={<AccessoryDetailPage />} />
              <Route path="items/:id" element={<ItemDetailPage />} />
              <Route path="admin" element={<Navigate to="/admin/data" replace />} />
              <Route path="admin/data" element={<RequireAdmin><AdminDataPage /></RequireAdmin>} />
              <Route path="admin/sync" element={<RequireAdmin><AdminSyncPage /></RequireAdmin>} />
              <Route path="jobs" element={<Navigate to="/admin/sync" replace />} />
              <Route path="*" element={<NotFound />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  )
}
