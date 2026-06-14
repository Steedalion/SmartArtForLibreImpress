package org.libreimpress.smartart;

import com.sun.star.awt.Point;
import com.sun.star.awt.Size;
import com.sun.star.beans.XPropertySet;
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

import org.libreimpress.smartart.helpers.LibreOfficeHelper;
import org.libreimpress.smartart.layout.CycleArrowLayout;
import org.libreimpress.smartart.layout.CycleBlockLayout;
import org.libreimpress.smartart.layout.CycleLayout;
import org.libreimpress.smartart.layout.DiagramLayout;
import org.libreimpress.smartart.layout.HierarchyLayout;
import org.libreimpress.smartart.layout.HubAndSpokeLayout;
import org.libreimpress.smartart.layout.ProcessFlowLayout;
import org.libreimpress.smartart.layout.PyramidLayout;
import org.libreimpress.smartart.layout.SequentialChevronLayout;
import org.libreimpress.smartart.models.ColorPalette;
import org.libreimpress.smartart.models.DiagramNode;
import org.libreimpress.smartart.models.DiagramType;
import org.libreimpress.smartart.parsers.HierarchyParser;
import org.libreimpress.smartart.parsers.ParseResult;
import org.libreimpress.smartart.rendering.SlideRenderer;

/**
 * [DEV ONLY] Appends one demo slide per diagram type to the current presentation.
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
     * One entry per diagram type: {DiagramType, slide-label, sample-input}.
     * Sample texts are hard-coded so the demo never touches the dialog or parser error paths.
     */
    private static final Object[][] DEMOS = {
        {
            DiagramType.HIERARCHY, "Hierarchy",
            "Company\n"
            + I  + "Products\n"
            + I2 + "Alpha\n"
            + I2 + "Bravo\n"
            + I  + "Services\n"
            + I2 + "Charlie\n"
            + I2 + "Delta"
        },
        {
            DiagramType.HUB_AND_SPOKE, "Hub & Spoke",
            "Innovation\n"
            + I  + "People\n"
            + I2 + "Training\n"
            + I  + "Process\n"
            + I  + "Technology\n"
            + I  + "Partners"
        },
        {
            DiagramType.PROCESS_FLOW, "Process Flow",
            "Research\n"
            + I + "Survey\n"
            + I + "Analysis\n"
            + "Design\n"
            + I + "Prototype\n"
            + I + "Testing\n"
            + "Launch"
        },
        {
            DiagramType.SEQUENTIAL_CHEVRON, "Sequential Chevron",
            "Plan\n"
            + I + "Scope\n"
            + I + "Schedule\n"
            + "Execute\n"
            + I + "Build\n"
            + I + "Test\n"
            + "Review"
        },
        {
            DiagramType.CYCLE, "Cycle",
            "Plan\nDo\nCheck\nAct"
        },
        {
            DiagramType.CYCLE_ARROW, "Cycle (Arrows)",
            "Plan\nDo\nCheck\nAct"
        },
        {
            DiagramType.CYCLE_BLOCK, "Cycle (Blocks)",
            "Plan\nDo\nCheck\nAct"
        },
        {
            DiagramType.PYRAMID, "Pyramid",
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
            String inputText   = (String) demo[2];

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
                // Guard against future regressions in the hard-coded strings.
                LibreOfficeHelper.showMessage(context, "SmartArt Demo – internal error",
                        slideLabel + ": " + parsed.getErrorMessage(), true);
                continue;
            }
            DiagramLayout layout = buildLayout(type, parsed.getRoot());
            renderer.drawHierarchy(layout, ColorPalette.EMPTY);
        }
    }

    private static DiagramLayout buildLayout(DiagramType type, DiagramNode root) {
        switch (type) {
            case HUB_AND_SPOKE:      return HubAndSpokeLayout.layout(root);
            case PROCESS_FLOW:       return ProcessFlowLayout.layout(root);
            case SEQUENTIAL_CHEVRON: return SequentialChevronLayout.layout(root);
            case CYCLE:              return CycleLayout.layout(root);
            case CYCLE_ARROW:        return CycleArrowLayout.layout(root);
            case CYCLE_BLOCK:        return CycleBlockLayout.layout(root);
            case PYRAMID:            return PyramidLayout.layout(root);
            default:                 return HierarchyLayout.layout(root);
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
}
