#!/usr/bin/env python3
"""Draw all SmartArt diagram types and export each as a PNG screenshot.

Called by scripts/make-screenshots.sh:
    python3 uno-tests/probes/screenshot_probe.py <port> <out-dir>

Outputs:
    <out-dir>/cycle.png
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


def font_size(s, pts):
    try:
        s.setPropertyValue("CharHeight", float(pts))
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
    # Centre text vertically (shape property) and horizontally (paragraph property),
    # mirroring SlideRenderer.centerChevronText.
    try:
        s.setPropertyValue("TextVerticalAdjust",
            uno.Enum("com.sun.star.drawing.TextVerticalAdjust", "CENTER"))
        cursor = s.createTextCursor()
        cursor.gotoStart(False)
        cursor.gotoEnd(True)
        cursor.setPropertyValue("ParaAdjust",
            uno.Enum("com.sun.star.style.ParagraphAdjust", "CENTER"))
    except Exception:
        pass
    return s


def add_connector(doc, page, start_shape, end_shape, straight=False, arrow_end=False):
    c = doc.createInstance("com.sun.star.drawing.ConnectorShape")
    page.add(c)
    c.setPropertyValue("StartShape", start_shape)
    c.setPropertyValue("EndShape", end_shape)
    c.setPropertyValue("StartGluePointIndex", -1)
    c.setPropertyValue("EndGluePointIndex", -1)
    c.setPropertyValue("LineColor", GREY)
    if straight:
        c.setPropertyValue("EdgeKind",
            uno.Enum("com.sun.star.drawing.ConnectorType", "LINE"))
    if arrow_end:
        c.setPropertyValue("LineEndName", "Arrow")
        c.setPropertyValue("LineEndWidth", 300)
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
    """3 steps: pentagon → chevron → chevron; sub-boxes horizontally below first two;
    first sub-item of Alpha (Bravo) has a level-3 child (Hotel)."""
    W, H = 4500, 1600; GAP = 800; MARGIN = 1200
    sub_w, sub_h = W - 400, 900
    SUB_GAP = 400           # horizontal gap between sub-items
    CHEVRON_TO_SUB = 700    # vertical gap from chevron to sub-items
    CHILD_V_GAP = 500       # vertical gap from sub-item to its level-3 children
    child_w, child_h = sub_w - 400, 700

    steps = [
        (MARGIN,               1000, "Alpha", "pentagon-right",
         [("Bravo", ["Hotel"]), ("Charlie", [])], BLUE),
        (MARGIN + W + GAP,     1000, "Delta", "chevron",
         [("Echo", []), ("Foxtrot", [])],          BLUE2),
        (MARGIN + 2*(W + GAP), 1000, "Golf",  "chevron",
         [],                                        BLUE3),
    ]
    for x, y, label, kind, subs, color in steps:
        add_chevron_shape(doc, page, x, y, W, H, kind, label, color)
        if not subs:
            continue
        cx = x + W // 2
        total_sub_w = len(subs) * sub_w + (len(subs) - 1) * SUB_GAP
        sub_start_x = cx - total_sub_w // 2
        sub_y = y + H + CHEVRON_TO_SUB
        for j, (sub_label, grandchildren) in enumerate(subs):
            sx = sub_start_x + j * (sub_w + SUB_GAP)
            add_rect(doc, page, sx, sub_y, sub_w, sub_h, sub_label, GREEN)
            sub_cx = sx + sub_w // 2
            gy = sub_y + sub_h + CHILD_V_GAP
            for gc_label in grandchildren:
                add_rect(doc, page, sub_cx - child_w // 2, gy, child_w, child_h,
                         gc_label, BLUE2)
                gy += child_h + CHILD_V_GAP


def draw_hub_and_spoke(doc, page):
    """Hub circle in the centre; 5 spoke circles; first spoke has one child."""
    HUB_D    = 2200   # hub diameter
    SPOKE_D  = 1500   # spoke diameter
    CHILD_D  = 1200   # level-3 child diameter
    CHILD_GAP = 600   # gap between spoke edge and child edge
    CX, CY   = 12700, 9000
    RADIUS   = 5000

    hub = add_ellipse(doc, page, CX - HUB_D//2, CY - HUB_D//2, HUB_D, HUB_D,
                      "Alpha", BLUE)
    font_size(hub, 14)
    spokes_data = [
        ("Bravo",   ["Golf"]),   # first spoke has one child
        ("Charlie", []),
        ("Delta",   []),
        ("Echo",    []),
        ("Foxtrot", []),
    ]
    for i, (label, children) in enumerate(spokes_data):
        angle = math.radians(-90 + i * 360 / len(spokes_data))
        scx = int(CX + RADIUS * math.cos(angle))
        scy = int(CY + RADIUS * math.sin(angle))
        spoke = add_ellipse(doc, page, scx - SPOKE_D//2, scy - SPOKE_D//2,
                            SPOKE_D, SPOKE_D, label, BLUE2)
        font_size(spoke, 11)
        add_connector(doc, page, hub, spoke, straight=True)

        # Level-3 children radially outward from this spoke
        dist = SPOKE_D // 2 + CHILD_GAP + CHILD_D // 2
        for child_label in children:
            ccx = scx + int(dist * math.cos(angle))
            ccy = scy + int(dist * math.sin(angle))
            child = add_ellipse(doc, page, ccx - CHILD_D//2, ccy - CHILD_D//2,
                                CHILD_D, CHILD_D, child_label, BLUE3)
            font_size(child, 9)
            add_connector(doc, page, spoke, child, straight=True)
            dist += CHILD_D + CHILD_GAP


def draw_process_flow(doc, page):
    """4 steps in a horizontal row with arrow connectors; first two have sub-items."""
    W, H = 4000, 1500; GAP = 1500; Y1 = 1000
    W2, H2 = 3970, 1470; V_GAP = 800
    steps = [
        ("Alpha",  BLUE,  ["Bravo", "Charlie"]),
        ("Delta",  BLUE2, ["Echo"]),
        ("Golf",   BLUE3, []),
        ("Juliet", BLUE,  []),
    ]
    total_w = len(steps) * W + (len(steps) - 1) * GAP
    start_x = (25400 - total_w) // 2

    nodes = []
    for i, (label, color, _) in enumerate(steps):
        x = start_x + i * (W + GAP)
        s = add_rect(doc, page, x, Y1, W, H, label, color)
        font_size(s, 14)
        nodes.append(s)

    for i in range(len(nodes) - 1):
        c = add_connector(doc, page, nodes[i], nodes[i + 1], arrow_end=True)
        c.setPropertyValue("StartGluePointIndex", 1)   # right
        c.setPropertyValue("EndGluePointIndex",   3)   # left

    # Sub-items stacked below each step
    for i, (_, _, subs) in enumerate(steps):
        x = start_x + i * (W + GAP)
        cx = x + W // 2
        child_y = Y1 + H + V_GAP
        for sub in subs:
            cs = add_rect(doc, page, cx - W2 // 2, child_y, W2, H2, sub, GREEN)
            font_size(cs, 11)
            c = add_connector(doc, page, nodes[i], cs)
            c.setPropertyValue("StartGluePointIndex", 2)   # bottom
            c.setPropertyValue("EndGluePointIndex",   0)   # top
            child_y += H2 + V_GAP


def draw_hierarchy(doc, page):
    """Root → 2 children → 2 grandchildren each."""
    RW, RH = 3800, 1300; CW, CH = 3200, 1200; GW, GH = 2800, 1000
    GAP_X = 1000; GAP_Y = 1800
    CX = 25400 // 2

    root = add_rect(doc, page, CX - RW//2, 1200, RW, RH, "Alpha", BLUE)
    font_size(root, 14)

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
        font_size(cs, 12)
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
            font_size(gs, 10)
            add_connector(doc, page, cs, gs)


def draw_pyramid(doc, page):
    """4 tiers: Strategy (apex) → Goals → Tactics → Actions (base).
    Goals tier has two children (Goal A, Goal B) to verify child placement."""
    TIER_H    = 1600
    GAP       = 150
    MAX_W     = 18000
    MIN_W     = 3000
    TOP_Y     = 2000
    SLIDE_W   = 25400
    CHILD_W   = 3000
    CHILD_H   = 900
    CHILD_GAP = 300
    CHILD_V_GAP = 150

    tiers = [
        ("Strategy", BLUE,  []),
        ("Goals",    BLUE2, ["Goal A", "Goal B"]),
        ("Tactics",  BLUE3, []),
        ("Actions",  BLUE,  []),
    ]
    n = len(tiers)
    tier_shapes = []
    tier_ws     = []
    tier_ys     = []
    for i, (label, color, _) in enumerate(tiers):
        w = MIN_W + (MAX_W - MIN_W) * i // (n - 1)
        x = (SLIDE_W - w) // 2
        y = TOP_Y + i * (TIER_H + GAP)
        s = add_rect(doc, page, x, y, w, TIER_H, label, color)
        font_size(s, 14 if i == 0 else 11)
        tier_shapes.append(s)
        tier_ws.append(w)
        tier_ys.append(y)

    for i, (_, _, children) in enumerate(tiers):
        if not children:
            continue
        cx = (SLIDE_W + tier_ws[i]) // 2 + CHILD_GAP
        cy = tier_ys[i]
        for child_label in children:
            cs = add_rect(doc, page, cx, cy, CHILD_W, CHILD_H, child_label, GREEN)
            font_size(cs, 9)
            cy += CHILD_H + CHILD_V_GAP


def draw_cycle(doc, page):
    """5 nodes in a clockwise ring joined by directed straight arrows."""
    NODE_W, NODE_H = 3500, 1400
    RING_R = 6000
    CX, CY = 25400 // 2, 19050 // 2
    labels = ["Alpha", "Bravo", "Charlie", "Delta", "Echo"]
    colors = [BLUE, BLUE2, BLUE3, BLUE, BLUE2]
    nodes = []
    for i, (label, color) in enumerate(zip(labels, colors)):
        angle = math.radians(-90 + i * 360 / len(labels))
        nx = CX + int(RING_R * math.cos(angle))
        ny = CY + int(RING_R * math.sin(angle))
        s = add_rect(doc, page, nx - NODE_W // 2, ny - NODE_H // 2,
                     NODE_W, NODE_H, label, color)
        font_size(s, 14)
        nodes.append(s)
    # Directed arrows: i → i+1, last → 0
    for i in range(len(nodes)):
        c = add_connector(doc, page, nodes[i], nodes[(i + 1) % len(nodes)],
                          straight=True, arrow_end=True)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

DIAGRAMS = [
    ("pyramid",            draw_pyramid),
    ("cycle",              draw_cycle),
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
