package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class CycleBlockLayoutTest {

    private static DiagramNode root(String... labels) {
        DiagramNode root = new DiagramNode("", 0);
        for (String label : labels) {
            root.addChild(new DiagramNode(label, 1));
        }
        return root;
    }

    @Test
    public void emptyInputProducesEmptyLayout() {
        DiagramLayout layout = CycleBlockLayout.layout(new DiagramNode("", 0));
        assertEquals(0, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void fourNodesProducesFourRectanglesAndFourArrows() {
        DiagramLayout layout = CycleBlockLayout.layout(root("A", "B", "C", "D"));
        long rects  = layout.getShapes().stream().filter(s -> s.getKind() == ShapeKind.RECTANGLE).count();
        long arrows = layout.getShapes().stream().filter(s -> s.getKind() == ShapeKind.BLOCK_ARROW).count();
        assertEquals(4, rects);
        assertEquals(4, arrows);
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void nodeShapesAreRectangles() {
        DiagramLayout layout = CycleBlockLayout.layout(root("A", "B", "C"));
        for (int i = 0; i < 3; i++) {
            assertEquals(ShapeKind.RECTANGLE, layout.getShapes().get(i).getKind());
        }
    }

    @Test
    public void arrowShapesAreBlockArrows() {
        DiagramLayout layout = CycleBlockLayout.layout(root("A", "B", "C"));
        for (int i = 3; i < 6; i++) {
            assertEquals(ShapeKind.BLOCK_ARROW, layout.getShapes().get(i).getKind());
        }
    }

    @Test
    public void arrowsPointTowardNextNode() {
        DiagramLayout layout = CycleBlockLayout.layout(root("A", "B", "C", "D"));
        int n = 4;
        for (int i = 0; i < n; i++) {
            LaidOutShape from = layout.getShapes().get(i);
            LaidOutShape to   = layout.getShapes().get((i + 1) % n);
            LaidOutShape arr  = layout.getShapes().get(n + i);

            double dx = to.centerX() - from.centerX();
            double dy = to.centerY() - from.centerY();
            // RotateAngle is counter-clockwise; negate dy to convert screen coords.
            int expected = (int) Math.round(Math.atan2(-dy, dx) * 18000.0 / Math.PI);
            if (expected < 0) expected += 36000;

            assertEquals("arrow " + i + " should point toward next node",
                    expected, arr.getRotateAngle100());
        }
    }

    @Test
    public void arrowCentresAreBetweenAdjacentRectangleCentres() {
        DiagramLayout layout = CycleBlockLayout.layout(root("A", "B", "C", "D"));
        int n = 4;
        for (int i = 0; i < n; i++) {
            LaidOutShape r1  = layout.getShapes().get(i);
            LaidOutShape r2  = layout.getShapes().get((i + 1) % n);
            LaidOutShape arr = layout.getShapes().get(n + i);
            int expectedCX = (r1.centerX() + r2.centerX()) / 2;
            int expectedCY = (r1.centerY() + r2.centerY()) / 2;
            assertEquals("arrow centre X", expectedCX, arr.centerX());
            assertEquals("arrow centre Y", expectedCY, arr.centerY());
        }
    }

    @Test
    public void allRectanglesAreEquidistantFromSlideCentre() {
        DiagramLayout layout = CycleBlockLayout.layout(root("A", "B", "C", "D", "E"));
        int cx = CycleBlockLayout.SLIDE_W / 2;
        int cy = CycleBlockLayout.SLIDE_H / 2;
        int r  = CycleBlockLayout.RING_RADIUS;
        for (int i = 0; i < 5; i++) {
            LaidOutShape s = layout.getShapes().get(i);
            double dx = s.centerX() - cx;
            double dy = s.centerY() - cy;
            double dist = Math.sqrt(dx * dx + dy * dy);
            assertTrue("rectangle " + i + " should be at ring radius ±1",
                    Math.abs(dist - r) <= 1.0);
        }
    }

    @Test
    public void arrowLabelsAreEmpty() {
        DiagramLayout layout = CycleBlockLayout.layout(root("A", "B", "C"));
        for (int i = 3; i < 6; i++) {
            assertEquals("", layout.getShapes().get(i).getText());
        }
    }
}
