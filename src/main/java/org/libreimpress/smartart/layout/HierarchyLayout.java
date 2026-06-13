package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

/**
 * Lays out a parsed hierarchy as a top-down tree: each level sits on its own
 * row, leaves are spread left-to-right, and every parent is centred over its
 * children. Pure Java (no UNO), so the positioning is unit-testable; a renderer
 * turns the result into shapes. Units are 1/100 mm.
 */
public final class HierarchyLayout {

    static final int NODE_W = 4000;
    static final int NODE_H = 1500;
    static final int H_GAP = 1500; // gap between adjacent boxes on a row
    static final int V_GAP = 1500; // gap between rows
    static final int MARGIN_X = 1000;
    static final int MARGIN_Y = 1000;

    private HierarchyLayout() {
    }

    /**
     * @param root the synthetic root (level 0) produced by the parser; its
     *             children are the level-1 nodes.
     */
    public static DiagramLayout layout(DiagramNode root) {
        DiagramLayout out = new DiagramLayout();
        int[] leafCursor = { 0 };
        for (DiagramNode top : root.getChildren()) {
            place(top, out, leafCursor);
        }
        return out;
    }

    /** Places {@code node} and its subtree; returns its shape index. */
    private static int place(DiagramNode node, DiagramLayout out, int[] leafCursor) {
        int centerX;
        int[] childIndices = new int[node.getChildren().size()];

        if (node.getChildren().isEmpty()) {
            centerX = MARGIN_X + NODE_W / 2 + leafCursor[0] * (NODE_W + H_GAP);
            leafCursor[0]++;
        } else {
            int firstCenter = Integer.MIN_VALUE;
            int lastCenter = 0;
            int i = 0;
            for (DiagramNode child : node.getChildren()) {
                int childIndex = place(child, out, leafCursor);
                childIndices[i++] = childIndex;
                int cc = out.getShapes().get(childIndex).centerX();
                if (firstCenter == Integer.MIN_VALUE) {
                    firstCenter = cc;
                }
                lastCenter = cc;
            }
            centerX = (firstCenter + lastCenter) / 2;
        }

        int y = MARGIN_Y + (node.getLevel() - 1) * (NODE_H + V_GAP);
        LaidOutShape shape = new LaidOutShape(
                node.getText(), node.getLevel(), centerX - NODE_W / 2, y, NODE_W, NODE_H);
        int myIndex = out.addShape(shape);
        for (int childIndex : childIndices) {
            out.addEdge(myIndex, childIndex);
        }
        return myIndex;
    }
}
