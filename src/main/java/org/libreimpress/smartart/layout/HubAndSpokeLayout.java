package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Hub & Spoke diagram: each level-1 node is a hub, with its
 * level-2 children (spokes) arranged in a circle around it, and any level-3+
 * descendants stacked radially outward from their spoke. Multiple hubs are
 * placed left-to-right across the slide. Pure Java (no UNO).
 * Units are 1/100 mm.
 */
public final class HubAndSpokeLayout {

    static final int BASE_NODE_W = 4000;
    static final int BASE_NODE_H = 1500;
    static final int SIZE_DECREMENT = 30; // reduce diameter by 30 per level depth
    static final int SPOKE_RADIUS = 3500; // hub-centre to spoke-centre distance
    static final int CHILD_GAP    = 600;  // gap between spoke edge and first child edge
    static final int HUB_SPACING  = 16000; // space from one hub centre to the next

    private HubAndSpokeLayout() {
    }

    /** Diameter of a circle node at the given level. */
    private static int nodeDiameter(int level) {
        return Math.max(1500, BASE_NODE_H - (level - 1) * SIZE_DECREMENT);
    }

    /**
     * @param root the synthetic root (level 0) produced by the parser.
     */
    public static DiagramLayout layout(DiagramNode root) {
        DiagramLayout out = new DiagramLayout();
        List<DiagramNode> level1 = root.getChildren();
        if (level1.isEmpty()) {
            return out;
        }

        int slideW = 25400;
        int numHubs = level1.size();
        int totalHubWidth = numHubs * HUB_SPACING;
        int startX = (slideW - totalHubWidth) / 2 + HUB_SPACING / 2;
        int centerY = 9525;

        for (int h = 0; h < numHubs; h++) {
            DiagramNode hubNode = level1.get(h);
            int hubCX = startX + h * HUB_SPACING;
            int hubCY = centerY;

            int hubD = nodeDiameter(1);
            LaidOutShape hubShape = new LaidOutShape(
                    hubNode.getText(), 1,
                    hubCX - hubD / 2, hubCY - hubD / 2, hubD, hubD,
                    ShapeKind.ELLIPSE);
            int hubIndex = out.addShape(hubShape);

            List<DiagramNode> spokes = hubNode.getChildren();
            int n = spokes.size();
            for (int i = 0; i < n; i++) {
                double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
                int spokeCX = hubCX + (int) Math.round(SPOKE_RADIUS * Math.cos(angle));
                int spokeCY = hubCY + (int) Math.round(SPOKE_RADIUS * Math.sin(angle));
                DiagramNode spokeNode = spokes.get(i);
                int spokeD = nodeDiameter(2);
                LaidOutShape spokeShape = new LaidOutShape(
                        spokeNode.getText(), 2,
                        spokeCX - spokeD / 2, spokeCY - spokeD / 2, spokeD, spokeD,
                        ShapeKind.ELLIPSE);
                int spokeIndex = out.addShape(spokeShape);
                out.addEdge(new Edge(hubIndex, spokeIndex, -1, -1, true));

                // Level-3+ children stacked radially outward from the spoke.
                placeChildren(out, spokeNode.getChildren(), spokeCX, spokeCY,
                        spokeD, angle, 3, spokeIndex);
            }
        }

        return out;
    }

    /**
     * Recursively places children of a spoke (or a child node) along the
     * same radial angle, further from the hub centre.
     *
     * @param parentCX  centre X of the parent circle
     * @param parentCY  centre Y of the parent circle
     * @param parentD   diameter of the parent circle
     * @param angle     radial angle from hub (radians)
     * @param level     level of these children
     * @param parentIdx shape index of the parent
     */
    private static void placeChildren(DiagramLayout out,
                                      List<DiagramNode> children,
                                      int parentCX, int parentCY,
                                      int parentD, double angle,
                                      int level, int parentIdx) {
        if (children.isEmpty()) {
            return;
        }
        int childD = nodeDiameter(level);
        // Starting centre-to-centre distance: parent radius + gap + child radius.
        double dist = parentD / 2.0 + CHILD_GAP + childD / 2.0;

        for (DiagramNode child : children) {
            int childCX = parentCX + (int) Math.round(dist * Math.cos(angle));
            int childCY = parentCY + (int) Math.round(dist * Math.sin(angle));
            LaidOutShape childShape = new LaidOutShape(
                    child.getText(), level,
                    childCX - childD / 2, childCY - childD / 2, childD, childD,
                    ShapeKind.ELLIPSE);
            int childIdx = out.addShape(childShape);
            out.addEdge(new Edge(parentIdx, childIdx, -1, -1, true));
            placeChildren(out, child.getChildren(), childCX, childCY,
                    childD, angle, level + 1, childIdx);
            dist += childD + CHILD_GAP;
        }
    }
}
