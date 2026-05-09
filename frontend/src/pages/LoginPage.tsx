import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [password, setPassword] = useState('')
  const [error, setError] = useState(false)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(false)
    const ok = await login(password)
    setLoading(false)
    if (ok) {
      navigate('/jobs')
    } else {
      setError(true)
    }
  }

  return (
    <div className="min-h-screen bg-vault-bg flex items-center justify-center p-4">
      <form onSubmit={handleSubmit} className="w-full max-w-sm bg-vault-surface border border-vault-outline-variant rounded-xl p-8">
        <h1 className="text-xl font-bold text-vault-primary mb-1">TheVaultBO</h1>
        <p className="text-sm text-vault-on-surface-variant mb-6">Connexion admin</p>

        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Mot de passe"
          className="w-full px-4 py-2.5 rounded-lg border border-vault-outline-variant bg-vault-surface-low text-vault-on-surface text-sm placeholder:text-vault-on-surface-variant/50 mb-4"
          autoFocus
        />

        {error && <p className="text-xs text-vault-error mb-3">Mot de passe incorrect</p>}

        <button
          type="submit"
          disabled={loading}
          className="w-full py-2.5 rounded-lg text-sm font-semibold text-white disabled:opacity-50"
          style={{ background: 'linear-gradient(135deg, #8455EF, #EC63FF)' }}
        >
          {loading ? 'Connexion…' : 'Se connecter'}
        </button>
      </form>
    </div>
  )
}
