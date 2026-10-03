import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AppPlaceholder } from '../layouts/AppPlaceholder'
import { NotFoundPage, UnauthorizedPage } from '../features/system/pages'

describe('foundation pages', () => {
  it('renders the application placeholder', () => {
    render(<AppPlaceholder title="Wastina Digital Trust Platform" />)
    expect(
      screen.getByRole('heading', { name: 'Wastina Digital Trust Platform' }),
    ).toBeInTheDocument()
  })

  it('supports a router route', () => {
    render(
      <MemoryRouter initialEntries={['/app']}>
        <Routes>
          <Route
            path="/app"
            element={<AppPlaceholder title="Application workspace" />}
          />
        </Routes>
      </MemoryRouter>,
    )
    expect(
      screen.getByRole('heading', { name: 'Application workspace' }),
    ).toBeInTheDocument()
  })

  it('renders system states accessibly', () => {
    render(
      <>
        <NotFoundPage />
        <UnauthorizedPage />
      </>,
    )
    expect(
      screen.getByRole('heading', { name: 'Page not found' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { name: 'Access unavailable' }),
    ).toBeInTheDocument()
  })
})
