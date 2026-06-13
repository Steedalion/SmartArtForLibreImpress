package org.libreimpress.smartart.rendering;

import org.libreimpress.smartart.layout.ShapeKind;

/**
 * Default fill/text colours for diagram shapes when no user palette is provided.
 *
 * <p>Rules:
 * <ul>
 *   <li>PENTAGON / CHEVRON — cycle through blue shades in chevron sequence order
 *       (index 0 = darkest, so the first/leftmost step is most prominent).</li>
 *   <li>ELLIPSE — dark blue for the hub (level 1), medium blue for spokes (level 2+).</li>
 *   <li>RECTANGLE — dark blue for level-1 boxes, green for level-2 sub-items,
 *       medium blue for deeper levels.</li>
 * </ul>
 * All fills are dark enough that white text is legible over them.
 */
public final class DefaultPalette {

    private static final int DARK_BLUE   = 0x4472C4;
    private static final int MED_BLUE    = 0x5B9BD5;
    private static final int STEEL_BLUE  = 0x2E75B6;
    private static final int NAVY_BLUE   = 0x1F4E79;
    private static final int GREEN       = 0x70AD47;
    public  static final int TEXT_WHITE  = 0xFFFFFF;

    private static final int[] CHEVRON_BLUES = {DARK_BLUE, MED_BLUE, STEEL_BLUE, NAVY_BLUE};

    private DefaultPalette() {
    }

    /** Fill colour for a CHEVRON or PENTAGON shape at position {@code sequenceIndex}. */
    public static int chevronFill(int sequenceIndex) {
        return CHEVRON_BLUES[sequenceIndex % CHEVRON_BLUES.length];
    }

    /** Font size in points, decreasing by level for visual hierarchy. */
    public static float fontSize(int level) {
        switch (level) {
            case 1:  return 14f;
            case 2:  return 11f;
            default: return 9f;
        }
    }

    /** Fill colour for a non-chevron shape, chosen by kind and level. */
    public static int fill(ShapeKind kind, int level) {
        if (kind == ShapeKind.ELLIPSE) {
            return level == 1 ? DARK_BLUE : MED_BLUE;
        }
        // RECTANGLE (and any future kinds)
        switch (level) {
            case 1:  return DARK_BLUE;
            case 2:  return GREEN;
            default: return MED_BLUE;
        }
    }
}
