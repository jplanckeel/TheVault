import type { Item } from '../../types'
import Chip from './Chip'
import MaterialIcon from './MaterialIcon'
import BrandPlaceholder from '../BrandPlaceholder'

type ConditionBadge = {
  label: string
  tone: 'neutral' | 'primary' | 'secondary' | 'success' | 'error' | 'warning' | 'info'
}

/** Map raw condition string to a styled badge. */
function conditionBadge(condition: string): ConditionBadge | null {
  const c = condition?.toLowerCase().trim()
  if (!c) return null
  if (c === 'sealed' || c === 'new') return { label: 'SEALED', tone: 'error' }
  if (c === 'cib') return { label: 'CIB', tone: 'neutral' }
  if (c === 'loose') return { label: 'LOOSE', tone: 'neutral' }
  if (c === 'mint') return { label: 'MINT', tone: 'success' }
  if (c === 'graded') return { label: 'GRADED', tone: 'warning' }
  return { label: condition.toUpperCase(), tone: 'neutral' }
}

interface Props {
  item: Item
  /** Estimated price (€) */
  price?: number
  /** Trend arrow */
  trend?: 'up' | 'down' | 'flat'
  /** Override condition badge */
  badge?: ConditionBadge
  /** Click handler */
  onClick?: () => void
  /** Show wishlist star icon corner instead of condition */
  wishlist?: boolean
  className?: string
}

/**
 * Premium game cover card. Aspect 3:4 cover, top badge, glassmorphic footer.
 * Used in Collection, Wishlist, Catalogue, Latest Additions.
 */
export default function GameCard({ item, price, trend, badge, onClick, wishlist, className = '' }: Props) {
  const finalBadge = badge ?? conditionBadge(item.condition)
  const platform = item.platform?.toUpperCase() || item.type?.toUpperCase()

  return (
    <button
      onClick={onClick}
      className={[
        'group relative flex flex-col rounded-2xl overflow-hidden text-left',
        'bg-vault-surface-low border border-vault-outline-variant/30',
        'transition-all duration-200 ease-out',
        'hover:border-vault-primary/60 hover:-translate-y-0.5 hover:glow-purple',
        'focus-ring',
        className,
      ].join(' ')}
    >
      {/* Cover */}
      <div className="relative aspect-[3/4] w-full overflow-hidden bg-vault-surface-high">
        {item.cover_url ? (
          <img
            src={item.cover_url}
            alt={item.title}
            loading="lazy"
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
        ) : (
          <BrandPlaceholder platform={item.platform} className="h-full w-full" />
        )}

        {/* Top badge */}
        {finalBadge && !wishlist && (
          <div className="absolute top-2 right-2">
            <Chip tone={finalBadge.tone} size="sm" variant="solid">
              {finalBadge.label}
            </Chip>
          </div>
        )}
        {wishlist && (
          <div className="absolute top-2 right-2 h-7 w-7 rounded-full bg-vault-primary/90 text-vault-on-primary flex items-center justify-center">
            <MaterialIcon name="star" size={14} filled />
          </div>
        )}

        {/* Bottom gradient overlay */}
        <div className="absolute inset-x-0 bottom-0 h-2/5 bg-gradient-to-t from-black/85 via-black/40 to-transparent" />
      </div>

      {/* Footer */}
      <div className="p-3 min-w-0">
        {platform && (
          <p className="text-[10px] font-bold tracking-widest text-vault-primary uppercase truncate">
            {platform}
          </p>
        )}
        <h3 className="mt-0.5 text-sm font-semibold text-vault-on-surface line-clamp-1">{item.title}</h3>
        <div className="mt-1.5 flex items-center justify-between gap-2">
          <span className="text-sm font-bold tabular-nums text-vault-on-surface">
            {price ? price.toLocaleString('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }) : <span className="text-vault-on-surface-variant text-xs font-normal">—</span>}
          </span>
          {trend && (
            <MaterialIcon
              name={trend === 'up' ? 'trending_up' : trend === 'down' ? 'trending_down' : 'trending_flat'}
              size={16}
              className={
                trend === 'up'
                  ? 'text-vault-success'
                  : trend === 'down'
                    ? 'text-vault-error'
                    : 'text-vault-on-surface-variant'
              }
            />
          )}
        </div>
      </div>
    </button>
  )
}
