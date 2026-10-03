import { PageContainer } from '../components/layout/PageContainer'
import { Badge } from '../components/primitives/Badge'

type AppPlaceholderProps = {
  title: string
  description?: string
}

export function AppPlaceholder({
  title,
  description = 'This workspace is ready for the next Wastina feature module.',
}: AppPlaceholderProps) {
  return (
    <PageContainer
      title={title}
      description={description}
      showBreadcrumbs
      actions={<Badge tone="neutral">Coming soon</Badge>}
    >
      <section className="placeholder-panel">
        <p>
          The shared shell and navigation foundation are in place. Future domain screens
          will plug into this layout without recreating the application structure.
        </p>
      </section>
    </PageContainer>
  )
}
