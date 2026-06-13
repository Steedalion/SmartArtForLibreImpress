package org.libreimpress.smartart;

import org.libreimpress.smartart.editing.OutlineEditor;

/**
 * Configuration and defaults for SmartArt. Can be overridden for custom
 * seed text or other settings.
 */
public final class SmartArtConfig {

    /** Default seed text for the input dialog. Override by calling setSeedText(). */
    private static String seedText = buildDefaultSeedText();

    private SmartArtConfig() {
    }

    /** Get the current seed text (default or overridden). */
    public static String getSeedText() {
        return seedText;
    }

    /** Override the seed text globally. */
    public static void setSeedText(String text) {
        seedText = text;
    }

    /** Reset to the built-in default seed text. */
    public static void resetToDefault() {
        seedText = buildDefaultSeedText();
    }

    /** The built-in default: 3-level hierarchy with NATO phonetic alphabet names. */
    private static String buildDefaultSeedText() {
        return "Alpha\n"
                + OutlineEditor.INDENT + "Bravo\n"
                + OutlineEditor.INDENT + "Charlie\n"
                + "Delta\n"
                + OutlineEditor.INDENT + "Echo\n"
                + OutlineEditor.INDENT + "Foxtrot\n"
                + OutlineEditor.INDENT + OutlineEditor.INDENT + "Golf\n"
                + OutlineEditor.INDENT + OutlineEditor.INDENT + "Hotel";
    }
}
