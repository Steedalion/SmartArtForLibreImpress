package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Basic Venn diagram: level-1 nodes become equal circles arranged so
 * that adjacent circles overlap, around the slide centre. The renderer draws
 * {@link ShapeKind#VENN_CIRCLE} with partial transparency so the overlaps read.
 * Deeper descendants are ignored (only level-1 content is meaningful), mirroring
 * {@link CycleLayout}. No connectors are drawn. Pure Java (no UNO). Units are
 * 1/100 mm.
 */
public final class VennLayout {

    static final int SLIDE_W = 25400;
    static final int SLIDE_H = 19050;

    private VennLayout() {}

    /**
     * @param root the synthetic root (level 0) produced by the parser; its
     *             level-1 children become the overlapping circles. Deeper
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

        // Circle radius shrinks as more circles are added so they stay readable.
        int r;
        if (n <= 3) {
            r = 4500;
        } else if (n <= 5) {
            r = 3800;
        } else {
            r = 3200;
        }
        int d = 2 * r;

        if (n == 1) {
            out.addShape(circle(nodes.get(0).getText(), cx, cy, r, d));
            return out;
        }

        // Place circle centres on a ring small enough that neighbours overlap.
        double ringFactor = (n == 2) ? 0.65 : 0.62;
        int ringR = (int) Math.round(r * ringFactor);
        // n==2 sits horizontally (left, right); n>=3 starts at the top, clockwise.
        double startAngle = (n == 2) ? Math.PI : -Math.PI / 2;

        for (int i = 0; i < n; i++) {
            double angle = startAngle + 2 * Math.PI * i / n;
            int ccx = cx + (int) Math.round(ringR * Math.cos(angle));
            int ccy = cy + (int) Math.round(ringR * Math.sin(angle));
            out.addShape(circle(nodes.get(i).getText(), ccx, ccy, r, d));
        }

        return out;
    }

    private static LaidOutShape circle(String text, int ccx, int ccy, int r, int d) {
        return new LaidOutShape(text, 1, ccx - r, ccy - r, d, d, ShapeKind.VENN_CIRCLE);
    }
}
