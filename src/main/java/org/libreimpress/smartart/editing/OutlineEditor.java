package org.libreimpress.smartart.editing;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java text transforms that make a plain multi-line field behave like an
 * indented outline using the dash-prefix format:
 * <pre>
 *   Root
 *   - Child        (depth 1: one dash + space)
 *   -- Grandchild  (depth 2: two dashes + space)
 * </pre>
 * {@link #indent} adds one dash level; {@link #outdent} removes one dash level.
 * No UNO dependencies — fully unit-testable.
 */
public final class OutlineEditor {

    private OutlineEditor() {
    }

    /** Result of an edit: the new text and the new selection/caret offsets. */
    public static final class Edit {
        public final String text;
        public final int selStart;
        public final int selEnd;

        public Edit(String text, int selStart, int selEnd) {
            this.text = text;
            this.selStart = selStart;
            this.selEnd = selEnd;
        }
    }

    // -------------------------------------------------------------------------
    // Public operations
    // -------------------------------------------------------------------------

    /** Increases the dash depth of every line touched by the selection by one. */
    public static Edit indent(String text, int selStart, int selEnd) {
        return transformDepth(text, selStart, selEnd, +1);
    }

    /** Decreases the dash depth of every line touched by the selection by one (minimum 0). */
    public static Edit outdent(String text, int selStart, int selEnd) {
        return transformDepth(text, selStart, selEnd, -1);
    }

    /**
     * Replaces the selection with a newline followed by the current line's
     * dash prefix, so a new item appears at the same depth as the current one.
     */
    public static Edit newlineKeepingIndent(String text, int selStart, int selEnd) {
        int[] n = normalize(text, selStart, selEnd);
        int s = n[0];
        int e = n[1];
        String merged = text.substring(0, s) + text.substring(e);
        int caret = s;
        int ls = lineStartOf(merged, caret);
        int depth = dashDepth(merged.substring(ls));
        String newPrefix = prefix(depth);
        String out = merged.substring(0, caret) + "\n" + newPrefix + merged.substring(caret);
        int caretOut = caret + 1 + newPrefix.length();
        return new Edit(out, caretOut, caretOut);
    }

    // -------------------------------------------------------------------------
    // Package-visible helpers (used by HierarchyParser and tests)
    // -------------------------------------------------------------------------

    /** Returns the dash-prefix string for a given depth (0 → "", 1 → "- ", 2 → "-- ", …). */
    static String prefix(int depth) {
        if (depth == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(depth + 1);
        for (int i = 0; i < depth; i++) {
            sb.append('-');
        }
        sb.append(' ');
        return sb.toString();
    }

    /** Counts leading {@code -} characters (ignores everything after them). */
    static int dashDepth(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == '-') {
            i++;
        }
        return i;
    }

    /** Strips the leading dashes and one optional space, returning bare content. */
    static String bareText(String line) {
        int d = dashDepth(line);
        String rest = line.substring(d);
        if (rest.startsWith(" ")) {
            rest = rest.substring(1);
        }
        return rest;
    }

    // -------------------------------------------------------------------------
    // Private implementation
    // -------------------------------------------------------------------------

    private static Edit transformDepth(String text, int selStart, int selEnd, int delta) {
        int[] n = normalize(text, selStart, selEnd);
        int s = n[0];
        int e = n[1];

        List<String> lines = new ArrayList<>();
        List<Integer> lineStarts = new ArrayList<>();
        splitLines(text, lines, lineStarts);

        int first = lineAt(lineStarts, s);
        int last  = lineAt(lineStarts, e);
        // If selection ends exactly at a line boundary, that line is untouched.
        if (e > s && last > first && lineStarts.get(last).intValue() == e) {
            last--;
        }

        List<String> newLines = new ArrayList<>(lines);
        int[] prefixChange = new int[lines.size()];

        for (int i = first; i <= last; i++) {
            String line = lines.get(i);
            int oldDepth = dashDepth(line);
            int newDepth = Math.max(0, oldDepth + delta);
            newLines.set(i, prefix(newDepth) + bareText(line));
            prefixChange[i] = prefix(newDepth).length() - prefix(oldDepth).length();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < newLines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(newLines.get(i));
        }

        int ns = adjustCaret(s, lines, lineStarts, prefixChange, first, last);
        int ne = adjustCaret(e, lines, lineStarts, prefixChange, first, last);
        return new Edit(sb.toString(), ns, ne);
    }

    /**
     * Recomputes a caret offset after prefix lengths have changed on lines
     * {@code first..last}.
     */
    private static int adjustCaret(int offset, List<String> lines, List<Integer> lineStarts,
            int[] prefixChange, int first, int last) {
        int lineIdx = lineAt(lineStarts, offset);

        // Sum prefix changes from affected lines that come before the caret line.
        int cumDelta = 0;
        for (int i = first; i < lineIdx && i <= last; i++) {
            cumDelta += prefixChange[i];
        }

        // If the caret is on an affected line, handle intra-line adjustment.
        if (lineIdx >= first && lineIdx <= last && prefixChange[lineIdx] != 0) {
            int ls = lineStarts.get(lineIdx);
            int posInLine = offset - ls;
            int oldPrefLen = prefix(dashDepth(lines.get(lineIdx))).length();
            if (posInLine <= oldPrefLen) {
                // Caret was inside the old prefix — snap to end of the new prefix.
                int newPrefLen = Math.max(0, oldPrefLen + prefixChange[lineIdx]);
                return ls + cumDelta + newPrefLen;
            }
            cumDelta += prefixChange[lineIdx];
        }

        return offset + cumDelta;
    }

    private static void splitLines(String text, List<String> lines, List<Integer> starts) {
        starts.add(0);
        int p = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines.add(text.substring(p, i));
                starts.add(i + 1);
                p = i + 1;
            }
        }
        lines.add(text.substring(p));
    }

    private static int lineAt(List<Integer> starts, int offset) {
        int idx = 0;
        for (int i = 0; i < starts.size(); i++) {
            if (starts.get(i) <= offset) {
                idx = i;
            } else {
                break;
            }
        }
        return idx;
    }

    private static int lineStartOf(String text, int offset) {
        int start = 0;
        for (int i = 0; i < offset && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                start = i + 1;
            }
        }
        return start;
    }

    private static int[] normalize(String text, int s, int e) {
        int len = text.length();
        s = Math.max(0, Math.min(s, len));
        e = Math.max(0, Math.min(e, len));
        if (s > e) {
            int t = s;
            s = e;
            e = t;
        }
        return new int[]{ s, e };
    }
}
