#!/usr/bin/env bash
set -euo pipefail

# scripts/setup_toolchain.sh - Sets up depot_tools and checks Android build prerequisites

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== Helium Browser Android: Toolchain Setup & Verification ===${NC}"

# Check for essential host binaries
REQUIRED_CMDS=("git" "python3" "curl" "tar")
for cmd in "${REQUIRED_CMDS[@]}"; do
    if ! command -v "$cmd" &>/dev/null; then
        echo -e "${YELLOW}Warning: '${cmd}' is not installed. Please install it with: sudo apt-get install -y ${cmd}${NC}"
    else
        echo -e "${GREEN}✓ Found: ${cmd}${NC}"
    fi
done

# Setup depot_tools if not already present
DEPOT_TOOLS_DIR="${DEPOT_TOOLS_DIR:-$HOME/depot_tools}"
if [ ! -d "$DEPOT_TOOLS_DIR" ]; then
    echo -e "${BLUE}Cloning depot_tools to ${DEPOT_TOOLS_DIR}...${NC}"
    git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git "$DEPOT_TOOLS_DIR"
else
    echo -e "${GREEN}✓ depot_tools is present at ${DEPOT_TOOLS_DIR}${NC}"
fi

export PATH="$DEPOT_TOOLS_DIR:$PATH"
echo -e "${GREEN}✓ PATH configured with depot_tools.${NC}"

# Check Java version (recommend Java 17)
if command -v java &>/dev/null; then
    JAVA_VER=$(java -version 2>&1 | head -n 1)
    echo -e "${GREEN}✓ Java runtime: ${JAVA_VER}${NC}"
else
    echo -e "${YELLOW}Notice: Java 17 not found in PATH. Install OpenJDK 17 for Android packaging.${NC}"
fi

echo -e "${BLUE}=== Toolchain environment ready ===${NC}"
