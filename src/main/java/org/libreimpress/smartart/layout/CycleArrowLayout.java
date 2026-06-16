package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Cycle (Arrows) diagram: level-1 nodes as equally-spaced circles
 * arranged clockwise around a ring, connected by curved directed arrows between
 * adjacent circles. Sub-items are ignored. Pure Java (no UNO). Units 1/100 mm.
 */
public final class CycleArrowLayout {

    static final int CIRCLE_D        = 2200;  // circle diameter
    static final int RING_RADIUS     = 5500;  // default slide-centre to circle-centre
    static final int MIN_NODE_GAP    = 400;   // minimum gap between adjacent circle edges
    static final int MAX_RING_RADIUS = 7800;  // cap to keep shapes on-slide
    static final int SLIDE_W         = 25400;
    static final int SLIDE_H         = 19050;

    private CycleArrowLayout() {
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
        int[] indices = new int[n];

        // Scale radius up when many circles would otherwise overlap.
        // Minimum radius: chord between adjacent circles ≥ CIRCLE_D + MIN_NODE_GAP.
        int ringRadius = RING_RADIUS;
        if (n >= 2) {
            int rMin = (int) Math.ceil(
                    (CIRCLE_D + MIN_NODE_GAP) / (2.0 * Math.sin(Math.PI / n)));
            ringRadius = Math.min(MAX_RING_RADIUS, Math.max(RING_RADIUS, rMin));
        }

        for (int i = 0; i < n; i++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
            int nodeCX = cx + (int) Math.round(ringRadius * Math.cos(angle));
            int nodeCY = cy + (int) Math.round(ringRadius * Math.sin(angle));
            LaidOutShape shape = new LaidOutShape(
                    nodes.get(i).getText(), 1,
                    nodeCX - CIRCLE_D / 2, nodeCY - CIRCLE_D / 2,
                    CIRCLE_D, CIRCLE_D,
                    ShapeKind.ELLIPSE, true);
            indices[i] = out.addShape(shape);
        }

        // Curved directed arrows from each circle to the next, wrapping around.
        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;
            out.addEdge(new Edge(indices[i], indices[next], -1, -1, false, true, true));
        }

        return out;
    }
}
