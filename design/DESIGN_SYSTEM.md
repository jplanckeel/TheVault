# TheVault — Design System « Premium Security »

Source unique du design pour **le web (`web/`) et le mobile (`composeApp/`)**.
Référence visuelle : maquettes Stitch dans `design/stitch/*/screen.png`.
Dark mode uniquement.

> Remplace l'ancien thème mobile « Radiant Obsidian Vault » (`.agent/design/`), qui n'est plus utilisé.

| Plateforme | Implémentation des tokens |
|---|---|
| Web | `web/src/index.css` (`@theme`, préfixe `vault-`) |
| Mobile | `composeApp/src/commonMain/kotlin/com/thevault/ui/theme/` (`Color.kt`, `Typography.kt`, `Shape.kt`, `Spacing.kt`) |

Toute modification de token se fait **dans les deux implémentations et dans ce fichier**.

---

## 1. Principes

- **Obsidienne + lavande** : fonds neutres très sombres, un seul accent (lavande `primary`). Pas de dégradé violet→magenta.
- **Élévation tonale** : la hiérarchie se fait par paliers de surface (`surface-low` → `surface-highest`), avec au plus une bordure très discrète (`outline-variant` à 30 %).
- **Contenu d'abord** : les jaquettes et photos portent la couleur ; l'interface reste sobre.
- **Données réelles uniquement** : pas de chiffres décoratifs ou inventés. Si une donnée manque, on affiche `—` ou un état vide.
- **Même vocabulaire partout** : un composant porte le même nom et le même rendu sur web et mobile (cf. §6).

---

## 2. Couleurs

### Surfaces

| Token | Hex | Web | Compose |
|---|---|---|---|
| background | `#0f0f12` | `vault-bg` | `Background` |
| surface-lowest | `#0e0e11` | `vault-surface-lowest` | `SurfaceContainerLowest` |
| surface-low | `#1b1b1e` | `vault-surface-low` | `SurfaceContainerLow` |
| surface | `#1f1f22` | `vault-surface` | `SurfaceContainer` |
| surface-high | `#2a2a2d` | `vault-surface-high` | `SurfaceContainerHigh` |
| surface-highest | `#353438` | `vault-surface-highest` | `SurfaceContainerHighest` |
| surface-bright | `#39393c` | `vault-bright` | `SurfaceBright` |

Usage : `background` pour la page, `surface-low` pour les cartes, `surface-high` pour les champs et éléments interactifs, `surface-highest` pour les pop-overs et les puces neutres.

### Accent

| Token | Hex | Web | Compose |
|---|---|---|---|
| primary | `#d8b9ff` | `vault-primary` | `Primary` |
| primary-dim | `#ae72ff` | `vault-primary-dim` | `PrimaryDim` |
| primary-deep | `#7e28df` | `vault-primary-deep` | `PrimaryDeep` |
| on-primary | `#450086` | `vault-on-primary` | `OnPrimary` |
| secondary | `#e2b5ff` | `vault-secondary` | `Secondary` |
| secondary-container | `#60377c` | `vault-secondary-container` | `SecondaryContainer` |
| on-secondary-container | `#d5a6f4` | `vault-on-secondary-container` | `OnSecondaryContainer` |
| tertiary | `#cfc0e5` | `vault-tertiary` | `Tertiary` |

### Texte et contours

| Token | Hex | Web | Compose |
|---|---|---|---|
| on-surface | `#e4e1e6` | `vault-on-surface` | `OnSurface` |
| on-surface-variant | `#cec2d7` | `vault-on-surface-variant` | `OnSurfaceVariant` |
| outline | `#978da0` | `vault-outline` | `Outline` |
| outline-variant | `#4c4355` | `vault-outline-variant` | `OutlineVariant` |

Jamais de texte `#FFFFFF` pur : utiliser `on-surface`.

### Sémantique

| Token | Hex | Web | Compose |
|---|---|---|---|
| success | `#34d399` | `vault-success` | `Success` |
| warning | `#fbbf24` | `vault-warning` | `Warning` |
| info | `#60a5fa` | `vault-info` | `Info` |
| error | `#ffb4ab` | `vault-error` | `Error` |
| error-container | `#93000a` | `vault-error-container` | `ErrorContainer` |

Prix par condition : loose `#60a5fa`, CIB `#d8b9ff`, neuf `#34d399`.

### Couleurs de marque (placeholders sans jaquette)

| Marque | Fond | Détection (regex, insensible à la casse) |
|---|---|---|
| Xbox / Microsoft | `#107C10` | `xbox`, `microsoft` |
| Nintendo | `#E60012` | `nintendo`, `nes`, `snes`, `n64`, `gamecube`, `wii`, `switch`, `game boy`, `gba`, `ds`, `3ds` |
| PlayStation / Sony | `#0070CC` | `playstation`, `sony`, `ps1`…`ps5`, `psp`, `vita` |
| Autre | `surface-highest` | icône manette générique |

Logo blanc centré, à 50 % de la largeur.

---

## 3. Typographie

Deux familles : **Montserrat** (titres, chiffres clés) et **Inter** (texte, labels).
Web : Google Fonts. Mobile : `composeResources/font/` (licences OFL dans `design/fonts/`).

| Style | Famille | Taille / interligne | Graisse | Web | Compose |
|---|---|---|---|---|---|
| display-lg | Montserrat | 48/56 (mobile 32/40), -0.02em | 800 | `.text-display-lg` | `displayLarge` (32/40) |
| headline-md | Montserrat | 24/32 | 700 | `.text-headline-md` | `headlineMedium` |
| headline-sm | Montserrat | 20/28 | 600 | `.text-headline-sm` | `headlineSmall` |
| title | Inter | 16/24 | 600 | `text-base font-semibold` | `titleMedium` |
| body-md | Inter | 14/20 | 400 | défaut `body` | `bodyMedium` |
| body-sm | Inter | 12/16 | 400 | `text-xs` | `bodySmall` |
| label-bold | Inter | 12/16, +0.05em, MAJUSCULES | 700 | `.text-label-bold` | `labelMedium` |
| label-xs | Inter | 10/14, +0.1em, MAJUSCULES | 700 | `text-[10px] font-bold tracking-widest` | `labelSmall` |

Les montants utilisent des chiffres tabulaires (`tabular-nums` / `fontFeatureSettings = "tnum"`).

---

## 4. Formes, espacements, effets

### Rayons

| Token | Valeur | Usage |
|---|---|---|
| md | 8 | puces carrées, petits boutons-icônes |
| lg | 12 | boutons, champs, éléments de liste |
| xl | 16 | cartes standard |
| 2xl | 24 | cartes de jaquette, cartes KPI, sections |
| full | pill | puces (`Chip`), avatars |

Aucun angle vif ni rayon < 8 (hors pills).

### Espacements (base 4)

`4 · 8 · 12 · 16 · 20 · 24 · 32 · 40 · 48 · 64`. Marges d'écran : 16 (mobile), 32 (web desktop). Espace entre sections : 24 (mobile), 32 (web).

### Effets

- **Glow** : `0 0 15px rgba(216,185,255,0.3)` sur l'élément actif ou au survol (web). Sur mobile, pas d'ombre : on utilise un contour `primary` à 60 %.
- **Glass** : `rgba(36,26,54,0.4)` + flou 12 px, réservé à la barre de navigation et aux en-têtes collants.
- **Overlay de jaquette** : dégradé noir 85 % → transparent sur le bas de l'image (40 % de la hauteur).

---

## 5. Navigation et structure

Même architecture d'information sur les deux plateformes :

| Section | Web | Mobile |
|---|---|---|
| Dashboard (valeur, compteurs, derniers ajouts, répartition par plateforme) | sidebar | onglet 1 |
| Collection (Jeux · Consoles · Accessoires) | sidebar | onglet 2, puces de catégorie en haut |
| Ajouter | bouton CTA de la sidebar | bouton rond central de la barre d'onglets |
| Wishlist | sidebar | onglet 4 |
| Réglages (import / export CSV, synchro IGDB) | import CSV dans « Ma collection », synchros dans le back-office | onglet 5 |
| Explorer › Catalogue (catalogue partagé, mêmes filtres que la collection) | sidebar, section « Explorer » | — |
| Back-office (admin) | sidebar, section « Admin », visible si `is_admin` | — (web uniquement) |

- **Web** : sidebar fixe de 256 px (logo, sections, CTA « Ajouter », pied de page compte) + en-tête collant (recherche, avatar). En dessous de 768 px : barre supérieure compacte et barre d'onglets identique à l'app mobile (Dashboard · Collection · + · Wishlist · Catalogue).
- **Grilles Collection / Wishlist / Catalogue** : même composant et mêmes règles de filtre sur les deux plateformes (`web/src/lib/collectionView.ts` ↔ `CollectionModels.kt`) — type (`?type=` sur le web), recherche, plateforme, tri Récents / Valeur / A → Z.
- **Mobile** : barre d'onglets `surface-lowest` à 96 % avec un séparateur `outline-variant` 30 % (Compose n'a pas de flou d'arrière-plan natif), 4 onglets + bouton « Ajouter » central rond `primary`. Elle est masquée sur les écrans de détail et le formulaire. Chaque écran racine a un en-tête : titre `display-lg`, sous-titre de comptage en `body-md`.

---|---|---|
| Dashboard | sidebar | onglet 1 |
| Collection (Jeux · Consoles · Accessoires) | sidebar | onglet 2, sélecteur segmenté en haut |
| Ajouter | bouton CTA de la sidebar | bouton central de la barre d'onglets |
| Wishlist | sidebar | onglet 4 |
| Explorer (catalogue partagé) | sidebar, section « Explorer » | — |
| Statistiques / Marché | page Dashboard | onglet 5 |
| Back-office (admin) | sidebar, section « Admin », visible si `is_admin` | — (web uniquement) |

- **Web** : sidebar fixe de 256 px (logo, sections, CTA « Ajouter », pied de page compte) + en-tête collant (recherche, avatar). En dessous de 768 px : barre supérieure compacte.
- **Mobile** : barre d'onglets en verre (`surface-bright` 60 % + flou), 4 onglets + bouton « Ajouter » central rond `primary`. Chaque écran racine a un en-tête : titre `display-lg`, sous-titre de comptage en `body-md`.

---

## 6. Composants communs

| Composant | Web (`web/src/components/ui/`) | Compose (`ui/component/`) | Rendu |
|---|---|---|---|
| Card | `Card.tsx` | `VaultCard` | `surface-low`, rayon xl, bordure `outline-variant` 30 % |
| Button | `Button.tsx` | `VaultButton` | variantes `primary` (plein lavande), `ghost` (contour), `subtle` (`surface-high`), `danger` ; hauteurs 32/40/48 ; rayon lg |
| Chip | `Chip.tsx` | `VaultChip` | pill ; tons `neutral/primary/success/warning/error/info` ; variantes `soft/solid/outline` |
| Stat | `Stat.tsx` | `StatCard` | icône dans une pastille `primary` 15 %, label `label-bold`, valeur Montserrat 800 |
| GameCard | `GameCard.tsx` | `ItemCard` | jaquette 3:4, badge de condition en haut à droite, plateforme `label-xs` en `primary`, titre sur une ligne, prix + tendance |
| BrandPlaceholder | `BrandPlaceholder.tsx` | `BrandPlaceholder` | cf. §2 couleurs de marque |
| Section | `Section.tsx` | `SectionHeader` | titre `headline-sm` + action texte `primary` à droite |
| EmptyState | `EmptyState.tsx` | `EmptyState` | icône 48 dans un cercle `surface-high`, titre, texte, CTA optionnel |
| Input | `Input.tsx` | `VaultTextField` | `surface-high`, rayon lg, icône à gauche, focus = contour `primary` |
| Skeleton | `Skeleton.tsx` | `Skeleton` | `surface-high` animé |

### Badges de condition

| Condition | Libellé | Ton |
|---|---|---|
| neuf / scellé | `SEALED` | error, solid |
| CIB | `CIB` | neutral, solid |
| CIB sans notice | `CIB-` | neutral, solid |
| loose | `LOOSE` | neutral, solid |
| mint | `MINT` | success, solid |

### Back-office

- Pages `Données` et `Synchronisations` : même en-tête que les autres pages, précédé d'un sur-titre `label-bold` « Back-office » en `primary`.
- Tableaux : conteneur `surface-low` rayon 2xl, en-têtes `label-bold`, lignes séparées par `outline-variant` 15 %, survol `surface-high` 40 %. Sous 768 px, les lignes deviennent des cartes.
- Édition : panneau latéral droit (max 672 px, plein écran sur mobile), sections en cartes, zone dangereuse bordée `error` 30 %, suppression confirmée en deux temps (jamais de `window.confirm`).

| Problème de donnée | Libellé | Ton |
|---|---|---|
| duplicate | Doublon | error |
| missing_platform | Sans plateforme | warning |
| missing_cover | Sans jaquette | warning |
| missing_igdb | Sans IGDB | info |
| missing_price | Sans prix | neutral |

### Statuts de synchro (back-office)

| Statut | Libellé | Ton |
|---|---|---|
| pending | En attente | neutral |
| running | En cours | primary, animé |
| done | Terminé | success |
| failed | Échoué | error |

---

## 7. Langue et formats

- Interface en **français**.
- Montants en euros, format `fr-FR` (`1 234 €`), sans décimales dans les listes et avec 2 décimales sur la fiche détail.
- Dates : `19 oct. 2026`. Relatif (« il y a 3 h ») pour les synchros.
