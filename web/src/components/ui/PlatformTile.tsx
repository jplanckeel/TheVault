import MaterialIcon from './MaterialIcon'
import { familyLogo, familyOf, FAMILY_LABEL } from '../../lib/platforms'

interface Props {
  platform: string
  count: number
  unit?: string
  value?: string
  onClick: () => void
}

/** Full-width platform row: manufacturer band, logo, name, count, value. */
export default function PlatformTile({ platform, count, unit = 'objets', value, onClick }: Props) {
  const family = familyOf(platform)
  const logo = familyLogo(family)
  return (
    <button type="button" onClick={onClick} className="tv-ptile">
      <span className={`tv-ptile-band tv-fam-${family}`} aria-hidden="true" />
      {logo && <span className="tv-ptile-logo" style={{ ['--logo' as string]: `url(${logo})` }} aria-hidden="true" />}
      <span className="tv-ptile-body">
        <span className="tv-ptile-name">{platform}</span>
        <span className="tv-ptile-meta">
          {FAMILY_LABEL[family]} · {count.toLocaleString('fr-FR')} {unit}
        </span>
      </span>
      {value && (
        <span className="tv-ptile-end">
          <span className="tv-ptile-value">{value}</span>
        </span>
      )}
      <MaterialIcon name="chevron_right" size={20} className="text-ink-faint" />
    </button>
  )
}
