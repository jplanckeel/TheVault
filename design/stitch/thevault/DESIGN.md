---
name: TheVault
colors:
  surface: '#131316'
  surface-dim: '#131316'
  surface-bright: '#39393c'
  surface-container-lowest: '#0e0e11'
  surface-container-low: '#1b1b1e'
  surface-container: '#1f1f22'
  surface-container-high: '#2a2a2d'
  surface-container-highest: '#353438'
  on-surface: '#e4e1e6'
  on-surface-variant: '#cec2d7'
  inverse-surface: '#e4e1e6'
  inverse-on-surface: '#303033'
  outline: '#978da0'
  outline-variant: '#4c4355'
  surface-tint: '#d8b9ff'
  primary: '#d8b9ff'
  on-primary: '#450086'
  primary-container: '#ae72ff'
  on-primary-container: '#3c0076'
  inverse-primary: '#7e28df'
  secondary: '#e2b5ff'
  on-secondary: '#451d61'
  secondary-container: '#60377c'
  on-secondary-container: '#d5a6f4'
  tertiary: '#cfc0e5'
  on-tertiary: '#362b48'
  tertiary-container: '#988bad'
  on-tertiary-container: '#2f2541'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#eddcff'
  primary-fixed-dim: '#d8b9ff'
  on-primary-fixed: '#290055'
  on-primary-fixed-variant: '#6200bc'
  secondary-fixed: '#f3daff'
  secondary-fixed-dim: '#e2b5ff'
  on-secondary-fixed: '#2e014b'
  on-secondary-fixed-variant: '#5d357a'
  tertiary-fixed: '#ebdcff'
  tertiary-fixed-dim: '#cfc0e5'
  on-tertiary-fixed: '#201632'
  on-tertiary-fixed-variant: '#4d4160'
  background: '#131316'
  on-background: '#e4e1e6'
  surface-variant: '#353438'
typography:
  display-lg:
    fontFamily: Montserrat
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 56px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Montserrat
    fontSize: 32px
    fontWeight: '800'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Montserrat
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  headline-sm:
    fontFamily: Montserrat
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-bold:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.05em
  label-mono:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 8px
  container-max: 1440px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 48px
---

## Brand & Style
The brand identity centers on the intersection of high-end security and the vibrant energy of gaming culture. It is designed to evoke a sense of digital permanence, "the vault" being a secure, impenetrable space for high-value assets. 

The aesthetic is **Modern-Sleek with Glassmorphic accents**, characterized by deep obsidian surfaces, sharp technical precision, and rhythmic pulses of violet light. It avoids the cluttered tropes of traditional "gaming" UIs in favor of a sophisticated, data-first environment that feels like a premium financial tool reimagined for the modern collector. The UI should feel immersive, expensive, and reactive.

## Colors
The palette is rooted in a "Deep Dark" foundation to prioritize content immersion and reduce eye strain during long cataloging sessions.

*   **Primary (Amethyst/Electric Purple):** Used for primary actions, progress indicators, and data peaks. It represents the "energy" of the platform.
*   **Secondary (Soft Lavender):** Used for interactive states, secondary buttons, and text highlights. It provides necessary contrast against dark backgrounds.
*   **Tertiary (Midnight Purple):** A deep, desaturated violet used for surface containers and subtle layering.
*   **Neutral (Obsidian):** The true-black and near-black shades used for the base canvas and structural elements.
*   **Semantic Accents:** Use a vibrant emerald for positive price movement and a hot crimson for market dips, both desaturated to maintain the dark-mode harmony.

## Typography
The typography system balances the bold, geometric impact of **Montserrat** for headings with the systematic clarity of **Inter** for data-heavy interfaces. 

Headlines utilize a heavy weight and tight letter-spacing to feel authoritative and "tech-forward." Body text is optimized for readability against dark backgrounds, using a slightly increased line height and regular weights to prevent "haloing" (text appearing to glow/blur). Use `label-mono` specifically for price data, serial numbers, and platform tags to reinforce the technical nature of the application.

## Layout & Spacing
This design system utilizes a **12-column fluid grid** for desktop and a **4-column grid** for mobile. The rhythm is built on an 8px base unit.

*   **Desktop:** Content is centered within a 1440px max-width container. Large lateral margins (48px) are used to create a "premium" sense of space.
*   **Mobile:** Margins shrink to 16px. Cards and data tables should switch to a stacked vertical format or a horizontally scrollable list where data integrity is paramount.
*   **Density:** For the collection manager, use "Compact" spacing (8px between items); for the market dashboard, use "Spacious" spacing (24px) to allow price charts to breathe.

## Elevation & Depth
Depth is created through **Tonal Layering and Glassmorphism** rather than traditional shadows. 

1.  **Level 0 (Base):** Pure obsidian (#0F0F12).
2.  **Level 1 (Surface):** Tertiary purple (#241A36) at 40% opacity with a 12px backdrop blur.
3.  **Level 2 (Active/Floating):** Tertiary purple at 80% opacity with a subtle 1px inner-border in Soft Lavender (20% opacity).
4.  **Active States:** Elements should have a `0px 0px 15px` outer glow using the Primary Electric Purple at 30% opacity to simulate a "powered-on" hardware feel.

## Shapes
The shape language is "Squircle" inspired—modern and approachable but structured. 

Standard components (buttons, input fields) use a 0.5rem (8px) radius. Larger containers like game cards or dashboard modules use a 1rem (16px) radius. This "Rounded" approach softens the technical aesthetic, making the "Vault" feel like a high-end consumer product rather than a sterile database.

## Components
*   **Buttons:** Primary buttons use a solid Electric Purple fill with white text. Secondary buttons use a "Ghost" style: a Soft Lavender border (1px) and text, which fills with color only on hover.
*   **Cards:** Game cards feature high-quality box art. Information is housed in a glassmorphic footer overlaying the bottom 30% of the image.
*   **Price Charts:** Use clean vector lines (not filled areas) for price history. The line should glow slightly. The X/Y axes should be minimal, using `label-mono` typography.
*   **Chips/Tags:** Small, pill-shaped indicators for "Mint Condition," "Complete in Box," or "Digital." Use low-opacity background tints of the secondary color.
*   **Input Fields:** Deep-set appearance with a 1px border that glows Electric Purple when focused. Labels should always be visible above the field using `label-bold`.
*   **Progress Bars:** Thin, high-contrast bars used for "Collection Completion" or "Value Growth" metrics, featuring a subtle gradient from Tertiary to Primary purple.