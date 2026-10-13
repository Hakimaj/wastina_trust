import { useMemo } from 'react'
import { useAuth } from '../../features/auth/AuthContext'
import { useI18n } from '../../i18n/useI18n'
import type { OrganizationMembership } from '../../features/auth/authTypes'

export type OrganizationScope = {
  /** Memberships the user can actually act through right now. */
  selectable: OrganizationMembership[]
  /** Memberships carrying employer verification rights. */
  verifiable: OrganizationMembership[]
  active: OrganizationMembership | null
  /** The active membership, or the first verifiable one. */
  effective: OrganizationMembership | null
  isEmpty: boolean
}

/**
 * Resolves the organization a feature should act in.
 *
 * Kept out of the components so the shell and every organization-scoped screen agree on the same
 * answer. A membership is selectable only while `ACTIVE`: offering a suspended or left membership
 * would produce requests the backend will reject.
 *
 * `effective` falls back to the first organization that can verify employment, so the verification
 * inbox has a sensible target even when the shell selection is stale.
 */
export function useOrganizationScope(): OrganizationScope {
  const auth = useAuth()

  return useMemo(() => {
    const selectable = auth.organizations.filter(
      (membership) => membership.status === 'ACTIVE',
    )
    const verifiable = selectable.filter((membership) => membership.canVerifyEmployment)

    const active =
      auth.activeOrganization &&
      selectable.some(
        (membership) => membership.id === auth.activeOrganization?.id,
      )
        ? auth.activeOrganization
        : null

    return {
      selectable,
      verifiable,
      active,
      effective: active ?? verifiable[0] ?? selectable[0] ?? null,
      isEmpty: selectable.length === 0,
    }
  }, [auth.activeOrganization, auth.organizations])
}

export type OrganizationSwitcherProps = {
  /**
   * Restricts the list to organizations with verification rights. Used by the verification
   * workspace, which cannot do anything with the others.
   */
  verifiersOnly?: boolean
  id?: string
}

/**
 * Organization selector for the application shell.
 *
 * Always rendered when the user is authenticated, including with a single "no organization"
 * option, so the control does not appear and disappear as memberships load.
 */
export function OrganizationSwitcher({ verifiersOnly = false, id }: OrganizationSwitcherProps) {
  const auth = useAuth()
  const { t } = useI18n()
  const { selectable, verifiable, active, isEmpty } = useOrganizationScope()

  const options = verifiersOnly ? verifiable : selectable
  const value = active?.organizationId ?? ''

  return (
    <label className="select-control" htmlFor={id ?? 'organization-switcher'}>
      <span>{t('common.organization')}</span>
      <select
        id={id ?? 'organization-switcher'}
        value={value}
        disabled={options.length === 0}
        onChange={(event) => {
          const target = options.find(
            (membership) => membership.organizationId === event.target.value,
          )
          // Selecting the blank option clears the active organization, which is how a user says
          // "act on my own account" rather than "act inside an organization".
          auth.setActiveOrganization(target ?? null)
        }}
      >
        <option value="">{isEmpty ? t('layout.noOrganization') : t('layout.selectOrganization')}</option>
        {options.map((membership) => (
          <option key={membership.id} value={membership.organizationId}>
            {membership.name ?? membership.legalName ?? membership.organizationId}
          </option>
        ))}
      </select>
    </label>
  )
}