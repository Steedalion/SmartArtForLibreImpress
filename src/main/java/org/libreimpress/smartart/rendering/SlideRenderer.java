package org.libreimpress.smartart.rendering;

import com.sun.star.awt.Point;
import com.sun.star.awt.Size;
import com.sun.star.beans.XPropertySet;
import com.sun.star.drawing.XDrawPage;
import com.sun.star.drawing.XDrawView;
import com.sun.star.drawing.XShape;
import com.sun.star.drawing.XShapeGrouper;
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

import java.util.ArrayList;
import java.util.List;

import org.libreimpress.smartart.layout.DiagramLayout;
import org.libreimpress.smartart.layout.Edge;
import org.libreimpress.smartart.layout.LaidOutShape;
import org.libreimpress.smartart.layout.ShapeKind;
import org.libreimpress.smartart.models.ColorPalette;

/**
 * Draws shapes on the current Impress slide. Phase 4.1 only adds a single
 * rectangle; later sub-phases build layout, connectors and grouping on top.
 *
 * <p>This is UNO runtime glue (like {@code LibreOfficeHelper}) and is verified
 * manually / in Impress rather than by unit tests.
 */
public class SlideRenderer {

    // Placeholder geometry in 1/100 mm; real layout arrives in Phase 4.3.
    private static final int POS_X = 2000;
    private static final int POS_Y = 2000;
    private static final int WIDTH = 6000;
    private static final int HEIGHT = 3000;

    private final XComponentContext context;

    public SlideRenderer(XComponentContext context) {
        this.context = context;
    }

    /** Adds one rectangle containing {@code text} to the current slide. */
    public XShape drawRectangle(String text) throws Exception {
        XComponent document = currentComponent();
        XDrawPage page = currentPage(document);
        if (page == null) {
            throw new Exception("No active slide — open a slide in Impress first.");
        }

        XMultiServiceFactory factory =
                UnoRuntime.queryInterface(XMultiServiceFactory.class, document);
        Object shape = factory.createInstance("com.sun.star.drawing.RectangleShape");
        XShape xShape = UnoRuntime.queryInterface(XShape.class, shape);

        XShapes shapes = UnoRuntime.queryInterface(XShapes.class, page);
        shapes.add(xShape);
        xShape.setSize(new Size(WIDTH, HEIGHT));
        xShape.setPosition(new Point(POS_X, POS_Y));

        XText xText = UnoRuntime.queryInterface(XText.class, shape);
        if (xText != null) {
            xText.setString(text);
        }
        return xShape;
    }

    /**
     * Draws a laid-out diagram using the built-in default palette.
     */
    public void drawHierarchy(DiagramLayout layout) throws Exception {
        drawHierarchy(layout, ColorPalette.EMPTY);
    }

    /**
     * Draws a laid-out diagram, applying {@code palette} fill colours where set
     * and falling back to {@link DefaultPalette} for unset levels.
     */
    public void drawHierarchy(DiagramLayout layout, ColorPalette palette) throws Exception {
        XComponent document = currentComponent();
        XDrawPage page = currentPage(document);
        if (page == null) {
            throw new Exception("No active slide — open a slide in Impress first.");
        }
        XMultiServiceFactory factory =
                UnoRuntime.queryInterface(XMultiServiceFactory.class, document);
        XShapes shapes = UnoRuntime.queryInterface(XShapes.class, page);
        List<XShape> created = new ArrayList<>();

        List<LaidOutShape> laidOut = layout.getShapes();
        XShape[] boxes = new XShape[laidOut.size()];
        int chevronSeq = 0; // sequence counter for chevron colour cycling
        for (int i = 0; i < laidOut.size(); i++) {
            LaidOutShape s = laidOut.get(i);
            String service;
            if (s.getKind() == ShapeKind.ELLIPSE) {
                service = "com.sun.star.drawing.EllipseShape";
            } else if (s.getKind() == ShapeKind.CHEVRON || s.getKind() == ShapeKind.PENTAGON
                    || s.getKind() == ShapeKind.BLOCK_ARROW) {
                service = "com.sun.star.drawing.CustomShape";
            } else {
                service = "com.sun.star.drawing.RectangleShape"; // RECTANGLE and PYRAMID_TIER
            }
            Object shape = factory.createInstance(service);
            XShape xShape = UnoRuntime.queryInterface(XShape.class, shape);
            shapes.add(xShape);
            xShape.setSize(new Size(s.getWidth(), s.getHeight()));
            xShape.setPosition(new Point(s.getX(), s.getY()));
            if (s.getKind() == ShapeKind.CHEVRON || s.getKind() == ShapeKind.PENTAGON) {
                applyChevronGeometry(shape, s.getKind());
                int userColor = palette.getFillColor(s.getLevel());
                int fill = (userColor != ColorPalette.UNSET)
                        ? userColor
                        : DefaultPalette.chevronFill(chevronSeq);
                chevronSeq++;
                applyStyle(shape, fill, DefaultPalette.TEXT_WHITE,
                        DefaultPalette.fontSize(s.getLevel()));
            } else if (s.getKind() == ShapeKind.BLOCK_ARROW) {
                int userColor = palette.getFillColor(s.getLevel());
                int fill = (userColor != ColorPalette.UNSET)
                        ? userColor : DefaultPalette.ARROW_ACCENT;
                applyStyle(shape, fill, DefaultPalette.TEXT_WHITE, 9f);
                applyBlockArrowGeometry(shape);
                XPropertySet rProps = UnoRuntime.queryInterface(XPropertySet.class, shape);
                rProps.setPropertyValue("RotateAngle", Integer.valueOf(s.getRotateAngle100()));
            } else if (s.getKind() == ShapeKind.PYRAMID_TIER) {
                int userColor = palette.getFillColor(s.getLevel());
                int fill = (userColor != ColorPalette.UNSET)
                        ? userColor
                        : DefaultPalette.chevronFill(chevronSeq);
                chevronSeq++;
                applyStyle(shape, fill, DefaultPalette.TEXT_WHITE,
                        DefaultPalette.fontSize(s.getLevel()));
            } else {
                int userColor = palette.getFillColor(s.getLevel());
                int fill = (userColor != ColorPalette.UNSET)
                        ? userColor
                        : DefaultPalette.fill(s.getKind(), s.getLevel());
                applyStyle(shape, fill, DefaultPalette.TEXT_WHITE,
                        DefaultPalette.fontSize(s.getLevel()));
            }
            XText xText = UnoRuntime.queryInterface(XText.class, shape);
            if (xText != null) {
                xText.setString(s.getText());
                if (s.getKind() == ShapeKind.CHEVRON || s.getKind() == ShapeKind.PENTAGON) {
                    centerChevronText(shape, xText);
                }
            }
            boxes[i] = xShape;
            created.add(xShape);
        }

        for (Edge edge : layout.getEdges()) {
            Object connector = factory.createInstance("com.sun.star.drawing.ConnectorShape");
            XShape xConnector = UnoRuntime.queryInterface(XShape.class, connector);
            shapes.add(xConnector);
            XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, connector);
            props.setPropertyValue("StartShape", boxes[edge.getParent()]);
            props.setPropertyValue("EndShape", boxes[edge.getChild()]);
            props.setPropertyValue("StartGluePointIndex",
                    Integer.valueOf(edge.getStartGlue()));
            props.setPropertyValue("EndGluePointIndex",
                    Integer.valueOf(edge.getEndGlue()));
            if (edge.isStraight()) {
                props.setPropertyValue("EdgeKind",
                        com.sun.star.drawing.ConnectorType.LINE);
            } else if (edge.isCurved()) {
                props.setPropertyValue("EdgeKind",
                        com.sun.star.drawing.ConnectorType.CURVE);
            }
            if (edge.hasArrowEnd()) {
                props.setPropertyValue("LineEndName", "Arrow");
                props.setPropertyValue("LineEndWidth", Integer.valueOf(300));
            }
            created.add(xConnector);
        }

        groupShapes(page, created);
    }

    /** Applies solid fill, text colour, font size, and auto-shrink to a shape. */
    private static void applyStyle(Object shape, int fillColor, int textColor,
                                   float fontSize) throws Exception {
        XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, shape);
        props.setPropertyValue("FillStyle",
                com.sun.star.drawing.FillStyle.SOLID);
        props.setPropertyValue("FillColor", Integer.valueOf(fillColor));
        props.setPropertyValue("CharColor", Integer.valueOf(textColor));
        props.setPropertyValue("CharHeight", Float.valueOf(fontSize));
        props.setPropertyValue("LineStyle",
                com.sun.star.drawing.LineStyle.NONE);
        // Auto-shrink text when it doesn't fit; never enlarges text that already fits.
        props.setPropertyValue("TextFitToSize",
                com.sun.star.drawing.TextFitToSizeType.AUTOFIT);
    }

    /**
     * Applies the LibreOffice built-in chevron or pentagon-right preset geometry
     * to a {@code com.sun.star.drawing.CustomShape}.
     *
     * <p>Java's statically-typed {@code PropertyValue[]} array is passed to
     * {@code setPropertyValue} and the UNO bridge annotates the {@code Any} as
     * {@code sequence<PropertyValue>} — the same annotation that Python's
     * attribute-assignment path applies, which is what makes the property take
     * effect in LibreOffice's C++ shape engine.
     */
    private static void applyChevronGeometry(Object shape, ShapeKind kind)
            throws Exception {
        String typeName = (kind == ShapeKind.PENTAGON) ? "pentagon-right" : "chevron";
        com.sun.star.beans.PropertyValue typeVal = new com.sun.star.beans.PropertyValue();
        typeVal.Name  = "Type";
        typeVal.Value = typeName;
        XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, shape);
        props.setPropertyValue("CustomShapeGeometry",
                new com.sun.star.beans.PropertyValue[]{ typeVal });
    }

    /** Applies the {@code right-arrow} block-arrow preset geometry. */
    private static void applyBlockArrowGeometry(Object shape) throws Exception {
        com.sun.star.beans.PropertyValue typeVal = new com.sun.star.beans.PropertyValue();
        typeVal.Name  = "Type";
        typeVal.Value = "right-arrow";
        XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, shape);
        props.setPropertyValue("CustomShapeGeometry",
                new com.sun.star.beans.PropertyValue[]{ typeVal });
    }

    /** Centers text both vertically and horizontally inside a chevron/pentagon. */
    private static void centerChevronText(Object shape, XText xText) throws Exception {
        XPropertySet shapeProps = UnoRuntime.queryInterface(XPropertySet.class, shape);
        shapeProps.setPropertyValue("TextVerticalAdjust",
                com.sun.star.drawing.TextVerticalAdjust.CENTER);

        com.sun.star.text.XTextCursor cursor = xText.createTextCursor();
        cursor.gotoStart(false);
        cursor.gotoEnd(true);
        XPropertySet cursorProps = UnoRuntime.queryInterface(XPropertySet.class, cursor);
        cursorProps.setPropertyValue("ParaAdjust",
                com.sun.star.style.ParagraphAdjust.CENTER);
    }

    /** Groups all the diagram's shapes into one editable group on the page. */
    private void groupShapes(XDrawPage page, List<XShape> created) throws Exception {
        if (created.size() < 2) {
            return; // nothing meaningful to group
        }
        // ShapeCollection is a global service, not a document one.
        XMultiComponentFactory smgr = context.getServiceManager();
        Object collectionObj =
                smgr.createInstanceWithContext("com.sun.star.drawing.ShapeCollection", context);
        XShapes collection = UnoRuntime.queryInterface(XShapes.class, collectionObj);
        for (XShape shape : created) {
            collection.add(shape);
        }
        XShapeGrouper grouper = UnoRuntime.queryInterface(XShapeGrouper.class, page);
        if (grouper != null) {
            grouper.group(collection);
        }
    }

    private XComponent currentComponent() throws Exception {
        XMultiComponentFactory smgr = context.getServiceManager();
        Object desktopObj =
                smgr.createInstanceWithContext("com.sun.star.frame.Desktop", context);
        XDesktop desktop = UnoRuntime.queryInterface(XDesktop.class, desktopObj);
        XComponent component = desktop.getCurrentComponent();
        if (component == null) {
            throw new Exception("No active document.");
        }
        return component;
    }

    private XDrawPage currentPage(XComponent document) {
        XModel model = UnoRuntime.queryInterface(XModel.class, document);
        if (model == null) {
            return null;
        }
        XController controller = model.getCurrentController();
        XDrawView view = UnoRuntime.queryInterface(XDrawView.class, controller);
        return view == null ? null : view.getCurrentPage();
    }
}
