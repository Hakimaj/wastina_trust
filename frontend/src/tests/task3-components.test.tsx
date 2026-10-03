import { useState } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { DataTable } from '../components/data/DataTable'
import { ToastProvider } from '../components/feedback/ToastProvider'
import { useToast } from '../components/feedback/useToast'
import { Dialog } from '../components/primitives/Dialog'
import { Tabs } from '../components/primitives/Tabs'
import { I18nProvider } from '../i18n/I18nProvider'

function DialogHarness() {
  const [open, setOpen] = useState(false)
  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>Open dialog</button>
      <Dialog open={open} title="Confirm action" description="Please confirm." onClose={() => setOpen(false)}>
        <button type="button">Continue</button>
      </Dialog>
    </>
  )
}

function ToastHarness() {
  const { notify } = useToast()
  return <button type="button" onClick={() => notify('Saved successfully.')}>Save</button>
}

describe('Task 3 accessible components', () => {
  it('opens an accessible dialog and dismisses it with Escape', async () => {
    const user = userEvent.setup()
    render(<DialogHarness />)
    await user.click(screen.getByRole('button', { name: 'Open dialog' }))
    const dialog = screen.getByRole('dialog', { name: 'Confirm action' })
    expect(dialog).toHaveAttribute('aria-modal', 'true')
    expect(screen.getByText('Please confirm.')).toBeInTheDocument()
    const closeButton = screen.getByRole('button', { name: 'Close' })
    screen.getByRole('button', { name: 'Continue' }).focus()
    fireEvent.keyDown(screen.getByRole('button', { name: 'Continue' }), { key: 'Tab' })
    expect(closeButton).toHaveFocus()
    fireEvent.keyDown(dialog, { key: 'Escape' })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Open dialog' })).toHaveFocus()
  })

  it('supports arrow-key selection in tabs', async () => {
    const user = userEvent.setup()
    render(
      <Tabs
        label="Account sections"
        items={[
          { id: 'profile', label: 'Profile', content: <p>Profile panel</p> },
          { id: 'security', label: 'Security', content: <p>Security panel</p> },
        ]}
      />,
    )
    const profileTab = screen.getByRole('tab', { name: 'Profile' })
    profileTab.focus()
    await user.keyboard('{ArrowRight}')
    expect(screen.getByRole('tab', { name: 'Security' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tabpanel')).toHaveTextContent('Security panel')
  })

  it('renders semantic table headings, row headers, and empty state', () => {
    render(
      <DataTable
        caption="Organizations"
        columns={[
          { key: 'name', header: 'Name', rowHeader: true },
          { key: 'status', header: 'Status' },
        ]}
        rows={[{ id: 'org-1', name: 'Example Org', status: 'Active' }]}
        getRowKey={(row) => row.id}
      />,
    )
    expect(screen.getByRole('table', { name: 'Organizations' })).toBeInTheDocument()
    expect(screen.getByRole('rowheader', { name: 'Example Org' })).toBeInTheDocument()

    render(
      <DataTable
        caption="Empty organizations"
        columns={[{ key: 'name', header: 'Name' }]}
        rows={[]}
        getRowKey={() => 'unused'}
        emptyState="No organizations are available."
      />,
    )
    expect(screen.getByText('No organizations are available.')).toBeInTheDocument()
  })

  it('announces and dismisses toast feedback', async () => {
    const user = userEvent.setup()
    render(
      <I18nProvider>
        <ToastProvider><ToastHarness /></ToastProvider>
      </I18nProvider>,
    )
    await user.click(screen.getByRole('button', { name: 'Save' }))
    expect(screen.getByRole('status')).toHaveTextContent('Saved successfully.')
    await user.click(screen.getByRole('button', { name: 'Dismiss notification' }))
    expect(screen.queryByText('Saved successfully.')).not.toBeInTheDocument()
  })
})
