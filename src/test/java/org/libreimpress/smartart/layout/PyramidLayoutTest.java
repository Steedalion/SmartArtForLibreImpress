package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class PyramidLayoutTest {

    private static DiagramNode root(String... labels) {
        DiagramNode root = new DiagramNode("", 0);
        for (String label : labels) {
            root.addChild(new DiagramNode(label, 1));
        }
        return root;
    }

    @Test
    public void threeTiersProducesThreeShapes() {
        DiagramLayout layout = PyramidLayout.layout(root("A", "B", "C"));
        long tierCount = layout.getShapes().stream()
                .filter(s -> s.getKind() == ShapeKind.PYRAMID_TIER).count();
        long rectCount = layout.getShapes().stream()
                .filter(s -> s.getKind() == ShapeKind.RECTANGLE).count();
        assertEquals(3, tierCount);
        assertEquals(0, rectCount);
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void tiersAreWidestAtBottom() {
        DiagramLayout layout = PyramidLayout.layout(root("A", "B", "C", "D"));
        int prev = -1;
        for (LaidOutShape s : layout.getShapes()) {
            assertTrue(s.getWidth() > prev);
            prev = s.getWidth();
        }
    }

    @Test
    public void tiersAreCentredOnSlide() {
        DiagramLayout layout = PyramidLayout.layout(root("A", "B", "C"));
        int expected = PyramidLayout.SLIDE_W / 2;
        for (LaidOutShape s : layout.getShapes()) {
            int cx = s.getX() + s.getWidth() / 2;
            assertTrue("tier centre-X should equal slide centre ±1",
                    Math.abs(cx - expected) <= 1);
        }
    }

    @Test
    public void singleNodeProducesMaxWidth() {
        DiagramLayout layout = PyramidLayout.layout(root("Solo"));
        assertEquals(1, layout.getShapes().size());
        assertEquals(PyramidLayout.MAX_W, layout.getShapes().get(0).getWidth());
    }

    @Test
    public void level2ChildrenPlacedRightOfTier() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode tier = new DiagramNode("Tier", 1);
        tier.addChild(new DiagramNode("Child", 2));
        root.addChild(tier);

        DiagramLayout layout = PyramidLayout.layout(root);
        LaidOutShape tierShape  = layout.getShapes().get(0);
        LaidOutShape childShape = layout.getShapes().get(1);

        int minChildX = tierShape.getX() + tierShape.getWidth() + PyramidLayout.CHILD_GAP;
        assertTrue("child x should be right of tier + gap", childShape.getX() >= minChildX);
    }

    @Test
    public void level2ChildrenStackVertically() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode tier = new DiagramNode("Tier", 1);
        tier.addChild(new DiagramNode("C1", 2));
        tier.addChild(new DiagramNode("C2", 2));
        root.addChild(tier);

        DiagramLayout layout = PyramidLayout.layout(root);
        LaidOutShape c1 = layout.getShapes().get(1);
        LaidOutShape c2 = layout.getShapes().get(2);
        assertTrue("second child should be below first", c2.getY() > c1.getY());
    }

    @Test
    public void tierLevelEqualsIndexPlusOne() {
        DiagramLayout layout = PyramidLayout.layout(root("A", "B", "C"));
        for (int i = 0; i < 3; i++) {
            assertEquals(i + 1, layout.getShapes().get(i).getLevel());
        }
    }

    @Test
    public void emptyInputProducesEmptyLayout() {
        DiagramLayout layout = PyramidLayout.layout(new DiagramNode("", 0));
        assertEquals(0, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }
}
