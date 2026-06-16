package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class VennLayoutTest {

    private static DiagramNode root(String... labels) {
        DiagramNode root = new DiagramNode("", 0);
        for (String label : labels) {
            root.addChild(new DiagramNode(label, 1));
        }
        return root;
    }

    @Test
    public void emptyInputProducesEmptyLayout() {
        DiagramLayout layout = VennLayout.layout(new DiagramNode("", 0));
        assertEquals(0, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void oneShapePerLevel1NodeAndNoEdges() {
        DiagramLayout layout = VennLayout.layout(root("A", "B", "C"));
        assertEquals(3, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void allShapesAreVennCircles() {
        DiagramLayout layout = VennLayout.layout(root("A", "B", "C"));
        for (LaidOutShape s : layout.getShapes()) {
            assertEquals(ShapeKind.VENN_CIRCLE, s.getKind());
            assertEquals("circles must be square (equal w/h)", s.getWidth(), s.getHeight());
        }
    }

    @Test
    public void singleCircleIsCentred() {
        DiagramLayout layout = VennLayout.layout(root("Only"));
        LaidOutShape s = layout.getShapes().get(0);
        assertEquals(VennLayout.SLIDE_W / 2, s.centerX());
        assertEquals(VennLayout.SLIDE_H / 2, s.centerY());
    }

    @Test
    public void twoCirclesArePlacedHorizontally() {
        DiagramLayout layout = VennLayout.layout(root("Left", "Right"));
        LaidOutShape a = layout.getShapes().get(0);
        LaidOutShape b = layout.getShapes().get(1);
        // Same vertical centre, different horizontal centre.
        assertEquals(a.centerY(), b.centerY());
        assertTrue(a.centerX() < VennLayout.SLIDE_W / 2);
        assertTrue(b.centerX() > VennLayout.SLIDE_W / 2);
    }

    @Test
    public void adjacentCirclesOverlap() {
        DiagramLayout layout = VennLayout.layout(root("A", "B", "C"));
        int r = layout.getShapes().get(0).getWidth() / 2;
        for (int i = 0; i < 3; i++) {
            LaidOutShape a = layout.getShapes().get(i);
            LaidOutShape b = layout.getShapes().get((i + 1) % 3);
            double dist = distance(a.centerX(), a.centerY(), b.centerX(), b.centerY());
            assertTrue("adjacent circles must overlap (centre distance < 2r)", dist < 2 * r);
        }
    }

    @Test
    public void circlesStayWithinTheSlide() {
        DiagramLayout layout = VennLayout.layout(root("A", "B", "C", "D", "E", "F"));
        for (LaidOutShape s : layout.getShapes()) {
            assertTrue(s.getX() >= 0);
            assertTrue(s.getY() >= 0);
            assertTrue(s.getX() + s.getWidth() <= VennLayout.SLIDE_W);
            assertTrue(s.getY() + s.getHeight() <= VennLayout.SLIDE_H);
        }
    }

    private static double distance(int x1, int y1, int x2, int y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
