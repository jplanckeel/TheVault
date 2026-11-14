/** The four completeness steps of the design system (4-segment gauge). */
export type Condition = 'loose' | 'cib' | 'new' | 'sealed'

export const CONDITIONS: { value: Condition; label: string }[] = [
  { value: 'loose', label: 'Loose' },
  { value: 'cib', label: 'Complet' },
  { value: 'new', label: 'Neuf' },
  { value: 'sealed', label: 'Scellé' },
]

const LABEL: Record<Condition, string> = { loose: 'Loose', cib: 'Complet', new: 'Neuf', sealed: 'Scellé' }
const PIPS: Record<Condition, number> = { loose: 1, cib: 2, new: 3, sealed: 4 }

/** Normalises the condition strings stored in the catalogue; unknown values give null. */
export function parseCondition(raw: string | null | undefined): Condition | null {
  const c = (raw ?? '').trim().toLowerCase()
  if (c === 'loose') return 'loose'
  if (c === 'cib' || c === 'complete' || c === 'complet') return 'cib'
  if (c === 'new' || c === 'neuf' || c === 'mint') return 'new'
  if (c === 'sealed' || c === 'scellé' || c === 'scelle') return 'sealed'
  return null
}

export const conditionLabel = (c: Condition) => LABEL[c]
export const conditionPips = (c: Condition) => PIPS[c]

/** eBay quotes exist for loose, cib and new (a sealed copy is quoted as new). */
export type PriceCondition = 'loose' | 'cib' | 'new'
export const PRICE_CONDITIONS: { value: PriceCondition; label: string }[] = [
  { value: 'loose', label: 'Loose' },
  { value: 'cib', label: 'Complet' },
  { value: 'new', label: 'Neuf' },
]
