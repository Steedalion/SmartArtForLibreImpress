package org.libreimpress.smartart.models;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * The source of a generated diagram, persisted in the diagram group shape's
 * {@code Description} property (serialized to {@code <svg:desc>} in ODF, which
 * survives save/reload — proven by {@code metadata_persistence_probe.py}).
 * Re-invoking Create Diagram with such a group selected prefills the dialog
 * from this metadata and replaces the group in place.
 *
 * <p>Wire format (single line):
 * <pre>smartart:v1;type=&lt;DiagramType.name()&gt;;template=&lt;id&gt;;palette=&lt;base64&gt;;outline=&lt;base64&gt;</pre>
 * Outline and palette are Base64-encoded UTF-8 so newlines, semicolons and
 * unicode never need escaping. Unknown keys are ignored and unknown versions
 * parse to empty, so future formats can evolve without breaking older builds.
 */
public final class SmartArtMetadata {

    /** Version marker every serialized form starts with. */
    public static final String MARKER = "smartart:v1;";

    private final DiagramType type;
    private final String template;
    private final String paletteText;
    private final String outline;

    public SmartArtMetadata(DiagramType type, String template,
            String paletteText, String outline) {
        this.type = type;
        this.template = (template == null || template.isEmpty())
                ? "DEFAULT" : template;
        this.paletteText = paletteText == null ? "" : paletteText;
        this.outline = outline == null ? "" : outline;
    }

    public DiagramType getType() { return type; }
    public String getTemplate() { return template; }
    public String getPaletteText() { return paletteText; }
    public String getOutline() { return outline; }

    /** A short human-readable label for the group's {@code Name} property. */
    public String displayName() {
        return "SmartArt: " + type.getLabel();
    }

    public String serialize() {
        return MARKER
                + "type=" + type.name()
                + ";template=" + template
                + ";palette=" + encode(paletteText)
                + ";outline=" + encode(outline);
    }

    /**
     * Parses a {@code Description} value. Empty when the value is not SmartArt
     * metadata, is from an unknown version, or is corrupt — callers then treat
     * the selection as an ordinary shape.
     */
    public static Optional<SmartArtMetadata> tryParse(String description) {
        if (description == null || !description.startsWith(MARKER)) {
            return Optional.empty();
        }
        String typeName = null;
        String template = "";
        String palette = "";
        String outline = "";
        try {
            for (String field : description.substring(MARKER.length()).split(";")) {
                int eq = field.indexOf('=');
                if (eq < 0) {
                    continue;
                }
                String key = field.substring(0, eq);
                String value = field.substring(eq + 1);
                switch (key) {
                    case "type":     typeName = value; break;
                    case "template": template = value; break;
                    case "palette":  palette = decode(value); break;
                    case "outline":  outline = decode(value); break;
                    default:         break; // forward compatibility
                }
            }
            if (typeName == null) {
                return Optional.empty();
            }
            return Optional.of(new SmartArtMetadata(
                    DiagramType.valueOf(typeName), template, palette, outline));
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // unknown type name or bad Base64
        }
    }

    private static String encode(String s) {
        return Base64.getEncoder()
                .encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String s) {
        return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
    }
}
