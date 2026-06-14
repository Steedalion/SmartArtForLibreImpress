package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

/**
 * Lays out a parsed hierarchy as a top-down tree: each level sits on its own
 * row, leaves are spread left-to-right, and every parent is centred over its
 * children. Optimized for 2-level hierarchies (parent → children) but supports
 * arbitrary depth. Pure Java (no UNO), so the positioning is unit-testable;
 * a renderer turns the result into shapes. Units are 1/100 mm.
 */
public final class HierarchyLayout {

    static final int BASE_NODE_W   = 4000;
    static final int BASE_NODE_H   = 1500;
    static final int SIZE_DECREMENT = 30;  // reduce size by 30 per level depth
    static final int H_GAP          = 1500; // gap between adjacent boxes on a row
    static final int V_GAP          = 1500; // gap between rows
    static final int MARGIN_X       = 1000;
    static final int MARGIN_Y       = 1000;
    static final int SLIDE_W        = 25400;

    private HierarchyLayout() {
    }

    /** Node width at the given level, relative to an effective base width. */
    private static int nodeWidth(int level, int effectiveBaseW) {
        return Math.max(800, effectiveBaseW - (level - 1) * SIZE_DECREMENT);
    }

    /** Calculate node height based on level (reduces 30/level from base). */
    private static int nodeHeight(int level) {
        return Math.max(600, BASE_NODE_H - (level - 1) * SIZE_DECREMENT);
    }

    /** Counts leaf nodes (nodes with no children) in the subtree rooted at {@code node}. */
    private static int countLeaves(DiagramNode node) {
        if (node.getChildren().isEmpty()) {
            return 1;
        }
        int count = 0;
        for (DiagramNode child : node.getChildren()) {
            count += countLeaves(child);
        }
        return count;
    }

    /**
     * @param root the synthetic root (level 0) produced by the parser; its
     *             children are the level-1 nodes.
     */
    public static DiagramLayout layout(DiagramNode root) {
        DiagramLayout out = new DiagramLayout();
        if (root.getChildren().isEmpty()) {
            return out;
        }

        // Count leaves to determine if horizontal scaling is needed.
        int numLeaves = 0;
        for (DiagramNode top : root.getChildren()) {
            numLeaves += countLeaves(top);
        }
        int availW = SLIDE_W - 2 * MARGIN_X;
        int effectiveBaseW = BASE_NODE_W;
        int effectiveHGap = H_GAP;
        int defaultTotal = numLeaves * BASE_NODE_W + (numLeaves - 1) * H_GAP;
        if (defaultTotal > availW) {
            effectiveBaseW = Math.max(800, (availW - (numLeaves - 1) * H_GAP) / numLeaves);
            int totalAtMin = numLeaves * effectiveBaseW + (numLeaves - 1) * effectiveHGap;
            if (totalAtMin > availW) {
                effectiveHGap = Math.max(200, (availW - numLeaves * effectiveBaseW)
                        / Math.max(1, numLeaves - 1));
            }
        }

        int[] leafCursor = { 0 };
        for (DiagramNode top : root.getChildren()) {
            place(top, out, leafCursor, effectiveBaseW, effectiveHGap);
        }
        return out;
    }

    /** Places {@code node} and its subtree; returns its shape index. */
    private static int place(DiagramNode node, DiagramLayout out, int[] leafCursor,
            int effectiveBaseW, int effectiveHGap) {
        int nodeW = nodeWidth(node.getLevel(), effectiveBaseW);
        int nodeH = nodeHeight(node.getLevel());
        int centerX;
        int[] childIndices = new int[node.getChildren().size()];

        if (node.getChildren().isEmpty()) {
            centerX = MARGIN_X + nodeW / 2 + leafCursor[0] * (nodeW + effectiveHGap);
            leafCursor[0]++;
        } else {
            int firstCenter = Integer.MIN_VALUE;
            int lastCenter = 0;
            int i = 0;
            for (DiagramNode child : node.getChildren()) {
                int childIndex = place(child, out, leafCursor, effectiveBaseW, effectiveHGap);
                childIndices[i++] = childIndex;
                int cc = out.getShapes().get(childIndex).centerX();
                if (firstCenter == Integer.MIN_VALUE) {
                    firstCenter = cc;
                }
                lastCenter = cc;
            }
            centerX = (firstCenter + lastCenter) / 2;
        }

        int y = MARGIN_Y + (node.getLevel() - 1) * (nodeH + V_GAP);
        LaidOutShape shape = new LaidOutShape(
                node.getText(), node.getLevel(), centerX - nodeW / 2, y, nodeW, nodeH);
        int myIndex = out.addShape(shape);
        for (int childIndex : childIndices) {
            out.addEdge(myIndex, childIndex);
        }
        return myIndex;
    }
}
