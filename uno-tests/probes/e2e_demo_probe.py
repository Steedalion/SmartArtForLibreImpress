#!/usr/bin/env python3
"""True end-to-end probe: drives the REAL Java render pipeline.

Run via uno-tests/run.sh with the extension installed:
    uno-tests/run.sh --install target/SmartArt.oxt uno-tests/probes/e2e_demo_probe.py

Dispatches org.libreimpress.smartart:Demo (DemoRunner) with an OutputDir
argument against a loaded Impress document. DemoRunner runs the extension's
actual HierarchyParser -> LayoutFactory -> SlideRenderer for every diagram
type, exports one 1280x960 PNG per type, and writes a demo-result.txt with
one "OK <slug>" line per type.

Unlike render_probe.py (which merely mirrors the renderer's UNO call
sequence from Python), a failure here means the shipped Java code is broken.

Asserts:
  1. demo-result.txt exists with an OK line for every expected diagram type.
  2. One slide per type was appended; each demo slide holds exactly one
     GroupShape (the diagram) plus the two dev-chrome text shapes.
  3. The expected node labels appear in the grouped shapes' text.
  4. Every PNG exists, is a real 1280x960 PNG, and is not trivially blank.

Exit 0 = pass, 1 = a check failed, 2 = could not connect.
"""

import os
import shutil
import sys
import tempfile
import time

import uno
from com.sun.star.beans import PropertyValue

from _connect import connect

DEMO_URL = "org.libreimpress.smartart:Demo"
RESULT_FILE = "demo-result.txt"
PNG_SIZE = (1280, 960)
PNG_MIN_BYTES = 5000
RESULT_TIMEOUT_S = 180

# Mirrors DemoRunner.DEMOS (order matters — slides are appended in this
# order). "texts" are substrings that must appear in the grouped diagram's
# text; "min_shapes" is a conservative lower bound on the group's children.
EXPECTED = [
    ("hierarchy",
     ["Company", "Products", "Alpha", "Bravo", "Services", "Charlie", "Delta"], 7),
    ("hub-and-spoke",
     ["Innovation", "People", "Training", "Process", "Technology", "Partners"], 6),
    ("process-flow",
     ["Research", "Survey", "Analysis", "Design", "Prototype", "Testing", "Launch"], 7),
    ("sequential-chevron",
     ["Plan", "Scope", "Schedule", "Execute", "Build", "Test", "Review"], 7),
    ("cycle", ["Plan", "Do", "Check", "Act"], 4),
    ("cycle-arrows", ["Plan", "Do", "Check", "Act"], 4),
    ("cycle-blocks", ["Plan", "Do", "Check", "Act"], 4),
    ("pyramid",
     ["Vision", "Strategy", "Execution", "Results",
      "Goal A", "Goal B", "Goal C"], 4),
    ("basic-block-list",
     ["Plan", "Scope", "Budget", "Design", "Build", "Backend", "Database",
      "Frontend", "Test", "Release", "Review"], 6),
    ("vertical-bullet-list",
     ["Introductions", "Welcome", "Goals for today", "Project status",
      "Milestones", "Phase 1 shipped", "Risks", "Next steps", "Q and A",
      "Open floor"], 3),
    ("basic-venn", ["Quality", "Speed", "Cost"], 3),
    ("basic-matrix",
     ["Urgent and Important", "Important, Not Urgent",
      "Urgent, Not Important", "Neither"], 4),
]


def pv(name, value):
    p = PropertyValue()
    p.Name = name
    p.Value = value
    return p


def supports(obj, service):
    try:
        return obj.supportsService(service)
    except Exception:  # noqa: BLE001
        return False


def collect_texts(shape, out):
    """Recursively collect the text of a shape (and group children)."""
    try:
        for i in range(shape.Count):
            collect_texts(shape.getByIndex(i), out)
        return
    except Exception:  # noqa: BLE001
        pass  # not a group
    try:
        s = shape.getString()
        if s:
            out.append(s)
    except Exception:  # noqa: BLE001
        pass


def check_result_file(outdir):
    path = os.path.join(outdir, RESULT_FILE)
    deadline = time.time() + RESULT_TIMEOUT_S
    while not os.path.exists(path):
        if time.time() > deadline:
            print("FAIL: %s never appeared (Demo dispatch did not complete?)"
                  % path)
            return False
        time.sleep(1)
    with open(path, encoding="utf-8") as f:
        lines = [ln.strip() for ln in f if ln.strip()]
    ok = True
    slugs = [slug for slug, _, _ in EXPECTED]
    got = {}
    for ln in lines:
        status, _, rest = ln.partition(" ")
        got[rest.split(":")[0].strip()] = (status, ln)
    for slug in slugs:
        if slug not in got:
            print("FAIL: no result line for %r" % slug)
            ok = False
        elif got[slug][0] != "OK":
            print("FAIL: %s" % got[slug][1])
            ok = False
    if ok:
        print("PASS: %s has OK lines for all %d types" % (RESULT_FILE, len(slugs)))
    return ok


def check_slides(doc, first_demo_page):
    pages = doc.getDrawPages()
    expected_count = first_demo_page + len(EXPECTED)
    if pages.getCount() != expected_count:
        print("FAIL: expected %d pages (%d + %d demo slides), got %d"
              % (expected_count, first_demo_page, len(EXPECTED),
                 pages.getCount()))
        return False
    ok = True
    for i, (slug, texts, min_shapes) in enumerate(EXPECTED):
        page = pages.getByIndex(first_demo_page + i)
        groups = [page.getByIndex(j) for j in range(page.Count)
                  if supports(page.getByIndex(j),
                              "com.sun.star.drawing.GroupShape")]
        # diagram group + corner label + input listing
        if page.Count != 3 or len(groups) != 1:
            print("FAIL: %s: expected 3 shapes incl. 1 group, got %d shapes / "
                  "%d groups" % (slug, page.Count, len(groups)))
            ok = False
            continue
        group = groups[0]
        if group.Count < min_shapes:
            print("FAIL: %s: group has %d shapes, expected >= %d"
                  % (slug, group.Count, min_shapes))
            ok = False
            continue
        found = []
        collect_texts(group, found)
        joined = "\n".join(found)
        missing = [t for t in texts if t not in joined]
        if missing:
            print("FAIL: %s: missing node text(s) %s in group text %r"
                  % (slug, missing, joined))
            ok = False
            continue
        print("PASS: %s: 1 group (%d shapes), all %d node texts present"
              % (slug, group.Count, len(texts)))
    return ok


def check_pngs(outdir):
    ok = True
    for slug, _, _ in EXPECTED:
        path = os.path.join(outdir, slug + ".png")
        if not os.path.exists(path):
            print("FAIL: missing screenshot %s" % path)
            ok = False
            continue
        size = os.path.getsize(path)
        with open(path, "rb") as f:
            head = f.read(24)
        if head[:8] != b"\x89PNG\r\n\x1a\n" or head[12:16] != b"IHDR":
            print("FAIL: %s is not a PNG" % path)
            ok = False
            continue
        dims = (int.from_bytes(head[16:20], "big"),
                int.from_bytes(head[20:24], "big"))
        if dims != PNG_SIZE:
            print("FAIL: %s is %s, expected %s" % (path, dims, PNG_SIZE))
            ok = False
        elif size < PNG_MIN_BYTES:
            print("FAIL: %s is only %d bytes — likely blank" % (path, size))
            ok = False
        else:
            print("PASS: %s.png %dx%d, %d bytes" % (slug, *dims, size))
    return ok


def main():
    if len(sys.argv) < 2:
        print("usage: e2e_demo_probe.py <port>")
        sys.exit(2)
    ctx = connect(int(sys.argv[1]))
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    doc = desktop.loadComponentFromURL("private:factory/simpress", "_blank", 0, ())
    os.makedirs("target", exist_ok=True)
    outdir = tempfile.mkdtemp(prefix="smartart-e2e.", dir="target")
    try:
        initial_pages = doc.getDrawPages().getCount()

        frame = doc.getCurrentController().getFrame()
        transformer = smgr.createInstanceWithContext(
            "com.sun.star.util.URLTransformer", ctx)
        url = uno.createUnoStruct("com.sun.star.util.URL")
        url.Complete = DEMO_URL
        _, url = transformer.parseStrict(url)
        disp = frame.queryDispatch(url, "_self", 0)
        if disp is None:
            print("FAIL: queryDispatch(%r) returned None" % DEMO_URL)
            sys.exit(1)

        print("==> Dispatching %s with OutputDir=%s" % (DEMO_URL, outdir))
        disp.dispatch(url, (pv("OutputDir", os.path.abspath(outdir)),))

        ok = check_result_file(os.path.abspath(outdir))
        ok = check_slides(doc, initial_pages) and ok
        ok = check_pngs(os.path.abspath(outdir)) and ok

        if ok:
            print("E2E DEMO PROBE: all checks passed")
            sys.exit(0)
        print("E2E DEMO PROBE: one or more checks failed")
        sys.exit(1)
    finally:
        doc.close(False)
        desktop.terminate()
        shutil.rmtree(outdir, ignore_errors=True)


main()
