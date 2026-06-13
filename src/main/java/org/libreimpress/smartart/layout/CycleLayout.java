package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Cycle diagram: level-1 nodes as rectangles equally spaced
 * clockwise around a ring, joined by directed straight-line arrows that
 * wrap from the last node back to the first. Sub-items are intentionally
 * ignored — all meaningful content lives at level 1. Pure Java (no UNO).
 * Units are 1/100 mm.
 */
public final class CycleLayout {

    static final int NODE_W       = 3500;  // node width
    static final int NODE_H       = 1400;  // node height
    static final int RING_RADIUS  = 6000;  // slide-centre to node-centre
    static final int SLIDE_W      = 25400;
    static final int SLIDE_H      = 19050;

    private CycleLayout() {
    }

    /**
     * @param root the synthetic root (level 0) produced by the parser; its
     *             level-1 children become the clockwise cycle nodes. Deeper
     *             descendants are silently ignored.
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

        // Place cycle nodes on the ring.
        for (int i = 0; i < n; i++) {
            // Start at the top (−90°) and advance clockwise.
            double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
            DiagramNode node = nodes.get(i);
            int nodeCX = cx + (int) Math.round(RING_RADIUS * Math.cos(angle));
            int nodeCY = cy + (int) Math.round(RING_RADIUS * Math.sin(angle));
            LaidOutShape shape = new LaidOutShape(node.getText(), 1,
                    nodeCX - NODE_W / 2, nodeCY - NODE_H / 2, NODE_W, NODE_H);
            indices[i] = out.addShape(shape);
        }

        // Directed arrows completing the clockwise cycle (last → first wraps around).
        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;
            out.addEdge(new Edge(indices[i], indices[next], -1, -1, true, true));
        }

        return out;
    }
}
