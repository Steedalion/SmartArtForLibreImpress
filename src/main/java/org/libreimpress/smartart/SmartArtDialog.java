package org.libreimpress.smartart;

import com.sun.star.awt.Key;
import com.sun.star.awt.KeyEvent;
import com.sun.star.awt.KeyModifier;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.Selection;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XExtendedToolkit;
import com.sun.star.awt.XKeyHandler;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow2;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import org.libreimpress.smartart.editing.OutlineEditor;
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

    /** The field starts as — and is kept as — an indented list. */
    private static final String SEED_TEXT =
            "Main idea\n" + OutlineEditor.INDENT + "Supporting point\n"
                    + OutlineEditor.INDENT + OutlineEditor.INDENT + "Detail";

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
        addLabel(modelFactory, container, "lblText",
                "Points (Enter = new item · Tab / Shift+Tab = level):", 8, 28, 214, 12);
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

        // Make Tab/Shift+Tab/Enter behave like an outline editor in the text box.
        XControlContainer controls = UnoRuntime.queryInterface(XControlContainer.class, dialog);
        XControl editControl = controls.getControl("txtInput");
        XTextComponent editText = UnoRuntime.queryInterface(XTextComponent.class, editControl);
        XWindow2 editWindow = UnoRuntime.queryInterface(XWindow2.class, editControl);
        XExtendedToolkit extToolkit =
                UnoRuntime.queryInterface(XExtendedToolkit.class, toolkitObj);
        XKeyHandler keyHandler = new OutlineKeyHandler(editText, editWindow);
        if (extToolkit != null) {
            extToolkit.addKeyHandler(keyHandler);
        }

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
            if (extToolkit != null) {
                extToolkit.removeKeyHandler(keyHandler);
            }
            com.sun.star.lang.XComponent disposable =
                    UnoRuntime.queryInterface(com.sun.star.lang.XComponent.class, dialog);
            if (disposable != null) {
                disposable.dispose();
            }
        }
    }

    /**
     * Repurposes Tab / Shift+Tab / Enter inside the text box so it edits as an
     * indented outline (a list that stays a list). Only acts while the text box
     * has focus; all other keys pass through. The actual text transforms live in
     * {@link OutlineEditor}.
     */
    private static final class OutlineKeyHandler implements XKeyHandler {
        private final XTextComponent edit;
        private final XWindow2 window;

        OutlineKeyHandler(XTextComponent edit, XWindow2 window) {
            this.edit = edit;
            this.window = window;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (edit == null || window == null || !window.hasFocus()) {
                return false;
            }
            if (event.KeyCode == Key.TAB) {
                boolean shift = (event.Modifiers & KeyModifier.SHIFT) != 0;
                apply(shift ? Op.OUTDENT : Op.INDENT);
                return true;
            }
            if (event.KeyCode == Key.RETURN && event.Modifiers == 0) {
                apply(Op.NEWLINE);
                return true;
            }
            return false;
        }

        @Override
        public boolean keyReleased(KeyEvent event) {
            // Consume Tab's release too, so focus never leaves the box.
            return edit != null && window != null && window.hasFocus()
                    && event.KeyCode == Key.TAB;
        }

        @Override
        public void disposing(com.sun.star.lang.EventObject event) {
            // nothing to release
        }

        private enum Op { INDENT, OUTDENT, NEWLINE }

        private void apply(Op op) {
            String text = edit.getText();
            Selection sel = edit.getSelection();
            int min = Math.min(sel.Min, sel.Max);
            int max = Math.max(sel.Min, sel.Max);
            OutlineEditor.Edit result;
            switch (op) {
                case INDENT:
                    result = OutlineEditor.indent(text, min, max);
                    break;
                case OUTDENT:
                    result = OutlineEditor.outdent(text, min, max);
                    break;
                default:
                    result = OutlineEditor.newlineKeepingIndent(text, min, max);
                    break;
            }
            edit.setText(result.text);
            edit.setSelection(new Selection(result.selStart, result.selEnd));
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
        p.setPropertyValue("HideInactiveSelection", Boolean.TRUE);
        p.setPropertyValue("Text", SEED_TEXT);
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
