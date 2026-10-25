# Phase 19 Implementation Plan — Target, Basic Timeline, Radial List

## Goal

Three popular PowerPoint layouts using only existing machinery (spec §8
"Additional diagram types"). Gear was skipped (no gear preset in
LibreOffice's main.xcd) and Counterbalance (low value).

## The types

- **Target** (`TargetLayout`): concentric circles on the slide centre, first
  item outermost, last the bullseye (PowerPoint's convention). Emitted
  outermost-first so insertion order stacks smaller rings on top (the z-order
  trick Pyramid/Venn already rely on). New `ShapeKind.TARGET_RING`: ellipse
  service, sequence-coloured, text anchored TOP into the exposed band,
  shadow off (rings would shade each other). Level-2+ ignored (like Venn).
- **Basic Timeline** (`TimelineLayout`): thin spine rectangle + one
  `TIMELINE_MARKER` circle per level-1 item + label boxes alternating
  above/below, joined by short straight connectors. Level-2+ children become
  bullets under the title (reuses `BulletText.withTitle`). Outermost labels
  are clamped onto the slide.
- **Radial List** (`RadialListLayout`): per level-1 node a central circle
  with its level-2 children as rectangular satellites on a ring (Hub & Spoke
  trig incl. the crowding-pushes-radius-out rule); level-3+ as bullets inside
  the satellite. Hub deliberately does NOT scale-to-fit (PROPORTIONAL would
  balloon short labels).

## Files Changed

| File | Change |
|------|--------|
| `models/DiagramType.java` | `TARGET`, `BASIC_TIMELINE`, `RADIAL_LIST` appended (index order feeds the dropdown) |
| `layout/TargetLayout.java`, `TimelineLayout.java`, `RadialListLayout.java` | New |
| `layout/ShapeKind.java` | `TARGET_RING`, `TIMELINE_MARKER` |
| `layout/LayoutFactory.java` | Three new cases |
| `rendering/SlideRenderer.java` | Ellipse service + accent-cycled branch for the two new kinds; TARGET_RING gets `TextVerticalAdjust.TOP` + no shadow |
| `src/test/java/.../layout/{Target,Timeline,RadialList}LayoutTest.java` | 8 tests each: counts, kinds, geometry invariants (rings concentric/strictly shrinking, markers on spine + alternation + same-side non-overlap, satellites equidistant + radius grows with crowding), bullet nesting, single-node/empty cases |
| `DemoRunner.java` | 3 new demo entries → automatic E2E + screenshot coverage |
| `uno-tests/probes/e2e_demo_probe.py` | 3 new expectation rows |
| `README.md` | 3 new table rows |

## Bug found & fixed along the way: screenshot aspect distortion

The default Impress page is **28000×15750 (16:9)**, but `DemoRunner`
exported every page at a fixed 1280×960 (4:3) — vertically stretching every
diagram in every screenshot since screenshots existed (circles rendered as
ellipses). Diagnosed with a throwaway UNO probe (page size + shape sizes:
the shapes are true circles; only the export distorted). Fixes:

- `DemoRunner.exportScreenshot` reads the page's `Width`/`Height` and
  exports 1280-wide at the true aspect (→ 1280×720 on default pages).
- `e2e_demo_probe.py` computes the expected PNG size from the page.
- `TargetLayout.MAX_D` (12000) and `RadialListLayout.BASE_RADIUS` (5000)
  sized so the bottom half fits below the layouts' assumed centre line
  (y=9525) even on a 15750-high widescreen page.
- `scripts/make-screenshots.sh` now exports via the **real** pipeline
  (new `uno-tests/probes/export_screenshots.py` dispatches Demo with
  `OutputDir`); the hand-synced `screenshot_probe.py` is retired from the
  script. All 15 README PNGs regenerated aspect-correct.

**Known follow-up (pre-existing, all types):** layouts assume a 25400×19050
page, so diagrams sit slightly left/low of centre on default 16:9 slides.
Reading the real page size into the layouts is a future phase.

## Verified

- `mvn package` green (179 unit tests).
- `e2e_demo_probe.py` green for all 15 types (groups, texts, metadata,
  aspect-correct PNGs).
- Screenshots visually inspected: rings circular and fully on-slide,
  timeline labels on-slide with alternation, radial hub text normal size.
