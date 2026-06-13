package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Process Flow diagram: the level-1 nodes are placed as a
 * left-to-right sequence of equally-sized boxes, centred on the slide, with a
 * connector from each box to the next. Pure Java (no UNO). Units are 1/100 mm.
 */
public final class ProcessFlowLayout {

    static final int BASE_NODE_W = 4000;
    static final int BASE_NODE_H = 1500;
    static final int SIZE_DECREMENT = 30; // reduce size by 30 per level depth
    static final int H_GAP = 1500;  // space between consecutive boxes
    static final int SLIDE_W = 25400; // standard Impress slide: 254 mm
    static final int SLIDE_H = 19050; // standard Impress slide: 190.5 mm
    static final int MARGIN_X = 1000;

    private ProcessFlowLayout() {
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
     *             children are the level-1 nodes that become the flow steps.
     *
     * <p>Layout: level-1 steps only, arranged left-to-right on a single row,
     * centred on the slide. Edges connect adjacent steps horizontally.
     */
    public static DiagramLayout layout(DiagramNode root) {
        DiagramLayout out = new DiagramLayout();
        List<DiagramNode> steps = root.getChildren();
        int n = steps.size();
        if (n == 0) {
            return out;
        }

        int w1 = nodeWidth(1);
        int h1 = nodeHeight(1);
        int totalWidth = n * w1 + (n - 1) * H_GAP;
        int startX = Math.max(MARGIN_X, (SLIDE_W - totalWidth) / 2);
        int y = (SLIDE_H - h1) / 2;

        int[] indices = new int[n];
        for (int i = 0; i < n; i++) {
            int x = startX + i * (w1 + H_GAP);
            indices[i] = out.addShape(
                    new LaidOutShape(steps.get(i).getText(), 1, x, y, w1, h1));
        }

        // Connect the steps horizontally.
        for (int i = 0; i < n - 1; i++) {
            out.addEdge(indices[i], indices[i + 1], 1, 3);
        }

        return out;
    }
}
