#!/usr/bin/env python3
"""Spike probe for Phase 17: does a group shape's Description survive ODP
save/reload?

The edit-existing-diagram feature stores the source outline/type/palette in
the diagram group's Description property (serialized to <svg:desc> in ODF).
This probe proves the round-trip before the Java flow is built on it:

  1. create an Impress doc, draw two rectangles, group them;
  2. set Description (multi-line, unicode, ';'-heavy — like real metadata)
     and Name on the group;
  3. save as .odp, close, reload;
  4. assert both properties survived byte-for-byte on the reloaded group.

Run:  uno-tests/run.sh uno-tests/probes/metadata_persistence_probe.py
Exit 0 = pass, 1 = a check failed, 2 = could not connect.
"""

import os
import sys
import tempfile

import uno  # noqa: F401 — registers the com.sun.star module machinery

from com.sun.star.awt import Point, Size
from com.sun.star.beans import PropertyValue

from _connect import connect

DESCRIPTION = ("smartart:v1;type=HIERARCHY;template=DEFAULT;"
               "palette=MT0jNDQ3MkM0;outline=Um9vdAotIENoaWxkIMOfw6nDvA==\n"
               "second line; with ; semicolons")
NAME = "SmartArt: Hierarchy"


def pv(name, value):
    p = PropertyValue()
    p.Name = name
    p.Value = value
    return p


def make_rect(doc, x):
    shape = doc.createInstance("com.sun.star.drawing.RectangleShape")
    shape.Position = Point(x, 2000)
    shape.Size = Size(3000, 2000)
    return shape


def main():
    if len(sys.argv) < 2:
        print("usage: metadata_persistence_probe.py <port>")
        sys.exit(2)
    ctx = connect(int(sys.argv[1]))
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)

    os.makedirs("target", exist_ok=True)
    fd, path = tempfile.mkstemp(prefix="smartart-meta.", suffix=".odp",
                                dir="target")
    os.close(fd)
    url = "file://" + os.path.abspath(path)
    ok = False
    try:
        doc = desktop.loadComponentFromURL("private:factory/simpress",
                                           "_blank", 0, ())
        page = doc.getDrawPages().getByIndex(0)
        a, b = make_rect(doc, 2000), make_rect(doc, 6000)
        page.add(a)
        page.add(b)
        collection = smgr.createInstanceWithContext(
            "com.sun.star.drawing.ShapeCollection", ctx)
        collection.add(a)
        collection.add(b)
        group = page.group(collection)
        group.Description = DESCRIPTION
        group.Name = NAME

        doc.storeToURL(url, (pv("FilterName", "impress8"),))
        doc.close(False)

        doc = desktop.loadComponentFromURL(url, "_blank", 0, ())
        page = doc.getDrawPages().getByIndex(0)
        groups = [page.getByIndex(i) for i in range(page.Count)
                  if page.getByIndex(i).supportsService(
                      "com.sun.star.drawing.GroupShape")]
        if len(groups) != 1:
            print("FAIL: expected 1 group after reload, got %d" % len(groups))
        else:
            g = groups[0]
            if g.Description != DESCRIPTION:
                print("FAIL: Description changed after reload:\n  before=%r\n"
                      "  after =%r" % (DESCRIPTION, g.Description))
            elif g.Name != NAME:
                print("FAIL: Name changed after reload: %r" % g.Name)
            else:
                print("PASS: Description (%d chars, multiline/unicode/';') and "
                      "Name survived ODP save/reload" % len(DESCRIPTION))
                ok = True
        doc.close(False)
    finally:
        desktop.terminate()
        try:
            os.remove(path)
        except OSError:
            pass

    if ok:
        print("METADATA PERSISTENCE PROBE: all checks passed")
        sys.exit(0)
    print("METADATA PERSISTENCE PROBE: one or more checks failed")
    sys.exit(1)


main()
