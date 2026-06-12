# Phase 1: Maven Project Setup - Implementation Plan

## Objective
Create a Maven project structure that compiles to a LibreOffice UNO extension (.oxt file) with proper dependencies, build configuration, and project layout.

---

## 1. Maven Project Structure

```
LibreImpress-SmartArt/
├── pom.xml                          # Main Maven configuration
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/
│   │   │       └── libreimpress/
│   │   │           └── smartart/
│   │   │               ├── SmartArtCommand.java
│   │   │               ├── SmartArtDialog.java
│   │   │               └── helpers/
│   │   │                   └── LibreOfficeHelper.java
│   │   ├── resources/
│   │   │   ├── dialogs/
│   │   │   │   └── SmartArtDialog.xml
│   │   │   ├── META-INF/
│   │   │   │   └── manifest.xml        # UNO extension manifest
│   │   │   └── images/
│   │   │       └── extension.png       # Extension icon
│   │   └── uno/
│   │       └── SmartArtImpl.xml         # UNO component description
│   └── test/
│       └── java/
│           └── org/libreimpress/smartart/
│               └── [Unit tests]
├── target/
│   ├── classes/
│   ├── SmartArt.oxt                    # Final deliverable
│   └── [build artifacts]
├── README.md
└── .gitignore
```

---

## 2. Key Dependencies & Versions

### LibreOffice UNO SDK
- **Purpose:** Core API for LibreOffice extensions
- **Versions available:** 7.0+, 7.4 (stable), 8.0+
- **Recommended:** 7.4 (widely compatible)

### Maven Plugins Required
- `maven-compiler-plugin` - Java compilation
- `maven-assembly-plugin` - Package .oxt extension
- `maven-jar-plugin` - Create JAR
- `maven-shade-plugin` - Optional: Bundle dependencies

### Testing Dependencies
- JUnit 4/5
- Mockito (for mocking UNO objects)

---

## 3. pom.xml Configuration

### Key Sections:
1. **Project metadata** - GroupId, ArtifactId, Version
2. **Properties** - Java version (11+), UNO version, encoding (UTF-8)
3. **Dependencies** - UNO SDK, testing libraries
4. **Build plugins** - Compiler, JAR, assembly, UNO manifest generation
5. **Repositories** - LibreOffice artifact repositories (if needed)

### Critical Configuration:
- Target Java version: 11 (LTS, supported by LibreOffice 7.4+)
- Source encoding: UTF-8
- UNO SDK location detection
- .oxt packaging as custom assembly

---

## 4. UNO Extension Manifest

### manifest.xml (META-INF/)
Must contain:
- Extension metadata (name, version, author)
- UNO component registration
- Entry point class reference
- Icon and description

### uno/SmartArtImpl.xml
Declares the UNO component:
- Interface implementation
- Service names
- Component class path

### description.xml

- Must adhere to Attributes of `<description>` https://wiki.documentfoundation.org/Documentation/DevGuide/Extensions#Extension_Manager

---

## 5. Build Output Target

### Final Deliverable: SmartArt.oxt
Structure inside .oxt:
```
SmartArt.oxt (ZIP archive)
├── META-INF/
│   └── manifest.xml
├── content.xml
├── SmartArt.jar              # Compiled Java code
├── description.xml           # Extension description
├── dialogs/
│   └── SmartArtDialog.xml    # Dialog XML
├── icons/
│   └── extension.png
└── uno/
    └── SmartArtImpl.xml       # UNO component descriptor
```

---

## 6. Development Environment Setup

### Prerequisites
1. **Java Development Kit (JDK)**
   - Version: 11 or higher
   - Installation: Download from adoptopenjdk.org or eclipse.org

2. **Maven**
   - Version: 3.6.0+
   - Installation: Download from maven.apache.org

3. **LibreOffice SDK**
   - Include path for UNO headers and libraries
   - Download from libreoffice.org (development version)

4. **IDE (Optional but recommended)**
   - IntelliJ IDEA Community (Maven support built-in)
   - VS Code + Maven extension
   - Eclipse + Maven plugin

### Environment Variables (if needed)
```bash
export JAVA_HOME=/path/to/jdk-11
export M2_HOME=/path/to/maven
export PATH=$JAVA_HOME/bin:$M2_HOME/bin:$PATH
```

---

## 7. Build Commands

### Commands to support in pom.xml:

```bash
# Clean build
mvn clean

# Compile
mvn compile

# Run tests
mvn test

# Build JAR
mvn package

# Generate .oxt extension
mvn package -P build-extension

# Install locally (for testing in LibreOffice)
mvn install

# Full build with tests
mvn clean test package
```

---

## 8. Configuration Decisions to Lock In

### Decision 1: Java Version
- **Option A:** Java 8 (maximum compatibility)
- **Option B:** Java 11 (modern, LTS, recommended)
- **Decision:** **Java 11** (balances compatibility and modern practices)

### Decision 2: Build Profile Strategy
- **Option A:** Single pom.xml with all configuration
- **Option B:** Separate profiles for dev/test/release
- **Decision:** **Single pom.xml** for MVP (can add profiles later)

### Decision 3: Dependency Management
- **Option A:** Maven Central exclusively
- **Option B:** Include LibreOffice-specific repositories
- **Decision:** **Maven Central + LibreOffice repos** (may need both)

### Decision 4: UNO Component Registration
- **Option A:** XML-based (manual registration)
- **Option B:** Annotation-based (if available in UNO SDK)
- **Decision:** **XML-based** (XML manifest in META-INF/)

### Decision 5: Dialog Format
- **Option A:** LibreOffice Basic dialog (.xdl format)
- **Option B:** UNO dialog XML (.xml format)
- **Decision:** **XML-based UNO dialog** (more control, programmatic)

---

## 9. Phase 1 Deliverables

### At completion of Phase 1, you should have:

✅ **pom.xml** with all dependencies and plugins configured  
✅ **Project structure** ready for code development  
✅ **Build verification** - `mvn clean package` compiles successfully  
✅ **IDE integration** - Project opens in IDE without errors  
✅ **README** documenting build steps and setup  
✅ **.gitignore** configured for Java/Maven projects  
✅ **Skeleton classes** - Empty SmartArtCommand.java, SmartArtDialog.java  
✅ **Test framework ready** - JUnit configured, sample test passes  

---

## 10. Verification Checklist

- [ ] `mvn --version` runs successfully
- [ ] `java -version` shows JDK 11+
- [ ] Project imports in IDE without errors
- [ ] `mvn clean compile` succeeds
- [ ] `mvn test` runs (even if no tests yet)
- [ ] `mvn package` generates target/ artifacts
- [ ] Target .oxt file is created (even if incomplete)
- [ ] Project structure matches layout above

---

## 11. Iteration Notes for Spec Refinement

After completing Phase 1, before moving to Phase 2, we'll:

1. **Review** what we learned about Maven/UNO build process
2. **Refine spec** with concrete build output details
3. **Document** any build quirks or considerations
4. **Delete** Phase 1 code to start fresh with Phase 2 (if desired)
5. **Update spec** with lessons learned

---

## Next Phase: Phase 2 - Dialog UI

Once Phase 1 is complete:
- Create Dialog XML in resources/dialogs/
- Implement SmartArtDialog controller
- Test dialog opens/closes in LibreOffice
