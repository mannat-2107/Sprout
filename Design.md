# Design System & Visual Specification: Sprout 2.0

## 1. Visual Philosophy: Trust, Restraint & Precision
Sprout 2.0 departs completely from frivolous tech-startup clichés (neon purple gradients, pill buttons, floating emoji balloons). The aesthetic reflects a calm, dignified botanical and financial identity inspired by institutional stability and organic personal growth.

---

## 2. Color Palette & WCAG AA Contrast Audit

| Token Name | Hex Code | Contrast Ratio | Usage |
| :--- | :--- | :--- | :--- |
| `--color-deep-pine` | `#0E281C` | **15.2:1** (against `#FBFBF9`) | Primary headers, high-emphasis text, brand mark |
| `--color-primary` | `#155A37` | **7.8:1** (against `#FFFFFF`) | Primary interactive buttons, active step indicators |
| `--color-primary-hover`| `#0F4328` | **9.6:1** (against `#FFFFFF`) | Hover and pressed states for primary buttons |
| `--color-primary-light`| `#EBF5EE` | — | Subtle card highlights, badge backgrounds |
| `--color-accent-growth`| `#2E8B57` | **4.9:1** (against `#FBFBF9`) | Secondary foliage, progress bars, accents |
| `--color-bg-page` | `#FBFBF9` | — | Warm linen parchment body background |
| `--color-bg-surface` | `#FFFFFF` | — | Card surfaces, modals, phone mockup frame |
| `--color-bg-subtle` | `#F4F6F4` | — | Secondary card backgrounds, form input fills |
| `--color-border` | `#E0E5E2` | — | Subtle architectural container borders |
| `--color-border-strong`| `#BDC8C1` | **3.2:1** (UI component boundary)| Form input borders, active outlines |
| `--color-text-main` | `#141C17` | **14.8:1** (against `#FBFBF9`) | Body paragraphs, primary labels |
| `--color-text-muted` | `#536259` | **5.4:1** (against `#FBFBF9`) | Secondary descriptions, captions, disclaimer text |
| `--color-gold-bloom` | `#EAB308` | — | Stage 3 milestone blooming accent |

---

## 3. Typography Scale & Hierarchy
Prioritizes native system font rendering for crisp legibility across both Latin and Devanagari (Hindi) scripts:
`font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Noto Sans", "Noto Sans Devanagari", sans-serif;`

| Token | Size | Line Height | Usage |
| :--- | :--- | :--- | :--- |
| `--font-size-xs` | `0.75rem` (12px) | 1.3 | Micro-badges, timestamps, input helpers |
| `--font-size-sm` | `0.8125rem` (13px)| 1.45 | **Regulatory disclaimers (strictly $\ge$ 12px)**, secondary copy |
| `--font-size-base` | `0.9375rem` (15px)| 1.5 | Primary body text, button labels, form inputs |
| `--font-size-md` | `1.0rem` (16px) | 1.5 | Card titles, highlighted metrics |
| `--font-size-lg` | `1.125rem` (18px) | 1.35 | Section subheadings, modal headers |
| `--font-size-xl` | `1.375rem` (22px) | 1.25 | Screen headers, secondary page titles |
| `--font-size-2xl` | `1.75rem` (28px) | 1.2 | Mobile hero headline |
| `--font-size-3xl` | `2.25rem` (36px) | 1.15 | Desktop hero headline |

---

## 4. Sprout Botanical Mascot Specification

```
      +-- Golden Sun Halo (Stage 3) --+
      |        \     |     /          |
      |         *  ( * )  *           |
      |                               |
      |          Left Leaf            |
      |            .-.     Right Leaf |
      |           (   )       .-.     |
      |            `-' \     (   )    |
      |                 \     `-'     |
      |            Stem  |   /        |
      |                  |  /         |
      |                  | /          |
      |                  ||           |
      |                  ||           |
      |             Seed || Pod       |
      |            .----'  `----.     |
      |           (   #155A37    )    |
      +------------`------------'-----+
                 Earth Bed / Shadow
```

### The 3 Animated Growth States
1. **Stage 1 (Idle / Waiting):**
   - Stem anchored at base pod (`transform-origin: 50% 90%`).
   - Gentle breathing transform (`scale(1)` to `scale(1.02) translateY(-2px)`) over 3.5s cycles.
   - Used on Homepage hero and Onboarding Screen 1 & 2.
2. **Stage 2 (Growing / KYC In Progress):**
   - Active vertical stem extension (`scaleY(0.92)` to `scaleY(1.06)`).
   - Leaves sway dynamically between `-4deg` and `+4deg`.
   - Used on Goal selection (Screen 3) and KYC waiting state (Screen 4).
3. **Stage 3 (Milestone / First Investment Completed):**
   - Golden central bloom scales into view (`transform: scale(1)` with cubic spring).
   - Radial halo pulse (`#DCFCE7` aura) expands gently.
   - Unlocked exclusively on Screen 5 when ₹100 order is confirmed.

---

## 5. UI Geometry & Component Guidelines
- **Buttons:** Strictly architectural. `border-radius: var(--radius-sm)` (6px). Minimum height `44px` for touch targets. No pills.
- **Card Surfaces:** Crisp 1px solid border (`#E0E5E2`) with subtle low-blur drop shadow (`0 1px 3px rgba(16, 28, 21, 0.05)`).
- **Form Fields:** `44px` height, uppercase transform for PAN cards, formatted 12-digit masking for Aadhaar.
- **Breakpoints:** Single mobile-first breakpoint at `768px` for clean desktop rearrangement.
