import { useMemo, useState, type ReactNode } from 'react'
import { NavLink } from 'react-router-dom'
import { config } from '../../app/config'
import { useAuth } from '../../features/auth'
import { useI18n } from '../../i18n/useI18n'
import { getVisibleNavigationItems } from '../navigation/navigationConfig'
import { OrganizationSwitcher } from './OrganizationSwitcher'

type AppShellProps = {
  children: ReactNode
}

/**
 * The authenticated application shell.
 *
 * Navigation visibility is driven by the roles the backend persisted in `/api/v1/me` and by
 * organization capabilities it computed. Hiding a link is a usability affordance only — the backend
 * remains the authority, and every screen re-checks before acting.
 */
export function AppShell({ children }: AppShellProps) {
  const auth = useAuth()
  const { t, locale, setLocale } = useI18n()
  const [mobileOpen, setMobileOpen] = useState(false)

  const navItems = useMemo(
    () => getVisibleNavigationItems(auth.roles, auth.organizations),
    [auth.roles, auth.organizations],
  )

  const accountName =
    auth.identity?.preferredUsername ?? auth.identity?.email ?? auth.account?.email ?? 'User'

  // Marked by the local JWT provider. Never inferred from a token claim the user could influence.
  const isLocalDevelopment =
    config.authMode === 'dev-jwt' || auth.identity?.claims.developmentOnly === true

  const persona = typeof auth.identity?.claims.persona === 'string' ? auth.identity.claims.persona : null

  return (
    <div className="app-shell">
      {isLocalDevelopment ? (
        <div className="dev-banner" role="status">
          <strong>{t('devAuth.warningTitle')}</strong>
          <span>{t('devAuth.warning')}</span>
          {persona ? (
            <span className="dev-banner__persona">
              {t('devAuth.activePersona')}: <code>{persona}</code>
            </span>
          ) : null}
        </div>
      ) : null}

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
            <span className="app-shell__brand-mark" aria-hidden="true">W</span>
            <span>{t('common.appName')}</span>
          </div>
        </div>

        <div className="app-shell__toolbar">
          <OrganizationSwitcher />

          <label className="select-control" htmlFor="locale-switcher">
            <span>{t('common.language')}</span>
            <select
              id="locale-switcher"
              value={locale}
              onChange={(event) => setLocale(event.target.value as 'en' | 'am')}
            >
              <option value="en">English</option>
              <option value="am">አማርኛ</option>
            </select>
          </label>

          <div className="user-menu">
            <span className="account-label" aria-label={`${t('layout.signedInAs')} ${accountName}`}>
              {accountName}
            </span>
            <button
              type="button"
              className="button button--ghost"
              onClick={() => void auth.logout()}
            >
              {isLocalDevelopment ? t('common.resetLocalSession') : t('common.logout')}
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