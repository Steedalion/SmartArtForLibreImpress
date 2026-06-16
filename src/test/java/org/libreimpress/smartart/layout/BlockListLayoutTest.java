package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class BlockListLayoutTest {

    private static DiagramNode root(String... labels) {
        DiagramNode root = new DiagramNode("", 0);
        for (String label : labels) {
            root.addChild(new DiagramNode(label, 1));
        }
        return root;
    }

    @Test
    public void emptyInputProducesEmptyLayout() {
        DiagramLayout layout = BlockListLayout.layout(new DiagramNode("", 0));
        assertEquals(0, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void oneShapePerLevel1NodeAndNoEdges() {
        DiagramLayout layout = BlockListLayout.layout(root("A", "B", "C", "D", "E"));
        assertEquals(5, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void allBlocksAreRectangles() {
        DiagramLayout layout = BlockListLayout.layout(root("A", "B", "C"));
        for (LaidOutShape s : layout.getShapes()) {
            assertEquals(ShapeKind.RECTANGLE, s.getKind());
        }
    }

    @Test
    public void blocksWrapIntoAGrid() {
        // 4 nodes -> cols = ceil(sqrt(4)) = 2, so a 2x2 grid.
        DiagramLayout layout = BlockListLayout.layout(root("A", "B", "C", "D"));
        LaidOutShape a = layout.getShapes().get(0);
        LaidOutShape b = layout.getShapes().get(1);
        LaidOutShape c = layout.getShapes().get(2);
        // B is to the right of A on the same row.
        assertEquals(a.getY(), b.getY());
        assertTrue(b.getX() > a.getX());
        // C wraps to the next row, back at A's column.
        assertEquals(a.getX(), c.getX());
        assertTrue(c.getY() > a.getY());
    }

    @Test
    public void childrenBecomeBulletLinesInBlockText() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode a = new DiagramNode("Fruit", 1);
        a.addChild(new DiagramNode("Apple", 2));
        a.addChild(new DiagramNode("Pear", 2));
        root.addChild(a);
        root.addChild(new DiagramNode("B", 1));
        root.addChild(new DiagramNode("C", 1));

        DiagramLayout layout = BlockListLayout.layout(root);
        String text = layout.getShapes().get(0).getText();
        assertTrue(text.startsWith("Fruit"));
        assertTrue(text.contains("• Apple"));
        assertTrue(text.contains("• Pear"));
    }

    @Test
    public void level3AndDeeperChildrenAreNestedNotDropped() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode block = new DiagramNode("Build", 1);
        DiagramNode sub = new DiagramNode("Backend", 2);
        sub.addChild(new DiagramNode("Database", 3));
        block.addChild(sub);
        root.addChild(block);
        root.addChild(new DiagramNode("B", 1));
        root.addChild(new DiagramNode("C", 1));

        DiagramLayout layout = BlockListLayout.layout(root);
        String text = layout.getShapes().get(0).getText();
        assertTrue("level-2 bullet present", text.contains("• Backend"));
        assertTrue("level-3 item must be nested, not dropped", text.contains("Database"));
    }

    @Test
    public void blocksStayWithinTheSlide() {
        DiagramLayout layout = BlockListLayout.layout(root("A", "B", "C", "D", "E", "F"));
        for (LaidOutShape s : layout.getShapes()) {
            assertTrue(s.getX() >= 0);
            assertTrue(s.getY() >= 0);
            assertTrue(s.getX() + s.getWidth() <= BlockListLayout.SLIDE_W);
            assertTrue(s.getY() + s.getHeight() <= BlockListLayout.SLIDE_H);
        }
    }
}
