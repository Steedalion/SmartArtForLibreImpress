package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Cycle (Arrows) diagram: level-1 nodes as circles equally spaced
 * clockwise around a ring, connected by Bézier-curved directed arrows that wrap
 * back to the first node. Sub-items are ignored (same rationale as CycleLayout).
 * Pure Java (no UNO). Units are 1/100 mm.
 */
public final class CycleArrowLayout {

    static final int NODE_D      = 2200;  // circle diameter
    static final int RING_RADIUS = 5500;  // slide-centre to node-centre
    static final int SLIDE_W     = 25400;
    static final int SLIDE_H     = 19050;

    private CycleArrowLayout() {
    }

    /**
     * @param root the synthetic root (level 0); its level-1 children become the
     *             clockwise cycle nodes. Deeper descendants are silently ignored.
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

        for (int i = 0; i < n; i++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
            DiagramNode node = nodes.get(i);
            int nodeCX = cx + (int) Math.round(RING_RADIUS * Math.cos(angle));
            int nodeCY = cy + (int) Math.round(RING_RADIUS * Math.sin(angle));
            LaidOutShape shape = new LaidOutShape(node.getText(), 1,
                    nodeCX - NODE_D / 2, nodeCY - NODE_D / 2, NODE_D, NODE_D,
                    ShapeKind.ELLIPSE);
            indices[i] = out.addShape(shape);
        }

        // Curved directed arrows completing the clockwise cycle.
        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;
            out.addEdge(new Edge(indices[i], indices[next], -1, -1, false, true, true));
        }

        return out;
    }
}
