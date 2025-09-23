#!/usr/bin/env python3
"""
Fix diagram references in Markdown files.
- Update PlantUML (.puml) references to generated PNG files
- Keep Mermaid diagrams as-is (they render natively in GitHub)
- Update SVG references to PNG where appropriate
"""

import os
import re
import glob
from pathlib import Path

def find_generated_png_files():
    """Find all generated PNG files and create a mapping."""
    png_mapping = {}
    generated_dir = Path("docs/diagrams/generated")
    
    if generated_dir.exists():
        for png_file in generated_dir.rglob("*.png"):
            # Extract the base name without extension
            base_name = png_file.stem
            # Create relative path from docs root
            relative_path = png_file.relative_to(Path("docs"))
            png_mapping[base_name.lower()] = str(relative_path)
            
            # Also map with spaces replaced by hyphens
            hyphenated_name = base_name.replace(" ", "-").lower()
            png_mapping[hyphenated_name] = str(relative_path)
    
    return png_mapping

def find_puml_to_png_mapping():
    """Create mapping from PlantUML source files to generated PNG files."""
    puml_to_png = {}
    
    # Find all PlantUML source files
    puml_files = list(Path("docs/diagrams/viewpoints").rglob("*.puml"))
    
    for puml_file in puml_files:
        puml_name = puml_file.stem
        
        # Look for corresponding PNG in generated directory
        # Try different possible locations
        possible_locations = [
            f"docs/diagrams/generated/functional/{puml_name}.png",
            f"docs/diagrams/generated/information/{puml_name}.png",
            f"docs/diagrams/generated/deployment/{puml_name}.png",
            f"docs/diagrams/generated/concurrency/{puml_name}.png",
            f"docs/diagrams/generated/development/{puml_name}.png",
            f"docs/diagrams/generated/operational/{puml_name}.png",
            f"docs/diagrams/generated/perspectives/{puml_name}.png",
        ]
        
        for png_path in possible_locations:
            if Path(png_path).exists():
                puml_to_png[str(puml_file)] = png_path
                break
    
    return puml_to_png

def fix_diagram_references_in_file(file_path, png_mapping, puml_to_png):
    """Fix diagram references in a single Markdown file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        changes_made = []
        
        # Pattern to match image references
        img_pattern = r'!\[([^\]]*)\]\(([^)]+\.(puml|svg|png))\)'
        
        def replace_image_ref(match):
            alt_text = match.group(1)
            image_path = match.group(2)
            extension = match.group(3)
            
            # Skip external URLs
            if image_path.startswith(('http://', 'https://')):
                return match.group(0)
            
            # Skip Mermaid references (keep as-is)
            if '.mmd' in image_path:
                return match.group(0)
            
            # Handle PlantUML references
            if extension == 'puml':
                # Try to find corresponding PNG
                puml_name = Path(image_path).stem
                
                # Look for PNG in generated directory
                for category in ['functional', 'information', 'deployment', 'concurrency', 'development', 'operational', 'perspectives']:
                    png_name_variants = [
                        puml_name,
                        puml_name.replace('-', ' '),
                        puml_name.replace('_', ' '),
                        puml_name.replace('_', '-'),
                    ]
                    
                    for variant in png_name_variants:
                        png_path = f"docs/diagrams/generated/{category}/{variant}.png"
                        if Path(png_path).exists():
                            # Calculate relative path from current file
                            current_dir = Path(file_path).parent
                            try:
                                relative_png_path = os.path.relpath(png_path, current_dir)
                                changes_made.append(f"  {image_path} -> {relative_png_path}")
                                return f'![{alt_text}]({relative_png_path})'
                            except ValueError:
                                # Fallback to absolute path from docs root
                                relative_png_path = png_path.replace('docs/', '')
                                changes_made.append(f"  {image_path} -> {relative_png_path}")
                                return f'![{alt_text}]({relative_png_path})'
                
                # If no PNG found, keep original but add warning comment
                changes_made.append(f"  WARNING: No PNG found for {image_path}")
                return f'<!-- WARNING: PNG not found for {image_path} -->\n{match.group(0)}'
            
            # Handle SVG references - convert to PNG if available
            elif extension == 'svg':
                svg_name = Path(image_path).stem
                
                # Look for corresponding PNG in generated directory
                for category in ['functional', 'information', 'deployment', 'concurrency', 'development', 'operational', 'perspectives', 'plantuml', 'legacy']:
                    png_path = f"docs/diagrams/generated/{category}/{svg_name}.png"
                    if Path(png_path).exists():
                        current_dir = Path(file_path).parent
                        try:
                            relative_png_path = os.path.relpath(png_path, current_dir)
                            changes_made.append(f"  {image_path} -> {relative_png_path} (SVG->PNG)")
                            return f'![{alt_text}]({relative_png_path})'
                        except ValueError:
                            relative_png_path = png_path.replace('docs/', '')
                            changes_made.append(f"  {image_path} -> {relative_png_path} (SVG->PNG)")
                            return f'![{alt_text}]({relative_png_path})'
                
                # Also check for PNG with different naming patterns
                svg_name_variants = [
                    svg_name,
                    svg_name.replace('-', ' '),
                    svg_name.replace('_', ' '),
                    svg_name.replace('_', '-'),
                    svg_name.replace(' ', '-'),
                    svg_name.replace(' ', '_'),
                ]
                
                for variant in svg_name_variants:
                    for category in ['functional', 'information', 'deployment', 'concurrency', 'development', 'operational', 'perspectives', 'plantuml', 'legacy']:
                        png_path = f"docs/diagrams/generated/{category}/{variant}.png"
                        if Path(png_path).exists():
                            current_dir = Path(file_path).parent
                            try:
                                relative_png_path = os.path.relpath(png_path, current_dir)
                                changes_made.append(f"  {image_path} -> {relative_png_path} (SVG->PNG, variant: {variant})")
                                return f'![{alt_text}]({relative_png_path})'
                            except ValueError:
                                relative_png_path = png_path.replace('docs/', '')
                                changes_made.append(f"  {image_path} -> {relative_png_path} (SVG->PNG, variant: {variant})")
                                return f'![{alt_text}]({relative_png_path})'
                
                # Keep SVG if no PNG alternative found, but add a note
                if 'diagrams/viewpoints' in image_path or 'diagrams/plantuml' in image_path:
                    changes_made.append(f"  WARNING: No PNG found for {image_path} (should be generated)")
                
                return match.group(0)
            
            # Keep PNG references as-is
            return match.group(0)
        
        # Apply replacements
        content = re.sub(img_pattern, replace_image_ref, content)
        
        # Write back if changes were made
        if content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            
            print(f"✅ Updated {file_path}")
            for change in changes_made:
                print(change)
            return True
        else:
            print(f"⚪ No changes needed for {file_path}")
            return False
            
    except Exception as e:
        print(f"❌ Error processing {file_path}: {e}")
        return False

def main():
    """Main function to fix all diagram references."""
    print("🔍 Finding generated PNG files...")
    png_mapping = find_generated_png_files()
    print(f"Found {len(png_mapping)} PNG files")
    
    print("\n🔍 Creating PlantUML to PNG mapping...")
    puml_to_png = find_puml_to_png_mapping()
    print(f"Found {len(puml_to_png)} PlantUML mappings")
    
    print("\n🔧 Processing Markdown files...")
    
    # Find all Markdown files
    md_files = []
    for pattern in ['**/*.md', '*.md']:
        md_files.extend(glob.glob(pattern, recursive=True))
    
    # Remove duplicates and sort
    md_files = sorted(set(md_files))
    
    updated_count = 0
    total_count = len(md_files)
    
    for md_file in md_files:
        # Skip certain directories
        if any(skip_dir in md_file for skip_dir in ['.git', 'node_modules', '.kiro/hooks']):
            continue
            
        if fix_diagram_references_in_file(md_file, png_mapping, puml_to_png):
            updated_count += 1
    
    print(f"\n📊 Summary:")
    print(f"  Total files processed: {total_count}")
    print(f"  Files updated: {updated_count}")
    print(f"  Files unchanged: {total_count - updated_count}")
    
    if updated_count > 0:
        print(f"\n✅ Successfully updated diagram references in {updated_count} files!")
        print("📋 Next steps:")
        print("  1. Review the changes to ensure they're correct")
        print("  2. Test that all images display properly in GitHub")
        print("  3. Commit the changes")
    else:
        print("\n⚪ No files needed updates.")

if __name__ == "__main__":
    main()