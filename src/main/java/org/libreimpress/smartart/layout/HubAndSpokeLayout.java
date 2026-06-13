package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays out a Hub & Spoke diagram: the first level-1 node sits at the centre of
 * the slide as the hub; its level-2 children (plus any extra level-1 siblings)
 * are placed as spokes arranged evenly around a circle. Pure Java (no UNO).
 * Units are 1/100 mm.
 */
public final class HubAndSpokeLayout {

    static final int NODE_W = 4000;
    static final int NODE_H = 1500;
    static final int CENTER_X = 12700; // centre of a 254 mm wide slide
    static final int CENTER_Y = 9525;  // centre of a 190.5 mm tall slide
    static final int SPOKE_RADIUS = 5500; // hub-centre to spoke-centre distance

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

        DiagramNode hubNode = level1.get(0);

        // Spokes = hub's own children, then any extra level-1 siblings.
        List<DiagramNode> spokesNodes = new ArrayList<>(hubNode.getChildren());
        for (int i = 1; i < level1.size(); i++) {
            spokesNodes.add(level1.get(i));
        }

        LaidOutShape hubShape = new LaidOutShape(
                hubNode.getText(), 1,
                CENTER_X - NODE_W / 2, CENTER_Y - NODE_H / 2, NODE_W, NODE_H,
                ShapeKind.ELLIPSE);
        int hubIndex = out.addShape(hubShape);

        int n = spokesNodes.size();
        for (int i = 0; i < n; i++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
            int spokeCX = CENTER_X + (int) Math.round(SPOKE_RADIUS * Math.cos(angle));
            int spokeCY = CENTER_Y + (int) Math.round(SPOKE_RADIUS * Math.sin(angle));
            LaidOutShape spokeShape = new LaidOutShape(
                    spokesNodes.get(i).getText(), 2,
                    spokeCX - NODE_W / 2, spokeCY - NODE_H / 2, NODE_W, NODE_H,
                    ShapeKind.ELLIPSE);
            int spokeIndex = out.addShape(spokeShape);
            out.addEdge(hubIndex, spokeIndex);
        }

        return out;
    }
}
