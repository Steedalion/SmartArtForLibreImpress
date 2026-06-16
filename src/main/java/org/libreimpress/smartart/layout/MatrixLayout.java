package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Basic Matrix: the first up to four level-1 nodes become equal
 * rectangular quadrants of a 2x2 grid centred on the slide (order top-left,
 * top-right, bottom-left, bottom-right), each coloured distinctly. Any level-1
 * nodes beyond the fourth are ignored, and level-2 and deeper children become
 * nested, indented bullet lines inside their quadrant. No connectors are drawn.
 * Pure Java (no UNO). Units are 1/100 mm.
 */
public final class MatrixLayout {

    static final int SLIDE_W   = 25400;
    static final int SLIDE_H   = 19050;
    static final int TOTAL_W   = 18000;
    static final int TOTAL_H   = 13000;
    static final int CENTRE_GAP = 200;

    private MatrixLayout() {}

    /**
     * @param root the synthetic root (level 0) produced by the parser; its first
     *             four level-1 children become the quadrants. Extras are ignored.
     */
    public static DiagramLayout layout(DiagramNode root) {
        DiagramLayout out = new DiagramLayout();
        List<DiagramNode> nodes = root.getChildren();
        int n = Math.min(nodes.size(), 4);
        if (n == 0) {
            return out;
        }

        int cellW = (TOTAL_W - CENTRE_GAP) / 2;
        int cellH = (TOTAL_H - CENTRE_GAP) / 2;
        int left = (SLIDE_W - TOTAL_W) / 2;
        int top  = (SLIDE_H - TOTAL_H) / 2;
        int rightX = left + cellW + CENTRE_GAP;
        int botY   = top + cellH + CENTRE_GAP;

        int[] xs = { left, rightX, left, rightX };
        int[] ys = { top, top, botY, botY };

        for (int i = 0; i < n; i++) {
            out.addShape(new LaidOutShape(
                    BulletText.withTitle(nodes.get(i)), 1,
                    xs[i], ys[i], cellW, cellH, ShapeKind.MATRIX_CELL));
        }

        return out;
    }
}
