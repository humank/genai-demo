#!/bin/bash

# Check for documentation drift
# Detects when code changes are not accompanied by documentation updates

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KIRO_DIR="$(dirname "$SCRIPT_DIR")"
REPORT_DIR="$KIRO_DIR/reports/doc-drift"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}📊 Documentation Drift Detection${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Create report directory
mkdir -p "$REPORT_DIR"

# Configuration
DAYS_THRESHOLD=${1:-30}  # Default: check last 30 days
BASE_BRANCH=${2:-main}   # Default: compare with main branch

# Create detailed report
REPORT_FILE="$REPORT_DIR/drift-detection-$(date +%Y%m%d-%H%M%S).md"
echo "# Documentation Drift Detection Report" > "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "**Date**: $(date)" >> "$REPORT_FILE"
echo "**Period**: Last $DAYS_THRESHOLD days" >> "$REPORT_FILE"
echo "**Base Branch**: $BASE_BRANCH" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Initialize counters
total_code_changes=0
total_doc_changes=0
drift_detected=0

# Function to check if a file is documentation
is_documentation_file() {
    local file="$1"
    
    # Documentation file patterns
    if [[ $file == *.md ]] || \
       [[ $file == docs/* ]] || \
       [[ $file == .kiro/steering/* ]] || \
       [[ $file == .kiro/examples/* ]] || \
       [[ $file == README* ]] || \
       [[ $file == *.puml ]] || \
       [[ $file == *.mmd ]]; then
        return 0
    fi
    
    return 1
}

# Function to check if a file is source code
is_source_code_file() {
    local file="$1"
    
    # Source code file patterns
    if [[ $file == *.java ]] || \
       [[ $file == *.ts ]] || \
       [[ $file == *.tsx ]] || \
       [[ $file == *.js ]] || \
       [[ $file == *.jsx ]] || \
       [[ $file == *.py ]] || \
       [[ $file == *.go ]] || \
       [[ $file == *.rs ]] || \
       [[ $file == *.kt ]] || \
       [[ $file == *.scala ]]; then
        return 0
    fi
    
    return 1
}

# Function to get related documentation files for a source file
get_related_docs() {
    local source_file="$1"
    local related_docs=()
    
    # Extract package/module name
    local package_name=""
    if [[ $source_file == *"/domain/"* ]]; then
        package_name=$(echo "$source_file" | sed -n 's|.*/domain/\([^/]*\)/.*|\1|p')
    elif [[ $source_file == *"/application/"* ]]; then
        package_name=$(echo "$source_file" | sed -n 's|.*/application/\([^/]*\)/.*|\1|p')
    fi
    
    # Look for related documentation
    if [ -n "$package_name" ]; then
        # Check viewpoint documentation
        if [ -f "docs/viewpoints/functional/${package_name}.md" ]; then
            related_docs+=("docs/viewpoints/functional/${package_name}.md")
        fi
        
        # Check API documentation
        if [ -f "docs/api/rest/endpoints/${package_name}.md" ]; then
            related_docs+=("docs/api/rest/endpoints/${package_name}.md")
        fi
        
        # Check event documentation
        if [ -f "docs/api/events/contexts/${package_name}-events.md" ]; then
            related_docs+=("docs/api/events/contexts/${package_name}-events.md")
        fi
    fi
    
    # Check for README in same directory
    local dir=$(dirname "$source_file")
    if [ -f "$dir/README.md" ]; then
        related_docs+=("$dir/README.md")
    fi
    
    echo "${related_docs[@]}"
}

# Get list of changed files in the specified period
echo -e "${BLUE}🔍 Analyzing changes in last $DAYS_THRESHOLD days...${NC}"
echo ""

# Get date threshold
DATE_THRESHOLD=$(date -d "$DAYS_THRESHOLD days ago" +%Y-%m-%d 2>/dev/null || date -v-${DAYS_THRESHOLD}d +%Y-%m-%d)

# Get changed files
CHANGED_FILES=$(git log --since="$DATE_THRESHOLD" --name-only --pretty=format: | sort -u | grep -v '^$')

if [ -z "$CHANGED_FILES" ]; then
    echo -e "${YELLOW}⚠️  No changes found in the last $DAYS_THRESHOLD days${NC}"
    echo "" >> "$REPORT_FILE"
    echo "## Summary" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "No changes detected in the specified period." >> "$REPORT_FILE"
    exit 0
fi

# Analyze changes
echo "## Changed Files Analysis" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

declare -A code_changes_by_context
declare -A doc_changes_by_context

while IFS= read -r file; do
    if [ -z "$file" ]; then
        continue
    fi
    
    if is_source_code_file "$file"; then
        total_code_changes=$((total_code_changes + 1))
        
        # Extract context
        context="unknown"
        if [[ $file == *"/domain/"* ]]; then
            context=$(echo "$file" | sed -n 's|.*/domain/\([^/]*\)/.*|\1|p')
        elif [[ $file == *"/application/"* ]]; then
            context=$(echo "$file" | sed -n 's|.*/application/\([^/]*\)/.*|\1|p')
        fi
        
        code_changes_by_context[$context]=$((${code_changes_by_context[$context]:-0} + 1))
        
        echo -e "${BLUE}📝 Code change: $file${NC}"
        
        # Check for related documentation changes
        related_docs=$(get_related_docs "$file")
        
        if [ -n "$related_docs" ]; then
            doc_updated=false
            for doc in $related_docs; do
                # Check if this doc was also changed
                if echo "$CHANGED_FILES" | grep -q "^$doc$"; then
                    doc_updated=true
                    echo -e "${GREEN}  ✅ Related doc updated: $doc${NC}"
                fi
            done
            
            if [ "$doc_updated" = false ]; then
                drift_detected=$((drift_detected + 1))
                echo -e "${RED}  ❌ No related doc updates found${NC}"
                echo -e "${YELLOW}     Suggested docs to update:${NC}"
                for doc in $related_docs; do
                    echo -e "${YELLOW}     - $doc${NC}"
                done
                
                # Add to report
                echo "### ❌ Drift Detected: $file" >> "$REPORT_FILE"
                echo "" >> "$REPORT_FILE"
                echo "**Context**: $context" >> "$REPORT_FILE"
                echo "**Code Changed**: $file" >> "$REPORT_FILE"
                echo "**Related Docs Not Updated**:" >> "$REPORT_FILE"
                for doc in $related_docs; do
                    echo "- $doc" >> "$REPORT_FILE"
                done
                echo "" >> "$REPORT_FILE"
            fi
        else
            echo -e "${YELLOW}  ⚠️  No related documentation found${NC}"
        fi
        
    elif is_documentation_file "$file"; then
        total_doc_changes=$((total_doc_changes + 1))
        
        # Extract context from doc path
        context="general"
        if [[ $file == *"/functional/"* ]]; then
            context=$(basename "$file" .md)
        elif [[ $file == *"/endpoints/"* ]]; then
            context=$(basename "$file" .md)
        elif [[ $file == *"/events/"* ]]; then
            context=$(basename "$file" -events.md)
        fi
        
        doc_changes_by_context[$context]=$((${doc_changes_by_context[$context]:-0} + 1))
        
        echo -e "${GREEN}📄 Doc change: $file${NC}"
    fi
done <<< "$CHANGED_FILES"

# Generate summary
echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "## Summary" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "- **Total Code Changes**: $total_code_changes" >> "$REPORT_FILE"
echo "- **Total Documentation Changes**: $total_doc_changes" >> "$REPORT_FILE"
echo "- **Drift Detected**: $drift_detected cases" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

if [ $drift_detected -eq 0 ]; then
    echo "**Status**: ✅ NO DRIFT DETECTED" >> "$REPORT_FILE"
else
    echo "**Status**: ❌ DRIFT DETECTED" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "### Recommendations" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "1. Review the code changes listed above" >> "$REPORT_FILE"
    echo "2. Update the related documentation files" >> "$REPORT_FILE"
    echo "3. Ensure API documentation reflects any interface changes" >> "$REPORT_FILE"
    echo "4. Update architecture diagrams if structure changed" >> "$REPORT_FILE"
    echo "5. Add or update ADRs for significant decisions" >> "$REPORT_FILE"
fi

# Context-based analysis
echo "" >> "$REPORT_FILE"
echo "## Changes by Context" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| Context | Code Changes | Doc Changes | Ratio |" >> "$REPORT_FILE"
echo "|---------|--------------|-------------|-------|" >> "$REPORT_FILE"

for context in "${!code_changes_by_context[@]}"; do
    code_count=${code_changes_by_context[$context]}
    doc_count=${doc_changes_by_context[$context]:-0}
    
    if [ $code_count -gt 0 ]; then
        ratio=$(echo "scale=2; $doc_count / $code_count" | bc)
    else
        ratio="N/A"
    fi
    
    echo "| $context | $code_count | $doc_count | $ratio |" >> "$REPORT_FILE"
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${BLUE}📊 Documentation Drift Summary${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Period: Last $DAYS_THRESHOLD days"
echo "Total code changes: $total_code_changes"
echo "Total documentation changes: $total_doc_changes"
echo -e "${RED}Drift detected: $drift_detected cases${NC}"
echo ""
echo -e "${BLUE}📋 Detailed report: $REPORT_FILE${NC}"
echo ""

if [ $drift_detected -eq 0 ]; then
    echo -e "${GREEN}✅ No documentation drift detected!${NC}"
    echo -e "${GREEN}   Code changes are accompanied by documentation updates.${NC}"
    exit 0
else
    echo -e "${RED}❌ Documentation drift detected in $drift_detected cases${NC}"
    echo -e "${YELLOW}💡 Recommendations:${NC}"
    echo "   1. Review the code changes and update related documentation"
    echo "   2. Ensure API documentation reflects interface changes"
    echo "   3. Update architecture diagrams if needed"
    echo "   4. Add ADRs for significant architectural decisions"
    echo ""
    echo -e "${YELLOW}⚠️  Consider setting up a pre-commit hook to remind developers${NC}"
    echo -e "${YELLOW}   to update documentation when making code changes.${NC}"
    exit 1
fi
