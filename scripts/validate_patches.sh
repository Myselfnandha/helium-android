#!/usr/bin/env bash
set -euo pipefail

# scripts/validate_patches.sh - Validates syntax and structure of all patch files

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

TARGET_DIR="${1:-.}"
FAILED=0
CHECKED=0

echo -e "${BLUE}=== Validating unified diff patches in ${TARGET_DIR} ===${NC}"

# Find all .patch files in patches/ and subprojects_patches/
while IFS= read -r patch_file; do
    CHECKED=$((CHECKED + 1))
    echo -n "Checking $(basename "$patch_file") ... "

    # Check 1: File is non-empty
    if [ ! -s "$patch_file" ]; then
        echo -e "${RED}FAILED: Empty file${NC}"
        FAILED=$((FAILED + 1))
        continue
    fi

    # Check 2: Check for CRLF line endings
    if file "$patch_file" | grep -q "CRLF"; then
        echo -e "${RED}FAILED: Contains Windows CRLF line endings${NC}"
        FAILED=$((FAILED + 1))
        continue
    fi

    # Check 3: Check for standard unified diff headers (--- and +++)
    if ! grep -q "^--- " "$patch_file" || ! grep -q "^+++ " "$patch_file"; then
        echo -e "${RED}FAILED: Missing standard unified diff headers (--- / +++)${NC}"
        FAILED=$((FAILED + 1))
        continue
    fi

    # Check 4: Check for chunk headers (@@ ... @@)
    if ! grep -q "^@@ " "$patch_file"; then
        echo -e "${RED}FAILED: Missing diff chunk header (@@ ... @@)${NC}"
        FAILED=$((FAILED + 1))
        continue
    fi

    echo -e "${GREEN}VALID${NC}"
done < <(find "$TARGET_DIR" -type f -name "*.patch" | sort)

echo "--------------------------------------------------------"
if [ "$CHECKED" -eq 0 ]; then
    echo -e "${YELLOW}Notice: No .patch files found to validate in ${TARGET_DIR}.${NC}"
    exit 0
elif [ "$FAILED" -gt 0 ]; then
    echo -e "${RED}Validation completed with ${FAILED} errors out of ${CHECKED} patches checked.${NC}"
    exit 1
else
    echo -e "${GREEN}All ${CHECKED} patch files passed validation successfully!${NC}"
    exit 0
fi
