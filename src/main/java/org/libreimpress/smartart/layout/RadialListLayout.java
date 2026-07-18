package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Radial List: each level-1 node is a central circle whose level-2
 * children become rectangular satellites arranged in a ring around it, joined
 * by straight connectors (Hub &amp; Spoke geometry with list-style satellites).
 * Level-3 and deeper descendants appear as nested bullet lines under the
 * satellite's title (via {@link BulletText}), keeping the ring uncluttered.
 * Multiple level-1 nodes get their own ring, left-to-right. Pure Java
 * (no UNO). Units are 1/100 mm.
 */
public final class RadialListLayout {

    static final int SLIDE_W = 25400;
    static final int CENTER_Y = 9525;
    static final int HUB_D = 3400;
    static final int SAT_W = 4800;
    static final int SAT_H = 2200;
    // Hub centre → satellite centre. Sized so the bottom satellite fits below
    // the assumed centre line on the default 16:9 page (15750 high).
    static final int BASE_RADIUS = 5000;
    static final int MIN_SAT_GAP = 500;  // between adjacent satellite edges
    static final int HUB_SPACING = 17000; // one ring centre to the next

    private RadialListLayout() {}

    /**
     * @param root the synthetic root (level 0) produced by the parser.
     */
    public static DiagramLayout layout(DiagramNode root) {
        DiagramLayout out = new DiagramLayout();
        List<DiagramNode> hubs = root.getChildren();
        int numHubs = hubs.size();
        if (numHubs == 0) {
            return out;
        }

        int startX = (SLIDE_W - numHubs * HUB_SPACING) / 2 + HUB_SPACING / 2;

        for (int h = 0; h < numHubs; h++) {
            DiagramNode hub = hubs.get(h);
            int hubCX = startX + h * HUB_SPACING;
            // No scale-to-fit on the hub: PROPORTIONAL would balloon a short
            // label to fill the circle; AUTOFIT shrinks long ones only.
            int hubIdx = out.addShape(new LaidOutShape(hub.getText(), 1,
                    hubCX - HUB_D / 2, CENTER_Y - HUB_D / 2, HUB_D, HUB_D,
                    ShapeKind.ELLIPSE));

            List<DiagramNode> items = hub.getChildren();
            int n = items.size();
            if (n == 0) {
                continue;
            }
            // Push the ring out when many satellites would otherwise overlap:
            // the chord between adjacent centres must clear a satellite width.
            int radius = BASE_RADIUS;
            if (n >= 2) {
                int rMin = (int) Math.ceil(
                        (SAT_W + MIN_SAT_GAP) / (2.0 * Math.sin(Math.PI / n)));
                radius = Math.max(BASE_RADIUS, rMin);
            }
            for (int i = 0; i < n; i++) {
                double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
                int scx = hubCX + (int) Math.round(radius * Math.cos(angle));
                int scy = CENTER_Y + (int) Math.round(radius * Math.sin(angle));
                int satIdx = out.addShape(new LaidOutShape(
                        BulletText.withTitle(items.get(i)), 2,
                        scx - SAT_W / 2, scy - SAT_H / 2, SAT_W, SAT_H,
                        ShapeKind.RECTANGLE));
                out.addEdge(new Edge(hubIdx, satIdx, -1, -1, true));
            }
        }
        return out;
    }
}
