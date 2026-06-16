package org.libreimpress.smartart.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.libreimpress.smartart.models.DiagramNode;

public class VerticalBulletListLayoutTest {

    private static DiagramNode nodeWithChildren(String title, String... children) {
        DiagramNode node = new DiagramNode(title, 1);
        for (String child : children) {
            node.addChild(new DiagramNode(child, 2));
        }
        return node;
    }

    @Test
    public void emptyInputProducesEmptyLayout() {
        DiagramLayout layout = VerticalBulletListLayout.layout(new DiagramNode("", 0));
        assertEquals(0, layout.getShapes().size());
        assertEquals(0, layout.getEdges().size());
    }

    @Test
    public void nodeWithoutChildrenProducesOneTitleBar() {
        DiagramNode root = new DiagramNode("", 0);
        root.addChild(new DiagramNode("A", 1));
        root.addChild(new DiagramNode("B", 1));
        DiagramLayout layout = VerticalBulletListLayout.layout(root);
        assertEquals(2, layout.getShapes().size());
        for (LaidOutShape s : layout.getShapes()) {
            assertEquals(1, s.getLevel());
        }
    }

    @Test
    public void nodeWithChildrenProducesTitleAndContentBox() {
        DiagramNode root = new DiagramNode("", 0);
        root.addChild(nodeWithChildren("Agenda", "First", "Second"));
        DiagramLayout layout = VerticalBulletListLayout.layout(root);
        assertEquals(2, layout.getShapes().size());

        LaidOutShape title = layout.getShapes().get(0);
        LaidOutShape content = layout.getShapes().get(1);
        assertEquals("Agenda", title.getText());
        assertEquals(1, title.getLevel());
        assertEquals(2, content.getLevel());
        assertTrue(content.getText().contains("• First"));
        assertTrue(content.getText().contains("• Second"));
        // Content sits below its title.
        assertTrue(content.getY() >= title.getY() + title.getHeight());
    }

    @Test
    public void titlesStackTopToBottom() {
        DiagramNode root = new DiagramNode("", 0);
        root.addChild(new DiagramNode("A", 1));
        root.addChild(new DiagramNode("B", 1));
        root.addChild(new DiagramNode("C", 1));
        DiagramLayout layout = VerticalBulletListLayout.layout(root);
        int prevY = -1;
        for (LaidOutShape s : layout.getShapes()) {
            assertTrue("titles should descend the slide", s.getY() > prevY);
            prevY = s.getY();
        }
    }

    @Test
    public void shapesStayWithinTheSlide() {
        DiagramNode root = new DiagramNode("", 0);
        root.addChild(nodeWithChildren("A", "a1", "a2"));
        root.addChild(nodeWithChildren("B", "b1"));
        root.addChild(new DiagramNode("C", 1));
        DiagramLayout layout = VerticalBulletListLayout.layout(root);
        for (LaidOutShape s : layout.getShapes()) {
            assertTrue(s.getX() >= 0);
            assertTrue(s.getY() >= 0);
            assertTrue(s.getX() + s.getWidth() <= VerticalBulletListLayout.SLIDE_W);
            assertTrue(s.getY() + s.getHeight() <= VerticalBulletListLayout.SLIDE_H);
        }
    }
}
