#!/usr/bin/env python3
"""Exports the README screenshots through the REAL Java pipeline.

Usage (via scripts/make-screenshots.sh):
    export_screenshots.py <port> <output-dir>

Dispatches org.libreimpress.smartart:Demo with OutputDir=<output-dir> against
a fresh Impress doc — DemoRunner renders every diagram type via the shipped
SlideRenderer and exports one 1280x960 PNG per type, so the screenshots always
match actual output (unlike the retired hand-synced screenshot_probe.py).

Exit 0 = all types exported OK, 1 = a type failed, 2 = could not connect.
"""

import os
import sys

import uno
from com.sun.star.beans import PropertyValue

from _connect import connect


def main():
    if len(sys.argv) < 3:
        print("usage: export_screenshots.py <port> <output-dir>")
        sys.exit(2)
    port, outdir = int(sys.argv[1]), os.path.abspath(sys.argv[2])

    ctx = connect(port)
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    doc = desktop.loadComponentFromURL("private:factory/simpress", "_blank", 0, ())
    try:
        frame = doc.getCurrentController().getFrame()
        transformer = smgr.createInstanceWithContext(
            "com.sun.star.util.URLTransformer", ctx)
        url = uno.createUnoStruct("com.sun.star.util.URL")
        url.Complete = "org.libreimpress.smartart:Demo"
        _, url = transformer.parseStrict(url)
        disp = frame.queryDispatch(url, "_self", 0)
        if disp is None:
            print("FAIL: Demo dispatch unavailable — is the extension installed?")
            sys.exit(1)
        arg = PropertyValue()
        arg.Name = "OutputDir"
        arg.Value = outdir
        disp.dispatch(url, (arg,))

        result = os.path.join(outdir, "demo-result.txt")
        with open(result, encoding="utf-8") as f:
            lines = [ln.strip() for ln in f if ln.strip()]
        bad = [ln for ln in lines if not ln.startswith("OK ")]
        for ln in lines:
            print("  " + ln)
        os.remove(result)  # not a screenshot; keep the output dir clean
        if bad:
            print("FAIL: %d diagram type(s) did not export" % len(bad))
            sys.exit(1)
        print("PASS: %d screenshots exported to %s" % (len(lines), outdir))
    finally:
        doc.close(False)
        desktop.terminate()


main()
