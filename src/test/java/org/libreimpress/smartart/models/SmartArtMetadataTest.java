package org.libreimpress.smartart.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

public class SmartArtMetadataTest {

    @Test
    public void roundTripsAllFields() {
        SmartArtMetadata meta = new SmartArtMetadata(DiagramType.HUB_AND_SPOKE,
                "CLASSIC", "1=#4472C4\n2=#ED7D31", "Root\n- Child\n-- Grandchild");
        Optional<SmartArtMetadata> back = SmartArtMetadata.tryParse(meta.serialize());
        assertTrue(back.isPresent());
        assertEquals(DiagramType.HUB_AND_SPOKE, back.get().getType());
        assertEquals("CLASSIC", back.get().getTemplate());
        assertEquals("1=#4472C4\n2=#ED7D31", back.get().getPaletteText());
        assertEquals("Root\n- Child\n-- Grandchild", back.get().getOutline());
    }

    @Test
    public void roundTripsSemicolonsUnicodeAndBlankLines() {
        String outline = "Größe; Prüfung\n- a=b;c=d\n\n- Ω λ";
        SmartArtMetadata meta = new SmartArtMetadata(DiagramType.CYCLE, null, "", outline);
        Optional<SmartArtMetadata> back = SmartArtMetadata.tryParse(meta.serialize());
        assertTrue(back.isPresent());
        assertEquals(outline, back.get().getOutline());
        assertEquals("", back.get().getPaletteText());
    }

    @Test
    public void serializedFormIsSingleLineWithMarker() {
        SmartArtMetadata meta = new SmartArtMetadata(DiagramType.PYRAMID, "MONO",
                "1=#111111", "Top\n- Mid\n-- Low");
        String s = meta.serialize();
        assertTrue(s.startsWith(SmartArtMetadata.MARKER));
        assertFalse(s.contains("\n"));
    }

    @Test
    public void nullTemplateDefaults() {
        SmartArtMetadata meta = new SmartArtMetadata(DiagramType.CYCLE, null, "", "x");
        assertEquals("DEFAULT", meta.getTemplate());
    }

    @Test
    public void rejectsNonMetadataDescriptions() {
        assertFalse(SmartArtMetadata.tryParse(null).isPresent());
        assertFalse(SmartArtMetadata.tryParse("").isPresent());
        assertFalse(SmartArtMetadata.tryParse("a user-written description").isPresent());
        assertFalse(SmartArtMetadata.tryParse("smartart:v2;type=CYCLE").isPresent());
    }

    @Test
    public void rejectsUnknownTypeAndBadBase64() {
        assertFalse(SmartArtMetadata.tryParse(
                SmartArtMetadata.MARKER + "type=NO_SUCH_TYPE").isPresent());
        assertFalse(SmartArtMetadata.tryParse(
                SmartArtMetadata.MARKER + "type=CYCLE;outline=!!notbase64!!").isPresent());
        assertFalse(SmartArtMetadata.tryParse(
                SmartArtMetadata.MARKER + "template=MONO").isPresent()); // no type
    }

    @Test
    public void ignoresUnknownKeysForForwardCompatibility() {
        Optional<SmartArtMetadata> meta = SmartArtMetadata.tryParse(
                SmartArtMetadata.MARKER + "type=CYCLE;future=stuff;alsofuture");
        assertTrue(meta.isPresent());
        assertEquals(DiagramType.CYCLE, meta.get().getType());
    }

    @Test
    public void displayNameUsesTypeLabel() {
        assertEquals("SmartArt: Hub & Spoke",
                new SmartArtMetadata(DiagramType.HUB_AND_SPOKE, null, "", "")
                        .displayName());
    }
}
