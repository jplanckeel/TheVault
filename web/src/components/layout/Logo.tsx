import { useId } from 'react'

/**
 * TheVault hexagon mark. The gradient id is unique per instance: the sidebar copy is
 * `display: none` on small screens and would otherwise hide the gradient of the others.
 */
export default function Logo({ className = 'w-10 h-10' }: { className?: string }) {
  const gradientId = `logo-gradient-${useId().replace(/:/g, '')}`
  const fill = `url(#${gradientId})`
  return (
    <svg viewBox="0 0 100 100" className={`shrink-0 ${className}`} aria-hidden>
      <defs>
        <linearGradient id={gradientId} x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#d8b9ff" />
          <stop offset="100%" stopColor="#7e28df" />
        </linearGradient>
      </defs>
      <path d="M20 30 L50 15 L80 30 L80 70 L50 85 L20 70 Z" fill="none" stroke={fill} strokeWidth="6" strokeLinejoin="round" />
      <path d="M35 45 L50 35 L65 45 L65 60 L50 70 L35 60 Z" fill={fill} />
      <rect x="47" y="52" width="6" height="10" rx="3" fill="#e4e1e6" opacity="0.85" />
    </svg>
  )
}
