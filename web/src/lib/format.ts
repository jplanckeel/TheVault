/** Euro amount in fr-FR format: `1 234 €` in lists, `12,50 €` with decimals on detail pages. */
export function formatEuro(amount: number, decimals: 0 | 2 = 0): string {
  return amount.toLocaleString('fr-FR', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  })
}

/** Same as {@link formatEuro}, with an em dash for unknown amounts. */
export function formatEuroOrDash(amount: number | null | undefined, decimals: 0 | 2 = 0): string {
  return amount == null || amount <= 0 ? '—' : formatEuro(amount, decimals)
}

/** Relative time in French ("il y a 3 h", "il y a 2 j"). */
export function formatRelative(iso: string, now: Date = new Date()): string {
  const seconds = Math.round((now.getTime() - new Date(iso).getTime()) / 1000)
  const rtf = new Intl.RelativeTimeFormat('fr', { numeric: 'auto' })
  if (seconds < 60) return rtf.format(-seconds, 'second')
  if (seconds < 3600) return rtf.format(-Math.round(seconds / 60), 'minute')
  if (seconds < 86400) return rtf.format(-Math.round(seconds / 3600), 'hour')
  return rtf.format(-Math.round(seconds / 86400), 'day')
}
