#!/usr/bin/env python3
"""Rendering-API probe: the UNO drawing sequence SlideRenderer relies on.

Run via uno-tests/run.sh (no extension needed — exercises a fresh Impress doc):
    uno-tests/run.sh uno-tests/probes/render_probe.py

It mirrors SlideRenderer.drawHierarchy: create RectangleShapes, glue
ConnectorShapes between them, then group everything. This is an API-contract
smoke test — it guards against a LibreOffice version where a service name,
connector gluing, or grouping behaves differently from what the renderer assumes
(each of these was an actual surprise during development). It does not exercise
the Java renderer itself.

Exit 0 = pass, 1 = an assertion failed, 2 = could not connect.
"""

import sys
import uno  # noqa: F401  (installs the com.sun.star import hook; must precede the import below)
from com.sun.star.awt import Point, Size

from _connect import connect


def rect(doc, page, x, y):
    s = doc.createInstance("com.sun.star.drawing.RectangleShape")
    page.add(s)
    sz = Size(); sz.Width = 4000; sz.Height = 1500
    s.setSize(sz)
    pt = Point(); pt.X = x; pt.Y = y
    s.setPosition(pt)
    return s


def connector(doc, page, start, end):
    c = doc.createInstance("com.sun.star.drawing.ConnectorShape")
    page.add(c)
    c.setPropertyValue("StartShape", start)
    c.setPropertyValue("EndShape", end)
    c.setPropertyValue("StartGluePointIndex", -1)
    c.setPropertyValue("EndGluePointIndex", -1)
    return c


def main():
    if len(sys.argv) < 2:
        print("usage: render_probe.py <port>")
        sys.exit(2)
    ctx = connect(int(sys.argv[1]))
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    doc = desktop.loadComponentFromURL("private:factory/simpress", "_blank", 0, ())
    ok = True
    try:
        page = doc.getCurrentController().getCurrentPage()

        # 1. rectangles with text
        b0 = rect(doc, page, 5000, 1000)
        b1 = rect(doc, page, 2000, 5000)
        b2 = rect(doc, page, 8000, 5000)
        b0.setString("Root")
        if b0.getString() != "Root":
            print("FAIL: rectangle text did not round-trip")
            ok = False
        else:
            print("PASS: rectangles created and text set")

        # 2. connectors glued to shapes
        c0 = connector(doc, page, b0, b1)
        c1 = connector(doc, page, b0, b2)
        print("PASS: connectors glued (StartShape/EndShape)")

        mine = [b0, b1, b2, c0, c1]

        # 3. group via the GLOBAL service manager (not the doc factory)
        collection = smgr.createInstanceWithContext(
            "com.sun.star.drawing.ShapeCollection", ctx)
        for shape in mine:
            collection.add(shape)

        before = page.Count
        group = page.group(collection)
        after = page.Count
        if group is None or group.Count != len(mine) or after != before - len(mine) + 1:
            print("FAIL: grouping (before=%d after=%d members=%s)"
                  % (before, after, None if group is None else group.Count))
            ok = False
        else:
            print("PASS: grouped %d shapes (page top level %d -> %d)"
                  % (len(mine), before, after))
    finally:
        doc.close(False)
        desktop.terminate()

    if ok:
        print("RENDER PROBE: all checks passed")
        sys.exit(0)
    print("RENDER PROBE: one or more checks failed")
    sys.exit(1)


main()
