#!/usr/bin/env python3
"""
Fix PlantUML syntax issues in generated diagrams
"""

import os
import re
from pathlib import Path

def fix_plantuml_file(file_path: Path):
    """Fix common PlantUML syntax issues"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # Fix escaped quotes in activity diagrams
        content = re.sub(r':\\"([^"]+)\\"', r':\1', content)
        content = re.sub(r':\\"([^"]*)\\"([^;]*);', r':\1\2;', content)
        
        # Fix duplicate element names by adding suffixes
        # This is a simple approach - in practice, we'd need more sophisticated logic
        lines = content.split('\n')
        element_names = set()
        fixed_lines = []
        
        for line in lines:
            # Look for element definitions like [<b>Name</b>] as ElementName
            match = re.search(r'\[<b>([^<]+)</b>[^\]]*\]\s+as\s+(\w+)', line)
            if match:
                element_name = match.group(2)
                if element_name in element_names:
                    # Add suffix to make it unique
                    counter = 1
                    new_name = f"{element_name}_{counter}"
                    while new_name in element_names:
                        counter += 1
                        new_name = f"{element_name}_{counter}"
                    line = line.replace(f" as {element_name}", f" as {new_name}")
                    element_names.add(new_name)
                else:
                    element_names.add(element_name)
            
            # Look for rectangle definitions like rectangle "Name" as ElementName
            match = re.search(r'rectangle\s+"[^"]+"\s+as\s+(\w+)', line)
            if match:
                element_name = match.group(1)
                if element_name in element_names:
                    # Add suffix to make it unique
                    counter = 1
                    new_name = f"{element_name}_{counter}"
                    while new_name in element_names:
                        counter += 1
                        new_name = f"{element_name}_{counter}"
                    line = line.replace(f" as {element_name}", f" as {new_name}")
                    element_names.add(new_name)
                else:
                    element_names.add(element_name)
            
            fixed_lines.append(line)
        
        content = '\n'.join(fixed_lines)
        
        # Only write if content changed
        if content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"✅ Fixed syntax issues in {file_path}")
            return True
        else:
            print(f"ℹ️  No issues found in {file_path}")
            return False
            
    except Exception as e:
        print(f"⚠️  Error fixing {file_path}: {e}")
        return False

def main():
    """Main function"""
    diagram_dir = Path("docs/diagrams/viewpoints/functional")
    
    if not diagram_dir.exists():
        print(f"❌ Directory not found: {diagram_dir}")
        return
    
    puml_files = list(diagram_dir.glob("*.puml"))
    print(f"🔧 Fixing PlantUML syntax issues in {len(puml_files)} files...")
    
    fixed_count = 0
    for file_path in puml_files:
        if fix_plantuml_file(file_path):
            fixed_count += 1
    
    print(f"🎉 Fixed syntax issues in {fixed_count} files")

if __name__ == "__main__":
    main()