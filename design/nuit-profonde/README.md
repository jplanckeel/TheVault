# TheVault — Design mobile « Nuit profonde, un seul violet »

Référence visuelle : [`refonte-mobile.html`](refonte-mobile.html) (maquettes interactives de tous les écrans).
Implémentation : `composeApp/src/commonMain/kotlin/com/thevault/ui/theme/` et `ui/component/`.

> L'application **mobile** utilise ce design. Le **web** est encore sur « Premium Security » (`design/DESIGN_SYSTEM.md`).

## Couleurs (`Color.kt`)

| Rôle | Token Compose | Valeur |
|---|---|---|
| Fond | `Background` | `#060E20` |
| Barre de navigation | `SurfaceContainerLowest` | `#0B1530` |
| Carte | `SurfaceContainerLow` | `#0D1730` |
| Carte haute / options | `SurfaceContainerHigh` | `#141F38` |
| Piste (progress, segmented) | `SurfaceContainerHighest` | `#1A2547` |
| Segment sélectionné | `SurfaceBright` | `#22305A` |
| Bordure de carte | `OutlineVariant` / `CardBorderColor` | `#1E2A4D` |
| Contour (champs, options) | `Outline` | `#26335E` |
| Bouton contour | `OutlineStrong` | `#2E3B66` |
| Texte | `OnSurface` | `#DEE5FF` |
| Texte doux / atténué / clair | `OnSurfaceVariant` / `OnSurfaceMuted` / `OnSurfaceLight` | `#9AA3C2` / `#8E97B8` / `#C9D1F0` |
| Action (seul violet) | `Primary` / `OnPrimary` | `#BA9EFF` / `#1C0A47` |
| Plus-value / moins-value | `Success` / `Error` | `#6FE3C1` / `#FF9F7F` |
| Bandeau d'information | `HighlightContainer` / `HighlightBorder` / `OnHighlightVariant` | `#1B1840` / `#3A2F7A` / `#B9B2E0` |

Badges (`Badges.kt`) — une teinte par constructeur et par format :

| Badge | Fond | Texte | Bordure |
|---|---|---|---|
| Nintendo | `#3A1820` | `#FFB3B7` | `#5E2430` |
| Microsoft | `#12301C` | `#A9EDB3` | `#1F4D2C` |
| Sony | `#15244A` | `#B5CBFF` | `#24397A` |
| Loose | `#3A2A1C` | `#FFC58F` | — |
| CIB | `#2A2152` | `#CDBBFF` | — |
| Neuf / Possédé | `#123B36` | `#7FE8CB` | — |

## Typographie (`Typography.kt`)

Une seule famille : **Plus Jakarta Sans** (OFL, `design/fonts/PlusJakartaSans-OFL.txt`), chiffres tabulaires partout.

| Usage | Style Material | Taille / graisse |
|---|---|---|
| Montant | `displayLarge` | 40 / 800 |
| Titre d'écran | `headlineLarge` | 26 / 800 |
| Section | `headlineSmall` | 18 / 800 |
| Corps fort | `titleMedium` / `labelLarge` | 15 / 700 |
| Légende | `bodySmall` | 12 / 500 |
| Repère (eyebrow) | `labelSmall` | 11 / 700, majuscules, +0.08em |

## Formes et composants

- Rayons (`VaultRadius`) : badge 6, md 10, lg 12, bouton 14, xl 16, carte 18, xxl 20, pilule.
- Boutons 52dp, rayon 14 (`VaultButton`). Cartes : fond `SurfaceContainerLow` + bordure 1dp `CardBorderColor` (`VaultCard`).
- Barre du bas opaque, 5 entrées : Accueil, Collection, **+** (52dp, rayon 16), Wishlist, Stats. Masquée sur les fiches, l'ajout et les réglages.
- Les réglages (import / export CSV, IGDB) s'ouvrent depuis l'icône engrenage de l'Accueil.

## Icône « La cartouche »

Icône adaptative générée depuis le SVG de la maquette : `androidApp/src/main/res/drawable/ic_launcher_{background,foreground,monochrome}.xml`
(dégradé `#A78BFF → #6A3FE0` à 160°, couche monochrome pour les icônes à thème). Android 7 utilise `mipmap-anydpi/` (layer-list vectoriel).

## Non implémenté faute de données

Jamais de chiffre inventé : ces éléments de la maquette attendent une source de données.

- Courbe des ventes eBay et cote par format (Loose / CIB / Neuf) sur la fiche jeu
- Notes d'état Jeu / Boîte / Notice
- Prix cible et alerte de prix dans la wishlist (remplacés par « Cote estimée » et « Ajouté le »)
- Évolution mensuelle de la valeur
