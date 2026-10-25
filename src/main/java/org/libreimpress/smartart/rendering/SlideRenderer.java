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
    private final StyleTemplate style;

    /** Renders with the {@link StyleTemplate#DEFAULT} look. */
    public SlideRenderer(XComponentContext context) {
        this(context, StyleTemplate.DEFAULT);
    }

    public SlideRenderer(XComponentContext context, StyleTemplate style) {
        this.context = context;
        this.style = style == null ? StyleTemplate.DEFAULT : style;
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
    public XShape drawHierarchy(DiagramLayout layout) throws Exception {
        return drawHierarchy(layout, ColorPalette.EMPTY);
    }

    /**
     * Draws a laid-out diagram onto the current slide of the current document,
     * applying {@code palette} fill colours where set.
     *
     * @return the group holding the diagram, or {@code null} if the diagram
     *         had fewer than two shapes (nothing to group)
     */
    public XShape drawHierarchy(DiagramLayout layout, ColorPalette palette) throws Exception {
        XComponent document = currentComponent();
        XDrawPage page = currentPage(document);
        if (page == null) {
            throw new Exception("No active slide — open a slide in Impress first.");
        }
        XMultiServiceFactory factory =
                UnoRuntime.queryInterface(XMultiServiceFactory.class, document);
        return drawHierarchy(page, factory, layout, palette);
    }

    /**
     * Draws a laid-out diagram onto an explicit {@code page} using the given
     * {@code factory}. Used by preview and screenshot export where the target
     * page is not the desktop's current view.
     *
     * @return the group holding the diagram, or {@code null} if ungrouped
     */
    public XShape drawHierarchy(XDrawPage page, XMultiServiceFactory factory,
            DiagramLayout layout, ColorPalette palette) throws Exception {
        XShapes shapes = UnoRuntime.queryInterface(XShapes.class, page);
        List<XShape> created = new ArrayList<>();

        List<LaidOutShape> laidOut = layout.getShapes();
        XShape[] boxes = new XShape[laidOut.size()];
        int chevronSeq = 0; // sequence counter for chevron colour cycling
        for (int i = 0; i < laidOut.size(); i++) {
            LaidOutShape s = laidOut.get(i);
            String service;
            if (s.getKind() == ShapeKind.ELLIPSE || s.getKind() == ShapeKind.VENN_CIRCLE
                    || s.getKind() == ShapeKind.TARGET_RING
                    || s.getKind() == ShapeKind.TIMELINE_MARKER) {
                service = "com.sun.star.drawing.EllipseShape";
            } else if (s.getKind() == ShapeKind.CHEVRON || s.getKind() == ShapeKind.PENTAGON
                    || s.getKind() == ShapeKind.BLOCK_ARROW) {
                service = "com.sun.star.drawing.CustomShape";
            } else {
                // RECTANGLE, PYRAMID_TIER and MATRIX_CELL
                service = "com.sun.star.drawing.RectangleShape";
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
                        : style.accent(chevronSeq);
                chevronSeq++;
                applyStyle(shape, fill, DefaultPalette.fontSize(s.getLevel()));
            } else if (s.getKind() == ShapeKind.BLOCK_ARROW) {
                int userColor = palette.getFillColor(s.getLevel());
                int fill = (userColor != ColorPalette.UNSET)
                        ? userColor : style.arrowAccent();
                applyStyle(shape, fill, 9f);
                applyBlockArrowGeometry(shape);
                XPropertySet rProps = UnoRuntime.queryInterface(XPropertySet.class, shape);
                rProps.setPropertyValue("RotateAngle", Integer.valueOf(s.getRotateAngle100()));
            } else if (s.getKind() == ShapeKind.PYRAMID_TIER
                    || s.getKind() == ShapeKind.MATRIX_CELL) {
                int userColor = palette.getFillColor(s.getLevel());
                int fill = (userColor != ColorPalette.UNSET)
                        ? userColor
                        : style.accent(chevronSeq);
                chevronSeq++;
                applyStyle(shape, fill, DefaultPalette.fontSize(s.getLevel()));
                roundCorners(shape, style.getCornerRadius());
            } else if (s.getKind() == ShapeKind.TARGET_RING
                    || s.getKind() == ShapeKind.TIMELINE_MARKER) {
                int userColor = palette.getFillColor(s.getLevel());
                int fill = (userColor != ColorPalette.UNSET)
                        ? userColor
                        : style.accent(chevronSeq);
                chevronSeq++;
                applyStyle(shape, fill, DefaultPalette.fontSize(s.getLevel()));
                XPropertySet eProps = UnoRuntime.queryInterface(XPropertySet.class, shape);
                if (s.getKind() == ShapeKind.TARGET_RING) {
                    // Rings stack on top of each other: anchor the label in the
                    // exposed top band and drop the shadow, which would darken
                    // every ring below.
                    eProps.setPropertyValue("TextVerticalAdjust",
                            com.sun.star.drawing.TextVerticalAdjust.TOP);
                }
                eProps.setPropertyValue("Shadow", Boolean.FALSE);
            } else if (s.getKind() == ShapeKind.VENN_CIRCLE) {
                int userColor = palette.getFillColor(s.getLevel());
                int fill = (userColor != ColorPalette.UNSET)
                        ? userColor
                        : style.accent(chevronSeq);
                chevronSeq++;
                applyStyle(shape, fill, DefaultPalette.fontSize(s.getLevel()));
                // Partial transparency so overlapping circles remain visible;
                // drop the shadow, which muddies translucent overlaps.
                XPropertySet vProps = UnoRuntime.queryInterface(XPropertySet.class, shape);
                vProps.setPropertyValue("FillTransparence",
                        Integer.valueOf(style.getVennTransparence()));
                vProps.setPropertyValue("Shadow", Boolean.FALSE);
            } else {
                int userColor = palette.getFillColor(s.getLevel());
                int fill = (userColor != ColorPalette.UNSET)
                        ? userColor
                        : style.fill(s.getKind(), s.getLevel());
                applyStyle(shape, fill, DefaultPalette.fontSize(s.getLevel()));
                // Round rectangle corners (not ellipses).
                if (s.getKind() == ShapeKind.RECTANGLE) {
                    roundCorners(shape, style.getCornerRadius());
                }
            }
            // Small label boxes (process/chevron sub-items) scale text to fit so
            // long single-line labels shrink instead of overflowing the box.
            if (s.scalesTextToFit()) {
                XPropertySet fp = UnoRuntime.queryInterface(XPropertySet.class, shape);
                fp.setPropertyValue("TextFitToSize",
                        com.sun.star.drawing.TextFitToSizeType.PROPORTIONAL);
            }
            XText xText = UnoRuntime.queryInterface(XText.class, shape);
            if (xText != null) {
                xText.setString(s.getText());
                // Apply the text colour to the inserted text itself. Setting
                // CharColor on the shape before it holds text does not reliably
                // colour text added later (especially with TextFitToSize), so
                // colour the runs via a cursor after setString.
                applyTextColor(xText, style.getTextColor());
                if (s.getKind() == ShapeKind.CHEVRON || s.getKind() == ShapeKind.PENTAGON
                        || s.getKind() == ShapeKind.VENN_CIRCLE) {
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
            props.setPropertyValue("LineColor",
                    Integer.valueOf(style.getConnectorColor()));
            props.setPropertyValue("LineWidth",
                    Integer.valueOf(style.getConnectorWidth()));
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

        return groupShapes(page, created);
    }

    /**
     * Writes {@code meta} onto a diagram group: the serialized form into
     * {@code Description} (persisted as {@code <svg:desc>} in ODF) and a
     * friendly label into {@code Name}. No-op on a null group (ungrouped
     * single-shape diagram — such diagrams cannot be edited later).
     */
    public static void stampMetadata(XShape group,
            org.libreimpress.smartart.models.SmartArtMetadata meta) {
        if (group == null || meta == null) {
            return;
        }
        try {
            XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, group);
            props.setPropertyValue("Description", meta.serialize());
            props.setPropertyValue("Name", meta.displayName());
        } catch (Exception e) {
            // Metadata is an enhancement; the drawn diagram is still valid.
        }
    }

    /** Applies solid fill, text colour, font size, and auto-shrink to a shape. */
    private void applyStyle(Object shape, int fillColor, float fontSize)
            throws Exception {
        XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, shape);
        props.setPropertyValue("FillStyle",
                com.sun.star.drawing.FillStyle.SOLID);
        props.setPropertyValue("FillColor", Integer.valueOf(fillColor));
        props.setPropertyValue("CharColor", Integer.valueOf(style.getTextColor()));
        props.setPropertyValue("CharHeight", Float.valueOf(fontSize));
        props.setPropertyValue("LineStyle",
                com.sun.star.drawing.LineStyle.NONE);
        // Soft drop shadow to lift the shape off the slide (template-dependent).
        props.setPropertyValue("Shadow", Boolean.valueOf(style.hasShadow()));
        if (style.hasShadow()) {
            props.setPropertyValue("ShadowColor",
                    Integer.valueOf(style.getShadowColor()));
            props.setPropertyValue("ShadowTransparence",
                    Integer.valueOf(style.getShadowTransparence()));
            props.setPropertyValue("ShadowXDistance",
                    Integer.valueOf(style.getShadowDistance()));
            props.setPropertyValue("ShadowYDistance",
                    Integer.valueOf(style.getShadowDistance()));
        }
        // Wrap text to the shape width and auto-shrink on overflow. Word-wrap is
        // what makes AUTOFIT respect width, so long labels in narrow boxes shrink
        // to fit instead of overflowing — while short text is never enlarged.
        props.setPropertyValue("TextWordWrap", Boolean.TRUE);
        props.setPropertyValue("TextFitToSize",
                com.sun.star.drawing.TextFitToSizeType.AUTOFIT);
    }

    /**
     * Rounds the corners of a {@code RectangleShape} (radius in 1/100 mm). Must
     * only be called on rectangle shapes — {@code CornerRadius} is not a property
     * of ellipses or custom shapes and would throw {@code UnknownPropertyException}.
     */
    private static void roundCorners(Object shape, int radius) throws Exception {
        XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, shape);
        props.setPropertyValue("CornerRadius", Integer.valueOf(radius));
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

    /** Colours all of a shape's text by selecting it with a cursor after insertion. */
    private static void applyTextColor(XText xText, int color) throws Exception {
        com.sun.star.text.XTextCursor cursor = xText.createTextCursor();
        cursor.gotoStart(false);
        cursor.gotoEnd(true);
        XPropertySet cursorProps = UnoRuntime.queryInterface(XPropertySet.class, cursor);
        cursorProps.setPropertyValue("CharColor", Integer.valueOf(color));
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

    /**
     * Groups all the diagram's shapes into one editable group on the page.
     *
     * @return the group shape, or {@code null} if grouping was skipped/failed
     */
    private XShape groupShapes(XDrawPage page, List<XShape> created) {
        if (created.size() < 2) {
            return null;
        }
        try {
            XMultiComponentFactory smgr = context.getServiceManager();
            Object collectionObj = smgr.createInstanceWithContext(
                    "com.sun.star.drawing.ShapeCollection", context);
            XShapes collection = UnoRuntime.queryInterface(XShapes.class, collectionObj);
            for (XShape shape : created) {
                collection.add(shape);
            }
            XShapeGrouper grouper = UnoRuntime.queryInterface(XShapeGrouper.class, page);
            if (grouper != null) {
                return grouper.group(collection);
            }
            return null;
        } catch (Exception e) {
            // Grouping is cosmetic; leave shapes ungrouped if it fails.
            return null;
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
