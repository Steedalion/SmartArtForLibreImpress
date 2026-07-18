#!/usr/bin/env python3
"""Registration & dispatch probe for the SmartArt extension.

Run via uno-tests/run.sh with the extension installed:
    uno-tests/run.sh --install target/SmartArt.oxt uno-tests/probes/registration_probe.py

Asserts two things that packaging/structural checks cannot:
  1. The SmartArt menu item is present in LibreOffice's *merged* Addons config.
  2. The command URL resolves to a dispatch in an Impress frame.

Check (2) is decisive: LibreOffice hides an addon menu item whose command cannot
be dispatched, so a null dispatch == an empty submenu (e.g. the component jar in
the wrong place). See docs/plans/Phase2_ImplementationPlan.md §15.

Exit 0 = pass, 1 = a check failed, 2 = could not connect.
"""

import sys
import uno
from com.sun.star.beans import PropertyValue

from _connect import connect

MENU_NODE = "org.libreimpress.smartart"
COMMAND_URL = "org.libreimpress.smartart:CreateDiagram"
MENU_PATH = "/org.openoffice.Office.Addons/AddonUI/OfficeMenuBar"


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
        print("usage: registration_probe.py <port>")
        sys.exit(2)
    ctx = connect(int(sys.argv[1]))
    ok_config = check_config(ctx)
    ok_dispatch = check_dispatch(ctx)
    if ok_config and ok_dispatch:
        print("REGISTRATION PROBE: all checks passed")
        sys.exit(0)
    print("REGISTRATION PROBE: one or more checks failed")
    sys.exit(1)


main()
