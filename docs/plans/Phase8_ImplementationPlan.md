# Phase 8: Hub & Spoke Children — Implementation Plan

## Objective

Level-3+ descendants of Hub & Spoke spokes were previously silently dropped.
This phase places them as progressively smaller circles stacked radially
outward from their parent spoke, along the same angle from the hub.

## Status: ✅ done

---

## Design

### Layout change (`HubAndSpokeLayout`)

A new private recursive method `placeChildren()` is called after each spoke
is placed. It walks the spoke's `DiagramNode.getChildren()` and places each
child further along the same radial angle:

```
dist = parentRadius + CHILD_GAP + childRadius   // centre-to-centre distance
childCX = parentCX + (int)(dist × cos(angle))
childCY = parentCY + (int)(dist × sin(angle))
```

For the next sibling child, `dist` advances by `childD + CHILD_GAP`.
The method recurses into each child's own children, accumulating distance.

All child edges are flagged `straight=true` for visual consistency with
hub→spoke edges.

### New constant

| Constant | Value | Meaning |
|----------|-------|---------|
| `CHILD_GAP` | 600 | gap between the edge of a parent circle and the edge of its child circle |

### Node sizing

`nodeDiameter(level)` is already level-aware:
- Level 1 (hub): 2200 (unclamped: 1500 base)
- Level 2 (spoke): 1500
- Level 3+: `max(1500, 1500 − (level−1)×30)` → shrinks slightly per level

---

## Testing

- `HubAndSpokeLayoutTest`: two new tests:
  - `spokeChildrenArePlacedBeyondTheSpoke` — child is further from hub than spoke
  - `spokeChildrenAreStraightConnected` — all edges have `isStraight() == true`
- **Manual (required):** verify in Impress that level-3 nodes appear beyond
  their spoke along the radial line, connected by a straight line.
