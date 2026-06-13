# UNO tests (live LibreOffice)

These are the project's **runtime** tests — they drive a real, headless
LibreOffice over a UNO socket to check things the JVM unit tests and the `.oxt`
structure checks cannot: that the extension actually registers and dispatches,
and that the drawing API the renderer relies on behaves as assumed. This is
"layer 3" in [`../TESTING_STRATEGY.md`](../TESTING_STRATEGY.md).

They were promoted here from throwaway `/tmp` scripts because they are now
essential and need to be versioned and repeatable.

## Layout
```
uno-tests/
├── run.sh                       # launches a throwaway headless LibreOffice, runs a probe, tears down
└── probes/
    ├── _connect.py              # shared: connect to the running instance over a socket
    ├── registration_probe.py    # menu item is in the merged config AND the command dispatches
    └── render_probe.py          # the rectangle / connector / group API sequence SlideRenderer uses
```

The throwaway user profile is created under `target/` (gitignored), never `/tmp`.

## Requirements
- `libreoffice` (`unopkg`, `soffice`) and `python3-uno` on `PATH`.
- A display, or `xvfb` for headless CI (`xvfb-run -a …`).

## Run
```bash
mvn clean package            # needed for the registration probe (builds the .oxt)

# registration + dispatch (installs the .oxt into the throwaway profile first)
uno-tests/run.sh --install target/SmartArt.oxt uno-tests/probes/registration_probe.py

# rendering API (no extension needed — exercises a fresh Impress doc)
uno-tests/run.sh uno-tests/probes/render_probe.py
```
Each prints `UNO TEST PASS: <probe>` / `UNO TEST FAIL: …` and exits 0 / non-zero.
Override the UNO socket port with `UNO_TEST_PORT` if 2150 is taken.

## CI
`.github/workflows/build-and-validate.yml` runs both probes under `xvfb` on every
push, after the Maven build and `.oxt` structure validation.

## Note
`render_probe.py` is an **API-contract smoke test**: it mirrors
`SlideRenderer.drawHierarchy`'s UNO calls to catch a LibreOffice that behaves
differently (a service name, connector gluing, or grouping) — it does not invoke
the Java renderer itself (that needs the modal dialog, which can't run headless).
