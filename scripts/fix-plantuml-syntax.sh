#!/bin/bash

# Fix PlantUML Syntax - Add missing @enduml directives
# This script adds @enduml to all PlantUML files that are missing it

set -e

echo "=========================================="
echo "  PlantUML Syntax Fix Script"
echo "=========================================="
echo ""

# Find all .puml files
PUML_FILES=$(find docs/diagrams -name "*.puml" -type f)
FIXED_COUNT=0
TOTAL_COUNT=0

for file in $PUML_FILES; do
    TOTAL_COUNT=$((TOTAL_COUNT + 1))
    
    # Check if file ends with @enduml
    if ! tail -1 "$file" | grep -q "@enduml"; then
        echo "Fixing: $file"
        
        # Add @enduml to the end of the file
        echo "@enduml" >> "$file"
        
        FIXED_COUNT=$((FIXED_COUNT + 1))
    fi
done

echo ""
echo "=========================================="
echo "  Fix Summary"
echo "=========================================="
echo "Total PlantUML files: $TOTAL_COUNT"
echo "Files fixed: $FIXED_COUNT"
echo "Files already correct: $((TOTAL_COUNT - FIXED_COUNT))"
echo ""

if [ $FIXED_COUNT -gt 0 ]; then
    echo "✅ Fixed $FIXED_COUNT PlantUML files"
    echo ""
    echo "Next steps:"
    echo "1. Run: ./scripts/generate-diagrams.sh"
    echo "2. Verify diagrams generated correctly"
else
    echo "✅ All PlantUML files already have correct syntax"
fi

exit 0
