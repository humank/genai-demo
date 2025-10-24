#!/bin/bash

# Validate all markdown links in documentation
# Checks both internal and external links

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "🔗 Validating markdown links in documentation..."
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

total_files=0
total_links=0
broken_links=0
valid_links=0

# Function to check if a file exists relative to the markdown file
check_relative_link() {
    local md_file="$1"
    local link="$2"
    local md_dir="$(dirname "$md_file")"
    
    # Remove anchor if present
    local file_path="${link%%#*}"
    
    # Skip external links
    if [[ $file_path =~ ^https?:// ]]; then
        return 0
    fi
    
    # Skip mailto links
    if [[ $file_path =~ ^mailto: ]]; then
        return 0
    fi
    
    # Resolve relative path
    local full_path="$md_dir/$file_path"
    
    # Check if file exists
    if [[ -e "$full_path" ]]; then
        return 0
    else
        return 1
    fi
}

# Function to extract markdown links from a file
extract_links() {
    local file="$1"
    
    # Extract [text](link) style links
    grep -oE '\[([^\]]+)\]\(([^)]+)\)' "$file" | sed -E 's/\[([^\]]+)\]\(([^)]+)\)/\2/' || true
}

# Find all markdown files
while IFS= read -r md_file; do
    total_files=$((total_files + 1))
    
    echo -e "${BLUE}Checking:${NC} $md_file"
    
    # Extract links from file
    while IFS= read -r link; do
        if [[ -n "$link" ]]; then
            total_links=$((total_links + 1))
            
            if check_relative_link "$md_file" "$link"; then
                valid_links=$((valid_links + 1))
                echo -e "  ${GREEN}✓${NC} $link"
            else
                broken_links=$((broken_links + 1))
                echo -e "  ${RED}✗${NC} $link - BROKEN"
            fi
        fi
    done < <(extract_links "$md_file")
    
    echo ""
done < <(find "$PROJECT_ROOT/.kiro" "$PROJECT_ROOT/docs" -name "*.md" -type f 2>/dev/null || true)

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Link Validation Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Files checked: $total_files"
echo "Total links: $total_links"
echo -e "${GREEN}Valid links: $valid_links${NC}"
echo -e "${RED}Broken links: $broken_links${NC}"
echo ""

if [ $broken_links -gt 0 ]; then
    echo -e "${RED}❌ Found broken links!${NC}"
    echo "Please fix the broken links before proceeding."
    echo ""
    exit 1
else
    echo -e "${GREEN}✅ All links are valid!${NC}"
    echo ""
    exit 0
fi
