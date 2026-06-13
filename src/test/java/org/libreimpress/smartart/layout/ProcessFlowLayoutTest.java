package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;

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
        assertEquals(ProcessFlowLayout.NODE_W + ProcessFlowLayout.H_GAP, x1 - x0);
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
    public void stepsAreCentredVertically() {
        DiagramLayout layout = ProcessFlowLayout.layout(root("A", "B"));
        int expectedY = (ProcessFlowLayout.SLIDE_H - ProcessFlowLayout.NODE_H) / 2;
        for (LaidOutShape s : layout.getShapes()) {
            assertEquals(expectedY, s.getY());
        }
    }
}
