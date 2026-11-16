import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import { Button, Input } from '../components/ui'
import AuthShell from '../components/layout/AuthShell'

/** Sign-in with a TheVault account (the same one the mobile app uses for backups). */
export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    const ok = await login(email, password).catch(() => false)
    setLoading(false)
    if (ok) navigate('/')
    else setError('Email ou mot de passe incorrect')
  }

  return (
    <AuthShell
      title="Connexion"
      subtitle="Retrouve ton coffre et tes envies."
      error={error}
      onSubmit={handleSubmit}
      footer={
        <>
          Pas encore de compte ?{' '}
          <Link to="/register" className="tv-link text-[14px]">
            Créer un compte
          </Link>
        </>
      }
    >
      <Input label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
      <Input label="Mot de passe" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
      <Button type="submit" size="lg" fullWidth disabled={loading || !email || !password}>
        {loading ? 'Connexion…' : 'Se connecter'}
      </Button>
    </AuthShell>
  )
}
