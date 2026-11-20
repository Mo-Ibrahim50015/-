---
name: Serene Balance
colors:
  surface: '#f7f9fb'
  surface-dim: '#d8dadc'
  surface-bright: '#f7f9fb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f6'
  surface-container: '#eceef0'
  surface-container-high: '#e6e8ea'
  surface-container-highest: '#e0e3e5'
  on-surface: '#191c1e'
  on-surface-variant: '#45474c'
  inverse-surface: '#2d3133'
  inverse-on-surface: '#eff1f3'
  outline: '#75777d'
  outline-variant: '#c5c6cd'
  surface-tint: '#545f73'
  primary: '#091426'
  on-primary: '#ffffff'
  primary-container: '#1e293b'
  on-primary-container: '#8590a6'
  inverse-primary: '#bcc7de'
  secondary: '#006c4a'
  on-secondary: '#ffffff'
  secondary-container: '#82f5c1'
  on-secondary-container: '#00714e'
  tertiary: '#320008'
  on-tertiary: '#ffffff'
  tertiary-container: '#590015'
  on-tertiary-container: '#ff4e67'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d8e3fb'
  primary-fixed-dim: '#bcc7de'
  on-primary-fixed: '#111c2d'
  on-primary-fixed-variant: '#3c475a'
  secondary-fixed: '#85f8c4'
  secondary-fixed-dim: '#68dba9'
  on-secondary-fixed: '#002114'
  on-secondary-fixed-variant: '#005137'
  tertiary-fixed: '#ffdada'
  tertiary-fixed-dim: '#ffb3b6'
  on-tertiary-fixed: '#40000c'
  on-tertiary-fixed-variant: '#920028'
  background: '#f7f9fb'
  on-background: '#191c1e'
  surface-variant: '#e0e3e5'
typography:
  display:
    fontFamily: IBM Plex Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 44px
  headline-lg:
    fontFamily: IBM Plex Sans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 36px
  headline-lg-mobile:
    fontFamily: IBM Plex Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 32px
  headline-md:
    fontFamily: IBM Plex Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 26px
  body-lg:
    fontFamily: IBM Plex Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 26px
  body-md:
    fontFamily: IBM Plex Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 22px
  label-lg:
    fontFamily: IBM Plex Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  label-md:
    fontFamily: IBM Plex Sans
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 18px
  label-sm:
    fontFamily: IBM Plex Sans
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
  amount-display:
    fontFamily: IBM Plex Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  amount-body:
    fontFamily: IBM Plex Sans
    fontSize: 15px
    fontWeight: '600'
    lineHeight: 22px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  margin-tablet: 1.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system crafts an anxiety-free, mindful financial sanctuary tailored specifically for Arabic-first native mobile environments. Traditional finance interfaces overwhelm users with harsh contrasts, alarming reds, and rigid data density; this design system reorients expense tracking into a tranquil, purposeful ritual.

The visual direction marries **Modern Calming Minimalism** with soft physical presence—using subtle borders, warm white paper-like surfaces, and organic, grounding accents. 

### Design Principles
- **Clarity Over Clutter:** Eliminate non-essential ornamentation to keep cognitive load minimal during money management.
- **RTL-First Flow:** Visual hierarchy, structural alignments, gestural arcs, and numerical readouts are native to right-to-left optical reading patterns.
- **Supportive Nuance:** Financial gains and losses are communicated with dignified emerald and soft rose hues rather than punitive neon alerts, fostering confidence instead of guilt.

## Colors

The palette creates a serene, grounded atmosphere anchored by deep slate architecture, crisp white modular canvases, and purposeful semantic tones.

### Palette Hierarchy & Intent
- **Primary Canvas & Neutral Surfaces:** The base canvas operates on `#F8FAFC`, transitioning to `#F1F5F9` for nested recessed areas, and pure `#FFFFFF` for primary interaction cards.
- **Primary Brand Anchor (`#1E293B` & `#0F172A`):** Anchors typographic hierarchy, key active navigation nodes, and high-emphasis controls. It delivers the authority of institutional banking without its coldness.
- **Secondary / Positive Inflow (`#059669` & `#10B981`):** Muted botanical emerald denotes income, budget safety margins, and successful transactions.
- **Tertiary / Outflow & Expenses (`#E11D48` & `#F43F5E`):** A soft rose-coral signaling debits, approaching thresholds, and transactional outlays with clarity rather than panic.
- **Structural Outlines (`#E2E8F0`):** Extremely crisp, quiet borders that delineate modules cleanly against light backgrounds under high ambient sunlight.

## Typography

The type system is built on **IBM Plex Sans** (with Arabic glyph pairing), chosen for its architectural precision, open counter-forms, and balance between engineered clarity and humanistic rhythm.

### Script & Numerals
- **Bilingual Typographic Harmony:** The Arabic glyph set features generous vertical proportions. Ensure line-heights remain open (`1.4` to `1.6`) to prevent diacritics and ligatures from clipping.
- **Tabular Figures:** All monetary values and balance summaries must enable tabular lining figures (`font-variant-numeric: tabular-nums; tnum`) to keep columns and decimals vertically aligned across multiple transaction lines.
- **Currency Presentation:** The currency symbol (e.g., ر.س, د.إ, or ISO code) matches the label weight and sits at a secondary scale relative to the integer amount.

## Layout & Spacing

The layout model is tailored for mobile screens using a fluid single-column module structure with strict 4px/8px incremental rhythm.

### Layout Rules
- **Canvas Margins:** Mobile canvases maintain an outer margin of `1rem` (16px), while tablet viewports step up to `1.5rem` (24px) with maximum content containment widths of `640px` for focused data legibility.
- **Modular Cards:** Content units (balance overviews, monthly targets, breakdown charts) reside in full-width containers separated by `space-md` (`1rem`).
- **RTL Fluidity:** Asymmetrical paddings naturally invert: start padding represents right spacing, and end padding represents left spacing. Visual progression moves naturally from right to left.

## Elevation & Depth

This design system eschews harsh drop shadows and heavy elevation planes in favor of an **ambient surface tier system** complemented by hairline boundary definition.

### Depth Hierarchy
1. **Level 0 (Canvas):** Base background tinted in `#F8FAFC`. Zero elevation.
2. **Level 1 (Card Modules):** Pure white `#FFFFFF` surfaces featuring a composite depth profile:
   - Primary outline: `1px solid #E2E8F0`
   - Ambient drop: `box-shadow: 0 4px 16px -2px rgba(15, 23, 42, 0.04), 0 2px 6px -1px rgba(15, 23, 42, 0.02)`
3. **Level 2 (Popovers, Sticky Filters & Floating Nav):** Pure white or ultra-slight blur surfaces (`backdrop-filter: blur(8px)` with `rgba(255, 255, 255, 0.94)`):
   - Outline: `1px solid #CBD5E1`
   - Ambient drop: `box-shadow: 0 10px 25px -4px rgba(15, 23, 42, 0.08), 0 4px 10px -2px rgba(15, 23, 42, 0.03)`

## Shapes

The design system establishes a welcoming, organic geometry that balances contemporary precision with tactile comfort.

- **Base Radius (0.5rem / 8px):** Applied to form fields, nested micro-elements, segmented controls, and badges.
- **Card & Sheet Radius (1rem / 16px):** Applied to transaction modular cards, analytical surfaces, and quick-action bottom sheets.
- **Pill Radius (9999px):** Applied to categorical chips, state pills, action triggers, and active category selectors to contrast against rectangular cards.

## Components

### Buttons
- **Primary Action:** Solid `#1E293B` background with `#FFFFFF` text, `0.75rem` vertical by `1.5rem` horizontal padding, fully pill-shaped or `0.75rem` corner radius, tactile press state downscaled to `0.98`.
- **Secondary / Soft:** Tinted `#F1F5F9` background, `#1E293B` text, and zero border.
- **Income / Expense Quick Actions:** Circular icon buttons with subtle tint backdrops (`#059669` or `#E11D48` at 10% opacity) enclosing the colored icon.

### Chips & Category Selectors
- **Pill Filter Chips:** Height of `36px`, rounded pill geometry. 
  - *Inactive:* White surface, hairline `#E2E8F0` border, muted `#64748B` typography.
  - *Active:* Deep slate `#1E293B` fill, `#FFFFFF` text, accompanied by an optional right-aligned micro-icon in RTL.

### Transaction Row / Data Lists
- **Structure:** Modular horizontal rows within white containers.
  - Right: Category icon framed in a soft rounded square (`40px × 40px`) with 10% tinted background.
  - Center: Category title in `label-lg`, secondary note/merchant and time in `body-md` (`#64748B`).
  - Left: Numerical total in `amount-body` using tabular numbers. Positive entries styled in `#059669` with a leading `+`, expenses styled in `#1E293B` or `#E11D48` with a leading `-`.

### Form Fields & Money Inputs
- **Base Fields:** `#F8FAFC` fill with an understated `#E2E8F0` border that sharpens to `1.5px solid #1E293B` on focus.
- **Arabic Numeric Entry:** Large display text field aligned to the right (RTL), showing currency postfix or prefix positioned in fixed low-contrast slate `#94A3B8`.

### Segmented Controls & Toggles
- Recessed `#F1F5F9` pill container with a sliding pure white `#FFFFFF` segment pill elevated by a micro-shadow (`0 2px 4px rgba(0,0,0,0.06)`). Text seamlessly switches weight between regular (`400`) and semibold (`600`).