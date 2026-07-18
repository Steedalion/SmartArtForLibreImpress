# Phase 18 Implementation Plan — Style Templates

## Goal

Named visual styles selectable in the dialog (spec §8 "Diagram style
templates"): the accent ramp, connector styling, corner rounding and shadows
become a bundled, persisted choice. The user palette field still overrides
individual fill colours; the template governs everything else.

## Presets

| Template | Label | Look |
|----------|-------|------|
| `DEFAULT` | Modern | Exact pre-Phase-18 navy→teal aesthetics (pinned by test) |
| `CLASSIC` | Classic | Office-style blues, orange secondary, square corners, stronger shadow |
| `MINIMAL` | Minimal | Flat: blue-grey ramp, no shadows, square corners, thin light connectors |
| `MONO` | Mono | Greyscale ramp, subtle shadow |

All preset fills keep white text legible (luminance guard in the unit test,
threshold anchored at the lightest colour shipped since v0.3.0).

## Files Changed

| File | Change |
|------|--------|
| `src/main/java/org/libreimpress/smartart/rendering/StyleTemplate.java` | New enum: ramp + secondary + text/connector/corner/shadow/venn params; `accent()`, `fill(kind, level)`, `arrowAccent()`, `labels()`, `fromIndex()`, `fromName()` (unknown → DEFAULT) |
| `src/main/java/org/libreimpress/smartart/rendering/SlideRenderer.java` | New `SlideRenderer(context, style)` constructor (1-arg keeps DEFAULT for existing call sites). All colour/corner/shadow/connector decisions now read from the template, including text colour (was hardcoded white) |
| `src/main/java/org/libreimpress/smartart/rendering/DefaultPalette.java` | Slimmed to template-independent typography (`fontSize`); colours moved to `StyleTemplate.DEFAULT` |
| `src/main/java/org/libreimpress/smartart/SmartArtDialog.java` | "Style:" dropdown row at y≈188; OK/Cancel moved to y=208; `DIALOG_H` 216 → 234. `Result` carries the `StyleTemplate`; the preview renders with the selected template; edit-mode prefill seeds the dropdown from metadata |
| `src/main/java/org/libreimpress/smartart/SmartArtCommand.java` | Passes the selected template to the renderer and persists `template=<name>` in the group metadata (field reserved in Phase 17) |
| `src/test/java/org/libreimpress/smartart/rendering/StyleTemplateTest.java` | 6 tests: DEFAULT pin (every historical constant), labels/fromIndex/fromName clamping, MINIMAL flatness, white-text luminance guard across all presets |

## Verified

- `mvn package` green (155 unit tests, incl. the DEFAULT aesthetics pin).
- `e2e_demo_probe.py` passes after the renderer refactor — the Phase 16
  safety net covering every draw branch (DemoRunner renders with DEFAULT, so
  the pinned look is what CI exercises).
- Manual GUI check (user): each template in dialog preview and on the slide;
  palette override on top of a template; editing a diagram re-selects its
  stored template.

## Notes

- Text colour is per-template plumbing-ready (`getTextColor()`), currently
  white in all presets — a future light template only needs to override it.
- Venn keeps shadows off in every template (translucent overlaps).
