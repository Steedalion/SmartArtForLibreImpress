package org.libreimpress.smartart.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An optional per-level fill-colour palette supplied by the user.
 * Levels not present in the map fall back to the built-in {@code DefaultPalette}.
 */
public final class ColorPalette {

    /** Sentinel returned by {@link #getFillColor} when no entry exists for a level. */
    public static final int UNSET = -1;

    /** A palette with no entries — every level falls back to the default. */
    public static final ColorPalette EMPTY = new ColorPalette(new HashMap<>());

    private final Map<Integer, Integer> levelColors;

    public ColorPalette(Map<Integer, Integer> levelColors) {
        this.levelColors = Collections.unmodifiableMap(new HashMap<>(levelColors));
    }

    /**
     * Returns the user-specified fill colour for {@code level}, or
     * {@link #UNSET} ({@code -1}) if none was provided for that level.
     */
    public int getFillColor(int level) {
        Integer c = levelColors.get(level);
        return c != null ? c : UNSET;
    }

    public boolean isEmpty() {
        return levelColors.isEmpty();
    }
}
