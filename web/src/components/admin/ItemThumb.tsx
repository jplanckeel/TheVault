import { useState } from 'react'
import type { Item } from '../../types'
import BrandPlaceholder from '../BrandPlaceholder'

/** Small cover thumbnail with the brand placeholder as fallback. */
export default function ItemThumb({ item, className = 'h-12 w-9' }: { item: Pick<Item, 'cover_url' | 'platform' | 'title'>; className?: string }) {
  const [failed, setFailed] = useState(false)
  return (
    <div className={`shrink-0 overflow-hidden rounded-md bg-vault-surface-high ${className}`}>
      {item.cover_url && !failed ? (
        <img src={item.cover_url} alt="" loading="lazy" className="h-full w-full object-cover" onError={() => setFailed(true)} />
      ) : (
        <BrandPlaceholder platform={item.platform} />
      )}
    </div>
  )
}
