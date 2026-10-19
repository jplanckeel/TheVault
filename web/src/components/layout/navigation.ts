/** Navigation entry shared by the sidebar and the mobile tab bar. */
export interface NavItem {
  to: string
  icon: string
  label: string
  end?: boolean
}

/** Personal sections — same order as the mobile app tabs. */
export const USER_LINKS: NavItem[] = [
  { to: '/', icon: 'dashboard', label: 'Dashboard', end: true },
  { to: '/collection', icon: 'inventory_2', label: 'Ma collection' },
  { to: '/wishlist', icon: 'favorite', label: 'Wishlist' },
]

/** Public catalogue. */
export const EXPLORE_LINKS: NavItem[] = [{ to: '/catalogue', icon: 'travel_explore', label: 'Catalogue' }]

/** Back-office, visible to admins only. */
export const ADMIN_LINKS: NavItem[] = [{ to: '/jobs', icon: 'sync', label: 'Synchronisations' }]
