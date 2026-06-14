package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Pyramid diagram: level-1 nodes become centre-aligned rectangular
 * tiers stacked top-to-bottom, narrowest at the apex and widest at the base.
 * Level-2+ children are stacked vertically to the right of their parent tier.
 * No connectors are drawn. Units are 1/100 mm.
 */
public final class PyramidLayout {

    static final int TIER_H     = 1600;
    static final int GAP        = 150;
    static final int MAX_W      = 18000;
    static final int MIN_W      = 3000;
    static final int TOP_Y      = 2000;
    static final int SLIDE_W    = 25400;
    static final int CHILD_W    = 3000;
    static final int CHILD_H    = 900;
    static final int CHILD_GAP  = 300;
    static final int CHILD_V_GAP = 150;

    private PyramidLayout() {}

    /**
     * @param root the synthetic root (level 0) produced by the parser; its
     *             children become the tiers (index 0 = apex, last = base).
     */
    public static DiagramLayout layout(DiagramNode root) {
        DiagramLayout out = new DiagramLayout();
        List<DiagramNode> tiers = root.getChildren();
        int n = tiers.size();
        if (n == 0) {
            return out;
        }

        int[] tierWidths = new int[n];
        int[] tierXs     = new int[n];
        int[] tierYs     = new int[n];

        for (int i = 0; i < n; i++) {
            int w = (n == 1) ? MAX_W : MIN_W + (MAX_W - MIN_W) * i / (n - 1);
            int x = (SLIDE_W - w) / 2;
            int y = TOP_Y + i * (TIER_H + GAP);
            tierWidths[i] = w;
            tierXs[i]     = x;
            tierYs[i]     = y;
            out.addShape(new LaidOutShape(
                    tiers.get(i).getText(), i + 1, x, y, w, TIER_H, ShapeKind.PYRAMID_TIER));
        }

        // Child column is anchored to the right edge of the base (widest tier)
        // so that all children share a consistent column regardless of tier width.
        int childColumnX = (SLIDE_W + MAX_W) / 2 + CHILD_GAP;

        for (int i = 0; i < n; i++) {
            List<DiagramNode> children = tiers.get(i).getChildren();
            if (children.isEmpty()) {
                continue;
            }
            int nc = children.size();
            int totalH = nc * CHILD_H + (nc - 1) * CHILD_V_GAP;
            int childY = tierYs[i] + (TIER_H - totalH) / 2;
            for (DiagramNode child : children) {
                out.addShape(new LaidOutShape(
                        child.getText(), child.getLevel(), childColumnX, childY,
                        CHILD_W, CHILD_H, ShapeKind.RECTANGLE));
                childY += CHILD_H + CHILD_V_GAP;
            }
        }

        return out;
    }
}
