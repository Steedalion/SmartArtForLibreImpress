package org.libreimpress.smartart.layout;

/** The UNO shape service a {@link LaidOutShape} should be drawn with. */
public enum ShapeKind {
    RECTANGLE,
    ELLIPSE,
    PENTAGON,         // Flat-left, pointed-right first step in Sequential Chevron
    CHEVRON,          // Notched-left, pointed-right intermediate step
    PYRAMID_TIER      // Centered rectangle whose width encodes tier depth
}
