import { useCallback, useEffect, useState } from 'react'

/** « Automatique » follows the system, otherwise one of the two shells. */
export type ThemeMode = 'auto' | 'shell' | 'black'

export const THEME_MODES: { value: ThemeMode; label: string }[] = [
  { value: 'auto', label: 'Auto' },
  { value: 'shell', label: 'Coque grise' },
  { value: 'black', label: 'Black Edition' },
]

const KEY = 'thevault.theme'
const EVENT = 'thevault:theme'

function readMode(): ThemeMode {
  try {
    const v = localStorage.getItem(KEY)
    return v === 'shell' || v === 'black' ? v : 'auto'
  } catch {
    return 'auto'
  }
}

/** Applies the stored shell before the first paint (called from main.tsx). */
export function applyStoredTheme() {
  applyMode(readMode())
}

function applyMode(mode: ThemeMode) {
  const root = document.documentElement
  if (mode === 'auto') root.removeAttribute('data-theme')
  else root.setAttribute('data-theme', mode)
}

/** True when the Black Edition shell is showing (picked, or followed from the system). */
function isDark(mode: ThemeMode): boolean {
  if (mode === 'black') return true
  if (mode === 'shell') return false
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false
}

/** Current shell and setter; every caller re-renders when the shell changes. */
export function useTheme() {
  const [mode, setModeState] = useState<ThemeMode>(readMode)
  const [dark, setDark] = useState(() => isDark(readMode()))

  useEffect(() => {
    const media = window.matchMedia?.('(prefers-color-scheme: dark)')
    const sync = () => {
      const m = readMode()
      setModeState(m)
      setDark(isDark(m))
    }
    window.addEventListener(EVENT, sync)
    media?.addEventListener('change', sync)
    return () => {
      window.removeEventListener(EVENT, sync)
      media?.removeEventListener('change', sync)
    }
  }, [])

  const setMode = useCallback((next: ThemeMode) => {
    try {
      if (next === 'auto') localStorage.removeItem(KEY)
      else localStorage.setItem(KEY, next)
    } catch {
      // Private mode: the choice simply lasts for this page.
    }
    applyMode(next)
    window.dispatchEvent(new Event(EVENT))
  }, [])

  return { mode, dark, setMode }
}

/** Resolved value of a colour token, for SVG libraries that cannot read CSS variables. */
export function tokenColor(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(`--${name}`).trim()
}
