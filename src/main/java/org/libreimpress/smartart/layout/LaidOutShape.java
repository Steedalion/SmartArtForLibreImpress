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

    public LaidOutShape(String text, int level, int x, int y, int width, int height) {
        this(text, level, x, y, width, height, ShapeKind.RECTANGLE);
    }

    public LaidOutShape(String text, int level, int x, int y, int width, int height,
            ShapeKind kind) {
        this.text = text;
        this.level = level;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.kind = kind;
    }

    public String getText() {
        return text;
    }

    public int getLevel() {
        return level;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }

    public ShapeKind getKind() {
        return kind;
    }
}
