package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a node's descendants as nested, indented bullet lines for the
 * list-style diagrams (Basic Block List, Vertical Bullet List, Basic Matrix).
 * Every level below the node is emitted — depth is no longer capped at one — so
 * level-3 and deeper items are indented under their parent rather than dropped.
 * Pure Java (no UNO).
 */
final class BulletText {

    /** Bullet glyph per depth; the deepest glyph repeats for any further levels. */
    private static final String[] MARKERS = {"•", "◦", "▪"};
    /** Indent prepended once per depth level. */
    private static final String INDENT = "    ";

    private BulletText() {}

    /**
     * The node's own text as the first line, followed by its nested descendant
     * bullets. Used where the item's title shares a box with its sub-items
     * (Block List blocks, Matrix cells).
     */
    static String withTitle(DiagramNode node) {
        List<String> lines = new ArrayList<>();
        lines.add(node.getText());
        collect(lines, node, 0);
        return String.join("\n", lines);
    }

    /**
     * The node's nested descendant bullets only (no title line). Used where the
     * title lives in a separate shape (Vertical Bullet List content boxes).
     */
    static String bullets(DiagramNode node) {
        List<String> lines = new ArrayList<>();
        collect(lines, node, 0);
        return String.join("\n", lines);
    }

    private static void collect(List<String> lines, DiagramNode node, int depth) {
        for (DiagramNode child : node.getChildren()) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                line.append(INDENT);
            }
            line.append(MARKERS[Math.min(depth, MARKERS.length - 1)])
                .append(' ')
                .append(child.getText());
            lines.add(line.toString());
            collect(lines, child, depth + 1);
        }
    }
}
