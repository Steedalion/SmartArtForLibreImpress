package org.libreimpress.smartart.rendering;

import org.libreimpress.smartart.layout.ShapeKind;

/**
 * Default fill/text colours for diagram shapes when no user palette is provided.
 *
 * <p>A cohesive modern blue→teal accent ramp, with a contrasting teal-green for
 * level-2 sub-items. Every colour is dark enough that white text stays legible.
 *
 * <p>Rules:
 * <ul>
 *   <li>PENTAGON / CHEVRON / PYRAMID_TIER / MATRIX_CELL / VENN_CIRCLE — cycle
 *       through the accent ramp in sequence order (index 0 = darkest, so the
 *       first/most prominent element is the deepest tone).</li>
 *   <li>ELLIPSE — darkest accent for the hub (level 1), mid accent for spokes.</li>
 *   <li>RECTANGLE — darkest accent for level-1 boxes, teal-green for level-2
 *       sub-items, light accent for deeper levels.</li>
 * </ul>
 */
public final class DefaultPalette {

    // Cohesive modern blue→teal accent ramp (index 0 = darkest / most prominent).
    private static final int ACCENT_1 = 0x1D3557; // darkest navy
    private static final int ACCENT_2 = 0x2A6F97; // deep blue
    private static final int ACCENT_3 = 0x2C7DA0; // cerulean
    private static final int ACCENT_4 = 0x468FAF; // light teal-blue
    private static final int SECONDARY = 0x2A9D8F; // teal-green for level-2 sub-items

    public  static final int TEXT_WHITE   = 0xFFFFFF;
    /** Fill colour for BLOCK_ARROW connector shapes. */
    public  static final int ARROW_ACCENT = ACCENT_3;

    private static final int[] ACCENTS = {ACCENT_1, ACCENT_2, ACCENT_3, ACCENT_4};

    private DefaultPalette() {
    }

    /** Fill colour for a sequence-coloured shape at position {@code sequenceIndex}. */
    public static int chevronFill(int sequenceIndex) {
        return ACCENTS[sequenceIndex % ACCENTS.length];
    }

    /** Font size in points, decreasing by level for visual hierarchy. */
    public static float fontSize(int level) {
        switch (level) {
            case 1:  return 14f;
            case 2:  return 11f;
            default: return 9f;
        }
    }

    /** Fill colour for a non-sequence shape, chosen by kind and level. */
    public static int fill(ShapeKind kind, int level) {
        if (kind == ShapeKind.ELLIPSE) {
            return level == 1 ? ACCENT_1 : ACCENT_3;
        }
        // RECTANGLE (and any future kinds)
        switch (level) {
            case 1:  return ACCENT_1;
            case 2:  return SECONDARY;
            default: return ACCENT_4;
        }
    }
}
