import { createContext, useContext, useState, type ReactNode } from 'react'

interface AuthContextType {
  isAdmin: boolean
  token: string | null
  login: (password: string) => Promise<boolean>
  logout: () => void
}

const AuthContext = createContext<AuthContextType>({
  isAdmin: false,
  token: null,
  login: async () => false,
  logout: () => {},
})

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => sessionStorage.getItem('admin_token'))

  async function login(password: string): Promise<boolean> {
    const res = await fetch('/api/v1/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ password }),
    })
    if (!res.ok) return false
    const data = await res.json()
    sessionStorage.setItem('admin_token', data.token)
    setToken(data.token)
    return true
  }

  function logout() {
    sessionStorage.removeItem('admin_token')
    setToken(null)
  }

  return (
    <AuthContext.Provider value={{ isAdmin: !!token, token, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
