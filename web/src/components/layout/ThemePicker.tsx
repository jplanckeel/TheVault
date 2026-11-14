import { SegmentedControl } from '../ui'
import { THEME_MODES, useTheme } from '../../lib/theme'

/** « Coque » picker: Auto, Coque grise or Black Edition (same choice as the mobile settings). */
export default function ThemePicker() {
  const { mode, setMode } = useTheme()
  return (
    <div>
      <p className="tv-engrave mb-2">Coque</p>
      <SegmentedControl options={THEME_MODES.map((m) => ({ value: m.value, label: m.label === 'Black Edition' ? 'Black' : m.label === 'Coque grise' ? 'Grise' : m.label }))} value={mode} onChange={setMode} label="Coque de l'application" />
    </div>
  )
}
