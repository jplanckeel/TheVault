import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../AuthContext'

const LOGO_SVG = `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg"><defs><linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" style="stop-color:#9333ea"/><stop offset="100%" style="stop-color:#4f46e5"/></linearGradient></defs><path d="M20 30 L50 15 L80 30 L80 70 L50 85 L20 70 Z" fill="none" stroke="url(#g)" stroke-width="6" stroke-linejoin="round"/><path d="M35 45 L50 35 L65 45 L65 60 L50 70 L35 60 Z" fill="url(#g)"/><rect x="47" y="52" width="6" height="10" rx="3" fill="#fff" opacity="0.8"/></svg>`

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    const ok = await login(email, password)
    setLoading(false)
    if (ok) navigate('/')
    else setError('Email ou mot de passe incorrect')
  }

  return (
    <div className="min-h-screen bg-vault-bg flex items-center justify-center p-4">
      <div className="w-full max-w-sm">
        <div className="flex flex-col items-center mb-8">
          <div className="w-16 h-16 mb-4" dangerouslySetInnerHTML={{ __html: LOGO_SVG }} />
          <h1 className="text-xl font-bold bg-gradient-to-r from-purple-400 to-indigo-400 bg-clip-text text-transparent">TheVault</h1>
          <p className="text-sm text-vault-on-surface-variant mt-1">Connecte-toi à ton compte</p>
        </div>

        <form onSubmit={handleSubmit} className="bg-vault-surface border border-vault-outline-variant rounded-xl p-6 space-y-4">
          {error && <p className="text-sm text-vault-error">{error}</p>}
          <div>
            <label className="text-xs font-medium text-vault-on-surface-variant">Email</label>
            <input type="email" value={email} onChange={e => setEmail(e.target.value)} required
              className="mt-1 w-full px-3 py-2 bg-vault-surface-high border border-vault-outline-variant rounded-lg text-sm text-vault-on-surface focus:outline-none focus:border-vault-primary" />
          </div>
          <div>
            <label className="text-xs font-medium text-vault-on-surface-variant">Mot de passe</label>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} required
              className="mt-1 w-full px-3 py-2 bg-vault-surface-high border border-vault-outline-variant rounded-lg text-sm text-vault-on-surface focus:outline-none focus:border-vault-primary" />
          </div>
          <button type="submit" disabled={loading}
            className="w-full py-2.5 bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white text-sm font-medium rounded-lg transition-all disabled:opacity-50">
            {loading ? 'Connexion…' : 'Se connecter'}
          </button>
        </form>

        <p className="text-center text-sm text-vault-on-surface-variant mt-4">
          Pas encore de compte ?{' '}
          <Link to="/register" className="text-vault-primary hover:underline">Créer un compte</Link>
        </p>
      </div>
    </div>
  )
}
