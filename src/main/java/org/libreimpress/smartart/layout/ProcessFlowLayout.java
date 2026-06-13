package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Process Flow diagram: the level-1 nodes are placed as a
 * left-to-right sequence of equally-sized boxes, centred on the slide, with a
 * connector from each box to the next. Pure Java (no UNO). Units are 1/100 mm.
 */
public final class ProcessFlowLayout {

    static final int NODE_W = 4000;
    static final int NODE_H = 1500;
    static final int H_GAP = 1500;  // space between consecutive boxes
    static final int SLIDE_W = 25400; // standard Impress slide: 254 mm
    static final int SLIDE_H = 19050; // standard Impress slide: 190.5 mm
    static final int MARGIN_X = 1000;

    private ProcessFlowLayout() {
    }

    /**
     * @param root the synthetic root (level 0) produced by the parser; its
     *             children are the level-1 nodes that become the flow steps.
     */
    public static DiagramLayout layout(DiagramNode root) {
        DiagramLayout out = new DiagramLayout();
        List<DiagramNode> steps = root.getChildren();
        int n = steps.size();
        if (n == 0) {
            return out;
        }

        int totalWidth = n * NODE_W + (n - 1) * H_GAP;
        int startX = Math.max(MARGIN_X, (SLIDE_W - totalWidth) / 2);
        int y = (SLIDE_H - NODE_H) / 2;

        int[] indices = new int[n];
        for (int i = 0; i < n; i++) {
            int x = startX + i * (NODE_W + H_GAP);
            indices[i] = out.addShape(
                    new LaidOutShape(steps.get(i).getText(), 1, x, y, NODE_W, NODE_H));
        }
        // Glue right-side(1) of each box to left-side(3) of the next so connectors
        // route horizontally rather than auto-routing top/bottom.
        for (int i = 0; i < n - 1; i++) {
            out.addEdge(indices[i], indices[i + 1], 1, 3);
        }
        return out;
    }
}
