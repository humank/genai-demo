#!/bin/bash

# Validate #[[file:]] references in steering rules
# This script checks if all referenced files exist

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KIRO_DIR="$(dirname "$SCRIPT_DIR")"
STEERING_DIR="$KIRO_DIR/steering"

echo "🔍 Validating #[[file:]] references in steering rules..."
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

total_refs=0
valid_refs=0
invalid_refs=0
missing_files=()

# Find all markdown files in steering directory
while IFS= read -r file; do
    # Extract #[[file:...]] references
    while IFS= read -r line; do
        if [[ $line =~ \#\[\[file:([^\]]+)\]\] ]]; then
            ref_path="${BASH_REMATCH[1]}"
            total_refs=$((total_refs + 1))
            
            # Resolve relative path from the file's directory
            file_dir="$(dirname "$file")"
            
            # Handle relative paths
            if [[ $ref_path == ../* ]]; then
                # Relative to parent directory
                full_path="$file_dir/$ref_path"
            elif [[ $ref_path == ./* ]]; then
                # Relative to current directory
                full_path="$file_dir/$ref_path"
            else
                # Relative to current directory (no ./)
                full_path="$file_dir/$ref_path"
            fi
            
            # Normalize path
            full_path="$(cd "$(dirname "$full_path")" 2>/dev/null && pwd)/$(basename "$full_path")" || full_path="$full_path"
            
            # Check if file or directory exists
            if [[ -e "$full_path" ]]; then
                valid_refs=$((valid_refs + 1))
                echo -e "${GREEN}✓${NC} $ref_path (from $(basename "$file"))"
            else
                invalid_refs=$((invalid_refs + 1))
                missing_files+=("$ref_path (referenced in $(basename "$file"))")
                echo -e "${RED}✗${NC} $ref_path (from $(basename "$file")) - NOT FOUND"
            fi
        fi
    done < "$file"
done < <(find "$STEERING_DIR" -name "*.md" -type f)

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Validation Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Total references found: $total_refs"
echo -e "${GREEN}Valid references: $valid_refs${NC}"
echo -e "${RED}Invalid references: $invalid_refs${NC}"
echo ""

if [ $invalid_refs -gt 0 ]; then
    echo -e "${RED}❌ Missing files:${NC}"
    for missing in "${missing_files[@]}"; do
        echo "  - $missing"
    done
    echo ""
    exit 1
else
    echo -e "${GREEN}✅ All file references are valid!${NC}"
    echo ""
    exit 0
fi
