package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class TimelineLayoutTest {

    private static DiagramNode root(String... labels) {
        DiagramNode root = new DiagramNode("", 0);
        for (String label : labels) {
            root.addChild(new DiagramNode(label, 1));
        }
        return root;
    }

    private static List<LaidOutShape> markers(DiagramLayout layout) {
        return layout.getShapes().stream()
                .filter(s -> s.getKind() == ShapeKind.TIMELINE_MARKER)
                .collect(Collectors.toList());
    }

    private static List<LaidOutShape> labels(DiagramLayout layout) {
        return layout.getShapes().stream()
                .filter(s -> s.getKind() == ShapeKind.RECTANGLE && !s.getText().isEmpty())
                .collect(Collectors.toList());
    }

    @Test
    public void emptyInputProducesEmptyLayout() {
        DiagramLayout layout = TimelineLayout.layout(new DiagramNode("", 0));
        assertEquals(0, layout.getShapes().size());
    }

    @Test
    public void spinePlusMarkerAndLabelPerEvent() {
        DiagramLayout layout = TimelineLayout.layout(root("A", "B", "C"));
        // 1 spine + 3 markers + 3 labels
        assertEquals(7, layout.getShapes().size());
        assertEquals(3, markers(layout).size());
        assertEquals(3, labels(layout).size());
        assertEquals("one marker→label connector per event",
                3, layout.getEdges().size());
    }

    @Test
    public void spineIsFirstSoEverythingDrawsOnTopOfIt() {
        DiagramLayout layout = TimelineLayout.layout(root("A", "B"));
        LaidOutShape spine = layout.getShapes().get(0);
        assertEquals(ShapeKind.RECTANGLE, spine.getKind());
        assertTrue(spine.getText().isEmpty());
        assertEquals(TimelineLayout.SPINE_Y, spine.centerY());
    }

    @Test
    public void markersSitOnTheSpineInListOrder() {
        DiagramLayout layout = TimelineLayout.layout(root("A", "B", "C", "D"));
        List<LaidOutShape> ms = markers(layout);
        int prevX = Integer.MIN_VALUE;
        for (LaidOutShape m : ms) {
            assertEquals(TimelineLayout.SPINE_Y, m.centerY());
            assertTrue("markers must advance left→right", m.centerX() > prevX);
            prevX = m.centerX();
        }
        assertEquals(TimelineLayout.MARGIN_X, ms.get(0).centerX());
        assertEquals(TimelineLayout.SLIDE_W - TimelineLayout.MARGIN_X,
                ms.get(ms.size() - 1).centerX());
    }

    @Test
    public void labelsAlternateAboveAndBelowTheSpine() {
        DiagramLayout layout = TimelineLayout.layout(root("A", "B", "C"));
        List<LaidOutShape> ls = labels(layout);
        assertTrue("first label above", ls.get(0).centerY() < TimelineLayout.SPINE_Y);
        assertTrue("second label below", ls.get(1).centerY() > TimelineLayout.SPINE_Y);
        assertTrue("third label above", ls.get(2).centerY() < TimelineLayout.SPINE_Y);
    }

    @Test
    public void singleEventIsCentred() {
        DiagramLayout layout = TimelineLayout.layout(root("Only"));
        assertEquals(TimelineLayout.SLIDE_W / 2, markers(layout).get(0).centerX());
    }

    @Test
    public void level2ChildrenBecomeBulletsInTheLabel() {
        DiagramNode root = root("Kickoff", "Build");
        root.getChildren().get(0).addChild(new DiagramNode("Charter", 2));
        DiagramLayout layout = TimelineLayout.layout(root);
        assertEquals("no extra shapes for children", 5, layout.getShapes().size());
        String text = labels(layout).get(0).getText();
        assertTrue(text.startsWith("Kickoff"));
        assertTrue(text.contains("• Charter"));
    }

    @Test
    public void manyEventsLabelsDoNotOverlapOnTheSameSide() {
        DiagramLayout layout = TimelineLayout.layout(
                root("1", "2", "3", "4", "5", "6", "7", "8"));
        List<LaidOutShape> ls = labels(layout);
        for (int i = 2; i < ls.size(); i += 2) { // same (upper) side: 0,2,4…
            assertTrue("label " + i + " overlaps label " + (i - 2),
                    ls.get(i).getX() >= ls.get(i - 2).getX() + ls.get(i - 2).getWidth());
        }
    }
}
