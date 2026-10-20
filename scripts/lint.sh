#!/usr/bin/env bash
set -euo pipefail

# scripts/lint.sh - Checks bash scripts, Android XML files, and source syntax

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

FAILED=0

echo -e "${BLUE}=== Running Helium Android Linters ===${NC}"

# 1. Bash syntax check
echo -e "\n${BLUE}[1/3] Checking bash syntax...${NC}"
for script in scripts/*.sh; do
    if [ -f "$script" ]; then
        if bash -n "$script"; then
            echo -e "${GREEN}✓ ${script} syntax ok${NC}"
        else
            echo -e "${RED}✗ ${script} syntax error${NC}"
            FAILED=$((FAILED + 1))
        fi
    fi
done

# 2. XML syntax check
echo -e "\n${BLUE}[2/3] Checking Android XML resources...${NC}"
XML_COUNT=0
while IFS= read -r xml_file; do
    XML_COUNT=$((XML_COUNT + 1))
    if python3 -c "import xml.etree.ElementTree as ET; ET.parse('$xml_file')" 2>/dev/null; then
        echo -e "${GREEN}✓ ${xml_file} valid XML${NC}"
    else
        echo -e "${RED}✗ ${xml_file} malformed XML${NC}"
        FAILED=$((FAILED + 1))
    fi
done < <(find helium-android -type f -name "*.xml" 2>/dev/null || true)

if [ "$XML_COUNT" -eq 0 ]; then
    echo "No XML files found yet to check."
fi

# 3. GitHub Actions YAML check
echo -e "\n${BLUE}[3/3] Checking GitHub Actions workflows...${NC}"
YAML_COUNT=0
while IFS= read -r yaml_file; do
    YAML_COUNT=$((YAML_COUNT + 1))
    if python3 -c "import yaml; yaml.safe_load(open('$yaml_file'))" 2>/dev/null; then
        echo -e "${GREEN}✓ ${yaml_file} valid YAML${NC}"
    else
        echo -e "${RED}✗ ${yaml_file} malformed YAML${NC}"
        FAILED=$((FAILED + 1))
    fi
done < <(find .github/workflows -type f -name "*.yml" 2>/dev/null || true)

if [ "$YAML_COUNT" -eq 0 ]; then
    echo "No YAML workflows found yet to check."
fi

# 4. Android Resource Reference Integrity check
echo -e "\n${BLUE}[4/4] Checking Android resource references...${NC}"
AVAILABLE_COLORS=$(grep -oE '<color name="[^"]+"' helium-android/res/values/colors.xml | sed 's/<color name="//;s/"//' | sort -u || true)
while IFS= read -r f; do
    # Find all @color/ references
    for ref in $(grep -oE '@color/[a-zA-Z0-9_]+' "$f" | sed 's/@color\///' | sort -u); do
        if ! echo "$AVAILABLE_COLORS" | grep -qx "$ref"; then
            echo -e "${RED}✗ ${f}: missing @color/${ref}${NC}"
            FAILED=$((FAILED + 1))
        fi
    done
done < <(find helium-android/res/layout helium-android/res/drawable -type f -name "*.xml" 2>/dev/null || true)
if [ "$FAILED" -eq 0 ]; then
    echo -e "${GREEN}✓ All resource color references are defined${NC}"
fi

# 5. Java Import Integrity check
echo -e "\n${BLUE}[5/5] Checking Java symbol import integrity...${NC}"
JAVA_IMPORT_ERRORS=0
for f in $(find helium-android/src/main/java -name "*.java"); do
    if grep -E '\bBuild\.VERSION' "$f" >/dev/null && ! grep -E 'import android\.os\.Build;' "$f" >/dev/null; then
        echo -e "${RED}✗ ${f}: missing import android.os.Build;${NC}"
        JAVA_IMPORT_ERRORS=$((JAVA_IMPORT_ERRORS + 1))
    fi
    if grep -E '\bToast\.makeText' "$f" >/dev/null && ! grep -E 'import android\.widget\.Toast;' "$f" >/dev/null; then
        echo -e "${RED}✗ ${f}: missing import android.widget.Toast;${NC}"
        JAVA_IMPORT_ERRORS=$((JAVA_IMPORT_ERRORS + 1))
    fi
done
if [ "$JAVA_IMPORT_ERRORS" -gt 0 ]; then
    FAILED=$((FAILED + JAVA_IMPORT_ERRORS))
else
    echo -e "${GREEN}✓ All Java SDK symbol imports verified${NC}"
fi

echo "--------------------------------------------------------"
if [ "$FAILED" -gt 0 ]; then
    echo -e "${RED}Linting finished with ${FAILED} errors.${NC}"
    exit 1
else
    echo -e "${GREEN}All checks passed successfully!${NC}"
    exit 0
fi
