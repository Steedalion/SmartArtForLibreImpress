package org.libreimpress.smartart;

import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XToolkit;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import org.libreimpress.smartart.models.DiagramType;

/**
 * A programmatically-built UNO dialog that collects the diagram type and the
 * hierarchical text. No {@code .xdl} file is packaged, so the dialog adds
 * nothing to the extension's registration contract (master spec §5.5).
 */
public class SmartArtDialog {

    /** What the user entered when they pressed Create. */
    public static final class Result {
        private final String text;
        private final DiagramType type;

        Result(String text, DiagramType type) {
            this.text = text;
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public DiagramType getType() {
            return type;
        }
    }

    private final XComponentContext context;

    public SmartArtDialog(XComponentContext context) {
        this.context = context;
    }

    /**
     * Shows the dialog modally.
     *
     * @return the entered text and type on Create, or {@code null} on Cancel.
     */
    public Result show() throws Exception {
        XMultiComponentFactory smgr = context.getServiceManager();

        Object dialogModel = smgr.createInstanceWithContext(
                "com.sun.star.awt.UnoControlDialogModel", context);
        XPropertySet dialogProps =
                UnoRuntime.queryInterface(XPropertySet.class, dialogModel);
        dialogProps.setPropertyValue("PositionX", Integer.valueOf(100));
        dialogProps.setPropertyValue("PositionY", Integer.valueOf(80));
        dialogProps.setPropertyValue("Width", Integer.valueOf(230));
        dialogProps.setPropertyValue("Height", Integer.valueOf(190));
        dialogProps.setPropertyValue("Title", "SmartArt – Create Diagram");

        XMultiServiceFactory modelFactory =
                UnoRuntime.queryInterface(XMultiServiceFactory.class, dialogModel);
        XNameContainer container =
                UnoRuntime.queryInterface(XNameContainer.class, dialogModel);

        addLabel(modelFactory, container, "lblType", "Diagram type:", 8, 8, 70, 12);
        addListBox(modelFactory, container, "lstType", 80, 6, 142, 14);
        addLabel(modelFactory, container, "lblText", "Text points (indent to nest):",
                8, 28, 214, 12);
        addEdit(modelFactory, container, "txtInput", 8, 42, 214, 116);
        addButton(modelFactory, container, "btnOk", "Create", 110, 166, 54, 16,
                PushButtonType.OK_value, true);
        addButton(modelFactory, container, "btnCancel", "Cancel", 168, 166, 54, 16,
                PushButtonType.CANCEL_value, false);

        Object dialog = smgr.createInstanceWithContext(
                "com.sun.star.awt.UnoControlDialog", context);
        XControl control = UnoRuntime.queryInterface(XControl.class, dialog);
        control.setModel(UnoRuntime.queryInterface(XControlModel.class, dialogModel));

        Object toolkitObj = smgr.createInstanceWithContext(
                "com.sun.star.awt.Toolkit", context);
        XToolkit toolkit = UnoRuntime.queryInterface(XToolkit.class, toolkitObj);
        control.createPeer(toolkit, null);

        XDialog xDialog = UnoRuntime.queryInterface(XDialog.class, dialog);
        try {
            short ret = xDialog.execute();
            if (ret != PushButtonType.OK_value) {
                return null;
            }
            String text = (String) getModelProp(container, "txtInput", "Text");
            short[] selected = (short[]) getModelProp(container, "lstType", "SelectedItems");
            int index = (selected != null && selected.length > 0) ? selected[0] : 0;
            return new Result(text == null ? "" : text, DiagramType.fromIndex(index));
        } finally {
            com.sun.star.lang.XComponent disposable =
                    UnoRuntime.queryInterface(com.sun.star.lang.XComponent.class, dialog);
            if (disposable != null) {
                disposable.dispose();
            }
        }
    }

    private void addLabel(XMultiServiceFactory factory, XNameContainer container,
            String name, String caption, int x, int y, int w, int h) throws Exception {
        XPropertySet p = newControl(factory, container,
                "com.sun.star.awt.UnoControlFixedTextModel", name, x, y, w, h);
        p.setPropertyValue("Label", caption);
    }

    private void addEdit(XMultiServiceFactory factory, XNameContainer container,
            String name, int x, int y, int w, int h) throws Exception {
        XPropertySet p = newControl(factory, container,
                "com.sun.star.awt.UnoControlEditModel", name, x, y, w, h);
        p.setPropertyValue("MultiLine", Boolean.TRUE);
        p.setPropertyValue("VScroll", Boolean.TRUE);
    }

    private void addListBox(XMultiServiceFactory factory, XNameContainer container,
            String name, int x, int y, int w, int h) throws Exception {
        XPropertySet p = newControl(factory, container,
                "com.sun.star.awt.UnoControlListBoxModel", name, x, y, w, h);
        p.setPropertyValue("Dropdown", Boolean.TRUE);
        p.setPropertyValue("StringItemList", DiagramType.labels());
        p.setPropertyValue("SelectedItems", new short[] { 0 });
    }

    private void addButton(XMultiServiceFactory factory, XNameContainer container,
            String name, String caption, int x, int y, int w, int h,
            int pushButtonType, boolean isDefault) throws Exception {
        XPropertySet p = newControl(factory, container,
                "com.sun.star.awt.UnoControlButtonModel", name, x, y, w, h);
        p.setPropertyValue("Label", caption);
        p.setPropertyValue("PushButtonType", Short.valueOf((short) pushButtonType));
        if (isDefault) {
            p.setPropertyValue("DefaultButton", Boolean.TRUE);
        }
    }

    private XPropertySet newControl(XMultiServiceFactory factory, XNameContainer container,
            String service, String name, int x, int y, int w, int h) throws Exception {
        Object model = factory.createInstance(service);
        XPropertySet p = UnoRuntime.queryInterface(XPropertySet.class, model);
        p.setPropertyValue("PositionX", Integer.valueOf(x));
        p.setPropertyValue("PositionY", Integer.valueOf(y));
        p.setPropertyValue("Width", Integer.valueOf(w));
        p.setPropertyValue("Height", Integer.valueOf(h));
        p.setPropertyValue("Name", name);
        container.insertByName(name, model);
        return p;
    }

    private Object getModelProp(XNameContainer container, String controlName,
            String property) throws Exception {
        Object model = container.getByName(controlName);
        XPropertySet p = UnoRuntime.queryInterface(XPropertySet.class, model);
        return p.getPropertyValue(property);
    }
}
