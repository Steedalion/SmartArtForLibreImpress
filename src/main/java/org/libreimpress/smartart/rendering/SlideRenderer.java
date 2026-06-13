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
     * Draws a laid-out hierarchy: one rectangle per node and a connector glued
     * between each parent and child (connectors auto-route, glue indices -1),
     * then groups the whole diagram into one editable group object.
     */
    public void drawHierarchy(DiagramLayout layout) throws Exception {
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
        for (int i = 0; i < laidOut.size(); i++) {
            LaidOutShape s = laidOut.get(i);
            Object shape = factory.createInstance("com.sun.star.drawing.RectangleShape");
            XShape xShape = UnoRuntime.queryInterface(XShape.class, shape);
            shapes.add(xShape);
            xShape.setSize(new Size(s.getWidth(), s.getHeight()));
            xShape.setPosition(new Point(s.getX(), s.getY()));
            XText xText = UnoRuntime.queryInterface(XText.class, shape);
            if (xText != null) {
                xText.setString(s.getText());
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
            props.setPropertyValue("StartGluePointIndex", Integer.valueOf(-1));
            props.setPropertyValue("EndGluePointIndex", Integer.valueOf(-1));
            created.add(xConnector);
        }

        groupShapes(page, created);
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
