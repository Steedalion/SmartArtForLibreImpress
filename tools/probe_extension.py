#!/usr/bin/env python3
"""Runtime verification probe for the LibreImpress SmartArt extension.

Connects to a headless LibreOffice instance (started by verify-extension.sh) and
asserts two things that structural/packaging checks cannot catch:

  1. The SmartArt menu item is present in LibreOffice's *merged* Addons config.
  2. The command URL actually resolves to a dispatch in an Impress frame.

Check (2) is the decisive one: LibreOffice silently hides an addon menu item
whose command cannot be dispatched, so a null dispatch == an empty submenu. This
is exactly the failure mode caused by the component jar being in the wrong place
(uri is resolved relative to the descriptor) — see impressSmartArt.md §5.5.

Usage: probe_extension.py <port>
Exit code 0 = all checks pass, 1 = a check failed, 2 = could not connect.
"""

import sys
import time
import uno
from com.sun.star.beans import PropertyValue

# --- values from the registration contract (impressSmartArt.md §5.5.2) ---
MENU_NODE = "org.libreimpress.smartart"
COMMAND_URL = "org.libreimpress.smartart:CreateDiagram"
MENU_PATH = "/org.openoffice.Office.Addons/AddonUI/OfficeMenuBar"


def connect(port, tries=60):
    local = uno.getComponentContext()
    resolver = local.ServiceManager.createInstanceWithContext(
        "com.sun.star.bridge.UnoUrlResolver", local)
    last = None
    for _ in range(tries):
        try:
            return resolver.resolve(
                "uno:socket,host=localhost,port=%d;urp;StarOffice.ComponentContext"
                % port)
        except Exception as e:  # noqa: BLE001
            last = e
            time.sleep(0.5)
    print("PROBE: could not connect to port %d: %s" % (port, last))
    sys.exit(2)


def check_config(ctx):
    cp = ctx.ServiceManager.createInstanceWithContext(
        "com.sun.star.configuration.ConfigurationProvider", ctx)
    arg = PropertyValue()
    arg.Name = "nodepath"
    arg.Value = MENU_PATH
    node = cp.createInstanceWithArguments(
        "com.sun.star.configuration.ConfigurationAccess", (arg,))
    if not node.hasByName(MENU_NODE):
        print("FAIL: menu node %r missing from merged config" % MENU_NODE)
        return False
    submenu = node.getByName(MENU_NODE).getByName("Submenu")
    items = list(submenu.getElementNames())
    if not items:
        print("FAIL: menu node present but Submenu is empty")
        return False
    urls = [submenu.getByName(i).getByName("URL") for i in items]
    print("PASS: config has menu %r with submenu items %s (URLs %s)"
          % (MENU_NODE, items, urls))
    return COMMAND_URL in urls


def check_dispatch(ctx):
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    doc = desktop.loadComponentFromURL("private:factory/simpress", "_blank", 0, ())
    try:
        frame = doc.getCurrentController().getFrame()
        transformer = smgr.createInstanceWithContext(
            "com.sun.star.util.URLTransformer", ctx)
        url = uno.createUnoStruct("com.sun.star.util.URL")
        url.Complete = COMMAND_URL
        _, url = transformer.parseStrict(url)
        disp = frame.queryDispatch(url, "_self", 0)
        if disp is None:
            print("FAIL: queryDispatch(%r) returned None — component/dispatch "
                  "broken; LibreOffice will hide the menu item" % COMMAND_URL)
            return False
        print("PASS: queryDispatch(%r) -> %s"
              % (COMMAND_URL, disp.ImplementationName))
        return True
    finally:
        doc.close(False)
        desktop.terminate()


def main():
    if len(sys.argv) < 2:
        print("usage: probe_extension.py <port>")
        sys.exit(2)
    ctx = connect(int(sys.argv[1]))
    ok_config = check_config(ctx)
    ok_dispatch = check_dispatch(ctx)
    if ok_config and ok_dispatch:
        print("PROBE: all checks passed")
        sys.exit(0)
    print("PROBE: one or more checks failed")
    sys.exit(1)


main()
