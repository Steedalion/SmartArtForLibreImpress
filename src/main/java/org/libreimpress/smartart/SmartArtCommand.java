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
import com.sun.star.beans.PropertyValue;
import com.sun.star.uno.AnyConverter;

import org.libreimpress.smartart.helpers.LibreOfficeHelper;
import org.libreimpress.smartart.layout.DiagramLayout;
import org.libreimpress.smartart.layout.LayoutFactory;
import org.libreimpress.smartart.models.ColorPalette;
import org.libreimpress.smartart.models.DiagramNode;
import org.libreimpress.smartart.models.DiagramType;
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
            SmartArtDialog dialog = new SmartArtDialog(xComponentContext);
            SmartArtDialog.Result result = dialog.show();
            if (result == null) {
                return; // user cancelled
            }

            ParseResult parsed = new HierarchyParser().parse(result.getText());
            if (parsed.isValid()) {
                DiagramNode root = parsed.getRoot();
                DiagramLayout layout = LayoutFactory.build(result.getType(), root);
                ColorPalette palette = PaletteParser.parse(result.getPaletteText());
                new SlideRenderer(xComponentContext).drawHierarchy(layout, palette);
            } else {
                LibreOfficeHelper.showMessage(xComponentContext,
                        "SmartArt – Invalid input", parsed.getErrorMessage(), true);
            }
        } catch (Exception e) {
            LibreOfficeHelper.showMessage(xComponentContext,
                    "SmartArt – Error", String.valueOf(e), true);
        }
    }
}
