package org.libreimpress.smartart.layout;

/**
 * A node positioned by a layout: a box with text at an absolute position and
 * size (units are 1/100 mm, matching UNO). Pure data — no UNO.
 */
public final class LaidOutShape {

    private final String text;
    private final int level;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final ShapeKind kind;
    /** Counter-clockwise rotation in 1/100 degrees (LibreOffice RotateAngle convention) (0 = no rotation). */
    private final int rotateAngle100;
    /** Arc span in 1/100 degrees; only meaningful for CIRCULAR_ARROW shapes. */
    private final int arcSpan100;

    public LaidOutShape(String text, int level, int x, int y, int width, int height) {
        this(text, level, x, y, width, height, ShapeKind.RECTANGLE, 0, 0);
    }

    public LaidOutShape(String text, int level, int x, int y, int width, int height,
            ShapeKind kind) {
        this(text, level, x, y, width, height, kind, 0, 0);
    }

    public LaidOutShape(String text, int level, int x, int y, int width, int height,
            ShapeKind kind, int rotateAngle100, int arcSpan100) {
        this.text = text;
        this.level = level;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.kind = kind;
        this.rotateAngle100 = rotateAngle100;
        this.arcSpan100 = arcSpan100;
    }

    public String getText() { return text; }
    public int getLevel()   { return level; }
    public int getX()       { return x; }
    public int getY()       { return y; }
    public int getWidth()   { return width; }
    public int getHeight()  { return height; }
    public ShapeKind getKind() { return kind; }

    public int centerX() { return x + width / 2; }
    public int centerY() { return y + height / 2; }

    /** Counter-clockwise rotation in 1/100 degrees (LibreOffice RotateAngle convention) (0 = upright). */
    public int getRotateAngle100() { return rotateAngle100; }

    /** Arc span in 1/100 degrees (reserved for future arc-shaped types). */
    public int getArcSpan100() { return arcSpan100; }
}
