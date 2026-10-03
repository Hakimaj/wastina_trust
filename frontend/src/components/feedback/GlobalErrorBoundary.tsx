import { Component, type ErrorInfo, type ReactNode } from 'react'

type Props = { children: ReactNode }
type State = { hasError: boolean; referenceId: string }

export class GlobalErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, referenceId: '' }

  static getDerivedStateFromError(): State {
    return { hasError: true, referenceId: crypto.randomUUID() }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // Deliberately avoid logging component details or user data in the foundation.
    void error
    void info
  }

  render() {
    if (!this.state.hasError) return this.props.children

    return (
      <main className="page-frame">
        <section className="system-page" role="alert">
          <p className="eyebrow">Wastina system</p>
          <h1>Something needs attention</h1>
          <p>The application recovered safely from an unexpected error.</p>
          <p className="reference">Reference: {this.state.referenceId}</p>
        </section>
      </main>
    )
  }
}
