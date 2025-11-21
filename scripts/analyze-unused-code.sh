#!/bin/bash

# Advanced script to analyze potentially unused code
# This helps prioritize manual review

echo "🔍 Advanced Unused Code Analysis"
echo "=================================="
echo ""

# 1. Find example/demo classes (likely to have unused methods)
echo "📝 Example/Demo Classes (may have intentionally unused code):"
echo "-------------------------------------------------------------"
find app/src/main/java -name "*Example*.java" -o -name "*Demo*.java" -o -name "*Sample*.java" | head -10
echo ""

# 2. Find test utility classes
echo "📝 Test Utility Classes:"
echo "------------------------"
find app/src/main/java -path "*/test/*" -name "*.java" | head -10
echo ""

# 3. Find configuration classes (fields may be used by Spring via reflection)
echo "📝 Configuration Classes (fields likely used by framework):"
echo "-----------------------------------------------------------"
find app/src/main/java -name "*Configuration*.java" -o -name "*Config*.java" | wc -l
echo "Total configuration files found"
echo ""

# 4. Find adapter classes (may have unused methods for interface compliance)
echo "📝 Adapter Classes (may have unused interface methods):"
echo "--------------------------------------------------------"
find app/src/main/java -name "*Adapter*.java" | head -10
echo ""

# 5. Summary
echo "📊 Summary:"
echo "-----------"
echo "Total Java files: $(find app/src/main/java -name "*.java" | wc -l)"
echo "Example/Demo files: $(find app/src/main/java -name "*Example*.java" -o -name "*Demo*.java" | wc -l)"
echo "Configuration files: $(find app/src/main/java -name "*Configuration*.java" -o -name "*Config*.java" | wc -l)"
echo "Adapter files: $(find app/src/main/java -name "*Adapter*.java" | wc -l)"
echo ""

echo "💡 Recommendation:"
echo "------------------"
echo "1. Example/Demo classes: Keep as-is (educational purpose)"
echo "2. Configuration classes: Keep fields (used by Spring framework)"
echo "3. Adapter classes: Review carefully (may implement unused interface methods)"
echo "4. Regular classes: Use IDE inspection to find truly unused code"
echo ""
echo "✅ For production-critical cleanup, use IntelliJ IDEA's 'Unused Declaration' inspection"
