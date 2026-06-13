package org.libreimpress.smartart.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of laying out a diagram: positioned shapes and the connectors
 * between them (by shape index). Pure data — no UNO; a renderer turns it into
 * real shapes.
 */
public final class DiagramLayout {

    private final List<LaidOutShape> shapes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();

    public int addShape(LaidOutShape shape) {
        shapes.add(shape);
        return shapes.size() - 1;
    }

    public void addEdge(int parentIndex, int childIndex) {
        edges.add(new Edge(parentIndex, childIndex));
    }

    public void addEdge(int parentIndex, int childIndex, int startGlue, int endGlue) {
        edges.add(new Edge(parentIndex, childIndex, startGlue, endGlue));
    }

    public List<LaidOutShape> getShapes() {
        return Collections.unmodifiableList(shapes);
    }

    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }
}
