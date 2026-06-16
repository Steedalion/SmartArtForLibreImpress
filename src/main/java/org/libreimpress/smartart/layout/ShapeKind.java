package org.libreimpress.smartart.layout;

/** The UNO shape service a {@link LaidOutShape} should be drawn with. */
public enum ShapeKind {
    RECTANGLE,
    ELLIPSE,
    PENTAGON,         // Flat-left, pointed-right first step in Sequential Chevron
    CHEVRON,          // Notched-left, pointed-right intermediate step
    PYRAMID_TIER,     // Centered rectangle whose width encodes tier depth
    BLOCK_ARROW,      // "right-arrow" CustomShape rotated to point toward the next node
    VENN_CIRCLE,      // Translucent ellipse; overlapping circles read as a Venn diagram
    MATRIX_CELL       // Rectangle coloured per-sequence for a 2x2 matrix quadrant
}
