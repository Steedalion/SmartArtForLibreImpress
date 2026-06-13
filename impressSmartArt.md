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

**Input Dialog:**
```
Diagram Type: [Dropdown: Hierarchy / Hub & Spoke / Process Flow]

Text Points:
[Multiline text area with indentation support]
Level 1 Item
  Level 2 Item
    Level 3 Item
  Level 2 Item B
    Level 3 Item B1
    Level 3 Item B2

Color Palette (Optional):
[Text field or JSON input]
{
  "level1": {"fill": "#FF0000", "font": "Arial", "fontSize": 14},
  "level2": {"fill": "#00FF00", "font": "Arial", "fontSize": 12},
  "level3": {"fill": "#0000FF", "font": "Arial", "fontSize": 10}
}

[Create Button] [Cancel Button]
```

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

## 5.5 Packaging & UNO Registration (Extension Infrastructure)

The plugin ships as a `.oxt` (a ZIP). Getting it to install and expose a menu
requires several files to agree exactly. These requirements were verified by
installing the built `.oxt` with `unopkg` (see §5.5.4); each was a real
failure mode encountered during development.

### 5.5.1 Required files inside the `.oxt`
| File | Purpose |
|------|---------|
| `META-INF/manifest.xml` | Package manifest: registers every `.xcu` and the component descriptor |
| `description.xml` | Extension metadata (identifier, version, display name) |
| `Addons.xcu` | Adds the menu entry to the LibreOffice UI |
| `ProtocolHandler.xcu` | Binds the command-URL protocol to the Java handler |
| `uno/SmartArtImpl.xml` | UNO component descriptor (which class implements which service) |
| `uno/smartart.jar` | Compiled Java code (must sit beside the descriptor — see §5.5.3 rule 2) |

### 5.5.2 Name-matching contract (the part that made the menu hard)

The menu only works when the same identifier strings are repeated **verbatim**
across several files. A single differing character (e.g. `…/2010/component` vs
`…/2010/uno-components`) fails silently or with an opaque error. Each token below
must be **identical** in every listed location; to repurpose this extension,
change a token in *all* its locations at once:

| Token | Value (this project) | Must be identical in |
|-------|----------------------|----------------------|
| Component namespace | `http://openoffice.org/2010/uno-components` | `uno/SmartArtImpl.xml` root `xmlns` |
| Implementation name | `org.libreimpress.smartart.SmartArtCommand` | `SmartArtImpl.xml` `<implementation name>` · `ProtocolHandler.xcu` `HandlerSet` child `oor:name` · JAR `MANIFEST.MF` `RegistrationClassName` · the Java class (FQN + `IMPLEMENTATION_NAME`) |
| Service name | `com.sun.star.frame.ProtocolHandler` | `SmartArtImpl.xml` `<service name>` · Java `SERVICE_NAME` / `getSupportedServiceNames()` |
| Command protocol prefix | `org.libreimpress.smartart` | `ProtocolHandler.xcu` `Protocols` (as `…:*`) · the part of the `Addons.xcu` menu `URL` before the `:` · the Java `dispatch`/`queryDispatch` `startsWith(...)` guard |
| Full command URL | `org.libreimpress.smartart:CreateDiagram` | `Addons.xcu` menu item `URL` value |
| Extension identifier | `org.libreimpress.smartart` | `description.xml` `<identifier value>` · the argument to `unopkg remove` |
| Component jar name | `smartart.jar` | `SmartArtImpl.xml` `uri` · assembly `oxt.xml` include · pom jar `finalName` (packaged as `uno/smartart.jar`, beside the descriptor — see §5.5.3 rule 2) |

> The command **protocol prefix** and the **extension identifier** happen to be
> the same text (`org.libreimpress.smartart`) but are independent roles: the
> first routes menu clicks to the handler, the second names the installed
> package. They need not be equal, and changing one does not change the other.

### 5.5.3 Exact-match rules (each one is a silent-failure trap)
1. **Component descriptor namespace** — `uno/SmartArtImpl.xml` must use
   `xmlns="http://openoffice.org/2010/uno-components"`. A wrong namespace makes
   `unopkg`/LibreOffice throw `InvalidRegistryException: unexpected item in outer
   level` (the C++ parser rejects the root element before reading attributes).
   This is the same namespace LibreOffice's own `program/services.rdb` uses.
2. **Component `uri`** — must be the bare jar name, `uri="smartart.jar"` (not a
   `jar:*…!/…Class` URL). **The `uri` is resolved relative to the descriptor's
   own location**, so the jar must sit in the *same directory* as
   `SmartArtImpl.xml`. Because the descriptor lives in `uno/`, the jar is
   packaged as `uno/smartart.jar`. Get this wrong and the component fails to load
   with `java.io.FileNotFoundException: …/uno/smartart.jar`; the dispatch then
   returns null and LibreOffice **silently hides the menu item** (the top-level
   menu shows but its submenu is empty — identical symptom to rule 6).
3. **`description.xml` identifier/version use `value` attributes** —
   `<identifier value="org.libreimpress.smartart"/>` and
   `<version value="0.1.0"/>`. Using element *text*
   (`<identifier>…</identifier>`) is ignored; LibreOffice then falls back to a
   `org.openoffice.legacy.<filename>` identifier, which also breaks
   `unopkg remove org.libreimpress.smartart`.
4. **`display-name`/`publisher`** use child `<name lang="en">…</name>` elements,
   not direct text.
5. **`Addons.xcu`** must be `oor:component-data` merging into
   `org.openoffice.Office.Addons / AddonUI` (under `OfficeMenuBar` for a
   top-level menu, or `AddonMenu` for Tools → Add-Ons). Every `prop` declares
   `oor:type` and uses `oor:op="replace"`.
6. **Localized `Title` needs an empty default** — each `Title` prop must list a
   bare `<value/>` *before* the `<value xml:lang="en-US">…</value>`. Without the
   default, LibreOffice cannot resolve a title for the running locale and
   **silently drops the menu item** — the top-level menu appears but its submenu
   is empty. (The top-level menu still shows because containers fall back to the
   node name; leaf items do not.)
7. **`ProtocolHandler.xcu`** `HandlerSet` node name must equal the
   `implementation name` in `uno/SmartArtImpl.xml`, and its `Protocols` value
   (`org.libreimpress.smartart:*`) must match the prefix of the menu command URL.
8. **Java side** — the implementation class must expose static
   `__getComponentFactory(String)` and `__writeRegistryServiceInfo(XRegistryKey)`
   (built via `com.sun.star.lib.uno.helper.Factory`), and the JAR's
   `META-INF/MANIFEST.MF` must declare
   `RegistrationClassName: org.libreimpress.smartart.SmartArtCommand`.

### 5.5.4 Install verification (local and CI)
Structural checks alone do not catch a bad namespace, identifier, or a
mis-placed jar — those only fail at *runtime*, when LibreOffice tries to load the
component and dispatch the command. The authoritative test therefore installs the
`.oxt` into a throwaway profile, starts a **headless** LibreOffice, and asserts
via UNO that the menu item is registered **and** that the command actually
dispatches (a null dispatch is why a broken extension shows an empty submenu).

This is automated as a committed test, `tools/verify-extension.sh` (orchestrator)
+ `tools/probe_extension.py` (the UNO probe):

```bash
mvn clean package
xvfb-run -a bash tools/verify-extension.sh target/SmartArt.oxt
#   PASS: config has menu 'org.libreimpress.smartart' with submenu items ['m1'] …
#   PASS: queryDispatch('org.libreimpress.smartart:CreateDiagram') -> …SmartArtCommand
#   VERIFY PASS
```

The GitHub Actions workflow (`.github/workflows/build-and-validate.yml`) runs
this on every push: build → validate OXT structure → install LibreOffice +
`python3-uno` → `xvfb-run … tools/verify-extension.sh`. A registration **or
dispatch** regression (wrong namespace, bad identifier, jar in the wrong
directory, missing file) fails CI instead of only surfacing during a manual
install. See `TESTING_STRATEGY.md` for the full three-layer test plan.

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


