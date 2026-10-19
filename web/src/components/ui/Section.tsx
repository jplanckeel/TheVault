import type { ReactNode } from 'react'

interface Props {
  title: ReactNode
  /** Right-side action (button, link, chevrons) */
  action?: ReactNode
  /** Description under the title */
  description?: ReactNode
  /** Use Montserrat headline-md (24px). Default true. */
  headline?: boolean
  children: ReactNode
  className?: string
}

/**
 * Page / module section with consistent header.
 * Used for "Latest Additions", "Price Alerts", "Market Listings", etc.
 */
export default function Section({
  title,
  action,
  description,
  headline = true,
  children,
  className = '',
}: Props) {
  return (
    <section className={className}>
      <div className="flex items-end justify-between gap-4 mb-4">
        <div className="min-w-0">
          <h2
            className={
              headline
                ? 'text-headline-md text-vault-on-surface'
                : 'text-label-bold text-vault-on-surface-variant'
            }
          >
            {title}
          </h2>
          {description && (
            <p className="text-sm text-vault-on-surface-variant mt-1">{description}</p>
          )}
        </div>
        {action && <div className="shrink-0">{action}</div>}
      </div>
      {children}
    </section>
  )
}
