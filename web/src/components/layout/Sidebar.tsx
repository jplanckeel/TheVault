import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../../AuthContext'
import { Logo, MaterialIcon } from '../ui'
import ThemePicker from './ThemePicker'
import { ADMIN_LINKS, EXPLORE_LINKS, USER_LINKS, type NavItem } from './navigation'

function SideLink({ item }: { item: NavItem }) {
  return (
    <NavLink to={item.to} end={item.end} className={({ isActive }) => `tv-nav ${isActive ? 'is-active' : ''}`}>
      {({ isActive }) => (
        <>
          <MaterialIcon name={item.icon} size={22} filled={isActive} />
          <span>{item.label}</span>
          {isActive && <span className="tv-nav-led" aria-hidden="true" />}
        </>
      )}
    </NavLink>
  )
}

/** Desktop navigation (≥ 768 px), sitting on the shell like the console's top plate. */
export default function Sidebar() {
  const { user, isAdmin, logout } = useAuth()
  const name = user ? user.name || user.email.split('@')[0] : ''
  return (
    <aside className="hidden md:flex fixed inset-y-0 left-0 w-64 flex-col gap-6 px-4 py-6 bg-shell border-r border-line z-40">
      <Link to="/" className="px-2" aria-label="TheVault, accueil">
        <Logo size={34} tagline />
      </Link>

      {user && (
        <Link to="/add" className="tv-btn tv-btn-md tv-btn-primary tv-btn-block">
          <MaterialIcon name="add" size={22} />
          Ajouter
        </Link>
      )}

      <nav className="flex flex-col gap-1 flex-1 overflow-y-auto" aria-label="Navigation principale">
        {user && USER_LINKS.map((item) => <SideLink key={item.to} item={item} />)}
        {EXPLORE_LINKS.map((item) => <SideLink key={item.to} item={item} />)}
        {isAdmin && (
          <>
            <p className="tv-engrave px-3 mt-5 mb-2">Back-office</p>
            {ADMIN_LINKS.map((item) => <SideLink key={item.to} item={item} />)}
          </>
        )}
      </nav>

      <ThemePicker />

      {user ? (
        <div className="flex items-center gap-3 px-1">
          <span className="h-10 w-10 rounded-md bg-lavender text-on-lavender flex items-center justify-center font-display font-extrabold" aria-hidden="true">
            {name.charAt(0).toUpperCase()}
          </span>
          <span className="flex-1 min-w-0">
            <span className="block text-[14px] font-semibold truncate">{name}</span>
            <span className="block tv-serial truncate">{isAdmin ? 'ADMIN · ' : ''}{user.email}</span>
          </span>
          <button type="button" onClick={logout} className="tv-iconbtn tv-iconbtn-sm tv-iconbtn-ghost" aria-label="Se déconnecter" title="Se déconnecter">
            <MaterialIcon name="logout" size={20} />
          </button>
        </div>
      ) : (
        <div className="flex flex-col gap-2">
          <Link to="/login" className="tv-btn tv-btn-md tv-btn-secondary tv-btn-block">
            Se connecter
          </Link>
          <Link to="/register" className="tv-link text-center">
            Créer un compte
          </Link>
        </div>
      )}
    </aside>
  )
}

