import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import { Button, Input } from '../components/ui'
import AuthShell from '../components/layout/AuthShell'

/** Same rule as the backend (validatePassword). */
const MIN_PASSWORD = 8

/** Reads the one-time token from `#token=…` (a fragment never reaches server logs). */
function tokenFromHash(hash: string): string {
  return new URLSearchParams(hash.replace(/^#/, '')).get('token') ?? ''
}

/**
 * New password from a reset link made on the server (`server reset-password <email>`).
 * Without a link, explains how to get one.
 */
export default function ResetPasswordPage() {
  const { resetPassword } = useAuth()
  const navigate = useNavigate()
  const [token] = useState(() => tokenFromHash(window.location.hash))
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    if (password.length < MIN_PASSWORD) return setError(`Le mot de passe doit contenir au moins ${MIN_PASSWORD} caractères.`)
    if (password !== confirm) return setError('Les deux mots de passe ne correspondent pas.')
    setLoading(true)
    const failure = await resetPassword(token, password)
    setLoading(false)
    if (failure) setError(failure)
    else {
      window.history.replaceState(null, '', window.location.pathname)
      navigate('/')
    }
  }

  const footer = (
    <Link to="/login" className="tv-link text-[14px]">
      Retour à la connexion
    </Link>
  )

  if (!token) {
    return (
      <AuthShell title="Mot de passe oublié" subtitle="Un lien de réinitialisation se demande à l'administrateur du site." onSubmit={(e) => e.preventDefault()} footer={footer}>
        <p className="text-[14px] text-ink-muted">
          L'administrateur génère un lien valable une heure avec la commande <code className="tv-serial">server reset-password ton@email</code>. Ouvre ce lien
          pour choisir un nouveau mot de passe.
        </p>
      </AuthShell>
    )
  }

  return (
    <AuthShell title="Nouveau mot de passe" subtitle="Choisis le mot de passe de ton compte TheVault." error={error} onSubmit={handleSubmit} footer={footer}>
      <Input label="Nouveau mot de passe" type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
      <Input label="Confirmer" type="password" autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required />
      <Button type="submit" size="lg" fullWidth disabled={loading || !password || !confirm}>
        {loading ? 'Enregistrement…' : 'Changer le mot de passe'}
      </Button>
    </AuthShell>
  )
}
