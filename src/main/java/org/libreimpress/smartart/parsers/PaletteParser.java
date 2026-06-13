package org.libreimpress.smartart.parsers;

import org.libreimpress.smartart.models.ColorPalette;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses an optional user-supplied colour palette from plain text.
 *
 * <p>Format — one entry per line:
 * <pre>
 *   1=#4472C4
 *   2=#70AD47
 *   3=5B9BD5
 * </pre>
 * Leading {@code #} on the hex value is optional. Blank lines and
 * lines that do not match the {@code <level>=<hex>} pattern are skipped silently.
 */
public final class PaletteParser {

    private PaletteParser() {
    }

    /**
     * Parses {@code text} into a {@link ColorPalette}.
     * Returns {@link ColorPalette#EMPTY} for null / blank input.
     */
    public static ColorPalette parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return ColorPalette.EMPTY;
        }
        Map<Integer, Integer> colors = new HashMap<>();
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String levelStr = line.substring(0, eq).trim();
            String colorStr = line.substring(eq + 1).trim();
            if (colorStr.startsWith("#")) {
                colorStr = colorStr.substring(1);
            }
            try {
                int level = Integer.parseInt(levelStr);
                int color = Integer.parseInt(colorStr, 16);
                if (level >= 1) {
                    colors.put(level, color);
                }
            } catch (NumberFormatException ignored) {
                // skip malformed lines
            }
        }
        return new ColorPalette(colors);
    }
}
