# Backoffice Redesign — Vault Design System

**Date:** 2026-04-14
**Scope:** Full frontend redesign with Tailwind CSS, replacing @semcore/ui visually, aligned with TheVault mobile design system.

---

## Design System

### Couleurs (from TheVault mobile Color.kt)

| Token | Hex | Usage |
|---|---|---|
| Surface | `#060E20` | Background principal |
| SurfaceContainerLow | `#091328` | Sidebar |
| SurfaceContainer | `#0F1930` | Cartes |
| SurfaceContainerHigh | `#141F38` | Cartes élevées |
| SurfaceContainerHighest | `#192540` | Modals / hover |
| Primary | `#BA9EFF` | Accent violet principal |
| PrimaryDim | `#8455EF` | Début gradient CTA |
| Tertiary | `#EC63FF` | Fin gradient CTA |
| OnSurface | `#DEE5FF` | Texte principal |
| OnSurfaceVariant | `#A3AAC4` | Texte secondaire |
| SecondaryContainer | `#444173` | Fond badges |
| OnSecondaryContainer | `#CFCBFF` | Texte badges |
| OutlineVariant | `#40485D` | Bordures |
| Error | `#FF6E84` | Erreurs |

### Prix — couleurs par condition

| Condition | Couleur | Usage |
|---|---|---|
| `loose` | `#3B82F6` (bleu) | Prix cartouche + texte condition |
| `cib` | `#BA9EFF` (violet) | Prix cartouche + texte condition |
| `new` | `#10B981` (vert) | Prix cartouche + texte condition |

### Typographie

- **Font** : Plus Jakarta Sans (Google Fonts)
- Titres de page : semibold, `#DEE5FF`
- Label section : ALL-CAPS, tracking large, `#A3AAC4`
- Texte secondaire : `#A3AAC4`

---

## Layout Global

- **Sidebar gauche** fixe (`w-64`) : fond `#091328`, border-right `#40485D`
  - Logo "TheVaultBO" en haut avec accent violet
  - Navigation : Catalogue / Jobs
  - Lien actif : texte `#BA9EFF`, bg `#0F1930`
- **Zone contenu** : fond `#060E20`, padding `2rem`
- Approche : Tailwind CSS full replacement de @semcore/ui

---

## Page Catalogue

- Header : label `CATALOGUE` ALL-CAPS + titre "Catalogue"
- Bouton "Trigger Scrape" : gradient `#8455EF → #EC63FF`, radius `8px`
- Items en **grille de cartes** (`grid-cols-1 md:grid-cols-2 lg:grid-cols-3`)
  - Fond `#0F1930`, border `#40485D`, radius `12px`
  - Hover : border `#BA9EFF` + glow `0 0 0 1px #BA9EFF`
  - Titre `#DEE5FF` semibold, platform/type `#A3AAC4`, badge région

---

## Page Item Detail

- Bouton retour → Catalogue
- Header : titre item + badges platform/type/region/condition (`#444173` / `#CFCBFF`)
- **Cartes prix par condition** (3 cards côte à côte) :
  - Médiane en grand (colorée selon condition)
  - Min / Max en secondaire
  - Badge sample count
- **Historique** en table : date, condition (colorée), min/médiane/max/samples

---

## Page Jobs

- Header : label `SCRAPE JOBS` + titre
- Table avec colonnes : Started At, Triggered By, Status, Progress, Error
- **Badges statut** :
  - `pending` : `#6D758C`
  - `running` : `#BA9EFF` + animation pulse
  - `done` : `#10B981`
  - `failed` : `#FF6E84`
- Barre de progression `items_done / items_total`
- Erreur en `#FF6E84`
- Auto-refresh toutes les 5s (conservé)

---

## Fichiers à modifier

- `frontend/index.html` — ajouter Google Fonts (Plus Jakarta Sans)
- `frontend/package.json` + config — installer Tailwind CSS v4
- `frontend/src/App.tsx` — nouveau Layout avec sidebar
- `frontend/src/pages/CataloguePage.tsx` — grille de cartes
- `frontend/src/pages/ItemDetailPage.tsx` — cartes prix colorées + table historique
- `frontend/src/pages/JobsPage.tsx` — badges statut + barre progression
