#!/usr/bin/env bash
set -euo pipefail

# Package the Compose Desktop release distributable into an AppImage.
#
# Prerequisites:
#   - Run `./gradlew app-desktop:createReleaseDistributable` first.
#   - The appropriate `appimagetool` binary must be downloaded.
#
# Usage:
#   ./bin/package-appimage.sh <appimagetool-path> [arch]
#
# Arguments:
#   appimagetool-path  Path to the appimagetool binary (required).
#   arch               Architecture tag (x86_64 or aarch64); defaults to x86_64.

if [ $# -lt 1 ]; then
  echo "Error: appimagetool path is required."
  echo "Usage: $0 <appimagetool-path> [arch]"
  exit 1
fi

APPIMAGE_TOOL="$1"
ARCH="${2:-x86_64}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

APP_DIR="$ROOT_DIR/modules/app-desktop/build/compose/binaries/main-release/app/archivekeep-desktop"
APP_IMAGE_DIR="$ROOT_DIR/ArchiveKeep.AppDir"

# --- Check prerequisites ---

if [ ! -d "$APP_DIR" ]; then
  echo "Error: Distributable not found at $APP_DIR"
  echo "Run: ./gradlew app-desktop:createReleaseDistributable"
  exit 1
fi

if ! command -v "$APPIMAGE_TOOL" &>/dev/null && [ ! -f "$APPIMAGE_TOOL" ]; then
  echo "Error: appimagetool not found at: $APPIMAGE_TOOL"
  echo "Download it with:"
  echo "  wget -q https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-${ARCH}.AppImage"
  echo "  chmod +x appimagetool-${ARCH}.AppImage"
  exit 1
fi

# --- Prepare AppDir ---

echo "Preparing AppDir..."
rm -rf "$APP_IMAGE_DIR"
cp -r "$APP_DIR" "$APP_IMAGE_DIR"

# Create AppRun entry point
cat > "$APP_IMAGE_DIR/AppRun" << 'EOF'
#!/bin/sh
APPDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
exec "$APPDIR/bin/archivekeep-desktop" "$@"
EOF
chmod +x "$APP_IMAGE_DIR/AppRun"

# Copy .desktop file from repository
cp "$ROOT_DIR/distribution/resources/org.archivekeep.ArchiveKeep.desktop" \
   "$APP_IMAGE_DIR/org.archivekeep.ArchiveKeep.desktop"

# Copy icon if available
if [ -f "$ROOT_DIR/modules/app-desktop/src/main/composeResources/drawable/ic_app.svg" ]; then
  cp "$ROOT_DIR/modules/app-desktop/src/main/composeResources/drawable/ic_app.svg" "$APP_IMAGE_DIR/archivekeep.svg"
fi

# --- Build AppImage ---

echo "Building AppImage for ${ARCH}..."
cd "$ROOT_DIR"
ARCH="${ARCH}" "$APPIMAGE_TOOL" --no-appstream ArchiveKeep.AppDir

echo "Done. AppImage created in: $ROOT_DIR"
