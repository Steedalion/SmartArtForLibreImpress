# LibreImpress SmartArt Plugin - Specification

## 1. Overview

**Project:** LibreImpress SmartArt Plugin  
**Purpose:** A UNO extension for LibreOffice Impress that enables users to quickly create structured diagrams from hierarchical text input.  
**Architecture:** UNO Extension (Java/Python-based or native)

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
- **Input:** Optional `@paletteObject` parameter
- **Default Behavior:** If no palette provided, use LibreOffice template defaults
- **Palette Structure:**
  - Color definitions per level
  - Optional font styling (size, family, weight)
  - Optional shape styling (fill, border)

---

## 4. User Workflow

### 4.1 Triggering the Plugin
1. User opens LibreImpress presentation
2. User selects menu: **Insert > SmartArt** (or similar)
3. Plugin dialog opens

### 4.2 Creating a Diagram
1. User selects diagram type (Hierarchy / Hub & Spoke / Process Flow)
2. User enters text points in multi-line input with indentation
3. User (optionally) provides color palette object
4. User clicks **Create**
5. Plugin generates diagram on current slide

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

✅ Plugin loads without errors  
✅ Dialog accepts text input with indentation  
✅ All three diagram types generate correctly  
✅ Hierarchy is visually reflected in output  
✅ Color palette (when provided) is applied  
✅ Default styling applied when palette missing  
✅ Output is grouped and editable  
✅ Minimum 3 levels supported  
✅ User-friendly error messages  
✅ UNO extension architecture implemented


