package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Cycle (Blocks) diagram: level-1 nodes as rectangles arranged
 * clockwise around a ring, with a solid block arrow between each pair of
 * adjacent rectangles pointing toward the next node. Sub-items are ignored.
 * Pure Java (no UNO). Units are 1/100 mm.
 */
public final class CycleBlockLayout {

    static final int NODE_W          = 3000;
    static final int NODE_H          = 1400;
    static final int RING_RADIUS     = 5500;  // default slide-centre to node-centre
    static final int MIN_NODE_GAP    = 400;   // minimum gap between adjacent node edges
    static final int MAX_RING_RADIUS = 7800;  // cap to keep shapes on-slide
    static final int ARROW_W         = 1000;
    static final int ARROW_H         = 600;
    static final int SLIDE_W         = 25400;
    static final int SLIDE_H         = 19050;

    private CycleBlockLayout() {
    }

    /**
     * @param root synthetic root (level 0); its level-1 children become the
     *             cycle nodes. Deeper descendants are silently ignored.
     */
    public static DiagramLayout layout(DiagramNode root) {
        DiagramLayout out = new DiagramLayout();
        List<DiagramNode> nodes = root.getChildren();
        int n = nodes.size();
        if (n == 0) {
            return out;
        }

        int cx = SLIDE_W / 2;
        int cy = SLIDE_H / 2;

        // Scale radius up when many nodes would otherwise overlap.
        // Minimum radius: chord between adjacent nodes ≥ NODE_W + MIN_NODE_GAP.
        int ringRadius = RING_RADIUS;
        if (n >= 2) {
            int rMin = (int) Math.ceil(
                    (NODE_W + MIN_NODE_GAP) / (2.0 * Math.sin(Math.PI / n)));
            ringRadius = Math.min(MAX_RING_RADIUS, Math.max(RING_RADIUS, rMin));
        }

        int[] rectCX = new int[n];
        int[] rectCY = new int[n];

        // Place rectangles equally spaced around the ring, starting at the top.
        for (int i = 0; i < n; i++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
            rectCX[i] = cx + (int) Math.round(ringRadius * Math.cos(angle));
            rectCY[i] = cy + (int) Math.round(ringRadius * Math.sin(angle));
            out.addShape(new LaidOutShape(
                    nodes.get(i).getText(), 1,
                    rectCX[i] - NODE_W / 2, rectCY[i] - NODE_H / 2,
                    NODE_W, NODE_H));
        }

        // Place a block arrow between each pair of adjacent rectangles.
        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;
            int arrowCX = (rectCX[i] + rectCX[next]) / 2;
            int arrowCY = (rectCY[i] + rectCY[next]) / 2;
            double dx = rectCX[next] - rectCX[i];
            double dy = rectCY[next] - rectCY[i];
            // RotateAngle is counter-clockwise. Screen Y is inverted, so negate dy
            // to convert from screen coords to standard math angle.
            int rotate100 = (int) Math.round(Math.atan2(-dy, dx) * 18000.0 / Math.PI);
            if (rotate100 < 0) {
                rotate100 += 36000;
            }
            out.addShape(new LaidOutShape(
                    "", 0,
                    arrowCX - ARROW_W / 2, arrowCY - ARROW_H / 2,
                    ARROW_W, ARROW_H,
                    ShapeKind.BLOCK_ARROW, rotate100, 0));
        }

        return out;
    }
}
