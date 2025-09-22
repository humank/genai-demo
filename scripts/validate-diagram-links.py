#!/usr/bin/env python3
"""
Validate all diagram links in Markdown files.
Check if referenced files exist and categorize issues.
"""

import os
import re
import glob
from pathlib import Path
from urllib.parse import urlparse

def is_external_url(path):
    """Check if path is an external URL."""
    return path.startswith(('http://', 'https://'))

def validate_diagram_links():
    """Validate all diagram links in Markdown files."""
    print("🔍 Validating diagram links in Markdown files...")
    
    # Find all Markdown files
    md_files = []
    for pattern in ['**/*.md', '*.md']:
        md_files.extend(glob.glob(pattern, recursive=True))
    
    # Remove duplicates and sort
    md_files = sorted(set(md_files))
    
    # Statistics
    total_files = 0
    total_links = 0
    valid_links = 0
    invalid_links = 0
    external_links = 0
    mermaid_links = 0
    
    # Issue tracking
    issues = {
        'missing_files': [],
        'svg_should_be_png': [],
        'puml_not_converted': [],
        'broken_paths': []
    }
    
    # Pattern to match image references
    img_pattern = r'!\[([^\]]*)\]\(([^)]+\.(png|svg|puml|mmd))\)'
    
    for md_file in md_files:
        # Skip certain directories
        if any(skip_dir in md_file for skip_dir in ['.git', 'node_modules', '.kiro/hooks']):
            continue
            
        total_files += 1
        
        try:
            with open(md_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            matches = re.findall(img_pattern, content)
            
            for alt_text, image_path, extension in matches:
                total_links += 1
                
                # Skip external URLs
                if is_external_url(image_path):
                    external_links += 1
                    continue
                
                # Skip Mermaid files (they're handled differently)
                if '.mmd' in image_path:
                    mermaid_links += 1
                    continue
                
                # Calculate absolute path
                current_dir = Path(md_file).parent
                if image_path.startswith('/'):
                    # Absolute path from root
                    abs_path = Path(image_path[1:])
                else:
                    # Relative path
                    abs_path = current_dir / image_path
                
                # Normalize path
                abs_path = abs_path.resolve()
                
                # Check if file exists
                if abs_path.exists():
                    valid_links += 1
                    
                    # Check for optimization opportunities
                    if extension == 'svg' and 'diagrams/viewpoints' in image_path:
                        # Check if PNG alternative exists
                        png_path = str(abs_path).replace('.svg', '.png')
                        if 'generated' not in png_path:
                            # Look for PNG in generated directory
                            svg_name = abs_path.stem
                            for category in ['functional', 'information', 'deployment', 'concurrency', 'development', 'operational', 'perspectives']:
                                potential_png = Path(f"docs/diagrams/generated/{category}/{svg_name}.png")
                                if potential_png.exists():
                                    issues['svg_should_be_png'].append({
                                        'file': md_file,
                                        'current': image_path,
                                        'suggested': str(potential_png.relative_to(current_dir)),
                                        'alt_text': alt_text
                                    })
                                    break
                    
                    elif extension == 'puml':
                        issues['puml_not_converted'].append({
                            'file': md_file,
                            'path': image_path,
                            'alt_text': alt_text
                        })
                        
                else:
                    invalid_links += 1
                    issues['missing_files'].append({
                        'file': md_file,
                        'path': image_path,
                        'alt_text': alt_text,
                        'extension': extension
                    })
        
        except Exception as e:
            print(f"❌ Error processing {md_file}: {e}")
    
    # Print summary
    print(f"\n📊 Validation Summary:")
    print(f"  Total files scanned: {total_files}")
    print(f"  Total image links found: {total_links}")
    print(f"  Valid links: {valid_links}")
    print(f"  Invalid links: {invalid_links}")
    print(f"  External links: {external_links}")
    print(f"  Mermaid links: {mermaid_links}")
    
    # Print issues
    if issues['missing_files']:
        print(f"\n❌ Missing Files ({len(issues['missing_files'])}):")
        for issue in issues['missing_files'][:10]:  # Show first 10
            print(f"  📄 {issue['file']}")
            print(f"     Missing: {issue['path']}")
            print(f"     Alt text: {issue['alt_text']}")
        if len(issues['missing_files']) > 10:
            print(f"     ... and {len(issues['missing_files']) - 10} more")
    
    if issues['svg_should_be_png']:
        print(f"\n⚠️ SVG Should Be PNG ({len(issues['svg_should_be_png'])}):")
        for issue in issues['svg_should_be_png'][:10]:  # Show first 10
            print(f"  📄 {issue['file']}")
            print(f"     Current: {issue['current']}")
            print(f"     Suggested: {issue['suggested']}")
        if len(issues['svg_should_be_png']) > 10:
            print(f"     ... and {len(issues['svg_should_be_png']) - 10} more")
    
    if issues['puml_not_converted']:
        print(f"\n⚠️ PlantUML Not Converted ({len(issues['puml_not_converted'])}):")
        for issue in issues['puml_not_converted'][:10]:  # Show first 10
            print(f"  📄 {issue['file']}")
            print(f"     PlantUML: {issue['path']}")
        if len(issues['puml_not_converted']) > 10:
            print(f"     ... and {len(issues['puml_not_converted']) - 10} more")
    
    # Overall status
    if invalid_links == 0 and len(issues['svg_should_be_png']) == 0 and len(issues['puml_not_converted']) == 0:
        print(f"\n✅ All diagram links are valid and optimized!")
    else:
        print(f"\n📋 Issues found that need attention:")
        if invalid_links > 0:
            print(f"  - {invalid_links} broken links")
        if issues['svg_should_be_png']:
            print(f"  - {len(issues['svg_should_be_png'])} SVG links that should be PNG")
        if issues['puml_not_converted']:
            print(f"  - {len(issues['puml_not_converted'])} PlantUML links not converted")
    
    return {
        'total_files': total_files,
        'total_links': total_links,
        'valid_links': valid_links,
        'invalid_links': invalid_links,
        'external_links': external_links,
        'mermaid_links': mermaid_links,
        'issues': issues
    }

def main():
    """Main function."""
    results = validate_diagram_links()
    
    # Generate recommendations
    print(f"\n💡 Recommendations:")
    
    if results['issues']['svg_should_be_png']:
        print(f"  1. Run fix-diagram-references.py again to convert remaining SVG references")
    
    if results['issues']['missing_files']:
        print(f"  2. Check if missing files need to be regenerated or paths corrected")
    
    if results['issues']['puml_not_converted']:
        print(f"  3. Ensure all PlantUML files are converted to PNG references")
    
    if results['invalid_links'] == 0 and not any(results['issues'].values()):
        print(f"  🎉 No action needed - all links are valid and optimized!")

if __name__ == "__main__":
    main()