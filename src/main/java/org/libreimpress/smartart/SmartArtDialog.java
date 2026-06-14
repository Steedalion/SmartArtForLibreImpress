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
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XTextListener;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow2;
import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.document.XExporter;
import com.sun.star.document.XFilter;
import com.sun.star.drawing.XDrawPage;
import com.sun.star.drawing.XDrawPages;
import com.sun.star.drawing.XDrawPagesSupplier;
import com.sun.star.frame.XDesktop;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XModifiable;

import java.io.File;

import org.libreimpress.smartart.editing.OutlineEditor;
import org.libreimpress.smartart.layout.DiagramLayout;
import org.libreimpress.smartart.layout.LayoutFactory;
import org.libreimpress.smartart.models.ColorPalette;
import org.libreimpress.smartart.models.DiagramType;
import org.libreimpress.smartart.parsers.HierarchyParser;
import org.libreimpress.smartart.parsers.ParseResult;
import org.libreimpress.smartart.rendering.SlideRenderer;

/**
 * A programmatically-built UNO dialog that collects the diagram type and the
 * hierarchical text. A preview panel on the right renders the diagram into a
 * hidden Impress document (pre-created before the modal loop starts) and
 * displays it as a PNG when the user clicks "▶ Preview". No {@code .xdl}
 * file is packaged (master spec §5.5).
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

        public String getText() { return text; }
        public DiagramType getType() { return type; }
        public String getPaletteText() { return paletteText != null ? paletteText : ""; }
    }

    // ---- layout constants (UNO dialog units) ----
    private static final int LEFT_W   = 230;
    private static final int SEP      = 8;
    private static final int PREV_W   = 242;
    private static final int DIALOG_W = LEFT_W + SEP + PREV_W;  // 480
    private static final int DIALOG_H = 216;

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
        dialogProps.setPropertyValue("Width",     Integer.valueOf(DIALOG_W));
        dialogProps.setPropertyValue("Height",    Integer.valueOf(DIALOG_H));
        dialogProps.setPropertyValue("Title",     "SmartArt – Create Diagram");

        XMultiServiceFactory modelFactory =
                UnoRuntime.queryInterface(XMultiServiceFactory.class, dialogModel);
        XNameContainer container =
                UnoRuntime.queryInterface(XNameContainer.class, dialogModel);

        // ---- left panel (unchanged from before) ----
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

        // ---- right panel: preview ----
        int px = LEFT_W + SEP;
        addLabel(modelFactory, container, "lblPreview", "Preview", px, 8, 60, 10);
        addImageControl(modelFactory, container, "imgPreview", px, 20, PREV_W, 166);
        addLabel(modelFactory, container, "lblPreviewStatus",
                "Click ▶ to preview", px, 188, PREV_W, 10);
        addButton(modelFactory, container, "btnPreview", "▶ Preview", px, 200, 64, 12,
                PushButtonType.STANDARD_value, false);

        Object dialog = smgr.createInstanceWithContext(
                "com.sun.star.awt.UnoControlDialog", context);
        XControl control = UnoRuntime.queryInterface(XControl.class, dialog);
        control.setModel(UnoRuntime.queryInterface(XControlModel.class, dialogModel));

        Object toolkitObj = smgr.createInstanceWithContext(
                "com.sun.star.awt.Toolkit", context);
        XToolkit toolkit = UnoRuntime.queryInterface(XToolkit.class, toolkitObj);
        control.createPeer(toolkit, null);

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

        bindButton(controls, "btnIndent",
                new LevelButtonListener(editing, OutlineEditing.Op.INDENT));
        bindButton(controls, "btnOutdent",
                new LevelButtonListener(editing, OutlineEditing.Op.OUTDENT));

        // Capture the current Impress document BEFORE the modal dialog opens.
        // getCurrentComponent() is reliable here; inside the event handler it
        // might return the dialog itself. Creating a NEW document from inside
        // the event handler is blocked, but inserting a slide into the existing
        // document is fine — that is how the preview works.
        XComponent currentDoc = null;
        XDrawPages currentPages = null;
        XMultiServiceFactory docFactory = null;
        String previewInitError = null;
        try {
            Object desktopObj = smgr.createInstanceWithContext(
                    "com.sun.star.frame.Desktop", context);
            XDesktop desktop = UnoRuntime.queryInterface(XDesktop.class, desktopObj);
            currentDoc = desktop.getCurrentComponent();
            if (currentDoc != null) {
                XDrawPagesSupplier ps =
                        UnoRuntime.queryInterface(XDrawPagesSupplier.class, currentDoc);
                if (ps != null) currentPages = ps.getDrawPages();
                docFactory = UnoRuntime.queryInterface(XMultiServiceFactory.class, currentDoc);
            }
        } catch (Exception e) {
            previewInitError = e.getClass().getSimpleName()
                    + (e.getMessage() != null ? ": " + e.getMessage() : "");
        }
        if (previewInitError == null
                && (currentDoc == null || currentPages == null || docFactory == null)) {
            previewInitError = "doc=" + currentDoc + " pages=" + currentPages
                    + " factory=" + docFactory;
        }

        PreviewController preview = new PreviewController(container, currentDoc,
                currentPages, docFactory, previewInitError);
        bindButton(controls, "btnPreview", new PreviewButtonListener(preview));

        // Live preview: re-render (debounced) whenever the input, palette, or
        // diagram type changes. The outline buttons and key handler edit the
        // text via setText(), which also fires textChanged — so they are covered
        // automatically without separate wiring.
        XTextListener previewText = new PreviewTextListener(preview);
        if (editText != null) {
            editText.addTextListener(previewText);
        }
        XTextComponent paletteEdit = UnoRuntime.queryInterface(
                XTextComponent.class, controls.getControl("txtPalette"));
        if (paletteEdit != null) {
            paletteEdit.addTextListener(previewText);
        }
        XListBox typeList = UnoRuntime.queryInterface(
                XListBox.class, controls.getControl("lstType"));
        if (typeList != null) {
            typeList.addItemListener(new PreviewItemListener(preview));
        }

        XDialog xDialog = UnoRuntime.queryInterface(XDialog.class, dialog);
        // Render an initial preview shortly after the dialog appears.
        preview.schedule();
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
            preview.shutdown();
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

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    private void bindButton(XControlContainer controls, String name,
            XActionListener listener) {
        XButton button = UnoRuntime.queryInterface(XButton.class, controls.getControl(name));
        if (button != null) {
            button.addActionListener(listener);
        }
    }

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
            if (window != null) window.setFocus();
        }

        void apply(Op op) {
            if (edit == null) return;
            String text = edit.getText();
            Selection sel = edit.getSelection();
            int min = Math.min(sel.Min, sel.Max);
            int max = Math.max(sel.Min, sel.Max);
            OutlineEditor.Edit result;
            switch (op) {
                case INDENT:  result = OutlineEditor.indent(text, min, max); break;
                case OUTDENT: result = OutlineEditor.outdent(text, min, max); break;
                default:      result = OutlineEditor.newlineKeepingIndent(text, min, max); break;
            }
            edit.setText(result.text);
            edit.setSelection(new Selection(result.selStart, result.selEnd));
        }
    }

    private static final class OutlineKeyHandler implements XKeyHandler {
        private final OutlineEditing editing;

        OutlineKeyHandler(OutlineEditing editing) { this.editing = editing; }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (!editing.editFocused()) return false;
            if ((event.Modifiers & KeyModifier.MOD1) != 0) {
                char c = event.KeyChar;
                if (c == ']' || c == 0x1d) { editing.apply(OutlineEditing.Op.INDENT);  return true; }
                if (c == '[' || c == 0x1b) { editing.apply(OutlineEditing.Op.OUTDENT); return true; }
            }
            if (event.KeyCode == Key.RETURN && event.Modifiers == 0) {
                editing.apply(OutlineEditing.Op.NEWLINE);
                return true;
            }
            return false;
        }

        @Override public boolean keyReleased(KeyEvent event) { return false; }
        @Override public void disposing(com.sun.star.lang.EventObject event) {}
    }

    private static final class LevelButtonListener implements XActionListener {
        private final OutlineEditing editing;
        private final OutlineEditing.Op op;

        LevelButtonListener(OutlineEditing editing, OutlineEditing.Op op) {
            this.editing = editing;
            this.op = op;
        }

        @Override public void actionPerformed(ActionEvent event) { editing.apply(op); editing.focusEdit(); }
        @Override public void disposing(com.sun.star.lang.EventObject event) {}
    }

    /**
     * Renders the current input into a temporary slide on the already-open
     * Impress document, exports it as PNG, removes the slide, and sets the
     * ImageControl's URL. All failures are shown in the status label.
     *
     * <p>A hidden document cannot be used here because LibreOffice's internal
     * shape/connector code dereferences a null controller on hidden docs,
     * throwing RuntimeException("Null pointer"). Using the existing document
     * avoids that — inserting a slide into an open document works fine from
     * within a modal dialog's event handler.
     */
    private final class PreviewController {
        private static final long DEBOUNCE_MS = 350;

        private final XNameContainer container;
        private final XComponent currentDoc;
        private final XDrawPages currentPages;
        private final XMultiServiceFactory docFactory;
        private final String initError;

        private final java.util.Timer timer =
                new java.util.Timer("smartart-preview", true);
        private java.util.TimerTask pending;
        private volatile boolean active = true;

        PreviewController(XNameContainer container,
                XComponent currentDoc, XDrawPages currentPages,
                XMultiServiceFactory docFactory, String initError) {
            this.container    = container;
            this.currentDoc   = currentDoc;
            this.currentPages = currentPages;
            this.docFactory   = docFactory;
            this.initError    = initError;
        }

        /** Coalesces rapid edits into a single render after a short pause. */
        synchronized void schedule() {
            if (!active) {
                return;
            }
            if (pending != null) {
                pending.cancel();
            }
            pending = new java.util.TimerTask() {
                @Override public void run() { render(); }
            };
            try {
                timer.schedule(pending, DEBOUNCE_MS);
            } catch (IllegalStateException ignored) {
                // Timer already cancelled because the dialog is closing.
            }
        }

        /** Stops any further renders; called when the dialog closes. */
        synchronized void shutdown() {
            active = false;
            timer.cancel();
        }

        /**
         * Renders the current input into a temporary slide appended to the open
         * document, exports it as PNG, removes the slide, and shows the PNG.
         * Synchronized so the button click and the debounce timer never overlap.
         * The document's modified flag is preserved so previewing never marks
         * the document dirty or triggers a save prompt.
         */
        synchronized void render() {
            if (!active) {
                return;
            }
            if (initError != null) {
                setStatus("Init error: " + initError);
                return;
            }
            setStatus("Rendering…");
            XModifiable mod = UnoRuntime.queryInterface(XModifiable.class, currentDoc);
            boolean wasModified = false;
            try {
                wasModified = mod != null && mod.isModified();
            } catch (Exception ignored) {}
            // Track the last operation attempted so a "Null pointer" RuntimeException
            // (whose message alone doesn't say where it came from) is pinpointed.
            String step = "start";
            XDrawPage renderPage = null;
            boolean inserted = false;
            try {
                step = "read-input";
                String inputText = (String) getModelProp(container, "txtInput", "Text");
                short[] selected = (short[]) getModelProp(container, "lstType", "SelectedItems");
                int index = (selected != null && selected.length > 0) ? selected[0] : 0;
                DiagramType type = DiagramType.fromIndex(index);

                step = "parse";
                ParseResult parsed = new HierarchyParser()
                        .parse(inputText == null ? "" : inputText);
                if (!parsed.isValid()) {
                    setStatus("Parse error: " + parsed.getErrorMessage());
                    return;
                }
                step = "layout";
                DiagramLayout layout = LayoutFactory.build(type, parsed.getRoot());

                // Append a temporary slide, render the diagram onto it, export
                // it as PNG, then remove the slide — leaving the document unchanged.
                step = "insert-slide";
                int origCount = currentPages.getCount();
                currentPages.insertNewByIndex(origCount);
                inserted = true;

                step = "get-page";
                renderPage = UnoRuntime.queryInterface(XDrawPage.class,
                        currentPages.getByIndex(origCount));

                step = "render";
                new SlideRenderer(context)
                        .drawHierarchy(renderPage, docFactory, layout, ColorPalette.EMPTY);

                step = "temp-file";
                File tmp = File.createTempFile("smartart_prev_", ".png");
                tmp.deleteOnExit();
                String fileUrl = tmp.toURI().toString();

                step = "create-exporter";
                XMultiComponentFactory smgr = context.getServiceManager();
                Object expObj = smgr.createInstanceWithContext(
                        "com.sun.star.drawing.GraphicExportFilter", context);
                if (expObj == null) {
                    throw new Exception("GraphicExportFilter service unavailable");
                }
                XExporter exporter = UnoRuntime.queryInterface(XExporter.class, expObj);
                XFilter filter = UnoRuntime.queryInterface(XFilter.class, expObj);
                if (exporter == null || filter == null) {
                    throw new Exception("exporter=" + exporter + " filter=" + filter);
                }

                // GraphicExporter exports the component passed to setSourceDocument.
                // Pass the draw PAGE itself (it implements XComponent), not the whole
                // document with a Selection — the document path needs a live view and
                // can throw RuntimeException("Null pointer").
                step = "set-source";
                XComponent pageComp =
                        UnoRuntime.queryInterface(XComponent.class, renderPage);
                exporter.setSourceDocument(pageComp);

                step = "filter-export";
                filter.filter(new PropertyValue[]{
                    pv("MediaType",  "image/png"),
                    pv("URL",        fileUrl),
                    pv("FilterData", new PropertyValue[]{
                        pv("PixelWidth",  Integer.valueOf(800)),
                        pv("PixelHeight", Integer.valueOf(600)),
                    }),
                });

                step = "set-image";
                XPropertySet imgModel = UnoRuntime.queryInterface(XPropertySet.class,
                        container.getByName("imgPreview"));
                imgModel.setPropertyValue("ImageURL", fileUrl);
                setStatus("");
            } catch (Exception e) {
                String desc = e.getClass().getSimpleName();
                if (e.getMessage() != null) desc += ": " + e.getMessage();
                setStatus("Error@" + step + ": " + desc);
            } finally {
                if (inserted && renderPage != null) {
                    try { currentPages.remove(renderPage); } catch (Exception ignored) {}
                }
                if (mod != null) {
                    try { mod.setModified(wasModified); } catch (Exception ignored) {}
                }
            }
        }

        private void setStatus(String msg) {
            try {
                XPropertySet p = UnoRuntime.queryInterface(XPropertySet.class,
                        container.getByName("lblPreviewStatus"));
                p.setPropertyValue("Label", msg);
            } catch (Exception ignored) {}
        }
    }

    /** "▶ Preview" button — forces an immediate render. */
    private final class PreviewButtonListener implements XActionListener {
        private final PreviewController controller;
        PreviewButtonListener(PreviewController controller) { this.controller = controller; }
        @Override public void actionPerformed(ActionEvent event) { controller.render(); }
        @Override public void disposing(com.sun.star.lang.EventObject event) {}
    }

    /** Re-renders (debounced) when the input or palette text changes. */
    private final class PreviewTextListener implements XTextListener {
        private final PreviewController controller;
        PreviewTextListener(PreviewController controller) { this.controller = controller; }
        @Override public void textChanged(com.sun.star.awt.TextEvent event) { controller.schedule(); }
        @Override public void disposing(com.sun.star.lang.EventObject event) {}
    }

    /** Re-renders (debounced) when the diagram type selection changes. */
    private final class PreviewItemListener implements com.sun.star.awt.XItemListener {
        private final PreviewController controller;
        PreviewItemListener(PreviewController controller) { this.controller = controller; }
        @Override public void itemStateChanged(com.sun.star.awt.ItemEvent event) { controller.schedule(); }
        @Override public void disposing(com.sun.star.lang.EventObject event) {}
    }

    // -------------------------------------------------------------------------
    // Control factory helpers
    // -------------------------------------------------------------------------

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
        p.setPropertyValue("SelectedItems", new short[]{ 0 });
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

    private void addImageControl(XMultiServiceFactory factory, XNameContainer container,
            String name, int x, int y, int w, int h) throws Exception {
        XPropertySet p = newControl(factory, container,
                "com.sun.star.awt.UnoControlImageControlModel", name, x, y, w, h);
        p.setPropertyValue("ScaleImage", Boolean.TRUE);
        p.setPropertyValue("Border", Short.valueOf((short) 1));
    }

    private XPropertySet newControl(XMultiServiceFactory factory, XNameContainer container,
            String service, String name, int x, int y, int w, int h) throws Exception {
        Object model = factory.createInstance(service);
        XPropertySet p = UnoRuntime.queryInterface(XPropertySet.class, model);
        p.setPropertyValue("PositionX", Integer.valueOf(x));
        p.setPropertyValue("PositionY", Integer.valueOf(y));
        p.setPropertyValue("Width",     Integer.valueOf(w));
        p.setPropertyValue("Height",    Integer.valueOf(h));
        p.setPropertyValue("Name",      name);
        container.insertByName(name, model);
        return p;
    }

    private Object getModelProp(XNameContainer container, String controlName,
            String property) throws Exception {
        Object model = container.getByName(controlName);
        XPropertySet p = UnoRuntime.queryInterface(XPropertySet.class, model);
        return p.getPropertyValue(property);
    }

    private static PropertyValue pv(String name, Object value) {
        PropertyValue p = new PropertyValue();
        p.Name  = name;
        p.Value = value;
        p.State = PropertyState.DIRECT_VALUE;
        return p;
    }
}
