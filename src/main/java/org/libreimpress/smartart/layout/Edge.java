package org.libreimpress.smartart.layout;

/**
 * A parent→child connection, referenced by index into a
 * {@link DiagramLayout}'s shape list. Pure data — no UNO.
 */
public final class Edge {

    private final int parent;
    private final int child;

    public Edge(int parent, int child) {
        this.parent = parent;
        this.child = child;
    }

    public int getParent() {
        return parent;
    }

    public int getChild() {
        return child;
    }
}
