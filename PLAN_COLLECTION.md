# TheVault — Plan d'implémentation : mieux suivre la collection

> Source : rapport « TheVault — Pistes pour mieux suivre la collection » (13 nov. 2026).
> À lire avec `.agent/AGENTS.md` et `.agent/workflows/new_feature.md`.

## Contexte

- App KMP (Compose Multiplatform, Decompose, Koin, SQLDelight, Ktor, Coil 3), Clean Architecture Domain → Data → Presentation.
- Qualité : Ktlint, Detekt (`config/detekt/detekt.yml`), Kover ≥ 80 % sur domain + data, Conventional Commits.
- Schéma actuel : `shared/src/commonMain/sqldelight/com/thevault/database/TheVault.sq` (+ migrations `1.sqm`, `2.sqm`).
- Modèles : `shared/src/commonMain/kotlin/com/thevault/domain/model/Models.kt`.
- Données de référence : `2026_03_19_ge_collection.csv` (export Gameye, 467 objets).

### Problèmes constatés

| Point | Aujourd'hui | Cible |
| --- | --- | --- |
| État | `ItemCondition` mélange état (MINT…POOR) et complétude (LOOSE, CIB, NEW_SEALED) | Complétude par composant + état par composant |
| Valeur | Un seul `current_value`, écrasé | Historique de cotes Loose / CIB / Neuf |
| Région | `Region` = PAL / NTSC_US / NTSC_JP / OTHER | Région + variante (FAH, UKV, EUR, NOE…) |
| Quantité | Non gérée | Plusieurs exemplaires par titre |
| Achat | `purchase_price` seul (vide pour 63 % de l'export) | Prix, port, date, source |
| Tags / emplacement | Absents | Tags libres + emplacement |
| Prêt / vente | Statuts LOANED / SOLD sans détails | Emprunteur + date retour ; prix et date de vente |
| eBay | Lien de recherche ebay.fr sur la fiche | Cotes stockées et historisées |

**Décision (14 nov. 2026) : toute la collection est PAL.** La colonne `Country` de l'export Gameye n'est pas fiable (315 objets marqués « United States of America » alors qu'ils sont PAL). Conséquences :
- à l'import, `region = PAL` par défaut, `Country` ignoré pour la région ; variante laissée vide, à compléter plus tard ;
- les cotes `PriceLoose` / `PriceCIB` / `PriceNew` de l'export sont des prix US (PriceCharting) : les importer avec `source = gameye_import_us`, ne jamais les présenter comme une cote PAL ;
- la cote PAL (eBay .fr / .de / .co.uk) devient la valeur de référence dès la phase 2.

---

## Phase 1 — Données fiables (prérequis de tout le reste)

### 1.1 Modèle domaine
- Séparer `ItemCondition` en :
  - `Grade` (MINT, VERY_GOOD, GOOD, FAIR, POOR)
  - composants : `hasMedia`, `hasBox`, `hasManual`, `hasExtras` + `Grade?` pour chacun
  - `isSealed: Boolean`
- `Completeness` (LOOSE, CIB, NEW) **dérivée** des composants (pas saisie).
- `Region` : garder l'enum + ajouter `regionVariant: String?` (FAH, UKV, EUR, NOE, USA, JPN…).
- Nouveaux champs : `barcode` (EAN/UPC), `productCode`, `edition` (STANDARD, PLAYERS_CHOICE, PLATINUM, COLLECTOR…), `quantity` ou une ligne par exemplaire (préférer **une ligne par exemplaire**).
- Achat : `purchasePrice`, `shippingCost`, `acquiredAt`, `acquiredFrom` (brocante, Leboncoin, eBay, Vinted, boutique…).
- `location: String?`, `tags` (table de jointure), `loanedTo`, `loanDueAt`, `soldPrice`, `soldAt`.
- Photos perso : table `item_photo` (item_id, uri, created_at).

### 1.2 Base de données
- Migration `3.sqm` : nouvelles colonnes / tables, conversion des anciennes valeurs `condition_` :
  - LOOSE → media seul ; CIB_NO_MANUAL → media + box ; CIB → media + box + manual ; NEW_SEALED → tout + `isSealed`
  - MINT…POOR → grade appliqué au media.
- Tests de migration obligatoires.

### 1.3 Import Gameye fidèle
- Adapter `CsvService` / `CsvUtils` pour mapper : `Ownership` (CIB, Loose, New, Boxed, CIB+, Loose+, Dématérialisé), `ItemCondition` / `BoxCondition` / `ManualCondition` (0–1 → Grade), `PriceLoose` / `PriceCIB` / `PriceNew` (→ premier snapshot de prix, phase 2), `PricePaid` (`-1.0` = inconnu), `Country` ignoré (tout en PAL, voir décision ci-dessus), `Tags`.
- Attention au format décimal à virgule (`"10,94"`) et aux dates françaises (`"28 nov. 2021"`).
- Test : réimport complet du CSV de référence sans perte.

### 1.4 UI minimale
- Formulaire d'ajout/édition : cases composants + état, région/variante, édition, prix payé rapide.
- Fiche : badge Loose / CIB / Neuf dérivé, tags, emplacement.

**Critère de fin :** le CSV Gameye se réimporte avec états par composant, régions et prix conservés ; Kover ≥ 80 % sur le nouveau code domain/data.

---

## Phase 2 — Valeur suivie

- Table `price_snapshot` : `id`, `catalog_ref` (titre + plateforme + région/variante, ou id IGDB), `date`, `price_loose`, `price_cib`, `price_new`, `currency`, `source` (ebay.fr, ebay.de, ebay.co.uk, gameye_import…), `sample_size`.
- Valeur d'un exemplaire = cote correspondant à sa complétude dérivée.
- Backend eBay : ventes **conclues** uniquement, filtre par site et région, exclusion des lots / « pour pièces », médiane + nombre de ventes (indice de confiance).
- Écrans : courbe de prix sur la fiche, courbe de la valeur totale, plus-value latente (valeur − prix payé − port) par objet / plateforme / total.
- Tri « plus fortes variations » sur 30 jours.
- Rapport après mise à jour des cotes (hausses en vert, baisses en rouge, total).
- Onglet « Vendu » : prix de vente, date, bénéfice réalisé.

**Question ouverte :** où vit le backend de scraping eBay (repo, API exposée) ? Il n'est pas dans ce dépôt.

---

## Phase 3 — Saisie et usage

- Scan code-barres (EAN) avec base EAN → jeu alimentée au fil des scans.
- Mode inventaire : scans en série, validation des états en lot.
- Édition en lot (région, emplacement, tags).
- Sets : complétion par plateforme / série / liste perso, filtre « manquants », coût estimé pour compléter.
- Wishlist : priorité, prix max, état visé ; alerte quand une annonce passe sous le prix ; avertissement doublon au scan.
- Mode hors ligne « je l'ai déjà ? » + cote locale.
- Stats : valeur, répartition plateforme / complétude / région, achats par mois, top 10.
- Partage en lecture (collection ou wishlist seule), backup / synchro via backend Go, export assurance (PDF/CSV), option masquer les prix.
- Backlog : statut joué / en cours / fini / 100 %, temps de jeu, note.

---

## Méthode

1. Une branche par sous-étape (`feat/condition-components`, `feat/migration-3`, `feat/gameye-import`…).
2. Suivre `.agent/workflows/new_feature.md`, puis `/check_quality`.
3. Commencer par 1.1 → 1.2 → 1.3 ; ne pas toucher à l'UI avant que la migration soit testée.

---

## État d'avancement (14 nov. 2026)

Implémenté sur `feat/collection-tracking`, sauf le scan de codes-barres et le mode inventaire par scan.

| Étape | Commit | Notes |
| --- | --- | --- |
| 1.1 + 1.2 Modèle et migration | `d58f6b5` | Migration `4.sqm` (la `3.sqm` existait déjà), tests de migration sur SQLite JVM |
| 1.3 Import Gameye | `3107023` | Aller-retour sans perte testé sur le CSV de référence ; `PricePaid = -1.0` n'est plus importé comme −1 € |
| 1.4 UI minimale | `e2e9468` | Formulaire par composant, édition des consoles / accessoires |
| 2 Backend eBay | `79fef97` | API Browse = **annonces en cours** (pas de ventes conclues : Marketplace Insights est sur demande) |
| 2 Valeur suivie | `62c3378` | Historique, plus-value avec port, rapport, onglet « Vendus » |
| 3 Données | `c09e8af` | Migration `6.sqm` : envies, backlog, listes |
| 3 Envies / backlog | `e429a7c` | Alerte Android uniquement |
| 3 Listes de complétion | `b462817` | Listes personnelles (pas de catalogue complet par plateforme) |
| 3 Lot, « Je l'ai déjà ? » | `c038d22` | |
| 3 Sauvegarde / partage | `a86d537`, `b154034` | Migration backend `000010` ; export assurance PDF sur Android, CSV ; iOS : pas de sélecteur de fichiers |
| Couverture | `cf29d24` | domain + data : 83,8 % |

**Réponse à la question ouverte :** le backend de scraping est dans ce dépôt (`backend/internal/scraper`).
