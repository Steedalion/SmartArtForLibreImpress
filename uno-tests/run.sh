#!/usr/bin/env bash
#
# Run a headless-LibreOffice UNO probe from this repo.
#
# Launches an isolated, throwaway LibreOffice instance (profile under target/,
# never /tmp), runs the given probe against it over a UNO socket, and tears
# everything down. Optionally installs an .oxt first (for the registration probe).
#
# Usage:
#   uno-tests/run.sh [--install <path-to.oxt>] <probe.py>
#
# Examples:
#   uno-tests/run.sh --install target/SmartArt.oxt uno-tests/probes/registration_probe.py
#   uno-tests/run.sh uno-tests/probes/render_probe.py
#
# Requires libreoffice (unopkg, soffice) + python3-uno on PATH. Under CI, wrap
# with xvfb:  xvfb-run -a uno-tests/run.sh ...
#
# Exit: 0 = pass, non-zero = failure.

set -u

INSTALL=""
if [ "${1:-}" = "--install" ]; then
    INSTALL="${2:?--install needs an .oxt path}"
    shift 2
fi
PROBE="${1:?usage: uno-tests/run.sh [--install <oxt>] <probe.py>}"
PORT="${UNO_TEST_PORT:-2150}"

mkdir -p target
WORK="$(mktemp -d "${PWD}/target/uno-test.XXXXXX")"
# The repo path may contain spaces; a file:// URL must percent-encode them.
PROFILE="file://$(printf '%s' "${WORK}/profile" | sed 's/ /%20/g')"
SOFFICE_PGID=""

cleanup() {
    [ -n "${SOFFICE_PGID}" ] && kill -- -"${SOFFICE_PGID}" 2>/dev/null
    rm -rf "${WORK}"
}
trap cleanup EXIT

if [ ! -f "${PROBE}" ]; then
    echo "UNO TEST FAIL: no such probe: ${PROBE}" >&2
    exit 2
fi

if [ -n "${INSTALL}" ]; then
    if [ ! -f "${INSTALL}" ]; then
        echo "UNO TEST FAIL: no such .oxt: ${INSTALL} (build it with 'mvn clean package')" >&2
        exit 2
    fi
    echo "==> Installing ${INSTALL} into a throwaway profile"
    if ! unopkg add --suppress-license -env:UserInstallation="${PROFILE}" "${INSTALL}"; then
        echo "UNO TEST FAIL: unopkg add failed" >&2
        exit 1
    fi
fi

echo "==> Starting headless LibreOffice on port ${PORT}"
setsid soffice --headless --invisible --norestore --nologo --nofirststartwizard \
    -env:UserInstallation="${PROFILE}" \
    --accept="socket,host=localhost,port=${PORT};urp;" \
    >"${WORK}/soffice.log" 2>&1 &
SOFFICE_PGID=$!

for _ in $(seq 1 60); do
    if python3 -c "import socket,sys; s=socket.socket(); s.settimeout(0.3); \
        sys.exit(0 if s.connect_ex(('localhost',${PORT}))==0 else 1)" 2>/dev/null; then
        break
    fi
    sleep 0.5
done

echo "==> Running $(basename "${PROBE}")"
python3 "${PROBE}" "${PORT}"
rc=$?

if [ "${rc}" -eq 0 ]; then
    echo "UNO TEST PASS: $(basename "${PROBE}")"
else
    echo "UNO TEST FAIL: $(basename "${PROBE}") (rc=${rc})" >&2
    tail -n 20 "${WORK}/soffice.log" 2>/dev/null
fi
exit "${rc}"
