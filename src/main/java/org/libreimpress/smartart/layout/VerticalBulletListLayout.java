package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Vertical Bullet List: level-1 nodes become title bars stacked
 * top-to-bottom; each title's level-2 and deeper children appear as nested,
 * indented bullet lines in a content box directly beneath its title. A node
 * with no children gives a title
 * bar that fills its whole vertical slot. No connectors are drawn. Pure Java
 * (no UNO). Units are 1/100 mm.
 */
public final class VerticalBulletListLayout {

    static final int SLIDE_W   = 25400;
    static final int SLIDE_H   = 19050;
    static final int MARGIN    = 2000;
    static final int SLOT_GAP  = 400;   // vertical gap between level-1 groups
    static final int INNER_GAP = 100;   // gap between a title bar and its content
    static final int TITLE_H   = 1000;  // title-bar height when a content box follows

    private VerticalBulletListLayout() {}

    /**
     * @param root the synthetic root (level 0) produced by the parser; its
     *             level-1 children become the stacked titles. Each title's
     *             level-2 children become bullet lines beneath it.
     */
    public static DiagramLayout layout(DiagramNode root) {
        DiagramLayout out = new DiagramLayout();
        List<DiagramNode> items = root.getChildren();
        int n = items.size();
        if (n == 0) {
            return out;
        }

        int width = SLIDE_W - 2 * MARGIN;
        int availH = SLIDE_H - 2 * MARGIN;
        int slotH = (availH - (n - 1) * SLOT_GAP) / n;

        int y = MARGIN;
        for (DiagramNode item : items) {
            boolean hasChildren = !item.getChildren().isEmpty();
            if (hasChildren) {
                int titleH = Math.min(TITLE_H, slotH);
                out.addShape(new LaidOutShape(
                        item.getText(), 1, MARGIN, y, width, titleH,
                        ShapeKind.RECTANGLE));
                int contentY = y + titleH + INNER_GAP;
                int contentH = slotH - titleH - INNER_GAP;
                if (contentH > 0) {
                    out.addShape(new LaidOutShape(
                            BulletText.bullets(item), 2, MARGIN, contentY, width, contentH,
                            ShapeKind.RECTANGLE));
                }
            } else {
                out.addShape(new LaidOutShape(
                        item.getText(), 1, MARGIN, y, width, slotH,
                        ShapeKind.RECTANGLE));
            }
            y += slotH + SLOT_GAP;
        }

        return out;
    }
}
