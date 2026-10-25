# Phase 17 Implementation Plan — Edit Existing Diagram

## Goal

Select a previously generated diagram, re-open the dialog prefilled with its
source, and regenerate it in place. Diagram **type conversion** falls out for
free (change the type dropdown, press Update). Spec §8 items "Edit existing
diagram" and "Diagram type conversion".

## Persistence design

The source is stamped on the diagram's group shape:

- **`Description`** ← `SmartArtMetadata.serialize()` — serialized to
  `<svg:desc>` in ODF. Survival across ODP save/reload was proven **before**
  building the flow by the spike probe
  `uno-tests/probes/metadata_persistence_probe.py` (multi-line, unicode,
  semicolon-heavy value round-trips byte-for-byte; runs in CI).
- **`Name`** ← `SmartArt: <type label>` — user-visible friendly label only;
  never parsed.

Wire format (`models/SmartArtMetadata.java`, pure Java, unit-tested):

```
smartart:v1;type=<DiagramType.name()>;template=<id>;palette=<b64>;outline=<b64>
```

Outline and palette are Base64/UTF-8, killing all delimiter/newline escaping.
`tryParse` returns `Optional.empty()` for foreign descriptions, unknown
versions, unknown type names, or bad Base64; unknown keys are ignored
(forward compatibility). `template` is reserved for Phase 18 (always
`DEFAULT` until then; carried through edits).

## Files Changed

| File | Change |
|------|--------|
| `src/main/java/org/libreimpress/smartart/models/SmartArtMetadata.java` | New: format above |
| `src/test/java/org/libreimpress/smartart/models/SmartArtMetadataTest.java` | 8 unit tests: round-trips, unicode/semicolons, rejection, forward-compat |
| `src/main/java/org/libreimpress/smartart/rendering/SlideRenderer.java` | `drawHierarchy`/`groupShapes` return the group `XShape` (from `grouper.group(...)`); new static `stampMetadata(group, meta)` |
| `src/main/java/org/libreimpress/smartart/SmartArtCommand.java` | `findEditTarget()` reads the selection **before** the modal dialog (inside the event loop `getCurrentComponent()` can return the dialog). Exactly-one-shape selection whose `Description` parses → edit mode: dialog prefilled, on OK draw new → stamp → `setPosition` to old group's position → remove old group (old removed only after successful render). Create flow now also stamps metadata |
| `src/main/java/org/libreimpress/smartart/SmartArtDialog.java` | `show(SmartArtMetadata prefill)`: seeds outline/type/palette, title "Edit Diagram", OK button "Update" |
| `src/main/java/org/libreimpress/smartart/DemoRunner.java` | Stamps metadata on every demo group — puts the stamp path under E2E coverage |
| `uno-tests/probes/e2e_demo_probe.py` | Per-slide: asserts `Description` starts with the marker and contains the right `type=`, and `Name` starts with `SmartArt: ` |
| `uno-tests/probes/metadata_persistence_probe.py` | New spike/regression probe (see above) |
| `.github/workflows/build-and-validate.yml` | Runs the persistence probe |
| `README.md` | "Edit an existing diagram" section incl. limitations |

No new menu item: selection detection inside the existing Create Diagram
command covers edit mode with zero extra UI.

## Known limitations (documented in README)

- Single-shape diagrams are not grouped (`created.size() < 2`) → no metadata
  carrier → not editable.
- Manual edits inside the group are lost on Update (regenerates from stored
  outline).
- A child selected *inside* an entered group has no metadata → create flow
  (correct behaviour).

## Verified

- `mvn package` green (unit tests incl. new `SmartArtMetadataTest`).
- `metadata_persistence_probe.py` passes: `Description`+`Name` survive ODP
  save/close/reload byte-for-byte.
- `e2e_demo_probe.py` passes: all 12 demo groups carry parseable metadata
  with the correct type through the real Java pipeline.
- Manual GUI check (user): create → save → reopen → select → dialog
  prefilled → change type → Update replaces in place.
