#!/usr/bin/env python3
"""Verify that the Java renderer now uses CustomShape (not PolyPolygonShape) for chevrons.
Simulates what SlideRenderer.applyChevronGeometry does: creates CustomShape,
sets size+position, then calls setPropertyValue("CustomShapeGeometry", PropertyValue[]).

Run:
    uno-tests/run.sh --install target/SmartArt.oxt uno-tests/probes/chevron_customshape_probe.py

Exit 0 = CustomShape chevrons with custGeom confirmed.
"""

import sys, os, zipfile, re, tempfile
import uno
from com.sun.star.awt import Point, Size
from com.sun.star.beans import PropertyValue
from _connect import connect


def pt(x,y): p=Point(); p.X=x; p.Y=y; return p
def sz(w,h): s=Size(); s.Width=w; s.Height=h; return s


def make_custom_shape(doc, page, x, y, w, h, type_name, label):
    """Mirrors SlideRenderer.applyChevronGeometry exactly."""
    s = doc.createInstance("com.sun.star.drawing.CustomShape")
    page.add(s)
    s.setSize(sz(w, h))
    s.setPosition(pt(x, y))
    # Java passes new PropertyValue[]{typeVal} — statically typed, so bridge
    # tags the Any as sequence<PropertyValue>. Replicate with attribute assignment.
    pv = PropertyValue(); pv.Name = "Type"; pv.Value = type_name
    s.CustomShapeGeometry = (pv,)  # proven-working path; matches Java's static-type path
    try: s.setString(label)
    except: pass
    return s


def main():
    if len(sys.argv) < 2:
        print("usage: chevron_customshape_probe.py <port>"); sys.exit(2)
    ctx  = connect(int(sys.argv[1]))
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    doc  = desktop.loadComponentFromURL("private:factory/simpress", "_blank", 0, ())
    page = doc.getCurrentController().getCurrentPage()
    pptx = tempfile.mktemp(suffix=".pptx")
    ok   = True
    try:
        W, H = 4500, 1600; GAP = 800; Y = 2000

        # Mirror SequentialChevronLayout: PENTAGON first, then CHEVRONs
        pentagon = make_custom_shape(doc, page, 1000,           Y, W, H, "pentagon-right", "Alpha")
        chevron1 = make_custom_shape(doc, page, 1000+W+GAP,     Y, W, H, "chevron",        "Delta")
        chevron2 = make_custom_shape(doc, page, 1000+2*(W+GAP), Y, W, H, "chevron",        "Golf")

        doc.storeToURL("file://" + pptx, (
            PropertyValue("FilterName",0,"Impress MS PowerPoint 2007 XML",0),))

        with zipfile.ZipFile(pptx) as z:
            slides = sorted(n for n in z.namelist()
                            if re.match(r'ppt/slides/slide\d+\.xml', n))
            xml = z.read(slides[0]).decode()
            sps = re.findall(r'<p:sp>.*?</p:sp>', xml, re.DOTALL)

        print(f"Total shape elements in PPTX: {len(sps)}")
        custom_geoms = 0
        for i, block in enumerate(sps[2:], start=2):  # skip placeholders
            has_cust  = '<a:custGeom' in block
            pts_count = len(re.findall(r'<a:pt ', block))
            prst_m    = re.search(r'prst="([^"]+)"', block)
            text_m    = re.search(r'<a:t>([^<]+)</a:t>', block)
            label     = text_m.group(1) if text_m else "?"
            if has_cust:
                custom_geoms += 1
                print(f"  [{i}] '{label}': custGeom ({pts_count} pts) ✓")
            elif prst_m:
                print(f"  [{i}] '{label}': prstGeom prst={prst_m.group(1)!r}")
            else:
                print(f"  [{i}] '{label}': no geometry ✗")
                ok = False

        if custom_geoms >= 3:
            print(f"\nPASS: {custom_geoms} CustomShape chevron/pentagon shapes rendered")
        else:
            print(f"\nFAIL: only {custom_geoms}/3 shapes have custGeom")
            ok = False

        # Visual check
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

    sys.exit(0 if ok else 1)

main()
