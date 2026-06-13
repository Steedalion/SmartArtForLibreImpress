#!/usr/bin/env python3
"""Probe: use .uno:ArrowShapes.chevron dispatcher to insert a real chevron,
then read back its full CustomShapeGeometry so we can hardcode it in Java.

Run via:
    uno-tests/run.sh uno-tests/probes/dispatch_shape_probe.py

Exit 0 = successfully read geometry; prints it for use in Java.
Exit 2 = could not connect.
"""

import sys
import time

import uno  # noqa: F401
from com.sun.star.awt import Point, Size, Rectangle
from com.sun.star.beans import PropertyValue

from _connect import connect


def insert_shape_via_dispatch(frame, uno_command):
    """Dispatch a shape-insert UNO command and return immediately."""
    dispatcher = frame.queryDispatch(
        make_url(frame, uno_command), "_self", 0)
    if dispatcher is None:
        print(f"No dispatcher for {uno_command}")
        return False
    dispatcher.dispatch(make_url(frame, uno_command), ())
    return True


def make_url(frame, cmd):
    smgr = frame.queryInterface(
        uno.getTypeByName("com.sun.star.lang.XMultiServiceFactory"))
    # Use createUnoService approach
    ctx = uno.getComponentContext()
    transformer = ctx.ServiceManager.createInstanceWithContext(
        "com.sun.star.util.URLTransformer", ctx)
    url = uno.createUnoStruct("com.sun.star.util.URL")
    url.Complete = cmd
    result = transformer.parseStrict(url)
    return result[1] if isinstance(result, tuple) else url


def main():
    if len(sys.argv) < 2:
        print("usage: dispatch_shape_probe.py <port>")
        sys.exit(2)
    ctx = connect(int(sys.argv[1]))
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    doc = desktop.loadComponentFromURL("private:factory/simpress", "_blank", 0, ())

    try:
        controller = doc.getCurrentController()
        frame = controller.getFrame()
        page = controller.getCurrentPage()

        # Try dispatching the chevron shape command
        transformer = smgr.createInstanceWithContext(
            "com.sun.star.util.URLTransformer", ctx)

        shapes_to_test = [
            ".uno:ArrowShapes.chevron",
            ".uno:ArrowShapes.pentagon-right",
            ".uno:BasicShapes.pentagon",
        ]

        for cmd in shapes_to_test:
            url = uno.createUnoStruct("com.sun.star.util.URL")
            url.Complete = cmd
            ok, parsed_url = transformer.parseStrict(url)
            if not ok:
                print(f"Could not parse URL: {cmd}")
                continue

            dispatcher = frame.queryDispatch(parsed_url, "_self", 0)
            if dispatcher is None:
                print(f"No dispatcher for: {cmd}")
                continue

            # Set up a draw mode so the shape will be placed, not just selected
            # We need to set the shape bounds before dispatching
            # Actually, dispatch with specific args to set the shape
            args = (
                PropertyValue("FillColor", 0, 0x729FCF, 0),
            )
            dispatcher.dispatch(parsed_url, ())
            time.sleep(0.3)  # Give LO time to process

            # Check what shapes are now on the page
            n = page.Count
            print(f"After '{cmd}': page has {n} shapes")
            if n > 0:
                last = page.getByIndex(n - 1)
                print(f"  Last shape class: {last.getImplementationName() if hasattr(last, 'getImplementationName') else 'unknown'}")
                try:
                    geom = last.getPropertyValue("CustomShapeGeometry")
                    if geom:
                        print(f"  CustomShapeGeometry ({len(geom)} entries):")
                        for entry in geom:
                            val_str = repr(entry.Value)[:200]
                            print(f"    {entry.Name}: {val_str}")
                    else:
                        print("  CustomShapeGeometry: empty or None")
                except Exception as e:
                    print(f"  CustomShapeGeometry error: {e}")

    finally:
        try:
            doc.close(True)
        except Exception:
            pass

    sys.exit(0)


main()
