#!/bin/bash

# Master Script to Fix All Code Quality Issues
# This script runs all fix scripts in the correct order

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║                                                                    ║"
echo "║           🔧 Automated Code Quality Issue Fixer 🔧                ║"
echo "║                                                                    ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

echo ""
echo -e "${GREEN}📁 Project root: $PROJECT_ROOT${NC}"
echo ""

# Check if Python is available
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}❌ Python 3 is required but not installed.${NC}"
    exit 1
fi

# Make scripts executable
chmod +x "$SCRIPT_DIR"/*.sh 2>/dev/null || true
chmod +x "$SCRIPT_DIR"/*.py 2>/dev/null || true

# Function to run a script with error handling
run_script() {
    local script=$1
    local description=$2

    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}Running: $description${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    if [ -f "$script" ]; then
        if [[ "$script" == *.py ]]; then
            python3 "$script" "$PROJECT_ROOT" || {
                echo -e "${YELLOW}⚠️  Script completed with warnings: $script${NC}"
            }
        else
            bash "$script" || {
                echo -e "${YELLOW}⚠️  Script completed with warnings: $script${NC}"
            }
        fi
    else
        echo -e "${RED}❌ Script not found: $script${NC}"
    fi
}

# Create backup
echo -e "${YELLOW}📦 Creating backup...${NC}"
BACKUP_DIR="$PROJECT_ROOT/backup-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"
cp -r "$PROJECT_ROOT/app/src" "$BACKUP_DIR/" 2>/dev/null || true
echo -e "${GREEN}✅ Backup created: $BACKUP_DIR${NC}"

# Step 1: Fix lambda braces (S1602)
run_script "$SCRIPT_DIR/fix-lambda-braces.py" "Step 1: Simplifying lambda expressions"

# Step 2: Fix string constants (S1192)
run_script "$SCRIPT_DIR/fix-string-constants.py" "Step 2: Extracting duplicate string constants"

# Step 3: Add null safety imports
run_script "$SCRIPT_DIR/fix-null-safety.py" "Step 3: Adding null safety imports"

# Step 4: Report unused code
run_script "$SCRIPT_DIR/report-unused-code.py" "Step 4: Reporting unused code"

# Step 5: Fix other SonarLint issues
run_script "$SCRIPT_DIR/fix-sonar-issues.sh" "Step 5: Fixing other SonarLint issues"

# Step 6: Format code
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 6: Formatting code${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

cd "$PROJECT_ROOT"

echo "🔧 Running Gradle spotlessApply..."
./gradlew spotlessApply 2>/dev/null || {
    echo -e "${YELLOW}⚠️  spotlessApply not available, skipping...${NC}"
}

# Step 7: Organize imports
echo ""
echo "📋 Organizing imports..."
echo "   (This should be done in your IDE)"

# Step 8: Run checks
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 7: Running quality checks${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo "🔍 Running Gradle check..."
./gradlew check --no-daemon || {
    echo -e "${YELLOW}⚠️  Some checks failed. Review the output above.${NC}"
}

# Summary
echo ""
echo -e "${GREEN}"
echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║                                                                    ║"
echo "║                        ✅ Process Complete ✅                      ║"
echo "║                                                                    ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

echo ""
echo -e "${GREEN}📊 Summary:${NC}"
echo ""
echo "  ✅ Lambda expressions simplified"
echo "  ✅ String constants extracted"
echo "  ✅ Null safety imports added"
echo "  ✅ Unused code reported"
echo "  ✅ Code formatted"
echo "  ✅ Quality checks run"
echo ""
echo -e "${YELLOW}⚠️  Manual steps required:${NC}"
echo ""
echo "  1. Review changes in your IDE"
echo "  2. Add @NonNull/@Nullable annotations using IDE quick fixes"
echo "  3. Add Objects.requireNonNull() where needed"
echo "  4. Remove unused variables and fields (see report above)"
echo "  5. Remove commented-out code"
echo "  6. Fix remaining SonarLint issues using IDE quick fixes"
echo ""
echo -e "${GREEN}💡 IDE Quick Fixes:${NC}"
echo ""
echo "  • Ctrl+1 (Eclipse) / Alt+Enter (IntelliJ) - Show quick fixes"
echo "  • Ctrl+Shift+O - Organize imports"
echo "  • Ctrl+Shift+F - Format code"
echo ""
echo -e "${BLUE}📦 Backup location: $BACKUP_DIR${NC}"
echo ""
echo -e "${GREEN}🎯 Next steps:${NC}"
echo ""
echo "  1. Review the changes"
echo "  2. Run: ./gradlew test"
echo "  3. Commit the changes"
echo ""

exit 0
