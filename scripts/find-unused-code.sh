#!/bin/bash

# Script to find unused fields and methods in Java code
# This script helps identify potential code cleanup opportunities

echo "🔍 Finding Unused Code in Java Files..."
echo "========================================"
echo ""

# Find files with private fields
echo "📊 Analyzing private fields..."
PRIVATE_FIELDS=$(find app/src/main/java -name "*.java" -type f -exec grep -l "private.*final.*;" {} \; | wc -l)
echo "Files with private fields: $PRIVATE_FIELDS"
echo ""

# Find files with private methods
echo "📊 Analyzing private methods..."
PRIVATE_METHODS=$(find app/src/main/java -name "*.java" -type f -exec grep -l "private.*(" {} \; | wc -l)
echo "Files with private methods: $PRIVATE_METHODS"
echo ""

# List some example files for manual review
echo "📝 Sample files to review:"
echo "-------------------------"
find app/src/main/java -name "*.java" -type f -exec grep -l "private.*final.*;" {} \; | head -10

echo ""
echo "✅ Analysis complete!"
echo ""
echo "💡 Next steps:"
echo "1. Review files manually using IDE's 'unused' inspection"
echo "2. Check if fields/methods are used via reflection"
echo "3. Remove truly unused code"
echo "4. Keep framework-required fields (e.g., @Autowired)"
