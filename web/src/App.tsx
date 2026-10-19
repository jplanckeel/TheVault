import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import { AuthProvider, useAuth } from './AuthContext'
import { ToastProvider } from './components/ui'
import AppLayout from './components/layout/AppLayout'
import CataloguePage from './pages/CataloguePage'
import ConsoleDetailPage from './pages/ConsoleDetailPage'
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

function RequireAuth({ children, fallback = '/login' }: { children: ReactNode; fallback?: string }) {
  const { user } = useAuth()
  return user ? <>{children}</> : <Navigate to={fallback} replace />
}

function RequireAdmin({ children }: { children: ReactNode }) {
  const { isAdmin } = useAuth()
  return isAdmin ? <>{children}</> : <Navigate to="/admin" replace />
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
            <Route path="/admin" element={<AdminLoginPage />} />
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
              <Route path="jobs" element={<RequireAdmin><JobsPage /></RequireAdmin>} />
              <Route path="*" element={<NotFound />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  )
}
