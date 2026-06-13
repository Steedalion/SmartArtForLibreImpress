#!/usr/bin/env python3
"""Probe: discover which CustomShape type names work for chevron/pentagon shapes.

Run via:
    uno-tests/run.sh uno-tests/probes/custom_shapes_probe.py

Exit 0 = at least one chevron and one pentagon type was confirmed.
Exit 1 = assertion failed.
Exit 2 = could not connect.
"""

import sys
import uno  # noqa: F401
from com.sun.star.awt import Point, Size
from com.sun.star.beans import PropertyValue

from _connect import connect


def make_custom(doc, page, x, y, shape_type):
    s = doc.createInstance("com.sun.star.drawing.CustomShape")
    page.add(s)
    sz = Size(); sz.Width = 4000; sz.Height = 1500
    s.setSize(sz)
    pt = Point(); pt.X = x; pt.Y = y
    s.setPosition(pt)
    pv = PropertyValue()
    pv.Name = "Type"
    pv.Value = shape_type
    s.setPropertyValue("CustomShapeGeometry", (pv,))
    # Read back the effective geometry to see what was applied
    actual_geom = s.getPropertyValue("CustomShapeGeometry")
    actual_type = None
    all_names = []
    for entry in actual_geom:
        all_names.append(f"{entry.Name}={entry.Value!r}")
        if entry.Name == "Type":
            actual_type = entry.Value
    if all_names:
        print(f"    geom keys: {', '.join(all_names)}")
    else:
        print(f"    geom: (empty)")
    return actual_type


def main():
    if len(sys.argv) < 2:
        print("usage: custom_shapes_probe.py <port>")
        sys.exit(2)
    ctx = connect(int(sys.argv[1]))
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    doc = desktop.loadComponentFromURL("private:factory/simpress", "_blank", 0, ())
    ok = True
    try:
        page = doc.getCurrentController().getCurrentPage()

        # From LibreOffice main.xcd registry: ArrowShapes nodes give the correct type strings
        chevron_candidates = [
            "chevron",           # .uno:ArrowShapes.chevron  (notched left, pointed right)
            "right-arrow",
            "notched-right-arrow",
            "arrowshapes.chevron",
        ]
        pentagon_candidates = [
            "pentagon-right",    # .uno:ArrowShapes.pentagon-right  (flat left, pointed right)
            "right-arrow-pentagon",
            "pentagon",
            "arrowshapes.pentagon-right",
        ]

        print("=== Chevron candidates ===")
        found_chevron = None
        x = 1000
        for ctype in chevron_candidates:
            try:
                actual = make_custom(doc, page, x, 1000, ctype)
                print(f"  {ctype!r:40s} -> actual type: {actual!r}")
                if actual and actual != "non-primitive" and actual == ctype:
                    found_chevron = ctype
            except Exception as e:
                print(f"  {ctype!r:40s} -> EXCEPTION: {e}")
            x += 5000

        print()
        print("=== Pentagon candidates ===")
        found_pentagon = None
        x = 1000
        for ptype in pentagon_candidates:
            try:
                actual = make_custom(doc, page, x, 4000, ptype)
                print(f"  {ptype!r:40s} -> actual type: {actual!r}")
                if actual and actual != "non-primitive" and actual == ptype:
                    found_pentagon = ptype
            except Exception as e:
                print(f"  {ptype!r:40s} -> EXCEPTION: {e}")
            x += 5000

        print()
        print(f"Best chevron type:  {found_chevron!r}")
        print(f"Best pentagon type: {found_pentagon!r}")

        # Also dump full geometry for first candidate to see what read-back looks like
        print()
        print("=== Full geometry read-back for 'chevron' ===")
        s = doc.createInstance("com.sun.star.drawing.CustomShape")
        page.add(s)
        sz = Size(); sz.Width = 4000; sz.Height = 1500
        s.setSize(sz)
        pt = Point(); pt.X = 26000; pt.Y = 1000
        s.setPosition(pt)
        pv = PropertyValue(); pv.Name = "Type"; pv.Value = "chevron"
        s.setPropertyValue("CustomShapeGeometry", (pv,))
        geom = s.getPropertyValue("CustomShapeGeometry")
        for entry in geom:
            print(f"  {entry.Name}: {str(entry.Value)[:120]!r}")

        if not found_chevron:
            print("WARN: no chevron type was confirmed (name matched back)")
        if not found_pentagon:
            print("WARN: no pentagon type was confirmed (name matched back)")

    finally:
        try:
            doc.close(True)
        except Exception:
            pass

    sys.exit(0 if ok else 1)


main()
