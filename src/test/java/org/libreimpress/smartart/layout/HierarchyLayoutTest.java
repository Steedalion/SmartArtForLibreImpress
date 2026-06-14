package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class HierarchyLayoutTest {

    /** Builds: root(0) -> A(1) -> B(2) -> C(3) (a single chain). */
    private DiagramNode chain() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode a = new DiagramNode("A", 1);
        DiagramNode b = new DiagramNode("B", 2);
        DiagramNode c = new DiagramNode("C", 3);
        root.addChild(a);
        a.addChild(b);
        b.addChild(c);
        return root;
    }

    @Test
    public void chainStacksVerticallyAtSameColumn() {
        DiagramLayout layout = HierarchyLayout.layout(chain());
        assertEquals(3, layout.getShapes().size());
        assertEquals(2, layout.getEdges().size());

        // all three share the same centre x (a straight vertical line)
        int cx = layout.getShapes().get(0).centerX();
        for (LaidOutShape s : layout.getShapes()) {
            assertEquals(cx, s.centerX());
        }
    }

    @Test
    public void deeperLevelsAreLowerOnTheSlide() {
        DiagramLayout layout = HierarchyLayout.layout(chain());
        for (Edge e : layout.getEdges()) {
            LaidOutShape parent = layout.getShapes().get(e.getParent());
            LaidOutShape child = layout.getShapes().get(e.getChild());
            assertTrue("child must sit below its parent", child.getY() > parent.getY());
            assertTrue("child must be one level deeper",
                    child.getLevel() == parent.getLevel() + 1);
        }
    }

    @Test
    public void parentIsCentredOverItsChildren() {
        // root(0) -> P(1) -> {X(2), Y(2)}
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode p = new DiagramNode("P", 1);
        DiagramNode x = new DiagramNode("X", 2);
        DiagramNode y = new DiagramNode("Y", 2);
        root.addChild(p);
        p.addChild(x);
        p.addChild(y);

        DiagramLayout layout = HierarchyLayout.layout(root);
        assertEquals(3, layout.getShapes().size());

        // find P (level 1) and the two children (level 2)
        LaidOutShape pShape = null;
        int firstChild = Integer.MAX_VALUE;
        int lastChild = Integer.MIN_VALUE;
        for (LaidOutShape s : layout.getShapes()) {
            if (s.getLevel() == 1) {
                pShape = s;
            } else {
                firstChild = Math.min(firstChild, s.centerX());
                lastChild = Math.max(lastChild, s.centerX());
            }
        }
        assertEquals((firstChild + lastChild) / 2, pShape.centerX());
    }

    @Test
    public void siblingsDoNotOverlapHorizontally() {
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode p = new DiagramNode("P", 1);
        DiagramNode x = new DiagramNode("X", 2);
        DiagramNode y = new DiagramNode("Y", 2);
        root.addChild(p);
        p.addChild(x);
        p.addChild(y);

        DiagramLayout layout = HierarchyLayout.layout(root);
        LaidOutShape left = null;
        LaidOutShape right = null;
        for (LaidOutShape s : layout.getShapes()) {
            if (s.getLevel() == 2) {
                if (left == null || s.getX() < left.getX()) {
                    left = s;
                }
                if (right == null || s.getX() > right.getX()) {
                    right = s;
                }
            }
        }
        assertTrue("boxes must not overlap",
                left.getX() + left.getWidth() <= right.getX());
    }

    @Test
    public void manyLeavesScaleDownToFitSlide() {
        // 10 siblings at BASE_NODE_W=4000 + gaps=1500 each would overflow 25400.
        DiagramNode root = new DiagramNode("", 0);
        DiagramNode parent = new DiagramNode("Root", 1);
        root.addChild(parent);
        for (int i = 0; i < 10; i++) {
            parent.addChild(new DiagramNode("C" + i, 2));
        }
        DiagramLayout layout = HierarchyLayout.layout(root);
        int rightLimit = HierarchyLayout.SLIDE_W - HierarchyLayout.MARGIN_X;
        for (LaidOutShape s : layout.getShapes()) {
            assertTrue("shape right edge must stay on slide",
                    s.getX() + s.getWidth() <= rightLimit);
        }
    }

    @Test
    public void forestPlacesEachTopLevelTree() {
        // root(0) -> {A(1)->A1(2)->A1a(3), B(1)->B1(2)->B1a(3)}
        DiagramNode root = new DiagramNode("", 0);
        for (String name : new String[] { "A", "B" }) {
            DiagramNode top = new DiagramNode(name, 1);
            DiagramNode mid = new DiagramNode(name + "1", 2);
            DiagramNode leaf = new DiagramNode(name + "1a", 3);
            top.addChild(mid);
            mid.addChild(leaf);
            root.addChild(top);
        }
        DiagramLayout layout = HierarchyLayout.layout(root);
        assertEquals(6, layout.getShapes().size());
        assertEquals(4, layout.getEdges().size()); // 6 nodes - 2 roots
    }
}
