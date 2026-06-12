package org.libreimpress.smartart.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A node in the parsed diagram hierarchy.
 *
 * <p>The parser produces a synthetic root at {@code level == 0} whose children
 * are the level-1 (top-level) items. This class has no UNO dependencies so it
 * can be unit-tested without LibreOffice.
 */
public class DiagramNode {

    private final String text;
    private final int level;
    private DiagramNode parent;
    private final List<DiagramNode> children = new ArrayList<>();

    public DiagramNode(String text, int level) {
        this.text = text;
        this.level = level;
    }

    public String getText() {
        return text;
    }

    /** 0 = synthetic root, 1 = top-level item, 2+ = nested. */
    public int getLevel() {
        return level;
    }

    public DiagramNode getParent() {
        return parent;
    }

    public List<DiagramNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean isRoot() {
        return level == 0;
    }

    public void addChild(DiagramNode child) {
        child.parent = this;
        children.add(child);
    }

    /** Number of real nodes in this subtree, excluding the synthetic root. */
    public int countDescendants() {
        int count = isRoot() ? 0 : 1;
        for (DiagramNode child : children) {
            count += child.countDescendants();
        }
        return count;
    }

    /**
     * The deepest level number present in this subtree. For the synthetic root
     * this equals the number of levels in the whole tree (e.g. 3 for a tree
     * containing level-1, level-2 and level-3 items).
     */
    public int depth() {
        int max = level;
        for (DiagramNode child : children) {
            max = Math.max(max, child.depth());
        }
        return max;
    }
}
