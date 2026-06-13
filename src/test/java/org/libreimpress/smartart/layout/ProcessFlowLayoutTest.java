package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class ProcessFlowLayoutTest {

    private static DiagramNode root(String... labels) {
        DiagramNode root = new DiagramNode("", 0);
        for (String label : labels) {
            root.addChild(new DiagramNode(label, 1));
        }
        return root;
    }

    @Test
    public void emptyInputProducesEmptyLayout() {
        DiagramLayout layout = ProcessFlowLayout.layout(new DiagramNode("", 0));
        assertEquals(0, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void singleStepOneShapeNoEdges() {
        DiagramLayout layout = ProcessFlowLayout.layout(root("Only"));
        assertEquals(1, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void threeStepsThreeShapesTwoSequentialEdges() {
        DiagramLayout layout = ProcessFlowLayout.layout(root("A", "B", "C"));
        assertEquals(3, layout.getShapes().size());
        assertEquals(2, layout.getEdges().size());

        assertEquals(0, layout.getEdges().get(0).getParent());
        assertEquals(1, layout.getEdges().get(0).getChild());
        assertEquals(1, layout.getEdges().get(1).getParent());
        assertEquals(2, layout.getEdges().get(1).getChild());
    }

    @Test
    public void allStepsShareTheSameY() {
        DiagramLayout layout = ProcessFlowLayout.layout(root("A", "B", "C", "D"));
        int y = layout.getShapes().get(0).getY();
        for (LaidOutShape s : layout.getShapes()) {
            assertEquals(y, s.getY());
        }
    }

    @Test
    public void stepsAreEvenlySpaced() {
        DiagramLayout layout = ProcessFlowLayout.layout(root("A", "B", "C"));
        int x0 = layout.getShapes().get(0).getX();
        int x1 = layout.getShapes().get(1).getX();
        int x2 = layout.getShapes().get(2).getX();
        assertEquals(x1 - x0, x2 - x1);
        assertEquals(ProcessFlowLayout.BASE_NODE_W + ProcessFlowLayout.H_GAP, x1 - x0);
    }

    @Test
    public void singleStepIsCentredHorizontally() {
        DiagramLayout layout = ProcessFlowLayout.layout(root("Solo"));
        LaidOutShape step = layout.getShapes().get(0);
        int expectedCX = ProcessFlowLayout.SLIDE_W / 2;
        assertEquals(expectedCX, step.centerX());
    }

    @Test
    public void sequenceIsCentredHorizontally() {
        DiagramLayout layout = ProcessFlowLayout.layout(root("A", "B", "C"));
        LaidOutShape first = layout.getShapes().get(0);
        LaidOutShape last  = layout.getShapes().get(2);
        // midpoint of the sequence aligns with slide centre
        int seqMidX = (first.centerX() + last.centerX()) / 2;
        assertEquals(ProcessFlowLayout.SLIDE_W / 2, seqMidX);
    }

    @Test
    public void edgesUseRightToLeftGluePoints() {
        DiagramLayout layout = ProcessFlowLayout.layout(root("A", "B", "C"));
        for (Edge e : layout.getEdges()) {
            assertEquals("start glue should be right-side (1)", 1, e.getStartGlue());
            assertEquals("end glue should be left-side (3)", 3, e.getEndGlue());
        }
    }

    @Test
    public void stepEdgesHaveArrowAtEnd() {
        DiagramLayout layout = ProcessFlowLayout.layout(root("A", "B", "C"));
        for (Edge e : layout.getEdges()) {
            assertTrue("step connectors should have an arrowhead", e.hasArrowEnd());
        }
    }

    @Test
    public void stepsAreAtTopMargin() {
        DiagramLayout layout = ProcessFlowLayout.layout(root("A", "B"));
        for (LaidOutShape s : layout.getShapes()) {
            assertEquals(ProcessFlowLayout.MARGIN_Y, s.getY());
        }
    }

    @Test
    public void childrenArePlacedBelowParentStep() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode step = new DiagramNode("Step", 1);
        step.addChild(new DiagramNode("Sub1", 2));
        step.addChild(new DiagramNode("Sub2", 2));
        root.addChild(step);

        DiagramLayout layout = ProcessFlowLayout.layout(root);
        assertEquals(3, layout.getShapes().size()); // 1 step + 2 children

        LaidOutShape s = layout.getShapes().get(0); // the step
        LaidOutShape c1 = layout.getShapes().get(1);
        LaidOutShape c2 = layout.getShapes().get(2);

        assertTrue("child 1 should be below step", c1.getY() > s.getY());
        assertTrue("child 2 should be below child 1", c2.getY() > c1.getY());
        assertEquals("children centred under step", s.centerX(), c1.centerX());
        assertEquals("children centred under step", s.centerX(), c2.centerX());
    }

    @Test
    public void parentToChildEdgesArePresent() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode step = new DiagramNode("Step", 1);
        step.addChild(new DiagramNode("Sub", 2));
        root.addChild(step);

        DiagramLayout layout = ProcessFlowLayout.layout(root);
        // 1 step-to-step edge (none here, single step) + 1 parent→child edge
        assertEquals(1, layout.getEdges().size());
        assertEquals(0, layout.getEdges().get(0).getParent()); // step
        assertEquals(1, layout.getEdges().get(0).getChild());  // child
    }

    @Test
    public void childEdgesUseBottomToTopGluePoints() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode step = new DiagramNode("Step", 1);
        step.addChild(new DiagramNode("Sub", 2));
        root.addChild(step);

        DiagramLayout layout = ProcessFlowLayout.layout(root);
        Edge childEdge = layout.getEdges().get(0);
        assertEquals("parent glue should be bottom (2)", 2, childEdge.getStartGlue());
        assertEquals("child glue should be top (0)", 0, childEdge.getEndGlue());
    }
}
