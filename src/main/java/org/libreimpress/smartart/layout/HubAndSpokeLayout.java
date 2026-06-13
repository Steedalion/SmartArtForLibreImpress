package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Hub & Spoke diagram: each level-1 node is a hub, with its
 * children (spokes) arranged in a circle around it. Multiple hubs are
 * placed left-to-right across the slide. Pure Java (no UNO).
 * Units are 1/100 mm.
 */
public final class HubAndSpokeLayout {

    static final int BASE_NODE_W = 4000;
    static final int BASE_NODE_H = 1500;
    static final int SIZE_DECREMENT = 30; // reduce size by 30 per level depth
    static final int SPOKE_RADIUS = 3500; // hub-centre to spoke-centre distance
    static final int HUB_SPACING = 16000; // space from one hub centre to the next

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

        // Calculate the bounding box to fit all hubs left-to-right.
        int slideW = 25400; // standard Impress slide width in 1/100 mm
        int numHubs = level1.size();
        int totalHubWidth = numHubs * HUB_SPACING;
        int startX = (slideW - totalHubWidth) / 2 + HUB_SPACING / 2;
        int centerY = 9525; // centre Y of slide

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

            // Spokes are only the immediate children (level-2).
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
            }
        }

        return out;
    }
}
