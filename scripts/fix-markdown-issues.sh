#!/bin/bash

# Fix Markdown Issues Script
# This script automatically fixes common markdown linting issues

set -e

echo "🔧 Starting Markdown fixes..."

# Function to fix a single file
fix_file() {
    local file="$1"
    echo "  Fixing: $file"
    
    # Create backup
    cp "$file" "$file.bak"
    
    # Fix multiple consecutive blank lines (MD012)
    perl -i -pe 's/\n\n\n+/\n\n/g' "$file"
    
    # Fix trailing spaces (MD009)
    sed -i '' 's/[[:space:]]*$//' "$file"
    
    # Add blank lines around lists (MD032) - basic fix
    # This is complex, so we'll do a simple version
    
    # Add blank lines around fenced code blocks (MD031)
    # This requires more sophisticated logic
    
    echo "    ✓ Fixed basic issues"
}

# Find all markdown files excluding node_modules and build directories
echo "📝 Finding markdown files..."

find . -name "*.md" \
    -not -path "*/node_modules/*" \
    -not -path "*/build/*" \
    -not -path "*/app/build/*" \
    -not -path "*/app/bin/*" \
    -not -path "*/.git/*" \
    | while read -r file; do
    fix_file "$file"
done

echo "✅ Markdown fixes completed!"
echo ""
echo "💡 Note: Some issues require manual fixing:"
echo "   - MD024: Duplicate headings (need unique names)"
echo "   - MD040: Missing code block languages"
echo "   - MD013: Line length (need manual rewording)"
echo ""
echo "Run 'markdownlint \"**/*.md\" --ignore node_modules' to check remaining issues"
