export type ApiAuthBridge = {
  getAccessToken: () => string | undefined
  refreshAccessToken: () => Promise<boolean>
  handleUnauthorized: () => void
}

let bridge: ApiAuthBridge | undefined

export function configureApiAuth(nextBridge: ApiAuthBridge | undefined) {
  bridge = nextBridge
}

export function getApiAuth(): ApiAuthBridge | undefined {
  return bridge
}
