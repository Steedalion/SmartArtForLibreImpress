package org.libreimpress.smartart;

import com.sun.star.lang.XServiceInfo;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.uno.XComponentContext;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatch;
import com.sun.star.util.URL;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.beans.PropertyValue;

public class SmartArtCommand extends WeakBase implements XDispatchProvider, XDispatch, XServiceInfo {
    private XComponentContext xComponentContext;
    private static final String SERVICE_NAME = "com.sun.star.frame.ProtocolHandler";
    private static final String IMPLEMENTATION_NAME = "org.libreimpress.smartart.SmartArtCommand";

    public SmartArtCommand(XComponentContext xComponentContext) {
        this.xComponentContext = xComponentContext;
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
        if (aURL.Complete.startsWith("org.libreimpress.smartart:")) {
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
        // Phase 2: Proof of concept - shows that menu item works
        // Phase 3+: Will open dialog here
        System.out.println("SmartArt command executed!");
    }
}
