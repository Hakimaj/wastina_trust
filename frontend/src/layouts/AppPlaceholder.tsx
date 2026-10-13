type AppPlaceholderProps = { title: string }

export function AppPlaceholder({ title }: AppPlaceholderProps) {
  return (
    <section className="system-page">
      <p className="eyebrow">Foundation</p>
      <h1>{title}</h1>
      <p>This workspace is ready for the next Wastina feature module.</p>
    </section>
  )
}
