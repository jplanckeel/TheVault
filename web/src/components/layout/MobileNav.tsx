import { useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../../AuthContext'
import { IconButton, Logo, MaterialIcon } from '../ui'
import ThemePicker from './ThemePicker'
import { ADMIN_LINKS, EXPLORE_LINKS, USER_LINKS, type NavItem } from './navigation'

/** Compact top bar below 768 px: logo, then a menu key (shell, back-office, account). */
export function MobileTopBar() {
  const { user, isAdmin, logout } = useAuth()
  const [open, setOpen] = useState(false)
  return (
    <div className="md:hidden sticky top-0 z-30 bg-shell/95 backdrop-blur">
      <div className="flex items-center justify-between px-4 py-3">
        <Link to="/" aria-label="TheVault, accueil">
          <Logo size={28} />
        </Link>
        <IconButton icon={open ? 'close' : 'menu'} aria-label={open ? 'Fermer le menu' : 'Ouvrir le menu'} aria-expanded={open} onClick={() => setOpen(!open)} />
      </div>
      {open && (
        <div className="px-4 pb-4">
          <div className="tv-panel flex flex-col gap-3" onClick={(e) => (e.target as HTMLElement).closest('a') && setOpen(false)}>
            {!user && (
              <Link to="/login" className="tv-btn tv-btn-md tv-btn-primary tv-btn-block">
                Se connecter
              </Link>
            )}
            {isAdmin && (
              <div className="flex flex-col gap-1">
                <p className="tv-engrave px-3">Back-office</p>
                {ADMIN_LINKS.map((l) => (
                  <NavLink key={l.to} to={l.to} className={({ isActive }) => `tv-nav ${isActive ? 'is-active' : ''}`}>
                    <MaterialIcon name={l.icon} size={22} />
                    {l.label}
                  </NavLink>
                ))}
              </div>
            )}
            <ThemePicker />
            {user && (
              <button type="button" onClick={logout} className="tv-btn tv-btn-sm tv-btn-ghost self-start">
                <MaterialIcon name="logout" size={18} />
                Se déconnecter ({user.email})
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

function Tab({ item }: { item: NavItem }) {
  return (
    <NavLink to={item.to} end={item.end} className={({ isActive }) => `tv-tab ${isActive ? 'is-active' : ''}`}>
      {({ isActive }) => (
        <>
          <MaterialIcon name={item.icon} size={24} filled={isActive} />
          <span>{item.label}</span>
          <span className="tv-tab-led" aria-hidden="true" />
        </>
      )}
    </NavLink>
  )
}

/** Floating tab bar below 768 px: Accueil · Collection · + · Envies · Explorer. */
export function MobileTabBar() {
  const { user } = useAuth()
  if (!user) return null
  const [home, collection, wishlist] = USER_LINKS
  return (
    <nav aria-label="Navigation principale" className="md:hidden fixed inset-x-3.5 bottom-4 z-40 tv-tabbar">
      <Tab item={home} />
      <Tab item={collection} />
      <Link to="/add" className="tv-face" style={{ width: 52, height: 52 }} aria-label="Ajouter un objet">
        <MaterialIcon name="add" size={28} />
      </Link>
      <Tab item={wishlist} />
      <Tab item={EXPLORE_LINKS[0]} />
    </nav>
  )
}
