# Phase 4: Shape Rendering - Implementation Plan

## Objective
Turn the parsed hierarchy into real, editable shapes on the current Impress
slide. Built in small increments so each one is installable and verifiable on
its own.

## Sub-phases
| # | Scope | Status |
|---|-------|--------|
| 4.1 | Draw a **single** rectangle on the current slide (prove the UNO drawing pipeline) | ✅ done |
| 4.2 | Multi-level **Hierarchy**: a box per node laid out as a top-down tree + parent→child connectors | ✅ done |
| 4.3 | Group all shapes into one editable group object | planned |
| 4.4 | Diagram-type shapes/layouts (Hub & Spoke circle+spokes, Process Flow sequence) | planned |
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
- ❌ More than one shape, layout, connectors, grouping.
- ❌ Diagram-type-specific shapes.
- ❌ Colour/styling.

---

## 4.2 — Multi-level Hierarchy

### Goal
Draw the whole parsed hierarchy as a readable top-down tree: one box per node,
each level on its own row, parents centred over their children, with a connector
from each parent to each child.

### Split: pure-Java layout vs UNO rendering
- **`layout/` (pure Java, unit-tested)** computes positions — no UNO, so the
  geometry is verifiable in `mvn test`:
  - `LaidOutShape` — a box (text, level, x, y, w, h in 1/100 mm) with `centerX()`.
  - `Edge` — a parent→child link by shape index.
  - `DiagramLayout` — the produced shapes + edges.
  - `HierarchyLayout.layout(root)` — assigns leaf boxes left-to-right, centres
    each parent over its children, and places level *L* on row *L*. Covered by
    `HierarchyLayoutTest` (vertical stacking, deeper = lower, parent centred,
    no sibling overlap, forests).
- **`rendering/SlideRenderer.drawHierarchy(layout)` (UNO)** turns that into
  shapes: a `RectangleShape` per `LaidOutShape`, then a `ConnectorShape` per
  `Edge` glued to the parent and child boxes (glue indices left at -1 so
  LibreOffice auto-routes). The connector-gluing API was validated headlessly
  before wiring it in.

### Wiring
`SmartArtCommand.execute()` on a valid parse: `HierarchyLayout.layout(root)` →
`SlideRenderer.drawHierarchy(layout)`. Only the Hierarchy layout exists, so all
diagram types use it for now; type-specific layouts come in 4.4.

### Testing
- `HierarchyLayoutTest` (5 cases) covers the positioning math.
- `tools/verify-extension.sh` still passes (extension loads + dispatches).
- **Manual (required):** the multi-shape draw is UNO runtime work — verify in
  Impress that a 3-level outline renders as a centred tree of labelled boxes
  joined by connectors.

### Not in 4.2
- ❌ Grouping into one object (4.3); ❌ Hub & Spoke / Process Flow (4.4);
  ❌ colour/styling.
