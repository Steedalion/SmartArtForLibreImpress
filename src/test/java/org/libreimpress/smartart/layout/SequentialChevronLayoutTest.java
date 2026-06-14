package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class SequentialChevronLayoutTest {

    @Test
    public void emptyInputProducesEmptyLayout() {
        DiagramLayout layout = SequentialChevronLayout.layout(new DiagramNode("", 0));
        assertEquals(0, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void singleChevronNoSubitems() {
        DiagramNode root = new DiagramNode("", 0);
        root.addChild(new DiagramNode("Step1", 1));
        DiagramLayout layout = SequentialChevronLayout.layout(root);
        assertEquals(1, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void singleChevronWithTwoSubitems() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode chevron = new DiagramNode("Step1", 1);
        root.addChild(chevron);
        chevron.addChild(new DiagramNode("Sub1", 2));
        chevron.addChild(new DiagramNode("Sub2", 2));
        DiagramLayout layout = SequentialChevronLayout.layout(root);
        assertEquals(3, layout.getShapes().size()); // 1 chevron + 2 subitems
        assertEquals(2, layout.getEdges().size()); // chevron -> sub1, chevron -> sub2
    }

    @Test
    public void threeChevronsWithVariedSubitems() {
        DiagramNode root = new DiagramNode("", 0);
        for (int i = 1; i <= 3; i++) {
            DiagramNode chevron = new DiagramNode("Step" + i, 1);
            root.addChild(chevron);
            for (int j = 1; j <= i; j++) {
                chevron.addChild(new DiagramNode("S" + i + "I" + j, 2));
            }
        }
        DiagramLayout layout = SequentialChevronLayout.layout(root);
        // 3 chevrons + 1 + 2 + 3 subitems = 9 shapes
        assertEquals(9, layout.getShapes().size());
        // 1 + 2 + 3 = 6 edges (each subitem connects to its chevron)
        assertEquals(6, layout.getEdges().size());
    }

    @Test
    public void firstShapeIsPentagonRestAreChevrons() {
        DiagramNode root = new DiagramNode("", 0);
        root.addChild(new DiagramNode("Step1", 1));
        root.addChild(new DiagramNode("Step2", 1));
        root.addChild(new DiagramNode("Step3", 1));
        DiagramLayout layout = SequentialChevronLayout.layout(root);
        // First step uses a flat-back pentagon, subsequent steps use notched chevrons
        assertEquals(ShapeKind.PENTAGON, layout.getShapes().get(0).getKind());
        assertEquals(ShapeKind.CHEVRON,  layout.getShapes().get(1).getKind());
        assertEquals(ShapeKind.CHEVRON,  layout.getShapes().get(2).getKind());
    }

    @Test
    public void subitemsAreRectangles() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode chevron = new DiagramNode("Step", 1);
        root.addChild(chevron);
        chevron.addChild(new DiagramNode("Sub", 2));
        DiagramLayout layout = SequentialChevronLayout.layout(root);
        // The subitem (second shape) should be a rectangle (default)
        assertEquals(ShapeKind.RECTANGLE, layout.getShapes().get(1).getKind());
    }

    @Test
    public void level3ChildrenPlacedBelowSubitem() {
        DiagramNode root    = new DiagramNode("", 0);
        DiagramNode chevron = new DiagramNode("Step", 1);
        DiagramNode sub     = new DiagramNode("Sub", 2);
        DiagramNode child   = new DiagramNode("Child", 3);
        sub.addChild(child);
        chevron.addChild(sub);
        root.addChild(chevron);

        DiagramLayout layout = SequentialChevronLayout.layout(root);
        assertEquals(3, layout.getShapes().size()); // chevron + sub + child
        assertEquals(2, layout.getEdges().size());  // chevron→sub + sub→child

        LaidOutShape subShape   = layout.getShapes().get(1);
        LaidOutShape childShape = layout.getShapes().get(2);
        assertTrue("level-3 child should be below level-2 sub-item",
                childShape.getY() > subShape.getY() + subShape.getHeight());
        assertEquals("child should be centred on sub-item X",
                subShape.centerX(), childShape.centerX());
    }

    @Test
    public void level3EdgeUsesBottomToTopGlue() {
        DiagramNode root    = new DiagramNode("", 0);
        DiagramNode chevron = new DiagramNode("Step", 1);
        DiagramNode sub     = new DiagramNode("Sub", 2);
        sub.addChild(new DiagramNode("Child", 3));
        chevron.addChild(sub);
        root.addChild(chevron);

        DiagramLayout layout = SequentialChevronLayout.layout(root);
        Edge childEdge = layout.getEdges().get(1); // second edge = sub→child
        assertEquals("start glue should be bottom (2)", 2, childEdge.getStartGlue());
        assertEquals("end glue should be top (0)", 0, childEdge.getEndGlue());
    }

    @Test
    public void manyChevronsScaleDownToFitSlide() {
        // 8 chevrons at default width overflow; layout must shrink them to fit.
        DiagramNode root = new DiagramNode("", 0);
        for (int i = 0; i < 8; i++) {
            root.addChild(new DiagramNode("C" + i, 1));
        }
        DiagramLayout layout = SequentialChevronLayout.layout(root);
        int rightLimit = SequentialChevronLayout.SLIDE_W - SequentialChevronLayout.MARGIN_X;
        for (LaidOutShape s : layout.getShapes()) {
            if (s.getLevel() == 1) {
                assertTrue("chevron right edge must stay within slide",
                        s.getX() + s.getWidth() <= rightLimit);
            }
        }
    }

    @Test
    public void subitemsSizedSmaller() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode chevron = new DiagramNode("Step", 1);
        root.addChild(chevron);
        chevron.addChild(new DiagramNode("Sub", 2));
        DiagramLayout layout = SequentialChevronLayout.layout(root);
        LaidOutShape chevronShape = layout.getShapes().get(0);
        LaidOutShape subitemShape = layout.getShapes().get(1);
        // Subitem (level-2) should be smaller than chevron (level-1)
        assertEquals(chevronShape.getWidth() - 30, subitemShape.getWidth());
        assertEquals(chevronShape.getHeight() - 30, subitemShape.getHeight());
    }
}
