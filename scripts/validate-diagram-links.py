#!/usr/bin/env python3
"""
Validate and fix diagram links in documentation files
"""

import os
import re
import argparse
from pathlib import Path
import urllib.parse

def find_correct_diagram_path(broken_path, alt_text, md_file_dir):
    """Find the correct path for a broken diagram reference"""
    project_root = Path(".")
    diagrams_dir = project_root / "docs" / "diagrams"
    
    # Extract diagram name from broken path or alt text
    diagram_name = Path(broken_path).stem if broken_path != "\\1" else alt_text.lower().replace(" ", "-")
    
    # Search for matching diagrams
    candidates = []
    
    # Look for PNG files first (preferred for PlantUML)
    for png_file in diagrams_dir.rglob("*.png"):
        if diagram_name.lower() in png_file.stem.lower():
            candidates.append(png_file)
    
    # Look for SVG files (for Mermaid)
    for svg_file in diagrams_dir.rglob("*.svg"):
        if diagram_name.lower() in svg_file.stem.lower():
            candidates.append(svg_file)
    
    if candidates:
        # Prefer generated PNG files
        png_candidates = [c for c in candidates if c.suffix == '.png' and 'generated' in str(c)]
        if png_candidates:
            best_candidate = png_candidates[0]
        else:
            best_candidate = candidates[0]
        
        # Calculate relative path
        relative_path = os.path.relpath(best_candidate, md_file_dir)
        return relative_path.replace('\\', '/')
    
    return None

def fix_broken_references(md_file, broken_links):
    """Fix broken references in a markdown file"""
    try:
        with open(md_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        fixes_made = []
        
        for link in broken_links:
            if link['file'] == str(md_file):
                # Find correct path
                correct_path = find_correct_diagram_path(
                    link['link'], 
                    link['alt'], 
                    md_file.parent
                )
                
                if correct_path:
                    # Replace the broken reference
                    old_ref = f"![{link['alt']}]({link['link']})"
                    new_ref = f"![{link['alt']}]({correct_path})"
                    
                    if old_ref in content:
                        content = content.replace(old_ref, new_ref)
                        fixes_made.append(f"{link['link']} → {correct_path}")
                    elif link['link'] == "\\1":
                        # Handle placeholder references
                        placeholder_pattern = rf"!\[{re.escape(link['alt'])}\]\(\\1\)"
                        content = re.sub(placeholder_pattern, new_ref, content)
                        fixes_made.append(f"\\1 → {correct_path}")
        
        # Write back if changes were made
        if content != original_content:
            with open(md_file, 'w', encoding='utf-8') as f:
                f.write(content)
            return fixes_made
        
    except Exception as e:
        print(f"Error fixing {md_file}: {e}")
    
    return []

def validate_diagram_links(fix_broken=False):
    """Validate all diagram links in markdown files"""
    project_root = Path(".")
    docs_dir = project_root / "docs"
    
    broken_links = []
    valid_links = []
    
    # Find all markdown files
    md_files = list(docs_dir.rglob("*.md"))
    
    # Pattern to match diagram references
    pattern = r'!\[([^\]]*)\]\(([^)]+)\)'
    
    for md_file in md_files:
        try:
            with open(md_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            matches = re.finditer(pattern, content)
            for match in matches:
                alt_text = match.group(1)
                link_path = match.group(2)
                
                # Skip external URLs
                if link_path.startswith('http'):
                    continue
                
                # Skip if not a diagram reference
                if 'diagrams' not in link_path and link_path != "\\1":
                    continue
                
                # Handle placeholder references
                if link_path == "\\1":
                    broken_links.append({
                        'file': str(md_file),
                        'link': link_path,
                        'alt': alt_text,
                        'resolved': 'PLACEHOLDER'
                    })
                    continue
                
                # Resolve the path
                md_dir = md_file.parent
                
                # Handle URL encoding
                decoded_path = urllib.parse.unquote(link_path)
                full_path = (md_dir / decoded_path).resolve()
                
                if full_path.exists():
                    valid_links.append({
                        'file': str(md_file),
                        'link': link_path,
                        'alt': alt_text,
                        'resolved': str(full_path)
                    })
                else:
                    broken_links.append({
                        'file': str(md_file),
                        'link': link_path,
                        'alt': alt_text,
                        'resolved': str(full_path)
                    })
                    
        except Exception as e:
            print(f"Error processing {md_file}: {e}")
    
    # Fix broken links if requested
    total_fixes = 0
    if fix_broken and broken_links:
        print(f"🔧 Fixing {len(broken_links)} broken references...")
        
        # Group by file
        files_to_fix = {}
        for link in broken_links:
            file_path = Path(link['file'])
            if file_path not in files_to_fix:
                files_to_fix[file_path] = []
            files_to_fix[file_path].append(link)
        
        # Fix each file
        for md_file, file_broken_links in files_to_fix.items():
            fixes = fix_broken_references(md_file, file_broken_links)
            if fixes:
                print(f"   ✅ Fixed {len(fixes)} references in {md_file.name}")
                for fix in fixes:
                    print(f"      • {fix}")
                total_fixes += len(fixes)
        
        # Re-validate after fixes
        if total_fixes > 0:
            print(f"\n🔄 Re-validating after {total_fixes} fixes...")
            return validate_diagram_links(fix_broken=False)
    
    # Report results
    print(f"📊 Diagram Link Validation Results")
    print(f"   ✅ Valid links: {len(valid_links)}")
    print(f"   ❌ Broken links: {len(broken_links)}")
    
    if broken_links:
        print(f"\n❌ Broken Links:")
        for link in broken_links:
            print(f"   File: {Path(link['file']).name}")
            print(f"   Link: {link['link']}")
            print(f"   Alt: {link['alt']}")
            print()
    
    if valid_links and len(valid_links) <= 10:
        print(f"\n✅ Valid Links:")
        for link in valid_links:
            print(f"   {link['alt']} -> {Path(link['resolved']).name}")
    elif valid_links:
        print(f"\n✅ Valid Links Summary:")
        for link in valid_links[:5]:
            print(f"   {link['alt']} -> {Path(link['resolved']).name}")
        print(f"   ... and {len(valid_links) - 5} more")
    
    return len(broken_links) == 0

def main():
    parser = argparse.ArgumentParser(description='Validate and fix diagram links')
    parser.add_argument('--fix-broken', action='store_true', 
                       help='Automatically fix broken references')
    
    args = parser.parse_args()
    
    success = validate_diagram_links(fix_broken=args.fix_broken)
    exit(0 if success else 1)

if __name__ == '__main__':
    main()