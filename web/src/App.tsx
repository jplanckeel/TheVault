import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { lazy, Suspense, type ReactNode } from 'react'
import { AuthProvider, useAuth } from './AuthContext'
import { ToastProvider } from './components/ui'
import AppLayout from './components/layout/AppLayout'
const CataloguePage = lazy(() => import('./pages/CataloguePage'))
const ItemDetailPage = lazy(() => import('./pages/ItemDetailPage'))
const AdminDataPage = lazy(() => import('./pages/admin/AdminDataPage'))
const AdminSyncPage = lazy(() => import('./pages/admin/AdminSyncPage'))
const LoginPage = lazy(() => import('./pages/LoginPage'))
const RegisterPage = lazy(() => import('./pages/RegisterPage'))
const SharePage = lazy(() => import('./pages/SharePage'))
const DashboardPage = lazy(() => import('./pages/DashboardPage'))
const CollectionPage = lazy(() => import('./pages/CollectionPage'))
const WishlistPage = lazy(() => import('./pages/WishlistPage'))
const AddItemPage = lazy(() => import('./pages/AddItemPage'))
import { EmptyState } from './components/ui'

/** Shown while a page's code loads (each page is its own chunk). */
function PageLoading() {
  return <div className="min-h-[50vh]" aria-busy="true" />
}

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
        title="Accès réservé aux administrateurs"
        description="Ton compte n'a pas le rôle admin. Il est attribué par la variable ADMIN_EMAILS du backend."
      />
    )
  }
  return <>{children}</>
}

function NotFound() {
  return (
    <EmptyState title="Page introuvable" description="Cette adresse ne mène nulle part. Reviens à l'accueil." />
  )
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <Suspense fallback={<PageLoading />}>
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
              <Route path="consoles/:id" element={<ItemDetailPage />} />
              <Route path="accessories" element={<Navigate to="/catalogue?type=accessory" replace />} />
              <Route path="accessories/:id" element={<ItemDetailPage />} />
              <Route path="items/:id" element={<ItemDetailPage />} />
              <Route path="admin" element={<Navigate to="/admin/data" replace />} />
              <Route path="admin/data" element={<RequireAdmin><AdminDataPage /></RequireAdmin>} />
              <Route path="admin/sync" element={<RequireAdmin><AdminSyncPage /></RequireAdmin>} />
              <Route path="jobs" element={<Navigate to="/admin/sync" replace />} />
              <Route path="*" element={<NotFound />} />
            </Route>
          </Routes>
          </Suspense>
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  )
}
