package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class CycleArrowLayoutTest {

    private static DiagramNode root(String... labels) {
        DiagramNode root = new DiagramNode("", 0);
        for (String label : labels) {
            root.addChild(new DiagramNode(label, 1));
        }
        return root;
    }

    @Test
    public void emptyInputProducesEmptyLayout() {
        DiagramLayout layout = CycleArrowLayout.layout(new DiagramNode("", 0));
        assertEquals(0, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void threeNodesProducesThreeShapesAndThreeEdges() {
        DiagramLayout layout = CycleArrowLayout.layout(root("A", "B", "C"));
        assertEquals(3, layout.getShapes().size());
        assertEquals(3, layout.getEdges().size());
    }

    @Test
    public void allShapesAreEllipses() {
        DiagramLayout layout = CycleArrowLayout.layout(root("A", "B", "C"));
        for (LaidOutShape s : layout.getShapes()) {
            assertEquals(ShapeKind.ELLIPSE, s.getKind());
        }
    }

    @Test
    public void edgesAreCurvedAndDirected() {
        DiagramLayout layout = CycleArrowLayout.layout(root("A", "B", "C"));
        for (Edge e : layout.getEdges()) {
            assertFalse("cycle-arrow edges must be curved (not straight)", e.isStraight());
            assertTrue("cycle-arrow edges must have arrowhead", e.hasArrowEnd());
        }
    }

    @Test
    public void lastEdgeWrapsAroundToFirstNode() {
        DiagramLayout layout = CycleArrowLayout.layout(root("A", "B", "C"));
        Edge last = layout.getEdges().get(2);
        assertEquals(2, last.getParent());
        assertEquals(0, last.getChild());
    }

    @Test
    public void nodesPlacedOnRingAtDefaultRadius() {
        DiagramLayout layout = CycleArrowLayout.layout(root("A", "B", "C", "D"));
        int cx = CycleArrowLayout.SLIDE_W / 2;
        int cy = CycleArrowLayout.SLIDE_H / 2;
        for (LaidOutShape s : layout.getShapes()) {
            double d = Math.sqrt(Math.pow(s.centerX() - cx, 2) + Math.pow(s.centerY() - cy, 2));
            assertEquals("node should be on the ring", CycleArrowLayout.RING_RADIUS, (int) Math.round(d));
        }
    }

    @Test
    public void manyNodesScaleRadiusToPreventOverlap() {
        // 14 circles at default RING_RADIUS=5500 would overlap (chord < CIRCLE_D + gap).
        DiagramNode root = new DiagramNode("", 0);
        for (int i = 0; i < 14; i++) {
            root.addChild(new DiagramNode("N" + i, 1));
        }
        DiagramLayout layout = CycleArrowLayout.layout(root);

        int cx = CycleArrowLayout.SLIDE_W / 2;
        int cy = CycleArrowLayout.SLIDE_H / 2;
        LaidOutShape first = layout.getShapes().get(0);
        double actualRadius = Math.sqrt(Math.pow(first.centerX() - cx, 2)
                + Math.pow(first.centerY() - cy, 2));
        assertTrue("radius with 14 nodes must exceed default RING_RADIUS",
                actualRadius > CycleArrowLayout.RING_RADIUS);

        for (int i = 0; i < 14; i++) {
            LaidOutShape a = layout.getShapes().get(i);
            LaidOutShape b = layout.getShapes().get((i + 1) % 14);
            double chord = Math.sqrt(Math.pow(b.centerX() - a.centerX(), 2)
                    + Math.pow(b.centerY() - a.centerY(), 2));
            assertTrue("adjacent circles must not overlap (chord=" + (int) chord + ")",
                    chord > CycleArrowLayout.CIRCLE_D);
        }
    }
}
