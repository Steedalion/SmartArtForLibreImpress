# LibreOffice Impress — Available CustomShape Types

Reference list of preset shape types available in LibreOffice (Impress/Draw).
Each is created as a `com.sun.star.drawing.CustomShape` and configured by setting
the **Type** value below in the `CustomShapeGeometry` property
(see [technical facts](../impressSmartArt.md) and the `CustomShapeGeometry` notes).

Source: `/usr/lib/libreoffice/share/registry/main.xcd` (`.uno:*Shapes.*` commands).
The **Type** column is the value passed to `CustomShapeGeometry`; the **UNO command**
column is the dispatch name used by the toolbar/menu.

## Basic Shapes

| Type | UNO command |
|---|---|
| `rectangle` | `.uno:BasicShapes.rectangle` |
| `round-rectangle` | `.uno:BasicShapes.round-rectangle` |
| `quadrat` | `.uno:BasicShapes.quadrat` |
| `round-quadrat` | `.uno:BasicShapes.round-quadrat` |
| `circle` | `.uno:BasicShapes.circle` |
| `circle-pie` | `.uno:BasicShapes.circle-pie` |
| `ellipse` | `.uno:BasicShapes.ellipse` |
| `block-arc` | `.uno:BasicShapes.block-arc` |
| `isosceles-triangle` | `.uno:BasicShapes.isosceles-triangle` |
| `right-triangle` | `.uno:BasicShapes.right-triangle` |
| `trapezoid` | `.uno:BasicShapes.trapezoid` |
| `parallelogram` | `.uno:BasicShapes.parallelogram` |
| `diamond` | `.uno:BasicShapes.diamond` |
| `pentagon` | `.uno:BasicShapes.pentagon` |
| `hexagon` | `.uno:BasicShapes.hexagon` |
| `octagon` | `.uno:BasicShapes.octagon` |
| `cross` | `.uno:BasicShapes.cross` |
| `ring` | `.uno:BasicShapes.ring` |
| `cube` | `.uno:BasicShapes.cube` |
| `can` | `.uno:BasicShapes.can` |
| `frame` | `.uno:BasicShapes.frame` |
| `paper` | `.uno:BasicShapes.paper` |

## Arrow / Block-Arrow Shapes

| Type | UNO command |
|---|---|
| `right-arrow` | `.uno:ArrowShapes.right-arrow` |
| `left-arrow` | `.uno:ArrowShapes.left-arrow` |
| `up-arrow` | `.uno:ArrowShapes.up-arrow` |
| `down-arrow` | `.uno:ArrowShapes.down-arrow` |
| `left-right-arrow` | `.uno:ArrowShapes.left-right-arrow` |
| `up-down-arrow` | `.uno:ArrowShapes.up-down-arrow` |
| `up-right-arrow` | `.uno:ArrowShapes.up-right-arrow` |
| `up-right-down-arrow` | `.uno:ArrowShapes.up-right-down-arrow` |
| `quad-arrow` | `.uno:ArrowShapes.quad-arrow` |
| `corner-right-arrow` | `.uno:ArrowShapes.corner-right-arrow` |
| `split-arrow` | `.uno:ArrowShapes.split-arrow` |
| `split-round-arrow` | `.uno:ArrowShapes.split-round-arrow` |
| `s-sharped-arrow` | `.uno:ArrowShapes.s-sharped-arrow` |
| `striped-right-arrow` | `.uno:ArrowShapes.striped-right-arrow` |
| `notched-right-arrow` | `.uno:ArrowShapes.notched-right-arrow` |
| `pentagon-right` | `.uno:ArrowShapes.pentagon-right` |
| `chevron` | `.uno:ArrowShapes.chevron` |
| `circular-arrow` | `.uno:ArrowShapes.circular-arrow` |
| `right-arrow-callout` | `.uno:ArrowShapes.right-arrow-callout` |
| `left-arrow-callout` | `.uno:ArrowShapes.left-arrow-callout` |
| `up-arrow-callout` | `.uno:ArrowShapes.up-arrow-callout` |
| `down-arrow-callout` | `.uno:ArrowShapes.down-arrow-callout` |
| `left-right-arrow-callout` | `.uno:ArrowShapes.left-right-arrow-callout` |
| `up-down-arrow-callout` | `.uno:ArrowShapes.up-down-arrow-callout` |
| `up-right-arrow-callout` | `.uno:ArrowShapes.up-right-arrow-callout` |
| `quad-arrow-callout` | `.uno:ArrowShapes.quad-arrow-callout` |

## Flowchart Shapes

| Type | UNO command |
|---|---|
| `flowchart-process` | `.uno:FlowChartShapes.flowchart-process` |
| `flowchart-alternate-process` | `.uno:FlowChartShapes.flowchart-alternate-process` |
| `flowchart-decision` | `.uno:FlowChartShapes.flowchart-decision` |
| `flowchart-data` | `.uno:FlowChartShapes.flowchart-data` |
| `flowchart-predefined-process` | `.uno:FlowChartShapes.flowchart-predefined-process` |
| `flowchart-internal-storage` | `.uno:FlowChartShapes.flowchart-internal-storage` |
| `flowchart-document` | `.uno:FlowChartShapes.flowchart-document` |
| `flowchart-multidocument` | `.uno:FlowChartShapes.flowchart-multidocument` |
| `flowchart-terminator` | `.uno:FlowChartShapes.flowchart-terminator` |
| `flowchart-preparation` | `.uno:FlowChartShapes.flowchart-preparation` |
| `flowchart-manual-input` | `.uno:FlowChartShapes.flowchart-manual-input` |
| `flowchart-manual-operation` | `.uno:FlowChartShapes.flowchart-manual-operation` |
| `flowchart-connector` | `.uno:FlowChartShapes.flowchart-connector` |
| `flowchart-off-page-connector` | `.uno:FlowChartShapes.flowchart-off-page-connector` |
| `flowchart-card` | `.uno:FlowChartShapes.flowchart-card` |
| `flowchart-punched-tape` | `.uno:FlowChartShapes.flowchart-punched-tape` |
| `flowchart-summing-junction` | `.uno:FlowChartShapes.flowchart-summing-junction` |
| `flowchart-or` | `.uno:FlowChartShapes.flowchart-or` |
| `flowchart-collate` | `.uno:FlowChartShapes.flowchart-collate` |
| `flowchart-sort` | `.uno:FlowChartShapes.flowchart-sort` |
| `flowchart-extract` | `.uno:FlowChartShapes.flowchart-extract` |
| `flowchart-merge` | `.uno:FlowChartShapes.flowchart-merge` |
| `flowchart-stored-data` | `.uno:FlowChartShapes.flowchart-stored-data` |
| `flowchart-delay` | `.uno:FlowChartShapes.flowchart-delay` |
| `flowchart-sequential-access` | `.uno:FlowChartShapes.flowchart-sequential-access` |
| `flowchart-magnetic-disk` | `.uno:FlowChartShapes.flowchart-magnetic-disk` |
| `flowchart-direct-access-storage` | `.uno:FlowChartShapes.flowchart-direct-access-storage` |
| `flowchart-display` | `.uno:FlowChartShapes.flowchart-display` |

## Callout Shapes

| Type | UNO command |
|---|---|
| `rectangular-callout` | `.uno:CalloutShapes.rectangular-callout` |
| `round-rectangular-callout` | `.uno:CalloutShapes.round-rectangular-callout` |
| `round-callout` | `.uno:CalloutShapes.round-callout` |
| `cloud-callout` | `.uno:CalloutShapes.cloud-callout` |
| `line-callout-1` | `.uno:CalloutShapes.line-callout-1` |
| `line-callout-2` | `.uno:CalloutShapes.line-callout-2` |
| `line-callout-3` | `.uno:CalloutShapes.line-callout-3` |

## Star / Banner Shapes

| Type | UNO command |
|---|---|
| `star4` | `.uno:StarShapes.star4` |
| `star5` | `.uno:StarShapes.star5` |
| `star6` | `.uno:StarShapes.star6` |
| `star8` | `.uno:StarShapes.star8` |
| `star12` | `.uno:StarShapes.star12` |
| `star24` | `.uno:StarShapes.star24` |
| `concave-star6` | `.uno:StarShapes.concave-star6` |
| `bang` | `.uno:StarShapes.bang` |
| `signet` | `.uno:StarShapes.signet` |
| `doorplate` | `.uno:StarShapes.doorplate` |
| `horizontal-scroll` | `.uno:StarShapes.horizontal-scroll` |
| `vertical-scroll` | `.uno:StarShapes.vertical-scroll` |

## Symbol Shapes

| Type | UNO command |
|---|---|
| `smiley` | `.uno:SymbolShapes.smiley` |
| `sun` | `.uno:SymbolShapes.sun` |
| `moon` | `.uno:SymbolShapes.moon` |
| `heart` | `.uno:SymbolShapes.heart` |
| `flower` | `.uno:SymbolShapes.flower` |
| `cloud` | `.uno:SymbolShapes.cloud` |
| `lightning` | `.uno:SymbolShapes.lightning` |
| `forbidden` | `.uno:SymbolShapes.forbidden` |
| `puzzle` | `.uno:SymbolShapes.puzzle` |
| `diamond-bevel` | `.uno:SymbolShapes.diamond-bevel` |
| `octagon-bevel` | `.uno:SymbolShapes.octagon-bevel` |
| `quad-bevel` | `.uno:SymbolShapes.quad-bevel` |
| `brace-pair` | `.uno:SymbolShapes.brace-pair` |
| `bracket-pair` | `.uno:SymbolShapes.bracket-pair` |
| `left-brace` | `.uno:SymbolShapes.left-brace` |
| `right-brace` | `.uno:SymbolShapes.right-brace` |
| `left-bracket` | `.uno:SymbolShapes.left-bracket` |
| `right-bracket` | `.uno:SymbolShapes.right-bracket` |
