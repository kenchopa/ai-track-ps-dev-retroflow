# RetroFlow design system

The tokens behind the web UI in `ui/`. They live in `ui/src/styles.css`, as CSS custom properties
and a Tailwind v4 `@theme`. Blueprint: `designs/retroflow-web-ui.md`.

## Direction

- **Aesthetic:** design-engineering (`tools.cc.skills.design-engineering`).
- **Dials:** **DESIGN_VARIANCE 5, MOTION_INTENSITY 4, VISUAL_DENSITY 5.** The skill's default
  (8/6/4) is meant for expressive marketing pages. RetroFlow is a tool a team opens once a
  sprint and must use without training, so it gets predictable placement, motion only to explain a
  change, and normal app density.
- **Mood:** calm, focused, trustworthy. A workbench, not a showroom.
- **Rules:**
  - one accent colour
  - no purple and no gradients, except the skeleton shimmer
  - no glow and no pure black
  - no serif and no Inter
  - no emoji
  - cards only where elevation means something (board items, dialogs)

## Colour

Every token has a light and a dark value; dark follows `prefers-color-scheme`. The contrast
ratios were **computed**, not estimated. They're the worst case across `surface` and `surface-2`,
in both themes.

| Token | Light | Dark | Use | Contrast |
|---|---|---|---|---|
| `canvas` | `#fafafa` | `#0c0c0e` | page background | |
| `surface` | `#ffffff` | `#17171a` | panels, cards, inputs | |
| `surface-2` | `#f4f4f5` | `#222226` | hover, muted wells, skeletons | |
| `line` | `#e4e4e7` | `#2c2c31` | dividers, card borders | |
| `line-strong` | `#d4d4d8` | `#3f3f46` | decorative dividers, dashed empty states only | (decorative) |
| `control` | `#8a8a92` | `#6b6b73` | **borders of inputs and secondary buttons** | 3.3:1 (WCAG 1.4.11 needs 3:1) |
| `fg` | `#18181b` | `#f4f4f5` | primary text | 14.4:1 |
| `fg-muted` | `#52525b` | `#b4b4bc` | secondary text | 7.0:1 |
| `fg-subtle` | `#6b6b72` | `#94949e` | meta, placeholders, counts | 4.8:1 |
| `accent` | `#0f766e` | `#2dd4bf` | primary actions, focus ring, links, action items | 5.0:1 |
| `accent-strong` | `#115e59` | `#5eead4` | accent hover and pressed | 7.6:1 |
| `accent-soft` / `accent-line` | `#f0fdfa` / `#99f6e4` | `#0f2a28` / `#134e4a` | open-retro highlight | |
| `positive` (+ `-soft`) | `#047857` | `#34d399` | "Went well" marker | 5.0:1 |
| `attention` (+ `-soft`) | `#b45309` | `#fbbf24` | "Needs improvement" marker, MEDIUM priority | 4.6:1 (4.8:1 on its soft badge) |
| `danger` (+ `-soft`) | `#be123c` | `#fb7185` | delete, errors, HIGH priority | 5.7:1 |
| `on-accent` / `on-danger` | `#ffffff` / `#ffffff` | `#042f2e` / `#2a0a12` | text on accent and danger buttons | 5.5:1 / 6.3:1 |

**Corrected during validation:**
- `fg-subtle` was `#71717a` (4.4:1 on `surface-2`, below AA).
- Input borders used `line-strong` (1.5:1, below the 3:1 a control boundary needs).
- The composer used a filled field with almost no boundary.

All three are fixed: `fg-subtle` is darker, inputs use the new `control` token, and the composer is
a normal bordered field.

The category colours are functional, not decorative. They always come with an icon and a text
label, so nothing relies on colour alone.

## Typography

- **Families:** `Geist Variable` (UI) and `Geist Mono Variable` (dates, ids, counts, priorities),
  self-hosted through `@fontsource-variable`.
- **Scale (Tailwind):**

  | Size | Use |
  |---|---|
  | `text-xs` 12/16 | meta, badges |
  | `text-sm` 14/20 | UI default |
  | `text-base` 16/24 | item content |
  | `text-lg` 18/28 | section titles |
  | `text-2xl` 24/32 `tracking-tight` | page titles |

  No display sizes: hierarchy comes from weight (`font-medium` / `font-semibold`) and colour
  (`fg` / `fg-muted` / `fg-subtle`).
- **Measure:** body text stays within `max-w-[65ch]`.

## Spacing, radii, shadows

- **Spacing:** Tailwind's 4 px base. Field blocks use `gap-2`, sections `gap-6` and page gutters
  `px-4 sm:px-6 lg:px-8`. Content is contained in `max-w-[1400px]`.
- **Radii:**

  | Token | Use |
  |---|---|
  | `sm` 6 px | badges |
  | `md` 10 px | inputs, buttons |
  | `lg` 14 px | cards |
  | `xl` 20 px | dialogs, panels |

- **Shadows,** tinted to the zinc base and never a glow:
  - `shadow-sm`: cards at rest
  - `shadow-md`: hover and raised panels
  - `shadow-lg`: dialogs

## Motion

| What | Spec |
|---|---|
| Layout moves (re-sort, resize) | spring `stiffness 100, damping 20, mass 1` |
| Press feedback | `scale 0.98` on `:active` |
| Hover and colour | 150 ms `cubic-bezier(0.16, 1, 0.3, 1)` |
| Dialog | 200 ms opacity + scale 0.98 to 1 |
| List entrance | 40 ms stagger, at most 8 items staggered |
| Open-retro status dot | 2.4 s opacity "breathing", the only perpetual motion, isolated in a memoized leaf |

Only `transform` and `opacity` are animated. `MotionConfig reducedMotion="user"` and CSS media
queries switch movement off for `prefers-reduced-motion`.

## Components

- **Primitives** in `ui/src/components/ui/`: Button, IconButton, TextField, TextArea,
  SelectField, SegmentedControl, Badge, Avatar, Skeleton, EmptyState, InlineAlert, Dialog,
  ConfirmDialog, Tabs, Toaster and StatusDot.
- **Required states:** every interactive component has hover, `focus-visible` (2 px accent ring
  with offset), active, and a disabled state with a reason. Every data view has loading
  (skeletons in the final layout), empty (what it means and how to fill it) and error (inline,
  in plain language).
- **Icons:** Phosphor (`@phosphor-icons/react`), weight `regular`, 18 px by default, set once
  through `IconContext`.
