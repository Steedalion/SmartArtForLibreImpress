#!/usr/bin/env python3
"""Test: does wrapping CustomShapeGeometry in uno.Any with explicit type work?
This replicates what Java's statically-typed setPropertyValue call will do.

Run:
    uno-tests/run.sh uno-tests/probes/chevron_any_probe.py
Exit 0 = explicit uno.Any approach produces custGeom (not rect).
Exit 1 = failed.
"""

import sys, os, zipfile, re, tempfile
import uno
from com.sun.star.awt import Point, Size
from com.sun.star.beans import PropertyValue
from _connect import connect


def pt(x,y): p=Point(); p.X=x; p.Y=y; return p
def sz(w,h): s=Size(); s.Width=w; s.Height=h; return s
BLUE = 0x4472C4

def solid(s, color=BLUE):
    s.setPropertyValue("FillStyle", uno.Enum("com.sun.star.drawing.FillStyle","SOLID"))
    s.setPropertyValue("FillColor", color)
    s.setPropertyValue("LineStyle", uno.Enum("com.sun.star.drawing.LineStyle","NONE"))

def geom_shape(doc, page, x, y, w, h, type_name, approach):
    s = doc.createInstance("com.sun.star.drawing.CustomShape")
    page.add(s)
    s.setSize(sz(w, h)); s.setPosition(pt(x, y))
    pv = PropertyValue(); pv.Name = "Type"; pv.Value = type_name

    if approach == "attr":
        s.CustomShapeGeometry = (pv,)
    elif approach == "setpv":
        s.setPropertyValue("CustomShapeGeometry", (pv,))
    elif approach == "any_seq":
        # Explicit uno.Any typed as sequence<PropertyValue> — what Java does.
        # Must use uno.invoke when passing uno.Any explicitly.
        typed = uno.Any("[]com.sun.star.beans.PropertyValue", (pv,))
        uno.invoke(s, "setPropertyValue", ("CustomShapeGeometry", typed))
    elif approach == "xpropaccess":
        # XPropertyAccess.setPropertyValues path
        outer = PropertyValue()
        outer.Name  = "CustomShapeGeometry"
        outer.Value = uno.Any("[]com.sun.star.beans.PropertyValue", (pv,))
        pa = s.queryInterface(uno.getTypeByName("com.sun.star.beans.XPropertyAccess"))
        if pa:
            uno.invoke(pa, "setPropertyValues", (
                uno.Any("[]com.sun.star.beans.PropertyValue", (outer,)),))
        else:
            print(f"  [{approach}] no XPropertyAccess")
            return s

    try: s.setString(type_name + "\n" + approach)
    except: pass
    solid(s)
    return s


def shape_geom_in_pptx(pptx_path, idx):
    with zipfile.ZipFile(pptx_path) as z:
        slides = sorted(n for n in z.namelist()
                        if re.match(r'ppt/slides/slide\d+\.xml', n))
        xml = z.read(slides[0]).decode()
        sps = re.findall(r'<p:sp>.*?</p:sp>', xml, re.DOTALL)
        if idx >= len(sps): return "missing"
        block = sps[idx]
        if '<a:prstGeom' in block:
            m = re.search(r'prst="([^"]+)"', block)
            return f"prst={m.group(1)}" if m else "prstGeom"
        if '<a:custGeom' in block:
            pts = len(re.findall(r'<a:pt ', block))
            return f"custGeom({pts}pts)"
        return "no-geom"


def main():
    if len(sys.argv) < 2:
        print("usage: chevron_any_probe.py <port>"); sys.exit(2)
    ctx  = connect(int(sys.argv[1]))
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    doc  = desktop.loadComponentFromURL("private:factory/simpress", "_blank", 0, ())
    page = doc.getCurrentController().getCurrentPage()
    pptx = tempfile.mktemp(suffix=".pptx")
    ok   = False
    try:
        W, H = 4000, 1500
        approaches = [
            ("attr",        1000,  1000),  # confirmed working
            ("setpv",       6000,  1000),  # confirmed broken
            ("any_seq",    11000,  1000),  # Java equivalent — key test
            ("xpropaccess",16000,  1000),  # alternative path
        ]
        shapes = []
        for (approach, x, y) in approaches:
            shapes.append(geom_shape(doc, page, x, y, W, H, "chevron", approach))

        doc.storeToURL("file://" + pptx, (
            PropertyValue("FilterName",0,"Impress MS PowerPoint 2007 XML",0),))

        n_placeholders = 2
        print("Approach results (prstGeom != rect = success):")
        for i, (approach, _, _) in enumerate(approaches):
            result = shape_geom_in_pptx(pptx, n_placeholders + i)
            is_ok  = "rect" not in result and "no-geom" not in result
            marker = "PASS" if is_ok else "FAIL"
            print(f"  [{marker}] {approach:15s} -> {result}")
            if approach == "any_seq" and is_ok:
                ok = True

        # Also export PNG
        png = tempfile.mktemp(suffix=".png")
        doc.storeToURL("file://" + png, (
            PropertyValue("FilterName",0,"impress_png_Export",0),
            PropertyValue("PageRange",0,"1",0),))
        print(f"PNG: {png}")

    finally:
        try: doc.close(True)
        except: pass
        try: os.unlink(pptx)
        except: pass

    if ok:
        print("\nSUCCESS: uno.Any explicit type works — Java will work too")
    else:
        print("\nFAIL: uno.Any did not help — need different Java approach")
    sys.exit(0 if ok else 1)

main()
