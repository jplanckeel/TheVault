# TheVault — Design mobile « console »

Référence visuelle : [`maquettes-app-mobile.html`](maquettes-app-mobile.html) (18 écrans, deux coques).
Implémentation : `composeApp/src/commonMain/kotlin/com/thevault/ui/theme/` et `ui/component/`.

> L'application **mobile** utilise ce design. Le **web** reste en « Premium Security » (`design/DESIGN_SYSTEM.md`).
> L'ancien design « Nuit profonde » (`design/nuit-profonde/`) est conservé pour l'historique.

## Deux coques (`Color.kt`, `ThemeMode`)

L'utilisateur choisit dans **Réglages → Coque** : *Automatique* (suit le système), *Coque grise* (clair) ou *Black Edition* (sombre).
Les couleurs passent par `Vault.colors` (`LocalVaultColors`) — jamais de couleur codée en dur dans un écran.

| Jeton | Coque grise | Black Edition | Usage |
|---|---|---|---|
| `shell` | `#D6D3CC` | `#16151A` | Fond (plastique) |
| `panel` | `#EEECE7` | `#211F27` | Cartes, barre d'onglets, touches |
| `keycap` | `#FAF9F6` | `#2D2B34` | Segment actif |
| `well` | `#C3BFB7` | `#0E0D11` | Creux : sélecteurs, recherche, écran de courbe |
| `ink` / `inkMuted` / `inkFaint` | `#1F1D22` / `#4D4A51` / `#59565D` | `#ECEAF0` / `#A9A6B1` / `#8D8A96` | Texte |
| `indigo` / `indigoInk` | `#4A3AA8` / `#3F3196` | `#6E5CE6` / `#A99CFF` | Action unique (« bouton A »), liens, courbe |
| `lavender` | `#ABA3DD` | `#3D3756` | Bouton secondaire |
| `ledSync` / `ledPower` | `#2B9A55` / `#D9362E` | `#41D17A` / `#FF4D42` | LED : à jour / ancien, erreur |
| `gain` / `loss` | `#114F2B` / `#8A1F17` | `#5BD68A` / `#FF7B6E` | Hausse / baisse |
| `plat*` | Nintendo `#D7373D`, Sony `#2F62E0`, Sega `#0F8C95`, Xbox `#3A8F30`, autres `#8E6C47` | variantes claires | Bande constructeur |

Reliefs (`Surfaces.kt`) : `molded` (ombre douce en clair, liseré en sombre), `well` (creux), `faceShadow` (halo indigo), `floatShadow` (barre flottante).

## Typographie (`Typography.kt`, `Vault.type`)

**Archivo** (titres, gravures en largeur *Expanded*), **IBM Plex Sans** (texte), **IBM Plex Mono** (prix, numéros) — OFL, voir `design/fonts/OFL-Archivo-IBM-Plex.txt`.

| Style | Spéc. |
|---|---|
| `displayXl` | Archivo 800 40/44, chiffres tabulaires — valeur du coffre |
| `displayL` | Archivo 800 28/32 — titres d'écran |
| `displayM` | Archivo 800 18/24 — sections, plateformes |
| `engrave` | Archivo Expanded 700 10/12, majuscules, +1.6 — gravures |
| `body` / `bodyStrong` / `label` / `caption` / `small` | Plex Sans 15 · 15/600 · 13/600 · 13 · 12/500 |
| `monoPrice` / `monoTag` | Plex Mono 15/600 · 11/500 |

## Composants (`ui/component/`)

Rayons : xs 6, sm 10, md 14, lg 22 (panneaux), xl 30 (barre d'onglets), pilule (boutons, puces).

- `Panel`, `StatTile`, `PlatformTile` (bande constructeur), `ListRow`, `EmptyState`
- `VaultButton` (Primary indigo, Secondary lavande, Ghost, Danger — pilules 36/50/56), `KeyButton` 44, `FaceButton` (« + »)
- `VaultChip` (encrée quand sélectionnée, compteur mono), `SegmentedControl` (creux + touche), `SearchField`, `VaultTextField`
- `GameCover` (silhouette de cartouche : coins hauts arrondis, coins bas biseautés, bande constructeur), `CollectionCard`
- `ConditionTag` / `Pips` — 4 crans : Loose 1 · Complet 2 · Neuf 3 · Scellé 4
- `StatusLed`, `SourceBadge`, `PriceDelta` (▲ / ▼ mono), `Sparkline`, `LevelMeter`, `AchievementBadge`, `AchievementToast`, `VaultLogo`

Navigation : barre flottante *Accueil · Collection · + · Envies · Profil* ; Stats et Réglages s'ouvrent depuis le Profil.

## Données réelles uniquement

Jamais de chiffre inventé : ce qui manque s'affiche « — » ou est masqué.

| Élément | Source |
|---|---|
| Cote eBay (Loose / Complet / Neuf), « Plus fortes hausses », variations 30 j, rapport de mise à jour | Backend : annonces eBay FR + DE (UK si un taux `fx_gbp_eur` est configuré), lots / pour pièces / repros / NTSC exclus, médiane + nombre d'annonces (`ebay_pal`) |
| Fiche (jaquette, genre, année, studio) | Catalogue du backend (`/items/lookup` : enrichissement IGDB serveur, jaquettes et corrections du back-office), appliqué au démarrage (toutes les 12 h) et depuis Réglages |
| Valeur d'un exemplaire | Cote PAL fiable (≥ 3 annonces) de sa complétude dérivée, sinon valeur saisie, sinon prix US de l'import Gameye affiché « ≈ … cote US indicative » |
| Courbes | Valeur du coffre : instantané quotidien local (`value_snapshot`) ; fiche : historique backend complété par les relevés locaux (`price_snapshot`) |
| Plus-value | Valeur − prix payé − port |
| Niveau, XP, succès | Calculés depuis la collection (`screen/profile/Achievements.kt`) |
| Envies « sous ton prix » | Cote PAL de l'état visé ≤ prix max ; notification Android (WorkManager, toutes les 12 h) |

Non repris : scanner de codes-barres et mode inventaire par scan, numéro de série « SN », jeux « manquants » d'un catalogue complet de plateforme (remplacé par les listes de complétion personnelles).
