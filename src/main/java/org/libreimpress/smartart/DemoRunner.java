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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

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
        {
            DiagramType.BASIC_BLOCK_LIST, "Basic Block List", "basic-block-list",
            "Plan\n"
            + I  + "Scope\n"
            + I  + "Budget\n"
            + "Design\n"
            + "Build\n"
            + I  + "Backend\n"
            + I2 + "Database\n"
            + I  + "Frontend\n"
            + "Test\n"
            + "Release\n"
            + "Review"
        },
        {
            DiagramType.VERTICAL_BULLET_LIST, "Vertical Bullet List", "vertical-bullet-list",
            "Introductions\n"
            + I  + "Welcome\n"
            + I  + "Goals for today\n"
            + "Project status\n"
            + I  + "Milestones\n"
            + I2 + "Phase 1 shipped\n"
            + I  + "Risks\n"
            + I  + "Next steps\n"
            + "Q and A\n"
            + I  + "Open floor"
        },
        {
            DiagramType.BASIC_VENN, "Basic Venn", "basic-venn",
            "Quality\nSpeed\nCost"
        },
        {
            DiagramType.BASIC_MATRIX, "Basic Matrix", "basic-matrix",
            "Urgent and Important\n"
            + I  + "Do now\n"
            + "Important, Not Urgent\n"
            + I  + "Schedule\n"
            + "Urgent, Not Important\n"
            + I  + "Delegate\n"
            + "Neither\n"
            + I  + "Drop"
        },
    };

    private final XComponentContext context;

    DemoRunner(XComponentContext context) {
        this.context = context;
    }

    void run() throws Exception {
        run(null);
    }

    /**
     * Runs the demo. When {@code outputDir} is non-null the runner is in
     * headless/CI mode: PNGs go to that directory (created if missing), a
     * {@code demo-result.txt} with one {@code OK <slug>} / {@code FAIL <slug>}
     * line per diagram type is written there, no message boxes are shown, and
     * screenshot-export errors count as failures instead of being best-effort.
     */
    void run(String outputDir) throws Exception {
        boolean headless = outputDir != null;
        List<String> results = new ArrayList<>();
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

            try {
                // Append a new blank slide.
                pages.insertNewByIndex(pages.getCount());
                XDrawPage page = UnoRuntime.queryInterface(XDrawPage.class,
                        pages.getByIndex(pages.getCount() - 1));

                // Make it the active page so SlideRenderer can find it.
                view.setCurrentPage(page);

                ParseResult parsed = new HierarchyParser().parse(inputText);
                if (!parsed.isValid()) {
                    throw new Exception("parse: " + parsed.getErrorMessage());
                }
                DiagramLayout layout = LayoutFactory.build(type, parsed.getRoot());
                renderer.drawHierarchy(layout, ColorPalette.EMPTY);

                // Export a clean screenshot of just the diagram, then annotate the
                // on-screen dev slide with the label/input listing afterwards so the
                // dev chrome never appears in the exported PNG.
                exportScreenshot(document, page, slug, outputDir);

                addCornerLabel(factory, page, "[DEV DEMO] " + slideLabel);
                addInputListing(factory, page, inputText);
                results.add("OK " + slug);
            } catch (Exception e) {
                results.add("FAIL " + slug + ": " + e);
                if (!headless) {
                    LibreOfficeHelper.showMessage(context, "SmartArt Demo – error",
                            slideLabel + ": " + e, true);
                }
            }
        }

        if (headless) {
            Files.write(new File(outputDir, "demo-result.txt").toPath(),
                    (String.join("\n", results) + "\n")
                            .getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Exports {@code page} as a 1280×960 PNG to the screenshots directory.
     *
     * <p>Interactive mode ({@code outputDir} null): best-effort — the directory
     * comes from the system property {@code smartart.screenshots.dir} (default
     * {@code ~/Documents/SmartArtForLibreImpress/docs/screenshots}), a missing
     * directory or export error is silently ignored.
     *
     * <p>Headless mode ({@code outputDir} non-null): the directory is created if
     * missing and any export error propagates so the type is recorded as FAIL.
     */
    private void exportScreenshot(XComponent document, XDrawPage page, String slug,
            String outputDir) throws Exception {
        boolean headless = outputDir != null;
        String dir = headless ? outputDir
                : System.getProperty("smartart.screenshots.dir",
                        System.getProperty("user.home")
                        + "/Documents/SmartArtForLibreImpress/docs/screenshots");
        File outDir = new File(dir);
        if (!outDir.exists()) {
            if (!headless) {
                return;
            }
            if (!outDir.mkdirs()) {
                throw new Exception("cannot create output directory " + dir);
            }
        }
        try {
            String fileUrl = new File(outDir, slug + ".png").toURI().toString();

            XMultiComponentFactory smgr = context.getServiceManager();
            Object expObj = smgr.createInstanceWithContext(
                    "com.sun.star.drawing.GraphicExportFilter", context);
            XExporter exporter = UnoRuntime.queryInterface(XExporter.class, expObj);
            XFilter filter = UnoRuntime.queryInterface(XFilter.class, expObj);

            // Export the draw page directly — it implements XComponent. Passing the
            // whole document with a Selection needs a live view and can fail.
            XComponent pageComp = UnoRuntime.queryInterface(XComponent.class, page);
            exporter.setSourceDocument(pageComp);

            filter.filter(new PropertyValue[] {
                pv("MediaType",   "image/png"),
                pv("URL",         fileUrl),
                pv("FilterData",  new PropertyValue[] {
                    pv("PixelWidth",  Integer.valueOf(1280)),
                    pv("PixelHeight", Integer.valueOf(960)),
                }),
            });
        } catch (Exception e) {
            if (headless) {
                throw e;
            }
            // Interactive: screenshot export is best-effort; don't break the demo.
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
