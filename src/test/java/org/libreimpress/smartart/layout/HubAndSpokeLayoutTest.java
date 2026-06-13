package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;
import org.libreimpress.smartart.layout.ShapeKind;

public class HubAndSpokeLayoutTest {

    @Test
    public void emptyInputProducesEmptyLayout() {
        DiagramLayout layout = HubAndSpokeLayout.layout(new DiagramNode("", 0));
        assertEquals(0, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void singleHubNoSpokesOneShape() {
        DiagramNode root = new DiagramNode("", 0);
        root.addChild(new DiagramNode("Hub", 1));
        DiagramLayout layout = HubAndSpokeLayout.layout(root);
        assertEquals(1, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void singleHubWithThreeSpokes() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode hub = new DiagramNode("Center", 1);
        root.addChild(hub);
        hub.addChild(new DiagramNode("A", 2));
        hub.addChild(new DiagramNode("B", 2));
        hub.addChild(new DiagramNode("C", 2));
        DiagramLayout layout = HubAndSpokeLayout.layout(root);
        assertEquals(4, layout.getShapes().size()); // 1 hub + 3 spokes
        assertEquals(3, layout.getEdges().size()); // hub→A, hub→B, hub→C
    }

    @Test
    public void twoHubsPlacedLeftToRight() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode hub1 = new DiagramNode("Hub1", 1);
        DiagramNode hub2 = new DiagramNode("Hub2", 1);
        root.addChild(hub1);
        root.addChild(hub2);
        hub1.addChild(new DiagramNode("H1A", 2));
        hub2.addChild(new DiagramNode("H2A", 2));

        DiagramLayout layout = HubAndSpokeLayout.layout(root);
        assertEquals(4, layout.getShapes().size()); // 2 hubs + 2 spokes
        assertEquals(2, layout.getEdges().size()); // hub1→spoke1, hub2→spoke2

        LaidOutShape shape0 = layout.getShapes().get(0);
        LaidOutShape shape2 = layout.getShapes().get(2);
        assertTrue("hub1 should be left of hub2",
                shape0.centerX() < shape2.centerX());
    }

    @Test
    public void allShapesAreEllipses() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode hub = new DiagramNode("Hub", 1);
        root.addChild(hub);
        hub.addChild(new DiagramNode("A", 2));
        hub.addChild(new DiagramNode("B", 2));
        DiagramLayout layout = HubAndSpokeLayout.layout(root);
        for (LaidOutShape s : layout.getShapes()) {
            assertEquals(ShapeKind.ELLIPSE, s.getKind());
        }
    }

    @Test
    public void allSpokesAreEquidistantFromTheirHub() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode hub = new DiagramNode("Center", 1);
        root.addChild(hub);
        for (String name : new String[]{"A", "B", "C", "D"}) {
            hub.addChild(new DiagramNode(name, 2));
        }

        DiagramLayout layout = HubAndSpokeLayout.layout(root);
        LaidOutShape hubShape = layout.getShapes().get(0);
        int hubCX = hubShape.centerX();
        int hubCY = hubShape.centerY();
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

    @Test
    public void threeHubsWithMultipleSpokes() {
        DiagramNode root = new DiagramNode("", 0);
        for (String hubName : new String[]{"H1", "H2", "H3"}) {
            DiagramNode hub = new DiagramNode(hubName, 1);
            root.addChild(hub);
            for (String spokeName : new String[]{"A", "B"}) {
                hub.addChild(new DiagramNode(hubName + spokeName, 2));
            }
        }
        DiagramLayout layout = HubAndSpokeLayout.layout(root);
        assertEquals(9, layout.getShapes().size()); // 3 hubs + 6 spokes
        assertEquals(6, layout.getEdges().size()); // each hub has 2 spokes
    }
}
