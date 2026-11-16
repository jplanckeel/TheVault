import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'

export interface User {
  id: string
  email: string
  name: string
  /** Back-office access, granted on the backend through ADMIN_EMAILS. */
  is_admin: boolean
}

interface AuthContextType {
  user: User | null
  token: string | null
  isAdmin: boolean
  /** Signs in; resolves to null on success, otherwise the message to show. */
  login: (email: string, password: string) => Promise<string | null>
  register: (email: string, password: string, name: string) => Promise<string | null>
  logout: () => void
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  token: null,
  isAdmin: false,
  login: async () => null,
  register: async () => null,
  logout: () => {},
})

/** Message for a failed sign-in: wrong credentials, rate limit or server error. */
// eslint-disable-next-line react-refresh/only-export-components
export function loginError(status: number, retryAfter: string | null): string {
  if (status === 401) return 'Email ou mot de passe incorrect.'
  if (status === 429) {
    const s = Number(retryAfter)
    return `Trop de tentatives de connexion. Réessaie dans ${Number.isFinite(s) && s > 0 ? `${s} s` : 'une minute'}.`
  }
  return `Le serveur n'a pas pu te connecter (erreur ${status}). Réessaie plus tard.`
}

function readStoredUser(): User | null {
  try {
    const raw = localStorage.getItem('user')
    return raw ? (JSON.parse(raw) as User) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('token'))
  const [user, setUser] = useState<User | null>(readStoredUser)

  function saveSession(data: { token: string; user: User }) {
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
    setToken(data.token)
    setUser(data.user)
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setToken(null)
    setUser(null)
  }

  // Refresh the profile on load so a role granted or revoked server-side is picked up.
  useEffect(() => {
    if (!token) return
    fetch('/api/v1/auth/me', { headers: { Authorization: `Bearer ${token}` } })
      .then((r) => {
        if (r.status === 401) {
          logout()
          return null
        }
        return r.ok ? (r.json() as Promise<User>) : null
      })
      .then((data) => {
        if (data) {
          setUser(data)
          localStorage.setItem('user', JSON.stringify(data))
        }
      })
      .catch(() => {})
  }, [token])

  async function login(email: string, password: string): Promise<string | null> {
    let res: Response
    try {
      res = await fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email.trim(), password }),
      })
    } catch {
      return 'Serveur injoignable : vérifie ta connexion.'
    }
    if (res.ok) {
      saveSession(await res.json())
      return null
    }
    return loginError(res.status, res.headers.get('Retry-After'))
  }

  async function register(email: string, password: string, name: string): Promise<string | null> {
    const res = await fetch('/api/v1/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, name }),
    })
    if (res.status === 429) return loginError(429, res.headers.get('Retry-After'))
    if (res.status === 409) return 'Un compte existe déjà avec cet e-mail : connecte-toi.'
    if (!res.ok) return (await res.text()) || "L'inscription a échoué"
    saveSession(await res.json())
    return null
  }

  return (
    <AuthContext.Provider value={{ user, token, isAdmin: user?.is_admin === true, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  return useContext(AuthContext)
}
