package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Sequential Chevron diagram: level-1 nodes as a horizontal sequence
 * of chevrons (first as pentagon, rest as chevrons), with level-2 children
 * (sublists) placed as boxes below each parent. Pure Java (no UNO).
 * Units are 1/100 mm.
 */
public final class SequentialChevronLayout {

    static final int BASE_NODE_W = 4000;
    static final int BASE_NODE_H = 1500;
    static final int SIZE_DECREMENT = 30; // reduce size by 30 per level depth
    static final int CHEVRON_SPACING = 1500; // gap between chevrons
    static final int SUBITEM_GAP = 800; // gap between subitems below a chevron
    static final int CHEVRON_TO_SUBITEM_GAP = 1200; // gap from chevron to sublists below
    static final int SLIDE_W = 25400; // standard Impress slide: 254 mm
    static final int SLIDE_H = 19050; // standard Impress slide: 190.5 mm
    static final int MARGIN_X = 1000;
    static final int MARGIN_Y = 1000;
    static final int CHILD_V_GAP = 500; // vertical gap between a sub-item and its children

    private SequentialChevronLayout() {
    }

    /** Calculate node width based on level (reduces 30/level from base). */
    private static int nodeWidth(int level) {
        return Math.max(1500, BASE_NODE_W - (level - 1) * SIZE_DECREMENT);
    }

    /** Calculate node height based on level (reduces 30/level from base). */
    private static int nodeHeight(int level) {
        return Math.max(600, BASE_NODE_H - (level - 1) * SIZE_DECREMENT);
    }

    /**
     * @param root the synthetic root (level 0) produced by the parser; its
     *             children are the level-1 nodes that become the chevrons.
     *
     * <p>Layout: level-1 nodes as chevrons arranged left-to-right on the top row.
     * Level-2 children of each chevron are placed as boxes directly below it.
     */
    public static DiagramLayout layout(DiagramNode root) {
        DiagramLayout out = new DiagramLayout();
        List<DiagramNode> chevrons = root.getChildren();
        int n = chevrons.size();
        if (n == 0) {
            return out;
        }

        int w1 = nodeWidth(1);
        int h1 = nodeHeight(1);

        // Calculate chevron row layout (left-to-right, top-centered).
        int totalChevronWidth = n * w1 + (n - 1) * CHEVRON_SPACING;
        int chevronStartX = Math.max(MARGIN_X, (SLIDE_W - totalChevronWidth) / 2);
        int chevronY = MARGIN_Y;

        // Place chevrons and their sublists.
        for (int i = 0; i < n; i++) {
            DiagramNode chevronNode = chevrons.get(i);
            int chevronX = chevronStartX + i * (w1 + CHEVRON_SPACING);
            int chevronCX = chevronX + w1 / 2; // center X for positioning subitems

            // First chevron is a flat-back pentagon; rest are notched chevrons.
            ShapeKind shape = (i == 0) ? ShapeKind.PENTAGON : ShapeKind.CHEVRON;
            LaidOutShape chevronShape = new LaidOutShape(
                    chevronNode.getText(), 1,
                    chevronX, chevronY, w1, h1,
                    shape);
            int chevronIndex = out.addShape(chevronShape);

            // Place level-2 children stacked vertically below this chevron,
            // each centred on the chevron's X.  Vertical stacking prevents
            // overlap with sub-item columns from adjacent chevrons.
            List<DiagramNode> subitems = chevronNode.getChildren();
            if (!subitems.isEmpty()) {
                int w2 = nodeWidth(2);
                int h2 = nodeHeight(2);
                int subitemX = chevronCX - w2 / 2;
                int subitemY = chevronY + h1 + CHEVRON_TO_SUBITEM_GAP;

                for (DiagramNode subitem : subitems) {
                    LaidOutShape subitemShape = new LaidOutShape(
                            subitem.getText(), 2,
                            subitemX, subitemY, w2, h2);
                    int subitemIndex = out.addShape(subitemShape);
                    out.addEdge(chevronIndex, subitemIndex);
                    placeSubtree(out, subitem.getChildren(),
                            subitemX + w2 / 2, subitemY + h2, subitemIndex, 3);
                    subitemY += h2 + SUBITEM_GAP;
                }
            }
        }

        return out;
    }

    /**
     * Recursively places {@code children} stacked vertically below a parent
     * sub-item, centred on the parent's X, connecting parent → child.
     *
     * @param parentCX       centre X of the parent shape
     * @param parentBottomY  Y of the parent's bottom edge
     * @param parentIndex    shape index of the parent in {@code out}
     * @param level          level of these children (3, 4, …)
     */
    private static void placeSubtree(DiagramLayout out, List<DiagramNode> children,
                                     int parentCX, int parentBottomY,
                                     int parentIndex, int level) {
        if (children.isEmpty()) {
            return;
        }
        int w = nodeWidth(level);
        int h = nodeHeight(level);
        int childY = parentBottomY + CHILD_V_GAP;
        for (DiagramNode child : children) {
            int childX = parentCX - w / 2;
            LaidOutShape childShape = new LaidOutShape(child.getText(), level, childX, childY, w, h);
            int childIndex = out.addShape(childShape);
            out.addEdge(parentIndex, childIndex, 2, 0); // bottom of parent → top of child
            placeSubtree(out, child.getChildren(), parentCX, childY + h, childIndex, level + 1);
            childY += h + CHILD_V_GAP;
        }
    }
}
