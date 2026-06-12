package org.libreimpress.smartart;

import com.sun.star.awt.XButton;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class SmartArtDialog {
    private String hierarchyText = "";
    private String diagramType = "Hierarchy";
    private boolean userClickedCreate = false;

    public boolean show(XComponentContext context) {
        try {
            XMultiComponentFactory factory = context.getServiceManager();

            Object toolkit = factory.createInstanceWithContext(
                "com.sun.star.awt.Toolkit", context);

            Object dialogProvider = factory.createInstanceWithContext(
                "com.sun.star.awt.DialogProvider", context);

            XDialog dialog = UnoRuntime.queryInterface(XDialog.class, dialogProvider);

            if (dialog != null) {
                short result = dialog.execute();
                return result == 1;
            }

            return false;
        } catch (Exception e) {
            System.err.println("Error showing dialog: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public String getHierarchyText() {
        return hierarchyText;
    }

    public void setHierarchyText(String text) {
        this.hierarchyText = text;
    }

    public String getDiagramType() {
        return diagramType;
    }

    public void setDiagramType(String type) {
        this.diagramType = type;
    }

    public boolean isInputValid() {
        if (hierarchyText == null || hierarchyText.trim().isEmpty()) {
            return false;
        }
        return validateIndentation(hierarchyText);
    }

    private boolean validateIndentation(String text) {
        String[] lines = text.split("\n");
        boolean hasSpaces = false;
        boolean hasTabs = false;

        for (String line : lines) {
            if (line.isEmpty()) continue;

            for (char c : line.toCharArray()) {
                if (c == ' ') hasSpaces = true;
                else if (c == '\t') hasTabs = true;
                else break;
            }

            if (hasSpaces && hasTabs) {
                return false;
            }
        }

        return true;
    }
}
