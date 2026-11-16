# TheVault web — design

The web app uses the same « Collector System » design system as the mobile app
(design system artifact « TheVault », mobile reference `design/console/`).

- **Tokens**: `src/index.css` — both shells (*Coque grise* light, *Black Edition* dark) as CSS
  variables, exposed to Tailwind (`bg-panel`, `text-ink-muted`, `bg-indigo`…). The shell follows
  the system or the choice made in the sidebar (`data-theme`, stored in `localStorage`).
- **Component classes**: `tv-*` in `@layer components` (so Tailwind utilities still win).
- **Components**: `src/components/ui/` — `Logo`, `SnesDots`, `StatusLed`, `GameCover` (cartridge
  corner + manufacturer band), `CollectionCard`, `PlatformTile`, `ListRow`, `SegmentedControl`,
  `PlatformBadge`, `ConditionTag`, `SourceBadge`, `PriceDelta`, `Button`, `IconButton`, `Input`…
- **Fonts**: Archivo, IBM Plex Sans and Mono, bundled in `public/fonts/` (OFL).
- **Rules**: one indigo action per screen; never a hard-coded colour in a page; unknown values
  render as `—`; prices come from the backend eBay quotes only.

Covers without an image show the title on the blank cartridge (`--cover-blank`); manufacturer
logos for platform rows are in `public/logos/` (tinted with the ink colour through a CSS mask).
Uploaded covers are stored in `{UPLOAD_DIR}/covers/{item-id}.ext` and served under `/covers/`.
