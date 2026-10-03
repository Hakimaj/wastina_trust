import { useMemo, useState, type ReactNode } from 'react'
import { NavLink } from 'react-router-dom'
import { useAuth } from '../../features/auth'
import { useI18n } from '../../i18n/useI18n'
import { getVisibleNavigationItems } from '../navigation/navigationConfig'

type AppShellProps = {
  children: ReactNode
}

export function AppShell({ children }: AppShellProps) {
  const auth = useAuth()
  const { t, locale, setLocale } = useI18n()
  const [mobileOpen, setMobileOpen] = useState(false)

  const navItems = useMemo(
    () => getVisibleNavigationItems(auth.activeRole ?? auth.roles[0] ?? null),
    [auth.activeRole, auth.roles],
  )

  const roleOptions = auth.roles.length > 0 ? auth.roles : ['ROLE_WORKER']
  const organizationOptions = auth.organizations
  const accountName = auth.identity?.preferredUsername ?? auth.identity?.email ?? 'User'

  return (
    <div className="app-shell">
      <header className="app-shell__header" role="banner">
        <div className="app-shell__brand-wrap">
          <button
            type="button"
            className="app-shell__menu-button"
            aria-label={mobileOpen ? t('common.closeMenu') : t('common.openMenu')}
            aria-expanded={mobileOpen}
            aria-controls="primary-navigation"
            onClick={() => setMobileOpen((current) => !current)}
          >
            ☰
          </button>
          <div className="app-shell__brand">
            <span className="app-shell__brand-mark">W</span>
            <span>{t('common.appName')}</span>
          </div>
        </div>

        <div className="app-shell__toolbar">
          {auth.roles.length > 1 ? (
            <label className="select-control">
              <span>{t('common.role')}</span>
              <select
                value={auth.activeRole ?? ''}
                onChange={(event) => auth.setActiveRole(event.target.value)}
              >
                {roleOptions.map((role) => (
                  <option key={role} value={role}>
                    {role.replace('ROLE_', '').toLowerCase()}
                  </option>
                ))}
              </select>
            </label>
          ) : null}

          {organizationOptions.length > 0 ? (
            <label className="select-control">
              <span>{t('common.organization')}</span>
              <select
                value={auth.activeOrganization?.organizationId ?? ''}
                onChange={(event) => {
                  const target = organizationOptions.find(
                    (organization) => organization.organizationId === event.target.value,
                  )
                  if (target) auth.setActiveOrganization(target)
                }}
              >
                {organizationOptions.map((organization) => (
                  <option key={organization.organizationId} value={organization.organizationId}>
                    {organization.organizationId}
                  </option>
                ))}
              </select>
            </label>
          ) : null}

          <label className="select-control">
            <span>{t('common.language')}</span>
            <select
              value={locale}
              onChange={(event) => setLocale(event.target.value as 'en' | 'am')}
            >
              <option value="en">English</option>
              <option value="am">አማርኛ</option>
            </select>
          </label>

          <button
            type="button"
            className="button button--ghost"
            disabled
            aria-label={t('common.notificationsUnavailable')}
            title={t('common.notificationsUnavailable')}
          >
            {t('common.notifications')}
          </button>

          <div className="user-menu">
            <span className="account-label" aria-label={`${t('layout.signedInAs')} ${accountName}`}>
              {accountName}
            </span>
            <button
              type="button"
              className="button button--ghost"
              onClick={() => void auth.logout()}
            >
              {t('common.logout')}
            </button>
          </div>
        </div>
      </header>

      <a className="skip-link" href="#main-content">{t('common.skipToContent')}</a>

      <div className="app-shell__body">
        {mobileOpen ? (
          <button
            type="button"
            className="app-shell__backdrop"
            aria-label={t('common.closeMenu')}
            onClick={() => setMobileOpen(false)}
          />
        ) : null}
        <aside
          className={['app-shell__sidebar', mobileOpen ? 'is-open' : ''].join(' ')}
          aria-label={t('layout.primaryNavigation')}
        >
          <nav id="primary-navigation" aria-label={t('layout.primaryNavigation')}>
            <ul className="app-shell__nav-list">
              {navItems.map((item) => (
                <li key={item.id}>
                  <NavLink
                    to={item.path}
                    onClick={() => setMobileOpen(false)}
                    className={({ isActive }) =>
                      isActive ? 'nav-link nav-link--active' : 'nav-link'
                    }
                  >
                    <span>{t(item.labelKey)}</span>
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>
        </aside>

        <main id="main-content" className="app-shell__content" tabIndex={-1}>
          {children}
        </main>
      </div>
    </div>
  )
}
