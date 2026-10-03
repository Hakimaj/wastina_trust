import { useRef, useState, type KeyboardEvent, type ReactNode } from 'react'
import { useId } from 'react'

export type TabItem = {
  id: string
  label: string
  content: ReactNode
  disabled?: boolean
}

type TabsProps = {
  label: string
  items: TabItem[]
  orientation?: 'horizontal' | 'vertical'
  className?: string
}

export function Tabs({ label, items, orientation = 'horizontal', className = '' }: TabsProps) {
  const baseId = useId()
  const initial = items.find((item) => !item.disabled)?.id ?? ''
  const [selectedId, setSelectedId] = useState(initial)
  const tabRefs = useRef(new Map<string, HTMLButtonElement>())
  const selectedItem = items.find((item) => item.id === selectedId && !item.disabled)

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>, currentIndex: number) {
    const backward = orientation === 'horizontal' ? 'ArrowLeft' : 'ArrowUp'
    const forward = orientation === 'horizontal' ? 'ArrowRight' : 'ArrowDown'
    if (![backward, forward, 'Home', 'End'].includes(event.key)) return
    event.preventDefault()

    const enabledItems = items.filter((item) => !item.disabled)
    if (enabledItems.length === 0) return
    const current = enabledItems.findIndex((item) => item.id === items[currentIndex]?.id)
    const nextIndex = event.key === 'Home'
      ? 0
      : event.key === 'End'
        ? enabledItems.length - 1
        : (current + (event.key === forward ? 1 : -1) + enabledItems.length) % enabledItems.length
    const nextItem = enabledItems[nextIndex]
    setSelectedId(nextItem.id)
    tabRefs.current.get(nextItem.id)?.focus()
  }

  return (
    <div className={['tabs', className].filter(Boolean).join(' ')}>
      <div className="tabs__list" role="tablist" aria-label={label} aria-orientation={orientation}>
        {items.map((item, index) => {
          const selected = item.id === selectedItem?.id
          const tabId = `${baseId}-tab-${item.id}`
          return (
            <button
              key={item.id}
              ref={(element) => {
                if (element) tabRefs.current.set(item.id, element)
                else tabRefs.current.delete(item.id)
              }}
              id={tabId}
              type="button"
              role="tab"
              aria-selected={selected}
              aria-controls={`${baseId}-panel-${item.id}`}
              tabIndex={selected ? 0 : -1}
              disabled={item.disabled}
              onClick={() => setSelectedId(item.id)}
              onKeyDown={(event) => handleKeyDown(event, index)}
            >
              {item.label}
            </button>
          )
        })}
      </div>
      {selectedItem ? (
        <section
          id={`${baseId}-panel-${selectedItem.id}`}
          className="tabs__panel"
          role="tabpanel"
          aria-labelledby={`${baseId}-tab-${selectedItem.id}`}
          tabIndex={0}
        >
          {selectedItem.content}
        </section>
      ) : null}
    </div>
  )
}
