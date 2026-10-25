package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Basic Timeline: a thin horizontal spine across the slide with one
 * circular marker per level-1 node, evenly spaced left-to-right in list order.
 * Each marker gets a label box connected by a short straight line, alternating
 * above and below the spine so long labels don't collide. Level-2 and deeper
 * children appear as nested bullet lines under the title inside the label box
 * (via {@link BulletText}). Pure Java (no UNO). Units are 1/100 mm.
 */
public final class TimelineLayout {

    static final int SLIDE_W = 25400;
    static final int SPINE_Y = 9525;   // vertical centre of the slide
    static final int SPINE_H = 250;
    static final int MARGIN_X = 2200;  // spine inset from the slide edges
    static final int MARKER_D = 1200;
    static final int LABEL_H = 3200;
    static final int MAX_LABEL_W = 5200;
    static final int LABEL_GAP = 900;  // marker edge → label box edge
    static final int MIN_LABEL_SPACING = 400; // gap between adjacent label boxes

    private TimelineLayout() {}

    /**
     * @param root the synthetic root (level 0) produced by the parser; its
     *             level-1 children become the timeline events in order.
     */
    public static DiagramLayout layout(DiagramNode root) {
        DiagramLayout out = new DiagramLayout();
        List<DiagramNode> events = root.getChildren();
        int n = events.size();
        if (n == 0) {
            return out;
        }

        int spineW = SLIDE_W - 2 * MARGIN_X;
        // Spine first so markers and labels draw on top of it.
        out.addShape(new LaidOutShape("", 3,
                MARGIN_X, SPINE_Y - SPINE_H / 2, spineW, SPINE_H,
                ShapeKind.RECTANGLE));

        // Adjacent labels alternate sides, so boxes on the same side are two
        // marker-spacings apart — the width cap only needs to clear that.
        int spacing = (n == 1) ? 0 : spineW / (n - 1);
        int labelW = Math.min(MAX_LABEL_W,
                (n == 1) ? MAX_LABEL_W : 2 * spacing - MIN_LABEL_SPACING);

        for (int i = 0; i < n; i++) {
            int mcx = (n == 1) ? SLIDE_W / 2 : MARGIN_X + i * spacing;
            int markerIdx = out.addShape(new LaidOutShape("", 2,
                    mcx - MARKER_D / 2, SPINE_Y - MARKER_D / 2,
                    MARKER_D, MARKER_D, ShapeKind.TIMELINE_MARKER));

            boolean above = (i % 2 == 0);
            int labelY = above
                    ? SPINE_Y - MARKER_D / 2 - LABEL_GAP - LABEL_H
                    : SPINE_Y + MARKER_D / 2 + LABEL_GAP;
            // Keep the outermost labels on the slide instead of centring them
            // on markers that sit close to the edge.
            int labelX = Math.max(300,
                    Math.min(SLIDE_W - 300 - labelW, mcx - labelW / 2));
            int labelIdx = out.addShape(new LaidOutShape(
                    BulletText.withTitle(events.get(i)), 1,
                    labelX, labelY, labelW, LABEL_H,
                    ShapeKind.RECTANGLE));
            out.addEdge(new Edge(markerIdx, labelIdx, -1, -1, true));
        }
        return out;
    }
}
