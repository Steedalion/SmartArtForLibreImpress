package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class MatrixLayoutTest {

    private static DiagramNode root(String... labels) {
        DiagramNode root = new DiagramNode("", 0);
        for (String label : labels) {
            root.addChild(new DiagramNode(label, 1));
        }
        return root;
    }

    @Test
    public void emptyInputProducesEmptyLayout() {
        DiagramLayout layout = MatrixLayout.layout(new DiagramNode("", 0));
        assertEquals(0, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void fourNodesProduceFourCellsAndNoEdges() {
        DiagramLayout layout = MatrixLayout.layout(root("A", "B", "C", "D"));
        assertEquals(4, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
        for (LaidOutShape s : layout.getShapes()) {
            assertEquals(ShapeKind.MATRIX_CELL, s.getKind());
        }
    }

    @Test
    public void extraNodesBeyondFourAreIgnored() {
        DiagramLayout layout = MatrixLayout.layout(root("A", "B", "C", "D", "E", "F"));
        assertEquals(4, layout.getShapes().size());
    }

    @Test
    public void cellsFormTwoByTwoGrid() {
        DiagramLayout layout = MatrixLayout.layout(root("A", "B", "C", "D"));
        LaidOutShape tl = layout.getShapes().get(0);
        LaidOutShape tr = layout.getShapes().get(1);
        LaidOutShape bl = layout.getShapes().get(2);
        LaidOutShape br = layout.getShapes().get(3);
        // Top row shares a Y; bottom row shares a Y below it.
        assertEquals(tl.getY(), tr.getY());
        assertEquals(bl.getY(), br.getY());
        assertTrue(bl.getY() > tl.getY());
        // Left column shares an X; right column shares an X to its right.
        assertEquals(tl.getX(), bl.getX());
        assertEquals(tr.getX(), br.getX());
        assertTrue(tr.getX() > tl.getX());
    }

    @Test
    public void cellsAreEqualSize() {
        DiagramLayout layout = MatrixLayout.layout(root("A", "B", "C", "D"));
        LaidOutShape first = layout.getShapes().get(0);
        for (LaidOutShape s : layout.getShapes()) {
            assertEquals(first.getWidth(), s.getWidth());
            assertEquals(first.getHeight(), s.getHeight());
        }
    }

    @Test
    public void gridIsCentredOnTheSlide() {
        DiagramLayout layout = MatrixLayout.layout(root("A", "B", "C", "D"));
        LaidOutShape tl = layout.getShapes().get(0);
        LaidOutShape br = layout.getShapes().get(3);
        int midX = (tl.getX() + br.getX() + br.getWidth()) / 2;
        int midY = (tl.getY() + br.getY() + br.getHeight()) / 2;
        assertEquals(MatrixLayout.SLIDE_W / 2, midX, 2);
        assertEquals(MatrixLayout.SLIDE_H / 2, midY, 2);
    }

    @Test
    public void childrenBecomeBulletLinesInCellText() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode a = new DiagramNode("Quadrant", 1);
        a.addChild(new DiagramNode("Point", 2));
        root.addChild(a);
        root.addChild(new DiagramNode("B", 1));
        DiagramLayout layout = MatrixLayout.layout(root);
        String text = layout.getShapes().get(0).getText();
        assertTrue(text.startsWith("Quadrant"));
        assertTrue(text.contains("• Point"));
    }
}
