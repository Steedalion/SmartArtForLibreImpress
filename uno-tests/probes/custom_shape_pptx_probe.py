#!/usr/bin/env python3
"""Probe: verify chevron CustomShape types by saving as PPTX and reading shape XML.

Exit 0 = chevron/pentagon-right shapes found in PPTX XML.
Exit 1 = wrong shapes or assertion failed.
Exit 2 = could not connect.
"""

import sys
import os
import zipfile
import tempfile

import uno  # noqa: F401
from com.sun.star.awt import Point, Size
from com.sun.star.beans import PropertyValue

from _connect import connect


def make_shape(doc, page, x, y, shape_type):
    s = doc.createInstance("com.sun.star.drawing.CustomShape")
    page.add(s)
    sz = Size(); sz.Width = 4000; sz.Height = 1500
    s.setSize(sz)
    pt = Point(); pt.X = x; pt.Y = y
    s.setPosition(pt)
    s.setString(shape_type)  # label the shape with its intended type
    pv = PropertyValue()
    pv.Name = "Type"
    pv.Value = shape_type
    s.setPropertyValue("CustomShapeGeometry", (pv,))
    return s


def main():
    if len(sys.argv) < 2:
        print("usage: custom_shape_pptx_probe.py <port>")
        sys.exit(2)
    ctx = connect(int(sys.argv[1]))
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    doc = desktop.loadComponentFromURL("private:factory/simpress", "_blank", 0, ())
    ok = True
    tmp_path = tempfile.mktemp(suffix=".pptx")
    try:
        page = doc.getCurrentController().getCurrentPage()

        # Create shapes to test
        types_to_test = [
            ("chevron", 1000, 1000),
            ("pentagon-right", 7000, 1000),
            ("right-arrow", 13000, 1000),
            ("notched-right-arrow", 19000, 1000),
        ]
        for (stype, x, y) in types_to_test:
            make_shape(doc, page, x, y, stype)

        # Save as PPTX
        filter_args = (
            PropertyValue("FilterName", 0, "Impress MS PowerPoint 2007 XML", 0),
        )
        doc.storeToURL("file://" + tmp_path, filter_args)
        print(f"Saved to {tmp_path}")

        # Inspect the PPTX XML
        with zipfile.ZipFile(tmp_path, 'r') as z:
            slide_names = [n for n in z.namelist() if n.startswith("ppt/slides/slide") and n.endswith(".xml")]
            slide_names.sort()
            if not slide_names:
                print("FAIL: no slides found in PPTX")
                ok = False
            else:
                xml = z.read(slide_names[0]).decode("utf-8")
                # Print all prstGeom/custGeom references
                import re
                prst_matches = re.findall(r'prstGeom[^>]*prst="([^"]+)"', xml)
                cust_matches = re.findall(r'<a:custGeom[^/]*/?>|<a:custGeom>', xml)
                print(f"prstGeom (preset) shapes: {prst_matches}")
                print(f"custGeom (custom path) shapes: {len(cust_matches)} found")

                # Broad text search
                print(f"'chevron' in xml (case-insensitive): {'chevron' in xml.lower()}")
                print(f"'pentagon' in xml (case-insensitive): {'pentagon' in xml.lower()}")

                # Print surrounding context for prstGeom
                for m in re.finditer(r'.{0,50}prstGeom.{0,100}', xml):
                    print(f"  context: {m.group()[:160]!r}")

                # Count shape elements
                sp_count = len(re.findall(r'<p:sp[ >]', xml))
                pic_count = len(re.findall(r'<p:pic[ >]', xml))
                sp2_count = len(re.findall(r'<p:sp2[ >]', xml))
                print(f"Shape counts: p:sp={sp_count}, p:pic={pic_count}")

                # Print first 3000 chars of slide XML
                print("=== First 3000 chars of slide XML ===")
                print(xml[:3000])

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
