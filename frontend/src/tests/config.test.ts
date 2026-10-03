import { describe, expect, it } from 'vitest'
import { config } from '../app/config'

describe('public configuration', () => {
  it('exposes only browser-safe configuration', () => {
    expect(config).toHaveProperty('apiBaseUrl')
    expect(config).toHaveProperty('environment')
    expect(config).toHaveProperty('defaultLocale')
    expect(config).not.toHaveProperty('clientSecret')
  })
})
