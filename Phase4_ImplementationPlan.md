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
| 4.3 | Group all shapes into one editable group object | ✅ done |
| 4.4 | Diagram-type shapes/layouts (Hub & Spoke circle+spokes, Process Flow sequence) | ✅ done |
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
  pass; the registration/dispatch probe (`uno-tests/run.sh`) still
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
- `uno-tests/run.sh` (registration probe) still passes (extension loads + dispatches).
- **Manual (required):** the multi-shape draw is UNO runtime work — verify in
  Impress that a 3-level outline renders as a centred tree of labelled boxes
  joined by connectors.

### Not in 4.2
- ❌ Grouping into one object (4.3); ❌ Hub & Spoke / Process Flow (4.4);
  ❌ colour/styling.

---

## 4.3 — Group the shapes

### Goal
Collect all of a diagram's boxes and connectors into a single editable group, so
the user moves/deletes the diagram as one object (and can still double-click in to
edit a member). Satisfies the spec criterion "output is grouped and editable".

### Approach
`SlideRenderer.drawHierarchy` now records every shape it creates (boxes +
connectors), then groups them:
- create a `com.sun.star.drawing.ShapeCollection` — **from the global service
  manager** (`XComponentContext.getServiceManager()`), not the document factory
  (the document does not provide that service);
- add every created shape to the collection;
- `XShapeGrouper.group(collection)` on the draw page returns the group.

Validated headlessly first: grouping 5 shapes collapsed the page's top level from
7 to 3 (a group containing the 5).

### Not in 4.3
- ❌ Hub & Spoke / Process Flow (4.4); ❌ colour/styling.

---

## 4.4 — Diagram-type shapes/layouts

### Goal
Route the rendered output to a layout algorithm that matches the chosen diagram
type, so Hub & Spoke and Process Flow produce meaningfully different pictures
rather than always falling back to the hierarchy tree.

### Hub & Spoke (`HubAndSpokeLayout`)
The first level-1 node becomes the **hub**, placed at the slide centre
(12700, 9525 in 1/100 mm — the centre of a standard 254 × 190.5 mm slide).
Its immediate children (at any level ≥ 2), plus any extra level-1 siblings,
become the **spokes**, arranged evenly on a circle of radius 5500 around the hub.
The first spoke starts directly above the hub (angle −π/2) and the rest continue
clockwise. An edge runs from the hub to each spoke. Each spoke can have
arbitrarily deep descendants.

- Constants: `NODE_W = 4000`, `NODE_H = 1500`, `SPOKE_RADIUS = 5500`.
- Supports: hub + any-level children, level-1 siblings as additional spokes.
- Covered by `HubAndSpokeLayoutTest` (9 cases: including all-shapes-are-ellipses).

### Process Flow (`ProcessFlowLayout`)
The level-1 nodes become the **flow steps**, arranged left-to-right in a single
horizontal row centred on the slide. Children of each step are placed vertically
below it in an indented sub-tree: level-2 children sit one row down, level-3+
sit below that. Edges run from each step to the next, and from each parent to
its children.

- Constants: `NODE_W = 4000`, `NODE_H = 1500`, `H_GAP = 1500`,
  `SLIDE_W = 25400`, `SLIDE_H = 19050`, `MARGIN_X = 1000`.
- Supports: level-1 steps as the main row, then arbitrary depth of children below.
- Covered by `ProcessFlowLayoutTest` (9 cases: including edges use right-to-left
  glue points).

### Sequential Chevron (`SequentialChevronLayout`)

Level-1 nodes become the chevron/arrow steps arranged left-to-right. The first
step uses `ShapeKind.PENTAGON` (flat left, pointed right) and subsequent steps
use `ShapeKind.CHEVRON` (notched left, pointed right). Level-2 children are
placed as sub-item rectangles below their parent chevron.

#### Critical implementation note — `CustomShapeGeometry` type tagging

Chevron and pentagon shapes are `com.sun.star.drawing.CustomShape` instances
with their built-in LibreOffice geometry applied via the `CustomShapeGeometry`
property. **This property is deceptively hard to set correctly.**

The shape type string values (confirmed from `/usr/lib/libreoffice/share/registry/main.xcd`):
- First step (flat left, pointed right): `"pentagon-right"`
- Subsequent steps (notched left, pointed right): `"chevron"`

**What works** — `setPropertyValue("CustomShapeGeometry", new PropertyValue[]{typeVal})`:
Java's statically-typed `PropertyValue[]` array is passed to the UNO bridge,
which wraps it in an `Any` tagged as `sequence<com.sun.star.beans.PropertyValue>`.
This tag is what LibreOffice's C++ shape engine checks; without it the property
is silently ignored and the shape renders as a plain rectangle.

**What does NOT work** — `setPropertyValue("CustomShapeGeometry", tuple)` in
Python, where `tuple` is a plain Python tuple: the bridge does not infer the
correct type tag, so the property appears to be set but the shape stays a
rectangle. Python's equivalent that *does* work is attribute assignment
`shape.CustomShapeGeometry = (pv,)`, which carries the tag.

This was discovered after extensive investigation with 8+ approaches tried in
headless UNO probes (see `uno-tests/probes/find_chevron_probe.py` and related
files). The probe files are kept in the repo as a reference.

**Text centering in CustomShape** — unlike `RectangleShape`, `CustomShape` text
defaults to top-left. Two separate settings are required:
1. `TextVerticalAdjust = CENTER` on the shape's `XPropertySet`.
2. `ParaAdjust = CENTER` on the `XTextCursor` spanning the full text.

### Wiring (`SmartArtCommand`)
`execute()` calls `buildLayout(type, root)` which dispatches to the four
layout classes via a `switch` on `DiagramType`. The renderer path is unchanged.

### Testing
- All unit tests pass (`mvn test`).
- **Manual (required):** verify in Impress that selecting Hub & Spoke renders a
  central circle with spoke circles radiating outward; Process Flow renders a
  left-to-right sequence of boxes; Sequential Chevron renders pentagon + chevron
  arrow shapes — all grouped as one editable object.

### Not in 4.4
- ❌ Per-level colour palette & styling (Phase 5); ❌ wrapping for >4
  process-flow steps on one row; ❌ connectors from spokes to their children
  in Hub & Spoke (edges only go hub→spoke).
