package org.libreimpress.smartart.rendering;

import org.libreimpress.smartart.layout.ShapeKind;

/**
 * A named visual style for diagrams: accent colour ramp, connector styling,
 * corner rounding and drop-shadow parameters. Selected in the dialog's Style
 * dropdown and persisted in {@code SmartArtMetadata.template} so editing a
 * diagram keeps its look. The user palette field still overrides individual
 * fill colours; the template governs everything else.
 *
 * <p>{@link #DEFAULT} pins the exact pre-Phase-18 aesthetics (see
 * {@code DefaultPalette} and {@code StyleTemplateTest}); every ramp colour in
 * every preset is dark enough for white text.
 */
public enum StyleTemplate {

    /** The navy→teal look shipped since v0.3.0. */
    DEFAULT("Modern",
            new int[]{0x1D3557, 0x2A6F97, 0x2C7DA0, 0x468FAF}, 0x2A9D8F,
            0x595959, 40, 250,
            true, 0x404040, 75, 80,
            25),

    /** Office-style blues with an orange secondary; squared corners. */
    CLASSIC("Classic",
            new int[]{0x1F3864, 0x2F5597, 0x4472C4, 0x6C8EBF}, 0xC55A11,
            0x404040, 40, 0,
            true, 0x333333, 60, 100,
            25),

    /** Flat look: blue-grey ramp, no shadows, square corners, thin connectors. */
    MINIMAL("Minimal",
            new int[]{0x455A64, 0x546E7A, 0x607D8B, 0x78909C}, 0x00897B,
            0x9E9E9E, 20, 0,
            false, 0x000000, 100, 0,
            25),

    /** Greyscale ramp with a subtle shadow. */
    MONO("Mono",
            new int[]{0x212121, 0x424242, 0x616161, 0x757575}, 0x4F4F4F,
            0x757575, 40, 250,
            true, 0x404040, 85, 60,
            30);

    private final String label;
    private final int[] accents;
    private final int secondary;
    private final int connectorColor;
    private final int connectorWidth;
    private final int cornerRadius;
    private final boolean shadow;
    private final int shadowColor;
    private final int shadowTransparence;
    private final int shadowDistance;
    private final int vennTransparence;

    StyleTemplate(String label, int[] accents, int secondary,
            int connectorColor, int connectorWidth, int cornerRadius,
            boolean shadow, int shadowColor, int shadowTransparence,
            int shadowDistance, int vennTransparence) {
        this.label = label;
        this.accents = accents;
        this.secondary = secondary;
        this.connectorColor = connectorColor;
        this.connectorWidth = connectorWidth;
        this.cornerRadius = cornerRadius;
        this.shadow = shadow;
        this.shadowColor = shadowColor;
        this.shadowTransparence = shadowTransparence;
        this.shadowDistance = shadowDistance;
        this.vennTransparence = vennTransparence;
    }

    public String getLabel() { return label; }
    public int getTextColor() { return 0xFFFFFF; }
    public int getConnectorColor() { return connectorColor; }
    public int getConnectorWidth() { return connectorWidth; }
    public int getCornerRadius() { return cornerRadius; }
    public boolean hasShadow() { return shadow; }
    public int getShadowColor() { return shadowColor; }
    public int getShadowTransparence() { return shadowTransparence; }
    public int getShadowDistance() { return shadowDistance; }
    public int getVennTransparence() { return vennTransparence; }

    /** Fill for a sequence-coloured shape (chevrons, tiers, venn, cells…). */
    public int accent(int sequenceIndex) {
        return accents[sequenceIndex % accents.length];
    }

    /** Fill colour for BLOCK_ARROW connector shapes. */
    public int arrowAccent() {
        return accents[2];
    }

    /** Fill for a non-sequence shape, chosen by kind and level. */
    public int fill(ShapeKind kind, int level) {
        if (kind == ShapeKind.ELLIPSE) {
            return level == 1 ? accents[0] : accents[2];
        }
        // RECTANGLE (and any future kinds)
        switch (level) {
            case 1:  return accents[0];
            case 2:  return secondary;
            default: return accents[3];
        }
    }

    /** Labels in declaration order, for the listbox {@code StringItemList}. */
    public static String[] labels() {
        StyleTemplate[] values = values();
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].label;
        }
        return result;
    }

    /** Maps a 0-based dropdown index back to a template, clamped to valid. */
    public static StyleTemplate fromIndex(int index) {
        StyleTemplate[] values = values();
        if (index < 0 || index >= values.length) {
            return DEFAULT;
        }
        return values[index];
    }

    /** Maps a persisted template id back to a template; unknown → DEFAULT. */
    public static StyleTemplate fromName(String name) {
        if (name != null) {
            for (StyleTemplate t : values()) {
                if (t.name().equals(name)) {
                    return t;
                }
            }
        }
        return DEFAULT;
    }
}
