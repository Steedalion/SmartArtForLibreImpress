#!/usr/bin/env python3
"""Visual check: does CustomShape + Type='chevron' (attribute assignment) render a chevron?
Also compare with PolyPolygonShape chevron side-by-side.

Run:
    uno-tests/run.sh uno-tests/probes/chevron_visual_probe.py
"""

import sys, os, tempfile, time
import uno
from com.sun.star.awt import Point, Size
from com.sun.star.beans import PropertyValue
from _connect import connect

def pt(x,y): p=Point(); p.X=x; p.Y=y; return p
def sz(w,h): s=Size(); s.Width=w; s.Height=h; return s

BLUE  = 0x4472C4
BLUE2 = 0x5B9BD5

def solid(s, color):
    s.setPropertyValue("FillStyle", uno.Enum("com.sun.star.drawing.FillStyle","SOLID"))
    s.setPropertyValue("FillColor", color)
    s.setPropertyValue("LineStyle", uno.Enum("com.sun.star.drawing.LineStyle","NONE"))

def add_label(doc, page, x, y, text):
    s = doc.createInstance("com.sun.star.drawing.TextShape")
    page.add(s)
    s.setSize(sz(4000, 600)); s.setPosition(pt(x, y))
    s.setString(text)
    s.setPropertyValue("FillStyle", uno.Enum("com.sun.star.drawing.FillStyle","NONE"))
    s.setPropertyValue("LineStyle", uno.Enum("com.sun.star.drawing.LineStyle","NONE"))

def main():
    if len(sys.argv) < 2:
        print("usage: chevron_visual_probe.py <port>"); sys.exit(2)
    ctx = connect(int(sys.argv[1]))
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    doc = desktop.loadComponentFromURL("private:factory/simpress", "_blank", 0, ())
    page = doc.getCurrentController().getCurrentPage()
    W, H = 4500, 1800

    # Row 1: CustomShape + Type='chevron' via attribute assignment (approach C)
    add_label(doc, page, 500, 500, "CustomShape  Type='chevron'  (attribute assignment)")
    for i, (stype, x) in enumerate([
        ("chevron",       1000),
        ("pentagon-right",6500),
        ("right-arrow",  12000),
        ("notched-right-arrow", 17500),
    ]):
        s = doc.createInstance("com.sun.star.drawing.CustomShape")
        page.add(s)
        s.setSize(sz(W, H)); s.setPosition(pt(x, 1200))
        pv = PropertyValue(); pv.Name="Type"; pv.Value=stype
        s.CustomShapeGeometry = (pv,)   # attribute assignment — the working path
        try: s.setString(stype)
        except: pass
        solid(s, [BLUE, BLUE2, 0x2E75B6, 0x70AD47][i])

    # Row 2: PolyPolygonShape hand-drawn (current implementation)
    add_label(doc, page, 500, 3600, "PolyPolygonShape  (current hand-drawn polygon)")
    for i, (pts_fn, label, x) in enumerate([
        # Pentagon
        (lambda x,y,w,h: [pt(x,y),pt(x+w-w//4,y),pt(x+w,y+h//2),pt(x+w-w//4,y+h),pt(x,y+h)],
         "pentagon", 1000),
        # Chevron
        (lambda x,y,w,h: [pt(x+w//6,y),pt(x+w-w//4,y),pt(x+w,y+h//2),
                           pt(x+w-w//4,y+h),pt(x+w//6,y+h),pt(x,y+h//2)],
         "chevron", 6500),
    ]):
        s = doc.createInstance("com.sun.star.drawing.PolyPolygonShape")
        page.add(s)
        s.PolyPolygon = (tuple(pts_fn(x, 4300, W, H)),)
        try: s.setString(label)
        except: pass
        solid(s, [BLUE, BLUE2][i])

    # Export as PNG
    png = tempfile.mktemp(suffix=".png")
    doc.storeToURL("file://"+png, (
        PropertyValue("FilterName",0,"impress_png_Export",0),
        PropertyValue("PageRange",0,"1",0),))
    print(f"PNG: {png}")
    doc.close(True)

main()
