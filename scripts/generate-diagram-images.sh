#!/bin/bash

# Generate PNG and SVG images from PlantUML diagrams
# This script converts all PlantUML files to PNG/SVG format for documentation embedding
# Supports maximum directory coverage for comprehensive diagram automation

set -e

# Define all diagram directories to process
DIAGRAM_DIRS=(
    "docs/diagrams/plantuml"
    "docs/diagrams/plantuml/event-storming"
    "docs/diagrams/plantuml/structural"
    "docs/diagrams/plantuml/domain-event-handling"
    "docs/diagrams/viewpoints/functional"
    "docs/diagrams/viewpoints/information"
    "docs/diagrams/viewpoints/concurrency"
    "docs/diagrams/viewpoints/development"
    "docs/diagrams/viewpoints/deployment"
    "docs/diagrams/viewpoints/operational"
    "docs/diagrams/perspectives/security"
    "docs/diagrams/perspectives/performance"
    "docs/diagrams/perspectives/availability"
    "docs/diagrams/perspectives/evolution"
    "docs/diagrams/perspectives/cost"
    "docs/diagrams/perspectives/usability"
    "docs/diagrams/perspectives/location"
    "docs/diagrams/perspectives/regulation"
)

echo "🖼️  Generating PNG and SVG images from PlantUML diagrams..."
echo "📁 Processing ${#DIAGRAM_DIRS[@]} directories for maximum coverage..."

# Check if PlantUML JAR is available
PLANTUML_JAR="tools/plantuml.jar"
if [ ! -f "$PLANTUML_JAR" ]; then
    echo "❌ PlantUML JAR not found at $PLANTUML_JAR"
    echo "   Downloading PlantUML JAR..."
    mkdir -p tools
    curl -L -o "$PLANTUML_JAR" https://github.com/plantuml/plantuml/releases/download/v1.2024.8/plantuml-1.2024.8.jar
fi

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java to run PlantUML."
    exit 1
fi

# Initialize counters
TOTAL_PNG_COUNT=0
TOTAL_SVG_COUNT=0
PROCESSED_DIRS=0
SKIPPED_DIRS=0

# Process each directory
for DIAGRAM_DIR in "${DIAGRAM_DIRS[@]}"; do
    if [ -d "$DIAGRAM_DIR" ]; then
        # Check if directory contains .puml files
        PUML_FILES=$(find "$DIAGRAM_DIR" -maxdepth 1 -name "*.puml" 2>/dev/null | wc -l)
        
        if [ "$PUML_FILES" -gt 0 ]; then
            echo "📊 Processing directory: $DIAGRAM_DIR ($PUML_FILES files)"
            
            # Generate PNG images in the same directory
            echo "   🖼️  Generating PNG images..."
            java -jar "$PLANTUML_JAR" -tpng "$DIAGRAM_DIR"/*.puml 2>/dev/null || echo "   ⚠️  Some PNG generation issues in $DIAGRAM_DIR"
            
            # Generate SVG images in the same directory
            echo "   🖼️  Generating SVG images..."
            java -jar "$PLANTUML_JAR" -tsvg "$DIAGRAM_DIR"/*.puml 2>/dev/null || echo "   ⚠️  Some SVG generation issues in $DIAGRAM_DIR"
            
            # Count generated images in this directory
            # PNG files are no longer generated - using SVG for better quality
            DIR_SVG_COUNT=$(find "$DIAGRAM_DIR" -maxdepth 1 -name "*.svg" 2>/dev/null | wc -l)
            
            echo "   ✅ Generated $DIR_PNG_COUNT PNG and $DIR_SVG_COUNT SVG images"
            
            TOTAL_PNG_COUNT=$((TOTAL_PNG_COUNT + DIR_PNG_COUNT))
            TOTAL_SVG_COUNT=$((TOTAL_SVG_COUNT + DIR_SVG_COUNT))
            PROCESSED_DIRS=$((PROCESSED_DIRS + 1))
        else
            echo "⏭️  Skipping $DIAGRAM_DIR (no .puml files)"
            SKIPPED_DIRS=$((SKIPPED_DIRS + 1))
        fi
    else
        echo "⏭️  Skipping $DIAGRAM_DIR (directory not found)"
        SKIPPED_DIRS=$((SKIPPED_DIRS + 1))
    fi
done

echo ""
echo "🎉 Image generation completed!"
echo "=" * 50
echo "📊 Summary:"
echo "   Processed directories: $PROCESSED_DIRS"
echo "   Skipped directories: $SKIPPED_DIRS"
echo "   Total PNG images: $TOTAL_PNG_COUNT"
echo "   Total SVG images: $TOTAL_SVG_COUNT"
echo "   Total images: $((TOTAL_PNG_COUNT + TOTAL_SVG_COUNT))"
echo ""
echo "📁 Images are generated in the same directories as their source .puml files"
echo "   This maintains the organized structure and makes them easy to reference"