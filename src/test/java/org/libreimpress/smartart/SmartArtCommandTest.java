package org.libreimpress.smartart;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Sample test to verify the test framework is working.
 * This test will be removed in Phase 2 when real tests are added.
 */
public class SmartArtCommandTest {

    @Test
    public void testImplementationNameNotEmpty() {
        String name = SmartArtCommand.getImplementationName();
        assertNotNull("Implementation name should not be null", name);
        assertFalse("Implementation name should not be empty", name.isEmpty());
        assertEquals("Implementation name should match", 
            "org.libreimpress.smartart.SmartArtCommand", name);
    }

    @Test
    public void testSupportedServicesNotEmpty() {
        String[] services = SmartArtCommand.getSupportedServiceNames();
        assertNotNull("Supported services array should not be null", services);
        assertTrue("Should support at least one service", services.length > 0);
        assertEquals("Should support ProtocolHandler service", 
            "com.sun.star.frame.ProtocolHandler", services[0]);
    }

    @Test
    public void testSmartArtDialogInstantiation() {
        SmartArtDialog dialog = new SmartArtDialog();
        assertNotNull("Dialog should instantiate", dialog);
    }

    @Test
    public void testSmartArtDialogGettersSetters() {
        SmartArtDialog dialog = new SmartArtDialog();
        
        String hierarchyText = "Parent\n  Child1\n  Child2";
        dialog.setHierarchyText(hierarchyText);
        assertEquals("Hierarchy text should match", hierarchyText, dialog.getHierarchyText());
        
        String diagramType = "Hierarchy";
        dialog.setDiagramType(diagramType);
        assertEquals("Diagram type should match", diagramType, dialog.getDiagramType());
        
        String palette = "{\"level1\": \"#FF0000\"}";
        dialog.setPalette(palette);
        assertEquals("Palette should match", palette, dialog.getPalette());
    }
}
