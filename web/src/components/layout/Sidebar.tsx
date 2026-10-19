import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../AuthContext'
import { Button, MaterialIcon } from '../ui'
import Logo from './Logo'
import { ADMIN_LINKS, EXPLORE_LINKS, USER_LINKS, type NavItem } from './navigation'

function SidebarLink({ item }: { item: NavItem }) {
  return (
    <NavLink
      to={item.to}
      end={item.end}
      className={({ isActive }) =>
        [
          'relative flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-semibold transition-colors focus-ring',
          isActive
            ? 'bg-vault-primary/10 text-vault-primary'
            : 'text-vault-on-surface-variant hover:bg-vault-surface-low hover:text-vault-on-surface',
        ].join(' ')
      }
    >
      {({ isActive }) => (
        <>
          {isActive && <span className="absolute left-0 top-2 bottom-2 w-0.5 rounded-full bg-vault-primary" />}
          <MaterialIcon name={item.icon} size={20} filled={isActive} />
          <span>{item.label}</span>
        </>
      )}
    </NavLink>
  )
}

function GroupLabel({ children }: { children: string }) {
  return <p className="text-label-bold text-vault-on-surface-variant px-3 mb-2">{children}</p>
}

/** Desktop sidebar (≥ 768 px) — see design/DESIGN_SYSTEM.md §5. */
export default function Sidebar() {
  const { user, isAdmin, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <aside className="hidden md:flex fixed inset-y-0 left-0 w-64 flex-col bg-vault-surface-lowest border-r border-vault-outline-variant/15 z-40">
      <div className="px-6 pt-6 pb-8 flex items-center gap-3">
        <Logo />
        <div className="min-w-0">
          <p className="font-display text-lg font-extrabold text-vault-on-surface leading-tight">TheVault</p>
          <p className="text-[10px] font-bold tracking-[0.18em] text-vault-primary uppercase">Gaming collection</p>
        </div>
      </div>

      <nav className="flex flex-col gap-1 px-3 flex-1 overflow-y-auto" aria-label="Navigation principale">
        {user && USER_LINKS.map((item) => <SidebarLink key={item.to} item={item} />)}
        {user && <div className="h-px bg-vault-outline-variant/30 my-3" />}
        <GroupLabel>Explorer</GroupLabel>
        {EXPLORE_LINKS.map((item) => <SidebarLink key={item.to} item={item} />)}
        {isAdmin && (
          <>
            <div className="h-px bg-vault-outline-variant/30 my-3" />
            <GroupLabel>Back-office</GroupLabel>
            {ADMIN_LINKS.map((item) => <SidebarLink key={item.to} item={item} />)}
          </>
        )}
      </nav>

      {user && (
        <div className="px-4 pb-3">
          <Button leftIcon="add" fullWidth size="lg" onClick={() => navigate('/add')}>
            Ajouter un objet
          </Button>
        </div>
      )}

      <div className="px-3 pb-4 pt-3 border-t border-vault-outline-variant/20">
        {user ? (
          <>
            <button
              onClick={logout}
              className="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-vault-on-surface-variant hover:bg-vault-error/10 hover:text-vault-error transition-colors focus-ring"
            >
              <MaterialIcon name="logout" size={20} />
              <span>Se déconnecter</span>
            </button>
          </>
        ) : (
          <div className="flex flex-col gap-1 px-1">
            <NavLink to="/login" className="text-sm font-semibold text-vault-primary hover:text-vault-primary-dim px-2 py-1.5 focus-ring rounded">
              Se connecter →
            </NavLink>
            <NavLink to="/register" className="text-xs text-vault-on-surface-variant hover:text-vault-on-surface px-2 py-1 focus-ring rounded">
              Créer un compte
            </NavLink>
          </div>
        )}
      </div>
    </aside>
  )
}
