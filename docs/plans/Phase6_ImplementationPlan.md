# Phase 6: Arrow Heads & Font Size Scaling — Implementation Plan

## Objective

Make diagrams more readable: show flow direction on Process Flow connectors
with arrowheads, and scale text size so higher-level nodes are visually
more prominent than sub-items.

## Sub-phases

| # | Scope | Status |
|---|-------|--------|
| 6.1 | Arrowhead on Process Flow step→step connectors | ✅ done |
| 6.2 | Font size scaling by level (14 / 11 / 9 pt) | ✅ done |

---

## 6.1 — Arrow heads on Process Flow connectors

### Design

`Edge` gains a boolean `arrowEnd` flag (default `false`). `ProcessFlowLayout`
marks each step→step edge with `arrowEnd=true`. `SlideRenderer` reads the flag
and sets two properties on the connector shape:

```java
props.setPropertyValue("LineEndName",  "Arrow");
props.setPropertyValue("LineEndWidth", Integer.valueOf(300)); // 3 mm
```

`"Arrow"` is LibreOffice's standard filled forward arrowhead from its
line-end gallery. `LineEndWidth` is in 1/100 mm.

### Changes

- `Edge`: add `arrowEnd` field; new 6-arg full constructor; `hasArrowEnd()` getter.
- `ProcessFlowLayout`: step→step edges use `new Edge(..., false, true)`.
- `SlideRenderer`: after setting `EdgeKind` (if straight), apply `LineEndName` /
  `LineEndWidth` when `edge.hasArrowEnd()`.
- `ProcessFlowLayoutTest`: `stepEdgesHaveArrowAtEnd` assertion added.

---

## 6.2 — Font size scaling by level

### Design

`DefaultPalette.fontSize(int level)` returns:

| Level | Size |
|-------|------|
| 1 | 14 pt |
| 2 | 11 pt |
| 3+ | 9 pt |

`SlideRenderer.applyStyle()` gains a `float fontSize` parameter and sets
`CharHeight` on the shape's `XPropertySet`. Setting `CharHeight` at the shape
level applies it as the default for all text in the shape (overridden only by
explicit cursor-level formatting, of which there is none here).

---

## Testing

- All unit tests pass (`mvn test`).
- **Manual (required):** verify in Impress that Process Flow connectors show
  arrowheads, and that level-1 text is noticeably larger than sub-item text.
