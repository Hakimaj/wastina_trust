const CHANNEL_NAME = 'wastina-auth-events'
const LOGOUT_KEY = 'wastina.auth.logout'

type AuthEvent = { type: 'logout'; timestamp: number }

export function broadcastLogout() {
  const event: AuthEvent = { type: 'logout', timestamp: Date.now() }
  if ('BroadcastChannel' in window) {
    const channel = new BroadcastChannel(CHANNEL_NAME)
    channel.postMessage(event)
    channel.close()
    return
  }
  localStorage.setItem(LOGOUT_KEY, String(event.timestamp))
  localStorage.removeItem(LOGOUT_KEY)
}

export function subscribeToAuthEvents(onLogout: () => void): () => void {
  if ('BroadcastChannel' in window) {
    const channel = new BroadcastChannel(CHANNEL_NAME)
    const listener = (event: MessageEvent<AuthEvent>) => {
      if (event.data?.type === 'logout') onLogout()
    }
    channel.addEventListener('message', listener)
    return () => {
      channel.removeEventListener('message', listener)
      channel.close()
    }
  }

  const listener = (event: StorageEvent) => {
    if (event.key === LOGOUT_KEY && event.newValue) onLogout()
  }
  window.addEventListener('storage', listener)
  return () => window.removeEventListener('storage', listener)
}
