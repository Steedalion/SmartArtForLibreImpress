package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;

import java.util.List;

/**
 * Lays out a Basic Block List: level-1 nodes become equal rectangles arranged
 * in a near-square grid that fills the slide, wrapping into rows. Level-2
 * children become bullet lines inside their parent block. No connectors are
 * drawn. Pure Java (no UNO). Units are 1/100 mm.
 */
public final class BlockListLayout {

    static final int SLIDE_W = 25400;
    static final int SLIDE_H = 19050;
    static final int MARGIN  = 2000;
    static final int GAP     = 400;

    private BlockListLayout() {}

    /**
     * @param root the synthetic root (level 0) produced by the parser; its
     *             level-1 children become the grid blocks. Each block's level-2
     *             children are rendered as bullet lines inside it.
     */
    public static DiagramLayout layout(DiagramNode root) {
        DiagramLayout out = new DiagramLayout();
        List<DiagramNode> blocks = root.getChildren();
        int n = blocks.size();
        if (n == 0) {
            return out;
        }

        int cols = (int) Math.ceil(Math.sqrt(n));
        int rows = (int) Math.ceil((double) n / cols);

        int availW = SLIDE_W - 2 * MARGIN;
        int availH = SLIDE_H - 2 * MARGIN;
        int blockW = (availW - (cols - 1) * GAP) / cols;
        int blockH = (availH - (rows - 1) * GAP) / rows;

        for (int i = 0; i < n; i++) {
            int row = i / cols;
            int col = i % cols;
            int x = MARGIN + col * (blockW + GAP);
            int y = MARGIN + row * (blockH + GAP);
            out.addShape(new LaidOutShape(
                    composeText(blocks.get(i)), 1, x, y, blockW, blockH,
                    ShapeKind.RECTANGLE));
        }

        return out;
    }

    /** Joins a node's title with its children as bullet lines. */
    static String composeText(DiagramNode node) {
        StringBuilder sb = new StringBuilder(node.getText());
        for (DiagramNode child : node.getChildren()) {
            sb.append("\n• ").append(child.getText());
        }
        return sb.toString();
    }
}
