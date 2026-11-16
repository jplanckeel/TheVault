/** Navigation entry shared by the sidebar and the mobile tab bar. */
export interface NavItem {
  to: string
  icon: string
  label: string
  end?: boolean
}

/** Personal sections — same order as the mobile tabs (Accueil · Collection · + · Envies). */
export const USER_LINKS: NavItem[] = [
  { to: '/', icon: 'home', label: 'Accueil', end: true },
  { to: '/collection', icon: 'grid_view', label: 'Collection' },
  { to: '/wishlist', icon: 'favorite', label: 'Envies' },
]

/** Shared catalogue. */
export const EXPLORE_LINKS: NavItem[] = [{ to: '/catalogue', icon: 'travel_explore', label: 'Explorer' }]

/** Back-office, visible to admins only. */
export const ADMIN_LINKS: NavItem[] = [
  { to: '/admin/data', icon: 'database', label: 'Données' },
  { to: '/admin/sync', icon: 'sync', label: 'Synchronisations' },
]
