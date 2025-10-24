#!/bin/bash

# Diagram Validation Script
# This script validates PlantUML syntax and checks diagram references in markdown files
# Usage: ./scripts/validate-diagrams.sh [--check-syntax] [--check-references] [--check-missing]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SOURCE_DIR="docs/diagrams/viewpoints"
GENERATED_DIR="docs/diagrams/generated"
DOCS_DIR="docs"
CHECK_SYNTAX=true
CHECK_REFERENCES=true
CHECK_MISSING=true

# Parse command line arguments
for arg in "$@"; do
    case $arg in
        --check-syntax)
            CHECK_SYNTAX=true
            CHECK_REFERENCES=false
            CHECK_MISSING=false
            ;;
        --check-references)
            CHECK_SYNTAX=false
            CHECK_REFERENCES=true
            CHECK_MISSING=false
            ;;
        --check-missing)
            CHECK_SYNTAX=false
            CHECK_REFERENCES=false
            CHECK_MISSING=true
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --check-syntax       Only check PlantUML syntax"
            echo "  --check-references   Only check diagram references in markdown"
            echo "  --check-missing      Only check for missing diagrams"
            echo "  --help               Show this help message"
            echo ""
            echo "If no options are specified, all checks are performed."
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $arg${NC}"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Function to print colored messages
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check PlantUML syntax
check_syntax() {
    print_info "Checking PlantUML syntax..."
    echo ""
    
    local errors=0
    local total=0
    
    # Find all .puml files
    local files=$(find "$SOURCE_DIR" -name "*.puml" -type f 2>/dev/null)
    
    if [ -z "$files" ]; then
        print_warning "No .puml files found in $SOURCE_DIR"
        return 0
    fi
    
    # Check if plantuml command is available (Homebrew)
    if ! command -v plantuml &> /dev/null; then
        print_error "PlantUML is not installed. Please install it using Homebrew:"
        print_error "  brew install plantuml"
        return 1
    fi
    
    # Validate each file
    for file in $files; do
        ((total++))
        if plantuml -checkonly "$file" > /dev/null 2>&1; then
            print_success "✓ Valid: $file"
        else
            print_error "✗ Invalid: $file"
            plantuml -checkonly "$file"
            ((errors++))
        fi
    done
    
    echo ""
    echo "Syntax Check Summary:"
    echo "  Total files: $total"
    echo "  Errors: $errors"
    echo ""
    
    return $errors
}

# Function to check diagram references in markdown files
check_references() {
    print_info "Checking diagram references in markdown files..."
    echo ""
    
    local errors=0
    local warnings=0
    local total_refs=0
    
    # Find all markdown files
    local md_files=$(find "$DOCS_DIR" -name "*.md" -type f 2>/dev/null)
    
    if [ -z "$md_files" ]; then
        print_warning "No markdown files found in $DOCS_DIR"
        return 0
    fi
    
    # Check each markdown file for diagram references
    for md_file in $md_files; do
        # Find image references to diagrams
        local refs=$(grep -o '\!\[.*\](.*\.png\|.*\.svg)' "$md_file" 2>/dev/null | grep -o '([^)]*)' | tr -d '()')
        
        if [ -z "$refs" ]; then
            continue
        fi
        
        # Check each reference
        for ref in $refs; do
            ((total_refs++))
            
            # Skip external URLs
            if [[ "$ref" =~ ^https?:// ]]; then
                continue
            fi
            
            # Resolve relative path
            local md_dir=$(dirname "$md_file")
            local diagram_path="$md_dir/$ref"
            
            # Normalize path
            diagram_path=$(realpath -m "$diagram_path" 2>/dev/null || echo "$diagram_path")
            
            # Check if file exists
            if [ -f "$diagram_path" ]; then
                print_success "✓ Found: $ref (in $md_file)"
            else
                # Check if it's a generated diagram that might not exist yet
                if [[ "$ref" == *"/generated/"* ]]; then
                    print_warning "⚠ Missing generated diagram: $ref (in $md_file)"
                    print_info "  Run ./scripts/generate-diagrams.sh to generate it"
                    ((warnings++))
                else
                    print_error "✗ Missing diagram: $ref (in $md_file)"
                    ((errors++))
                fi
            fi
        done
    done
    
    echo ""
    echo "Reference Check Summary:"
    echo "  Total references: $total_refs"
    echo "  Errors: $errors"
    echo "  Warnings: $warnings"
    echo ""
    
    return $errors
}

# Function to check for missing generated diagrams
check_missing() {
    print_info "Checking for missing generated diagrams..."
    echo ""
    
    local missing=0
    local total=0
    
    # Find all .puml files
    local puml_files=$(find "$SOURCE_DIR" -name "*.puml" -type f 2>/dev/null)
    
    if [ -z "$puml_files" ]; then
        print_warning "No .puml files found in $SOURCE_DIR"
        return 0
    fi
    
    # Check each .puml file for corresponding generated files
    for puml_file in $puml_files; do
        ((total++))
        
        # Determine expected output path
        local rel_path="${puml_file#$SOURCE_DIR/}"
        local category=$(dirname "$rel_path")
        local filename=$(basename "$puml_file" .puml)
        local png_path="$GENERATED_DIR/$category/$filename.png"
        
        # Check if PNG exists
        if [ -f "$png_path" ]; then
            print_success "✓ Generated: $png_path"
        else
            print_warning "⚠ Missing: $png_path"
            print_info "  Source: $puml_file"
            ((missing++))
        fi
    done
    
    echo ""
    echo "Missing Diagrams Summary:"
    echo "  Total source files: $total"
    echo "  Missing generated: $missing"
    echo ""
    
    if [ $missing -gt 0 ]; then
        print_info "Run ./scripts/generate-diagrams.sh to generate missing diagrams"
    fi
    
    return $missing
}

# Main execution
main() {
    print_info "Diagram Validation Script"
    print_info "=========================="
    echo ""
    
    local exit_code=0
    
    # Run syntax check
    if [ "$CHECK_SYNTAX" = true ]; then
        if ! check_syntax; then
            exit_code=1
        fi
    fi
    
    # Run reference check
    if [ "$CHECK_REFERENCES" = true ]; then
        if ! check_references; then
            exit_code=1
        fi
    fi
    
    # Run missing diagrams check
    if [ "$CHECK_MISSING" = true ]; then
        if ! check_missing; then
            # Missing diagrams is a warning, not an error
            if [ $exit_code -eq 0 ]; then
                exit_code=0
            fi
        fi
    fi
    
    # Print final result
    echo "========================================="
    if [ $exit_code -eq 0 ]; then
        print_success "All validation checks passed!"
    else
        print_error "Validation failed with errors"
    fi
    echo "========================================="
    
    exit $exit_code
}

# Run main function
main
