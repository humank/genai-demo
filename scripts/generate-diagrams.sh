#!/bin/bash

# PlantUML Diagram Generation Script
# This script processes all .puml files and generates PNG/SVG diagrams
# Usage: ./scripts/generate-diagrams.sh [--format=png|svg|both] [--validate] [--clean] [file.puml]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PLANTUML_JAR="${PLANTUML_JAR:-plantuml.jar}"
PLANTUML_VERSION="1.2024.3"
PLANTUML_URL="https://github.com/plantuml/plantuml/releases/download/v${PLANTUML_VERSION}/plantuml-${PLANTUML_VERSION}.jar"
SOURCE_DIR="docs/diagrams/viewpoints"
OUTPUT_DIR="docs/diagrams/generated"
FORMAT="png"
VALIDATE_ONLY=false
CLEAN=false
SPECIFIC_FILE=""

# Parse command line arguments
for arg in "$@"; do
    case $arg in
        --format=*)
            FORMAT="${arg#*=}"
            ;;
        --validate)
            VALIDATE_ONLY=true
            ;;
        --clean)
            CLEAN=true
            ;;
        *.puml)
            SPECIFIC_FILE="$arg"
            ;;
        --help)
            echo "Usage: $0 [OPTIONS] [FILE]"
            echo ""
            echo "Options:"
            echo "  --format=png|svg|both    Output format (default: png)"
            echo "  --validate               Only validate syntax, don't generate"
            echo "  --clean                  Clean generated files before generating"
            echo "  --help                   Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                                    # Generate all diagrams as PNG"
            echo "  $0 --format=svg                       # Generate all diagrams as SVG"
            echo "  $0 --format=both                      # Generate both PNG and SVG"
            echo "  $0 --validate                         # Validate all diagrams"
            echo "  $0 bounded-contexts-overview.puml     # Generate specific diagram"
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

# Function to check if PlantUML is available
check_plantuml() {
    if [ ! -f "$PLANTUML_JAR" ]; then
        print_warning "PlantUML JAR not found. Downloading..."
        curl -L -o "$PLANTUML_JAR" "$PLANTUML_URL"
        if [ $? -eq 0 ]; then
            print_success "PlantUML downloaded successfully"
        else
            print_error "Failed to download PlantUML"
            exit 1
        fi
    fi
    
    # Check Java installation
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 11 or higher."
        exit 1
    fi
    
    print_info "Using PlantUML: $PLANTUML_JAR"
}

# Function to validate PlantUML syntax
validate_diagram() {
    local file="$1"
    print_info "Validating: $file"
    
    if java -jar "$PLANTUML_JAR" -syntax "$file" > /dev/null 2>&1; then
        print_success "✓ Valid: $file"
        return 0
    else
        print_error "✗ Invalid: $file"
        java -jar "$PLANTUML_JAR" -syntax "$file"
        return 1
    fi
}

# Function to generate diagram
generate_diagram() {
    local source_file="$1"
    local format="$2"
    
    # Determine output directory based on source location
    local rel_path="${source_file#$SOURCE_DIR/}"
    local category=$(dirname "$rel_path")
    local output_path="$OUTPUT_DIR/$category"
    
    # Create output directory if it doesn't exist
    mkdir -p "$output_path"
    
    print_info "Generating $format for: $source_file"
    
    # Generate diagram
    local format_flag=""
    case $format in
        png)
            format_flag="-tpng"
            ;;
        svg)
            format_flag="-tsvg"
            ;;
        *)
            print_error "Unknown format: $format"
            return 1
            ;;
    esac
    
    if java -jar "$PLANTUML_JAR" $format_flag -o "$(pwd)/$output_path" "$source_file" > /dev/null 2>&1; then
        local filename=$(basename "$source_file" .puml)
        print_success "✓ Generated: $output_path/$filename.$format"
        return 0
    else
        print_error "✗ Failed to generate: $source_file"
        java -jar "$PLANTUML_JAR" $format_flag -o "$(pwd)/$output_path" "$source_file"
        return 1
    fi
}

# Function to clean generated files
clean_generated() {
    print_info "Cleaning generated diagrams..."
    if [ -d "$OUTPUT_DIR" ]; then
        rm -rf "$OUTPUT_DIR"/*
        print_success "Cleaned: $OUTPUT_DIR"
    fi
}

# Function to process all diagrams
process_diagrams() {
    local validation_errors=0
    local generation_errors=0
    local total_files=0
    
    # Find all .puml files
    local files
    if [ -n "$SPECIFIC_FILE" ]; then
        if [ -f "$SPECIFIC_FILE" ]; then
            files="$SPECIFIC_FILE"
        else
            # Try to find the file in source directory
            files=$(find "$SOURCE_DIR" -name "$SPECIFIC_FILE" -type f)
            if [ -z "$files" ]; then
                print_error "File not found: $SPECIFIC_FILE"
                exit 1
            fi
        fi
    else
        files=$(find "$SOURCE_DIR" -name "*.puml" -type f)
    fi
    
    if [ -z "$files" ]; then
        print_warning "No .puml files found in $SOURCE_DIR"
        exit 0
    fi
    
    # Count total files
    total_files=$(echo "$files" | wc -l)
    print_info "Found $total_files diagram(s) to process"
    echo ""
    
    # Process each file
    for file in $files; do
        # Validate syntax
        if ! validate_diagram "$file"; then
            ((validation_errors++))
            continue
        fi
        
        # Generate diagrams if not validation-only mode
        if [ "$VALIDATE_ONLY" = false ]; then
            case $FORMAT in
                png)
                    if ! generate_diagram "$file" "png"; then
                        ((generation_errors++))
                    fi
                    ;;
                svg)
                    if ! generate_diagram "$file" "svg"; then
                        ((generation_errors++))
                    fi
                    ;;
                both)
                    if ! generate_diagram "$file" "png"; then
                        ((generation_errors++))
                    fi
                    if ! generate_diagram "$file" "svg"; then
                        ((generation_errors++))
                    fi
                    ;;
                *)
                    print_error "Unknown format: $FORMAT"
                    exit 1
                    ;;
            esac
        fi
        
        echo ""
    done
    
    # Print summary
    echo "========================================="
    echo "Summary:"
    echo "========================================="
    echo "Total files processed: $total_files"
    echo "Validation errors: $validation_errors"
    
    if [ "$VALIDATE_ONLY" = false ]; then
        echo "Generation errors: $generation_errors"
    fi
    
    echo "========================================="
    
    # Exit with error if there were any errors
    if [ $validation_errors -gt 0 ] || [ $generation_errors -gt 0 ]; then
        print_error "Diagram processing completed with errors"
        exit 1
    else
        print_success "All diagrams processed successfully!"
        exit 0
    fi
}

# Main execution
main() {
    print_info "PlantUML Diagram Generation Script"
    print_info "===================================="
    echo ""
    
    # Check PlantUML availability
    check_plantuml
    echo ""
    
    # Clean if requested
    if [ "$CLEAN" = true ]; then
        clean_generated
        echo ""
    fi
    
    # Process diagrams
    process_diagrams
}

# Run main function
main
