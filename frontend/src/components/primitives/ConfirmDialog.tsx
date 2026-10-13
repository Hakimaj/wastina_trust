import { useState, type ReactNode } from 'react'
import { Button } from './Button'
import { Dialog } from './Dialog'

export type ConfirmDialogProps = {
  open: boolean
  title: string
  description: ReactNode
  confirmLabel?: string
  cancelLabel?: string
  tone?: 'primary' | 'danger'
  /**
   * Runs when confirmed. Resolving closes the dialog; rejecting keeps it open and surfaces the
   * reason, so a failed destructive action never looks like it succeeded.
   */
  onConfirm: () => Promise<unknown> | unknown
  onCancel: () => void
}

/**
 * Confirmation for an irreversible or externally visible action.
 *
 * The confirm button stays disabled while the action is in flight, which prevents a double submit
 * from creating a duplicate record.
 */
export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  tone = 'primary',
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleConfirm() {
    setPending(true)
    setError(null)
    try {
      await onConfirm()
      setPending(false)
      onCancel()
    } catch (cause) {
      setPending(false)
      setError(cause instanceof Error ? cause.message : 'The action could not be completed.')
    }
  }

  function handleCancel() {
    if (pending) return
    setError(null)
    onCancel()
  }

  return (
    <Dialog
      open={open}
      title={title}
      onClose={handleCancel}
      closeLabel={cancelLabel}
      className="dialog--confirm"
      footer={
        <>
          <Button type="button" variant="ghost" onClick={handleCancel} disabled={pending}>
            {cancelLabel}
          </Button>
          <Button
            type="button"
            variant={tone === 'danger' ? 'danger' : 'primary'}
            onClick={() => void handleConfirm()}
            disabled={pending}
          >
            {pending ? 'Working…' : confirmLabel}
          </Button>
        </>
      }
    >
      <div className="dialog__content">
        {description}
        {error ? (
          <p className="dialog__error" role="alert">
            {error}
          </p>
        ) : null}
      </div>
    </Dialog>
  )
}