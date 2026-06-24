# Testing Strategy for LibreImpress SmartArt

Testing runs in **three automated layers** plus a small residue of genuinely
manual checks. Every layer runs in CI (`.github/workflows/build-and-validate.yml`)
on each push; layers 1–3 also run locally.

| Layer | What it proves | Needs LibreOffice? | Where |
|-------|----------------|--------------------|-------|
| 1. Java unit tests | Pure-Java logic (the parser) is correct | No | `mvn package` |
| 2. OXT structure validation | The `.oxt` contains the right files in the right places | No | CI shell step |
| 3. Runtime checks (registration/dispatch + drawing API) | LibreOffice loads the component, the command dispatches, and the drawing API behaves as assumed | Yes (headless) | `uno-tests/run.sh` |
| Manual | The dialog *looks* right on screen | Yes (GUI) | human |

The key change from earlier phases: **layer 3 is now automated.** A headless
LibreOffice plus python-UNO can assert that the extension registers *and* that the
menu command resolves to a dispatch — the exact failure that a wrong component
namespace, a bad identifier, or a mis-placed jar produces. Only the visual
appearance of the dialog still needs a human.

---

## Layer 1 — Java unit tests (fast, no LibreOffice)

**Location:** `src/test/java/.../parsers/HierarchyParserTest.java`

Tests the UNO-free core — the hierarchy parser and its validation (indentation
style, first-line rule, aligned widths, ≥3 nodes, ≥3 levels) and the outline
renderer. Pure Java, so it runs in plain Maven.

```bash
mvn clean package        # runs these during the test phase
```

As later phases add UNO-free logic (palette parsing, layout maths), put it behind
a plain class and unit-test it here.

---

## Layer 2 — OXT structure validation (no LibreOffice)

A shell step in CI asserts the built `.oxt` contains every required file and that
the fragile strings are correct, without launching LibreOffice:

- required entries present: `META-INF/manifest.xml`, `description.xml`,
  `Addons.xcu`, `ProtocolHandler.xcu`, `uno/SmartArtImpl.xml`, `uno/smartart.jar`;
- `uno/SmartArtImpl.xml` uses the `http://openoffice.org/2010/uno-components`
  namespace;
- `description.xml` declares `<identifier value="org.libreimpress.smartart"/>`.

These are cheap regression guards for the registration contract
(`docs/plans/Phase2_ImplementationPlan.md` §15), but they **cannot** prove the extension
actually works — that is layer 3.

---

## Layer 3 — Runtime checks against a headless LibreOffice

**Scripts:** `uno-tests/run.sh` (orchestrator) + the probes in `uno-tests/probes/`.
See [`uno-tests/README.md`](uno-tests/README.md). `run.sh` starts a throwaway,
headless LibreOffice (profile under `target/`, never `/tmp`), optionally installs
the `.oxt`, runs the given probe over a UNO socket, and tears everything down.

There are two probes:

**`registration_probe.py`** (installs the `.oxt`) asserts:
1. the SmartArt menu item is present in LibreOffice's **merged** Addons config; and
2. `frame.queryDispatch("org.libreimpress.smartart:CreateDiagram", …)` returns a
   real dispatch in an Impress frame.

Check 2 is the decisive one: LibreOffice **silently hides** an addon menu item
whose command cannot be dispatched, so a null dispatch means an empty submenu.
This catches the registration/dispatch regressions that layers 1–2 miss — e.g.
the component jar packaged at the OXT root instead of `uno/smartart.jar`, which
leaves the config perfect but the dispatch null (see `docs/plans/Phase2_ImplementationPlan.md`
§15).

**`render_probe.py`** (no extension needed) exercises the drawing API the renderer
relies on against a fresh Impress doc — create `RectangleShape`s, glue
`ConnectorShape`s, group via the global `ShapeCollection` — guarding against a
LibreOffice version where any of those behaves differently (each was a real
surprise during development). It is an API-contract smoke test; it does not invoke
the Java renderer (that needs the modal dialog, which can't run headless).

```bash
# requires libreoffice (unopkg, soffice) + python3-uno on PATH
mvn clean package
uno-tests/run.sh --install target/SmartArt.oxt uno-tests/probes/registration_probe.py
uno-tests/run.sh uno-tests/probes/render_probe.py
#   … PASS lines …
#   UNO TEST PASS: <probe>
```

Each run exits non-zero on failure, so they gate CI. The registration probe was
validated both ways: it passes on a correct build and **fails** on a deliberately
broken one (jar moved to the OXT root → `queryDispatch` returns `None`).

---

## What still must be manual

Only the **visual appearance and interaction** of the dialog:

- dialog layout / control sizing on screen;
- that Create echoes the parsed tree and Cancel does nothing;
- end-to-end shape rendering on a slide (Phase 4+).

Everything up to "the command dispatches to our handler" is automated; what the
human still confirms is what happens *after* the handler runs and draws on screen.

Manual smoke test:
```bash
unopkg remove org.libreimpress.smartart            # ignore "not found" the first time
unopkg add --suppress-license target/SmartArt.oxt  # close all LibreOffice windows first
# Impress → SmartArt → Create Diagram… → type a 3-level outline → Create
```

---

## CI summary

`.github/workflows/build-and-validate.yml` on every push:

1. `mvn clean package` — layer 1 + build the `.oxt`.
2. Validate OXT structure — layer 2.
3. Install LibreOffice + `python3-uno`, then run both `uno-tests/` probes under
   `xvfb-run` — layer 3.
4. Upload the `.oxt` artifact.
