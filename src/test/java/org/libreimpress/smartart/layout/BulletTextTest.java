package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class BulletTextTest {

    /** Builds Title > Child > Grandchild > Great-grandchild (levels 1..4). */
    private static DiagramNode deepNode() {
        DiagramNode title = new DiagramNode("Title", 1);
        DiagramNode child = new DiagramNode("Child", 2);
        DiagramNode grand = new DiagramNode("Grand", 3);
        DiagramNode great = new DiagramNode("Great", 4);
        grand.addChild(great);
        child.addChild(grand);
        title.addChild(child);
        return title;
    }

    @Test
    public void withTitleStartsWithTheTitleLine() {
        String text = BulletText.withTitle(deepNode());
        assertEquals("Title", text.split("\n", 2)[0]);
    }

    @Test
    public void bulletsOmitsTheTitleLine() {
        String text = BulletText.bullets(deepNode());
        assertFalse("first line must not be the title", text.startsWith("Title"));
        assertTrue(text.startsWith("• Child"));
    }

    @Test
    public void nestsAllDepthsBeyondTwo() {
        // The level-3 (Grand) and level-4 (Great) items must appear, not be dropped.
        String text = BulletText.withTitle(deepNode());
        assertTrue(text.contains("Grand"));
        assertTrue(text.contains("Great"));
    }

    @Test
    public void deeperLevelsAreIndentedAndUseDistinctMarkers() {
        String text = BulletText.withTitle(deepNode());
        // Level 2 -> "• " at column 0; level 3 -> indented "◦ "; level 4 -> deeper "▪ ".
        assertTrue(text.contains("\n• Child"));
        assertTrue(text.contains("\n    ◦ Grand"));
        assertTrue(text.contains("\n        ▪ Great"));
    }

    @Test
    public void noChildrenGivesTitleOnlyOrEmpty() {
        DiagramNode leaf = new DiagramNode("Solo", 1);
        assertEquals("Solo", BulletText.withTitle(leaf));
        assertEquals("", BulletText.bullets(leaf));
    }

    @Test
    public void siblingsAreListedInOrder() {
        DiagramNode node = new DiagramNode("T", 1);
        node.addChild(new DiagramNode("A", 2));
        node.addChild(new DiagramNode("B", 2));
        assertEquals("• A\n• B", BulletText.bullets(node));
    }
}
