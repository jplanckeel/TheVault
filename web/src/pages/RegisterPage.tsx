import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import Logo from '../components/layout/Logo'

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    const err = await register(email, password, name)
    setLoading(false)
    if (err) setError(err)
    else navigate('/')
  }

  return (
    <div className="min-h-screen bg-vault-bg flex items-center justify-center p-4">
      <div className="w-full max-w-sm">
        <div className="flex flex-col items-center mb-8">
          <Logo className="w-16 h-16 mb-4" />
          <h1 className="text-xl font-bold bg-gradient-to-r from-purple-400 to-indigo-400 bg-clip-text text-transparent">TheVault</h1>
          <p className="text-sm text-vault-on-surface-variant mt-1">Crée ton compte gratuitement</p>
        </div>

        <form onSubmit={handleSubmit} className="bg-vault-surface border border-vault-outline-variant rounded-xl p-6 space-y-4">
          {error && <p className="text-sm text-vault-error">{error}</p>}
          <div>
            <label className="text-xs font-medium text-vault-on-surface-variant">Nom</label>
            <input type="text" value={name} onChange={e => setName(e.target.value)}
              className="mt-1 w-full px-3 py-2 bg-vault-surface-high border border-vault-outline-variant rounded-lg text-sm text-vault-on-surface focus:outline-none focus:border-vault-primary" />
          </div>
          <div>
            <label className="text-xs font-medium text-vault-on-surface-variant">Email</label>
            <input type="email" value={email} onChange={e => setEmail(e.target.value)} required
              className="mt-1 w-full px-3 py-2 bg-vault-surface-high border border-vault-outline-variant rounded-lg text-sm text-vault-on-surface focus:outline-none focus:border-vault-primary" />
          </div>
          <div>
            <label className="text-xs font-medium text-vault-on-surface-variant">Mot de passe</label>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} required minLength={6}
              className="mt-1 w-full px-3 py-2 bg-vault-surface-high border border-vault-outline-variant rounded-lg text-sm text-vault-on-surface focus:outline-none focus:border-vault-primary" />
          </div>
          <button type="submit" disabled={loading}
            className="w-full py-2.5 bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white text-sm font-medium rounded-lg transition-all disabled:opacity-50">
            {loading ? 'Création…' : 'Créer mon compte'}
          </button>
        </form>

        <p className="text-center text-sm text-vault-on-surface-variant mt-4">
          Déjà un compte ?{' '}
          <Link to="/login" className="text-vault-primary hover:underline">Se connecter</Link>
        </p>
      </div>
    </div>
  )
}
