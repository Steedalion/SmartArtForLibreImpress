# Phase 10 Implementation Plan — User Colour Palette

## Goal

Let the user supply per-level fill colours via the dialog. An optional *Colours*
text field accepts `<level>=<#RRGGBB>` lines; levels not mentioned keep the
built-in `DefaultPalette` colours.

## Files Changed

| File | Change |
|------|--------|
| `src/main/java/org/libreimpress/smartart/models/ColorPalette.java` | New: `UNSET` sentinel, `EMPTY` constant, `getFillColor(level)`, `isEmpty()` |
| `src/main/java/org/libreimpress/smartart/parsers/PaletteParser.java` | New: parse `level=hex` lines; return `ColorPalette.EMPTY` for null/blank input |
| `src/test/java/org/libreimpress/smartart/parsers/PaletteParserTest.java` | New: 8 tests (null, blank, with/without `#`, multiple levels, skip blanks/malformed, UNSET for missing, case-insensitive) |
| `src/main/java/org/libreimpress/smartart/rendering/SlideRenderer.java` | `drawHierarchy(layout)` delegates to `drawHierarchy(layout, ColorPalette.EMPTY)`; palette-aware overload checks `palette.getFillColor(level)` before `DefaultPalette` |
| `src/main/java/org/libreimpress/smartart/SmartArtDialog.java` | Shrink `txtInput` height to 86; add `lblPalette` label and `txtPalette` edit at Y=136/148; move buttons to Y=190; dialog height 216; `Result` gains `paletteText` field |
| `src/main/java/org/libreimpress/smartart/SmartArtCommand.java` | Call `PaletteParser.parse(result.getPaletteText())` and pass `ColorPalette` to `drawHierarchy` |

## ColorPalette API

```java
public static final int UNSET = -1;
public static final ColorPalette EMPTY = new ColorPalette(new HashMap<>());

public int getFillColor(int level);   // returns UNSET if no entry for level
public boolean isEmpty();
```

## PaletteParser Format

One entry per line, extra whitespace and `#` prefix are ignored:

```
1=#4472C4
2=70AD47
3=5B9BD5
```

Lines that do not match `<integer>=<hex>` are silently skipped.

## Rendering Logic

For each shape at `level`:
1. `userColor = palette.getFillColor(level)`
2. If `userColor != UNSET` → use `userColor`
3. Else → use `DefaultPalette.fill(kind, level)` (or `DefaultPalette.chevronFill(seq)`)

Chevron sequence counter (`chevronSeq`) still increments per chevron regardless
of which colour source wins, so cycling is not disrupted by partial palette overrides.

## Dialog Layout (units are UNO dialog units)

| Control | Y | Height |
|---------|---|--------|
| `lstType` dropdown | 6 | 14 |
| `txtInput` (text area) | 44 | 86 |
| `lblPalette` label | 136 | 10 |
| `txtPalette` (palette field) | 148 | 36 |
| buttons | 190 | 16 |
| Dialog total height | — | 216 |

## Success Criteria

1. `PaletteParserTest` — all 8 tests pass.
2. `SmartArtDialog.Result.getPaletteText()` returns the text the user typed.
3. `SlideRenderer.drawHierarchy(layout, palette)` uses user colours where set
   and falls back to `DefaultPalette` for unset levels.
4. Build (`mvn clean package`) succeeds with no warnings.
