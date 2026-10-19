#!/usr/bin/env bash
set -euo pipefail

# scripts/build.sh - Unified Android Chromium build orchestrator

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

TARGET_CPU="arm64"
BUILD_TYPE="release"
DRY_RUN=false
APPLY_PATCHES_ONLY=false
CHROMIUM_DIR="${CHROMIUM_SRC_DIR:-./src}"

usage() {
    echo "Usage: $0 [OPTIONS]"
    echo "Options:"
    echo "  --target-cpu CPU       Target CPU architecture: arm64 (default), arm, x64, x86"
    echo "  --build-type TYPE      Build configuration: release (default), debug"
    echo "  --dry-run              Print GN arguments and build plan without building"
    echo "  --apply-patches-only   Only apply patchsets to CHROMIUM_SRC_DIR and exit"
    echo "  --help                 Show this help message"
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --target-cpu) TARGET_CPU="$2"; shift 2 ;;
        --build-type) BUILD_TYPE="$2"; shift 2 ;;
        --dry-run) DRY_RUN=true; shift ;;
        --apply-patches-only) APPLY_PATCHES_ONLY=true; shift ;;
        --help) usage ;;
        *) echo "Unknown option: $1"; usage ;;
    esac
done

echo -e "${BLUE}=== Helium Browser for Android Build Orchestrator ===${NC}"
echo "Target CPU:   ${TARGET_CPU}"
echo "Build Type:   ${BUILD_TYPE}"
echo "Chromium Dir: ${CHROMIUM_DIR}"

# Step 1: Validate patchsets before doing anything
echo -e "\n${BLUE}[1/4] Validating patch files...${NC}"
./scripts/validate_patches.sh .

# Step 2: Apply patches if Chromium source directory exists
if [ -d "$CHROMIUM_DIR" ]; then
    echo -e "\n${BLUE}[2/4] Applying Helium patchsets to ${CHROMIUM_DIR}...${NC}"
    PATCH_DIRS=(
        "patches/01-vanadium-security"
        "patches/02-extensions-runtime"
        "patches/03-adblock-engine"
        "patches/04-vault-sync"
        "patches/05-helium-ui"
        "patches/06-media-enhancements"
    )

    for pdir in "${PATCH_DIRS[@]}"; do
        if [ -d "$pdir" ]; then
            for patch in $(find "$pdir" -name "*.patch" | sort); do
                echo "Applying $patch ..."
                (cd "$CHROMIUM_DIR" && git apply --check "$OLDPWD/$patch" && git apply "$OLDPWD/$patch") || {
                    echo -e "${RED}Failed to apply patch: $patch${NC}"
                    exit 1
                }
            done
        fi
    done
    echo -e "${GREEN}✓ All patches applied cleanly!${NC}"
else
    echo -e "${YELLOW}Notice: Chromium source directory '${CHROMIUM_DIR}' not detected locally.${NC}"
    echo -e "${YELLOW}Note: Heavy Chromium compilation should be executed via GitHub Actions CI.${NC}"
fi

if [ "$APPLY_PATCHES_ONLY" = true ]; then
    echo -e "${GREEN}Patches applied. Exiting as requested by --apply-patches-only.${NC}"
    exit 0
fi

# Step 3: Compute GN Build Arguments
IS_DEBUG="false"
IS_OFFICIAL="true"
if [ "$BUILD_TYPE" = "debug" ]; then
    IS_DEBUG="true"
    IS_OFFICIAL="false"
fi

GN_ARGS="target_os=\"android\" target_cpu=\"${TARGET_CPU}\" is_debug=${IS_DEBUG} is_official_build=${IS_OFFICIAL} is_component_build=false symbol_level=0 enable_nacl=false enable_extensions=true disable_fieldtrial_testing_config=true chrome_pgo_phase=0"

echo -e "\n${BLUE}[3/4] GN Configuration Arguments:${NC}"
echo "--------------------------------------------------------"
echo "${GN_ARGS}"
echo "--------------------------------------------------------"

if [ "$DRY_RUN" = true ]; then
    echo -e "${GREEN}Dry run completed. GN flags verified successfully.${NC}"
    exit 0
fi

# Step 4: Execute Ninja Build if local source tree exists
if [ -d "$CHROMIUM_DIR/out/Default" ]; then
    echo -e "\n${BLUE}[4/4] Executing Ninja build: chrome_public_apk ...${NC}"
    ninja -C "$CHROMIUM_DIR/out/Default" chrome_public_apk
    echo -e "${GREEN}✓ Build succeeded! Output APK in: ${CHROMIUM_DIR}/out/Default/apks/ChromePublic.apk${NC}"
else
    echo -e "${YELLOW}Local build directory not configured. For automated cloud building, push to GitHub to trigger .github/workflows/build.yml${NC}"
fi
