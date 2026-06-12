package org.libreimpress.smartart.helpers;

import com.sun.star.awt.MessageBoxButtons;
import com.sun.star.awt.MessageBoxType;
import com.sun.star.awt.XMessageBox;
import com.sun.star.awt.XMessageBoxFactory;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/** Small UNO conveniences shared by the extension. */
public final class LibreOfficeHelper {

    private LibreOfficeHelper() {
    }

    /**
     * Shows a modal message box parented to the current document window.
     * Failures here are non-fatal (logged to stderr) so they never mask the
     * original action.
     */
    public static void showMessage(XComponentContext context, String title,
            String message, boolean isError) {
        try {
            XMultiComponentFactory smgr = context.getServiceManager();
            Object desktopObj = smgr.createInstanceWithContext(
                    "com.sun.star.frame.Desktop", context);
            XDesktop desktop = UnoRuntime.queryInterface(XDesktop.class, desktopObj);
            XFrame frame = desktop.getCurrentFrame();
            XWindowPeer parent = UnoRuntime.queryInterface(
                    XWindowPeer.class, frame.getContainerWindow());

            Object toolkitObj = smgr.createInstanceWithContext(
                    "com.sun.star.awt.Toolkit", context);
            XMessageBoxFactory factory = UnoRuntime.queryInterface(
                    XMessageBoxFactory.class, toolkitObj);

            MessageBoxType type = isError ? MessageBoxType.ERRORBOX : MessageBoxType.INFOBOX;
            XMessageBox box = factory.createMessageBox(
                    parent, type, MessageBoxButtons.BUTTONS_OK, title, message);
            box.execute();

            com.sun.star.lang.XComponent disposable =
                    UnoRuntime.queryInterface(com.sun.star.lang.XComponent.class, box);
            if (disposable != null) {
                disposable.dispose();
            }
        } catch (Exception e) {
            System.err.println("SmartArt: could not show message box: " + e);
            System.err.println("SmartArt [" + title + "]: " + message);
        }
    }
}
