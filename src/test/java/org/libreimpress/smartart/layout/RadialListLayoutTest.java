package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class RadialListLayoutTest {

    /** One hub with the given satellite labels. */
    private static DiagramNode hub(String hubLabel, String... items) {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode hub = new DiagramNode(hubLabel, 1);
        root.addChild(hub);
        for (String item : items) {
            hub.addChild(new DiagramNode(item, 2));
        }
        return root;
    }

    private static List<LaidOutShape> satellites(DiagramLayout layout) {
        return layout.getShapes().stream()
                .filter(s -> s.getKind() == ShapeKind.RECTANGLE)
                .collect(Collectors.toList());
    }

    @Test
    public void emptyInputProducesEmptyLayout() {
        DiagramLayout layout = RadialListLayout.layout(new DiagramNode("", 0));
        assertEquals(0, layout.getShapes().size());
    }

    @Test
    public void hubCirclePlusRectanglePerItemWithConnectors() {
        DiagramLayout layout = RadialListLayout.layout(hub("Hub", "A", "B", "C"));
        assertEquals(4, layout.getShapes().size());
        assertEquals(ShapeKind.ELLIPSE, layout.getShapes().get(0).getKind());
        assertEquals(3, satellites(layout).size());
        assertEquals(3, layout.getEdges().size());
        for (Edge e : layout.getEdges()) {
            assertEquals("all connectors start at the hub", 0, e.getParent());
            assertTrue(e.isStraight());
        }
    }

    @Test
    public void hubIsCentredOnTheSlide() {
        DiagramLayout layout = RadialListLayout.layout(hub("Hub", "A"));
        LaidOutShape hub = layout.getShapes().get(0);
        assertEquals(RadialListLayout.SLIDE_W / 2, hub.centerX());
        assertEquals(RadialListLayout.CENTER_Y, hub.centerY());
    }

    @Test
    public void satellitesAreEquidistantFromTheHub() {
        DiagramLayout layout = RadialListLayout.layout(hub("Hub", "A", "B", "C", "D"));
        LaidOutShape hub = layout.getShapes().get(0);
        Long expected = null;
        for (LaidOutShape s : satellites(layout)) {
            long dx = s.centerX() - hub.centerX();
            long dy = s.centerY() - hub.centerY();
            long d2 = dx * dx + dy * dy;
            if (expected == null) {
                expected = d2;
            } else {
                // Integer rounding: allow ~1% deviation on the squared distance.
                assertTrue(Math.abs(d2 - expected) < expected / 50);
            }
        }
    }

    @Test
    public void firstSatelliteStartsAtTheTop() {
        DiagramLayout layout = RadialListLayout.layout(hub("Hub", "A", "B", "C"));
        LaidOutShape first = satellites(layout).get(0);
        LaidOutShape hub = layout.getShapes().get(0);
        assertEquals(hub.centerX(), first.centerX());
        assertTrue(first.centerY() < hub.centerY());
    }

    @Test
    public void manySatellitesPushTheRingOutward() {
        DiagramLayout few = RadialListLayout.layout(hub("Hub", "A", "B", "C"));
        DiagramLayout many = RadialListLayout.layout(
                hub("Hub", "A", "B", "C", "D", "E", "F", "G", "H"));
        LaidOutShape hubFew = few.getShapes().get(0);
        LaidOutShape topFew = satellites(few).get(0);
        LaidOutShape hubMany = many.getShapes().get(0);
        LaidOutShape topMany = satellites(many).get(0);
        assertTrue("crowded ring must use a larger radius",
                hubMany.centerY() - topMany.centerY()
                        > hubFew.centerY() - topFew.centerY());
    }

    @Test
    public void level3ChildrenBecomeBulletsInsideTheSatellite() {
        DiagramNode root = hub("Wellness", "Diet", "Sleep");
        root.getChildren().get(0).getChildren().get(0)
                .addChild(new DiagramNode("Vegetables", 3));
        DiagramLayout layout = RadialListLayout.layout(root);
        assertEquals("no extra shapes for level-3", 3, layout.getShapes().size());
        String text = satellites(layout).get(0).getText();
        assertTrue(text.startsWith("Diet"));
        assertTrue(text.contains("• Vegetables"));
    }

    @Test
    public void multipleHubsGetTheirOwnRings() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode h1 = new DiagramNode("One", 1);
        DiagramNode h2 = new DiagramNode("Two", 1);
        h1.addChild(new DiagramNode("a", 2));
        h2.addChild(new DiagramNode("b", 2));
        root.addChild(h1);
        root.addChild(h2);
        DiagramLayout layout = RadialListLayout.layout(root);
        assertEquals(4, layout.getShapes().size());
        List<LaidOutShape> hubs = layout.getShapes().stream()
                .filter(s -> s.getKind() == ShapeKind.ELLIPSE)
                .collect(Collectors.toList());
        assertEquals(2, hubs.size());
        assertTrue(hubs.get(0).centerX() < hubs.get(1).centerX());
    }
}
