package org.libreimpress.smartart;

import com.sun.star.awt.Point;
import com.sun.star.awt.Size;
import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.document.XExporter;
import com.sun.star.document.XFilter;
import com.sun.star.drawing.XDrawPage;
import com.sun.star.drawing.XDrawPages;
import com.sun.star.drawing.XDrawPagesSupplier;
import com.sun.star.drawing.XDrawView;
import com.sun.star.drawing.XShape;
import com.sun.star.drawing.XShapes;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XText;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import java.io.File;

import org.libreimpress.smartart.helpers.LibreOfficeHelper;
import org.libreimpress.smartart.layout.DiagramLayout;
import org.libreimpress.smartart.layout.LayoutFactory;
import org.libreimpress.smartart.models.ColorPalette;
import org.libreimpress.smartart.models.DiagramType;
import org.libreimpress.smartart.parsers.HierarchyParser;
import org.libreimpress.smartart.parsers.ParseResult;
import org.libreimpress.smartart.rendering.SlideRenderer;

/**
 * [DEV ONLY] Appends one demo slide per diagram type to the current presentation,
 * then exports each slide as a PNG to {@code docs/screenshots/} (if the directory
 * exists). The output directory can be overridden with the system property
 * {@code smartart.screenshots.dir}.
 *
 * <p>To remove this feature entirely:
 * <ol>
 *   <li>Delete this file ({@code DemoRunner.java}).</li>
 *   <li>Remove the {@code m2} node from {@code Addons.xcu}.</li>
 *   <li>Remove the {@code DemoRunner} call in {@code SmartArtCommand.dispatch()}.</li>
 * </ol>
 */
final class DemoRunner {

    private static final String I  = "- ";    // one dash level (level 2)
    private static final String I2 = "-- ";   // two dash levels (level 3)

    /**
     * One entry per diagram type: {DiagramType, slide-label, sample-input, screenshot-slug}.
     * Sample texts are hard-coded so the demo never touches the dialog or parser error paths.
     */
    private static final Object[][] DEMOS = {
        {
            DiagramType.HIERARCHY, "Hierarchy", "hierarchy",
            "Company\n"
            + I  + "Products\n"
            + I2 + "Alpha\n"
            + I2 + "Bravo\n"
            + I  + "Services\n"
            + I2 + "Charlie\n"
            + I2 + "Delta"
        },
        {
            DiagramType.HUB_AND_SPOKE, "Hub & Spoke", "hub-and-spoke",
            "Innovation\n"
            + I  + "People\n"
            + I2 + "Training\n"
            + I  + "Process\n"
            + I  + "Technology\n"
            + I  + "Partners"
        },
        {
            DiagramType.PROCESS_FLOW, "Process Flow", "process-flow",
            "Research\n"
            + I + "Survey\n"
            + I + "Analysis\n"
            + "Design\n"
            + I + "Prototype\n"
            + I + "Testing\n"
            + "Launch"
        },
        {
            DiagramType.SEQUENTIAL_CHEVRON, "Sequential Chevron", "sequential-chevron",
            "Plan\n"
            + I + "Scope\n"
            + I + "Schedule\n"
            + "Execute\n"
            + I + "Build\n"
            + I + "Test\n"
            + "Review"
        },
        {
            DiagramType.CYCLE, "Cycle", "cycle",
            "Plan\nDo\nCheck\nAct"
        },
        {
            DiagramType.CYCLE_ARROW, "Cycle (Arrows)", "cycle-arrows",
            "Plan\nDo\nCheck\nAct"
        },
        {
            DiagramType.CYCLE_BLOCK, "Cycle (Blocks)", "cycle-blocks",
            "Plan\nDo\nCheck\nAct"
        },
        {
            DiagramType.PYRAMID, "Pyramid", "pyramid",
            "Vision\n"
            + I  + "Goal A\n"
            + "Strategy\n"
            + I  + "Goal B\n"
            + "Execution\n"
            + I  + "Goal C\n"
            + "Results"
        },
    };

    private final XComponentContext context;

    DemoRunner(XComponentContext context) {
        this.context = context;
    }

    void run() throws Exception {
        XMultiComponentFactory smgr = context.getServiceManager();
        Object desktopObj = smgr.createInstanceWithContext(
                "com.sun.star.frame.Desktop", context);
        XDesktop desktop = UnoRuntime.queryInterface(XDesktop.class, desktopObj);
        XComponent document = desktop.getCurrentComponent();
        if (document == null) {
            throw new Exception("No active document.");
        }
        XModel model = UnoRuntime.queryInterface(XModel.class, document);
        XController controller = model.getCurrentController();
        XDrawView view = UnoRuntime.queryInterface(XDrawView.class, controller);
        if (view == null) {
            throw new Exception("Active document is not an Impress presentation.");
        }
        XDrawPagesSupplier pagesSupplier =
                UnoRuntime.queryInterface(XDrawPagesSupplier.class, document);
        XDrawPages pages = pagesSupplier.getDrawPages();
        XMultiServiceFactory factory =
                UnoRuntime.queryInterface(XMultiServiceFactory.class, document);
        SlideRenderer renderer = new SlideRenderer(context);

        for (Object[] demo : DEMOS) {
            DiagramType type   = (DiagramType) demo[0];
            String slideLabel  = (String) demo[1];
            String slug        = (String) demo[2];
            String inputText   = (String) demo[3];

            // Append a new blank slide.
            pages.insertNewByIndex(pages.getCount());
            XDrawPage page = UnoRuntime.queryInterface(XDrawPage.class,
                    pages.getByIndex(pages.getCount() - 1));

            // Make it the active page so SlideRenderer can find it.
            view.setCurrentPage(page);

            addCornerLabel(factory, page, "[DEV DEMO] " + slideLabel);
            addInputListing(factory, page, inputText);

            ParseResult parsed = new HierarchyParser().parse(inputText);
            if (!parsed.isValid()) {
                LibreOfficeHelper.showMessage(context, "SmartArt Demo – internal error",
                        slideLabel + ": " + parsed.getErrorMessage(), true);
                continue;
            }
            DiagramLayout layout = LayoutFactory.build(type, parsed.getRoot());
            renderer.drawHierarchy(layout, ColorPalette.EMPTY);

            exportScreenshot(document, page, slug);
        }
    }

    /**
     * Exports {@code page} as a 1280×960 PNG to the screenshots directory.
     * Silently skips if the directory does not exist. Output directory is
     * controlled by the system property {@code smartart.screenshots.dir};
     * defaults to {@code ~/Documents/SmartArtForLibreImpress/docs/screenshots}.
     */
    private void exportScreenshot(XComponent document, XDrawPage page, String slug) {
        try {
            String dir = System.getProperty("smartart.screenshots.dir",
                    System.getProperty("user.home")
                    + "/Documents/SmartArtForLibreImpress/docs/screenshots");
            File outDir = new File(dir);
            if (!outDir.exists()) {
                return;
            }
            String fileUrl = new File(outDir, slug + ".png").toURI().toString();

            XMultiComponentFactory smgr = context.getServiceManager();
            Object expObj = smgr.createInstanceWithContext(
                    "com.sun.star.drawing.GraphicExporter", context);
            XExporter exporter = UnoRuntime.queryInterface(XExporter.class, expObj);
            XFilter filter = UnoRuntime.queryInterface(XFilter.class, expObj);

            exporter.setSourceDocument(document);

            filter.filter(new PropertyValue[] {
                pv("MediaType",   "image/png"),
                pv("URL",         fileUrl),
                pv("Selection",   page),
                pv("FilterData",  new PropertyValue[] {
                    pv("PixelWidth",  Integer.valueOf(1280)),
                    pv("PixelHeight", Integer.valueOf(960)),
                }),
            });
        } catch (Exception e) {
            // Screenshot export is best-effort; don't break the demo flow.
        }
    }

    /**
     * Monospace input listing in the bottom-left corner so the viewer can see
     * exactly what text was fed to the parser for this diagram.
     */
    private static void addInputListing(XMultiServiceFactory factory,
            XDrawPage page, String inputText) throws Exception {
        Object shape = factory.createInstance("com.sun.star.drawing.TextShape");
        XShape xShape = UnoRuntime.queryInterface(XShape.class, shape);
        XShapes xShapes = UnoRuntime.queryInterface(XShapes.class, page);
        xShapes.add(xShape);
        xShape.setPosition(new Point(400, 14200));
        xShape.setSize(new Size(9000, 4500));

        XText xText = UnoRuntime.queryInterface(XText.class, shape);
        if (xText != null) {
            xText.setString("Input:\n" + inputText);
        }
        XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, shape);
        props.setPropertyValue("FillStyle",   com.sun.star.drawing.FillStyle.NONE);
        props.setPropertyValue("LineStyle",   com.sun.star.drawing.LineStyle.NONE);
        props.setPropertyValue("CharColor",   Integer.valueOf(0x555555));
        props.setPropertyValue("CharHeight",  Float.valueOf(7.5f));
        props.setPropertyValue("CharFontName", "Liberation Mono");
        props.setPropertyValue("TextAutoGrowHeight", Boolean.TRUE);
    }

    /** Small grey italic label in the top-left corner to identify the slide as a dev demo. */
    private static void addCornerLabel(XMultiServiceFactory factory,
            XDrawPage page, String label) throws Exception {
        Object shape = factory.createInstance("com.sun.star.drawing.TextShape");
        XShape xShape = UnoRuntime.queryInterface(XShape.class, shape);
        XShapes xShapes = UnoRuntime.queryInterface(XShapes.class, page);
        xShapes.add(xShape);
        xShape.setPosition(new Point(400, 300));
        xShape.setSize(new Size(9000, 650));

        XText xText = UnoRuntime.queryInterface(XText.class, shape);
        if (xText != null) {
            xText.setString(label);
        }
        XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, shape);
        props.setPropertyValue("FillStyle",  com.sun.star.drawing.FillStyle.NONE);
        props.setPropertyValue("LineStyle",  com.sun.star.drawing.LineStyle.NONE);
        props.setPropertyValue("CharColor",  Integer.valueOf(0xAAAAAA));
        props.setPropertyValue("CharHeight", Float.valueOf(8f));
        props.setPropertyValue("CharPosture",
                com.sun.star.awt.FontSlant.ITALIC);
    }

    private static PropertyValue pv(String name, Object value) {
        PropertyValue p = new PropertyValue();
        p.Name  = name;
        p.Value = value;
        p.State = PropertyState.DIRECT_VALUE;
        return p;
    }
}
