package org.libreimpress.smartart.parsers;

import org.libreimpress.smartart.models.DiagramNode;

/**
 * Outcome of parsing hierarchy text: either a valid tree (its synthetic root)
 * or a human-readable error message. Immutable.
 */
public final class ParseResult {

    private final DiagramNode root;
    private final String errorMessage;

    private ParseResult(DiagramNode root, String errorMessage) {
        this.root = root;
        this.errorMessage = errorMessage;
    }

    public static ParseResult ok(DiagramNode root) {
        return new ParseResult(root, null);
    }

    public static ParseResult error(String message) {
        return new ParseResult(null, message);
    }

    public boolean isValid() {
        return errorMessage == null;
    }

    /** The synthetic root (level 0) when {@link #isValid()}; otherwise null. */
    public DiagramNode getRoot() {
        return root;
    }

    /** The error message when invalid; otherwise null. */
    public String getErrorMessage() {
        return errorMessage;
    }
}
