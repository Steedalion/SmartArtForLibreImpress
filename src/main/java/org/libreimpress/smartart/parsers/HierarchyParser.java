package org.libreimpress.smartart.parsers;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.libreimpress.smartart.models.DiagramNode;

/**
 * Parses indented multi-line text into a {@link DiagramNode} tree using standard
 * outline semantics, and validates it against the diagram constraints. Pure
 * Java (no UNO) so it is fully unit-testable.
 *
 * <p>See {@code Phase3_ImplementationPlan.md} §2 for the indentation model.
 */
public class HierarchyParser {

    public static final int MIN_LEVELS = 3;
    public static final int MIN_NODES = 3;

    private static final class Frame {
        final int indent;
        final DiagramNode node;

        Frame(int indent, DiagramNode node) {
            this.indent = indent;
            this.node = node;
        }
    }

    public ParseResult parse(String input) {
        if (input == null) {
            input = "";
        }
        String[] lines = input.split("\n", -1);

        DiagramNode root = new DiagramNode("", 0);
        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(-1, root));
        Map<Integer, Integer> levelIndent = new HashMap<>();

        boolean usesTab = false;
        boolean usesSpace = false;
        int lineNo = 0;
        int nonBlank = 0;

        for (String raw : lines) {
            lineNo++;
            if (raw.trim().isEmpty()) {
                continue;
            }
            nonBlank++;

            int indent = leadingWhitespace(raw);
            for (int i = 0; i < indent; i++) {
                char c = raw.charAt(i);
                if (c == '\t') {
                    usesTab = true;
                } else if (c == ' ') {
                    usesSpace = true;
                }
            }
            if (usesTab && usesSpace) {
                return ParseResult.error(
                        "Inconsistent indentation: mix of tabs and spaces (line "
                                + lineNo + "). Use one or the other.");
            }

            if (nonBlank == 1 && indent != 0) {
                return ParseResult.error(
                        "The first line must not be indented (line " + lineNo + ").");
            }

            while (stack.size() > 1 && indent < stack.peek().indent) {
                stack.pop();
            }

            int newLevel;
            if (indent == stack.peek().indent) {
                // Sibling of the current top: drop it so the parent becomes top.
                stack.pop();
                newLevel = stack.peek().node.getLevel() + 1;
            } else {
                // indent > top.indent: one level deeper than the line above.
                newLevel = stack.peek().node.getLevel() + 1;
            }

            Integer canonical = levelIndent.get(newLevel);
            if (canonical == null) {
                levelIndent.put(newLevel, indent);
            } else if (canonical.intValue() != indent) {
                return ParseResult.error(
                        "Inconsistent indentation (line " + lineNo + "): expected "
                                + canonical + " indent characters for level " + newLevel
                                + ", found " + indent + ".");
            }

            DiagramNode parent = stack.peek().node;
            DiagramNode node = new DiagramNode(raw.trim(), newLevel);
            parent.addChild(node);
            stack.push(new Frame(indent, node));
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
                            + " levels of indentation (found " + levels + ").");
        }

        return ParseResult.ok(root);
    }

    /** Renders a parsed tree as an indented bullet list for display. */
    public static String toOutline(DiagramNode root) {
        StringBuilder sb = new StringBuilder();
        for (DiagramNode child : root.getChildren()) {
            appendOutline(sb, child);
        }
        return sb.toString();
    }

    private static void appendOutline(StringBuilder sb, DiagramNode node) {
        for (int i = 1; i < node.getLevel(); i++) {
            sb.append("    ");
        }
        sb.append("• ").append(node.getText()).append('\n');
        for (DiagramNode child : node.getChildren()) {
            appendOutline(sb, child);
        }
    }

    private static int leadingWhitespace(String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }
}
