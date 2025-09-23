#!/bin/bash

# Documentation Quality Check Script
# Comprehensive quality assurance for Viewpoints & Perspectives documentation
# Implements requirement 6: 文件維護的自動化

set -e

echo "📋 Documentation Quality Check Starting..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Counters
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0
WARNING_CHECKS=0

# Configuration
DOCS_DIR="docs"
EN_DOCS_DIR="docs/en"
SCRIPTS_DIR="scripts"
REPORTS_DIR="build/reports/documentation-quality"

# Create reports directory
mkdir -p "$REPORTS_DIR"

# Function to log results
log_result() {
    local status=$1
    local message=$2
    local details=${3:-""}
    
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    
    case $status in
        "PASS")
            echo -e "${GREEN}✅ $message${NC}"
            PASSED_CHECKS=$((PASSED_CHECKS + 1))
            ;;
        "FAIL")
            echo -e "${RED}❌ $message${NC}"
            if [[ -n "$details" ]]; then
                echo -e "${RED}   Details: $details${NC}"
            fi
            FAILED_CHECKS=$((FAILED_CHECKS + 1))
            ;;
        "WARN")
            echo -e "${YELLOW}⚠️  $message${NC}"
            if [[ -n "$details" ]]; then
                echo -e "${YELLOW}   Details: $details${NC}"
            fi
            WARNING_CHECKS=$((WARNING_CHECKS + 1))
            ;;
        "INFO")
            echo -e "${BLUE}ℹ️  $message${NC}"
            ;;
    esac
}

# Function to check Markdown syntax using markdownlint
check_markdown_syntax() {
    echo -e "${BLUE}📝 Checking Markdown syntax...${NC}"
    
    # Check if markdownlint is available
    if ! command -v markdownlint &> /dev/null; then
        log_result "WARN" "markdownlint not found, installing via npm..."
        npm install -g markdownlint-cli 2>/dev/null || {
            log_result "FAIL" "Failed to install markdownlint"
            return 1
        }
    fi
    
    # Create temporary report file
    local markdown_report="$REPORTS_DIR/markdown-lint-report.txt"
    
    # Run markdownlint on all markdown files
    local markdown_files=$(find . -name "*.md" -not -path "./node_modules/*" -not -path "./.git/*")
    local total_files=$(echo "$markdown_files" | wc -l)
    local failed_files=0
    
    echo "Checking $total_files markdown files..." > "$markdown_report"
    echo "===========================================" >> "$markdown_report"
    
    while IFS= read -r file; do
        if [[ -n "$file" ]]; then
            if markdownlint "$file" >> "$markdown_report" 2>&1; then
                echo "✅ $file" >> "$markdown_report"
            else
                echo "❌ $file" >> "$markdown_report"
                failed_files=$((failed_files + 1))
            fi
        fi
    done <<< "$markdown_files"
    
    if [[ $failed_files -eq 0 ]]; then
        log_result "PASS" "All markdown files pass syntax check"
    else
        log_result "FAIL" "$failed_files markdown files have syntax issues" "See $markdown_report"
    fi
    
    echo "Markdown syntax check report saved to: $markdown_report"
}

# Function to check link validity
check_link_validity() {
    echo -e "${BLUE}🔗 Checking link validity...${NC}"
    
    local link_report="$REPORTS_DIR/link-check-report.txt"
    local broken_links=0
    local total_links=0
    
    echo "Link Validity Check Report" > "$link_report"
    echo "=========================" >> "$link_report"
    echo "Generated: $(date)" >> "$link_report"
    echo "" >> "$link_report"
    
    # Find all markdown files
    while IFS= read -r -d '' file; do
        if [[ -f "$file" ]]; then
            echo "Checking links in: $file" >> "$link_report"
            
            # Extract markdown links [text](url)
            grep -n -oE '\[([^\]]+)\]\(([^)]+)\)' "$file" 2>/dev/null | while IFS=: read -r line_num link; do
                total_links=$((total_links + 1))
                
                # Extract URL from link
                url=$(echo "$link" | sed -n 's/.*](\([^)]*\)).*/\1/p')
                
                # Skip external URLs (http/https) for now - focus on internal links
                if [[ "$url" =~ ^https?:// ]]; then
                    echo "  Line $line_num: $url (external - skipped)" >> "$link_report"
                    continue
                fi
                
                # Check internal links
                if [[ "$url" =~ ^[^#] ]]; then
                    # Resolve relative path
                    link_dir=$(dirname "$file")
                    
                    # Handle different link formats
                    if [[ "$url" =~ ^/ ]]; then
                        # Absolute path from root
                        target_file=".$url"
                    else
                        # Relative path
                        target_file=$(realpath -m "$link_dir/$url" 2>/dev/null || echo "$link_dir/$url")
                    fi
                    
                    # Remove anchor if present
                    target_file_clean=$(echo "$target_file" | cut -d'#' -f1)
                    
                    if [[ -f "$target_file_clean" ]]; then
                        echo "  Line $line_num: $url ✅" >> "$link_report"
                    else
                        echo "  Line $line_num: $url ❌ (target not found: $target_file_clean)" >> "$link_report"
                        broken_links=$((broken_links + 1))
                    fi
                else
                    echo "  Line $line_num: $url (anchor only)" >> "$link_report"
                fi
            done
        fi
    done < <(find . -name "*.md" -not -path "./node_modules/*" -not -path "./.git/*" -print0)
    
    echo "" >> "$link_report"
    echo "Summary:" >> "$link_report"
    echo "Total links checked: $total_links" >> "$link_report"
    echo "Broken links found: $broken_links" >> "$link_report"
    
    if [[ $broken_links -eq 0 ]]; then
        log_result "PASS" "All internal links are valid"
    else
        log_result "FAIL" "$broken_links broken internal links found" "See $link_report"
    fi
    
    echo "Link validity report saved to: $link_report"
}

# Function to test diagram rendering
check_diagram_rendering() {
    echo -e "${BLUE}📊 Checking diagram rendering...${NC}"
    
    local diagram_report="$REPORTS_DIR/diagram-check-report.txt"
    local failed_diagrams=0
    local total_diagrams=0
    
    echo "Diagram Rendering Check Report" > "$diagram_report"
    echo "=============================" >> "$diagram_report"
    echo "Generated: $(date)" >> "$diagram_report"
    echo "" >> "$diagram_report"
    
    # Check Mermaid diagrams
    echo "Checking Mermaid diagrams (.mmd files):" >> "$diagram_report"
    while IFS= read -r -d '' mermaid_file; do
        if [[ -f "$mermaid_file" ]]; then
            total_diagrams=$((total_diagrams + 1))
            echo "Checking: $mermaid_file" >> "$diagram_report"
            
            # Basic syntax check for Mermaid
            if grep -q -E "^(graph|flowchart|sequenceDiagram|classDiagram|stateDiagram|erDiagram|journey|gitgraph)" "$mermaid_file"; then
                echo "  ✅ Valid Mermaid syntax detected" >> "$diagram_report"
            else
                echo "  ❌ No valid Mermaid diagram type found" >> "$diagram_report"
                failed_diagrams=$((failed_diagrams + 1))
            fi
            
            # Check for common syntax errors
            if grep -q -E "^\s*$" "$mermaid_file" && [[ $(wc -l < "$mermaid_file") -gt 1 ]]; then
                echo "  ⚠️  Contains empty lines (may cause rendering issues)" >> "$diagram_report"
            fi
        fi
    done < <(find . -name "*.mmd" -not -path "./node_modules/*" -not -path "./.git/*" -print0)
    
    echo "" >> "$diagram_report"
    
    # Check PlantUML diagrams
    echo "Checking PlantUML diagrams (.puml files):" >> "$diagram_report"
    while IFS= read -r -d '' plantuml_file; do
        if [[ -f "$plantuml_file" ]]; then
            total_diagrams=$((total_diagrams + 1))
            echo "Checking: $plantuml_file" >> "$diagram_report"
            
            # Basic syntax check for PlantUML
            if grep -q "@startuml" "$plantuml_file" && grep -q "@enduml" "$plantuml_file"; then
                echo "  ✅ Valid PlantUML structure (@startuml...@enduml)" >> "$diagram_report"
            else
                echo "  ❌ Missing @startuml or @enduml tags" >> "$diagram_report"
                failed_diagrams=$((failed_diagrams + 1))
            fi
            
            # Check for balanced tags
            local start_tags=$(grep -c "@startuml" "$plantuml_file")
            local end_tags=$(grep -c "@enduml" "$plantuml_file")
            
            if [[ $start_tags -ne $end_tags ]]; then
                echo "  ❌ Unbalanced @startuml ($start_tags) and @enduml ($end_tags) tags" >> "$diagram_report"
                failed_diagrams=$((failed_diagrams + 1))
            fi
        fi
    done < <(find . -name "*.puml" -not -path "./node_modules/*" -not -path "./.git/*" -print0)
    
    echo "" >> "$diagram_report"
    echo "Summary:" >> "$diagram_report"
    echo "Total diagrams checked: $total_diagrams" >> "$diagram_report"
    echo "Failed diagrams: $failed_diagrams" >> "$diagram_report"
    
    if [[ $failed_diagrams -eq 0 ]]; then
        log_result "PASS" "All diagrams pass basic syntax check"
    else
        log_result "FAIL" "$failed_diagrams diagrams have syntax issues" "See $diagram_report"
    fi
    
    echo "Diagram check report saved to: $diagram_report"
}

# Function to check translation synchronization
check_translation_sync() {
    echo -e "${BLUE}🌐 Checking translation synchronization...${NC}"
    
    # Use existing translation quality check script
    if [[ -f "$SCRIPTS_DIR/check-translation-quality.sh" ]]; then
        log_result "INFO" "Running translation quality check..."
        
        if bash "$SCRIPTS_DIR/check-translation-quality.sh" > "$REPORTS_DIR/translation-sync-report.txt" 2>&1; then
            log_result "PASS" "Translation synchronization check passed"
        else
            log_result "FAIL" "Translation synchronization issues found" "See $REPORTS_DIR/translation-sync-report.txt"
        fi
    else
        log_result "WARN" "Translation quality check script not found"
    fi
}

# Function to validate document metadata
check_document_metadata() {
    echo -e "${BLUE}📋 Checking document metadata...${NC}"
    
    local metadata_report="$REPORTS_DIR/metadata-check-report.txt"
    local missing_metadata=0
    local total_docs=0
    
    echo "Document Metadata Check Report" > "$metadata_report"
    echo "==============================" >> "$metadata_report"
    echo "Generated: $(date)" >> "$metadata_report"
    echo "" >> "$metadata_report"
    
    # Check for Front Matter in key documentation files
    local key_dirs=("docs/viewpoints" "docs/perspectives" "docs/templates")
    
    for dir in "${key_dirs[@]}"; do
        if [[ -d "$dir" ]]; then
            echo "Checking metadata in: $dir" >> "$metadata_report"
            
            while IFS= read -r -d '' doc_file; do
                if [[ -f "$doc_file" ]]; then
                    total_docs=$((total_docs + 1))
                    echo "  Checking: $doc_file" >> "$metadata_report"
                    
                    # Check for YAML front matter
                    if head -n 1 "$doc_file" | grep -q "^---$"; then
                        # Has front matter, check for required fields
                        local has_title=$(sed -n '2,/^---$/p' "$doc_file" | grep -q "^title:" && echo "yes" || echo "no")
                        local has_description=$(sed -n '2,/^---$/p' "$doc_file" | grep -q "^description:" && echo "yes" || echo "no")
                        
                        if [[ "$has_title" == "yes" && "$has_description" == "yes" ]]; then
                            echo "    ✅ Has required metadata (title, description)" >> "$metadata_report"
                        else
                            echo "    ⚠️  Missing some metadata fields" >> "$metadata_report"
                            [[ "$has_title" == "no" ]] && echo "      - Missing: title" >> "$metadata_report"
                            [[ "$has_description" == "no" ]] && echo "      - Missing: description" >> "$metadata_report"
                        fi
                    else
                        echo "    ❌ No YAML front matter found" >> "$metadata_report"
                        missing_metadata=$((missing_metadata + 1))
                    fi
                fi
            done < <(find "$dir" -name "*.md" -print0)
            
            echo "" >> "$metadata_report"
        fi
    done
    
    echo "Summary:" >> "$metadata_report"
    echo "Total documents checked: $total_docs" >> "$metadata_report"
    echo "Documents missing metadata: $missing_metadata" >> "$metadata_report"
    
    if [[ $missing_metadata -eq 0 ]]; then
        log_result "PASS" "All key documents have proper metadata"
    elif [[ $missing_metadata -lt 5 ]]; then
        log_result "WARN" "$missing_metadata documents missing metadata" "See $metadata_report"
    else
        log_result "FAIL" "$missing_metadata documents missing metadata" "See $metadata_report"
    fi
    
    echo "Metadata check report saved to: $metadata_report"
}

# Function to check documentation structure consistency
check_structure_consistency() {
    echo -e "${BLUE}🏗️  Checking documentation structure consistency...${NC}"
    
    local structure_report="$REPORTS_DIR/structure-check-report.txt"
    local structure_issues=0
    
    echo "Documentation Structure Check Report" > "$structure_report"
    echo "====================================" >> "$structure_report"
    echo "Generated: $(date)" >> "$structure_report"
    echo "" >> "$structure_report"
    
    # Check Viewpoints structure
    echo "Checking Viewpoints structure:" >> "$structure_report"
    local expected_viewpoints=("functional" "information" "concurrency" "development" "deployment" "operational")
    
    for viewpoint in "${expected_viewpoints[@]}"; do
        local chinese_dir="docs/viewpoints/$viewpoint"
        local english_dir="docs/en/viewpoints/$viewpoint"
        
        if [[ -d "$chinese_dir" ]]; then
            echo "  ✅ Chinese viewpoint exists: $viewpoint" >> "$structure_report"
            
            if [[ -d "$english_dir" ]]; then
                echo "  ✅ English viewpoint exists: $viewpoint" >> "$structure_report"
            else
                echo "  ❌ Missing English viewpoint: $viewpoint" >> "$structure_report"
                structure_issues=$((structure_issues + 1))
            fi
        else
            echo "  ❌ Missing Chinese viewpoint: $viewpoint" >> "$structure_report"
            structure_issues=$((structure_issues + 1))
        fi
    done
    
    echo "" >> "$structure_report"
    
    # Check Perspectives structure
    echo "Checking Perspectives structure:" >> "$structure_report"
    local expected_perspectives=("security" "performance" "availability" "evolution" "usability" "regulation" "location" "cost")
    
    for perspective in "${expected_perspectives[@]}"; do
        local chinese_dir="docs/perspectives/$perspective"
        local english_dir="docs/en/perspectives/$perspective"
        
        if [[ -d "$chinese_dir" ]]; then
            echo "  ✅ Chinese perspective exists: $perspective" >> "$structure_report"
            
            if [[ -d "$english_dir" ]]; then
                echo "  ✅ English perspective exists: $perspective" >> "$structure_report"
            else
                echo "  ❌ Missing English perspective: $perspective" >> "$structure_report"
                structure_issues=$((structure_issues + 1))
            fi
        else
            echo "  ❌ Missing Chinese perspective: $perspective" >> "$structure_report"
            structure_issues=$((structure_issues + 1))
        fi
    done
    
    echo "" >> "$structure_report"
    echo "Summary:" >> "$structure_report"
    echo "Structure issues found: $structure_issues" >> "$structure_report"
    
    if [[ $structure_issues -eq 0 ]]; then
        log_result "PASS" "Documentation structure is consistent"
    else
        log_result "FAIL" "$structure_issues structure consistency issues found" "See $structure_report"
    fi
    
    echo "Structure check report saved to: $structure_report"
}

# Function to generate comprehensive quality report
generate_quality_report() {
    echo -e "${BLUE}📊 Generating comprehensive quality report...${NC}"
    
    local final_report="$REPORTS_DIR/documentation-quality-summary.md"
    
    cat > "$final_report" << EOF
# Documentation Quality Report

**Generated:** $(date)  
**Project:** Viewpoints & Perspectives Documentation Structure  
**Version:** 2.0

## Executive Summary

- **Total Checks:** $TOTAL_CHECKS
- **Passed:** $PASSED_CHECKS
- **Failed:** $FAILED_CHECKS
- **Warnings:** $WARNING_CHECKS

**Overall Status:** $(if [[ $FAILED_CHECKS -eq 0 ]]; then echo "✅ PASSED"; else echo "❌ FAILED"; fi)

## Quality Metrics

| Check Category | Status | Details |
|----------------|--------|---------|
| Markdown Syntax | $(if [[ -f "$REPORTS_DIR/markdown-lint-report.txt" ]]; then echo "📝 Checked"; else echo "⚠️ Skipped"; fi) | See markdown-lint-report.txt |
| Link Validity | $(if [[ -f "$REPORTS_DIR/link-check-report.txt" ]]; then echo "🔗 Checked"; else echo "⚠️ Skipped"; fi) | See link-check-report.txt |
| Diagram Rendering | $(if [[ -f "$REPORTS_DIR/diagram-check-report.txt" ]]; then echo "📊 Checked"; else echo "⚠️ Skipped"; fi) | See diagram-check-report.txt |
| Translation Sync | $(if [[ -f "$REPORTS_DIR/translation-sync-report.txt" ]]; then echo "🌐 Checked"; else echo "⚠️ Skipped"; fi) | See translation-sync-report.txt |
| Document Metadata | $(if [[ -f "$REPORTS_DIR/metadata-check-report.txt" ]]; then echo "📋 Checked"; else echo "⚠️ Skipped"; fi) | See metadata-check-report.txt |
| Structure Consistency | $(if [[ -f "$REPORTS_DIR/structure-check-report.txt" ]]; then echo "🏗️ Checked"; else echo "⚠️ Skipped"; fi) | See structure-check-report.txt |

## Recommendations

EOF

    # Add recommendations based on results
    if [[ $FAILED_CHECKS -gt 0 ]]; then
        echo "### Critical Issues to Address" >> "$final_report"
        echo "" >> "$final_report"
        echo "1. Review failed checks in individual reports" >> "$final_report"
        echo "2. Fix broken links and missing files" >> "$final_report"
        echo "3. Correct markdown syntax errors" >> "$final_report"
        echo "4. Update missing translations" >> "$final_report"
        echo "" >> "$final_report"
    fi
    
    if [[ $WARNING_CHECKS -gt 0 ]]; then
        echo "### Improvements Suggested" >> "$final_report"
        echo "" >> "$final_report"
        echo "1. Add missing document metadata" >> "$final_report"
        echo "2. Improve diagram documentation" >> "$final_report"
        echo "3. Enhance translation consistency" >> "$final_report"
        echo "" >> "$final_report"
    fi
    
    echo "### Next Steps" >> "$final_report"
    echo "" >> "$final_report"
    echo "1. Address all failed checks before proceeding" >> "$final_report"
    echo "2. Set up automated quality checks in CI/CD pipeline" >> "$final_report"
    echo "3. Schedule regular quality reviews" >> "$final_report"
    echo "4. Update documentation standards based on findings" >> "$final_report"
    
    echo "Comprehensive quality report saved to: $final_report"
    log_result "INFO" "Quality report generated successfully"
}

# Main execution function
main() {
    echo -e "${PURPLE}🚀 Starting comprehensive documentation quality check...${NC}"
    echo ""
    
    # Create reports directory
    mkdir -p "$REPORTS_DIR"
    
    # Run all quality checks
    check_markdown_syntax
    echo ""
    
    check_link_validity
    echo ""
    
    check_diagram_rendering
    echo ""
    
    check_translation_sync
    echo ""
    
    check_document_metadata
    echo ""
    
    check_structure_consistency
    echo ""
    
    # Generate final report
    generate_quality_report
    echo ""
    
    # Final summary
    echo -e "${PURPLE}📊 Documentation Quality Check Complete${NC}"
    echo "========================================"
    echo -e "Total Checks: ${BLUE}$TOTAL_CHECKS${NC}"
    echo -e "Passed: ${GREEN}$PASSED_CHECKS${NC}"
    echo -e "Failed: ${RED}$FAILED_CHECKS${NC}"
    echo -e "Warnings: ${YELLOW}$WARNING_CHECKS${NC}"
    echo ""
    echo -e "Reports saved to: ${BLUE}$REPORTS_DIR${NC}"
    echo ""
    
    # Exit with appropriate code
    if [[ $FAILED_CHECKS -eq 0 ]]; then
        echo -e "${GREEN}🎉 All documentation quality checks passed!${NC}"
        exit 0
    else
        echo -e "${RED}❌ $FAILED_CHECKS quality checks failed${NC}"
        echo "Please review the reports and fix the issues before proceeding."
        exit 1
    fi
}

# Run main function
main "$@"