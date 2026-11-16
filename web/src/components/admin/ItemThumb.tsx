import type { Item } from '../../types'
import { GameCover } from '../ui'

/** Small cartridge thumbnail of a catalogue item. */
export default function ItemThumb({ item, className = 'w-9' }: { item: Pick<Item, 'cover_url' | 'platform' | 'title'>; className?: string }) {
  return (
    <div className={`shrink-0 ${className}`}>
      <GameCover size="xs" title={item.title} platform={item.platform} src={item.cover_url || undefined} />
    </div>
  )
}
