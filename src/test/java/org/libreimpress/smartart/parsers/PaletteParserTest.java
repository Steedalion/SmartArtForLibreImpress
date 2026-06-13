package org.libreimpress.smartart.parsers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.ColorPalette;

public class PaletteParserTest {

    @Test
    public void nullInputReturnsEmpty() {
        assertTrue(PaletteParser.parse(null).isEmpty());
    }

    @Test
    public void blankInputReturnsEmpty() {
        assertTrue(PaletteParser.parse("   \n\n").isEmpty());
    }

    @Test
    public void parsesHexWithHash() {
        ColorPalette p = PaletteParser.parse("1=#4472C4");
        assertEquals(0x4472C4, p.getFillColor(1));
    }

    @Test
    public void parsesHexWithoutHash() {
        ColorPalette p = PaletteParser.parse("2=70AD47");
        assertEquals(0x70AD47, p.getFillColor(2));
    }

    @Test
    public void parsesMultipleLevels() {
        ColorPalette p = PaletteParser.parse("1=#4472C4\n2=#70AD47\n3=#5B9BD5");
        assertEquals(0x4472C4, p.getFillColor(1));
        assertEquals(0x70AD47, p.getFillColor(2));
        assertEquals(0x5B9BD5, p.getFillColor(3));
    }

    @Test
    public void skipsBlankLines() {
        ColorPalette p = PaletteParser.parse("\n1=#FF0000\n\n2=#00FF00\n");
        assertEquals(0xFF0000, p.getFillColor(1));
        assertEquals(0x00FF00, p.getFillColor(2));
    }

    @Test
    public void skipsMalformedLines() {
        ColorPalette p = PaletteParser.parse("not-valid\n1=#4472C4\nalso bad");
        assertEquals(0x4472C4, p.getFillColor(1));
        assertEquals(ColorPalette.UNSET, p.getFillColor(2));
    }

    @Test
    public void unsetLevelReturnsUnset() {
        ColorPalette p = PaletteParser.parse("1=#4472C4");
        assertEquals(ColorPalette.UNSET, p.getFillColor(99));
    }

    @Test
    public void caseInsensitiveHex() {
        ColorPalette p = PaletteParser.parse("1=#4472c4");
        assertEquals(0x4472C4, p.getFillColor(1));
    }
}
