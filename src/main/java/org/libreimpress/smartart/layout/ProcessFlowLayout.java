package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Process Flow diagram: the level-1 nodes are placed as a
 * left-to-right sequence of equally-sized boxes near the top of the slide,
 * with an arrow connector from each box to the next. Level-2 children of each
 * step are stacked vertically below their parent. Pure Java (no UNO).
 * Units are 1/100 mm.
 */
public final class ProcessFlowLayout {

    static final int BASE_NODE_W = 4000;
    static final int BASE_NODE_H = 1500;
    static final int SIZE_DECREMENT = 30; // reduce size by 30 per level depth
    static final int H_GAP = 1500;  // space between consecutive step boxes
    static final int V_GAP = 800;   // vertical gap between parent and child rows
    static final int CHILD_H_GAP = 400; // horizontal gap between children in a row
    static final int SUBITEM_H = 900;   // landscape sub-item height (text fills it instead of floating)
    static final int SLIDE_W = 25400; // standard Impress slide: 254 mm
    static final int SLIDE_H = 19050; // standard Impress slide: 190.5 mm
    static final int MARGIN_X = 1000;
    static final int MARGIN_Y = 1000;

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
     * <p>Level-1 steps are arranged left-to-right near the top of the slide.
     * Level-2 children of each step are stacked vertically below their parent,
     * centred on the parent's X. Edges connect adjacent steps horizontally
     * (with arrowheads) and each parent to its children vertically.
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
        // Scale step width down if all steps would overflow the slide.
        int availableW = SLIDE_W - 2 * MARGIN_X;
        if (n * w1 + (n - 1) * H_GAP > availableW) {
            w1 = Math.max(1500, (availableW - (n - 1) * H_GAP) / n);
        }
        int totalWidth = n * w1 + (n - 1) * H_GAP;
        int startX = Math.max(MARGIN_X, (SLIDE_W - totalWidth) / 2);
        int y1 = MARGIN_Y;

        // Place level-1 steps and record their centre-X for sub-item alignment.
        int[] indices  = new int[n];
        int[] stepCXs  = new int[n];
        for (int i = 0; i < n; i++) {
            int x = startX + i * (w1 + H_GAP);
            stepCXs[i] = x + w1 / 2;
            indices[i] = out.addShape(
                    new LaidOutShape(steps.get(i).getText(), 1, x, y1, w1, h1));
        }

        // Connect the steps horizontally with right→left arrowhead connectors.
        for (int i = 0; i < n - 1; i++) {
            out.addEdge(new Edge(indices[i], indices[i + 1], 1, 3, false, true));
        }

        // Place level-2 children below each step in a horizontal row, scaled to
        // fit within the step's width so columns never overlap.
        int baseW2 = nodeWidth(2);
        int h2 = SUBITEM_H;
        int childY = y1 + h1 + V_GAP;
        for (int i = 0; i < n; i++) {
            List<DiagramNode> children = steps.get(i).getChildren();
            if (children.isEmpty()) continue;
            int nc = children.size();
            int w2 = Math.min(baseW2, (w1 - (nc - 1) * CHILD_H_GAP) / nc);
            int totalChildW = nc * w2 + (nc - 1) * CHILD_H_GAP;
            int childStartX = stepCXs[i] - totalChildW / 2;
            for (int j = 0; j < nc; j++) {
                int childX = childStartX + j * (w2 + CHILD_H_GAP);
                int childIdx = out.addShape(new LaidOutShape(
                        children.get(j).getText(), 2, childX, childY, w2, h2, true));
                // bottom of parent (glue 2) → top of child (glue 0)
                out.addEdge(indices[i], childIdx, 2, 0);
            }
        }

        return out;
    }
}
