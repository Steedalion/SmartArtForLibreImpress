"""Shared helper: connect to the headless LibreOffice started by run.sh."""

import sys
import time

import uno


def connect(port, tries=60):
    """Resolve a UNO component context over the socket on localhost:<port>.

    Exits with code 2 if the connection never comes up.
    """
    local = uno.getComponentContext()
    resolver = local.ServiceManager.createInstanceWithContext(
        "com.sun.star.bridge.UnoUrlResolver", local)
    last = None
    for _ in range(tries):
        try:
            return resolver.resolve(
                "uno:socket,host=localhost,port=%d;urp;StarOffice.ComponentContext"
                % port)
        except Exception as e:  # noqa: BLE001
            last = e
            time.sleep(0.5)
    print("could not connect to port %d: %s" % (port, last))
    sys.exit(2)
