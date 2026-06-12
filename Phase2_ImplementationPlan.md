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

3. **Buttons**
   - "Create Diagram" - Primary action (CAN_DEFAULT=true)
   - "Cancel" - Close dialog
   - "Help" - No-op placeholder (Phase 4+)

---

## 2. Dialog Implementation Approach

### Programmatic Dialog Construction (Java-based)

Instead of using XML, we'll build the dialog entirely in Java code. This approach:
- Eliminates need for separate dialog XML files
- Simpler resource management (no XML parsing)
- More code, but easier to understand and debug
- Aligns with our UNO integration strategy

The `SmartArtDialog.show()` method will:
1. Create UNO `XDialog` instance
2. Programmatically add all controls (labels, text fields, dropdowns, buttons)
3. Set properties (position, size, text, callbacks)
4. Show dialog modally
5. Capture user input and return result

### Dialog Structure (programmatic)

```
SmartArtDialog {
  XDialog dialog
  XTextComponent hierarchyText        // Multiline text area
  XComboBox diagramType              // Dropdown: Hierarchy, Hub & Spoke, Process Flow
  XButton createButton               // Primary action
  XButton cancelButton               // Dismiss
  XButton helpButton                 // No-op in Phase 2 (Phase 4+)
}
```

### Note: Color Palettes

Color palette selection deferred to Phase 3+. Phase 2 uses default colors only.

---

## 3. Java Implementation

### SmartArtDialog.java - Dialog Controller

Key responsibilities:
1. Build dialog programmatically (no XML)
2. Create and show UNO dialog
3. Validate input in real-time (Phase 2)
4. Capture user input
5. Return results (true if Create, false if Cancel)

Implementation details:
- Extends/implements UNO dialog handling
- `show(XComponentContext context)` creates dialog, shows modally, returns boolean
- Real-time validation: Check hierarchy not empty, consistent indentation
- Show error messages in dialog via `XMessageBox` if validation fails
- Button handlers close dialog with appropriate result code

Input validation (Phase 2):
- **Hierarchy text**: Not empty, basic indentation check (consistent spacing)
- **Diagram type**: Required (dropdown always has selection)
- **Color palette**: Not validated (preset selection always valid)
- Error display: Show message box if validation fails, don't allow Create

```java
public class SmartArtDialog {
    private String hierarchyText;
    private String diagramType = "Hierarchy";
    
    public boolean show(XComponentContext context) {
        // 1. Create dialog programmatically
        // 2. Create controls (labels, text area, dropdown, buttons)
        // 3. Set event handlers for buttons
        // 4. Show dialog modally
        // 5. Validate input on Create click
        // 6. Return true if Create valid, false if Cancel
    }
    
    private boolean validateInput() {
        if (hierarchyText == null || hierarchyText.trim().isEmpty()) {
            showError("Hierarchy text cannot be empty");
            return false;
        }
        if (!validateIndentation(hierarchyText)) {
            showError("Indentation must be consistent (use spaces or tabs, not mixed)");
            return false;
        }
        return true;
    }
    
    private boolean validateIndentation(String text) {
        // Check indentation is consistent (all spaces or all tabs, not mixed)
        // Allow reasonable nesting depth
    }
}
```

### SmartArtCommand.java - Command Executor

Key responsibilities:
1. Act as UNO ProtocolHandler entry point
2. Create and show dialog
3. On user Create: Store input for Phase 3 processing
4. On user Cancel: Cleanup and return

```java
public class SmartArtCommand {
    public void execute(XComponentContext context) {
        SmartArtDialog dialog = new SmartArtDialog();
        if (dialog.show(context)) {
            // User clicked Create - input is validated
            String hierarchy = dialog.getHierarchyText();
            String type = dialog.getDiagramType();
            // TODO: Pass to parser in Phase 3
            System.out.println("Creating " + type + " diagram");
        }
        // If Cancel, method returns and dialog closes
    }
}
```

---

## 4. UNO Integration

### Dialog Creation Mechanism
- No XML file needed; dialog built entirely in Java
- `SmartArtDialog.show()` creates `XDialog` and adds controls programmatically
- Simpler packaging (no extra XML resources to manage)

### Event Handling
- Button click listeners capture user action
- Create button: Validates input, returns true if valid, false if error
- Cancel button: Returns false immediately
- Help button: No-op in Phase 2 (clickable, does nothing)

### Context Flow
```
ProtocolHandler dispatch (LibreOffice)
  ↓
SmartArtCommand.execute(XComponentContext)
  ↓
Create SmartArtDialog instance
  ↓
SmartArtDialog.show(context)
  ↓
Build XDialog programmatically
  ↓
Add controls (text area, dropdown, buttons)
  ↓
Set event handlers
  ↓
Show dialog modally (blocks)
  ↓
User interacts, clicks button
  ↓
Validate input (if Create)
  ↓
Return boolean result + captured data
```

### Required UNO Interfaces
- `com.sun.star.awt.XDialog` - Dialog window
- `com.sun.star.awt.XTextComponent` - Text input field
- `com.sun.star.awt.XComboBox` - Dropdown list
- `com.sun.star.awt.XButton` - Buttons
- `com.sun.star.awt.XMessageBox` - Error messages during validation

---

## 5. Build & Packaging

### No Assembly Changes Needed
Dialog is built programmatically in Java, no additional resources required. Existing `src/main/assembly/oxt.xml` configuration is sufficient.

### .oxt Structure (Phase 2)
```
SmartArt.oxt (ZIP)
├── META-INF/manifest.xml
├── SmartArt.jar                      # Contains SmartArtCommand, SmartArtDialog
├── SmartArtImpl.xml                   # UNO component registration
├── description.xml                   # Extension metadata
└── icons/
    └── extension.png                 # Extension icon
```

---

## 6. Testing Strategy

### Unit Tests (in Maven)
**SmartArtDialogTest.java:**
- Test dialog instantiation
- Test getter/setter for captured data
- Test input validation logic (empty text, indentation)
- Mock UNO context (if needed)

**SmartArtCommandTest.java (update):**
- Test command initialization
- Test execute() with mocked dialog
- Test dialog callbacks (Create/Cancel)

### Integration Tests (GitHub Actions)
**Artifact Validation (existing):**
- Verify SmartArt.jar exists in .oxt
- Verify all required files present

**Manual Testing (local):**
- Install extension in LibreOffice Impress
- Open Tools → LibreImpress SmartArt
- Verify dialog appears
- Test input capture (hierarchy text, diagram type)
- Test validation (error on empty text, indentation errors)
- Test Create/Cancel buttons

---

## 7. Phase 2 Deliverables

At completion of Phase 2, you should have:

✅ **SmartArtDialog controller** - Builds dialog programmatically, shows dialog, validates input, captures data
✅ **SmartArtCommand.execute()** - Opens dialog via UNO ProtocolHandler
✅ **Dialog integration** - Dialog displays and responds correctly in LibreOffice
✅ **Input validation** - Validates empty text, checks indentation consistency, shows error messages
✅ **Unit tests** - Dialog and command tests pass locally
✅ **Artifact validation** - Artifact tests pass in GitHub Actions
✅ **README updated** - Phase 2 build/test instructions
✅ **Build verification** - `mvn clean test package` succeeds

---

## 8. Verification Checklist

- [ ] Dialog creates and displays without errors
- [ ] All controls appear with correct labels and layout
- [ ] Hierarchy text area accepts multiline input
- [ ] Diagram type dropdown has 3 options (Hierarchy, Hub & Spoke, Process Flow)
- [ ] Input captures correctly in memory
- [ ] Empty text validation works (shows error)
- [ ] Indentation validation works (detects mixed spaces/tabs)
- [ ] Create button returns true from show() when input valid
- [ ] Cancel button returns false from show()
- [ ] Dialog closes properly in both cases
- [ ] Help button appears and is clickable (no-op)
- [ ] Unit tests pass locally (`mvn test`)
- [ ] Artifact tests pass in CI (`mvn test -Dtest=ExtensionValidationTest`)
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
