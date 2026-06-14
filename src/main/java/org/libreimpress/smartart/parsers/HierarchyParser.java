package org.libreimpress.smartart.parsers;

import org.libreimpress.smartart.models.DiagramNode;

/**
 * Parses dash-prefix multi-line text into a {@link DiagramNode} tree and
 * validates it against diagram constraints. Pure Java (no UNO).
 *
 * <p>Format: level is expressed by the number of leading {@code -} characters
 * followed by one space, e.g.:
 * <pre>
 *   Root
 *   - Child
 *   -- Grandchild
 * </pre>
 * Zero dashes = level 1; N dashes = level N+1.
 */
public class HierarchyParser {

    public static final int MIN_LEVELS = 1;
    public static final int MIN_NODES = 3;

    public ParseResult parse(String input) {
        if (input == null) {
            input = "";
        }
        String[] lines = input.split("\n", -1);

        DiagramNode root = new DiagramNode("", 0);
        // lastAtDepth[d] = the most-recently-seen node at dash-depth d
        DiagramNode[] lastAtDepth = new DiagramNode[64];

        int nonBlank = 0;
        int lineNo = 0;
        int prevDepth = -1;

        for (String raw : lines) {
            lineNo++;
            if (raw.trim().isEmpty()) {
                continue;
            }
            nonBlank++;

            int depth = countLeadingDashes(raw);
            String text = stripDashPrefix(raw, depth);

            if (nonBlank == 1 && depth != 0) {
                return ParseResult.error(
                        "The first line must not have a '-' prefix (line " + lineNo + ").");
            }

            if (depth > prevDepth + 1) {
                return ParseResult.error(
                        "Level jump at line " + lineNo + ": depth " + depth
                        + " follows depth " + prevDepth
                        + " — introduce each level through its parent first.");
            }

            int level = depth + 1;
            DiagramNode parent = (depth == 0) ? root : lastAtDepth[depth - 1];
            DiagramNode node = new DiagramNode(text, level);
            parent.addChild(node);
            lastAtDepth[depth] = node;
            prevDepth = depth;
        }

        if (nonBlank == 0) {
            return ParseResult.error("Please enter at least one line of text.");
        }

        int nodes = root.countDescendants();
        if (nodes < MIN_NODES) {
            return ParseResult.error(
                    "A diagram needs at least " + MIN_NODES + " nodes (found " + nodes + ").");
        }

        int levels = root.depth();
        if (levels < MIN_LEVELS) {
            return ParseResult.error(
                    "A diagram needs at least " + MIN_LEVELS
                            + " level (found " + levels + ").");
        }

        return ParseResult.ok(root);
    }

    /**
     * Renders a parsed tree back to the dash-prefix format so the output can
     * be round-tripped through {@link #parse}.
     */
    public static String toOutline(DiagramNode root) {
        StringBuilder sb = new StringBuilder();
        for (DiagramNode child : root.getChildren()) {
            appendOutline(sb, child);
        }
        return sb.toString();
    }

    private static void appendOutline(StringBuilder sb, DiagramNode node) {
        int depth = node.getLevel() - 1;
        for (int i = 0; i < depth; i++) {
            sb.append('-');
        }
        if (depth > 0) {
            sb.append(' ');
        }
        sb.append(node.getText()).append('\n');
        for (DiagramNode child : node.getChildren()) {
            appendOutline(sb, child);
        }
    }

    /** Returns the number of leading {@code -} characters on a line. */
    static int countLeadingDashes(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == '-') {
            i++;
        }
        return i;
    }

    /**
     * Strips the leading {@code depth} dashes and one optional space, then
     * trims the remainder.
     */
    static String stripDashPrefix(String line, int depth) {
        String s = line.substring(depth);
        if (s.startsWith(" ")) {
            s = s.substring(1);
        }
        return s.trim();
    }
}
