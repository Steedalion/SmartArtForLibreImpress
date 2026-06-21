# LibreImpress SmartArt Plugin - Specification

## 1. Overview

**Project:** LibreImpress SmartArt Plugin  
**Purpose:** A UNO extension for LibreOffice Impress that enables users to quickly create structured diagrams from hierarchical text input.  
**Architecture:** UNO extension implemented in **Java**, packaged as a `.oxt`.

### 1.1 Specification Document Hierarchy

This file (`impressSmartArt.md`) is the **functional specification** — the single
source of truth for *what* the plugin does and the packaging/registration rules
it must satisfy (§5.5). All other documents sit beneath it and must stay
consistent with it:

```
impressSmartArt.md                 ← master spec (this file): scope, behaviour, packaging rules
│
├── docs/plans/ — Phase plans (how each phase is built, in order). These files contain code and configuration snippets like
│   ├── Phase1_ImplementationPlan.md   — Phase 1: Empty OXT extension (installable skeleton)
│   ├── Phase2_ImplementationPlan.md   — Phase 2: Menu integration (top-level menu entry → dispatch)
│   ├── Phase3_ImplementationPlan.md   — Phase 3: Dialog & text parsing
│   ├── Phase4_ImplementationPlan.md   — Phase 4: Shape rendering (4.1 single shape → layout → grouping)
│   ├── Phase5_ImplementationPlan.md   — Phase 5: Default colour palette
│   ├── Phase6_ImplementationPlan.md   — Phase 6: Arrow heads & font size scaling
│   ├── Phase7_ImplementationPlan.md   — Phase 7: Process Flow sub-items
│   ├── Phase8_ImplementationPlan.md   — Phase 8: Hub & Spoke children
│   ├── Phase9_ImplementationPlan.md   — Phase 9: Sequential Chevron level-3+ children
│   ├── Phase10_ImplementationPlan.md  — Phase 10: User colour palette via dialog
│   ├── Phase11_ImplementationPlan.md  — Phase 11: Cycle diagram type
│   └── Phase12_ImplementationPlan.md  — Phase 12: Pyramid diagram type
│       (Phases 13–15 were delivered in batches without separate plan files; see § 1.2)
│
├── Architecture_VDiagram.md       — Architecture overview & V-model development process. This file uses mermaid diagrams, but does not contain any code.
├── TESTING_STRATEGY.md            — testing approach (Java unit · OXT structure · runtime dispatch)
└── README.md                      — build, install, and run instructions
```

**Conventions:**
- Each phase delivers one installable increment; later phases build on earlier ones.
- There is exactly one plan per phase (no "revised"/duplicate variants).
- When the master spec and a phase plan disagree, the master spec wins; update
  the phase plan to match.

### 1.2 Implementation Status (as of 2026-06-16, v0.3.0)

| Phase | Scope | Status |
|-------|-------|--------|
| 1 | Empty, installable `.oxt` skeleton | ✅ Done |
| 2 | Top-level **SmartArt** menu → dispatch to the Java handler | ✅ Done |
| 3 | Input dialog + hierarchy parser (validate, preview the parsed tree) | ✅ Done |
| 4 | Render the parsed tree as grouped, editable shapes on the slide | ✅ Done |
| 4.1 | Draw a single rectangle on the current slide (prove the drawing pipeline) | ✅ Done |
| 4.2 | Multi-level Hierarchy: a box per node as a top-down tree + connectors | ✅ Done |
| 4.3 | Group the diagram's shapes into one editable group object | ✅ Done |
| 4.4 | Diagram-type shapes/layouts (Hub & Spoke, Process Flow, Sequential Chevron) | ✅ Done |
| 5 | Default colour palette: blue/green by level, white text, no border | ✅ Done |
| 6 | Arrow heads on Process Flow step connectors; font size scaling by level | ✅ Done |
| 7 | Process Flow sub-items: horizontal row scaled to fit within each step's width | ✅ Done |
| 8 | Hub & Spoke level-3+ children spread in a radial fan around their spoke | ✅ Done |
| 9 | Sequential Chevron level-3+ children stacked below sub-items | ✅ Done |
| 10 | User-provided colour palette via dialog (optional, per-level hex colours) | ✅ Done |
| 11 | Cycle diagram: clockwise ring of rectangles with directed arrows; Cycle (Arrows): circles with curved connector arrows | ✅ Done |
| 12 | Pyramid diagram: stepped rectangular tiers, narrowest at top, widest at base | ✅ Done |
| 13 | Cycle (Blocks): rectangles in a ring with solid block-arrow shapes between them | ✅ Done |
| 14 | Four popular figures: Basic Block List, Vertical Bullet List, Basic Venn, Basic Matrix | ✅ Done |
| 15 | Aesthetics refresh: rounded corners, soft shadows, navy→teal palette, per-shape text fit | ✅ Done |

**What works today (Phases 1–15):** clicking **SmartArt → Create Diagram…**
opens a programmatic dialog (diagram-type dropdown + multiline text + optional
colour palette field); on **Create** the indented text is parsed into a validated
hierarchy and the chosen diagram type is rendered as grouped, editable shapes on
the current slide. Twelve diagram types are fully implemented:

| Type | Description |
|------|-------------|
| Hierarchy | Top-down tree of rectangles with connector lines |
| Hub & Spoke | Central circle with spoke circles radiating outward; level-3+ children fan radially from their spoke |
| Process Flow | Left-to-right steps; level-2 sub-items sit in a horizontal row scaled to fit the step width |
| Sequential Chevron | Pentagon → chevron steps; level-2 sub-items in a scaled horizontal row below each step |
| Cycle | Clockwise ring of rectangles with directed straight-line arrows |
| Cycle (Arrows) | Clockwise ring of circles with directed curved connector arrows |
| Cycle (Blocks) | Clockwise ring of rectangles with solid block-arrow shapes between adjacent nodes |
| Pyramid | Centre-aligned rectangular tiers stacked top-to-bottom, apex narrowest; level-2+ sub-items to the right |
| Basic Block List | Equal rectangles in a near-square grid; level-2 and deeper children become nested, indented bullet lines inside each block |
| Vertical Bullet List | Stacked title bars, each with a content box of its level-2 and deeper children as nested, indented bullets beneath |
| Basic Venn | Overlapping translucent circles, one per level-1 item, around the slide centre |
| Basic Matrix | First four level-1 items as the quadrants of a 2×2 grid |

All types share: built-in blue/green colour palette, font sizing by level, and
per-level colour overrides via the palette field (format: `1=#4472C4`).
Sections 2–8 below describe the target product.

### 1.3 How these documents are written (and the one-shot goal)

**Goal:** these documents should be complete and precise enough to **develop the
plugin in one shot** — a developer (or an AI agent) should be able to build any
phase straight from the plans, with no further clarification needed. To keep that
workable, each document has a fixed role and a rule about code:

- **This master specification is *functional* and contains *no code*.** It states
  *what* the plugin does and the externally-observable rules it must satisfy
  (behaviour, inputs, validation, packaging outcomes) — never *how* it is coded.
- **`Architecture_VDiagram.md` holds the *diagrams* (Mermaid) and *no code*** —
  the class/component structure, the runtime dispatch flow, and the V-model.
- **The phase plans (`PhaseN_ImplementationPlan.md`) are the only documents that
  may contain *code*** — concrete snippets, XML fragments, file layouts, and the
  exact UNO registration details needed to build each increment.

So when a build detail (a code/XML snippet, an exact UNO name, a file path) is
needed, it belongs in a phase plan — not here, and not in the architecture doc.

---

## 2. Core Functionality

### 2.1 Input System
- **Interface:** Multi-point dialog box
- **Features:**
  - Text input field for entering diagram points/nodes
  - Support for up to N levels of hierarchy depth
  - Each line represents one node
  - Leading dashes indicate depth: no dash = level 1, one dash = level 2, two dashes = level 3, etc.
  - Depth may increase by at most one level per line; it may decrease by any amount

### 2.2 Diagram Types (v1.0)

#### 2.2.1 Hierarchy Diagram
- **Purpose:** Organizational or classification tree structure
- **Level Structure:** 
  - Level 1: Root/top level
  - Level 2+: Child levels
- **Layout:** Top-down tree layout
- **Connections:** Parent nodes connect to child nodes with lines/connectors

#### 2.2.2 Hub & Spoke Diagram
- **Purpose:** Radial structure with central concept and peripheral items
- **Level Structure:**
  - Level 1: Hub/center node
  - Level 2: Primary spokes radiating from hub
  - Level 3+: Secondary levels (if applicable)
- **Layout:** Circular/radial arrangement around center
- **Connections:** All items connect to hub; secondary items connect to their parent spokes

#### 2.2.3 Pyramid Diagram
- **Purpose:** Hierarchical levels where size or importance decreases from base to apex
- **Level Structure:**
  - Level 1: Each item is one tier of the pyramid (first = apex, last = base)
  - Level 2+: Sub-items stacked to the right of their parent tier
- **Layout:** Center-aligned rectangular tiers stacked vertically, width increasing linearly from top to bottom
- **Connections:** None — the stepped shape expresses hierarchy visually

#### 2.2.4 Process Flow Diagram
- **Purpose:** Sequential steps or workflow
- **Level Structure:**
  - Level 1: Primary process steps
  - Level 2+: Sub-steps branching from each step
- **Layout:** Left-to-right row of steps; level-2 sub-items sit in a horizontal
  row below their parent step, scaled proportionally so the row fits within the
  step's width
- **Connections:** Arrow connectors between consecutive steps; vertical connectors
  from each step to its sub-items

#### 2.2.5 Sequential Chevron Diagram
- **Purpose:** Linear process with a visually progressive arrow motif
- **Level Structure:**
  - Level 1: Process steps (first rendered as a flat-back pentagon, rest as chevrons)
  - Level 2+: Sub-items below each chevron
- **Layout:** Left-to-right chevron strip; level-2 sub-items in a horizontal row
  below each parent chevron, scaled to fit within the chevron's width
- **Connections:** None between chevrons (the interlocking shape implies flow);
  connectors from chevrons to sub-item boxes

#### 2.2.6 Cycle Diagram
- **Purpose:** Circular process with no defined start or end
- **Level Structure:** Level 1 only (sub-items are ignored)
- **Layout:** Nodes as rectangles arranged clockwise at equal angular intervals
  around a ring
- **Connections:** Directed straight-line arrows between adjacent nodes (wrapping
  from the last node back to the first)

#### 2.2.7 Cycle (Arrows) Diagram
- **Purpose:** Circular process emphasising the flow between nodes
- **Level Structure:** Level 1 only (sub-items are ignored)
- **Layout:** Nodes as circles arranged clockwise around a ring
- **Connections:** Directed curved connector arrows between adjacent circles

#### 2.2.8 Cycle (Blocks) Diagram
- **Purpose:** Circular process using solid block shapes for both nodes and
  directional indicators
- **Level Structure:** Level 1 only (sub-items are ignored)
- **Layout:** Nodes as rectangles arranged clockwise around a ring; a solid
  block arrow (`right-arrow` CustomShape) is placed at the midpoint between each
  pair of adjacent rectangles and rotated to point toward the next node
- **Connections:** None (the block arrows are positioned shapes, not connectors)

#### 2.2.9 Basic Block List Diagram
- **Purpose:** Present a set of unordered, equally-weighted items
- **Level Structure:** Level 1 = blocks; level 2 and deeper = nested, indented
  bullet lines inside the block
- **Layout:** Equal rectangles in a near-square grid (`cols = ceil(sqrt(n))`)
  that fills the slide, wrapping into rows
- **Connections:** None

#### 2.2.10 Vertical Bullet List Diagram
- **Purpose:** Grouped lists under headings (agendas, feature lists)
- **Level Structure:** Level 1 = title bar; level 2 and deeper = nested,
  indented bullets beneath it
- **Layout:** Title bars stacked top-to-bottom; each title with children gets a
  content box of bullet lines directly below it; a childless title fills its slot
- **Connections:** None

#### 2.2.11 Basic Venn Diagram
- **Purpose:** Show overlapping or shared concepts
- **Level Structure:** Level 1 only (deeper descendants are ignored)
- **Layout:** Equal translucent circles arranged so adjacent circles overlap,
  around the slide centre (single circle centred; two placed horizontally)
- **Connections:** None

#### 2.2.12 Basic Matrix Diagram
- **Purpose:** Show the relationship of four parts to a whole
- **Level Structure:** First four level-1 items only; level 2 and deeper =
  nested, indented bullets in a cell
- **Layout:** Four equal rectangular quadrants of a 2×2 grid centred on the slide
  (order top-left, top-right, bottom-left, bottom-right), each coloured distinctly
- **Connections:** None

---

## 3. Shape & Styling System

### 3.1 Shape Usage
- Use existing LibreOffice shapes (rectangles, circles, diamonds, etc.)
- Shapes selected based on diagram type and level:
  - **Hierarchy:** Rectangles for all levels
  - **Hub & Spoke:** Circle for hub, rounded rectangles for spokes
  - **Process Flow:** Rectangles for steps, diamonds for decisions

### 3.2 Styling & Color Palette
- **Input:** Optional palette field in the dialog (`1=#4472C4` format, one entry per line)
- **Default Behavior:** If no palette provided, built-in blue/green palette applied by level
- **Palette Structure:**
  - Color definitions per level
  - Optional font styling (size, family, weight)
  - Optional shape styling (fill, border)

---

## 4. User Workflow

### 4.1 Triggering the Plugin
1. User opens a LibreImpress presentation
2. User selects **SmartArt → Create Diagram…** from the menu bar
3. Plugin dialog opens

### 4.2 Creating a Diagram
1. User selects diagram type from the dropdown
2. User enters text points in the multi-line outline field using dash-prefix notation
3. User (optionally) provides a color palette in the palette field
4. User clicks **Create**
5. Plugin generates the diagram on the current slide *(Phase 4; the current
   build instead previews the parsed hierarchy and reports validation errors)*

### 4.3 Output
- Grouped shape object containing all nodes and connectors
- Diagram is fully editable (user can modify shapes/text after creation)
- All elements part of a single group for easy manipulation

---

## 5. Technical Specifications

### 5.1 Input Format

The dialog collects, with **Create** and **Cancel** actions:

- **Diagram type** — one of Hierarchy, Hub & Spoke, Process Flow, Sequential
  Chevron, Cycle, Cycle (Arrows), Cycle (Blocks), Pyramid, Basic Block List,
  Vertical Bullet List, Basic Venn, or Basic Matrix (a dropdown).
- **Text points** — a multi-line outline field; each line is one node. Hierarchy
  depth is expressed by **leading dashes** followed by a space, resembling
  Markdown list syntax (per the rules in §5.2). It behaves as an outline
  editor — **Enter** starts a new item at the current depth, and the depth of the
  current line is changed with the **Indent / Outdent buttons** or the keyboard
  shortcuts **Ctrl+]** (deeper) / **Ctrl+[** (shallower). For example:
  ```
  Level 1 Item
  - Level 2 Item
  -- Level 3 Item
  - Level 2 Item B
  ```
- **Colour palette (optional)** — a palette assigning a fill colour per level (format: `1=#4472C4`, one entry per line). See §3.2.

### 5.2 Parsing
- Each non-blank line is one node; blank lines are ignored
- Depth = number of leading `-` characters; the text starts after the dashes and one optional space
- Level = depth + 1 (a line with zero dashes is level 1, one dash is level 2, etc.)
- Depth must not increase by more than one between consecutive non-blank lines; it may decrease by any amount
- The first non-blank line must have zero dashes (depth 0)
- A diagram needs at least 3 nodes

### 5.3 Diagram Generation Algorithm

#### Hierarchy Diagram
1. Identify root node (Level 1)
2. Recursively position children below parent
3. Center children under parent
4. Draw connectors from parent to children

#### Hub & Spoke Diagram
1. Identify hub node (first Level 1 item)
2. Place hub at center
3. Position Level 2 items in radial pattern around hub
4. Position Level 3+ items extending from their parent spokes
5. Draw connectors from hub to spokes, then spokes to secondary items

#### Process Flow Diagram
1. Position Level 1 items sequentially (left to right or top to bottom)
2. Position Level 2+ items branching below/beside their parents
3. Draw sequential connectors between Level 1 items
4. Draw branch connectors for sub-steps

### 5.4 Styling Application
1. Apply palette colors if provided
2. Otherwise, apply LibreOffice theme defaults
3. Scale fonts appropriately per level
4. Ensure text readability (contrast)

---

## 5.5 Packaging & Distribution

The plugin is delivered as a single installable LibreOffice extension (`.oxt`).
The functional requirements for delivery are:

- Installing the extension adds the **SmartArt** menu and its **Create Diagram…**
  command to Impress with no further setup; uninstalling removes them cleanly.
- It installs, registers, and uninstalls without errors through the Extension
  Manager (and `unopkg`), under one stable extension identifier
  (`org.libreimpress.smartart`).
- The menu command must actually reach the plugin's handler when clicked. An
  extension that installs but whose command does not dispatch is a **failure**
  (it shows up as an empty menu), so "the command dispatches" is part of *done*.

*How* this is achieved — the package layout, the UNO component / menu / protocol
registration, and the exact cross-file naming that makes dispatch work (the
"registration naming contract") — is build detail and lives in the phase plans:
**Phase 1** packages the installable `.oxt`, and **Phase 2** adds the menu and
component registration (and documents the naming contract and its silent-failure
traps). The runtime structure — how a click reaches the handler — is shown in
`Architecture_VDiagram.md`; automated verification that registration and dispatch
actually work is described in `TESTING_STRATEGY.md`.

---

## 6. Constraints & Assumptions

### 6.1 Constraints
- Minimum 3 list levels required
- Existing LibreOffice shapes only (no custom shape drawing)
- Single diagram per action (no batch generation)
- Diagrams placed on current active slide
- UNO extension architecture (Java/Python supported)

### 6.2 Assumptions
- User has LibreOffice Impress installed with UNO support
- User provides valid hierarchical text structure
- Indentation clearly indicates nesting (tabs or spaces)
- Color palette (if provided) is in valid format

---

## 7. Error Handling

- **Invalid hierarchy:** Alert user if structure doesn't meet minimum levels
- **Invalid indentation:** Alert if nesting is ambiguous
- **Invalid palette:** Use defaults if palette format is invalid
- **Insufficient data:** Alert if fewer than 3 nodes provided

---

## 8. Future Enhancements (v2.0+)

- Custom shape support
- Diagram style templates (modern, classic, minimal)
- Edit existing diagram
- Diagram type conversion
- Animation support
- Export to other formats
- Undo/Redo integration
- More diagram types (target, balance, picture-based, etc.)

---

## 9. Success Criteria

Status as of 2026-06-16, v0.3.0 (✅ met):

✅ Plugin loads / installs without errors (verified by `uno-tests/run.sh`)  
✅ Dialog accepts text input with indentation  
✅ Minimum 3 levels supported (enforced by the parser)  
✅ User-friendly error messages  
✅ UNO extension architecture implemented  
✅ Hierarchy is visually reflected in output (Phase 4.2)  
✅ Output is grouped and editable (Phase 4.3)  
✅ All diagram types generate correctly — Hierarchy, Hub & Spoke, Process Flow, Sequential Chevron (Phase 4.4)  
✅ Default styling applied when palette missing — built-in palette, font sizing, arrow heads (Phases 5–6)  
✅ Process Flow sub-items rendered in a horizontal row scaled to fit each step's width (Phase 7)  
✅ Hub & Spoke level-3+ children spread in a 90° radial fan around their spoke (Phase 8)  
✅ Sequential Chevron sub-items rendered in a horizontal row scaled to fit each chevron's width (Phase 9)  
✅ Color palette (when provided) is applied per level (Phase 10)  
✅ Cycle and Cycle (Arrows) diagrams render correctly — directed arrows, circular arrangement (Phase 11)  
✅ Pyramid diagram renders stepped tiers with apex at top and base at bottom (Phase 12)  
✅ Cycle (Blocks) renders rectangles in a ring with solid block-arrow shapes pointing clockwise (Phase 13)  
✅ Basic Block List, Vertical Bullet List, Basic Venn, and Basic Matrix render correctly (Phase 14)  
✅ Cohesive aesthetics applied — rounded corners, soft shadows, navy→teal palette, per-shape text fit (Phase 15)

