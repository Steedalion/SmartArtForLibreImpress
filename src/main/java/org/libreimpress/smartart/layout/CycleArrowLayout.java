package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Cycle (Arrows) diagram: level-1 nodes as LibreOffice
 * {@code circular-arrow} block-arrow shapes arranged concentrically so that
 * they tile a full 360°. Each arrow is rotated by {@code i × (360°/n)} and
 * spans {@code (360°/n − GAP°)} to leave a small visible gap between
 * adjacent arrows. Sub-items are ignored. Pure Java (no UNO). Units 1/100 mm.
 */
public final class CycleArrowLayout {

    /** Gap between adjacent arrows in 1/100 degrees. */
    static final int GAP100 = 500;  // 5°

    /** Side length of the square bounding box that contains all arrows. */
    static final int BBOX = 14000;

    static final int SLIDE_W = 25400;
    static final int SLIDE_H = 19050;

    private CycleArrowLayout() {
    }

    /**
     * @param root synthetic root (level 0); its level-1 children become the
     *             cycle arrows. Deeper descendants are ignored.
     */
    public static DiagramLayout layout(DiagramNode root) {
        DiagramLayout out = new DiagramLayout();
        List<DiagramNode> nodes = root.getChildren();
        int n = nodes.size();
        if (n == 0) {
            return out;
        }

        // All circular-arrow shapes share the same bounding box (centred on the slide).
        int bboxX = (SLIDE_W - BBOX) / 2;
        int bboxY = (SLIDE_H - BBOX) / 2;

        int sectorAngle100 = 36000 / n;          // full sector in 1/100 degrees
        int arcSpan100     = sectorAngle100 - GAP100;  // each arrow covers this much

        for (int i = 0; i < n; i++) {
            DiagramNode node = nodes.get(i);
            int rotate100 = i * sectorAngle100;  // clockwise rotation for this arrow
            LaidOutShape shape = new LaidOutShape(
                    node.getText(), 1,
                    bboxX, bboxY, BBOX, BBOX,
                    ShapeKind.CIRCULAR_ARROW,
                    rotate100, arcSpan100);
            out.addShape(shape);
        }

        // No connectors — the arrows themselves form the cycle.
        return out;
    }
}
