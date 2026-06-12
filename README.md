# LibreImpress SmartArt - Phase 1 Build Setup

Welcome to Phase 1! This document covers building and verifying the Maven project structure.

## Overview

LibreImpress SmartArt is a LibreOffice Impress UNO extension that generates professional diagrams from hierarchical text input.

**Current Phase:** 2 - Dialog UI Implementation  
**Status:** Dialog implementation complete with input validation, ready for Phase 3 (Parser)

## Quick Start

### Prerequisites

1. **Java Development Kit (JDK 11+)**
   ```bash
   java -version
   # Should show: openjdk version "11" or higher
   ```

2. **Maven 3.6.0+**
   ```bash
   mvn --version
   # Should show: Apache Maven 3.6.0 or higher
   ```

3. **LibreOffice (7.4+)** - Optional for testing
   - Required only when testing in actual LibreOffice
   - Can be installed via system package manager

### Build Commands

```bash
# Compile the project
mvn clean compile

# Run all tests
mvn test

# Package as JAR (with dependencies bundled)
mvn clean package

# Full build with all checks
mvn clean test package

# Build release profile (includes .oxt extension)
mvn clean package -P release
```

## Project Structure

```
LibreImpress-SmartArt/
├── pom.xml                              # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/org/libreimpress/smartart/
│   │   │   ├── SmartArtCommand.java     # Main entry point
│   │   │   ├── SmartArtDialog.java      # Dialog controller
│   │   │   ├── helpers/
│   │   │   │   └── LibreOfficeHelper.java
│   │   │   ├── models/                  # (Phase 3+)
│   │   │   ├── parsers/                 # (Phase 3+)
│   │   │   ├── generators/              # (Phase 5+)
│   │   │   └── rendering/               # (Phase 5.1+)
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── MANIFEST.MF
│   │       ├── uno/
│   │       │   └── SmartArtImpl.xml      # UNO component descriptor
│   │       ├── assembly/
│   │       │   └── oxt.xml              # .oxt packaging config
│   │       └── dialogs/                 # (Phase 2)
│   └── test/
│       └── java/org/libreimpress/smartart/
│           └── [Unit tests]
├── target/                              # Build output
│   ├── classes/                         # Compiled classes
│   ├── smartart-0.1.0-SNAPSHOT.jar     # JAR file
│   └── SmartArt.oxt                    # LibreOffice extension (release mode)
├── .gitignore
├── README.md                            # This file
└── Phase1_ImplementationPlan.md         # Detailed Phase 1 plan
```

## Maven Configuration Highlights

### Dependencies
- **UNO SDK 7.4** - LibreOffice extension API
- **GSON 2.10.1** - JSON parsing for palettes
- **JUnit 4** - Unit testing
- **Mockito 4.8** - Mocking for tests

### Plugins
- **Maven Compiler** - Java 11 compilation with UTF-8 encoding
- **Maven Jar** - Package compiled code
- **Maven Shade** - Bundle dependencies into single JAR
- **Maven Assembly** - Package .oxt extension (ZIP format)
- **Maven Surefire** - Run unit tests

### Build Profiles
- **dev** (default) - Normal development build with tests
- **release** - Full build with .oxt extension packaging

## Testing Strategy

**Local Tests (Maven):** Fast unit tests that don't depend on the compiled artifact
- `SmartArtCommandTest.java` - Tests core classes
- Run with `mvn test`

**Artifact Validation Tests (GitHub Actions):** Validates the `.oxt` extension package structure after build
- `ExtensionValidationTest.java` - Validates zip structure, manifests, required files
- Runs automatically on push/PR via `.github/workflows/build-and-validate.yml`
- Excluded from local `mvn test` to avoid circular dependencies with `mvn clean`

## Verification Checklist

After cloning/setting up, verify everything works:

```bash
# 1. Check prerequisites
java -version
mvn --version

# 2. Compile
mvn clean compile
# Expected: BUILD SUCCESS

# 3. Run unit tests
mvn test
# Expected: Unit tests run and pass

# 4. Package
mvn package
# Expected: target/smartart-0.1.0-SNAPSHOT.jar and target/SmartArt.oxt created

# 5. Full build with tests
mvn clean test package
# Expected: All unit tests pass and artifact is created
```

**Note:** Artifact validation tests run in GitHub Actions on each push/PR. See `.github/workflows/build-and-validate.yml`

## IDE Setup

### IntelliJ IDEA
1. File → Open → Select project root
2. IDEA auto-detects Maven project
3. Right-click pom.xml → "Add as Maven Project"
4. Maven panel appears on right side
5. Verify project compiles: Run → Run Tests or Ctrl+Shift+F10

### VS Code
1. Install "Maven for Java" extension (vscjava.maven-for-java)
2. File → Open Folder → Select project root
3. VS Code detects pom.xml automatically
4. Maven view appears in Explorer
5. Click "build workspace" to compile

### Eclipse
1. File → Import → Existing Maven Projects
2. Select project root
3. Finish
4. Right-click project → Run As → Maven clean
5. Then right-click → Run As → Maven build...

## Phase 1 Deliverables ✅

This Phase 1 setup provides:

- ✅ Maven project structure (ready for Java development)
- ✅ UNO SDK dependencies configured
- ✅ Build pipeline (compile → test → package → .oxt)
- ✅ Skeleton classes (SmartArtCommand, SmartArtDialog, LibreOfficeHelper)
- ✅ IDE-ready (IntelliJ, VS Code, Eclipse compatible)
- ✅ .oxt packaging configured (LibreOffice extension format)
- ✅ Test framework ready (JUnit + Mockito)

## Phase 2: Dialog UI ✅ COMPLETE

Phase 2 is now complete! The dialog implementation includes:

**Delivered:**
- SmartArtDialog controller with programmatic UNO dialog creation
- Input validation: empty text check, indentation consistency (spaces vs tabs)
- SmartArtCommand.execute() opens dialog and handles user interaction
- 12 unit tests (4 original + 8 new validation tests)
- Full build pipeline working: `mvn clean test package` succeeds

**Test Results:**
- 12 unit tests pass
- SmartArt.oxt created successfully (1.9MB with UNO SDK bundled)

**What's Next → Phase 3: Parser**
- Parse hierarchy text into internal data model
- Validate nesting depth and structure
- Create diagram objects from parsed hierarchy

See documentation:
- `Phase1_ImplementationPlan.md` - Phase 1 project setup
- `Phase2_ImplementationPlan.md` - Phase 2 dialog implementation
- `Phase3_ImplementationPlan.md` - Phase 3 parser (coming soon)

## Troubleshooting

### Issue: `mvn: command not found`
**Solution:** Maven not installed or not in PATH
```bash
export PATH=$PATH:/path/to/maven/bin
mvn --version  # Should work now
```

### Issue: `[ERROR] COMPILATION ERROR`
**Solution:** Check Java version (need 11+)
```bash
java -version   # Should show 11 or higher
```

### Issue: UNO SDK dependencies not found
**Solution:** Ensure internet connection for Maven Central
```bash
mvn dependency:tree  # Shows all dependencies and versions
```

### Issue: Build hangs or is very slow
**Solution:** First build is slow (downloads dependencies). Try:
```bash
mvn clean compile -T 1C  # Use single thread for troubleshooting
```

## Support & Documentation

- **UNO API Docs:** https://api.libreoffice.org/
- **Maven Docs:** https://maven.apache.org/
- **Project Architecture:** See `Architecture_VDiagram.md`
- **Specification:** See `impressSmartArt.md`

---

**Version:** 0.1.0-SNAPSHOT  
**Last Updated:** 2026-06-11  
**Maintainer:** LibreImpress SmartArt Team
