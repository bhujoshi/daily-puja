# Pavitra Mandir — Sacred Design System (पवित्र मंदिर डिज़ाइन प्रणाली)

This document specifies the design language, color hierarchy, lighting model, and component aesthetics of **Pavitra Mandir (पवित्र मंदिर)**.

---

## 1. Core Aesthetic Philosophy: Garbhagriha Chiaroscuro

Traditional devotional apps often suffer from "raw RGB syndrome" — jarring primary yellow `#FFD700`, neon orange `#FF8C00`, and flat dark browns that look dated.

Pavitra Mandir follows **Garbhagriha Chiaroscuro**:
1. **Sacred Shadow & Divine Light**: An intimate, cool stone sanctum (गर्भगृह) enveloped in deep obsidian umber, softly illuminated by the warm honey-amber glow of natural ghee diyas.
2. **Authentic Aristocratic Materials**: Brushed temple brass (पीतल), aged gold (सुवर्ण), raw silk brocade (रेशम), and cool Makrana marble (संगमरमर) — avoiding plastic or fluorescent tones.
3. **Adaptive Translucent Surfaces**: Floating UI panels behave like fine frosted temple glass with warm gold hairline rims, blending seamlessly over the sanctum without obscuring the altar.
4. **Mindful Reverence over Gamification**: Serene transitions, gentle haptics, and zero jarring popups.

---

## 2. Color System & Design Tokens

### Primary Palette (Garbhagriha Chiaroscuro)

| Token Name | Hex Code | Android Compose / CSS Token | Semantic Purpose |
| :--- | :--- | :--- | :--- |
| **Obsidian Umber** | `#0D0B0A` | `DarkSanctumBackground` / `--sanctum-dark` | Sanctum canvas, app root background, altar vignetting |
| **Teak Deep** | `#140D0A` | `AltarTeakDark` / `--teak-dark` | Deep carved wood shadows |
| **Teak Wood** | `#22140F` | `AltarTeakWood` / `--teak-wood` | Temple altar pillars and architecture |
| **Temple Brass** | `#D8AF52` | `SacredGold` / `--gold` | Primary actions, golden trim, halo accents, active state |
| **Satin Brass** | `#C5A059` | `SacredBrass` / `--brass` | Secondary metallic elements, bell/thali surfaces |
| **Honey Kesariya** | `#FF9E2C` | `SacredSaffron` / `--saffron` | Diya flame aura, streak indicators, warm radiance |
| **Terracotta Saffron** | `#D85816` | `SacredDeepSaffron` / `--deep-saffron` | High-emphasis spiritual highlights |
| **Ceremonial Kumkum** | `#A82424` | `SacredVermilion` / `--vermilion` | Tilak marks, auspicious red accents, error states |
| **Sacred Roli** | `#881818` | `SacredKumkum` | Deeper red tones, curtain shadow folds |
| **Karpura Ivory** | `#FFFDF8` | `SacredTextPrimary` / `--text-primary` | Headings, Sanskrit mantras, high-contrast readable text |
| **Sandalwood Dust** | `#CFC2B2` | `SacredTextSecondary` / `--text-secondary` | Secondary labels, subtitles, step hints |
| **Roasted Coffee Ink** | `#261C16` | `RitualInk` | High-contrast readable text on cream/sand surfaces (AAA) |
| **Temple Ochre Gold** | `#B3833B` | `RitualGold` | Icons, active outlines, streak fire on light panels |
| **Sandalwood Ivory** | `#FAF6EE` | `Surface` / Light Mode Surface | Dialog cards, drawer background, customizer sheet |
| **Bisque Sandstone** | `#F3E7D3` | `SurfaceVariant` | Card item selections, active option rows |

### Secondary Flower & Offering Tones

| Offering | Color | Hex Code | Compose Token |
| :--- | :--- | :--- | :--- |
| **Lotus Blossom** | Rose Petal | `#E26A8D` | `LotusPink` / `--lotus-pink` |
| **Fresh Marigold** | Bright Genda | `#FF7A18` | `MarigoldOrange` / `--marigold` |
| **Pitambar Genda** | Golden Genda | `#FFC837` | `MarigoldYellow` |
| **Imperial Maroon** | Raw Silk Curtains | `#6B1212` | `SilkCurtainRed` / `--silk-red` |

---

## 3. Lighting, Flame & Materiality Rules

1. **Diya Flame Rendering**:
   - Never use a flat yellow circle. Use a 4-stop radial gradient:
     - Core: `#FFFDF8` (Karpura White)
     - Inner Body: `#FFC107` (Honey Amber)
     - Outer Mantle: `#FF6D00` (Flame Tangerine)
     - Ambient Halo: `rgba(255, 158, 44, 0.27)` with smooth falloff
   - Natural organic micro-flicker: wobble between $\pm 3$px and height scale between $0.92\times$ and $1.08\times$.

2. **Deity Halos (प्रभामंडल)**:
   - Light must bloom from **behind** the idols, never overlaying and washing out photographed deity faces.
   - 3 concentric luminous rings: Outer diffuse shimmer (`#FFCA3A`), sharp thin golden edge (`#FFE777`), and near-white core line (`#FFFBE3`).

3. **Floating Ritual Dock (`RitualGlassPanel`)**:
   - Background: Translucent frosted pearl-sandalwood (`#F5FFFDF8` $\rightarrow$ `#EEFAF3E6`) blurred over the underlying scene.
   - Border: Delicate 1px gradient brass rim (`#FAF4E8` $\rightarrow$ `rgba(216, 175, 82, 0.5)` $\rightarrow$ `#F2E6D0`).
   - Shadow: Warm ambient drop-shadow (`Color(0x661A1108)`) giving tactile elevation.

4. **Interactive Ritual Choice Buttons (`RitualChoice`)**:
   - Minimum height: **52dp** (accommodates elderly users and motor accessibility).
   - Unselected: Crisp 1px outline (`#66B3833B`), translucent background (`#99FFFFFF`), `FontWeight.Medium`.
   - Selected: 1.5px solid gold outline (`#B3833B`), warm bisque fill (`#F0E0C8`), checkmark prefix, `FontWeight.SemiBold`.

---

## 4. Typography & Accessibility Standards

- **Indic-First Hierarchy**: Default language is Hindi (हिंदी), with instant seamless switch to English.
- **Font Pairing**:
  - Headings & Mantras: Devotional serif (*Tiro Devanagari Hindi* for Hindi, *Cinzel* for Latin titles).
  - Body & Interactive Controls: High-clarity humanist sans (*Outfit* or system sans).
- **Legibility Rules**:
  - Action labels must never fall below `16sp`.
  - Step progress indicators must be at least `13sp` with `FontWeight.Medium`.
  - Contrast must meet **WCAG 2.1 AAA** for primary reading copy.
  - Never place light gold text on white/cream backgrounds.
  - All interactive elements must supply explicit accessible content descriptions (TalkBack support).
