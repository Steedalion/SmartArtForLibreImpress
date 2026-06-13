package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Cycle diagram: level-1 nodes as rectangles equally spaced
 * clockwise around a ring, joined by directed straight-line arrows. Level-2+
 * children are placed radially outward from their parent node. Pure Java
 * (no UNO). Units are 1/100 mm.
 */
public final class CycleLayout {

    static final int NODE_W       = 3500;  // level-1 node width
    static final int NODE_H       = 1400;  // level-1 node height
    static final int RING_RADIUS  = 6000;  // centre-to-node-centre distance
    static final int CHILD_GAP    = 600;   // gap between parent edge and child edge
    static final int SLIDE_W      = 25400;
    static final int SLIDE_H      = 19050;

    private CycleLayout() {
    }

    /**
     * @param root the synthetic root (level 0) produced by the parser; its
     *             children become the clockwise cycle nodes.
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
        double[] angles = new double[n];

        // Place cycle nodes on the ring.
        for (int i = 0; i < n; i++) {
            // Start at the top (−90°) and advance clockwise.
            double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
            angles[i] = angle;
            DiagramNode node = nodes.get(i);
            int nodeCX = cx + (int) Math.round(RING_RADIUS * Math.cos(angle));
            int nodeCY = cy + (int) Math.round(RING_RADIUS * Math.sin(angle));
            LaidOutShape shape = new LaidOutShape(node.getText(), 1,
                    nodeCX - NODE_W / 2, nodeCY - NODE_H / 2, NODE_W, NODE_H);
            indices[i] = out.addShape(shape);

            // Recursively place level-2+ children radially outward.
            placeChildren(out, node.getChildren(), nodeCX, nodeCY, angle, 2, indices[i]);
        }

        // Directed arrows completing the clockwise cycle (last → first wraps around).
        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;
            out.addEdge(new Edge(indices[i], indices[next], -1, -1, true, true));
        }

        return out;
    }

    /**
     * Recursively places children outward along the radial angle from the
     * parent node.
     *
     * @param parentCX  centre X of the parent shape
     * @param parentCY  centre Y of the parent shape
     * @param angle     radial angle the parent sits on (radians)
     * @param level     depth of these children (2, 3, …)
     * @param parentIdx shape index of the parent in {@code out}
     */
    private static void placeChildren(DiagramLayout out, List<DiagramNode> children,
            int parentCX, int parentCY, double angle, int level, int parentIdx) {
        if (children.isEmpty()) {
            return;
        }
        int w = Math.max(1500, NODE_W - (level - 1) * 200);
        int h = Math.max(700,  NODE_H - (level - 1) * 100);
        // Use NODE_W/2 as parent half-size so children always clear the widest dimension.
        double dist = NODE_W / 2.0 + CHILD_GAP + h / 2.0;

        for (DiagramNode child : children) {
            int childCX = parentCX + (int) Math.round(dist * Math.cos(angle));
            int childCY = parentCY + (int) Math.round(dist * Math.sin(angle));
            LaidOutShape childShape = new LaidOutShape(child.getText(), level,
                    childCX - w / 2, childCY - h / 2, w, h);
            int childIdx = out.addShape(childShape);
            out.addEdge(new Edge(parentIdx, childIdx, -1, -1, true));
            placeChildren(out, child.getChildren(), childCX, childCY, angle, level + 1, childIdx);
            dist += h + CHILD_GAP;
        }
    }
}
