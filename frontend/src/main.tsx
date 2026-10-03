import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Application } from './app/Application'
import { loadConfig } from './app/config'
import { GlobalErrorBoundary } from './components/feedback/GlobalErrorBoundary'
import './styles/global.css'

loadConfig()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <GlobalErrorBoundary>
      <Application />
    </GlobalErrorBoundary>
  </StrictMode>,
)
