package org.libreimpress.smartart.rendering;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.layout.ShapeKind;

public class StyleTemplateTest {

    /**
     * DEFAULT must match the exact pre-Phase-18 aesthetics (the historical
     * DefaultPalette ramp and SlideRenderer constants) so the shipped look
     * cannot drift silently.
     */
    @Test
    public void defaultPinsHistoricalAesthetics() {
        StyleTemplate t = StyleTemplate.DEFAULT;
        assertEquals(0x1D3557, t.accent(0)); // darkest navy
        assertEquals(0x2A6F97, t.accent(1)); // deep blue
        assertEquals(0x2C7DA0, t.accent(2)); // cerulean
        assertEquals(0x468FAF, t.accent(3)); // light teal-blue
        assertEquals(0x1D3557, t.accent(4)); // ramp wraps
        assertEquals(0x2C7DA0, t.arrowAccent());
        assertEquals(0x1D3557, t.fill(ShapeKind.ELLIPSE, 1));
        assertEquals(0x2C7DA0, t.fill(ShapeKind.ELLIPSE, 2));
        assertEquals(0x1D3557, t.fill(ShapeKind.RECTANGLE, 1));
        assertEquals(0x2A9D8F, t.fill(ShapeKind.RECTANGLE, 2)); // teal-green
        assertEquals(0x468FAF, t.fill(ShapeKind.RECTANGLE, 3));
        assertEquals(0xFFFFFF, t.getTextColor());
        assertEquals(0x595959, t.getConnectorColor());
        assertEquals(40, t.getConnectorWidth());
        assertEquals(250, t.getCornerRadius());
        assertTrue(t.hasShadow());
        assertEquals(0x404040, t.getShadowColor());
        assertEquals(75, t.getShadowTransparence());
        assertEquals(80, t.getShadowDistance());
        assertEquals(25, t.getVennTransparence());
    }

    @Test
    public void labelsMatchDeclarationOrder() {
        String[] labels = StyleTemplate.labels();
        assertEquals(StyleTemplate.values().length, labels.length);
        assertEquals("Modern", labels[0]);
        for (int i = 0; i < labels.length; i++) {
            assertEquals(StyleTemplate.values()[i].getLabel(), labels[i]);
        }
    }

    @Test
    public void fromIndexClampsToDefault() {
        assertEquals(StyleTemplate.DEFAULT, StyleTemplate.fromIndex(-1));
        assertEquals(StyleTemplate.DEFAULT, StyleTemplate.fromIndex(99));
        assertEquals(StyleTemplate.values()[1], StyleTemplate.fromIndex(1));
    }

    @Test
    public void fromNameFallsBackToDefault() {
        assertEquals(StyleTemplate.MONO, StyleTemplate.fromName("MONO"));
        assertEquals(StyleTemplate.DEFAULT, StyleTemplate.fromName(null));
        assertEquals(StyleTemplate.DEFAULT, StyleTemplate.fromName("NO_SUCH"));
    }

    @Test
    public void minimalIsFlat() {
        StyleTemplate t = StyleTemplate.MINIMAL;
        assertFalse(t.hasShadow());
        assertEquals(0, t.getCornerRadius());
    }

    /**
     * Every preset keeps white text legible. Threshold 0.55: the lightest
     * colour shipped since v0.3.0 (0x468FAF, luminance ≈ 0.51) is the
     * accepted upper bound for white-on-fill contrast.
     */
    @Test
    public void allPresetFillsAreDarkEnoughForWhiteText() {
        for (StyleTemplate t : StyleTemplate.values()) {
            for (int i = 0; i < 4; i++) {
                assertTrue(t.name() + " accent " + i + " too light",
                        luminance(t.accent(i)) < 0.55);
            }
            assertTrue(t.name() + " secondary too light",
                    luminance(t.fill(ShapeKind.RECTANGLE, 2)) < 0.55);
        }
    }

    private static double luminance(int rgb) {
        double r = ((rgb >> 16) & 0xFF) / 255.0;
        double g = ((rgb >> 8) & 0xFF) / 255.0;
        double b = (rgb & 0xFF) / 255.0;
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }
}
