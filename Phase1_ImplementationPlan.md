# Phase 1: Empty OXT Extension - Implementation Plan

## Objective
Create a minimal but valid LibreOffice UNO extension (.oxt file) that can be installed and recognized by LibreOffice Impress. This is the foundation upon which all subsequent phases build.

---

## 1. What is an OXT File?
An `.oxt` file is a ZIP archive containing:
- LibreOffice extension metadata and configuration files
- Optional: Java code, dialogs, icons, etc.

For Phase 1, we're creating the absolute minimum.

---

## 2. Phase 1 Deliverables

### Required Files (Minimal)

```
target/
└── SmartArt.oxt (ZIP archive containing:)
    ├── META-INF/
    │   └── manifest.xml          # LibreOffice extension manifest
    └── description.xml           # Extension metadata (name, version, etc.)
```

### Source Files Needed

```
src/main/resources/
├── META-INF/
│   └── manifest.xml
└── description.xml

pom.xml                           # Maven build configuration (already exists)
```

---

## 3. File Specifications

### 3.1 META-INF/manifest.xml
This file tells LibreOffice:
- What this extension contains
- How to load components
- What services are provided

**Minimal content for Phase 1:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<manifest xmlns="urn:sun:star:package:manifest">
    <entry full-path="/" media-type="application/vnd.sun.star.package:package"/>
</manifest>
```

This is the absolute minimum—just declares the OXT as a valid extension package.

### 3.2 description.xml
This file contains extension metadata displayed in LibreOffice's Extension Manager.

**Minimum required fields:**
- `identifier` - Unique ID (e.g., `org.libreimpress.smartart`)
- `version` - Version number (e.g., `0.1.0`)
- `display-name` - User-friendly name
- `summary` - Short description
- `description` - Longer description
- `license-text` - License information
- `publisher` - Author/organization

---

## 4. Maven Configuration

The existing `pom.xml` is already configured to:
1. Compile Java code (if present)
2. Package resources into a JAR
3. Create the .oxt file as a ZIP archive

**Key plugin:** `maven-assembly-plugin` with `oxt.xml` descriptor

### 4.1 Assembly Descriptor (src/main/assembly/oxt.xml)

This file tells Maven how to structure the OXT archive. Minimal version:
```xml
<assembly xmlns="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.3"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.3
                              http://maven.apache.org/xsd/assembly-1.1.3.xsd">
    <id>oxt</id>
    <formats>
        <format>zip</format>
    </formats>
    <includeBaseDirectory>false</includeBaseDirectory>
    <fileSets>
        <fileSet>
            <directory>${project.basedir}/src/main/resources</directory>
            <outputDirectory>/</outputDirectory>
            <includes>
                <include>META-INF/**</include>
                <include>description.xml</include>
            </includes>
        </fileSet>
    </fileSets>
</assembly>
```

---

## 5. Build Instructions

### Prerequisites
- Java 11+ installed
- Maven 3.6.0+ installed

### Build Steps

```bash
# Step 1: Navigate to project directory
cd /path/to/LibreImpress-SmartArt

# Step 2: Clean and package
mvn clean package

# Expected output:
# - target/smartart-0.1.0-SNAPSHOT.jar
# - target/SmartArt.oxt
```

### Verifying the OXT

```bash
# Unzip and inspect the contents
unzip -l target/SmartArt.oxt

# Expected structure:
# Archive:  target/SmartArt.oxt
#   Length     Date   Time    Name
# --------  ---------- -----   ----
#      XXX  YYYY-MM-DD HH:MM   META-INF/manifest.xml
#      XXX  YYYY-MM-DD HH:MM   description.xml
```

---

## 6. Installation & Testing (Optional)

Once Phase 1 is complete, the OXT can be tested:

1. **Copy OXT to LibreOffice extensions folder:**
   ```bash
   cp target/SmartArt.oxt ~/.config/libreoffice/4/user/extensions/
   # (Path varies by OS and LibreOffice version)
   ```

2. **Or use LibreOffice GUI:**
   - Tools → Extensions → Add → Select SmartArt.oxt
   - Restart LibreOffice
   - Tools → Extensions Manager → Verify "LibreImpress SmartArt" appears

3. **Verify in Extension Manager:**
   - LibreOffice will display the extension name and description
   - (No functionality yet—just validates the OXT structure)

---

## 7. Phase 1 Deliverables Checklist

- [ ] `src/main/resources/META-INF/manifest.xml` created
- [ ] `src/main/resources/description.xml` created
- [ ] `src/main/resources/assembly/oxt.xml` created (assembly descriptor)
- [ ] `mvn clean package` builds successfully
- [ ] `target/SmartArt.oxt` exists and contains correct files
- [ ] OXT can be unzipped and inspected
- [ ] README updated with Phase 1 completion

---

## 8. What's NOT in Phase 1

- ❌ Java code (no SmartArtCommand, SmartArtDialog, etc.)
- ❌ UNO component registration
- ❌ Menu or toolbar integration
- ❌ Dialog definitions
- ❌ Any functionality

Phase 1 is **purely structural** — proving we can build a valid OXT.

---

## 9. What's Next: Phase 2

Phase 2 will add:
- UNO component registration (manifest.xml with component entry)
- SmartArtCommand.java (UNO service entry point)
- Menu item in Insert menu
- LibreOffice integration

See `Phase2_ImplementationPlan.md`

---

## 10. Key Decisions for Phase 1

| Decision | Choice | Rationale |
|----------|--------|-----------|
| OXT vs other packaging | OXT (ZIP) | Standard LibreOffice extension format |
| Metadata format | XML | LibreOffice standard |
| Java code in Phase 1 | No | Separate functionality for Phase 2 |
| Testing approach | Manual unzip + Extension Manager | Validates structure without requiring LibreOffice SDK |

---

## 11. Common Issues & Solutions

### Issue: OXT won't install
**Solution:** Verify manifest.xml and description.xml are in correct locations and have valid XML syntax
```bash
unzip -t target/SmartArt.oxt  # Test zip integrity
```

### Issue: Extension doesn't appear in Extension Manager
**Solution:** 
- Check description.xml has all required fields
- Verify identifier is unique (use org.libreimpress.smartart)
- Restart LibreOffice after installation

### Issue: `mvn package` fails
**Solution:**
- Ensure Maven can find assembly descriptor: `src/main/assembly/oxt.xml`
- Check pom.xml assembly plugin configuration

---

## Success Criteria

✅ `mvn clean package` completes without errors  
✅ `target/SmartArt.oxt` file created (size > 0 bytes)  
✅ `unzip -l target/SmartArt.oxt` shows expected files  
✅ Manual test: OXT can be installed in LibreOffice Extension Manager  
✅ README.md updated documenting Phase 1

---

**Status:** Ready for implementation  
**Estimated Time:** 30 minutes  
**Next Phase:** Phase 2 - Menu and Toolbar Integration
