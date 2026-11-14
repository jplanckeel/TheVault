import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../AuthContext'
import { MaterialIcon } from '../ui'
import Logo from './Logo'
import { EXPLORE_LINKS, USER_LINKS, type NavItem } from './navigation'

/** Compact top bar below 768 px. */
export function MobileTopBar() {
  const { user, isAdmin, logout } = useAuth()
  return (
    <div className="md:hidden sticky top-0 z-30 flex items-center justify-between px-4 py-3 bg-vault-bg/95 border-b border-vault-outline-variant/20">
      <div className="flex items-center gap-2">
        <Logo className="w-8 h-8" />
        <span className="font-display font-extrabold text-vault-on-surface">TheVault</span>
      </div>
      {user ? (
        <div className="flex items-center gap-1">
          {isAdmin && (
            <NavLink to="/admin/data" aria-label="Back-office" className="p-2 rounded-lg text-vault-primary focus-ring">
              <MaterialIcon name="shield_person" size={20} />
            </NavLink>
          )}
          <button onClick={logout} aria-label="Se déconnecter" className="p-2 rounded-lg text-vault-on-surface-variant focus-ring">
            <MaterialIcon name="logout" size={20} />
          </button>
        </div>
      ) : (
        <NavLink to="/login" className="text-sm font-semibold text-vault-primary focus-ring rounded px-2 py-1">
          Se connecter
        </NavLink>
      )}
    </div>
  )
}

function TabLink({ item }: { item: NavItem }) {
  return (
    <NavLink
      to={item.to}
      end={item.end}
      className={({ isActive }) =>
        `flex flex-1 flex-col items-center gap-0.5 py-1.5 rounded-lg text-[10px] font-bold tracking-wide focus-ring ${
          isActive ? 'text-vault-primary' : 'text-vault-on-surface-variant'
        }`
      }
    >
      {({ isActive }) => (
        <>
          <MaterialIcon name={item.icon} size={24} filled={isActive} />
          <span className="truncate max-w-full">{item.label}</span>
        </>
      )}
    </NavLink>
  )
}

/** Bottom tab bar below 768 px — same layout as the mobile app (4 tabs + central add button). */
export function MobileTabBar() {
  const { user } = useAuth()
  const navigate = useNavigate()
  if (!user) return null
  const [dashboard, collection, wishlist] = USER_LINKS
  return (
    <nav
      aria-label="Navigation principale"
      className="md:hidden fixed inset-x-0 bottom-0 z-40 flex items-center px-2 h-[68px] bg-vault-surface-lowest/95 border-t border-vault-outline-variant/30"
    >
      <TabLink item={dashboard} />
      <TabLink item={{ ...collection, label: 'Collection' }} />
      <div className="flex flex-1 justify-center">
        <button
          onClick={() => navigate('/add')}
          aria-label="Ajouter un objet"
          className="h-[52px] w-[52px] rounded-full bg-vault-primary text-vault-on-primary flex items-center justify-center focus-ring"
        >
          <MaterialIcon name="add" size={28} />
        </button>
      </div>
      <TabLink item={wishlist} />
      <TabLink item={EXPLORE_LINKS[0]} />
    </nav>
  )
}
