#!/usr/bin/env python3
"""Try every known approach to getting a real LibreOffice CustomShape chevron.

Exit 0 = at least one approach produced a non-rect prstGeom in the PPTX.
Exit 1 = none worked.
Exit 2 = could not connect.

Run:
    uno-tests/run.sh uno-tests/probes/find_chevron_probe.py
"""

import sys, os, zipfile, re, tempfile, time
import uno
from com.sun.star.awt import Point, Size
from com.sun.star.beans import PropertyValue
from _connect import connect


def pt(x, y):  p = Point(); p.X = x; p.Y = y; return p
def sz(w, h):  s = Size();  s.Width = w; s.Height = h; return s


def export_pptx(doc, path):
    doc.storeToURL("file://" + path, (
        PropertyValue("FilterName", 0, "Impress MS PowerPoint 2007 XML", 0),))


def shapes_prstGeom(pptx_path):
    """Return list of prstGeom prst= values from the first slide."""
    with zipfile.ZipFile(pptx_path) as z:
        slides = sorted(n for n in z.namelist()
                        if re.match(r'ppt/slides/slide\d+\.xml', n))
        if not slides:
            return []
        xml = z.read(slides[0]).decode()
        return re.findall(r'<a:prstGeom[^>]+prst="([^"]+)"', xml)


def try_approach(label, doc, page, fn, pptx_path):
    before = page.Count
    try:
        fn(doc, page)
    except Exception as e:
        print(f"  [{label}] EXCEPTION: {e}")
        return None
    added = [page.getByIndex(i) for i in range(before, page.Count)]
    export_pptx(doc, pptx_path)
    prsts = shapes_prstGeom(pptx_path)
    # Only the newly added shapes matter — check last N prsts
    relevant = prsts[-(len(added)):] if added else []
    result = [p for p in relevant if p != "rect"]
    print(f"  [{label}] added={len(added)}, prstGeom={relevant} -> non-rect={result}")
    return result or None


# ── Approach A: geometry AFTER add+size+pos (what we tried before) ──────────
def approach_a(doc, page):
    s = doc.createInstance("com.sun.star.drawing.CustomShape")
    page.add(s)
    s.setSize(sz(4000, 1500)); s.setPosition(pt(1000, 1000))
    pv = PropertyValue(); pv.Name = "Type"; pv.Value = "chevron"
    s.setPropertyValue("CustomShapeGeometry", (pv,))


# ── Approach B: geometry BEFORE add ─────────────────────────────────────────
def approach_b(doc, page):
    s = doc.createInstance("com.sun.star.drawing.CustomShape")
    pv = PropertyValue(); pv.Name = "Type"; pv.Value = "chevron"
    s.setPropertyValue("CustomShapeGeometry", (pv,))
    page.add(s)
    s.setSize(sz(4000, 1500)); s.setPosition(pt(6000, 1000))


# ── Approach C: attribute assignment (s.CustomShapeGeometry = ...) ──────────
def approach_c(doc, page):
    s = doc.createInstance("com.sun.star.drawing.CustomShape")
    page.add(s)
    s.setSize(sz(4000, 1500)); s.setPosition(pt(11000, 1000))
    pv = PropertyValue(); pv.Name = "Type"; pv.Value = "chevron"
    s.CustomShapeGeometry = (pv,)


# ── Approach D: geometry BEFORE add, attribute style ────────────────────────
def approach_d(doc, page):
    s = doc.createInstance("com.sun.star.drawing.CustomShape")
    pv = PropertyValue(); pv.Name = "Type"; pv.Value = "chevron"
    s.CustomShapeGeometry = (pv,)
    page.add(s)
    s.setSize(sz(4000, 1500)); s.setPosition(pt(16000, 1000))


# ── Approach E: pentagon-right type ─────────────────────────────────────────
def approach_e(doc, page):
    s = doc.createInstance("com.sun.star.drawing.CustomShape")
    page.add(s)
    s.setSize(sz(4000, 1500)); s.setPosition(pt(1000, 4000))
    pv = PropertyValue(); pv.Name = "Type"; pv.Value = "pentagon-right"
    s.CustomShapeGeometry = (pv,)


# ── Approach F: use com.sun.star.presentation.CustomPresentationShape ────────
def approach_f(doc, page):
    try:
        s = doc.createInstance("com.sun.star.presentation.CustomPresentationShape")
    except Exception:
        s = doc.createInstance("com.sun.star.drawing.CustomShape")
    page.add(s)
    s.setSize(sz(4000, 1500)); s.setPosition(pt(6000, 4000))
    pv = PropertyValue(); pv.Name = "Type"; pv.Value = "chevron"
    s.CustomShapeGeometry = (pv,)


# ── Approach G: full geometry with EnhancedPath ──────────────────────────────
# LibreOffice chevron from svx EnhancedCustomShapeGeometry.cxx:
#   ViewBox (0,0,21600,21600), path uses adjustment ?f0 for the notch
def approach_g(doc, page):
    s = doc.createInstance("com.sun.star.drawing.CustomShape")
    page.add(s)
    s.setSize(sz(4000, 1500)); s.setPosition(pt(11000, 4000))

    def pv(name, val): v = PropertyValue(); v.Name = name; v.Value = val; return v

    adj = uno.createUnoStruct("com.sun.star.drawing.EnhancedCustomShapeAdjustmentValue")
    adj.Value = 16200  # default notch ~75% across
    adj.State = uno.Enum("com.sun.star.beans.PropertyState", "DIRECT_VALUE")

    geom = (
        pv("Type", "chevron"),
        pv("AdjustmentValues", (adj,)),
        pv("EnhancedPath", "M ?f0 0 L 21600 0 L 21600 21600 L ?f0 21600 L 0 10800 Z N"),
        pv("ViewBox", uno.createUnoStruct("com.sun.star.awt.Rectangle",
                                          0, 0, 21600, 21600)),
    )
    s.CustomShapeGeometry = geom


# ── Approach H: dispatch .uno:ArrowShapes.chevron with DrawingView active ────
def approach_h(ctx, smgr, doc, page):
    frame = doc.getCurrentController().getFrame()
    transformer = smgr.createInstanceWithContext("com.sun.star.util.URLTransformer", ctx)
    url = uno.createUnoStruct("com.sun.star.util.URL")
    url.Complete = ".uno:ArrowShapes.chevron"
    ok, parsed = transformer.parseStrict(url)
    dispatcher = frame.queryDispatch(parsed, "_self", 0)
    if dispatcher is None:
        print("  [H] no dispatcher")
        return
    # Provide explicit insert-rectangle args so LO places the shape
    args = (
        PropertyValue("KeyModifier",     0, 0,     0),
        PropertyValue("StartPosition.X", 0, 16000, 0),
        PropertyValue("StartPosition.Y", 0, 4000,  0),
        PropertyValue("EndPosition.X",   0, 20000, 0),
        PropertyValue("EndPosition.Y",   0, 5500,  0),
    )
    dispatcher.dispatch(parsed, args)
    time.sleep(0.5)


def main():
    if len(sys.argv) < 2:
        print("usage: find_chevron_probe.py <port>")
        sys.exit(2)
    ctx = connect(int(sys.argv[1]))
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    doc = desktop.loadComponentFromURL("private:factory/simpress", "_blank", 0, ())
    page = doc.getCurrentController().getCurrentPage()
    pptx = tempfile.mktemp(suffix=".pptx")
    found = []
    try:
        print("Testing CustomShape type approaches:")
        for label, fn in [
            ("A: set after add+size+pos",      approach_a),
            ("B: set before add",               approach_b),
            ("C: attr after add",               approach_c),
            ("D: attr before add",              approach_d),
            ("E: pentagon-right attr",          approach_e),
            ("F: CustomPresentationShape",      approach_f),
            ("G: full EnhancedPath geometry",   approach_g),
        ]:
            result = try_approach(label, doc, page, fn, pptx)
            if result:
                found.extend(result)

        print("\nTesting dispatch approach H:")
        before = page.Count
        approach_h(ctx, smgr, doc, page)
        export_pptx(doc, pptx)
        prsts = shapes_prstGeom(pptx)
        after_prsts = prsts[-(page.Count - before):]
        print(f"  [H] added={page.Count-before}, prstGeom={after_prsts}")
        found.extend(p for p in after_prsts if p != "rect")

        # Dump shape geometry blocks from final slide XML
        export_pptx(doc, pptx)
        with zipfile.ZipFile(pptx) as z:
            slides = sorted(n for n in z.namelist()
                            if re.match(r'ppt/slides/slide\d+\.xml', n))
            if slides:
                xml = z.read(slides[0]).decode()
                # Extract each <p:sp> block (shape) and print its spPr section
                sp_blocks = re.findall(r'<p:sp>.*?</p:sp>', xml, re.DOTALL)
                print(f"\n=== {len(sp_blocks)} shape blocks in PPTX slide ===")
                for i, block in enumerate(sp_blocks):
                    # Extract spPr content
                    sppr = re.search(r'<p:spPr>(.*?)</p:spPr>', block, re.DOTALL)
                    name_m = re.search(r'name="([^"]*)"', block)
                    name = name_m.group(1) if name_m else f"shape{i}"
                    if sppr:
                        geom_type = "unknown"
                        if '<a:prstGeom' in sppr.group(1):
                            m = re.search(r'prst="([^"]+)"', sppr.group(1))
                            geom_type = f"prstGeom prst={m.group(1)!r}" if m else "prstGeom"
                        elif '<a:custGeom' in sppr.group(1):
                            pts = len(re.findall(r'<a:pt ', sppr.group(1)))
                            geom_type = f"custGeom ({pts} pts)"
                        else:
                            geom_type = "no geom"
                        print(f"  [{i}] {name!r:20s}: {geom_type}")

    finally:
        try: doc.close(True)
        except Exception: pass
        try: os.unlink(pptx)
        except Exception: pass

    if found:
        print(f"\nSUCCESS: found non-rect preset shapes: {found}")
        sys.exit(0)
    else:
        print("\nFAIL: no approach produced a real chevron prstGeom")
        sys.exit(1)

main()
