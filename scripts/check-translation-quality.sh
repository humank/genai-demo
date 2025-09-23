#!/bin/bash

# Translation Quality Check Script for Viewpoints & Perspectives Structure
# Verifies consistency and completeness of Chinese-English documentation pairs
# Supports new Rozanski & Woods Viewpoints & Perspectives documentation structure

set -e

echo "🔍 Translation Quality Check Starting..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
TOTAL_FILES=0
MISSING_TRANSLATIONS=0
INCONSISTENT_TERMS=0
BROKEN_LINKS=0

# Load terminology dictionary
TERMINOLOGY_FILE="docs/.terminology.json"
if [[ ! -f "$TERMINOLOGY_FILE" ]]; then
    echo -e "${RED}❌ Terminology dictionary not found: $TERMINOLOGY_FILE${NC}"
    exit 1
fi

echo -e "${BLUE}📚 Using terminology dictionary: $TERMINOLOGY_FILE${NC}"

# Function to check if Chinese file has corresponding English translation
check_translation_completeness() {
    echo -e "${BLUE}📋 Checking translation completeness...${NC}"
    
    # Find all Chinese markdown files (excluding English directory)
    while IFS= read -r -d '' chinese_file; do
        TOTAL_FILES=$((TOTAL_FILES + 1))
        
        # Determine expected English file path
        if [[ "$chinese_file" == "README.md" ]]; then
            english_file="docs/en/PROJECT_README.md"
        else
            # Remove docs/ prefix and add docs/en/ prefix
            relative_path="${chinese_file#docs/}"
            english_file="docs/en/$relative_path"
        fi
        
        # Check if English translation exists
        if [[ ! -f "$english_file" ]]; then
            echo -e "${YELLOW}⚠️  Missing translation: $chinese_file → $english_file${NC}"
            MISSING_TRANSLATIONS=$((MISSING_TRANSLATIONS + 1))
        else
            echo -e "${GREEN}✅ Translation exists: $chinese_file → $english_file${NC}"
        fi
        
    done < <(find . -name "*.md" -not -path "./docs/en/*" -not -path "./node_modules/*" -not -path "./.git/*" -not -path "./.kiro/*" -print0)
}

# Function to check terminology consistency
check_terminology_consistency() {
    echo -e "${BLUE}📖 Checking terminology consistency...${NC}"
    
    # Extract key terms from terminology dictionary - Enhanced for new structure
    local terms=(
        # Rozanski & Woods Viewpoints
        "架構視點:Architectural Viewpoint"
        "功能視點:Functional Viewpoint"
        "資訊視點:Information Viewpoint"
        "並發視點:Concurrency Viewpoint"
        "開發視點:Development Viewpoint"
        "部署視點:Deployment Viewpoint"
        "運營視點:Operational Viewpoint"
        "上下文視點:Context Viewpoint"
        # Rozanski & Woods Perspectives
        "架構觀點:Architectural Perspective"
        "安全性觀點:Security Perspective"
        "性能觀點:Performance & Scalability Perspective"
        "性能與可擴展性觀點:Performance & Scalability Perspective"
        "可用性觀點:Availability & Resilience Perspective"
        "可用性與韌性觀點:Availability & Resilience Perspective"
        "演進性觀點:Evolution Perspective"
        "使用性觀點:Usability Perspective"
        "法規觀點:Regulation Perspective"
        "位置觀點:Location Perspective"
        "成本觀點:Cost Perspective"
        # DDD & Architecture
        "領域驅動設計:Domain-Driven Design"
        "六角形架構:Hexagonal Architecture"
        "六角架構:Hexagonal Architecture"
        "聚合根:Aggregate Root"
        "值對象:Value Object"
        "領域事件:Domain Event"
        "界限上下文:Bounded Context"
        "事件風暴:Event Storming"
        "基礎設施即程式碼:Infrastructure as Code"
        # Stakeholder terminology
        "利害關係人:Stakeholder"
        "主要關注者:Primary Stakeholder"
        "次要關注者:Secondary Stakeholder"
        "架構師:Architect"
        "開發者:Developer"
        # Design strategies
        "設計策略:Design Strategy"
        "架構元素:Architectural Element"
        "關注點:Concern"
        "品質屬性:Quality Attribute"
        "跨視點應用:Cross-Viewpoint Application"
        "實現技術:Implementation Technique"
        "測試和驗證:Testing and Verification"
        "監控和度量:Monitoring and Measurement"
    )
    
    # Check each term pair
    for term_pair in "${terms[@]}"; do
        IFS=':' read -r chinese_term english_term <<< "$term_pair"
        
        # Find Chinese files containing the term
        chinese_files=$(grep -l "$chinese_term" docs/**/*.md 2>/dev/null | grep -v "docs/en/" || true)
        
        if [[ -n "$chinese_files" ]]; then
            # Check corresponding English files for correct translation
            while IFS= read -r chinese_file; do
                if [[ "$chinese_file" == "README.md" ]]; then
                    english_file="docs/en/PROJECT_README.md"
                else
                    relative_path="${chinese_file#docs/}"
                    english_file="docs/en/$relative_path"
                fi
                
                if [[ -f "$english_file" ]]; then
                    if ! grep -q "$english_term" "$english_file"; then
                        echo -e "${YELLOW}⚠️  Inconsistent terminology in $english_file: '$chinese_term' should be '$english_term'${NC}"
                        INCONSISTENT_TERMS=$((INCONSISTENT_TERMS + 1))
                    fi
                fi
            done <<< "$chinese_files"
        fi
    done
}

# Function to check internal links
check_internal_links() {
    echo -e "${BLUE}🔗 Checking internal links...${NC}"
    
    # Find all English markdown files
    while IFS= read -r -d '' english_file; do
        # Extract markdown links
        links=$(grep -oE '\[([^\]]+)\]\(([^)]+)\)' "$english_file" 2>/dev/null || true)
        
        if [[ -n "$links" ]]; then
            while IFS= read -r link; do
                # Extract URL from markdown link
                url=$(echo "$link" | sed -n 's/.*](\([^)]*\)).*/\1/p')
                
                # Check if it's an internal link (relative path)
                if [[ "$url" =~ ^[^http] && "$url" =~ \.md ]]; then
                    # Resolve relative path
                    link_dir=$(dirname "$english_file")
                    target_file=$(realpath -m "$link_dir/$url" 2>/dev/null || echo "$link_dir/$url")
                    
                    if [[ ! -f "$target_file" ]]; then
                        echo -e "${YELLOW}⚠️  Broken link in $english_file: $url → $target_file${NC}"
                        BROKEN_LINKS=$((BROKEN_LINKS + 1))
                    fi
                fi
            done <<< "$links"
        fi
        
    done < <(find docs/en -name "*.md" -print0 2>/dev/null || true)
}

# Function to check file structure consistency for Viewpoints & Perspectives
check_structure_consistency() {
    echo -e "${BLUE}🏗️  Checking Viewpoints & Perspectives directory structure consistency...${NC}"
    
    # Check critical directories for new structure
    local critical_dirs=(
        "docs/viewpoints"
        "docs/perspectives" 
        "docs/diagrams"
        "docs/templates"
        "docs/api"
        "docs/mcp"
        "docs/releases"
        "docs/reports"
    )
    
    for chinese_dir in "${critical_dirs[@]}"; do
        if [[ -d "$chinese_dir" ]]; then
            relative_dir="${chinese_dir#docs/}"
            english_dir="docs/en/$relative_dir"
            
            if [[ ! -d "$english_dir" ]]; then
                echo -e "${YELLOW}⚠️  Missing critical English directory: $english_dir${NC}"
            else
                echo -e "${GREEN}✅ Critical directory exists: $english_dir${NC}"
            fi
        fi
    done
    
    # Check if docs/en/ mirrors docs/ structure for all directories
    find docs -type d -not -path "docs/en*" -not -path "docs/.git*" -not -path "docs/legacy*" | while read -r chinese_dir; do
        relative_dir="${chinese_dir#docs/}"
        english_dir="docs/en/$relative_dir"
        
        if [[ "$relative_dir" != "." && ! -d "$english_dir" ]]; then
            echo -e "${YELLOW}⚠️  Missing English directory: $english_dir${NC}"
        fi
    done
    
    # Check for Viewpoints & Perspectives specific structure
    echo -e "${BLUE}📋 Checking Viewpoints & Perspectives specific structure...${NC}"
    
    # Seven Viewpoints
    local viewpoints=(
        "functional"
        "information"
        "concurrency"
        "development"
        "deployment"
        "operational"
    )
    
    for viewpoint in "${viewpoints[@]}"; do
        chinese_viewpoint_dir="docs/viewpoints/$viewpoint"
        english_viewpoint_dir="docs/en/viewpoints/$viewpoint"
        
        if [[ -d "$chinese_viewpoint_dir" && ! -d "$english_viewpoint_dir" ]]; then
            echo -e "${YELLOW}⚠️  Missing English viewpoint directory: $english_viewpoint_dir${NC}"
        elif [[ -d "$chinese_viewpoint_dir" && -d "$english_viewpoint_dir" ]]; then
            echo -e "${GREEN}✅ Viewpoint directory exists: $viewpoint${NC}"
        fi
    done
    
    # Eight Perspectives
    local perspectives=(
        "security"
        "performance"
        "availability"
        "evolution"
        "usability"
        "regulation"
        "location"
        "cost"
    )
    
    for perspective in "${perspectives[@]}"; do
        chinese_perspective_dir="docs/perspectives/$perspective"
        english_perspective_dir="docs/en/perspectives/$perspective"
        
        if [[ -d "$chinese_perspective_dir" && ! -d "$english_perspective_dir" ]]; then
            echo -e "${YELLOW}⚠️  Missing English perspective directory: $english_perspective_dir${NC}"
        elif [[ -d "$chinese_perspective_dir" && -d "$english_perspective_dir" ]]; then
            echo -e "${GREEN}✅ Perspective directory exists: $perspective${NC}"
        fi
    done
}

# Function to validate terminology dictionary for Viewpoints & Perspectives
validate_terminology_dictionary() {
    echo -e "${BLUE}📚 Validating enhanced terminology dictionary...${NC}"
    
    # Check if terminology file is valid JSON
    if ! python3 -m json.tool "$TERMINOLOGY_FILE" > /dev/null 2>&1; then
        echo -e "${RED}❌ Invalid JSON in terminology dictionary${NC}"
        return 1
    fi
    
    # Count terms in dictionary and check categories
    python3 -c "
import json
import sys

try:
    with open('$TERMINOLOGY_FILE', 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    # Check metadata
    if 'metadata' in data:
        version = data['metadata'].get('version', 'Unknown')
        print(f'📖 Dictionary version: {version}')
        
        if 'supportedStructure' in data['metadata']:
            structure = data['metadata']['supportedStructure']
            print(f'🏗️  Supported structure: {structure}')
    
    # Count terms by category
    total = 0
    categories = data.get('terminology', {})
    
    critical_categories = [
        'rozanski_woods_viewpoints',
        'rozanski_woods_perspectives',
        'ddd_strategic_patterns',
        'ddd_tactical_patterns',
        'stakeholder_terminology',
        'design_strategies'
    ]
    
    print('📊 Terms by category:')
    for category, terms in categories.items():
        count = len(terms)
        total += count
        status = '✅' if category in critical_categories else '📝'
        print(f'  {status} {category}: {count} terms')
    
    print(f'📚 Total terms: {total}')
    
    # Check for critical categories
    missing_critical = [cat for cat in critical_categories if cat not in categories]
    if missing_critical:
        print(f'⚠️  Missing critical categories: {missing_critical}')
        sys.exit(1)
    else:
        print('✅ All critical categories present')
        
except Exception as e:
    print(f'❌ Error validating dictionary: {e}')
    sys.exit(1)
" || return 1
    
    echo -e "${GREEN}✅ Enhanced terminology dictionary validation passed${NC}"
}

# Main execution
main() {
    echo -e "${BLUE}🚀 Starting comprehensive translation quality check...${NC}"
    echo ""
    
    validate_terminology_dictionary
    echo ""
    
    check_translation_completeness
    echo ""
    
    check_terminology_consistency
    echo ""
    
    check_internal_links
    echo ""
    
    check_structure_consistency
    echo ""
    
    # Summary report
    echo -e "${BLUE}📊 Translation Quality Report Summary${NC}"
    echo "=================================="
    echo -e "Total Chinese files checked: ${BLUE}$TOTAL_FILES${NC}"
    echo -e "Missing translations: ${YELLOW}$MISSING_TRANSLATIONS${NC}"
    echo -e "Inconsistent terms: ${YELLOW}$INCONSISTENT_TERMS${NC}"
    echo -e "Broken links: ${YELLOW}$BROKEN_LINKS${NC}"
    echo ""
    
    # Overall status
    total_issues=$((MISSING_TRANSLATIONS + INCONSISTENT_TERMS + BROKEN_LINKS))
    
    if [[ $total_issues -eq 0 ]]; then
        echo -e "${GREEN}🎉 All translation quality checks passed!${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  Found $total_issues translation quality issues${NC}"
        echo ""
        echo "Recommendations:"
        if [[ $MISSING_TRANSLATIONS -gt 0 ]]; then
            echo "- Run translation hook or manually translate missing files"
        fi
        if [[ $INCONSISTENT_TERMS -gt 0 ]]; then
            echo "- Review and update terminology usage in English files"
        fi
        if [[ $BROKEN_LINKS -gt 0 ]]; then
            echo "- Fix broken internal links in English documentation"
        fi
        exit 1
    fi
}

# Run main function
main "$@"