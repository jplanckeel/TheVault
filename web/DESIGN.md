# TheVaultBO — Design Guidelines

## Brand Placeholders

Quand un item n'a pas de `cover_url`, on affiche le logo du constructeur avec un fond de couleur de marque.

Composant : `web/src/components/BrandPlaceholder.tsx`

| Constructeur | Fichier source | Fichier public | Fond |
|---|---|---|---|
| **Xbox / Microsoft** | `logo/Xbox/Xbox_Symbol_9.svg` | `/public/logos/xbox.svg` | `#107C10` |
| **Nintendo** | `logo/Nintendo/Nintendo_Logo_3.svg` | `/public/logos/nintendo.svg` | `#E60012` |
| **PlayStation / Sony** | `logo/PlayStation®/PlayStation®_Logo_3.svg` | `/public/logos/playstation.svg` | `#0070CC` |

Le logo est rendu en blanc (`filter: brightness(0) invert(1)`) sur le fond coloré.

### Détection de plateforme (regex, insensible à la casse)

- **Xbox** : `xbox`, `microsoft`
- **Nintendo** : `nintendo`, `nes`, `snes`, `n64`, `gamecube`, `wii`, `switch`, `game boy`, `gba`, `ds`, `3ds`
- **PlayStation** : `playstation`, `sony`, `ps1`, `ps2`, `ps3`, `ps4`, `ps5`, `psp`, `vita`
- **Fallback** : SVG depuis `/public/controllercons/{platform-slug}.svg`, puis `joy-cons.svg`

### Usage

```tsx
import BrandPlaceholder from '../components/BrandPlaceholder'

{item.cover_url ? (
  <img src={item.cover_url} alt={item.title} className="w-full h-full object-cover" />
) : (
  <BrandPlaceholder platform={item.platform} className="w-full h-full" />
)}
```

---

## Cover Upload (Admin)

Survoler le thumbnail d'un item en mode admin affiche un overlay ✏️.  
Cliquer ouvre un file picker (JPEG, PNG, WebP, max 8 Mo).  
L'image est sauvegardée dans `{UPLOAD_DIR}/covers/{item-id}.ext` et servie via `/covers/`.

- Dev : `UPLOAD_DIR` = `./uploads` (créé automatiquement)
- Prod : configurer `STATIC_DIR` ou `UPLOAD_DIR` en variable d'environnement

---

## Palette de couleurs

Tokens CSS définis dans `web/src/index.css` (préfixe `vault-`).

| Token | Usage |
|---|---|
| `vault-primary` `#BA9EFF` | Accent violet |
| `vault-bg` | Fond principal |
| `vault-surface` | Surface carte / panel |
| `vault-on-surface` | Texte principal |
| `vault-on-surface-variant` | Texte secondaire |
| `vault-outline-variant` | Bordures |
| `vault-success` | Indicateurs positifs |
| `vault-error` | Erreurs / destructif |
| `vault-warning` | Avertissements |
