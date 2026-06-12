#!/usr/bin/env bash
#
# Runtime verification test for the SmartArt extension.
#
# Installs the built .oxt into a throwaway LibreOffice profile, starts a headless
# listening instance, and runs probe_extension.py to assert that the menu item is
# registered AND its command dispatches. This catches registration/dispatch
# regressions (e.g. the component jar in the wrong directory) that packaging
# checks cannot — see impressSmartArt.md §5.5.
#
# Requires: libreoffice (unopkg, soffice) and python3-uno on PATH.
# Run headless under xvfb in CI:  xvfb-run -a tools/verify-extension.sh
#
# Usage: tools/verify-extension.sh [path-to-oxt] [port]
# Exit:  0 = pass, non-zero = failure.

set -u

OXT="${1:-target/SmartArt.oxt}"
PORT="${2:-2102}"
HERE="$(cd "$(dirname "$0")" && pwd)"
PROFILE_DIR="$(mktemp -d)"
PROFILE="file://${PROFILE_DIR}"
SOFFICE_PGID=""

cleanup() {
    [ -n "${SOFFICE_PGID}" ] && kill -- -"${SOFFICE_PGID}" 2>/dev/null
    rm -rf "${PROFILE_DIR}"
}
trap cleanup EXIT

if [ ! -f "${OXT}" ]; then
    echo "VERIFY FAIL: no such .oxt: ${OXT} (build it with 'mvn clean package')" >&2
    exit 2
fi

echo "==> Installing ${OXT} into a throwaway profile"
if ! unopkg add --suppress-license -env:UserInstallation="${PROFILE}" "${OXT}"; then
    echo "VERIFY FAIL: unopkg add failed" >&2
    exit 1
fi

echo "==> Starting headless LibreOffice on port ${PORT}"
setsid soffice --headless --invisible --norestore --nologo --nofirststartwizard \
    -env:UserInstallation="${PROFILE}" \
    --accept="socket,host=localhost,port=${PORT};urp;" \
    >/tmp/verify_soffice.log 2>&1 &
SOFFICE_PGID=$!

for _ in $(seq 1 60); do
    if python3 -c "import socket,sys; s=socket.socket(); s.settimeout(0.3); \
        sys.exit(0 if s.connect_ex(('localhost',${PORT}))==0 else 1)" 2>/dev/null; then
        break
    fi
    sleep 0.5
done

echo "==> Probing registration and dispatch"
python3 "${HERE}/probe_extension.py" "${PORT}"
rc=$?

if [ "${rc}" -eq 0 ]; then
    echo "VERIFY PASS"
else
    echo "VERIFY FAIL: probe exited ${rc} (see /tmp/verify_soffice.log)" >&2
fi
exit "${rc}"
