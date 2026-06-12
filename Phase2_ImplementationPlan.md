# Phase 2: Menu Integration - Implementation Plan

## Objective
Add a "SmartArt" menu item under the Insert menu in LibreOffice Impress that users can click to trigger the SmartArt extension.

---

## 1. What's New in Phase 2

In Phase 2, we add:
1. **Java UNO Component** (`SmartArtCommand.java`) — Entry point for the extension
2. **UNO Component Registration** — Updated manifest.xml to register the service
3. **Menu Configuration** (`Addons.xcu`) — Adds a top-level **SmartArt** menu
4. **Protocol Handler binding** (`ProtocolHandler.xcu`) — Routes the menu command to the Java code
5. **Compiled JAR** — Package the Java code into the OXT

> ⚠️ **The hard part of Phase 2 is naming, not code.** The same identifier
> strings must be repeated verbatim across `SmartArtImpl.xml`,
> `ProtocolHandler.xcu`, `Addons.xcu`, `description.xml`, the JAR manifest, and
> the Java class — a single off-by-one character (famously
> `…/2010/component` instead of `…/2010/uno-components`) fails silently or with
> an opaque `InvalidRegistryException`. Before changing any name here, consult
> the **name-matching contract** and **exact-match rules** in the master spec
> ([`impressSmartArt.md`](impressSmartArt.md) §5.5.2–§5.5.3); they are the
> authoritative, repeatable checklist.

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

**Register every configuration file and the UNO component descriptor.** Use the
`manifest:` namespace and `manifest:file-entry` elements (not the older
`urn:sun:star:package:manifest` / `entry` form). Each `.xcu` is
`configuration-data`; the component descriptor is `uno-components`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<manifest:manifest xmlns:manifest="http://openoffice.org/2001/manifest">
    <manifest:file-entry
        manifest:media-type="application/vnd.sun.star.package:package"
        manifest:full-path="/"/>
    <manifest:file-entry
        manifest:media-type="application/vnd.sun.star.configuration-data"
        manifest:full-path="Addons.xcu"/>
    <manifest:file-entry
        manifest:media-type="application/vnd.sun.star.configuration-data"
        manifest:full-path="ProtocolHandler.xcu"/>
    <manifest:file-entry
        manifest:media-type="application/vnd.sun.star.uno-components"
        manifest:full-path="uno/SmartArtImpl.xml"/>
</manifest:manifest>
```

This tells LibreOffice:
- `Addons.xcu` and `ProtocolHandler.xcu` are configuration data to merge
- `uno/SmartArtImpl.xml` describes the UNO component(s) to load
- The `smartart.jar` itself is referenced from inside the component descriptor
  (`uri="smartart.jar"`), so it does **not** need its own manifest entry

### 3.3 New: UNO Component Descriptor (uno/SmartArtImpl.xml)

Location: `src/main/resources/uno/SmartArtImpl.xml`

**Purpose:** Describes the UNO component and its services

**Content:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<components xmlns="http://openoffice.org/2010/uno-components">
    <component loader="com.sun.star.loader.Java2" uri="smartart.jar">
        <implementation name="org.libreimpress.smartart.SmartArtCommand">
            <service name="com.sun.star.frame.ProtocolHandler"/>
        </implementation>
    </component>
</components>
```

This tells LibreOffice:
- Find SmartArtCommand class in smartart.jar
- Register it as a ProtocolHandler service
- Implementation name: `org.libreimpress.smartart.SmartArtCommand`

> ⚠️ **Two mistakes here will fail the install with
> `InvalidRegistryException: unexpected item in outer level`:**
> 1. **Namespace must be `http://openoffice.org/2010/uno-components`** — *not*
>    `.../2010/component`. The C++ parser in `servicemanager.cxx` rejects the
>    root `<components>` element at the outer level if the namespace is wrong,
>    before it ever reads any attributes.
> 2. **`uri` must be the bare jar name (`smartart.jar`)** — *not*
>    `jar:*smartart.jar!/...Class`. The `jar:*...!/...class` form is not valid
>    in this schema.
>
> And a third, separate trap at *load* time (not install time): the **`uri` is
> resolved relative to the descriptor's own folder**, so the jar must be
> packaged in `uno/` *beside* `SmartArtImpl.xml` (i.e. `uno/smartart.jar`). If it
> is at the OXT root instead, the component fails with
> `java.io.FileNotFoundException: …/uno/smartart.jar`, `queryDispatch` returns
> null, and the menu item is silently hidden (empty submenu).

### 3.4 New: Menu Configuration (Addons.xcu)

Location: `src/main/resources/Addons.xcu`

**Purpose:** Defines the menu item in the LibreOffice UI

**Content:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<oor:component-data xmlns:oor="http://openoffice.org/2001/registry"
                    xmlns:xs="http://www.w3.org/2001/XMLSchema"
                    oor:name="Addons" oor:package="org.openoffice.Office">
    <node oor:name="AddonUI">
        <node oor:name="OfficeMenuBar">
            <node oor:name="org.libreimpress.smartart" oor:op="replace">
                <prop oor:name="Title" oor:type="xs:string">
                    <value/>
                    <value xml:lang="en-US">SmartArt</value>
                </prop>
                <prop oor:name="Target" oor:type="xs:string">
                    <value>_self</value>
                </prop>
                <node oor:name="Submenu">
                    <node oor:name="m1" oor:op="replace">
                        <prop oor:name="URL" oor:type="xs:string">
                            <value>org.libreimpress.smartart:CreateDiagram</value>
                        </prop>
                        <prop oor:name="Title" oor:type="xs:string">
                            <value/>
                            <value xml:lang="en-US">Create Diagram…</value>
                        </prop>
                        <prop oor:name="Target" oor:type="xs:string">
                            <value>_self</value>
                        </prop>
                        <prop oor:name="Context" oor:type="xs:string">
                            <value>com.sun.star.presentation.PresentationDocument</value>
                        </prop>
                    </node>
                </node>
            </node>
        </node>
    </node>
</oor:component-data>
```

This tells LibreOffice:
- Add-on UI **must** merge into `org.openoffice.Office.Addons / AddonUI` with a
  root element of `oor:component-data` (not `oor:items` / a `Common/Menus` path)
- `OfficeMenuBar` gives a top-level menu; use `AddonMenu` for **Tools → Add-Ons**
- Every `prop` must declare `oor:type="xs:string"` and use `oor:op="replace"`
- **Each `Title` needs a bare `<value/>` default before the
  `<value xml:lang="en-US">` entry.** Omit it and LibreOffice cannot resolve the
  title for the running locale and **silently drops the item** — the top-level
  **SmartArt** menu appears but its submenu is empty.
- The command URL `org.libreimpress.smartart:CreateDiagram` is dispatched to
  SmartArtCommand via the protocol bound in `ProtocolHandler.xcu` (§3.5)
- `Context` limits the entry to Impress (`PresentationDocument`)

### 3.5 New: Protocol Handler binding (ProtocolHandler.xcu)

Location: `src/main/resources/ProtocolHandler.xcu`

**Purpose:** Binds the `org.libreimpress.smartart:` command-URL protocol to the
Java handler so that clicking the menu item reaches `SmartArtCommand`. The
`HandlerSet` node name **must equal** the implementation name in
`uno/SmartArtImpl.xml`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<oor:component-data xmlns:oor="http://openoffice.org/2001/registry"
                    xmlns:xs="http://www.w3.org/2001/XMLSchema"
                    oor:name="ProtocolHandler" oor:package="org.openoffice.Office">
    <node oor:name="HandlerSet">
        <node oor:name="org.libreimpress.smartart.SmartArtCommand" oor:op="replace">
            <prop oor:name="Protocols" oor:type="oor:string-list">
                <value>org.libreimpress.smartart:*</value>
            </prop>
        </node>
    </node>
</oor:component-data>
```

### 3.6 Java side: static factory methods + JAR manifest

The `Java2` loader instantiates the component by calling **static** methods on
the implementation class. Without them the component will not load even after
the descriptor parses:

```java
public static XSingleComponentFactory __getComponentFactory(String sImplementationName) {
    if (sImplementationName.equals(IMPLEMENTATION_NAME)) {
        return Factory.createComponentFactory(SmartArtCommand.class,
                new String[]{SERVICE_NAME});
    }
    return null;
}

public static boolean __writeRegistryServiceInfo(XRegistryKey xRegistryKey) {
    return Factory.writeRegistryServiceInfo(IMPLEMENTATION_NAME,
            new String[]{SERVICE_NAME}, xRegistryKey);
}
```

The JAR's `META-INF/MANIFEST.MF` must also declare:

```
RegistrationClassName: org.libreimpress.smartart.SmartArtCommand
```

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
│   ├── manifest.xml              # registers the .xcu files + the component descriptor
│   └── MANIFEST.MF               # JAR manifest (RegistrationClassName)
├── description.xml               # Extension metadata
├── Addons.xcu                    # Menu configuration
├── ProtocolHandler.xcu           # command-URL → handler binding
└── uno/
    ├── SmartArtImpl.xml           # Component descriptor (uri="smartart.jar")
    └── smartart.jar               # Compiled Java code — MUST sit beside the descriptor
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

### Issue: Top-level SmartArt menu appears but its submenu is empty
LibreOffice **hides** an addon menu item whose command URL cannot be dispatched,
so an empty submenu usually means the dispatch is failing. Check, in order:
- **Jar location (most common).** The component must actually load. The
  descriptor's `uri="smartart.jar"` resolves *relative to the descriptor*, so the
  jar must be packaged at `uno/smartart.jar`. If it is at the OXT root, loading
  fails with `java.io.FileNotFoundException: …/uno/smartart.jar` and the item is
  hidden. Verify the dispatch resolves (see the probe below).
- Each `Title` prop in `Addons.xcu` must have a bare `<value/>` default *before*
  the `<value xml:lang="en-US">…</value>`; otherwise the title cannot be resolved
  for the running locale and the item is dropped.
- Confirm the submenu item's `Context` matches the module you are testing in
  (`com.sun.star.presentation.PresentationDocument` for Impress).

**Diagnose without the GUI** — install into an isolated profile, start a headless
listening instance, and ask the frame whether the command dispatches:
```python
disp = frame.queryDispatch(url, "_self", 0)   # url.Complete = "org.libreimpress.smartart:CreateDiagram"
# None  -> component/dispatch broken (jar location, registration) -> menu item hidden
# object-> dispatch OK; the item will appear
```

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
