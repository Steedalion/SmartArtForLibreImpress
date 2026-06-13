package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class CycleLayoutTest {

    private static DiagramNode root(String... labels) {
        DiagramNode root = new DiagramNode("", 0);
        for (String label : labels) {
            root.addChild(new DiagramNode(label, 1));
        }
        return root;
    }

    @Test
    public void emptyInputProducesEmptyLayout() {
        DiagramLayout layout = CycleLayout.layout(new DiagramNode("", 0));
        assertEquals(0, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void fourNodesProducesFourShapesAndFourEdges() {
        DiagramLayout layout = CycleLayout.layout(root("A", "B", "C", "D"));
        assertEquals(4, layout.getShapes().size());
        assertEquals(4, layout.getEdges().size());
    }

    @Test
    public void threeNodesProducesThreeEdges() {
        DiagramLayout layout = CycleLayout.layout(root("A", "B", "C"));
        assertEquals(3, layout.getShapes().size());
        assertEquals(3, layout.getEdges().size());
    }

    @Test
    public void nodesArePlacedOnTheRing() {
        DiagramLayout layout = CycleLayout.layout(root("A", "B", "C", "D"));
        int cx = CycleLayout.SLIDE_W / 2;
        int cy = CycleLayout.SLIDE_H / 2;
        for (LaidOutShape s : layout.getShapes()) {
            double d = distance(s.centerX(), s.centerY(), cx, cy);
            assertEquals("node should be on the ring",
                    CycleLayout.RING_RADIUS, (int) Math.round(d));
        }
    }

    @Test
    public void firstNodeIsAtTop() {
        DiagramLayout layout = CycleLayout.layout(root("A", "B", "C"));
        LaidOutShape first = layout.getShapes().get(0);
        int cx = CycleLayout.SLIDE_W / 2;
        int cy = CycleLayout.SLIDE_H / 2;
        // At −90° the node is directly above the centre.
        assertEquals("first node should be above centre", first.centerX(), cx, 1);
        assertTrue("first node should be above centre",
                first.centerY() < cy);
    }

    @Test
    public void edgesAreDirectedWithArrowheads() {
        DiagramLayout layout = CycleLayout.layout(root("A", "B", "C"));
        for (Edge e : layout.getEdges()) {
            assertTrue("all cycle edges must have arrowEnd", e.hasArrowEnd());
        }
    }

    @Test
    public void edgesAreStraightLines() {
        DiagramLayout layout = CycleLayout.layout(root("A", "B", "C"));
        for (Edge e : layout.getEdges()) {
            assertTrue("all cycle edges must be straight", e.isStraight());
        }
    }

    @Test
    public void lastEdgeWrapsAroundToFirstNode() {
        DiagramLayout layout = CycleLayout.layout(root("A", "B", "C"));
        // Shapes: 0=A, 1=B, 2=C. Last edge must be C→A (parent=2, child=0).
        Edge last = layout.getEdges().get(2);
        assertEquals(2, last.getParent());
        assertEquals(0, last.getChild());
    }

    @Test
    public void edgesConnectConsecutiveNodes() {
        DiagramLayout layout = CycleLayout.layout(root("A", "B", "C", "D"));
        for (int i = 0; i < 4; i++) {
            Edge e = layout.getEdges().get(i);
            assertEquals(i, e.getParent());
            assertEquals((i + 1) % 4, e.getChild());
        }
    }

    @Test
    public void level2ChildrenAreIgnored() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode a = new DiagramNode("A", 1);
        a.addChild(new DiagramNode("A1", 2));
        root.addChild(a);
        root.addChild(new DiagramNode("B", 1));
        root.addChild(new DiagramNode("C", 1));

        DiagramLayout layout = CycleLayout.layout(root);
        // Sub-items are dropped; only the 3 cycle nodes and 3 cycle edges appear.
        assertEquals(3, layout.getShapes().size());
        assertEquals(3, layout.getEdges().size());
    }

    private static double distance(int x1, int y1, int x2, int y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
