package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class TargetLayoutTest {

    private static DiagramNode root(String... labels) {
        DiagramNode root = new DiagramNode("", 0);
        for (String label : labels) {
            root.addChild(new DiagramNode(label, 1));
        }
        return root;
    }

    @Test
    public void emptyInputProducesEmptyLayout() {
        DiagramLayout layout = TargetLayout.layout(new DiagramNode("", 0));
        assertEquals(0, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void oneRingPerLevel1NodeAndNoEdges() {
        DiagramLayout layout = TargetLayout.layout(root("A", "B", "C", "D"));
        assertEquals(4, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void allRingsAreConcentricCircles() {
        DiagramLayout layout = TargetLayout.layout(root("A", "B", "C"));
        for (LaidOutShape s : layout.getShapes()) {
            assertEquals(ShapeKind.TARGET_RING, s.getKind());
            assertEquals("rings must be circular", s.getWidth(), s.getHeight());
            assertEquals(TargetLayout.SLIDE_W / 2, s.centerX());
            assertEquals(TargetLayout.SLIDE_H / 2, s.centerY());
        }
    }

    @Test
    public void firstItemIsOutermostAndDiametersStrictlyShrink() {
        DiagramLayout layout = TargetLayout.layout(root("Outer", "Mid", "Inner"));
        assertEquals("Outer", layout.getShapes().get(0).getText());
        assertEquals("Inner", layout.getShapes().get(2).getText());
        assertEquals(TargetLayout.MAX_D, layout.getShapes().get(0).getWidth());
        assertEquals(TargetLayout.MIN_D, layout.getShapes().get(2).getWidth());
        for (int i = 1; i < layout.getShapes().size(); i++) {
            assertTrue("ring " + i + " must be smaller than ring " + (i - 1),
                    layout.getShapes().get(i).getWidth()
                            < layout.getShapes().get(i - 1).getWidth());
        }
    }

    @Test
    public void singleItemUsesTheStandaloneDiameter() {
        DiagramLayout layout = TargetLayout.layout(root("Only"));
        assertEquals(1, layout.getShapes().size());
        assertEquals(TargetLayout.SINGLE_D, layout.getShapes().get(0).getWidth());
    }

    @Test
    public void manyRingsStillShrinkMonotonically() {
        DiagramLayout layout = TargetLayout.layout(
                root("1", "2", "3", "4", "5", "6", "7"));
        for (int i = 1; i < 7; i++) {
            assertTrue(layout.getShapes().get(i).getWidth()
                    < layout.getShapes().get(i - 1).getWidth());
        }
    }

    @Test
    public void deeperDescendantsAreIgnored() {
        DiagramNode root = root("A", "B");
        root.getChildren().get(0).addChild(new DiagramNode("child", 2));
        DiagramLayout layout = TargetLayout.layout(root);
        assertEquals(2, layout.getShapes().size());
    }

    @Test
    public void ringsFitOnTheSlide() {
        DiagramLayout layout = TargetLayout.layout(root("A", "B", "C", "D", "E"));
        LaidOutShape outer = layout.getShapes().get(0);
        assertTrue(outer.getY() >= 0);
        assertTrue(outer.getY() + outer.getHeight() <= TargetLayout.SLIDE_H);
    }
}
