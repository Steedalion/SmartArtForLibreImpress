package org.libreimpress.smartart;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.Key;
import com.sun.star.awt.KeyEvent;
import com.sun.star.awt.KeyModifier;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.Selection;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
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
        private final String paletteText;

        Result(String text, DiagramType type, String paletteText) {
            this.text = text;
            this.type = type;
            this.paletteText = paletteText;
        }

        public String getText() {
            return text;
        }

        public DiagramType getType() {
            return type;
        }

        /** Raw palette text entered by the user, or empty string if not provided. */
        public String getPaletteText() {
            return paletteText != null ? paletteText : "";
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
        dialogProps.setPropertyValue("Height", Integer.valueOf(216));
        dialogProps.setPropertyValue("Title", "SmartArt – Create Diagram");

        XMultiServiceFactory modelFactory =
                UnoRuntime.queryInterface(XMultiServiceFactory.class, dialogModel);
        XNameContainer container =
                UnoRuntime.queryInterface(XNameContainer.class, dialogModel);

        addLabel(modelFactory, container, "lblType", "Diagram type:", 8, 8, 60, 12);
        addListBox(modelFactory, container, "lstType", 70, 6, 152, 14);
        addLabel(modelFactory, container, "lblText", "List points:", 8, 28, 96, 12);
        addButton(modelFactory, container, "btnOutdent", "← Outdent", 106, 26, 56, 14,
                PushButtonType.STANDARD_value, false);
        addButton(modelFactory, container, "btnIndent", "Indent →", 164, 26, 58, 14,
                PushButtonType.STANDARD_value, false);
        addEdit(modelFactory, container, "txtInput", 8, 44, 214, 86);
        addLabel(modelFactory, container, "lblPalette",
                "Colours (optional, e.g. 1=#4472C4):", 8, 136, 214, 10);
        addPaletteEdit(modelFactory, container, "txtPalette", 8, 148, 214, 36);
        addButton(modelFactory, container, "btnOk", "Create", 110, 190, 54, 16,
                PushButtonType.OK_value, true);
        addButton(modelFactory, container, "btnCancel", "Cancel", 168, 190, 54, 16,
                PushButtonType.CANCEL_value, false);

        Object dialog = smgr.createInstanceWithContext(
                "com.sun.star.awt.UnoControlDialog", context);
        XControl control = UnoRuntime.queryInterface(XControl.class, dialog);
        control.setModel(UnoRuntime.queryInterface(XControlModel.class, dialogModel));

        Object toolkitObj = smgr.createInstanceWithContext(
                "com.sun.star.awt.Toolkit", context);
        XToolkit toolkit = UnoRuntime.queryInterface(XToolkit.class, toolkitObj);
        control.createPeer(toolkit, null);

        // Drive the text box as an outline list. Level changes come from the
        // Indent/Outdent buttons (always work) and Ctrl+] / Ctrl+[ (the key
        // handler can intercept these; Tab can't be — it's focus traversal).
        XControlContainer controls = UnoRuntime.queryInterface(XControlContainer.class, dialog);
        XControl editControl = controls.getControl("txtInput");
        XTextComponent editText = UnoRuntime.queryInterface(XTextComponent.class, editControl);
        XWindow2 editWindow = UnoRuntime.queryInterface(XWindow2.class, editControl);
        OutlineEditing editing = new OutlineEditing(editText, editWindow);

        XExtendedToolkit extToolkit =
                UnoRuntime.queryInterface(XExtendedToolkit.class, toolkitObj);
        XKeyHandler keyHandler = new OutlineKeyHandler(editing);
        if (extToolkit != null) {
            extToolkit.addKeyHandler(keyHandler);
        }

        bindButton(controls, "btnIndent", new LevelButtonListener(editing, OutlineEditing.Op.INDENT));
        bindButton(controls, "btnOutdent", new LevelButtonListener(editing, OutlineEditing.Op.OUTDENT));

        XDialog xDialog = UnoRuntime.queryInterface(XDialog.class, dialog);
        try {
            short ret = xDialog.execute();
            if (ret != PushButtonType.OK_value) {
                return null;
            }
            String text = (String) getModelProp(container, "txtInput", "Text");
            short[] selected = (short[]) getModelProp(container, "lstType", "SelectedItems");
            int index = (selected != null && selected.length > 0) ? selected[0] : 0;
            String paletteText = (String) getModelProp(container, "txtPalette", "Text");
            return new Result(text == null ? "" : text, DiagramType.fromIndex(index),
                    paletteText == null ? "" : paletteText);
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

    private void bindButton(XControlContainer controls, String name,
            XActionListener listener) {
        XButton button = UnoRuntime.queryInterface(XButton.class, controls.getControl(name));
        if (button != null) {
            button.addActionListener(listener);
        }
    }

    /**
     * Applies the {@link OutlineEditor} transforms to the dialog's text control.
     * Shared by the key handler and the Indent/Outdent buttons.
     */
    private static final class OutlineEditing {
        enum Op { INDENT, OUTDENT, NEWLINE }

        private final XTextComponent edit;
        private final XWindow2 window;

        OutlineEditing(XTextComponent edit, XWindow2 window) {
            this.edit = edit;
            this.window = window;
        }

        boolean editFocused() {
            return edit != null && window != null && window.hasFocus();
        }

        void focusEdit() {
            if (window != null) {
                window.setFocus();
            }
        }

        void apply(Op op) {
            if (edit == null) {
                return;
            }
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

    /**
     * Keyboard shortcuts inside the text box, active only while it has focus:
     * Ctrl+] indents, Ctrl+[ outdents, Enter starts a new item at the current
     * level. Tab is intentionally NOT handled — a UNO dialog reserves it for
     * focus traversal and it cannot be reliably intercepted; the Indent/Outdent
     * buttons cover that case. All other keys pass through.
     */
    private static final class OutlineKeyHandler implements XKeyHandler {
        private final OutlineEditing editing;

        OutlineKeyHandler(OutlineEditing editing) {
            this.editing = editing;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (!editing.editFocused()) {
                return false;
            }
            if ((event.Modifiers & KeyModifier.MOD1) != 0) { // Ctrl
                char c = event.KeyChar;
                if (c == ']' || c == 0x1d) { // ']' or Ctrl+] control char
                    editing.apply(OutlineEditing.Op.INDENT);
                    return true;
                }
                if (c == '[' || c == 0x1b) { // '[' or Ctrl+[ control char
                    editing.apply(OutlineEditing.Op.OUTDENT);
                    return true;
                }
            }
            if (event.KeyCode == Key.RETURN && event.Modifiers == 0) {
                editing.apply(OutlineEditing.Op.NEWLINE);
                return true;
            }
            return false;
        }

        @Override
        public boolean keyReleased(KeyEvent event) {
            return false;
        }

        @Override
        public void disposing(com.sun.star.lang.EventObject event) {
            // nothing to release
        }
    }

    /** Indent/Outdent button click → apply the transform, then return focus. */
    private static final class LevelButtonListener implements XActionListener {
        private final OutlineEditing editing;
        private final OutlineEditing.Op op;

        LevelButtonListener(OutlineEditing editing, OutlineEditing.Op op) {
            this.editing = editing;
            this.op = op;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            editing.apply(op);
            editing.focusEdit();
        }

        @Override
        public void disposing(com.sun.star.lang.EventObject event) {
            // nothing to release
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
        p.setPropertyValue("HelpText",
                "One item per line. Use - for level 2, -- for level 3, etc. "
                        + "Enter = new item; Indent/Outdent buttons or Ctrl+]/Ctrl+[ change the level.");
        p.setPropertyValue("Text", SmartArtConfig.getSeedText());
    }

    private void addPaletteEdit(XMultiServiceFactory factory, XNameContainer container,
            String name, int x, int y, int w, int h) throws Exception {
        XPropertySet p = newControl(factory, container,
                "com.sun.star.awt.UnoControlEditModel", name, x, y, w, h);
        p.setPropertyValue("MultiLine", Boolean.TRUE);
        p.setPropertyValue("VScroll", Boolean.TRUE);
        p.setPropertyValue("HideInactiveSelection", Boolean.TRUE);
        p.setPropertyValue("HelpText",
                "One line per level: 1=#4472C4   Leave blank to use the default colours.");
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
