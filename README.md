# LibreImpress SmartArt

A LibreOffice Impress UNO extension that generates structured diagrams
(hierarchy, hub-and-spoke, process flow) from hierarchical text input.

**Current status**
- ✅ **Phase 1 — Empty OXT extension:** a minimal, installable `.oxt` skeleton.
- ✅ **Phase 2 — Menu integration:** a top-level **SmartArt** menu whose
  *Create Diagram…* item dispatches to the Java handler (`SmartArtCommand`).
- ✅ **Phase 3 — Dialog & text parsing:** *Create Diagram…* opens an input
  dialog (text + diagram-type dropdown); on Create the indented text is parsed
  into a validated hierarchy and the parsed tree (or a clear error) is shown.
- ⏳ **Phase 4 — Shape rendering:** draw the parsed tree as grouped, editable
  shapes on the slide. Not yet started.

See [`impressSmartArt.md`](impressSmartArt.md) for the master specification and
the full document hierarchy.

## Prerequisites

- **JDK 11+** — `java -version`
- **Maven 3.6+** — `mvn --version`
- **LibreOffice 7.4+** — only needed to install/test the extension; provides the
  `unopkg` tool used below.

## Build

```bash
mvn clean package
```

Produces **`target/SmartArt.oxt`** (the installable extension) and
`target/smartart.jar` (the compiled component).

## Install & verify

The reliable way to confirm the extension registers is to install it with
`unopkg`. Use an **isolated user profile** so testing never disturbs your real
LibreOffice profile:

```bash
PROFILE=file:///tmp/lo-test

# install
unopkg add    --suppress-license -env:UserInstallation=$PROFILE target/SmartArt.oxt

# verify — expect "Identifier: org.libreimpress.smartart"
unopkg list   -env:UserInstallation=$PROFILE

# uninstall
unopkg remove -env:UserInstallation=$PROFILE org.libreimpress.smartart
```

To install into your **real** LibreOffice instead, drop the `-env:` argument
(close all LibreOffice windows and the quickstarter first):

```bash
unopkg add --suppress-license target/SmartArt.oxt
```

Then open Impress — a **SmartArt** menu appears in the menu bar with
*Create Diagram…*. (Clicking it currently prints `SmartArt command executed!`
to the terminal where `soffice` was launched; the dialog arrives in Phase 3.)

## Project structure

```
LibreImpress-SmartArt/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/org/libreimpress/smartart/
│   │   │   ├── SmartArtCommand.java        # UNO ProtocolHandler component + dispatch
│   │   │   ├── SmartArtDialog.java         # programmatic UNO input dialog
│   │   │   ├── models/                     # DiagramNode, DiagramType
│   │   │   ├── parsers/                     # HierarchyParser, ParseResult (pure Java)
│   │   │   └── helpers/                     # LibreOfficeHelper (message boxes)
│   │   ├── assembly/
│   │   │   └── oxt.xml                      # assembles the .oxt
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   ├── manifest.xml            # OXT package manifest
│   │       │   └── MANIFEST.MF             # JAR manifest (RegistrationClassName)
│   │       ├── description.xml             # extension metadata (identifier/version)
│   │       ├── Addons.xcu                  # menu definition
│   │       ├── ProtocolHandler.xcu         # command-URL → handler binding
│   │       └── uno/
│   │           └── SmartArtImpl.xml         # UNO component descriptor
│   └── test/java/org/libreimpress/smartart/
│       └── parsers/HierarchyParserTest.java # parser unit tests (run in mvn package)
└── target/
    ├── smartart.jar
    └── SmartArt.oxt
```

## Continuous integration

`.github/workflows/build-and-validate.yml` runs on every push: it builds the
`.oxt`, validates its structure, installs LibreOffice, and then performs
`unopkg add` / `list` / `remove` under `xvfb`. A registration regression
(wrong component namespace, bad identifier, missing file) fails CI rather than
only surfacing during a manual install.

## Documentation

| Document | Purpose |
|----------|---------|
| [`impressSmartArt.md`](impressSmartArt.md) | Master specification + packaging/registration rules |
| [`Phase1_ImplementationPlan.md`](Phase1_ImplementationPlan.md) | Phase 1 — empty OXT extension |
| [`Phase2_ImplementationPlan.md`](Phase2_ImplementationPlan.md) | Phase 2 — menu integration |
| [`Phase3_ImplementationPlan.md`](Phase3_ImplementationPlan.md) | Phase 3 — dialog & text parsing |
| [`Architecture_VDiagram.md`](Architecture_VDiagram.md) | Architecture & V-model process |
| [`TESTING_STRATEGY.md`](TESTING_STRATEGY.md) | Testing approach |

---

**Version:** 0.1.0-SNAPSHOT
