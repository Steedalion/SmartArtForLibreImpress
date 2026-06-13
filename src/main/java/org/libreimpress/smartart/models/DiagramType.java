package org.libreimpress.smartart.models;

/**
 * The diagram layouts offered in the dialog. The {@link #getLabel() labels}
 * populate the type dropdown; {@link #fromIndex(int)} maps the selected index
 * back to a value.
 */
public enum DiagramType {

    HIERARCHY("Hierarchy"),
    HUB_AND_SPOKE("Hub & Spoke"),
    PROCESS_FLOW("Process Flow"),
    SEQUENTIAL_CHEVRON("Sequential Chevron"),
    CYCLE("Cycle"),
    CYCLE_ARROW("Cycle (Arrows)");

    private final String label;

    DiagramType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Labels in declaration order, for the listbox {@code StringItemList}. */
    public static String[] labels() {
        DiagramType[] values = values();
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].label;
        }
        return result;
    }

    /** Maps a 0-based dropdown index back to a type, clamped to a valid value. */
    public static DiagramType fromIndex(int index) {
        DiagramType[] values = values();
        if (index < 0 || index >= values.length) {
            return HIERARCHY;
        }
        return values[index];
    }
}
