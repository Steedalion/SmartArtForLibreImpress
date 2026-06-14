package org.libreimpress.smartart.parsers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class HierarchyParserTest {

    private final HierarchyParser parser = new HierarchyParser();

    @Test
    public void parsesThreeLevelOutline() {
        ParseResult r = parser.parse("Root\n- Child\n-- Grandchild");
        assertTrue(r.getErrorMessage(), r.isValid());
        DiagramNode root = r.getRoot();
        assertEquals(3, root.countDescendants());
        assertEquals(3, root.depth());

        assertEquals(1, root.getChildren().size());
        DiagramNode level1 = root.getChildren().get(0);
        assertEquals("Root", level1.getText());
        assertEquals(1, level1.getLevel());

        DiagramNode level2 = level1.getChildren().get(0);
        assertEquals("Child", level2.getText());
        assertEquals(2, level2.getLevel());
        assertEquals(level1, level2.getParent());

        DiagramNode level3 = level2.getChildren().get(0);
        assertEquals("Grandchild", level3.getText());
        assertEquals(3, level3.getLevel());
    }

    @Test
    public void supportsMultipleTopLevelItemsAndSiblings() {
        String text = "A\n- A1\n-- A1a\n- A2\nB\n- B1\n-- B1a";
        ParseResult r = parser.parse(text);
        assertTrue(r.getErrorMessage(), r.isValid());
        DiagramNode root = r.getRoot();
        assertEquals(2, root.getChildren().size());
        assertEquals(7, root.countDescendants());
        assertEquals(3, root.depth());

        DiagramNode a = root.getChildren().get(0);
        assertEquals(2, a.getChildren().size()); // A1, A2
        assertEquals("A2", a.getChildren().get(1).getText());
    }

    @Test
    public void ignoresBlankLines() {
        ParseResult r = parser.parse("Root\n\n- Child\n\n-- Grandchild\n");
        assertTrue(r.getErrorMessage(), r.isValid());
        assertEquals(3, r.getRoot().countDescendants());
    }

    @Test
    public void parsesWithDashPrefix() {
        ParseResult r = parser.parse("Root\n- Child\n-- Grandchild");
        assertTrue(r.getErrorMessage(), r.isValid());
        assertEquals(3, r.getRoot().depth());
    }

    @Test
    public void rejectsEmptyInput() {
        ParseResult r = parser.parse("   \n\n");
        assertFalse(r.isValid());
        assertNull(r.getRoot());
        assertTrue(r.getErrorMessage().contains("at least one line"));
    }

    @Test
    public void rejectsFirstLineWithDashPrefix() {
        ParseResult r = parser.parse("- Root\n-- Child\n--- Grandchild");
        assertFalse(r.isValid());
        assertTrue(r.getErrorMessage().contains("first line"));
    }

    @Test
    public void rejectsDepthJump() {
        // Skips from depth 0 directly to depth 3 — must introduce each level via its parent.
        ParseResult r = parser.parse("Root\n--- TooDeep\n--- Also");
        assertFalse(r.isValid());
        assertTrue(r.getErrorMessage().toLowerCase().contains("jump"));
    }

    @Test
    public void rejectsDepthJumpInMiddle() {
        // depth 1 → depth 3: skips depth 2
        ParseResult r = parser.parse("Root\n- Child\n--- GrandGrandChild\n--- Also");
        assertFalse(r.isValid());
        assertTrue(r.getErrorMessage().toLowerCase().contains("jump"));
    }

    @Test
    public void acceptsTwoLevels() {
        ParseResult r = parser.parse("Root\n- Child\n- Child2");
        assertTrue(r.getErrorMessage(), r.isValid());
        assertEquals(2, r.getRoot().depth());
    }

    @Test
    public void rejectsFewerThanThreeNodes() {
        ParseResult r = parser.parse("Root\n- Child");
        assertFalse(r.isValid());
        assertTrue(r.getErrorMessage().contains("3 nodes"));
    }

    @Test
    public void toOutlineRendersDashFormat() {
        ParseResult r = parser.parse("Root\n- Child\n-- Grandchild");
        assertNotNull(r.getRoot());
        String outline = HierarchyParser.toOutline(r.getRoot());
        assertEquals("Root\n- Child\n-- Grandchild\n", outline);
    }
}
