import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'

export default function AdminLoginPage() {
  const { adminLogin } = useAuth()
  const navigate = useNavigate()
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    const ok = await adminLogin(password)
    setLoading(false)
    if (ok) navigate('/jobs')
    else setError('Mot de passe incorrect')
  }

  return (
    <div className="min-h-screen bg-vault-bg flex items-center justify-center p-4">
      <div className="w-full max-w-sm">
        <div className="flex flex-col items-center mb-8">
          <div className="h-12 w-12 rounded-xl bg-vault-error/15 text-vault-error flex items-center justify-center mb-4">
            <span className="material-symbols-rounded text-2xl">shield_lock</span>
          </div>
          <h1 className="text-xl font-bold text-vault-on-surface">Admin Access</h1>
        </div>

        <form onSubmit={handleSubmit} className="bg-vault-surface border border-vault-outline-variant rounded-xl p-6 space-y-4">
          {error && <p className="text-sm text-vault-error">{error}</p>}
          <div>
            <label className="text-xs font-medium text-vault-on-surface-variant">Mot de passe admin</label>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} required autoFocus
              className="mt-1 w-full px-3 py-2 bg-vault-surface-high border border-vault-outline-variant rounded-lg text-sm text-vault-on-surface focus:outline-none focus:border-vault-primary" />
          </div>
          <button type="submit" disabled={loading}
            className="w-full py-2.5 bg-gradient-to-r from-red-600 to-orange-600 hover:from-red-500 hover:to-orange-500 text-white text-sm font-medium rounded-lg transition-all disabled:opacity-50">
            {loading ? 'Connexion…' : 'Accéder'}
          </button>
        </form>
      </div>
    </div>
  )
}
