package org.libreimpress.smartart;

import com.sun.star.uno.XComponentContext;

public class SmartArtCommand {

    private static final String IMPLEMENTATION_NAME =
        "org.libreimpress.smartart.SmartArtCommand";
    private static final String[] SERVICE_NAMES = {
        "com.sun.star.frame.ProtocolHandler"
    };

    private Object componentContext;

    public static String getImplementationName() {
        return IMPLEMENTATION_NAME;
    }

    public static String[] getSupportedServiceNames() {
        return SERVICE_NAMES;
    }

    public SmartArtCommand(Object componentContext) {
        this.componentContext = componentContext;
    }

    public void execute() {
        try {
            XComponentContext context = (XComponentContext) componentContext;
            SmartArtDialog dialog = new SmartArtDialog();

            if (dialog.show(context)) {
                String hierarchy = dialog.getHierarchyText();
                String type = dialog.getDiagramType();

                System.out.println("Creating " + type + " diagram");
                System.out.println("Hierarchy:\n" + hierarchy);
            }
        } catch (Exception e) {
            System.err.println("Error executing SmartArt command: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
