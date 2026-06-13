package org.libreimpress.smartart.editing;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java text transforms that make a plain multi-line field behave like an
 * indented outline: Tab indents the touched line(s), Shift+Tab outdents them,
 * and Enter starts a new line at the current indent. One indent level is
 * {@link #INDENT} (four spaces), which matches the parser's space-indentation
 * model. No UNO dependencies, so this is fully unit-testable; {@code SmartArtDialog}
 * wires it to the dialog's edit control via a key handler.
 */
public final class OutlineEditor {

    /** One indentation level. */
    public static final String INDENT = "    ";

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

    private OutlineEditor() {
    }

    /** Indents every line touched by the selection by one level. */
    public static Edit indent(String text, int selStart, int selEnd) {
        int[] n = normalize(text, selStart, selEnd);
        int s = n[0];
        int e = n[1];
        int[] starts = lineStarts(text);
        int first = lineIndexOf(starts, s);
        int last = lineIndexOf(starts, e);
        if (e > s && last > first && starts[last] == e) {
            last--; // selection ends exactly at a line start: that line is untouched
        }

        List<Integer> affected = new ArrayList<>();
        for (int i = first; i <= last; i++) {
            affected.add(starts[i]);
        }

        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (idx < affected.size() && affected.get(idx) == i) {
                sb.append(INDENT);
                idx++;
            }
            if (i < text.length()) {
                sb.append(text.charAt(i));
            }
        }

        int ns = s + INDENT.length() * countLE(affected, s);
        int ne = e + INDENT.length() * countLE(affected, e);
        return new Edit(sb.toString(), ns, ne);
    }

    /** Removes up to one indent level of leading spaces from each touched line. */
    public static Edit outdent(String text, int selStart, int selEnd) {
        int[] n = normalize(text, selStart, selEnd);
        int s = n[0];
        int e = n[1];
        int[] starts = lineStarts(text);
        int first = lineIndexOf(starts, s);
        int last = lineIndexOf(starts, e);
        if (e > s && last > first && starts[last] == e) {
            last--;
        }

        int[] removed = new int[starts.length];
        for (int li = first; li <= last; li++) {
            int ls = starts[li];
            int k = 0;
            while (k < INDENT.length() && ls + k < text.length() && text.charAt(ls + k) == ' ') {
                k++;
            }
            removed[li] = k;
        }

        StringBuilder sb = new StringBuilder();
        for (int li = 0; li < starts.length; li++) {
            int ls = starts[li];
            int le = (li + 1 < starts.length) ? starts[li + 1] : text.length();
            sb.append(text, ls + removed[li], le);
        }

        int ns = s - removedBefore(starts, removed, s);
        int ne = e - removedBefore(starts, removed, e);
        return new Edit(sb.toString(), ns, ne);
    }

    /**
     * Replaces the selection with a newline followed by the current line's
     * leading indentation, so a new item stays at the same level as the line
     * it was started from.
     */
    public static Edit newlineKeepingIndent(String text, int selStart, int selEnd) {
        int[] n = normalize(text, selStart, selEnd);
        int s = n[0];
        int e = n[1];
        String merged = text.substring(0, s) + text.substring(e);
        int caret = s;
        int ls = lineStartOf(merged, caret);
        int k = 0;
        while (ls + k < merged.length() && merged.charAt(ls + k) == ' ') {
            k++;
        }
        String leading = repeat(' ', k);
        String out = merged.substring(0, caret) + "\n" + leading + merged.substring(caret);
        int caretOut = caret + 1 + k;
        return new Edit(out, caretOut, caretOut);
    }

    // --- helpers ---------------------------------------------------------

    private static int[] normalize(String text, int s, int e) {
        int len = text.length();
        s = Math.max(0, Math.min(s, len));
        e = Math.max(0, Math.min(e, len));
        if (s > e) {
            int t = s;
            s = e;
            e = t;
        }
        return new int[] { s, e };
    }

    private static int[] lineStarts(String text) {
        List<Integer> list = new ArrayList<>();
        list.add(0);
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                list.add(i + 1);
            }
        }
        int[] arr = new int[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private static int lineIndexOf(int[] starts, int offset) {
        int idx = 0;
        for (int i = 0; i < starts.length; i++) {
            if (starts[i] <= offset) {
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

    private static int countLE(List<Integer> values, int x) {
        int c = 0;
        for (int v : values) {
            if (v <= x) {
                c++;
            }
        }
        return c;
    }

    private static int removedBefore(int[] starts, int[] removed, int offset) {
        int d = 0;
        for (int li = 0; li < starts.length; li++) {
            if (removed[li] == 0) {
                continue;
            }
            int ls = starts[li];
            if (offset > ls) {
                d += Math.min(offset - ls, removed[li]);
            }
        }
        return d;
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
