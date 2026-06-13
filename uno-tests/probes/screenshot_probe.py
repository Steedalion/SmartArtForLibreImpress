#!/usr/bin/env python3
"""Draw all SmartArt diagram types and export each as a PNG screenshot.

Called by scripts/make-screenshots.sh:
    python3 uno-tests/probes/screenshot_probe.py <port> <out-dir>

Outputs:
    <out-dir>/sequential-chevron.png
    <out-dir>/hub-and-spoke.png
    <out-dir>/process-flow.png
    <out-dir>/hierarchy.png
"""

import sys
import os
import math
import time

import uno  # noqa: F401
from com.sun.star.awt import Point, Size
from com.sun.star.beans import PropertyValue

# ---------------------------------------------------------------------------
# UNO helpers
# ---------------------------------------------------------------------------

def connect(port, tries=60):
    local = uno.getComponentContext()
    resolver = local.ServiceManager.createInstanceWithContext(
        "com.sun.star.bridge.UnoUrlResolver", local)
    last = None
    for _ in range(tries):
        try:
            return resolver.resolve(
                "uno:socket,host=localhost,port=%d;urp;StarOffice.ComponentContext" % port)
        except Exception as e:
            last = e
            time.sleep(0.5)
    print("could not connect to port %d: %s" % (port, last))
    sys.exit(2)


def pt(x, y):
    p = Point(); p.X = x; p.Y = y; return p


def sz(w, h):
    s = Size(); s.Width = w; s.Height = h; return s


BLUE   = 0x4472C4
BLUE2  = 0x5B9BD5
BLUE3  = 0x2E75B6
GREEN  = 0x70AD47
ORANGE = 0xED7D31
GREY   = 0x808080


def solid_fill(s, color):
    s.setPropertyValue("FillStyle", uno.Enum("com.sun.star.drawing.FillStyle", "SOLID"))
    s.setPropertyValue("FillColor", color)
    s.setPropertyValue("LineStyle", uno.Enum("com.sun.star.drawing.LineStyle", "NONE"))


def text_color(s, color=0xFFFFFF):
    try:
        s.setPropertyValue("CharColor", color)
    except Exception:
        pass


def add_rect(doc, page, x, y, w, h, label, color=GREEN):
    s = doc.createInstance("com.sun.star.drawing.RectangleShape")
    page.add(s)
    s.setSize(sz(w, h))
    s.setPosition(pt(x, y))
    try:
        s.setString(label)
    except Exception:
        pass
    solid_fill(s, color)
    text_color(s, 0xFFFFFF)
    return s


def add_ellipse(doc, page, x, y, w, h, label, color=BLUE):
    s = doc.createInstance("com.sun.star.drawing.EllipseShape")
    page.add(s)
    s.setSize(sz(w, h))
    s.setPosition(pt(x, y))
    try:
        s.setString(label)
    except Exception:
        pass
    solid_fill(s, color)
    text_color(s, 0xFFFFFF)
    return s


def add_chevron_shape(doc, page, x, y, w, h, kind, label, color=BLUE):
    """Create a LibreOffice built-in chevron/pentagon CustomShape.
    kind: 'pentagon-right' (first step) or 'chevron' (subsequent steps).
    Attribute assignment carries the correct UNO type tag for
    CustomShapeGeometry — same mechanism as Java's statically-typed array.
    Text colour is left as default (black) so it is visible even in the
    transparent notch area of a chevron.
    """
    from com.sun.star.beans import PropertyValue as PV
    s = doc.createInstance("com.sun.star.drawing.CustomShape")
    page.add(s)
    s.setSize(sz(w, h))
    s.setPosition(pt(x, y))
    pv = PV(); pv.Name = "Type"; pv.Value = kind
    s.CustomShapeGeometry = (pv,)
    try:
        s.setString(label)
    except Exception:
        pass
    solid_fill(s, color)
    # No text_color override: let LibreOffice auto-colour so text is readable
    # in both the filled body and the transparent notch area.
    return s


def add_connector(doc, page, start_shape, end_shape):
    c = doc.createInstance("com.sun.star.drawing.ConnectorShape")
    page.add(c)
    c.setPropertyValue("StartShape", start_shape)
    c.setPropertyValue("EndShape", end_shape)
    c.setPropertyValue("StartGluePointIndex", -1)
    c.setPropertyValue("EndGluePointIndex", -1)
    c.setPropertyValue("LineColor", GREY)
    return c


def export_png(doc, path):
    filter_args = (
        PropertyValue("FilterName", 0, "impress_png_Export", 0),
        PropertyValue("PageRange", 0, "1", 0),
    )
    doc.storeToURL("file://" + path, filter_args)


# ---------------------------------------------------------------------------
# Diagram drawings
# ---------------------------------------------------------------------------

def draw_sequential_chevron(doc, page):
    """3 steps: pentagon → chevron → chevron, with sub-boxes below first two."""
    W, H = 4500, 1600; GAP = 800; MARGIN = 1200
    sub_w, sub_h = W - 400, 900

    steps = [
        (MARGIN,               1000, "Alpha", "pentagon-right", ["Bravo", "Charlie"], BLUE),
        (MARGIN + W + GAP,     1000, "Delta", "chevron",        ["Echo", "Foxtrot"],  BLUE2),
        (MARGIN + 2*(W + GAP), 1000, "Golf",  "chevron",        [],                   BLUE3),
    ]
    for x, y, label, kind, subs, color in steps:
        add_chevron_shape(doc, page, x, y, W, H, kind, label, color)
        sy = y + H + 700
        for sl in subs:
            add_rect(doc, page, x + 200, sy, sub_w, sub_h, sl, GREEN)
            sy += sub_h + 300


def draw_hub_and_spoke(doc, page):
    """Hub ellipse in the centre; 5 spoke ellipses around it."""
    HUB_W, HUB_H = 3000, 1800
    SPOKE_W, SPOKE_H = 2000, 1200
    CX, CY = 12700, 9000   # centre of slide (254 mm × 190 mm → 25400 × 19050)
    RADIUS = 5000

    hub = add_ellipse(doc, page, CX - HUB_W//2, CY - HUB_H//2, HUB_W, HUB_H,
                      "Alpha", BLUE)
    spokes_labels = ["Bravo", "Charlie", "Delta", "Echo", "Foxtrot"]
    for i, label in enumerate(spokes_labels):
        angle = math.radians(-90 + i * 360 / len(spokes_labels))
        sx = int(CX + RADIUS * math.cos(angle)) - SPOKE_W // 2
        sy = int(CY + RADIUS * math.sin(angle)) - SPOKE_H // 2
        spoke = add_ellipse(doc, page, sx, sy, SPOKE_W, SPOKE_H, label, BLUE2)
        add_connector(doc, page, hub, spoke)


def draw_process_flow(doc, page):
    """4 nodes in a horizontal row joined by connectors."""
    W, H = 4000, 1800; GAP = 1200; Y = 8000
    labels = ["Alpha", "Delta", "Golf", "Juliet"]
    total_w = len(labels) * W + (len(labels) - 1) * GAP
    start_x = (25400 - total_w) // 2
    colors = [BLUE, BLUE2, BLUE3, BLUE]

    nodes = []
    for i, (label, color) in enumerate(zip(labels, colors)):
        x = start_x + i * (W + GAP)
        nodes.append(add_rect(doc, page, x, Y, W, H, label, color))

    for i in range(len(nodes) - 1):
        c = add_connector(doc, page, nodes[i], nodes[i + 1])
        c.setPropertyValue("StartGluePointIndex", 1)   # right
        c.setPropertyValue("EndGluePointIndex",   3)   # left


def draw_hierarchy(doc, page):
    """Root → 2 children → 2 grandchildren each."""
    RW, RH = 3800, 1300; CW, CH = 3200, 1200; GW, GH = 2800, 1000
    GAP_X = 1000; GAP_Y = 1800
    CX = 25400 // 2

    root = add_rect(doc, page, CX - RW//2, 1200, RW, RH, "Alpha", BLUE)

    children = [("Delta", BLUE2), ("Golf", BLUE2)]
    # Space children wide enough to hold two grandchildren each
    child_span = 2 * GW + GAP_X          # span of one child's grandchildren
    child_gap  = GAP_X * 2               # gap between the two child groups
    child_total = 2 * child_span + child_gap
    child_y = 1200 + RH + GAP_Y
    child_shapes = []
    for i, (label, color) in enumerate(children):
        # Centre each child over its two grandchildren
        group_start = CX - child_total // 2 + i * (child_span + child_gap)
        cx = group_start + child_span // 2 - CW // 2
        cs = add_rect(doc, page, cx, child_y, CW, CH, label, color)
        child_shapes.append((group_start, child_span, cs))
        add_connector(doc, page, root, cs)

    grandchild_labels = [["Bravo", "Charlie"], ["Echo", "Foxtrot"]]
    grand_y = child_y + CH + GAP_Y
    for (group_start, span, cs), glabels in zip(child_shapes, grandchild_labels):
        g_total = len(glabels) * GW + (len(glabels) - 1) * GAP_X
        g_start = group_start + span // 2 - g_total // 2
        for j, gl in enumerate(glabels):
            gx = g_start + j * (GW + GAP_X)
            gs = add_rect(doc, page, gx, grand_y, GW, GH, gl, ORANGE)
            add_connector(doc, page, cs, gs)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

DIAGRAMS = [
    ("sequential-chevron", draw_sequential_chevron),
    ("hub-and-spoke",      draw_hub_and_spoke),
    ("process-flow",       draw_process_flow),
    ("hierarchy",          draw_hierarchy),
]


def main():
    if len(sys.argv) < 3:
        print("usage: screenshot_probe.py <port> <out-dir>")
        sys.exit(2)
    port    = int(sys.argv[1])
    out_dir = sys.argv[2]
    os.makedirs(out_dir, exist_ok=True)

    ctx  = connect(port)
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)

    for name, draw_fn in DIAGRAMS:
        doc  = desktop.loadComponentFromURL("private:factory/simpress", "_blank", 0, ())
        page = doc.getCurrentController().getCurrentPage()
        draw_fn(doc, page)
        out_path = os.path.join(out_dir, name + ".png")
        export_png(doc, out_path)
        doc.close(True)
        print(f"  wrote {out_path}")


main()
