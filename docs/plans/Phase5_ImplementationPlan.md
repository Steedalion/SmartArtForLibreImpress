# Phase 5: Default Colour Palette — Implementation Plan

## Objective

Apply a professional default colour scheme to diagram shapes so that every
diagram type looks polished out of the box, with no user colour input required.
When a user-provided palette is available (planned for a later phase), it will
override these defaults.

## Sub-phases

| # | Scope | Status |
|---|-------|--------|
| 5.1 | Apply a built-in default palette to shapes based on kind and level | ✅ done |
| later | User-provided `@paletteObject` input via the dialog | planned |

---

## 5.1 — Default palette applied to shapes

### Goal

Every shape rendered by `SlideRenderer` receives a solid fill colour and white
text, derived from a small lookup table (`DefaultPalette`). No user input is
needed.

### Palette rules

| Shape kind | Condition | Fill colour |
|------------|-----------|-------------|
| PENTAGON / CHEVRON | sequence index 0 | `#4472C4` (dark blue) |
| PENTAGON / CHEVRON | sequence index 1 | `#5B9BD5` (medium blue) |
| PENTAGON / CHEVRON | sequence index 2 | `#2E75B6` (steel blue) |
| PENTAGON / CHEVRON | sequence index 3+ | cycles back through the above |
| ELLIPSE | level 1 (hub) | `#4472C4` (dark blue) |
| ELLIPSE | level 2+ (spoke) | `#5B9BD5` (medium blue) |
| RECTANGLE | level 1 | `#4472C4` (dark blue) |
| RECTANGLE | level 2 | `#70AD47` (Office green) |
| RECTANGLE | level 3+ | `#5B9BD5` (medium blue) |

All fills use white (`#FFFFFF`) text. `LineStyle` is set to `NONE` (no border)
for a clean, modern look.

### Components

**`rendering/DefaultPalette`** (new, pure Java, no UNO)
- Constants for the fill colours listed above.
- `chevronFill(int sequenceIndex)` — cycles through blue shades.
- `fill(ShapeKind kind, int level)` — level/kind lookup for non-chevron shapes.
- `TEXT_WHITE` constant (`0xFFFFFF`).

**`rendering/SlideRenderer`**
- New private helper `applyStyle(Object shape, int fillColor, int textColor)` —
  sets `FillStyle=SOLID`, `FillColor`, `CharColor`, `LineStyle=NONE` via
  `XPropertySet`.
- Shape rendering loop tracks a `chevronSeq` counter; increments it whenever a
  CHEVRON or PENTAGON shape is drawn and passes `chevronSeq` to
  `DefaultPalette.chevronFill()`.
- Non-chevron shapes call `DefaultPalette.fill(kind, level)`.

### Testing

- All 41 existing unit tests are unaffected (palette logic is in the renderer,
  which is UNO runtime code and not unit-tested).
- **Manual (required):** install `target/SmartArt.oxt`, open Impress, create
  each diagram type and verify that shapes are filled with the expected colours
  and text is white and legible.

### Not in 5.1

- ❌ User-provided colour palette (later phase).
- ❌ Per-step gradient for chevrons beyond the 4-shade cycle.
- ❌ Font size scaling per level.
- ❌ Border/line styling (all borders removed for now).
