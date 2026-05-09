# Backoffice Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesigner le frontend du backoffice avec Tailwind CSS v4 en suivant le design system "Radiant Obsidian Vault" de l'app mobile TheVault.

**Architecture:** Tailwind CSS v4 installé via plugin Vite, couleurs Vault définies comme custom theme dans `index.css`. Remplacement complet des inline styles et @semcore/ui par des classes Tailwind. Chaque page est un composant autonome.

**Tech Stack:** React 18, TypeScript, Tailwind CSS 4.2.2, @tailwindcss/vite 4.2.2, Plus Jakarta Sans (Google Fonts), react-router-dom v7

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `frontend/package.json` | Modify | Ajouter tailwindcss + @tailwindcss/vite |
| `frontend/vite.config.ts` | Modify | Ajouter plugin tailwindcss |
| `frontend/index.html` | Modify | Google Fonts + titre |
| `frontend/src/index.css` | Create | Tailwind v4 @import + @theme Vault |
| `frontend/src/main.tsx` | Modify | Importer index.css |
| `frontend/src/App.tsx` | Rewrite | Sidebar layout |
| `frontend/src/pages/CataloguePage.tsx` | Rewrite | Grille de cartes items |
| `frontend/src/pages/ItemDetailPage.tsx` | Rewrite | Cartes prix colorées + historique |
| `frontend/src/pages/JobsPage.tsx` | Rewrite | Table jobs + badges statut |

---

## Task 1 : Installer Tailwind CSS v4

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/vite.config.ts`
- Create: `frontend/src/index.css`
- Modify: `frontend/src/main.tsx`
- Modify: `frontend/index.html`

- [ ] **Step 1: Installer les paquets**

```bash
cd frontend && npm install tailwindcss @tailwindcss/vite
```

- [ ] **Step 2: Ajouter le plugin dans vite.config.ts**

```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
```

- [ ] **Step 3: Créer src/index.css avec le thème Vault**

```css
@import "tailwindcss";

@theme {
  --font-sans: 'Plus Jakarta Sans', sans-serif;

  /* Surfaces */
  --color-vault-bg: #060E20;
  --color-vault-surface-low: #091328;
  --color-vault-surface: #0F1930;
  --color-vault-surface-high: #141F38;
  --color-vault-surface-highest: #192540;
  --color-vault-bright: #1F2B49;

  /* Accents */
  --color-vault-primary: #BA9EFF;
  --color-vault-primary-dim: #8455EF;
  --color-vault-tertiary: #EC63FF;
  --color-vault-secondary-container: #444173;
  --color-vault-secondary-text: #CFCBFF;

  /* Text */
  --color-vault-on-surface: #DEE5FF;
  --color-vault-on-surface-variant: #A3AAC4;

  /* Borders */
  --color-vault-outline: #6D758C;
  --color-vault-outline-variant: #40485D;

  /* Semantic */
  --color-vault-error: #FF6E84;
  --color-vault-success: #10B981;
  --color-vault-info: #3B82F6;

  /* Prix par condition */
  --color-price-loose: #3B82F6;
  --color-price-cib: #BA9EFF;
  --color-price-new: #10B981;
}

body {
  background-color: var(--color-vault-bg);
  color: var(--color-vault-on-surface);
  font-family: var(--font-sans);
}
```

- [ ] **Step 4: Importer index.css dans main.tsx**

```tsx
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(<App />)
```

- [ ] **Step 5: Ajouter Google Fonts et titre dans index.html**

```html
<!doctype html>
<html lang="fr">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>TheVaultBO</title>
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet" />
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 6: Vérifier que le build passe**

```bash
cd frontend && npm run build
```
Expected: build réussi sans erreurs.

- [ ] **Step 7: Commit**

```bash
git add frontend/package.json frontend/package-lock.json frontend/vite.config.ts frontend/src/index.css frontend/src/main.tsx frontend/index.html
git commit -m "feat: install Tailwind CSS v4 with Vault design system theme"
```

---

## Task 2 : Layout avec sidebar (App.tsx)

**Files:**
- Rewrite: `frontend/src/App.tsx`

- [ ] **Step 1: Réécrire App.tsx avec sidebar Vault**

```tsx
import { BrowserRouter, NavLink, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import CataloguePage from './pages/CataloguePage'
import ItemDetailPage from './pages/ItemDetailPage'
import JobsPage from './pages/JobsPage'

function Sidebar() {
  return (
    <aside className="fixed top-0 left-0 h-screen w-64 flex flex-col bg-vault-surface-low border-r border-vault-outline-variant z-10">
      <div className="px-6 py-6 border-b border-vault-outline-variant">
        <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-1">Backoffice</p>
        <h1 className="text-lg font-bold text-vault-primary">TheVaultBO</h1>
      </div>
      <nav className="flex flex-col gap-1 px-3 py-4 flex-1">
        <NavLink
          to="/"
          end
          className={({ isActive }) =>
            `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
              isActive
                ? 'bg-vault-surface text-vault-primary'
                : 'text-vault-on-surface-variant hover:bg-vault-surface hover:text-vault-on-surface'
            }`
          }
        >
          <span>📦</span>
          Catalogue
        </NavLink>
        <NavLink
          to="/jobs"
          className={({ isActive }) =>
            `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
              isActive
                ? 'bg-vault-surface text-vault-primary'
                : 'text-vault-on-surface-variant hover:bg-vault-surface hover:text-vault-on-surface'
            }`
          }
        >
          <span>⚙️</span>
          Scrape Jobs
        </NavLink>
      </nav>
      <div className="px-6 py-4 border-t border-vault-outline-variant">
        <p className="text-xs text-vault-on-surface-variant">The Vault — Admin</p>
      </div>
    </aside>
  )
}

function Layout() {
  return (
    <div className="min-h-screen bg-vault-bg">
      <Sidebar />
      <main className="ml-64 min-h-screen p-8">
        <Outlet />
      </main>
    </div>
  )
}

function NotFound() {
  return (
    <div className="flex items-center justify-center h-64">
      <p className="text-vault-on-surface-variant text-lg">404 — Page not found</p>
    </div>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<CataloguePage />} />
          <Route path="items/:id" element={<ItemDetailPage />} />
          <Route path="jobs" element={<JobsPage />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
```

- [ ] **Step 2: Vérifier le build**

```bash
cd frontend && npm run build
```
Expected: build réussi.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/App.tsx
git commit -m "feat: add Vault sidebar layout"
```

---

## Task 3 : Page Catalogue (grille de cartes)

**Files:**
- Rewrite: `frontend/src/pages/CataloguePage.tsx`

- [ ] **Step 1: Réécrire CataloguePage.tsx**

```tsx
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item } from '../types'

function PageHeader({ onScrape, scraping }: { onScrape: () => void; scraping: boolean }) {
  return (
    <div className="flex items-start justify-between mb-8">
      <div>
        <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-2">
          Backoffice
        </p>
        <h1 className="text-2xl font-semibold text-vault-on-surface">Catalogue</h1>
      </div>
      <button
        onClick={onScrape}
        disabled={scraping}
        className="px-4 py-2 rounded-lg text-sm font-semibold text-white disabled:opacity-50 disabled:cursor-not-allowed transition-opacity"
        style={{ background: 'linear-gradient(135deg, #8455EF, #EC63FF)' }}
      >
        {scraping ? 'Scraping…' : 'Trigger Scrape'}
      </button>
    </div>
  )
}

function ItemCard({ item, onClick }: { item: Item; onClick: () => void }) {
  return (
    <div
      onClick={onClick}
      className="bg-vault-surface border border-vault-outline-variant rounded-xl p-5 cursor-pointer transition-all hover:border-vault-primary hover:shadow-[0_0_0_1px_#BA9EFF]"
    >
      <div className="flex items-start justify-between mb-3">
        <h3 className="text-sm font-semibold text-vault-on-surface leading-snug flex-1 mr-2">
          {item.title}
        </h3>
        {item.region && (
          <span className="shrink-0 text-xs px-2 py-0.5 rounded-full bg-vault-secondary-container text-vault-secondary-text font-medium">
            {item.region}
          </span>
        )}
      </div>
      <div className="flex items-center gap-2 flex-wrap">
        <span className="text-xs text-vault-on-surface-variant">{item.platform}</span>
        {item.platform && item.type && (
          <span className="text-vault-outline text-xs">·</span>
        )}
        <span className="text-xs text-vault-on-surface-variant capitalize">{item.type}</span>
      </div>
      {item.status && (
        <div className="mt-3 pt-3 border-t border-vault-outline-variant">
          <span className="text-xs text-vault-on-surface-variant">{item.status}</span>
        </div>
      )}
    </div>
  )
}

export default function CataloguePage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [scraping, setScraping] = useState(false)
  const [scrapeMsg, setScrapeMsg] = useState<string | null>(null)

  useEffect(() => {
    api.items.list()
      .then(setItems)
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false))
  }, [])

  async function triggerScrape() {
    setScraping(true)
    setScrapeMsg(null)
    try {
      const job = await api.scrape.trigger('user')
      setScrapeMsg(`Job démarré (ID: ${job.id})`)
    } catch (e) {
      setScrapeMsg(`Erreur: ${String(e)}`)
    } finally {
      setScraping(false)
    }
  }

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <p className="text-vault-on-surface-variant animate-pulse">Chargement…</p>
    </div>
  )

  if (error) return (
    <div className="rounded-xl bg-vault-surface border border-vault-error/30 p-4">
      <p className="text-vault-error text-sm">Erreur : {error}</p>
    </div>
  )

  return (
    <div>
      <PageHeader onScrape={triggerScrape} scraping={scraping} />

      {scrapeMsg && (
        <div className="mb-6 px-4 py-3 rounded-lg bg-vault-surface border border-vault-outline-variant">
          <p className="text-sm text-vault-primary">{scrapeMsg}</p>
        </div>
      )}

      {items.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-64 text-center">
          <p className="text-vault-on-surface-variant">Aucun item pour l'instant.</p>
          <p className="text-xs text-vault-outline mt-2">Lance un scrape pour commencer.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {items.map((item) => (
            <ItemCard
              key={item.id}
              item={item}
              onClick={() => navigate(`/items/${item.id}`)}
            />
          ))}
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Vérifier le build**

```bash
cd frontend && npm run build
```
Expected: build réussi.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/CataloguePage.tsx
git commit -m "feat: redesign CataloguePage with Vault card grid"
```

---

## Task 4 : Page Item Detail (cartes prix + historique)

**Files:**
- Rewrite: `frontend/src/pages/ItemDetailPage.tsx`

- [ ] **Step 1: Réécrire ItemDetailPage.tsx**

```tsx
import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Item, PriceSnapshot } from '../types'

const CONDITION_STYLE: Record<string, { color: string; bg: string; label: string }> = {
  loose: { color: '#3B82F6', bg: 'rgba(59,130,246,0.12)', label: 'Loose' },
  cib:   { color: '#BA9EFF', bg: 'rgba(186,158,255,0.12)', label: 'CIB' },
  new:   { color: '#10B981', bg: 'rgba(16,185,129,0.12)', label: 'New' },
}

function PriceCard({ condition, snapshots }: { condition: string; snapshots: PriceSnapshot[] }) {
  const style = CONDITION_STYLE[condition] ?? { color: '#A3AAC4', bg: 'rgba(163,170,196,0.12)', label: condition }
  const latest = snapshots[0]
  if (!latest) return null

  return (
    <div
      className="rounded-xl p-5 border flex-1 min-w-0"
      style={{ borderColor: style.color + '40', background: style.bg }}
    >
      <div className="flex items-center justify-between mb-4">
        <span className="text-xs font-semibold uppercase tracking-widest" style={{ color: style.color }}>
          {style.label}
        </span>
        <span className="text-xs px-2 py-0.5 rounded-full bg-vault-surface-highest text-vault-on-surface-variant">
          {latest.sample_count} ventes
        </span>
      </div>
      <p className="text-3xl font-bold mb-1" style={{ color: style.color }}>
        {latest.price_median.toFixed(2)} {latest.currency}
      </p>
      <p className="text-xs text-vault-on-surface-variant">médiane</p>
      <div className="flex gap-4 mt-4 pt-4 border-t" style={{ borderColor: style.color + '30' }}>
        <div>
          <p className="text-xs text-vault-on-surface-variant">Min</p>
          <p className="text-sm font-medium text-vault-on-surface">{latest.price_min.toFixed(2)} {latest.currency}</p>
        </div>
        <div>
          <p className="text-xs text-vault-on-surface-variant">Max</p>
          <p className="text-sm font-medium text-vault-on-surface">{latest.price_max.toFixed(2)} {latest.currency}</p>
        </div>
      </div>
    </div>
  )
}

function PriceHistoryTable({ prices }: { prices: PriceSnapshot[] }) {
  if (prices.length === 0) return (
    <p className="text-vault-on-surface-variant text-sm">Aucune donnée de prix.</p>
  )

  return (
    <div className="overflow-x-auto rounded-xl border border-vault-outline-variant">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-vault-outline-variant bg-vault-surface">
            {['Date', 'Condition', 'Min', 'Médiane', 'Max', 'Ventes'].map((h) => (
              <th key={h} className="text-left px-4 py-3 text-xs font-medium tracking-wider text-vault-on-surface-variant uppercase">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {prices.map((p) => {
            const style = CONDITION_STYLE[p.condition] ?? { color: '#A3AAC4', label: p.condition }
            return (
              <tr key={p.id} className="border-b border-vault-outline-variant/50 hover:bg-vault-surface/50 transition-colors">
                <td className="px-4 py-3 text-vault-on-surface-variant">
                  {new Date(p.scraped_at).toLocaleDateString('fr-FR')}
                </td>
                <td className="px-4 py-3">
                  <span className="text-xs font-semibold uppercase" style={{ color: style.color }}>
                    {style.label}
                  </span>
                </td>
                <td className="px-4 py-3 text-vault-on-surface">{p.price_min.toFixed(2)} €</td>
                <td className="px-4 py-3 font-semibold text-vault-on-surface">{p.price_median.toFixed(2)} €</td>
                <td className="px-4 py-3 text-vault-on-surface">{p.price_max.toFixed(2)} €</td>
                <td className="px-4 py-3 text-vault-on-surface-variant">{p.sample_count}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

export default function ItemDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [item, setItem] = useState<Item | null>(null)
  const [prices, setPrices] = useState<PriceSnapshot[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    Promise.all([api.items.get(id), api.items.prices(id)])
      .then(([item, prices]) => { setItem(item); setPrices(prices) })
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <p className="text-vault-on-surface-variant animate-pulse">Chargement…</p>
    </div>
  )
  if (error) return (
    <div className="rounded-xl bg-vault-surface border border-vault-error/30 p-4">
      <p className="text-vault-error text-sm">Erreur : {error}</p>
    </div>
  )
  if (!item) return <p className="text-vault-on-surface-variant">Item introuvable.</p>

  // Group prices by condition, sorted by date desc
  const byCondition: Record<string, PriceSnapshot[]> = {}
  for (const p of prices) {
    if (!byCondition[p.condition]) byCondition[p.condition] = []
    byCondition[p.condition].push(p)
  }

  const badges = [
    { label: item.platform },
    { label: item.type },
    { label: item.region },
    { label: item.condition },
  ].filter((b) => b.label)

  return (
    <div>
      <button
        onClick={() => navigate('/')}
        className="flex items-center gap-2 text-sm text-vault-on-surface-variant hover:text-vault-primary transition-colors mb-6"
      >
        ← Retour au catalogue
      </button>

      <div className="mb-8">
        <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-2">
          Item
        </p>
        <h1 className="text-2xl font-semibold text-vault-on-surface mb-4">{item.title}</h1>
        <div className="flex flex-wrap gap-2">
          {badges.map((b) => (
            <span
              key={b.label}
              className="text-xs px-3 py-1 rounded-full bg-vault-secondary-container text-vault-secondary-text font-medium"
            >
              {b.label}
            </span>
          ))}
        </div>
      </div>

      {Object.keys(byCondition).length > 0 && (
        <div className="mb-8">
          <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-4">
            Prix actuels
          </p>
          <div className="flex gap-4 flex-wrap">
            {['loose', 'cib', 'new'].map((cond) =>
              byCondition[cond] ? (
                <PriceCard key={cond} condition={cond} snapshots={byCondition[cond]} />
              ) : null
            )}
          </div>
        </div>
      )}

      <div>
        <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-4">
          Historique des prix
        </p>
        <PriceHistoryTable prices={prices} />
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Vérifier le build**

```bash
cd frontend && npm run build
```
Expected: build réussi.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/ItemDetailPage.tsx
git commit -m "feat: redesign ItemDetailPage with color-coded price cards"
```

---

## Task 5 : Page Jobs (badges statut + barre de progression)

**Files:**
- Rewrite: `frontend/src/pages/JobsPage.tsx`

- [ ] **Step 1: Réécrire JobsPage.tsx**

```tsx
import { useEffect, useState } from 'react'
import { api } from '../api'
import type { ScrapeJob } from '../types'

const STATUS_CONFIG: Record<string, { color: string; bg: string; label: string; pulse?: boolean }> = {
  pending: { color: '#6D758C', bg: 'rgba(109,117,140,0.15)', label: 'En attente' },
  running: { color: '#BA9EFF', bg: 'rgba(186,158,255,0.15)', label: 'En cours', pulse: true },
  done:    { color: '#10B981', bg: 'rgba(16,185,129,0.15)', label: 'Terminé' },
  failed:  { color: '#FF6E84', bg: 'rgba(255,110,132,0.15)', label: 'Échoué' },
}

function StatusBadge({ status }: { status: string }) {
  const cfg = STATUS_CONFIG[status] ?? { color: '#A3AAC4', bg: 'rgba(163,170,196,0.15)', label: status }
  return (
    <span
      className={`inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full ${cfg.pulse ? 'animate-pulse' : ''}`}
      style={{ color: cfg.color, background: cfg.bg }}
    >
      <span className="w-1.5 h-1.5 rounded-full" style={{ background: cfg.color }} />
      {cfg.label}
    </span>
  )
}

function ProgressBar({ done, total }: { done: number; total: number }) {
  const pct = total > 0 ? Math.round((done / total) * 100) : 0
  return (
    <div className="flex items-center gap-3 min-w-[120px]">
      <div className="flex-1 h-1.5 rounded-full bg-vault-outline-variant overflow-hidden">
        <div
          className="h-full rounded-full transition-all"
          style={{ width: `${pct}%`, background: 'linear-gradient(90deg, #8455EF, #EC63FF)' }}
        />
      </div>
      <span className="text-xs text-vault-on-surface-variant whitespace-nowrap">
        {done}/{total}
      </span>
    </div>
  )
}

export default function JobsPage() {
  const [jobs, setJobs] = useState<ScrapeJob[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  function loadJobs() {
    return api.scrape.list()
      .then(setJobs)
      .catch((e) => setError(String(e)))
  }

  useEffect(() => {
    loadJobs().finally(() => setLoading(false))
    const interval = setInterval(loadJobs, 5000)
    return () => clearInterval(interval)
  }, [])

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <p className="text-vault-on-surface-variant animate-pulse">Chargement…</p>
    </div>
  )

  if (error) return (
    <div className="rounded-xl bg-vault-surface border border-vault-error/30 p-4">
      <p className="text-vault-error text-sm">Erreur : {error}</p>
    </div>
  )

  return (
    <div>
      <div className="mb-8">
        <p className="text-xs font-medium tracking-widest text-vault-on-surface-variant uppercase mb-2">
          Backoffice
        </p>
        <h1 className="text-2xl font-semibold text-vault-on-surface">Scrape Jobs</h1>
      </div>

      {jobs.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-64 text-center">
          <p className="text-vault-on-surface-variant">Aucun job pour l'instant.</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-vault-outline-variant">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-vault-outline-variant bg-vault-surface">
                {['Démarré le', 'Déclenché par', 'Statut', 'Progression', 'Erreur'].map((h) => (
                  <th key={h} className="text-left px-4 py-3 text-xs font-medium tracking-wider text-vault-on-surface-variant uppercase">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {jobs.map((job) => (
                <tr key={job.id} className="border-b border-vault-outline-variant/50 hover:bg-vault-surface/50 transition-colors">
                  <td className="px-4 py-3 text-vault-on-surface-variant">
                    {new Date(job.started_at).toLocaleString('fr-FR')}
                  </td>
                  <td className="px-4 py-3 text-vault-on-surface">{job.triggered_by}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={job.status} />
                  </td>
                  <td className="px-4 py-3">
                    <ProgressBar done={job.items_done} total={job.items_total} />
                  </td>
                  <td className="px-4 py-3 text-vault-error text-xs max-w-xs truncate">
                    {job.error ?? ''}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Vérifier le build**

```bash
cd frontend && npm run build
```
Expected: build réussi.

- [ ] **Step 3: Commit final**

```bash
git add frontend/src/pages/JobsPage.tsx
git commit -m "feat: redesign JobsPage with Vault status badges and progress bar"
```
