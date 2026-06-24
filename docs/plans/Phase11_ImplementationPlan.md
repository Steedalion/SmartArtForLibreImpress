# Phase 11 Implementation Plan — Cycle Diagram

## Goal

Add a **Cycle** diagram type: level-1 nodes placed as rectangles equally spaced
clockwise around a ring, connected by straight directed arrows that wrap from the
last node back to the first. Level-2+ descendants are placed radially outward
from their parent node.

## Files Changed

| File | Change |
|------|--------|
| `src/main/java/org/libreimpress/smartart/layout/CycleLayout.java` | New layout class |
| `src/test/java/org/libreimpress/smartart/layout/CycleLayoutTest.java` | 9 unit tests |
| `src/main/java/org/libreimpress/smartart/models/DiagramType.java` | Add `CYCLE("Cycle")` |
| `src/main/java/org/libreimpress/smartart/SmartArtCommand.java` | Route `CYCLE` → `CycleLayout.layout()` |
| `uno-tests/probes/screenshot_probe.py` | Add `draw_cycle()` + entry in DIAGRAMS |

## Layout Algorithm

### Constants

| Constant | Value | Meaning |
|----------|-------|---------|
| `NODE_W` | 3500 | Level-1 node width (1/100 mm) |
| `NODE_H` | 1400 | Level-1 node height (1/100 mm) |
| `RING_RADIUS` | 6000 | Hub-centre to node-centre distance |
| `CHILD_GAP` | 600 | Gap between parent edge and child edge |

### Placement

1. `cx = SLIDE_W / 2`, `cy = SLIDE_H / 2` (slide centre).
2. For each of the `n` level-1 nodes at index `i`:
   - `angle = −π/2 + 2π × i / n` (starts at the top, advances clockwise).
   - `nodeCX = cx + RING_RADIUS × cos(angle)`
   - `nodeCY = cy + RING_RADIUS × sin(angle)`
   - Place rectangle at `(nodeCX − NODE_W/2, nodeCY − NODE_H/2, NODE_W, NODE_H)`.
   - Recursively call `placeChildren()` for level-2+ children.

### Directed Edges

After placing all nodes, for each `i` from 0 to n−1:

```
addEdge(indices[i], indices[(i+1) % n], startGlue=-1, endGlue=-1, straight=true, arrowEnd=true)
```

Auto-glue (`−1`) lets LibreOffice choose the nearest glue points, which works
well for straight connectors between adjacent nodes on a ring.

### Child Placement (`placeChildren`)

For level 2+ children of a cycle node at `(parentCX, parentCY)` along `angle`:

- `w = max(1500, NODE_W − (level − 1) × 200)`
- `h = max(700,  NODE_H − (level − 1) × 100)`
- Starting offset from parent centre: `NODE_W/2 + CHILD_GAP + h/2` (conservative
  — always clears the widest parent dimension regardless of angle).
- Each child: straight, no arrowhead. Recurse for further levels.

## Screenshot Probe

`draw_cycle()` draws 5 nodes (Alpha → Bravo → Charlie → Delta → Echo → Alpha)
with `straight=True, arrow_end=True` connectors.

## Success Criteria

1. `fourNodesProducesFourShapesAndFourEdges` — n nodes → n shapes, n edges.
2. `nodesArePlacedOnTheRing` — every node is at distance `RING_RADIUS` from
   the slide centre (±1 unit rounding).
3. `firstNodeIsAtTop` — first node is directly above the centre.
4. `edgesAreDirectedWithArrowheads` — all edges have `arrowEnd=true`.
5. `edgesAreStraightLines` — all edges have `straight=true`.
6. `lastEdgeWrapsAroundToFirstNode` — last edge has `parent=n−1, child=0`.
7. `edgesConnectConsecutiveNodes` — edge `i` connects node `i` → node `(i+1)%n`.
8. `level2ChildrenArePlacedOutwardFromParent` — child is further from centre
   than its parent cycle node.
9. Build (`mvn clean package`) succeeds with no warnings.
