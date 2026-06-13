package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays out a Hub & Spoke diagram: each level-1 node is a hub, with its
 * children (spokes) arranged in a circle around it. Multiple hubs are
 * placed left-to-right across the slide. Pure Java (no UNO).
 * Units are 1/100 mm.
 */
public final class HubAndSpokeLayout {

    static final int NODE_W = 4000;
    static final int NODE_H = 1500;
    static final int SPOKE_RADIUS = 3500; // hub-centre to spoke-centre distance
    static final int HUB_SPACING = 16000; // space from one hub centre to the next

    private HubAndSpokeLayout() {
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

            LaidOutShape hubShape = new LaidOutShape(
                    hubNode.getText(), 1,
                    hubCX - NODE_W / 2, hubCY - NODE_H / 2, NODE_W, NODE_H,
                    ShapeKind.ELLIPSE);
            int hubIndex = out.addShape(hubShape);

            List<DiagramNode> spokes = hubNode.getChildren();
            int n = spokes.size();
            for (int i = 0; i < n; i++) {
                double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
                int spokeCX = hubCX + (int) Math.round(SPOKE_RADIUS * Math.cos(angle));
                int spokeCY = hubCY + (int) Math.round(SPOKE_RADIUS * Math.sin(angle));
                DiagramNode spokeNode = spokes.get(i);
                LaidOutShape spokeShape = new LaidOutShape(
                        spokeNode.getText(), spokeNode.getLevel(),
                        spokeCX - NODE_W / 2, spokeCY - NODE_H / 2, NODE_W, NODE_H,
                        ShapeKind.ELLIPSE);
                int spokeIndex = out.addShape(spokeShape);
                out.addEdge(hubIndex, spokeIndex);
            }
        }

        return out;
    }
}
