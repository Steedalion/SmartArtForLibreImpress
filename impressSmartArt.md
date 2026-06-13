# LibreImpress SmartArt Plugin - Specification

## 1. Overview

**Project:** LibreImpress SmartArt Plugin  
**Purpose:** A UNO extension for LibreOffice Impress that enables users to quickly create structured diagrams from hierarchical text input.  
**Architecture:** UNO extension implemented in **Java**, packaged as a `.oxt`.

### 1.1 Specification Document Hierarchy

This file (`impressSmartArt.md`) is the **master specification** — the single
source of truth for *what* the plugin does and the packaging/registration rules
it must satisfy (§5.5). All other documents sit beneath it and must stay
consistent with it:

```
impressSmartArt.md                 ← master spec (this file): scope, behaviour, packaging rules
│
├── Phase plans (how each phase is built, in order)
│   ├── Phase1_ImplementationPlan.md   — Phase 1: Empty OXT extension (installable skeleton)
│   ├── Phase2_ImplementationPlan.md   — Phase 2: Menu integration (top-level menu entry → dispatch)
│   └── Phase3_ImplementationPlan.md   — Phase 3: Dialog & text parsing
│
├── Architecture_VDiagram.md       — architecture overview & V-model development process
├── TESTING_STRATEGY.md            — testing approach (Java unit · OXT structure · runtime dispatch)
└── README.md                      — build, install, and run instructions
```

**Conventions:**
- Each phase delivers one installable increment; later phases build on earlier ones.
- There is exactly one plan per phase (no "revised"/duplicate variants).
- When the master spec and a phase plan disagree, the master spec wins; update
  the phase plan to match.

### 1.2 Implementation Status (as of 2026-06-13)

| Phase | Scope | Status |
|-------|-------|--------|
| 1 | Empty, installable `.oxt` skeleton | ✅ Done |
| 2 | Top-level **SmartArt** menu → dispatch to the Java handler | ✅ Done |
| 3 | Input dialog + hierarchy parser (validate, preview the parsed tree) | ✅ Done |
| 4 | Render the parsed tree as grouped, editable shapes on the slide | ⏳ Next |
| later | Per-level colour palette & styling; Hub & Spoke / Process Flow layouts | ⏳ Planned |

**What works today:** clicking **SmartArt → Create Diagram…** opens a
programmatic dialog (diagram-type dropdown + multiline text); on **Create** the
indented text is parsed into a validated hierarchy (each indentation step = one
level; ≥ 3 nodes and ≥ 3 levels required) and the parsed tree — or a specific
error — is shown. **Shapes are not drawn yet** (Phase 4), and the colour-palette
input described in §3.2 / §5.1 is deferred to a later phase. Sections 2–8 below
describe the *target* product; see this table for what is implemented now.

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
  - Support for list level indentation (minimum 3 levels deep)
  - List levels organize the hierarchy structure
  - Each line represents one node
  - Indentation/nesting indicates parent-child relationships

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

#### 2.2.3 Process Flow Diagram
- **Purpose:** Sequential steps or workflow
- **Level Structure:**
  - Level 1: Primary process steps
  - Level 2+: Sub-steps or decision points
- **Layout:** Linear/sequential arrangement
- **Connections:** Sequential connections between steps; sub-steps branch from main path

---

## 3. Shape & Styling System

### 3.1 Shape Usage
- Use existing LibreOffice shapes (rectangles, circles, diamonds, etc.)
- Shapes selected based on diagram type and level:
  - **Hierarchy:** Rectangles for all levels
  - **Hub & Spoke:** Circle for hub, rounded rectangles for spokes
  - **Process Flow:** Rectangles for steps, diamonds for decisions

### 3.2 Styling & Color Palette
> ⏳ *Target feature — not yet implemented (deferred to a later phase). The
> current dialog has no palette field.*
- **Input:** Optional `@paletteObject` parameter
- **Default Behavior:** If no palette provided, use LibreOffice template defaults
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
1. User selects diagram type (Hierarchy / Hub & Spoke / Process Flow)
2. User enters text points in multi-line input with indentation
3. User (optionally) provides a color palette object *(planned; not in the
   current dialog)*
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

- **Diagram type** — one of Hierarchy, Hub & Spoke, or Process Flow (a dropdown).
- **Text points** — a multi-line field that **is a list and stays a list**: it
  opens pre-filled with a starter outline, each line is one node, and leading
  indentation expresses nesting (per the rules in §5.2). It behaves as an outline
  editor — **Enter** starts a new item at the current level, **Tab** indents the
  current line one level deeper, **Shift+Tab** moves it one level shallower (Tab
  never moves focus out of the field). For example:
  - Level 1 Item
    - Level 2 Item
      - Level 3 Item
    - Level 2 Item B
- **Colour palette (optional)** — a palette assigning a fill colour, and
  optionally font and shape styling, per level (see §3.2). *(Planned; not in the
  current dialog.)*

### 5.2 Parsing
- Parse text input to identify hierarchy levels based on indentation
- Validate that hierarchy depth is at least 3 levels for all types
- Extract node text and parent-child relationships
- Validate against diagram type constraints

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
- More diagram types (matrix, venn, pyramid, etc.)

---

## 9. Success Criteria

Status as of 2026-06-13 (✅ met · ⏳ pending the noted phase):

✅ Plugin loads / installs without errors (verified by `tools/verify-extension.sh`)  
✅ Dialog accepts text input with indentation  
✅ Minimum 3 levels supported (enforced by the parser)  
✅ User-friendly error messages  
✅ UNO extension architecture implemented  
⏳ Hierarchy is visually reflected in output — Phase 4  
⏳ Output is grouped and editable — Phase 4  
⏳ All three diagram types generate correctly — Phase 4+  
⏳ Color palette (when provided) is applied — later phase  
⏳ Default styling applied when palette missing — later phase


