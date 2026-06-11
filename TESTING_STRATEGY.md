# Testing Strategy for LibreImpress SmartArt

## Overview

LibreImpress SmartArt requires **three layers of testing**, each serving a different purpose:

1. **Maven Unit Tests** (Fast, no LibreOffice) - Run every build
2. **Extension Validation Tests** (Maven-based) - Verify packaging integrity
3. **Manual System Tests** (Requires LibreOffice) - Manual, ad-hoc

This document explains each layer, when to use them, and why we can't fully automate LibreOffice testing.

---

## Layer 1: Unit Tests (Always Run in Maven)

**Location:** `src/test/java/org/libreimpress/smartart/SmartArtCommandTest.java`

**Purpose:** Test business logic in isolation

**Test Cases:**
- Component naming and registration
- Dialog property getters/setters
- Skeleton method invocations

**Run With:**
```bash
mvn test
# or specifically:
mvn test -Dtest=SmartArtCommandTest
```

**When:** Every build (part of `mvn package`)

**Why:** Catches logic errors before packaging

---

## Layer 2: Extension Validation Tests (Maven-based)

**Location:** `src/test/java/org/libreimpress/smartart/ExtensionValidationTest.java`

**Purpose:** Validate .oxt package structure and integrity **without requiring LibreOffice**

**Test Cases (12 total):**

1. ✅ `.oxt file exists`
2. ✅ `.oxt file is not empty`
3. ✅ `.oxt is a valid ZIP archive`
4. ✅ Contains `META-INF/MANIFEST.MF`
5. ✅ Contains `SmartArtImpl.xml` (UNO descriptor)
6. ✅ Contains `description.xml` (extension metadata)
7. ✅ Contains compiled JAR
8. ✅ Contains `dialogs/` directory
9. ✅ Manifest has valid format (Manifest-Version, Created-By)
10. ✅ Component descriptor contains valid XML
11. ✅ Description XML contains valid format
12. ✅ .oxt file size is reasonable (100KB - 10MB)

**Run With:**
```bash
mvn clean package
mvn test -Dtest=ExtensionValidationTest
```

**When:** After packaging (validates assembly output)

**Result:** **12/12 tests passed** ✅

---

## Layer 3: Manual System Tests (LibreOffice Required)

**Location:** `SYSTEM_TESTS.md` (documentation)

**Purpose:** Verify LibreOffice can actually install and use the extension

### Test 1: Extension Installation

**Steps:**
1. Copy `target/SmartArt.oxt` to your Windows/Mac/Linux machine
2. Open LibreOffice Impress
3. Menu: `Tools → Extensions → Extension Manager`
4. Click `Add` button
5. Select `SmartArt.oxt`
6. Accept license dialog
7. Restart LibreOffice

**Expected Result:**
- ✅ Extension appears in Extension Manager as "LibreImpress SmartArt"
- ✅ Version shows "0.1.0"
- ✅ No error dialogs

**If it fails:**
- Check LibreOffice error logs
- Verify .oxt file integrity with `unzip -l SmartArt.oxt`
- Check manifest is valid: `unzip -p SmartArt.oxt META-INF/MANIFEST.MF | cat -A`

### Test 2: Component Registration (Phase 2+)

**Steps:**
1. After installation, Tools → Macros → Organize Macros → LibreOffice Basic
2. Look for SmartArtCommand in component tree

**Expected Result:**
- ✅ SmartArtCommand visible in components list

### Test 3: Menu Integration (Phase 2+)

**Steps:**
1. Open Insert menu
2. Look for "SmartArt" menu item

**Expected Result:**
- ✅ "SmartArt" menu item appears
- ✅ Click opens dialog

---

## Why We Can't Fully Automate LibreOffice Testing

### Problem 1: No Headless LibreOffice in Maven

UNO components require:
- Running LibreOffice process
- XComponentContext initialization
- Active document/slide context

Maven runs in **isolation** - no LibreOffice runtime available.

### Problem 2: Dialog Testing

- Dialogs are GUI elements
- Can't programmatically verify appearance
- Testing requires manual interaction
- No standard automation framework for LibreOffice dialogs

### Problem 3: Component Registration

- Requires LibreOffice registry update
- Each LibreOffice installation different
- Can't mock LibreOffice's component loader

---

## What IS Testable in Maven (and IS)

✅ **Package structure validation** - Done in ExtensionValidationTest  
✅ **XML format validation** - Done in ExtensionValidationTest  
✅ **JAR integrity** - Done in ExtensionValidationTest  
✅ **Business logic (parsers, generators)** - Done in Phase 3+  
✅ **File encodings** - Done in ExtensionValidationTest  

---

## What MUST Be Manual

❌ **Extension installation** - Requires LibreOffice  
❌ **Menu appearance** - GUI verification  
❌ **Dialog rendering** - GUI verification  
❌ **UNO component interaction** - Requires LibreOffice runtime  
❌ **End-to-end diagram generation** - Needs LibreOffice  

---

## Test Execution in CI/CD

### For GitHub Actions / Jenkins:

```yaml
# Can run:
mvn clean package    # Includes all Maven tests (4 + 12 = 16 tests)

# Cannot run:
# - Manual LibreOffice tests (requires X11/display, LibreOffice install)
```

### Recommended CI/CD Strategy

1. **Run on every commit:**
   - `mvn clean package` (all Maven tests included)
   - Reports test results
   - Builds .oxt artifact

2. **Run nightly (optional):**
   - Deploy .oxt to LibreOffice test environment
   - Run manual tests on actual LibreOffice
   - Report failures

3. **Pre-release:**
   - Manual verification on Windows, Mac, Linux
   - Test with multiple LibreOffice versions (7.4, 8.0, etc.)

---

## Current Test Status (Phase 1)

| Layer | Tests | Passing | Status |
|-------|-------|---------|--------|
| Unit Tests | 4 | 4 | ✅ Pass |
| Extension Validation | 12 | 12 | ✅ Pass |
| Manual/System Tests | N/A | N/A | ⏳ Manual only |

**Total Maven Tests: 16/16 passing** ✅

---

## Adding Tests for Phase 2+ Features

### Parser Tests (Phase 3)
```java
@Test
public void testParserValidHierarchy() {
    HierarchyParser parser = new HierarchyParser();
    Hierarchy h = parser.parse("Level1\n  Level2\n    Level3");
    assertEquals(3, h.getDepth());
}
```
**Type:** Maven unit test ✅

### Palette Tests (Phase 4)
```java
@Test
public void testPaletteJsonParsing() {
    PaletteParser parser = new PaletteParser();
    ColorPalette palette = parser.parse("{\"level1\": \"#FF0000\"}");
    assertEquals("#FF0000", palette.getColor(1));
}
```
**Type:** Maven unit test ✅

### Generator Tests (Phase 5)
```java
@Test
public void testHierarchyGeneratorPositions() {
    HierarchyDiagramGenerator gen = new HierarchyDiagramGenerator();
    List<GeneratedShape> shapes = gen.generate(hierarchy);
    assertEquals(3, shapes.size());  // Should have 3 shapes
}
```
**Type:** Maven unit test ✅

### Renderer Tests (Phase 5.1)
```java
@Test
public void testRendererCreatesUnoShapes() {
    DiagramRenderer renderer = new DiagramRenderer(shapes);
    // Note: Can't test UNO XShape, but can test shape conversion logic
}
```
**Type:** Partial Maven, partial manual ⚠️

### Integration Tests (Phase 7)
```java
@Test
public void testEndToEndPipelineWithMockLibreOffice() {
    // Would use mock LibreOfficeHelper for testing
}
```
**Type:** Maven integration test with mocks ✅

---

## Test Execution Quick Reference

```bash
# Run all Maven tests (Unit + Validation)
mvn clean test
# Result: 16 tests run, all automated

# Run only extension validation tests
mvn test -Dtest=ExtensionValidationTest
# Result: 12 tests verifying .oxt integrity

# Run only unit tests
mvn test -Dtest=SmartArtCommandTest
# Result: 4 tests verifying logic

# Full build with packaging (recommended)
mvn clean package
# Result: All tests + .oxt artifact created

# Manual testing (requires LibreOffice on your system)
# 1. Copy target/SmartArt.oxt
# 2. Open LibreOffice Impress
# 3. Tools → Extensions → Add SmartArt.oxt
# 4. Verify installation
```

---

## Summary: Testing Strategy

✅ **What we test automatically (Maven):**
- Code logic (SmartArtCommand, SmartArtDialog, etc.)
- Extension package integrity (structure, format, content)
- All 16 automated tests pass every build

⏳ **What requires manual testing:**
- Installation in actual LibreOffice
- Menu integration
- Dialog appearance
- End-to-end functionality

This approach balances:
- **Speed** - Automated tests run in seconds
- **Coverage** - Validation prevents packaging errors
- **Practicality** - Manual tests only where necessary
- **Continuity** - CI/CD can run full automated suite

---

## Next Phase: Phase 2 Dialog Implementation

When implementing the dialog in Phase 2:
1. Add unit tests for dialog logic (can test in Maven)
2. Manual verification in LibreOffice (dialog appearance)
3. No new automated test framework needed - keep current approach
