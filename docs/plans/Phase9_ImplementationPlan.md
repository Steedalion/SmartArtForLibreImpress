# Phase 9 Implementation Plan — Sequential Chevron Level-3+ Children

## Goal

Place level-3 (and deeper) descendants of Sequential Chevron sub-items as boxes
stacked vertically below their parent, connected parent-bottom → child-top.

## Background

Phase 4.4 placed level-1 chevrons and level-2 sub-items. Level-3+ children were
silently dropped. Phase 9 fills that gap using the same recursive strategy as
Phase 8 (Hub & Spoke children) but in the vertical direction.

## Files Changed

| File | Change |
|------|--------|
| `src/main/java/org/libreimpress/smartart/layout/SequentialChevronLayout.java` | Add `CHILD_V_GAP` constant; after placing each sub-item call `placeSubtree()` |
| `src/test/java/org/libreimpress/smartart/layout/SequentialChevronLayoutTest.java` | Add `level3ChildrenPlacedBelowSubitem` and `level3EdgeUsesBottomToTopGlue` tests |
| `uno-tests/probes/screenshot_probe.py` | Update `draw_sequential_chevron()` to add grandchild Hotel under Bravo |

## Algorithm

`placeSubtree(out, children, parentCX, parentBottomY, parentIndex, level)`:

1. `w = nodeWidth(level)`, `h = nodeHeight(level)` — same size-decrement formula
   as levels 1 and 2.
2. Starting `childY = parentBottomY + CHILD_V_GAP` (500 units).
3. For each child:
   - Place at `(parentCX − w/2, childY)`.
   - Add edge `(parentIndex → childIndex, glue 2 → 0)` (bottom of parent → top
     of child).
   - Recurse: `placeSubtree(out, child.getChildren(), parentCX, childY + h,
     childIndex, level + 1)`.
   - Advance `childY += h + CHILD_V_GAP`.

## Constants (in SequentialChevronLayout)

| Constant | Value | Meaning |
|----------|-------|---------|
| `CHILD_V_GAP` | 500 | Vertical gap (1/100 mm) between a sub-item and its level-3+ child |

## Screenshot Probe Changes

`draw_sequential_chevron()` now:
- Sub-items (Bravo, Charlie) under Alpha are placed side-by-side (horizontal),
  matching the real layout.
- Bravo carries one level-3 child (Hotel) stacked below it with `CHILD_V_GAP`.
- A narrower box (`child_w = sub_w − 400`, `child_h = 700`) represents the
  level-3 shape.

## Success Criteria

1. `level3ChildrenPlacedBelowSubitem` — the level-3 shape's Y equals
   `subitemY + subitemHeight + CHILD_V_GAP`.
2. `level3EdgeUsesBottomToTopGlue` — the edge from sub-item to its child uses
   `startGlue=2` (bottom) and `endGlue=0` (top).
3. Screenshot probe renders without error.
