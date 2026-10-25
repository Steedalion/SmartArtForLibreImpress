# Phase 7: Process Flow Sub-items — Implementation Plan

## Objective

Level-2+ children of Process Flow steps were previously silently dropped.
This phase places them as labelled boxes stacked vertically below their
parent step, connected with a bottom-to-top connector.

## Status: ✅ done

---

## Design

### Layout change (`ProcessFlowLayout`)

- Level-1 steps are now anchored at `MARGIN_Y` (top of slide) rather than
  vertically centred. This leaves room below for sub-items regardless of
  how many there are.
- After placing all steps and their horizontal arrow connectors, a second
  loop iterates each step's `DiagramNode.getChildren()` and stacks them:

```
childY = MARGIN_Y + h1 + V_GAP           // first child
       + (h2 + V_GAP) * childIndex       // each subsequent child
childX = stepCenterX - w2 / 2            // horizontally centred under step
```

- Each child gets a connector glued **bottom of parent (glue 2) → top of
  child (glue 0)** so LibreOffice draws a clean vertical line.

### New constants

| Constant | Value | Meaning |
|----------|-------|---------|
| `V_GAP` | 800 | vertical space between parent bottom and child top |
| `MARGIN_Y` | 1000 | Y of level-1 row (was `(SLIDE_H − h1) / 2`) |

### Connector style

Parent→child connectors are standard elbowed connectors (not straight, no
arrowhead) — they are structural/containment links, not sequential flow.

---

## Testing

- `ProcessFlowLayoutTest.stepsAreCentredVertically` renamed to
  `stepsAreAtTopMargin`; expected Y updated to `MARGIN_Y`.
- Three new tests:
  - `childrenArePlacedBelowParentStep` — Y ordering + centre-X alignment
  - `parentToChildEdgesArePresent` — edge count and parent/child indices
  - `childEdgesUseBottomToTopGluePoints` — glue points 2 and 0
- **Manual (required):** verify sub-steps appear below their parent step in
  Impress, centred on it, connected by a vertical line.
