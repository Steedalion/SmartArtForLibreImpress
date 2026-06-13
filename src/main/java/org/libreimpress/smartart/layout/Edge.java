package org.libreimpress.smartart.layout;

/**
 * A parent→child connection, referenced by index into a
 * {@link DiagramLayout}'s shape list. Pure data — no UNO.
 *
 * <p>Glue point indices follow the UNO convention:
 * 0 = top, 1 = right, 2 = bottom, 3 = left, -1 = auto-select.
 */
public final class Edge {

    private final int parent;
    private final int child;
    private final int startGlue;
    private final int endGlue;
    private final boolean straight;

    /** Auto-routed elbowed connector (glue points chosen by LibreOffice). */
    public Edge(int parent, int child) {
        this(parent, child, -1, -1, false);
    }

    public Edge(int parent, int child, int startGlue, int endGlue) {
        this(parent, child, startGlue, endGlue, false);
    }

    public Edge(int parent, int child, int startGlue, int endGlue, boolean straight) {
        this.parent = parent;
        this.child = child;
        this.startGlue = startGlue;
        this.endGlue = endGlue;
        this.straight = straight;
    }

    public int getParent() {
        return parent;
    }

    public int getChild() {
        return child;
    }

    /** Glue point on the parent shape, or -1 for auto. */
    public int getStartGlue() {
        return startGlue;
    }

    /** Glue point on the child shape, or -1 for auto. */
    public int getEndGlue() {
        return endGlue;
    }

    /** True when the connector should be a straight LINE rather than elbowed. */
    public boolean isStraight() {
        return straight;
    }
}
