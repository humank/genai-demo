#!/usr/bin/env python3
"""
Script to convert task bullet points to numbered checkboxes in tasks.md
"""

import re
import sys

def update_tasks_file(file_path):
    """Update tasks.md to convert bullet points to numbered checkboxes"""
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    lines = content.split('\n')
    updated_lines = []
    current_main_task = None
    sub_task_counter = 1
    
    for line in lines:
        # Check if this is a main task (- [ ] X.Y format)
        main_task_match = re.match(r'^- \[ \] (\d+\.\d+) (.+)$', line)
        if main_task_match:
            current_main_task = main_task_match.group(1)
            sub_task_counter = 1
            updated_lines.append(line)
            continue
        
        # Check if this is a bullet point under a main task (starts with "  - " and not a checkbox)
        if current_main_task and line.startswith('  - ') and not line.startswith('  - [ ]') and not line.startswith('  - [x]'):
            # Skip requirement lines (lines starting with "  - _需求:")
            if line.startswith('  - _需求:'):
                updated_lines.append(line)
                continue
            
            # Convert bullet point to numbered checkbox
            bullet_content = line[4:]  # Remove "  - " prefix
            new_line = f"  - [ ] {current_main_task}.{sub_task_counter} {bullet_content}"
            updated_lines.append(new_line)
            sub_task_counter += 1
        else:
            updated_lines.append(line)
    
    # Write updated content back to file
    updated_content = '\n'.join(updated_lines)
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(updated_content)
    
    print(f"✅ Updated {file_path} with numbered checkboxes")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python3 update-tasks-checkboxes.py <tasks.md file path>")
        sys.exit(1)
    
    file_path = sys.argv[1]
    update_tasks_file(file_path)