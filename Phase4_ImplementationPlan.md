# Phase 4: Shape Rendering - Implementation Plan

## Objective
Turn the parsed hierarchy into real, editable shapes on the current Impress
slide. Built in small increments so each one is installable and verifiable on
its own.

## Sub-phases
| # | Scope | Status |
|---|-------|--------|
| 4.1 | Draw a **single** rectangle on the current slide (prove the UNO drawing pipeline) | ◀ this increment |
| 4.2 | One rectangle per node (no layout yet — simple stacked placement) | planned |
| 4.3 | Hierarchy layout: position nodes as a top-down tree + parent→child connectors | planned |
| 4.4 | Group all shapes into one editable group object | planned |
| 4.5 | Diagram-type shapes/layouts (Hub & Spoke circle+spokes, Process Flow sequence) | planned |
| later | Per-level colour palette & styling | planned |

---

## 4.1 — Single shape

### Goal
When the user clicks **Create** with valid input, draw one rectangle on the
current slide containing the first top-level node's text. No layout, no
connectors, no grouping — this exists to prove the slide-drawing path works
end-to-end from the dispatched command.

### Behaviour
- Valid input → one rectangle is added to the current slide (fixed size and
  position), its text set to the first level-1 node. The shape itself is the
  feedback; the Phase 3 "parsed tree" message box is removed.
- Invalid input → the same validation error message as Phase 3 (no shape drawn).
- Any rendering failure (e.g. no active slide) → an error message box.

### Component: `rendering/SlideRenderer`
A thin UNO wrapper that resolves the current Impress slide and adds shapes.

- `drawRectangle(String text)`:
  1. `Desktop.getCurrentComponent()` → the active document.
  2. `XModel.getCurrentController()` → `XDrawView.getCurrentPage()` → the current
     slide (`XDrawPage`). If there is no draw view/page, fail with a clear message.
  3. The document (as `XMultiServiceFactory`) creates a
     `com.sun.star.drawing.RectangleShape`.
  4. Add it to the page (`XShapes.add`), then set `Size` and `Position`
     (units are 1/100 mm), then set its text (`XText.setString`).

### Wiring: `SmartArtCommand.execute()`
On a valid parse, call `new SlideRenderer(context).drawRectangle(label)` where
`label` is the first top-level node's text. Errors surface through the existing
message-box helper.

### Geometry (1/100 mm)
Position ≈ (2000, 2000), size ≈ 6000 × 3000 (≈ 6 cm × 3 cm) — comfortably visible
on a standard slide. These are placeholders; real layout arrives in 4.3.

---

## Testing
- `mvn clean package` / CI — unchanged unit tests (parser, outline editor) still
  pass; the registration/dispatch probe (`tools/verify-extension.sh`) still
  passes, confirming the extension loads after adding the renderer.
- **Manual (required):** the actual shape creation is UNO runtime work that is
  not headless-testable. Verify in Impress: SmartArt → Create Diagram… → Create →
  a labelled rectangle appears on the slide; it is selectable, movable, and its
  text editable.

## What's NOT in 4.1
- ❌ More than one shape, layout, connectors, grouping (4.2–4.4).
- ❌ Diagram-type-specific shapes (4.5).
- ❌ Colour/styling.
