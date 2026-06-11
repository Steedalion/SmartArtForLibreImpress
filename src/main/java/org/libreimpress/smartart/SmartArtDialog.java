package org.libreimpress.smartart;

/**
 * SmartArtDialog - Dialog controller for user input.
 * 
 * Responsibilities:
 * - Display dialog to user (text area, diagram type selector, palette field)
 * - Capture user input (hierarchy text, selected diagram type, optional palette)
 * - Validate input before passing to processing layer
 * - Return captured data to SmartArtCommand
 */
public class SmartArtDialog {
    
    private String hierarchyText;
    private String diagramType;
    private String palette;

    /**
     * Show the SmartArt dialog to the user.
     * Blocks until user clicks Create/Cancel.
     * 
     * @return true if user clicked Create, false if Cancel
     */
    public boolean show() {
        // To be implemented in Phase 2
        System.out.println("SmartArtDialog.show() called");
        return true;
    }

    /**
     * Get the hierarchical text entered by the user.
     * 
     * @return text with indentation representing hierarchy
     */
    public String getHierarchyText() {
        return hierarchyText;
    }

    /**
     * Set the hierarchy text.
     * 
     * @param text the hierarchical text input
     */
    public void setHierarchyText(String text) {
        this.hierarchyText = text;
    }

    /**
     * Get the selected diagram type.
     * 
     * @return one of: "Hierarchy", "HubSpoke", "ProcessFlow"
     */
    public String getDiagramType() {
        return diagramType;
    }

    /**
     * Set the diagram type.
     * 
     * @param type the diagram type
     */
    public void setDiagramType(String type) {
        this.diagramType = type;
    }

    /**
     * Get the optional color palette JSON.
     * 
     * @return JSON string or null if not provided
     */
    public String getPalette() {
        return palette;
    }

    /**
     * Set the palette JSON.
     * 
     * @param palette JSON string representing color palette
     */
    public void setPalette(String palette) {
        this.palette = palette;
    }
}
