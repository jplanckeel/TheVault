import { useState } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../AuthContext'
import { Input } from '../ui'
import Sidebar from './Sidebar'
import { MobileTabBar, MobileTopBar } from './MobileNav'

function HeaderSearch() {
  const [query, setQuery] = useState('')
  const navigate = useNavigate()
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        if (query.trim()) navigate(`/catalogue?q=${encodeURIComponent(query.trim())}`)
      }}
      className="flex-1 max-w-xl"
      role="search"
    >
      <Input leftIcon="search" placeholder="Rechercher dans le catalogue…" value={query} onChange={(e) => setQuery(e.target.value)} glass />
    </form>
  )
}

function UserAvatar() {
  const { user, isAdmin } = useAuth()
  if (!user) return null
  const name = user.name || user.email.split('@')[0]
  return (
    <div className="flex items-center gap-3 pl-2 pr-3 py-1" title={user.email}>
      <div className="h-9 w-9 rounded-full bg-gradient-to-br from-vault-primary to-vault-primary-deep flex items-center justify-center text-sm font-bold text-vault-on-primary">
        {name.charAt(0).toUpperCase()}
      </div>
      <div className="hidden lg:block leading-tight">
        <p className="text-sm font-semibold text-vault-on-surface">{name}</p>
        {isAdmin && <p className="text-[10px] font-bold tracking-widest uppercase text-vault-primary">Admin</p>}
      </div>
    </div>
  )
}

/** Application shell: sidebar + header on desktop, top bar + tab bar on small screens. */
export default function AppLayout() {
  return (
    <div className="min-h-screen bg-vault-bg">
      <Sidebar />
      <div className="md:ml-64 min-h-screen flex flex-col">
        <MobileTopBar />
        <header className="hidden md:flex sticky top-0 z-30 bg-vault-bg/85 backdrop-blur-md border-b border-vault-outline-variant/15 px-8 py-3 items-center justify-between gap-4">
          <HeaderSearch />
          <UserAvatar />
        </header>
        <main className="flex-1 w-full max-w-[1440px] mx-auto px-4 pt-6 pb-28 md:p-8">
          <Outlet />
        </main>
      </div>
      <MobileTabBar />
    </div>
  )
}
