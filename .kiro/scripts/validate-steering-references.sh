#!/bin/bash

# Validate Steering File References
# This script checks if all #[[file:]] references in steering files point to valid files

set -e

STEERING_DIR=".kiro/steering"
EXAMPLES_DIR=".kiro/examples"
ERRORS=0

echo "🔍 Validating steering file references..."
echo ""

# Function to check if a file exists
check_file() {
    local ref_file=$1
    local source_file=$2
    
    if [ ! -f "$ref_file" ]; then
        echo "❌ ERROR: Referenced file not found"
        echo "   Source: $source_file"
        echo "   Reference: $ref_file"
        echo ""
        ((ERRORS++))
    fi
}

# Find all #[[file:]] references in steering files
grep -r "#\[\[file:" "$STEERING_DIR" 2>/dev/null | while IFS=: read -r file rest; do
    # Extract reference using sed (more portable than grep -P)
    reference=$(echo "$rest" | sed -n 's/.*#\[\[file:\([^]]*\)\].*/\1/p')
    
    if [ -n "$reference" ]; then
        # Resolve relative path from the steering file
        dir=$(dirname "$file")
        ref_file="$dir/$reference"
        
        # Check if file exists (using test instead of realpath for portability)
        if [ ! -f "$ref_file" ]; then
            echo "❌ ERROR: Referenced file not found"
            echo "   Source: $file"
            echo "   Reference: $ref_file"
            echo ""
            ((ERRORS++))
        else
            echo "✓ Valid: $reference (from $(basename $file))"
        fi
    fi
done

echo ""
if [ $ERRORS -eq 0 ]; then
    echo "✅ All references are valid!"
    exit 0
else
    echo "❌ Found $ERRORS invalid reference(s)"
    exit 1
fi
