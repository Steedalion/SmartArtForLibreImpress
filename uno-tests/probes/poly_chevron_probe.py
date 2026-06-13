#!/usr/bin/env python3
"""Probe: draw a chevron and pentagon using PolyPolygonShape with explicit vertices.

If this renders correctly (visible in the PPTX XML as a polygon), we can use
the same technique in the Java renderer.

Run via:
    uno-tests/run.sh uno-tests/probes/poly_chevron_probe.py

Exit 0 = shapes created and PPTX saved (inspect manually or check XML).
Exit 2 = could not connect.
"""

import sys
import os
import zipfile
import tempfile
import re

import uno  # noqa: F401
from com.sun.star.awt import Point, Size
from com.sun.star.beans import PropertyValue

from _connect import connect


def make_point(x, y):
    p = Point()
    p.X = x
    p.Y = y
    return p


def make_polygon_shape(doc, page, pts, label, color=0x4472C4):
    """Generic polygon shape from a list of Point objects."""
    s = doc.createInstance("com.sun.star.drawing.PolyPolygonShape")
    page.add(s)
    # PolyPolygon expects sequence<sequence<Point>>; inner sequence must be a tuple.
    # Try attribute assignment (avoids type-checking in setPropertyValue path).
    try:
        s.PolyPolygon = (tuple(pts),)
    except Exception as e1:
        print(f"  PolyPolygon attr failed: {e1}")
        try:
            s.setPropertyValue("PolyPolygon", (tuple(pts),))
        except Exception as e2:
            print(f"  PolyPolygon setPropertyValue also failed: {e2}")
            return None
    try:
        s.setString(label)
    except Exception:
        pass
    try:
        s.setPropertyValue("FillStyle",
            uno.Enum("com.sun.star.drawing.FillStyle", "SOLID"))
        s.setPropertyValue("FillColor", color)
    except Exception as e:
        print(f"  Fill style error: {e}")
    return s


def make_pentagon(doc, page, x, y, w, h):
    """Flat-left, pointed-right pentagon (first chevron step)."""
    tip = w // 4
    pts = [
        make_point(x,           y),
        make_point(x + w - tip, y),
        make_point(x + w,       y + h // 2),
        make_point(x + w - tip, y + h),
        make_point(x,           y + h),
    ]
    return make_polygon_shape(doc, page, pts, "Step 1")


def make_chevron(doc, page, x, y, w, h):
    """Notched-left, pointed-right chevron (subsequent steps)."""
    tip   = w // 4
    notch = w // 6
    pts = [
        make_point(x + notch,   y),
        make_point(x + w - tip, y),
        make_point(x + w,       y + h // 2),
        make_point(x + w - tip, y + h),
        make_point(x + notch,   y + h),
        make_point(x,           y + h // 2),
    ]
    return make_polygon_shape(doc, page, pts, "Step 2")


def main():
    if len(sys.argv) < 2:
        print("usage: poly_chevron_probe.py <port>")
        sys.exit(2)
    ctx = connect(int(sys.argv[1]))
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    doc = desktop.loadComponentFromURL("private:factory/simpress", "_blank", 0, ())
    tmp_path = tempfile.mktemp(suffix=".pptx")
    ok = True
    try:
        page = doc.getCurrentController().getCurrentPage()

        # Draw a row of: pentagon, chevron, chevron
        W, H = 4000, 1500
        GAP = 500
        x = 1000
        y = 3000

        make_pentagon(doc, page, x, y, W, H)
        x += W + GAP
        make_chevron(doc, page, x, y, W, H)
        x += W + GAP
        make_chevron(doc, page, x, y, W, H)

        n = page.Count
        print(f"Page now has {n} shapes")

        # Save as PPTX and check XML
        filter_args = (
            PropertyValue("FilterName", 0, "Impress MS PowerPoint 2007 XML", 0),
        )
        doc.storeToURL("file://" + tmp_path, filter_args)
        print(f"Saved to {tmp_path}")

        with zipfile.ZipFile(tmp_path, 'r') as z:
            slide_names = sorted(n for n in z.namelist()
                                  if n.startswith("ppt/slides/slide") and n.endswith(".xml"))
            if slide_names:
                xml = z.read(slide_names[0]).decode("utf-8")
                # Look for custom geometry (polygon shapes become custGeom in PPTX)
                cust = len(re.findall(r'<a:custGeom', xml))
                prst = re.findall(r'prstGeom prst="([^"]+)"', xml)
                pts_count = len(re.findall(r'<a:pt ', xml))
                print(f"custGeom elements: {cust}")
                print(f"prstGeom presets:  {prst}")
                print(f"<a:pt> point elements: {pts_count}")
                if cust >= 3 or pts_count >= 6:
                    print("OK: polygon shapes found in PPTX XML")
                else:
                    print("WARN: expected polygon data not found")
                    ok = False
    finally:
        try:
            doc.close(True)
        except Exception:
            pass
        try:
            os.unlink(tmp_path)
        except Exception:
            pass

    sys.exit(0 if ok else 1)


main()
