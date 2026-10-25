#!/usr/bin/env bash
# Generate diagram screenshots for the README.
#
# Renders through the REAL Java pipeline: dispatches the Demo command with an
# OutputDir argument, so DemoRunner/SlideRenderer (the shipped code) produce
# every PNG. The old hand-synced screenshot_probe.py is no longer used here.
#
# Usage:
#   scripts/make-screenshots.sh [--oxt <path>]
#
# Builds the extension (unless --oxt is given), starts a throwaway headless
# LibreOffice instance, draws each diagram type, and exports PNGs to
#   docs/screenshots/
#
# Requires: libreoffice (soffice, unopkg), python3-uno
# Run from the repo root.

set -euo pipefail

OXT=""
if [ "${1:-}" = "--oxt" ]; then
    OXT="${2:?--oxt needs a path}"
    shift 2
fi

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="${REPO_ROOT}/docs/screenshots"
PORT="${SCREENSHOT_PORT:-2156}"
WORK="$(mktemp -d "${REPO_ROOT}/target/screenshots.XXXXXX")"
PROFILE="file://$(printf '%s' "${WORK}/profile" | sed 's/ /%20/g')"

mkdir -p "${OUT_DIR}"

cleanup() {
    [ -n "${SOFFICE_PGID:-}" ] && kill -- -"${SOFFICE_PGID}" 2>/dev/null || true
    rm -rf "${WORK}"
}
trap cleanup EXIT

# Build unless an .oxt was provided
if [ -z "${OXT}" ]; then
    echo "==> Building extension"
    mvn -q clean package -f "${REPO_ROOT}/pom.xml"
    OXT="${REPO_ROOT}/target/SmartArt.oxt"
fi

echo "==> Installing ${OXT}"
unopkg add --suppress-license -env:UserInstallation="${PROFILE}" "${OXT}"

echo "==> Starting headless LibreOffice on port ${PORT}"
setsid soffice --headless --invisible --norestore --nologo \
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

echo "==> Drawing diagrams and exporting PNGs to ${OUT_DIR}/"
python3 "${REPO_ROOT}/uno-tests/probes/export_screenshots.py" "${PORT}" "${OUT_DIR}"

echo "==> Done"
ls -lh "${OUT_DIR}/"
