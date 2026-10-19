import type { ReactNode } from 'react'
import MaterialIcon from './MaterialIcon'
import Button from './Button'

interface Props {
  /** Material symbol name to render at top */
  icon?: string
  title: string
  description?: ReactNode
  /** CTA button — pass label and onClick or href */
  cta?: { label: string; onClick?: () => void; icon?: string; href?: string }
  className?: string
}

export default function EmptyState({ icon = 'inbox', title, description, cta, className = '' }: Props) {
  return (
    <div
      className={[
        'flex flex-col items-center justify-center text-center py-16 px-6',
        'rounded-2xl border border-dashed border-vault-outline-variant/40 bg-vault-surface-low/40',
        className,
      ].join(' ')}
    >
      <div className="h-14 w-14 rounded-2xl bg-vault-primary/10 text-vault-primary flex items-center justify-center mb-4">
        <MaterialIcon name={icon} size={28} />
      </div>
      <h3 className="text-headline-sm text-vault-on-surface mb-2">{title}</h3>
      {description && (
        <p className="text-sm text-vault-on-surface-variant max-w-md mb-6">{description}</p>
      )}
      {cta &&
        (cta.href ? (
          <a href={cta.href}>
            <Button leftIcon={cta.icon}>{cta.label}</Button>
          </a>
        ) : (
          <Button leftIcon={cta.icon} onClick={cta.onClick}>
            {cta.label}
          </Button>
        ))}
    </div>
  )
}
