package org.libreimpress.smartart;

import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.uno.XComponentContext;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatch;
import com.sun.star.util.URL;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.awt.Point;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XChild;
import com.sun.star.drawing.XShape;
import com.sun.star.drawing.XShapes;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XModel;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.view.XSelectionSupplier;

import java.util.Optional;

import org.libreimpress.smartart.helpers.LibreOfficeHelper;
import org.libreimpress.smartart.layout.DiagramLayout;
import org.libreimpress.smartart.layout.LayoutFactory;
import org.libreimpress.smartart.models.ColorPalette;
import org.libreimpress.smartart.models.DiagramNode;
import org.libreimpress.smartart.models.DiagramType;
import org.libreimpress.smartart.models.SmartArtMetadata;
import org.libreimpress.smartart.parsers.HierarchyParser;
import org.libreimpress.smartart.parsers.PaletteParser;
import org.libreimpress.smartart.parsers.ParseResult;
import org.libreimpress.smartart.rendering.SlideRenderer;

public class SmartArtCommand extends WeakBase implements XDispatchProvider, XDispatch, XServiceInfo {
    private XComponentContext xComponentContext;
    private static final String SERVICE_NAME = "com.sun.star.frame.ProtocolHandler";
    private static final String IMPLEMENTATION_NAME = "org.libreimpress.smartart.SmartArtCommand";

    public SmartArtCommand(XComponentContext xComponentContext) {
        this.xComponentContext = xComponentContext;
    }

    /**
     * Returns a factory for creating this component. Called by the UNO Java2
     * loader when the service is requested.
     */
    public static XSingleComponentFactory __getComponentFactory(String sImplementationName) {
        XSingleComponentFactory xFactory = null;
        if (sImplementationName.equals(IMPLEMENTATION_NAME)) {
            xFactory = Factory.createComponentFactory(SmartArtCommand.class,
                    new String[]{SERVICE_NAME});
        }
        return xFactory;
    }

    /**
     * Writes the service registration information into the given registry key.
     */
    public static boolean __writeRegistryServiceInfo(XRegistryKey xRegistryKey) {
        return Factory.writeRegistryServiceInfo(IMPLEMENTATION_NAME,
                new String[]{SERVICE_NAME}, xRegistryKey);
    }

    @Override
    public XDispatch queryDispatch(URL aURL, String sTargetFrameName, int iSearchFlags) {
        if (aURL.Complete.startsWith("org.libreimpress.smartart:")) {
            return this;
        }
        return null;
    }

    @Override
    public XDispatch[] queryDispatches(DispatchDescriptor[] aRequests) {
        XDispatch[] dispatches = new XDispatch[aRequests.length];
        for (int i = 0; i < aRequests.length; i++) {
            dispatches[i] = queryDispatch(aRequests[i].FeatureURL, aRequests[i].FrameName, aRequests[i].SearchFlags);
        }
        return dispatches;
    }

    @Override
    public void dispatch(URL aURL, PropertyValue[] aArguments) {
        if ("org.libreimpress.smartart:Demo".equals(aURL.Complete)) {
            // DEV ONLY — remove this branch together with DemoRunner.java and Addons.xcu m2.
            try {
                // An "OutputDir" dispatch argument switches DemoRunner into
                // headless/CI mode: PNGs and a demo-result.txt land there.
                String outputDir = null;
                for (PropertyValue arg : aArguments) {
                    if ("OutputDir".equals(arg.Name)
                            && AnyConverter.isString(arg.Value)) {
                        outputDir = AnyConverter.toString(arg.Value);
                    }
                }
                new DemoRunner(xComponentContext).run(outputDir);
            } catch (Exception e) {
                LibreOfficeHelper.showMessage(xComponentContext,
                        "SmartArt Demo – Error", String.valueOf(e), true);
            }
        } else if (aURL.Complete.startsWith("org.libreimpress.smartart:")) {
            execute();
        }
    }

    @Override
    public void addStatusListener(com.sun.star.frame.XStatusListener xControl, URL aURL) {
        // Phase 2: Not implemented yet
    }

    @Override
    public void removeStatusListener(com.sun.star.frame.XStatusListener xControl, URL aURL) {
        // Phase 2: Not implemented yet
    }

    @Override
    public String getImplementationName() {
        return IMPLEMENTATION_NAME;
    }

    @Override
    public boolean supportsService(String sService) {
        return sService.equals(SERVICE_NAME);
    }

    @Override
    public String[] getSupportedServiceNames() {
        return new String[]{SERVICE_NAME};
    }

    private void execute() {
        try {
            // Selection must be read BEFORE the modal dialog opens — inside the
            // event loop getCurrentComponent() can return the dialog itself.
            EditTarget target = findEditTarget();

            SmartArtDialog dialog = new SmartArtDialog(xComponentContext);
            SmartArtDialog.Result result =
                    dialog.show(target == null ? null : target.meta);
            if (result == null) {
                return; // user cancelled
            }

            ParseResult parsed = new HierarchyParser().parse(result.getText());
            if (parsed.isValid()) {
                DiagramNode root = parsed.getRoot();
                DiagramLayout layout = LayoutFactory.build(result.getType(), root);
                ColorPalette palette = PaletteParser.parse(result.getPaletteText());
                XShape group = new SlideRenderer(xComponentContext)
                        .drawHierarchy(layout, palette);
                String template =
                        target != null ? target.meta.getTemplate() : "DEFAULT";
                SlideRenderer.stampMetadata(group, new SmartArtMetadata(
                        result.getType(), template, result.getPaletteText(),
                        result.getText()));
                if (target != null) {
                    // Replace in place: the old diagram is removed only after
                    // the new one rendered, and the new group inherits the old
                    // one's (possibly user-adjusted) position.
                    if (group != null) {
                        group.setPosition(target.position);
                    }
                    target.parent.remove(target.group);
                }
            } else {
                LibreOfficeHelper.showMessage(xComponentContext,
                        "SmartArt – Invalid input", parsed.getErrorMessage(), true);
            }
        } catch (Exception e) {
            LibreOfficeHelper.showMessage(xComponentContext,
                    "SmartArt – Error", String.valueOf(e), true);
        }
    }

    /** A previously generated diagram the user selected for editing. */
    private static final class EditTarget {
        final SmartArtMetadata meta;
        final XShape group;
        final XShapes parent;
        final Point position;

        EditTarget(SmartArtMetadata meta, XShape group, XShapes parent,
                Point position) {
            this.meta = meta;
            this.group = group;
            this.parent = parent;
            this.position = position;
        }
    }

    /**
     * Returns the selected SmartArt group if the current selection is exactly
     * one shape carrying parseable SmartArt metadata in its {@code Description};
     * {@code null} otherwise (→ ordinary create flow).
     */
    private EditTarget findEditTarget() {
        try {
            Object desktopObj = xComponentContext.getServiceManager()
                    .createInstanceWithContext("com.sun.star.frame.Desktop",
                            xComponentContext);
            XDesktop desktop = UnoRuntime.queryInterface(XDesktop.class, desktopObj);
            XModel model = UnoRuntime.queryInterface(XModel.class,
                    desktop.getCurrentComponent());
            if (model == null) {
                return null;
            }
            XSelectionSupplier supplier = UnoRuntime.queryInterface(
                    XSelectionSupplier.class, model.getCurrentController());
            if (supplier == null) {
                return null;
            }
            XShapes selected = UnoRuntime.queryInterface(XShapes.class,
                    supplier.getSelection());
            if (selected == null || selected.getCount() != 1) {
                return null;
            }
            XShape shape = UnoRuntime.queryInterface(XShape.class,
                    selected.getByIndex(0));
            XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, shape);
            Optional<SmartArtMetadata> meta = SmartArtMetadata.tryParse(
                    AnyConverter.toString(props.getPropertyValue("Description")));
            if (!meta.isPresent()) {
                return null;
            }
            XChild child = UnoRuntime.queryInterface(XChild.class, shape);
            XShapes parent = UnoRuntime.queryInterface(XShapes.class,
                    child.getParent());
            if (parent == null) {
                return null;
            }
            return new EditTarget(meta.get(), shape, parent, shape.getPosition());
        } catch (Exception e) {
            return null; // any doubt → plain create flow
        }
    }
}
