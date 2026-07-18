# Phase 16 Implementation Plan — True End-to-End CI Test

## Goal

Close the long-standing coverage gap: nothing in CI drove the **real** Java
render pipeline (`HierarchyParser` → `LayoutFactory` → `SlideRenderer`).
`render_probe.py` only mirrors the renderer's UNO call sequence from Python,
and the dialog path can't run headless (modal dialog).

Route: `DemoRunner` already renders every diagram type through the real
pipeline and is reachable headlessly by dispatching
`org.libreimpress.smartart:Demo`. Phase 16 makes that dispatch CI-drivable
via an `OutputDir` argument and adds a probe that asserts on the resulting
slides, PNGs, and a per-type result file.

## Files Changed

| File | Change |
|------|--------|
| `src/main/java/org/libreimpress/smartart/SmartArtCommand.java` | Demo branch reads optional `OutputDir` string from the dispatch `PropertyValue[]` (via `AnyConverter`) and passes it to `DemoRunner.run(outputDir)` |
| `src/main/java/org/libreimpress/smartart/DemoRunner.java` | `run(String outputDir)` overload. Headless mode (non-null): PNGs go to that dir (created if missing), per-type `OK <slug>` / `FAIL <slug>: <err>` lines are collected and written to `demo-result.txt`, no message boxes, export errors are fatal per type. Interactive mode unchanged (system-property dir, best-effort export) |
| `uno-tests/probes/e2e_demo_probe.py` | New probe (see below) |
| `.github/workflows/build-and-validate.yml` | New step running the probe via `run.sh --install`, plus a `git status --porcelain` guard proving the probe never writes into the checkout |
| `TESTING_STRATEGY.md` | Documents the new layer 3b |

## Probe design (`e2e_demo_probe.py`)

1. Load `private:factory/simpress`, record page count.
2. `queryDispatch` the Demo URL, dispatch with `OutputDir` = a temp dir under
   `target/` (probe cleans it up in `finally`).
3. Assert, per diagram type (expectation table mirrors `DemoRunner.DEMOS`,
   order matters):
   - `demo-result.txt` has an `OK <slug>` line for all 12 types;
   - one slide per type appended; each demo slide holds exactly 3 shapes —
     one `GroupShape` (the diagram) + the two dev-chrome text shapes;
   - group child count ≥ a conservative per-type minimum, and all expected
     node labels appear in the group's (recursive) shape texts;
   - `<slug>.png` exists, has a valid PNG IHDR of 1280×960 (parsed by hand,
     no PIL), and is > 5 KB (non-blank).

Dispatch over URP is synchronous, but the probe still polls for
`demo-result.txt` with a generous timeout as a belt-and-braces measure.

## Verified

- `mvn package` green (unit tests + OXT build).
- `uno-tests/run.sh --install target/SmartArt.oxt uno-tests/probes/e2e_demo_probe.py`
  passes locally: 12/12 OK lines, 12 grouped slides with expected texts
  (e.g. hierarchy group = 13 shapes: 7 nodes + 6 connectors), 12 valid PNGs.
- The probe run leaves `git status --porcelain` clean (no writes to
  `docs/screenshots/`).
- Negative check during development: an over-strict expectation (block list
  min-shapes 7 vs actual 6 blocks) failed the probe with a precise per-slug
  message, confirming failures are attributable.

## Notes / follow-ups

- A finer-grained `RenderOne` dispatch (type + outline as args) was considered
  and deferred: per-slide assertions on the Demo output already attribute
  failures to a single diagram type.
- Phase 17 will extend this probe to assert each demo group carries parseable
  SmartArt metadata in its `Description`.
