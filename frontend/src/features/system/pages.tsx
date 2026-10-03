function SystemPage({ title, message }: { title: string; message: string }) {
  return (
    <section className="system-page" role="status">
      <p className="eyebrow">Wastina system</p>
      <h1>{title}</h1>
      <p>{message}</p>
    </section>
  )
}

export function NotFoundPage() {
  return (
    <SystemPage
      title="Page not found"
      message="The requested route does not exist."
    />
  )
}

export function UnauthorizedPage() {
  return (
    <SystemPage
      title="Access unavailable"
      message="You do not have permission to view this page."
    />
  )
}

export function SessionExpiredPage() {
  return (
    <SystemPage
      title="Session expired"
      message="Your session needs to be established again."
    />
  )
}

export function ErrorPage() {
  return (
    <SystemPage
      title="Application error"
      message="The application could not render this page safely."
    />
  )
}
