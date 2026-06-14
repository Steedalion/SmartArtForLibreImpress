package org.libreimpress.smartart.editing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OutlineEditorTest {

    @Test
    public void indentSingleLineAddsOneDashPrefix() {
        // "abc" at depth 0 → "- abc" at depth 1; caret advances past the new "- "
        OutlineEditor.Edit r = OutlineEditor.indent("abc", 0, 0);
        assertEquals("- abc", r.text);
        assertEquals(2, r.selStart);
        assertEquals(2, r.selEnd);
    }

    @Test
    public void indentSecondLineOnly() {
        // "a\nbc", caret on second line → "a\n- bc"; caret advances by 2
        OutlineEditor.Edit r = OutlineEditor.indent("a\nbc", 3, 3);
        assertEquals("a\n- bc", r.text);
        assertEquals(5, r.selStart);
    }

    @Test
    public void outdentRemovesOneLevel() {
        // "-- abc" at depth 2 → "- abc" at depth 1; caret snaps to end of new prefix
        OutlineEditor.Edit r = OutlineEditor.outdent("-- abc", 3, 3);
        assertEquals("- abc", r.text);
        assertEquals(2, r.selStart);
    }

    @Test
    public void outdentDepthOneGoesToZero() {
        // "- abc" at depth 1 → "abc" at depth 0
        OutlineEditor.Edit r = OutlineEditor.outdent("- abc", 2, 2);
        assertEquals("abc", r.text);
        assertEquals(0, r.selStart);
    }

    @Test
    public void outdentAtDepthZeroIsNoop() {
        OutlineEditor.Edit r = OutlineEditor.outdent("abc", 1, 1);
        assertEquals("abc", r.text);
        assertEquals(1, r.selStart);
    }

    @Test
    public void newlineKeepsDashPrefix() {
        // caret at end of "- abc" → insert newline + same "- " prefix
        OutlineEditor.Edit r = OutlineEditor.newlineKeepingIndent("- abc", 5, 5);
        assertEquals("- abc\n- ", r.text);
        assertEquals(8, r.selStart);
    }

    @Test
    public void newlineAtTopLevelHasNoPrefix() {
        OutlineEditor.Edit r = OutlineEditor.newlineKeepingIndent("abc", 3, 3);
        assertEquals("abc\n", r.text);
        assertEquals(4, r.selStart);
    }

    @Test
    public void indentMultipleSelectedLines() {
        OutlineEditor.Edit r = OutlineEditor.indent("a\nb", 0, 3);
        assertEquals("- a\n- b", r.text);
    }

    @Test
    public void indentThenOutdentRoundTrips() {
        String text = "Main\nSub";
        OutlineEditor.Edit in = OutlineEditor.indent(text, 5, 5); // caret on "Sub"
        OutlineEditor.Edit out = OutlineEditor.outdent(in.text, in.selStart, in.selEnd);
        assertEquals(text, out.text);
    }
}
