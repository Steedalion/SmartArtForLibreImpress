package org.libreimpress.smartart.rendering;

/**
 * Template-independent typography defaults. Colour/shape aesthetics live in
 * {@link StyleTemplate} (Phase 18); {@code StyleTemplate.DEFAULT} carries the
 * original navy→teal ramp this class used to define, pinned by
 * {@code StyleTemplateTest} so the historical look cannot drift.
 */
public final class DefaultPalette {

    private DefaultPalette() {
    }

    /** Font size in points, decreasing by level for visual hierarchy. */
    public static float fontSize(int level) {
        switch (level) {
            case 1:  return 14f;
            case 2:  return 11f;
            default: return 9f;
        }
    }
}
