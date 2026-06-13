# Phase 3: Dialog & Text Parsing - Implementation Plan

## Objective
Make the menu do something visible: clicking **SmartArt → Create Diagram…**
opens an input dialog (multiline text + diagram-type dropdown + Create/Cancel).
On **Create**, the indented text is parsed into a validated hierarchy and the
result (the parsed tree, or a clear error) is shown to the user. **No shapes are
drawn yet** — that is Phase 4.

This phase adds only Java code to `smartart.jar`. It introduces **no new files
in the `.oxt`** and changes **none** of the names in the registration contract
(`Phase2_ImplementationPlan.md` §15) — the dialog is built programmatically, not
from an `.xdl`.

---

## 1. What's New in Phase 3
1. **Model** (`models/DiagramNode`, `models/DiagramType`) — an in-memory tree.
2. **Parser** (`parsers/HierarchyParser`, `parsers/ParseResult`) — pure Java, no
   UNO; turns indented text into a `DiagramNode` tree with validation.
3. **Dialog** (`SmartArtDialog`) — a programmatic `UnoControlDialog` collecting
   the text and diagram type, with a **list-enforcing outline editor** (§3.8).
4. **Outline editing** (`editing/OutlineEditor`) — pure-Java Tab/Shift+Tab/Enter
   text transforms that keep the input a list.
5. **Wiring** — `SmartArtCommand.execute()` now shows the dialog, parses the
   input, and displays the parsed tree or the validation error in a message box
   (via `helpers/LibreOfficeHelper`).
6. **Unit tests** (`HierarchyParserTest`, `OutlineEditorTest`) — run during
   `mvn package`/CI.

---

## 2. Indentation & validation model (the parser contract)
Indentation expresses nesting. The rules are deliberately simple and
predictable (standard outline semantics):

- The document must use **one** indentation character throughout — all tabs or
  all spaces. Mixing them is an error.
- The **first line must not be indented** (it is a level-1 item).
- A line **more indented** than the line above opens a new level exactly one
  deeper (its indent becomes that level's canonical width).
- A line **less or equally** indented returns to an existing level whose
  canonical indent **matches exactly**; a width that matches no level is an
  "inconsistent indentation" error.

**Acceptance validations** (map to spec §7 Error Handling and §6.1 Constraints):
| Rule | Message shown |
|------|---------------|
| Non-empty input | "Please enter at least one line of text." |
| First line not indented | "The first line must not be indented (line N)." |
| Single indent style | "Inconsistent indentation: mix of tabs and spaces (line N)." |
| Aligned indentation | "Inconsistent indentation (line N): expected X … found Y." |
| ≥ 3 nodes | "A diagram needs at least 3 nodes (found K)." |
| ≥ 3 levels | "A diagram needs at least 3 levels of indentation (found K)." |

---

## 3. Components

### 3.1 `models/DiagramNode`
Tree node: `text`, `level` (0 = synthetic root, 1 = top-level), `parent`,
`children`. Helpers: `addChild`, `countDescendants()` (excludes synthetic root),
`depth()` (deepest level number = number of levels). No UNO imports.

### 3.2 `models/DiagramType`
Enum `HIERARCHY`, `HUB_AND_SPOKE`, `PROCESS_FLOW` with display labels for the
dropdown and `fromIndex(int)`.

### 3.3 `parsers/ParseResult`
Immutable result: either `ok(root)` or `error(message)`; `isValid()`,
`getRoot()`, `getErrorMessage()`.

### 3.4 `parsers/HierarchyParser`
`ParseResult parse(String input)` implementing §2. Plus a static
`toOutline(DiagramNode root)` that renders the tree as an indented bullet list
for display. Pure Java — fully unit-testable without LibreOffice.

### 3.5 `SmartArtDialog`
Builds a `com.sun.star.awt.UnoControlDialogModel` at runtime with: a type label
+ dropdown listbox, an input label + multiline edit, and Create/Cancel buttons
(`PushButtonType.OK`/`CANCEL`). `Result show()` runs the dialog modally and
returns `{text, type}` on Create or `null` on Cancel.

The text box is a **list that stays a list** (see §3.8): it is seeded with a
starter outline and edits as an outline via a key handler.

### 3.6 `helpers/LibreOfficeHelper`
`showMessage(ctx, title, message, isError)` using the toolkit
`XMessageBoxFactory` against the current frame window.

### 3.7 `SmartArtCommand.execute()`
Show dialog → if cancelled, return → parse → show outline (info) or error.

### 3.8 List-enforcing input (revision)
The input must be a list from the start and remain one. Approach: a plain
multi-line edit driven as an outline by a key handler — no rich-list widget
(UNO dialogs do not offer one), no bullet glyphs; the "list" is the indentation
the parser already reads (one level = four spaces, `OutlineEditor.INDENT`).

- **`editing/OutlineEditor`** (pure Java, unit-tested): the text transforms —
  `indent`, `outdent`, and `newlineKeepingIndent` — each returning the new text
  plus caret/selection offsets. No UNO, so it is covered by `OutlineEditorTest`.
- **`SmartArtDialog`** seeds the edit with a 3-level starter outline and registers
  an `XKeyHandler` (via `XExtendedToolkit.addKeyHandler`, removed when the dialog
  closes). While the edit has focus:
  - **Tab** → `OutlineEditor.indent` (consume the event so focus does not move);
  - **Shift+Tab** → `OutlineEditor.outdent`;
  - **Enter** → `OutlineEditor.newlineKeepingIndent` (new item at the same level).
  All other keys pass through. The handler only acts when the edit reports focus
  (`XWindow2.hasFocus()`), so it never disturbs other controls.

The parser (§3.4) is unchanged: it already accepts the space-indented outline the
editor produces. Only the text transforms are unit-testable; the key-event wiring
itself needs a manual GUI check.

---

## 4. Build & verify
- `mvn clean package` compiles the new code and runs `HierarchyParserTest` and
  `OutlineEditorTest`.
- Install check (unchanged contract, but confirm the jar still loads): run
  `tools/verify-extension.sh` (see `TESTING_STRATEGY.md`).
- Manual: SmartArt → Create Diagram… → type a 3-level outline → Create →
  message box shows the parsed tree; try bad input → see the matching error.

---

## 5. What's NOT in Phase 3
- ❌ Drawing shapes / connectors on the slide (Phase 4).
- ❌ Layout algorithms per diagram type (Phase 4+).
- ❌ Colour palette / styling (later phase).

---

## 6. Success criteria
✅ Menu opens the dialog · ✅ valid 3-level input shows the parsed tree ·
✅ each invalid case shows its specific message · ✅ Cancel does nothing ·
✅ `HierarchyParserTest` passes in `mvn package` · ✅ extension still installs
and registers via `unopkg`.

---

**Next phase:** Phase 4 — render the parsed tree as grouped, editable shapes on
the current slide.
