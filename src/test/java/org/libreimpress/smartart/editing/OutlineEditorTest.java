package org.libreimpress.smartart.editing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OutlineEditorTest {

    @Test
    public void indentSingleLineMovesCaretAfterIndent() {
        OutlineEditor.Edit r = OutlineEditor.indent("abc", 0, 0);
        assertEquals("    abc", r.text);
        assertEquals(4, r.selStart);
        assertEquals(4, r.selEnd);
    }

    @Test
    public void indentSecondLineOnly() {
        // "a\nbc", caret before 'c' (offset 3)
        OutlineEditor.Edit r = OutlineEditor.indent("a\nbc", 3, 3);
        assertEquals("a\n    bc", r.text);
        assertEquals(7, r.selStart);
    }

    @Test
    public void outdentRemovesOneLevel() {
        OutlineEditor.Edit r = OutlineEditor.outdent("    abc", 4, 4);
        assertEquals("abc", r.text);
        assertEquals(0, r.selStart);
    }

    @Test
    public void outdentRemovesOnlyAvailableSpaces() {
        OutlineEditor.Edit r = OutlineEditor.outdent("  ab", 2, 2);
        assertEquals("ab", r.text);
        assertEquals(0, r.selStart);
    }

    @Test
    public void outdentAtColumnZeroIsNoop() {
        OutlineEditor.Edit r = OutlineEditor.outdent("abc", 1, 1);
        assertEquals("abc", r.text);
        assertEquals(1, r.selStart);
    }

    @Test
    public void newlineKeepsLeadingIndent() {
        // caret at end of "    abc" (offset 7)
        OutlineEditor.Edit r = OutlineEditor.newlineKeepingIndent("    abc", 7, 7);
        assertEquals("    abc\n    ", r.text);
        assertEquals(12, r.selStart);
    }

    @Test
    public void newlineAtTopLevelHasNoIndent() {
        OutlineEditor.Edit r = OutlineEditor.newlineKeepingIndent("abc", 3, 3);
        assertEquals("abc\n", r.text);
        assertEquals(4, r.selStart);
    }

    @Test
    public void indentMultipleSelectedLines() {
        OutlineEditor.Edit r = OutlineEditor.indent("a\nb", 0, 3);
        assertEquals("    a\n    b", r.text);
    }

    @Test
    public void indentThenOutdentRoundTrips() {
        String text = "Main\nSub";
        OutlineEditor.Edit in = OutlineEditor.indent(text, 5, 5); // caret on "Sub"
        OutlineEditor.Edit out = OutlineEditor.outdent(in.text, in.selStart, in.selEnd);
        assertEquals(text, out.text);
    }
}
