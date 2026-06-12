# Phase 2: Dialog UI Implementation - Plan

## Objective
Implement the LibreOffice dialog UI that allows users to input their hierarchy text, select a diagram type, and optionally customize colors. The dialog will serve as the entry point for users to begin the diagram generation workflow.

---

## 1. Dialog Design

### User Interface Layout
```
┌─────────────────────────────────────────────────────────┐
│  LibreImpress SmartArt Diagram Generator                │
├─────────────────────────────────────────────────────────┤
│                                                          │
│ Hierarchy Text:                                          │
│ ┌────────────────────────────────────────────────────┐ │
│ │ Enter hierarchical text (indentation = levels)    │ │
│ │ Example:                                           │ │
│ │   Executive                                        │ │
│ │     Department 1                                   │ │
│ │       Team 1-1                                     │ │
│ │     Department 2                                   │ │
│ │       Team 2-1                                     │ │
│ │       Team 2-2                                     │ │
│ │                                                    │ │
│ │ [10+ lines, multiline text box]                   │ │
│ │                                                    │ │
│ └────────────────────────────────────────────────────┘ │
│                                                          │
│ Diagram Type:  [ Hierarchy ▼ ]                          │
│   Options: Hierarchy, Hub & Spoke, Process Flow         │
│                                                          │
│ Color Palette (Optional):                                │
│ ┌────────────────────────────────────────────────────┐ │
│ │ Paste custom JSON or leave blank for defaults     │ │
│ │ {"level1": "#FF6B6B", "level2": "#4ECDC4", ...}  │ │
│ └────────────────────────────────────────────────────┘ │
│                                                          │
│  [ Create Diagram ]  [ Cancel ]  [ Help ]              │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Components
1. **Hierarchy Text Area** - MultilineTextBox
   - Placeholder text with examples
   - Focus on entry point
   - Minimum 10 visible lines

2. **Diagram Type Dropdown** - ComboBox
   - Options: "Hierarchy", "Hub & Spoke", "Process Flow"
   - Default: "Hierarchy"

3. **Color Palette Text Field** - TextField
   - Optional JSON palette
   - Placeholder shows example format
   - Validated in Phase 3

4. **Buttons**
   - "Create Diagram" - Primary action (CAN_DEFAULT=true)
   - "Cancel" - Close dialog
   - "Help" - Open documentation (Phase 4+)

---

## 2. LibreOffice Dialog XML Format

### File: `src/main/resources/dialogs/SmartArtDialog.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<dialog xmlns="http://openoffice.org/2010/dialog"
        xmlns:script="http://openoffice.org/2010/script"
        id="SmartArtDialog"
        left="0" top="0" width="600" height="450"
        closeable="true" moveable="true" resizable="false"
        title="LibreImpress SmartArt Diagram Generator">

    <!-- Hierarchy Text Area Label -->
    <label id="hierarchyLabel">
        <property name="DefaultControl">com.sun.star.awt.UnoControlFixedTextModel</property>
        <property name="PositionX">10</property>
        <property name="PositionY">10</property>
        <property name="Width">150</property>
        <property name="Height">14</property>
        <property name="Label">Hierarchy Text:</property>
    </label>

    <!-- Hierarchy Text Input Area -->
    <textfield id="hierarchyText">
        <property name="DefaultControl">com.sun.star.awt.UnoControlEditModel</property>
        <property name="PositionX">10</property>
        <property name="PositionY">25</property>
        <property name="Width">580</property>
        <property name="Height">150</property>
        <property name="MultiLine">true</property>
        <property name="VScroll">true</property>
        <property name="HScroll">true</property>
        <property name="Text"></property>
        <property name="HelpText">Enter your hierarchy with indentation. Indent with spaces or tabs to create levels.</property>
    </textfield>

    <!-- Diagram Type Label -->
    <label id="diagramTypeLabel">
        <property name="DefaultControl">com.sun.star.awt.UnoControlFixedTextModel</property>
        <property name="PositionX">10</property>
        <property name="PositionY">185</property>
        <property name="Width">100</property>
        <property name="Height">14</property>
        <property name="Label">Diagram Type:</property>
    </label>

    <!-- Diagram Type Dropdown -->
    <combobox id="diagramType">
        <property name="DefaultControl">com.sun.star.awt.UnoControlComboBoxModel</property>
        <property name="PositionX">120</property>
        <property name="PositionY">183</property>
        <property name="Width">200</property>
        <property name="Height">14</property>
        <property name="StringItemList">["Hierarchy", "Hub &amp; Spoke", "Process Flow"]</property>
        <property name="Text">Hierarchy</property>
        <property name="Dropdown">true</property>
    </combobox>

    <!-- Color Palette Label -->
    <label id="paletteLabel">
        <property name="DefaultControl">com.sun.star.awt.UnoControlFixedTextModel</property>
        <property name="PositionX">10</property>
        <property name="PositionY">210</property>
        <property name="Width">200</property>
        <property name="Height">14</property>
        <property name="Label">Color Palette (Optional JSON):</property>
    </label>

    <!-- Color Palette Input -->
    <textfield id="palette">
        <property name="DefaultControl">com.sun.star.awt.UnoControlEditModel</property>
        <property name="PositionX">10</property>
        <property name="PositionY">225</property>
        <property name="Width">580</property>
        <property name="Height">60</property>
        <property name="MultiLine">true</property>
        <property name="VScroll">true</property>
        <property name="Text"></property>
        <property name="HelpText">Optional: Custom color palette as JSON. Example: {"level1": "#FF6B6B", "level2": "#4ECDC4"}</property>
    </textfield>

    <!-- Create Button -->
    <button id="createButton">
        <property name="DefaultControl">com.sun.star.awt.UnoControlButtonModel</property>
        <property name="PositionX">350</property>
        <property name="PositionY">295</property>
        <property name="Width">70</property>
        <property name="Height">18</property>
        <property name="Label">Create Diagram</property>
        <property name="DefaultButton">true</property>
    </button>

    <!-- Cancel Button -->
    <button id="cancelButton">
        <property name="DefaultControl">com.sun.star.awt.UnoControlButtonModel</property>
        <property name="PositionX">430</property>
        <property name="PositionY">295</property>
        <property name="Width">60</property>
        <property name="Height">18</property>
        <property name="Label">Cancel</property>
    </button>

    <!-- Help Button -->
    <button id="helpButton">
        <property name="DefaultControl">com.sun.star.awt.UnoControlButtonModel</property>
        <property name="PositionX">500</property>
        <property name="PositionY">295</property>
        <property name="Width">50</property>
        <property name="Height">18</property>
        <property name="Label">Help</property>
    </button>

</dialog>
```

---

## 3. Java Implementation

### SmartArtDialog.java - Dialog Controller

Key responsibilities:
1. Load dialog XML from resources
2. Create UNO dialog instance
3. Bind button handlers
4. Capture user input
5. Return results (user clicked Create or Cancel)

Implementation approach:
- Use `com.sun.star.ui.dialogs.UnoDialogControl` or `com.sun.star.awt.XDialog`
- Load dialog descriptor from `dialogs/SmartArtDialog.xml`
- Implement `com.sun.star.awt.XDialogEventHandler` for button callbacks
- Store captured input in member variables
- Return true/false on `show()` method

```java
// Pseudocode for show() method:
public boolean show(XComponentContext context) {
    // 1. Load dialog XML from resources
    // 2. Create dialog from descriptor
    // 3. Set up event handlers
    // 4. Show dialog modally (blocks until user input)
    // 5. Capture text from controls
    // 6. Return true if Create clicked, false if Cancel
}
```

### SmartArtCommand.java - Command Executor

Key responsibilities:
1. Receive UNO protocol handler dispatch request
2. Create dialog instance
3. Show dialog to user
4. If user clicked Create:
   - Validate input (Phase 3)
   - Pass to parser (Phase 3)
5. If user clicked Cancel:
   - Cleanup and return

Implementation approach:
```java
// Pseudocode for execute():
public void execute(XComponentContext context) {
    SmartArtDialog dialog = new SmartArtDialog();
    if (dialog.show(context)) {
        // User clicked Create
        String hierarchy = dialog.getHierarchyText();
        String type = dialog.getDiagramType();
        String palette = dialog.getPalette();
        // TODO: Pass to parser in Phase 3
    }
    // If Cancel, method returns and dialog closes
}
```

---

## 4. UNO Integration

### Dialog Loading Mechanism
- Dialog XML stored in: `src/main/resources/dialogs/SmartArtDialog.xml`
- Loaded via UNO `DialogProvider` service
- URL: `vnd.sun.star.extension://org.libreimpress.smartart/dialogs/SmartArtDialog.xml`

### Event Handling
- Button events trigger dialog close
- Create button: closes with OK result
- Cancel button: closes with CANCEL result
- Help button: opens documentation (deferred to Phase 4)

### Context Flow
```
ProtocolHandler (SmartArtCommand)
  ↓
XComponentContext (provided by LibreOffice)
  ↓
SmartArtDialog.show()
  ↓
UNO DialogProvider
  ↓
Load SmartArtDialog.xml
  ↓
Create and show dialog modally
  ↓
Wait for user action
  ↓
Return user's choice + captured data
```

---

## 5. Build & Packaging

### Assembly Configuration
Update `src/main/assembly/oxt.xml` to include:
- Dialog XML files in `dialogs/` directory
- Dialog resources are packaged at root level of .oxt

### .oxt Structure (Phase 2)
```
SmartArt.oxt (ZIP)
├── META-INF/manifest.xml
├── SmartArt.jar                      # Contains SmartArtCommand, SmartArtDialog
├── SmartArtImpl.xml                   # UNO component registration
├── description.xml                   # Extension metadata
├── dialogs/
│   └── SmartArtDialog.xml            # Dialog UI definition
└── icons/
    └── extension.png                 # Extension icon
```

---

## 6. Testing Strategy

### Unit Tests (in Maven)
**SmartArtDialogTest.java:**
- Test dialog instantiation
- Test getter/setter for captured data
- Mock UNO context (if needed)

**SmartArtCommandTest.java (update):**
- Test command initialization
- Test execute() with mocked dialog
- Test dialog callbacks

### Integration Tests (GitHub Actions)
**Artifact Validation (existing):**
- Verify `dialogs/SmartArtDialog.xml` exists in .oxt
- Verify XML is well-formed

**Manual Testing (local):**
- Install extension in LibreOffice Impress
- Open Tools → LibreImpress SmartArt
- Verify dialog appears
- Test input capture (hierarchy, type, palette)
- Test Create/Cancel buttons

---

## 7. Phase 2 Deliverables

At completion of Phase 2, you should have:

✅ **Dialog XML** - `src/main/resources/dialogs/SmartArtDialog.xml`
✅ **SmartArtDialog controller** - Loads and shows dialog, captures input
✅ **SmartArtCommand.execute()** - Opens dialog via UNO
✅ **Dialog integration** - Dialog loads and displays correctly in LibreOffice
✅ **Unit tests** - Dialog and command tests pass locally
✅ **Artifact tests** - Dialog XML present in .oxt (GitHub Actions)
✅ **README updated** - Phase 2 build/test instructions
✅ **Build verification** - `mvn clean test package` succeeds

---

## 8. Verification Checklist

- [ ] Dialog XML is well-formed (parseable)
- [ ] Dialog loads without runtime errors
- [ ] All controls appear with correct labels and layout
- [ ] Input captures correctly in memory
- [ ] Create button returns true from show()
- [ ] Cancel button returns false from show()
- [ ] Dialog closes properly in both cases
- [ ] Help button appears (may be no-op in Phase 2)
- [ ] Unit tests pass locally (`mvn test`)
- [ ] Artifact tests pass in CI (`mvn test -Dtest=ExtensionValidationTest`)
- [ ] .oxt file contains dialog XML
- [ ] Extension can be loaded into LibreOffice without errors

---

## 9. Known Challenges & Solutions

### Challenge 1: UNO Dialog XML Format
**Issue:** LibreOffice dialog XML syntax is specialized  
**Solution:** Use standard ODF dialog format, test incrementally with LibreOffice

### Challenge 2: Loading Dialog from Extension
**Issue:** Dialog must be loaded from packaged .oxt resources  
**Solution:** Use proper `vnd.sun.star.extension://` URI protocol

### Challenge 3: Event Handling in UNO
**Issue:** Button events require proper event listener implementation  
**Solution:** Implement `XDialogEventHandler` with correct method signatures

### Challenge 4: Modal Dialog Blocking
**Issue:** Dialog must block execution until user responds  
**Solution:** Use `XDialog.execute()` which blocks until dialog closes

---

## 10. Next Phase: Phase 3

After Phase 2 completion:
1. **Input validation** - Validate hierarchy text format
2. **Parser implementation** - Convert hierarchy text to internal model
3. **Error handling** - Show error messages to user
4. **Default palettes** - Built-in color scheme library

---

## Notes for Spec Refinement

After completing Phase 2:
1. Document any UNO API quirks encountered
2. Verify dialog usability with test users
3. Refine XML structure based on actual rendering
4. Update error handling requirements
5. Plan Phase 3 with concrete examples

---

**Version:** 0.1.0-SNAPSHOT  
**Phase:** 2 - Dialog UI Implementation  
**Estimated Duration:** 2-3 days  
**Dependencies:** Phase 1 (Complete) ✅
