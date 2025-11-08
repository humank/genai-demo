#!/usr/bin/env python3
"""
Comprehensive Markdown Fixer
Fixes common markdown linting issues automatically
"""

import re
import sys
from pathlib import Path
from typing import List, Tuple

def fix_fenced_code_blocks(content: str) -> str:
    """Fix MD040: Add language to fenced code blocks"""
    # Pattern for code blocks without language
    pattern = r'^```\s*$'
    
    lines = content.split('\n')
    result = []
    in_code_block = False
    
    for i, line in enumerate(lines):
        if re.match(r'^```\s*$', line) and not in_code_block:
            # Start of code block without language
            # Try to guess language from context
            next_line = lines[i+1] if i+1 < len(lines) else ""
            
            if any(keyword in next_line for keyword in ['import', 'public class', 'package']):
                result.append('```java')
            elif any(keyword in next_line for keyword in ['def ', 'import ', 'class ']):
                result.append('```python')
            elif any(keyword in next_line for keyword in ['const ', 'let ', 'function', 'import {']):
                result.append('```javascript')
            elif any(keyword in next_line for keyword in ['#!/bin/bash', 'echo ', 'cd ']):
                result.append('```bash')
            elif re.match(r'^\s*[\{\[]', next_line):
                result.append('```json')
            elif re.match(r'^\s*[a-z_]+:', next_line):
                result.append('```yaml')
            else:
                result.append('```text')
            in_code_block = True
        elif re.match(r'^```', line):
            result.append(line)
            in_code_block = not in_code_block
        else:
            result.append(line)
    
    return '\n'.join(result)

def fix_ordered_lists(content: str) -> str:
    """Fix MD029: Ordered list numbering"""
    lines = content.split('\n')
    result = []
    in_list = False
    list_counter = 1
    
    for line in lines:
        # Check if line is an ordered list item
        match = re.match(r'^(\s*)(\d+)\.\s+(.+)$', line)
        if match:
            indent, num, text = match.groups()
            # Use sequential numbering
            result.append(f"{indent}{list_counter}. {text}")
            list_counter += 1
            in_list = True
        else:
            if in_list and line.strip() == '':
                # Empty line might end the list
                pass
            elif in_list and not line.startswith(' '):
                # Non-indented line ends the list
                list_counter = 1
                in_list = False
            result.append(line)
    
    return '\n'.join(result)

def fix_blank_lines_around_lists(content: str) -> str:
    """Fix MD032: Add blank lines around lists"""
    lines = content.split('\n')
    result = []
    prev_was_list = False
    
    for i, line in enumerate(lines):
        is_list = re.match(r'^\s*[-*+]\s+', line) or re.match(r'^\s*\d+\.\s+', line)
        prev_line = lines[i-1] if i > 0 else ""
        next_line = lines[i+1] if i+1 < len(lines) else ""
        
        # Add blank line before list if needed
        if is_list and not prev_was_list and prev_line.strip() != "" and i > 0:
            if not result or result[-1].strip() != "":
                result.append("")
        
        result.append(line)
        
        # Add blank line after list if needed
        if prev_was_list and not is_list and line.strip() != "" and not line.startswith('#'):
            if result[-2].strip() != "":
                result.insert(-1, "")
        
        prev_was_list = is_list
    
    return '\n'.join(result)

def process_file(file_path: Path) -> Tuple[bool, str]:
    """Process a single markdown file"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # Apply fixes
        content = fix_fenced_code_blocks(content)
        content = fix_ordered_lists(content)
        content = fix_blank_lines_around_lists(content)
        
        # Only write if changed
        if content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            return True, "Fixed"
        else:
            return False, "No changes"
            
    except Exception as e:
        return False, f"Error: {str(e)}"

def main():
    """Main function"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Fix markdown linting issues')
    parser.add_argument('paths', nargs='+', help='Paths to markdown files or directories')
    parser.add_argument('--dry-run', action='store_true', help='Show what would be changed without modifying files')
    
    args = parser.parse_args()
    
    files_to_process = []
    for path_str in args.paths:
        path = Path(path_str)
        if path.is_file() and path.suffix == '.md':
            files_to_process.append(path)
        elif path.is_dir():
            files_to_process.extend(path.rglob('*.md'))
    
    # Filter out node_modules and build directories
    files_to_process = [
        f for f in files_to_process 
        if 'node_modules' not in str(f) and 'build' not in str(f) and '.git' not in str(f)
    ]
    
    print(f"Processing {len(files_to_process)} markdown files...")
    
    fixed_count = 0
    for file_path in files_to_process:
        if args.dry_run:
            print(f"Would process: {file_path}")
        else:
            changed, message = process_file(file_path)
            if changed:
                print(f"✓ {file_path}: {message}")
                fixed_count += 1
    
    print(f"\n✅ Fixed {fixed_count} files")

if __name__ == '__main__':
    main()
