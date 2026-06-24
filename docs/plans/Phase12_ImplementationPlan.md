# Phase 12 Implementation Plan — Pyramid Diagram

## Goal

Add a **Pyramid** diagram type: level-1 nodes rendered as center-aligned
rectangular tiers stacked vertically, narrowest at the top (apex) and widest
at the bottom (base). Each tier's width increases linearly from top to bottom.
Level-2+ descendants appear as smaller rectangles stacked to the right of
their parent tier, mirroring the Process Flow sub-item pattern.

No connectors are drawn between tiers — the stepped width gradient makes the
hierarchy self-evident.

## Files Changed

| File | Change |
|------|--------|
| `src/main/java/org/libreimpress/smartart/layout/ShapeKind.java` | Add `PYRAMID_TIER` |
| `src/main/java/org/libreimpress/smartart/layout/PyramidLayout.java` | New layout class |
| `src/test/java/org/libreimpress/smartart/layout/PyramidLayoutTest.java` | 8 unit tests |
| `src/main/java/org/libreimpress/smartart/models/DiagramType.java` | Add `PYRAMID("Pyramid")` |
| `src/main/java/org/libreimpress/smartart/SmartArtCommand.java` | Route `PYRAMID` → `PyramidLayout.layout()` |
| `src/main/java/org/libreimpress/smartart/rendering/SlideRenderer.java` | Handle `PYRAMID_TIER` in draw loop |
| `uno-tests/probes/screenshot_probe.py` | Add `draw_pyramid()` + entry in `DIAGRAMS` |

## Layout Algorithm

### Constants

| Constant | Value | Meaning |
|----------|-------|---------|
| `TIER_H` | 1600 | Tier height in 1/100 mm |
| `GAP` | 150 | Vertical gap between tiers |
| `MAX_W` | 18000 | Width of the bottom (widest) tier |
| `MIN_W` | 3000 | Width of the top (narrowest) tier |
| `TOP_Y` | 2000 | Y-offset of the top tier from the slide top |
| `SLIDE_W` | 25400 | Standard slide width |
| `CHILD_W` | 3000 | Width of a level-2+ child rectangle |
| `CHILD_H` | 900 | Height of a level-2+ child rectangle |
| `CHILD_GAP` | 300 | Horizontal gap between the tier's right edge and first child column |
| `CHILD_V_GAP` | 150 | Vertical gap between consecutive child rectangles |

### Tier Placement

For `n` level-1 nodes, tier `i` (0 = top/apex, n−1 = bottom/base):

```
width_i = (n == 1) ? MAX_W : MIN_W + (MAX_W - MIN_W) * i / (n - 1)
x_i     = (SLIDE_W - width_i) / 2
y_i     = TOP_Y + i * (TIER_H + GAP)
level_i = i + 1    // 1-based: used as palette key and for font size
```

Add each tier as:

```java
new LaidOutShape(node.getText(), level_i, x_i, y_i, width_i, TIER_H, ShapeKind.PYRAMID_TIER)
```

The single-node case (`n == 1`) uses `MAX_W` so the lone tier fills the slide width.

### Child Placement

For level-2+ children of tier `i`, stacked vertically to the right of the tier:

```
child_x = (SLIDE_W + width_i) / 2 + CHILD_GAP
```

Children start at `y_i` and stack downward with `CHILD_V_GAP` between them:

```java
new LaidOutShape(child.getText(), child.getLevel(), child_x, child_y, CHILD_W, CHILD_H, ShapeKind.RECTANGLE)
child_y += CHILD_H + CHILD_V_GAP
```

Recurse for level-3+ grandchildren, each column shifted right by `CHILD_W + CHILD_GAP`.

### Edges

None. The stepped width gradient visually expresses the structure.

## Renderer Changes (`SlideRenderer`)

**Service selection** — add `PYRAMID_TIER` alongside `ELLIPSE` in the service-name branch:

```java
} else if (s.getKind() == ShapeKind.PYRAMID_TIER) {
    service = "com.sun.star.drawing.RectangleShape";
}
```

**Color and style** — add after the `CIRCULAR_ARROW` block:

```java
} else if (s.getKind() == ShapeKind.PYRAMID_TIER) {
    int userColor = palette.getFillColor(s.getLevel()); // level = tier index + 1
    int fill = (userColor != ColorPalette.UNSET)
            ? userColor
            : DefaultPalette.chevronFill(chevronSeq);
    chevronSeq++;
    applyStyle(shape, fill, DefaultPalette.TEXT_WHITE,
            DefaultPalette.fontSize(s.getLevel()));
}
```

`DefaultPalette.chevronFill()` cycles through four blue shades, giving a
gradient across tiers. `DefaultPalette.fontSize(s.getLevel())` yields 14 pt
for the top tier (level 1), 11 pt for level 2, 9 pt for deeper tiers —
naturally giving the apex the most visual weight.

No geometry, rotation, or text-centering calls are needed beyond the standard
`xText.setString()`.

## User Palette Mapping

Tier fill colours map to palette levels: `1=#color` applies to the top
(narrowest/apex) tier, `2=#color` to the second tier, and so on downward.
`DefaultPalette.chevronFill()` applies for any tier whose level has no user
palette entry.

## Screenshot Probe (`draw_pyramid`)

Draw 4 tiers with labels Strategy → Goals → Tactics → Actions, plus two
children under the Goals tier ("Goal A", "Goal B") to verify child placement.
Use `BLUE, BLUE2, BLUE3, BLUE` as tier colours (matching `chevronFill` cycle).

Add `("pyramid", draw_pyramid)` to the `DIAGRAMS` list.

## Success Criteria

1. `threeTiersProducesThreeShapes` — 3 level-1 nodes → 3 `PYRAMID_TIER` shapes,
   0 edges, 0 `RECTANGLE` shapes at the tier level.
2. `tiersAreWidestAtBottom` — `getWidth()` increases strictly from tier 0 to
   tier n−1 (for n > 1).
3. `tiersAreCentredOnSlide` — `getX() + getWidth() / 2` is the same value for
   all tiers (integer centre, ±1 rounding tolerance).
4. `singleNodeProducesMaxWidth` — 1 level-1 node → 1 tier with
   `getWidth() == MAX_W`.
5. `level2ChildrenPlacedRightOfTier` — every level-2 child's `getX()` is ≥
   `parentTier.getX() + parentTier.getWidth() + CHILD_GAP`.
6. `level2ChildrenStackVertically` — siblings in the same parent tier have
   strictly increasing `getY()`.
7. `tierLevelEqualsIndexPlusOne` — tier at position `i` has `getLevel() == i + 1`.
8. Build (`mvn clean package`) succeeds with no warnings.
