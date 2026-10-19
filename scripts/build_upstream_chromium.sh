#!/usr/bin/env bash
# ==============================================================================
# Helium Browser for Android - Upstream Chromium Build Automation Pipeline
# ==============================================================================
# Automates fetching upstream Chromium, applying Helium unified diffs,
# configuring GN arguments with hardening, and compiling chrome_public_apk.
# ==============================================================================
set -euo pipefail

HELIUM_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CHROMIUM_SRC="${CHROMIUM_SRC:-${HELIUM_ROOT}/src}"
BUILD_OUT="${CHROMIUM_SRC}/out/Default"
TARGET_ARCH="${1:-arm64}"

echo "========================================================"
echo " Helium Upstream Chromium Android Build Automation"
echo " Target Architecture: ${TARGET_ARCH}"
echo " Source Path:         ${CHROMIUM_SRC}"
echo "========================================================"

# 1. Verify depot_tools availability
if ! command -v fetch &>/dev/null || ! command -v gn &>/dev/null; then
    echo "[!] Google depot_tools not detected in PATH."
    echo "    Install depot_tools to build monolithic Chromium:"
    echo "    git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git ~/.depot_tools"
    echo "    export PATH=\"\$HOME/.depot_tools:\$PATH\""
    exit 1
fi

# 2. Check disk space (minimum 80GB recommended for Android Chromium tree)
AVAILABLE_KB=$(df -k . | awk 'NR==2 {print $4}')
if [ "$AVAILABLE_KB" -lt 80000000 ]; then
    echo "[WARNING] Less than 80GB free disk space detected. Chromium build may exhaust storage."
fi

# 3. Fetch upstream Android Chromium tree if not already cloned
if [ ! -d "${CHROMIUM_SRC}" ]; then
    echo "[1/5] Fetching upstream Chromium for Android..."
    mkdir -p "${CHROMIUM_SRC}"
    cd "${CHROMIUM_SRC}/.."
    fetch --nohooks android
    cd "${CHROMIUM_SRC}"
    gclient sync -D
else
    echo "[1/5] Upstream source tree detected at ${CHROMIUM_SRC}."
fi

cd "${CHROMIUM_SRC}"

# 4. Apply Helium Security & Extension Patchsets
echo "[2/5] Applying Helium unified patches..."
for patch_dir in "${HELIUM_ROOT}/patches"/*; do
    if [ -d "$patch_dir" ]; then
        for patch_file in "$patch_dir"/*.patch; do
            if [ -f "$patch_file" ]; then
                echo "  -> Applying $(basename "$patch_file")..."
                git apply --check "$patch_file" || {
                    echo "  [WARN] Patch $(basename "$patch_file") failed dry-run. Skipping if already applied."
                    continue
                }
                git apply "$patch_file"
            fi
        done
    fi
done

# 5. Configure GN build arguments
echo "[3/5] Generating GN build configuration..."
mkdir -p "${BUILD_OUT}"

cat <<EOF > "${BUILD_OUT}/args.gn"
target_os = "android"
target_cpu = "${TARGET_ARCH}"
is_debug = false
is_official_build = true
is_component_build = false
symbol_level = 0
blink_symbol_level = 0
chrome_pgo_phase = 0
ffmpeg_branding = "Chrome"
proprietary_codecs = true
enable_nacl = false
v8_enable_future = true
EOF

gn gen "${BUILD_OUT}"

# 6. Compile Chrome Public APK with Helium patches
echo "[4/5] Compiling Chrome Public APK via autoninja..."
autoninja -C "${BUILD_OUT}" chrome_public_apk

# 7. Locate output artifact
OUTPUT_APK="${BUILD_OUT}/apks/ChromePublic.apk"
if [ -f "${OUTPUT_APK}" ]; then
    echo "[5/5] Build SUCCESS!"
    echo "Monolithic Helium APK generated at:"
    echo "  -> ${OUTPUT_APK}"
else
    echo "[!] Build finished, but expected APK was not found at ${OUTPUT_APK}"
    exit 1
fi
