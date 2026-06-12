# Phase 2 (Revised): Menu Integration - Implementation Plan

## Objective
Add a "SmartArt" menu item under the Insert menu in LibreOffice Impress that users can click to trigger the SmartArt extension.

---

## 1. What's New in Phase 2

In Phase 2, we add:
1. **Java UNO Component** (`SmartArtCommand.java`) — Entry point for the extension
2. **UNO Component Registration** — Updated manifest.xml to register the service
3. **Menu Configuration** (`Addons.xcu`) — Defines the Insert > SmartArt menu item
4. **Compiled JAR** — Package the Java code into the OXT

---

## 2. Architecture Overview

```
Insert Menu (LibreOffice UI)
        ↓
    SmartArt [Menu Item]
        ↓
LibreOffice calls registered UNO service
        ↓
SmartArtCommand.java (execute method)
        ↓
[Phase 3+: Opens dialog, generates diagrams]
```

---

## 3. Files to Create/Modify

### 3.1 Java Source File: SmartArtCommand.java

Location: `src/main/java/org/libreimpress/smartart/SmartArtCommand.java`

**Purpose:** UNO component that handles the menu action

**Key Methods:**
- `execute()` — Called when user clicks menu item
- `getImplementationName()` — Returns service name
- `supportsService()` — Service registry check

**Minimal implementation for Phase 2:**
- Implements `XDispatchProvider` interface (required for menu commands)
- `execute()` shows a simple info message (proof of concept)
- Ready to extend in Phase 3 for actual dialog

### 3.2 Update: META-INF/manifest.xml

**Add UNO component registration:**
```xml
<manifest xmlns="urn:sun:star:package:manifest">
    <entry full-path="/" media-type="application/vnd.sun.star.package:package"/>
    <entry full-path="smartart.jar" media-type="application/vnd.sun.star.java.component.jar"/>
</manifest>
```

This tells LibreOffice:
- There's a JAR file containing UNO components
- Load components from `smartart.jar`

### 3.3 New: UNO Component Descriptor (uno/SmartArtImpl.xml)

Location: `src/main/resources/uno/SmartArtImpl.xml`

**Purpose:** Describes the UNO component and its services

**Content:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<components xmlns="http://openoffice.org/2010/component">
    <component loader="com.sun.star.loader.Java2" uri="jar:*smartart.jar!/org/libreimpress/smartart/SmartArtCommand.class">
        <implementation name="org.libreimpress.smartart.SmartArtCommand">
            <service name="com.sun.star.frame.ProtocolHandler"/>
        </implementation>
    </component>
</components>
```

This tells LibreOffice:
- Find SmartArtCommand class in smartart.jar
- Register it as a ProtocolHandler service
- Service name: `org.libreimpress.smartart.SmartArtCommand`

### 3.4 New: Menu Configuration (Addons.xcu)

Location: `src/main/resources/Addons.xcu`

**Purpose:** Defines the menu item in the LibreOffice UI

**Content:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<oor:items xmlns:oor="http://openoffice.org/2001/registry" xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <item oor:path="/org.openoffice.Office.Common/Menus/org.openoffice.Office.Impress/Insert">
        <node oor:name="SmartArt" oor:op="fuse">
            <prop oor:name="URL" oor:op="fuse"><value>private:factory/smartart</value></prop>
            <prop oor:name="Title" oor:op="fuse"><value>SmartArt</value></prop>
            <prop oor:name="Target" oor:op="fuse"><value>_self</value></prop>
            <prop oor:name="ImageIdentifier" oor:op="fuse"><value>private:factory/smartart</value></prop>
        </node>
    </item>
</oor:items>
```

This tells LibreOffice:
- Add a menu item "SmartArt" under Insert menu
- When clicked, execute URL: `private:factory/smartart`
- This URL is dispatched to SmartArtCommand

---

## 4. Build Process

### Updated pom.xml Behavior
1. Compile `SmartArtCommand.java` → `smartart.jar`
2. Package JAR into OXT
3. Include `uno/SmartArtImpl.xml` in OXT (tells LibreOffice where the component is)
4. Include `Addons.xcu` in OXT (tells LibreOffice about the menu)

### Updated Assembly Descriptor (oxt.xml)
```xml
<fileSets>
    <fileSet>
        <directory>${project.basedir}/src/main/resources</directory>
        <outputDirectory>/</outputDirectory>
        <includes>
            <include>META-INF/**</include>
            <include>description.xml</include>
            <include>uno/**</include>
            <include>Addons.xcu</include>
        </includes>
    </fileSet>
    <fileSet>
        <directory>${project.build.directory}</directory>
        <outputDirectory>/</outputDirectory>
        <includes>
            <include>smartart.jar</include>
        </includes>
    </fileSet>
</fileSets>
```

---

## 5. OXT Structure After Phase 2

```
SmartArt.oxt (ZIP archive)
├── META-INF/
│   ├── manifest.xml              # Updated: Now includes JAR reference
│   └── MANIFEST.MF
├── description.xml               # Extension metadata
├── Addons.xcu                    # Menu configuration
├── smartart.jar                  # Compiled Java code
└── uno/
    └── SmartArtImpl.xml           # Component descriptor
```

---

## 6. Build Instructions

```bash
# Clean and compile
mvn clean compile

# Package OXT
mvn package

# Result
target/SmartArt.oxt               # 50-100KB with JAR included
```

---

## 7. Testing Phase 2

### Manual Testing in LibreOffice

1. **Install the extension:**
   ```bash
   # Linux
   cp target/SmartArt.oxt ~/.config/libreoffice/4/user/extensions/
   
   # Or use LibreOffice GUI:
   Tools → Extensions → Add → Select SmartArt.oxt
   ```

2. **Restart LibreOffice Impress**

3. **Verify menu item:**
   - Open a presentation
   - Click Insert menu
   - Should see "SmartArt" option
   - Click it → Should see info message (proof of concept)

4. **Expected behavior:**
   - Menu click triggers SmartArtCommand.execute()
   - Displays: "SmartArt command executed!"
   - Ready for Phase 3: dialog implementation

---

## 8. Phase 2 Deliverables Checklist

- [ ] `src/main/java/org/libreimpress/smartart/SmartArtCommand.java` — UNO component
- [ ] `src/main/resources/uno/SmartArtImpl.xml` — Component descriptor
- [ ] `src/main/resources/Addons.xcu` — Menu configuration
- [ ] `src/main/resources/META-INF/manifest.xml` — Updated to reference JAR
- [ ] `src/main/resources/assembly/oxt.xml` — Updated assembly descriptor
- [ ] `mvn clean package` builds successfully
- [ ] `target/SmartArt.oxt` created with JAR included
- [ ] OXT structure verified with `unzip -l`
- [ ] Menu item appears in Insert menu (manual testing)
- [ ] Menu click executes SmartArtCommand

---

## 9. What's NOT in Phase 2

- ❌ Dialog (Phase 3)
- ❌ Diagram parsing (Phase 3)
- ❌ Diagram generation (Phase 4+)
- ❌ Icons or styling (Phase 5+)

Phase 2 is purely **infrastructure** — proving the menu integration works.

---

## 10. Key UNO Concepts Used

| Concept | Purpose | Used Here |
|---------|---------|-----------|
| `ProtocolHandler` | Handles URL dispatch | SmartArtCommand implements this |
| `XDispatchProvider` | Routes commands | SmartArtCommand extends this |
| `private:factory/smartart` | Custom URL | Dispatches to SmartArtCommand |
| `Addons.xcu` | Menu registration | Adds Insert > SmartArt menu |
| `uno/SmartArtImpl.xml` | Component descriptor | Tells LibreOffice about component |

---

## 11. Diagram: Menu Dispatch Flow

```
User clicks Insert > SmartArt
           ↓
LibreOffice URL dispatcher receives: private:factory/smartart
           ↓
Dispatcher finds ProtocolHandler for "smartart"
           ↓
SmartArtCommand.queryDispatch() called
           ↓
SmartArtCommand.dispatch() called
           ↓
execute() method runs
           ↓
Info message shown (Phase 2)
OR Dialog opens (Phase 3+)
```

---

## 12. Common Issues & Solutions

### Issue: Menu item doesn't appear
**Solution:** 
- Verify Addons.xcu is in OXT root: `unzip -l target/SmartArt.oxt | grep Addons`
- Check for XML syntax errors in Addons.xcu
- Restart LibreOffice after installing

### Issue: Menu item appears but clicking does nothing
**Solution:**
- Verify uno/SmartArtImpl.xml is in OXT: `unzip -l target/SmartArt.oxt | grep SmartArtImpl`
- Verify smartart.jar is in OXT root: `unzip -l target/SmartArt.oxt | grep smartart.jar`
- Check manifest.xml references the JAR

### Issue: Compilation fails with UNO classes not found
**Solution:**
- Ensure pom.xml has UNO SDK dependencies
- Run `mvn dependency:resolve` to verify dependencies download

---

## 13. Success Criteria

✅ `mvn clean package` builds without errors  
✅ `target/SmartArt.oxt` contains: smartart.jar, Addons.xcu, uno/SmartArtImpl.xml  
✅ Insert menu shows "SmartArt" option after installation  
✅ Clicking menu item executes SmartArtCommand.execute()  
✅ Info message appears confirming execution  

---

## 14. Next Phase: Phase 3

Phase 3 will:
- Open a dialog with text input
- Parse hierarchical text
- Prepare for diagram generation

See `Phase3_ImplementationPlan.md` (to be created)

---

**Status:** Ready for implementation  
**Estimated Time:** 1-2 hours  
**Next Phase:** Phase 3 - Dialog and Text Parsing
