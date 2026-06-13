package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;
import org.libreimpress.smartart.layout.ShapeKind;

public class HubAndSpokeLayoutTest {

    private static DiagramNode root(String hubText, String... spokeTexts) {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode hub = new DiagramNode(hubText, 1);
        root.addChild(hub);
        for (String s : spokeTexts) {
            hub.addChild(new DiagramNode(s, 2));
        }
        return root;
    }

    @Test
    public void emptyInputProducesEmptyLayout() {
        DiagramLayout layout = HubAndSpokeLayout.layout(new DiagramNode("", 0));
        assertEquals(0, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void hubOnlyNoSpokesOneShapeNoEdges() {
        DiagramLayout layout = HubAndSpokeLayout.layout(root("Hub"));
        assertEquals(1, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void hubWithFourSpokesProducesFiveShapesFourEdges() {
        DiagramLayout layout = HubAndSpokeLayout.layout(
                root("Center", "N", "E", "S", "W"));
        assertEquals(5, layout.getShapes().size());
        assertEquals(4, layout.getEdges().size());
        // every edge originates at shape 0 (the hub)
        for (Edge e : layout.getEdges()) {
            assertEquals(0, e.getParent());
        }
    }

    @Test
    public void hubIsPlacedAtSlideCenter() {
        DiagramLayout layout = HubAndSpokeLayout.layout(root("Hub", "A"));
        LaidOutShape hub = layout.getShapes().get(0);
        assertEquals(HubAndSpokeLayout.CENTER_X, hub.centerX());
        assertEquals(HubAndSpokeLayout.CENTER_Y, hub.centerY());
    }

    @Test
    public void singleSpokeSitsDirectlyAboveHub() {
        // one spoke → angle = -π/2 → directly above centre
        DiagramLayout layout = HubAndSpokeLayout.layout(root("Hub", "Top"));
        LaidOutShape spoke = layout.getShapes().get(1);
        assertEquals(HubAndSpokeLayout.CENTER_X, spoke.centerX());
        assertEquals(HubAndSpokeLayout.CENTER_Y - HubAndSpokeLayout.SPOKE_RADIUS,
                spoke.centerY());
    }

    @Test
    public void fourSpokesAreAtCardinalPoints() {
        // 4 spokes: top, right, bottom, left
        DiagramLayout layout = HubAndSpokeLayout.layout(
                root("Hub", "N", "E", "S", "W"));
        int cx = HubAndSpokeLayout.CENTER_X;
        int cy = HubAndSpokeLayout.CENTER_Y;
        int r  = HubAndSpokeLayout.SPOKE_RADIUS;

        LaidOutShape top    = layout.getShapes().get(1);
        LaidOutShape right  = layout.getShapes().get(2);
        LaidOutShape bottom = layout.getShapes().get(3);
        LaidOutShape left   = layout.getShapes().get(4);

        assertEquals(cx,     top.centerX());
        assertEquals(cy - r, top.centerY());

        assertEquals(cx + r, right.centerX());
        assertEquals(cy,     right.centerY());

        assertEquals(cx,     bottom.centerX());
        assertEquals(cy + r, bottom.centerY());

        assertEquals(cx - r, left.centerX());
        assertEquals(cy,     left.centerY());
    }

    @Test
    public void extraLevel1SiblingsBecomeSpokesToo() {
        // root → Hub(1), ExtraA(1), ExtraB(1)  (flat list, no level-2 children)
        DiagramNode root = new DiagramNode("", 0);
        root.addChild(new DiagramNode("Hub", 1));
        root.addChild(new DiagramNode("ExtraA", 1));
        root.addChild(new DiagramNode("ExtraB", 1));

        DiagramLayout layout = HubAndSpokeLayout.layout(root);
        assertEquals(3, layout.getShapes().size()); // hub + 2 spokes
        assertEquals(2, layout.getEdges().size());
        for (Edge e : layout.getEdges()) {
            assertEquals(0, e.getParent());
        }
    }

    @Test
    public void allShapesAreEllipses() {
        DiagramLayout layout = HubAndSpokeLayout.layout(root("Hub", "A", "B", "C"));
        for (LaidOutShape s : layout.getShapes()) {
            assertEquals(ShapeKind.ELLIPSE, s.getKind());
        }
    }

    @Test
    public void allSpokesAreEquidistantFromHub() {
        DiagramLayout layout = HubAndSpokeLayout.layout(
                root("Hub", "A", "B", "C", "D", "E"));
        LaidOutShape hub = layout.getShapes().get(0);
        int hubCX = hub.centerX();
        int hubCY = hub.centerY();
        int r = HubAndSpokeLayout.SPOKE_RADIUS;

        for (int i = 1; i < layout.getShapes().size(); i++) {
            LaidOutShape spoke = layout.getShapes().get(i);
            double dx = spoke.centerX() - hubCX;
            double dy = spoke.centerY() - hubCY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            // accept ±1 (1/100 mm rounding from integer Math.round)
            assertTrue("spoke " + i + " distance " + dist + " not near " + r,
                    Math.abs(dist - r) <= 1.0);
        }
    }
}
