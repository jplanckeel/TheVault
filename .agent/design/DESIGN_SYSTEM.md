# TheVault — Design System
> Source: Stitch MCP — Project ID `10534585831624995760`
> Thème : **"The Radiant Obsidian Vault"** — Dark Mode, Mobile, Plus Jakarta Sans

---

## 1. Vision créative

**North Star : "The Digital Curator"**

L'expérience doit ressembler à une galerie-musée privée et souterraine où des objets rares sont éclairés par une technologie bioluminescente. On abandonne l'esthétique chargée et bruyante des plateformes gaming classiques pour un rendu éditorial haut de gamme.

**Principes directeurs** :
- **Asymétrie intentionnelle** : sections hero avec typographie display chevauchant l'art du jeu, effet 3D.
- **Floating clusters** : pas de grilles rigides, les blocs de contenu "flottent" dans le fond océanique.
- **Tonal Carving** : pas de bordures 1px. L'espace est défini par les décalages de teinte de surface.

---

## 2. Palette de couleurs

### Couleurs de surface (hiérarchie)

| Niveau | Token | Valeur | Usage |
|--------|-------|--------|-------|
| Level 0 — Base | `surface` / `background` | `#060e20` | Fond principal |
| Level 1 — Sections | `surface_container_low` | `#091328` | Grandes régions de contenu |
| Level 2 — Cards | `surface_container` | `#0f1930` | Composants interactifs |
| Level 2+ — Cards élevées | `surface_container_high` | `#141f38` | Cards avec priorité |
| Level 3 — Pop-overs | `surface_container_highest` | `#192540` | Éléments flottants haute priorité |
| Bright | `surface_bright` | `#1f2b49` | Barres de navigation glassmorphiques |

### Couleurs primaires

| Token | Valeur | Usage |
|-------|--------|-------|
| `primary` | `#ba9eff` | Accents, textes importants |
| `primary_container` | `#ae8dff` | CTAs secondaires |
| `primary_dim` | `#8455ef` | Début du gradient CTA |
| `on_primary` | `#39008c` | Texte sur fond primary |
| `on_primary_container` | `#2b006e` | Texte sur primary_container |

### Couleurs secondaires

| Token | Valeur | Usage |
|-------|--------|-------|
| `secondary` | `#c7c3fe` | Éléments secondaires |
| `secondary_container` | `#444173` | Fond des status chips |
| `on_secondary_container` | `#cfcbff` | Texte des status chips |

### Couleurs tertiaires (accentuation)

| Token | Valeur | Usage |
|-------|--------|-------|
| `tertiary` | `#ec63ff` | Fin du gradient CTA, "heat nodes" critiques |
| `tertiary_container` | `#de4bf4` | Fond tertiary |
| `on_tertiary` | `#3d0047` | Texte sur tertiary |

### Couleurs de texte

| Token | Valeur | Usage |
|-------|--------|-------|
| `on_surface` | `#dee5ff` | Texte principal (JAMAIS blanc pur) |
| `on_surface_variant` | `#a3aac4` | Texte secondaire / metadata |
| `on_background` | `#dee5ff` | Texte sur fond |

### Couleurs utilitaires

| Token | Valeur | Usage |
|-------|--------|-------|
| `outline` | `#6d758c` | Contours discrets |
| `outline_variant` | `#40485d` | Ghost borders (15% opacity) |
| `surface_tint` | `#ba9eff` | Radial glow (30% opacity) |
| `error` | `#ff6e84` | Erreurs |
| `on_error_container` | `#ffb2b9` | Texte erreur sur fond sombre |

### Couleur signature

```
#8B5CF6  — Violet signature "Obsidian Vault"
```

---

## 3. Typographie

**Police unique : Plus Jakarta Sans** (Google Fonts)
- Headline font : Plus Jakarta Sans
- Body font : Plus Jakarta Sans
- Label font : Plus Jakarta Sans

### Échelle typographique

| Style | Taille | Letter Spacing | Couleur | Usage |
|-------|--------|---------------|---------|-------|
| Display lg/md | `3.5rem` / `2.8rem` | `-0.02em` | `on_surface` | Titres de jeux, stats vault |
| Headline lg | `2rem` | `-0.02em` | `primary` accent | Headers de catégorie |
| Headline sm | `1.5rem` | `-0.01em` | `on_surface` | Titres de section |
| Title md | `1.125rem` | `0` | `on_surface` | Titres de cards |
| Body lg | `1rem` | `0` | `on_surface` | Corps principal |
| Body md | `0.875rem` | `0` | `on_surface_variant` | Metadata secondaire |
| Label md/sm | `0.75rem` | `+0.05em` | `on_surface_variant` | ALL-CAPS : specs techniques (PLATFORM, FPS…) |

---

## 4. Élévation et profondeur

### Règle du "Tonal Layering"
Pas de drop shadows traditionnelles. L'élévation se matérialise par superposition de surfaces.

```
surface (base) 
  └── surface_container_low (sections)
        └── surface_container (cards)
              └── surface_container_highest (modals, pop-overs)
```

### Neon Wash Shadow (modals flottantes)
```
box-shadow: 0px 20px 40px rgba(139, 92, 246, 0.08)
```

### Ghost Border (fallback accessibilité)
```
border: 1px solid rgba(64, 72, 93, 0.15)  /* outline_variant @ 15% */
```

### Glassmorphism (navigation, menus flottants)
```
background: rgba(31, 43, 73, 0.60)   /* surface_bright @ 60% */
backdrop-filter: blur(24px)
```

---

## 5. Composants

### Boutons

**Primary (CTA principal)**
```kotlin
// Gradient 135°: primary_dim → tertiary
background = Brush.linearGradient(
    colors = listOf(Color(0xFF8455EF), Color(0xFFEC63FF)),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)
cornerRadius = 12.dp   // "md" = 0.75rem
textColor = Color(0xFF000000)  // on_primary_fixed
```

**Secondary (glassmorphique)**
```kotlin
background = Color.White.copy(alpha = 0.10f)
border = BorderStroke(1.dp, Color(0xFF40485D).copy(alpha = 0.15f))  // Ghost Border
```

### Game Cards
- Fond : `surface_container_low` (`#091328`)
- Hover/Focus : fond → `surface_container_highest` + outer glow `primary`
- Coins : `8dp` (roundedness "DEFAULT")
- **INTERDICTION de dividers** — utiliser `Spacing-8` (16dp) entre blocs
- Art du jeu peut **déborder de la card** (effet premium)

### Vault Status Chips
```kotlin
// Shape : pill (cornerRadius = full)
backgroundColor = Color(0xFF444173)   // secondary_container
textColor = Color(0xFFCFCBFF)         // on_secondary_container
```
Statuts définis : `OWNED` | `WISHLIST` | `SOLD` | `LOANED`

### Champs de saisie (Sunken style)
```kotlin
backgroundColor = Color(0xFF000000)   // surface_container_lowest
border = BorderStroke.None
// Focus state : bottom glow 1px en primary
focusIndicator = Color(0xFFBA9EFF)    // primary
```

### Barre de progression "Aether"
```kotlin
trackColor = Color(0xFF192540)        // surface_variant
progressColor = Color(0xFFEC63FF)     // tertiary
// Leading-edge glow en tertiary
```

### Bottom Navigation Bar
```kotlin
background = Color(0x991F2B49)        // surface_bright @ 60%
backdropBlur = 24.dp
```

### Aura Effect (featured game titles)
```kotlin
// Radial glow derrière les titres mis en avant
color = Color(0xFFBA9EFF).copy(alpha = 0.30f)  // surface_tint @ 30%
```

---

## 6. Espacement

L'échelle de spacing utilise `spacingScale = 2` (base 8dp) :

| Token | Valeur dp |
|-------|-----------|
| Spacing-1 | 4dp |
| Spacing-2 | 8dp |
| Spacing-4 | 16dp |
| Spacing-6 | 24dp |
| Spacing-8 | 32dp |
| Spacing-10 | 40dp |
| Spacing-12 | 48dp |
| Spacing-16 | 64dp |
| Spacing-20 | 80dp |

**Entre sections majeures** : Spacing-16 ou Spacing-20 (effet galerie).

---

## 7. Règles absolues (Do's & Don'ts)

### ✅ DO
- Utiliser `on_surface` (`#dee5ff`) pour tout texte — jamais `#FFFFFF`
- Utiliser des coins ≥ 8dp (roundedness `DEFAULT` ou `md`)
- Espacement vertical extrême entre sections majeures
- Laisser l'art du jeu déborder des containers
- Utiliser `tertiary` (`#ec63ff`) sparingly pour les nœuds interactifs les plus critiques

### ❌ DON'T
- **Jamais de bordures 1px solides** pour sectionner du contenu
- **Jamais de blanc pur** (`#FFFFFF`) comme couleur de texte
- **Jamais de coins à 90°** — minimum 8dp partout
- **Jamais de dividers standards** — utiliser un décalage de fond à la place
- **Jamais d'ombres "web 2.0"** — utiliser le tonal layering

---

## 8. Écrans identifiés (navigation)

| Screen ID | Titre | Description |
|-----------|-------|-------------|
| `c37bf5b35e6e42aaa21980add04510e8` | **Tableau de Bord** | Dashboard principal, stats de la collection |
| `43009fc650ca4286ab82424968450048` | **Ma Collection** | Liste complète des jeux |
| `1b5489ef9279461d96961a0e91e82e2c` | **Mes Consoles** | Liste des consoles |
| `7b255bc3bf864c559657c3d7da3571db` | **Mes Accessoires** | Liste des accessoires |
| `8af581b278a9441f958a78d63dab7b30` | **Détails du Jeu** | Page détail d'un jeu |
| `e2ec4f63935342b091099322682bb15c` | **Types de Jeux** | Catégories / genres |
| `817d050faf41479b9dfb34b5d9b0b33a` | **Suivi des Prix** | Tracker de valeur de la collection |
| `140a3c082b524cfebbc853fc3c55a338` | **Ajouter un Item** | Formulaire d'ajout (jeu/console/accessoire) |

### Navigation proposée
```
Bottom Navigation Bar (4 onglets) :
├── Dashboard (Tableau de Bord)
├── Collection (Ma Collection / Mes Consoles / Mes Accessoires)
├── Types (Types de Jeux)
└── Suivi des Prix
```
FAB ou bouton dédié → **Ajouter un Item**

---

## 9. Stitch MCP — Comment consulter un écran

Pour obtenir les détails d'un écran spécifique avant implémentation :

```
Tool: mcp_StitchMCP_get_screen
projectId: 10534585831624995760
screenId: <id de l'écran ci-dessus>
```

**OBLIGATOIRE** : toujours consulter l'écran Stitch correspondant via MCP avant d'implémenter un composable.
