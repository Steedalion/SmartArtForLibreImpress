package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Target diagram: level-1 nodes become concentric circles centred
 * on the slide, the first item outermost and the last item the bullseye
 * (PowerPoint's Target convention). Circles are emitted outermost-first so the
 * renderer's insertion order stacks each smaller ring on top of the previous
 * one; labels sit in the exposed top band of each ring (the renderer anchors
 * {@link ShapeKind#TARGET_RING} text to the top). Deeper descendants are
 * silently ignored, mirroring {@link VennLayout}. No connectors. Pure Java
 * (no UNO). Units are 1/100 mm.
 */
public final class TargetLayout {

    static final int SLIDE_W = 25400;
    static final int SLIDE_H = 19050;

    /**
     * Outer diameter of the outermost ring. Sized so the bottom half fits
     * below the assumed centre line even on the default 16:9 page
     * (28000×15750): 9525 + MAX_D/2 must stay within 15750.
     */
    static final int MAX_D = 12000;
    /** Diameter of the innermost circle when there are two or more rings. */
    static final int MIN_D = 4200;
    /** Diameter of the single circle when there is exactly one item. */
    static final int SINGLE_D = 8000;

    private TargetLayout() {}

    /**
     * @param root the synthetic root (level 0) produced by the parser; its
     *             level-1 children become the rings, first = outermost.
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

        for (int i = 0; i < n; i++) {
            int d = (n == 1)
                    ? SINGLE_D
                    : MAX_D - (int) Math.round((MAX_D - MIN_D) * (double) i / (n - 1));
            out.addShape(new LaidOutShape(nodes.get(i).getText(), 1,
                    cx - d / 2, cy - d / 2, d, d, ShapeKind.TARGET_RING));
        }
        return out;
    }
}
