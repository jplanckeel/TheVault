import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import { Button, Input } from '../components/ui'
import AuthShell from '../components/layout/AuthShell'

/** Account creation. */
export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    const err = await register(email, password, name).catch(() => "L'inscription a échoué")
    setLoading(false)
    if (err) setError(err)
    else navigate('/')
  }

  return (
    <AuthShell
      title="Créer un compte"
      subtitle="Un compte pour ton coffre, sur le web et dans l'application."
      error={error}
      onSubmit={handleSubmit}
      footer={
        <>
          Déjà un compte ?{' '}
          <Link to="/login" className="tv-link text-[14px]">
            Se connecter
          </Link>
        </>
      }
    >
      <Input label="Pseudo" autoComplete="nickname" value={name} onChange={(e) => setName(e.target.value)} />
      <Input label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
      <Input label="Mot de passe" type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
      <Button type="submit" size="lg" fullWidth disabled={loading || !email || !password}>
        {loading ? 'Création…' : 'Créer mon compte'}
      </Button>
    </AuthShell>
  )
}
