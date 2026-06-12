package org.libreimpress.smartart;

import org.junit.Test;
import static org.junit.Assert.*;

public class SmartArtDialogValidationTest {

    @Test
    public void testValidHierarchyWithSpaces() {
        SmartArtDialog dialog = new SmartArtDialog();
        dialog.setHierarchyText("Parent\n  Child1\n  Child2\n    Grandchild");
        assertTrue("Should accept consistent space indentation", dialog.isInputValid());
    }

    @Test
    public void testValidHierarchyWithTabs() {
        SmartArtDialog dialog = new SmartArtDialog();
        dialog.setHierarchyText("Parent\n\tChild1\n\tChild2\n\t\tGrandchild");
        assertTrue("Should accept consistent tab indentation", dialog.isInputValid());
    }

    @Test
    public void testInvalidHierarchyMixedIndentation() {
        SmartArtDialog dialog = new SmartArtDialog();
        dialog.setHierarchyText("Parent\n\tChild1\n  Child2");
        assertFalse("Should reject mixed tab and space indentation", dialog.isInputValid());
    }

    @Test
    public void testInvalidHierarchyEmpty() {
        SmartArtDialog dialog = new SmartArtDialog();
        dialog.setHierarchyText("");
        assertFalse("Should reject empty hierarchy text", dialog.isInputValid());
    }

    @Test
    public void testInvalidHierarchyWhitespaceOnly() {
        SmartArtDialog dialog = new SmartArtDialog();
        dialog.setHierarchyText("   \n\t\t\n  ");
        assertFalse("Should reject whitespace-only hierarchy text", dialog.isInputValid());
    }

    @Test
    public void testInvalidHierarchyNull() {
        SmartArtDialog dialog = new SmartArtDialog();
        dialog.setHierarchyText(null);
        assertFalse("Should reject null hierarchy text", dialog.isInputValid());
    }

    @Test
    public void testDiagramTypeDefaultIsHierarchy() {
        SmartArtDialog dialog = new SmartArtDialog();
        assertEquals("Default diagram type should be Hierarchy", "Hierarchy", dialog.getDiagramType());
    }

    @Test
    public void testSetAndGetDiagramType() {
        SmartArtDialog dialog = new SmartArtDialog();
        dialog.setDiagramType("Hub & Spoke");
        assertEquals("Should set diagram type correctly", "Hub & Spoke", dialog.getDiagramType());
    }
}
