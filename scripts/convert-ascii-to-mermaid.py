#!/usr/bin/env python3
"""
Convert ASCII diagrams to Mermaid diagrams in Markdown files
Improved version that handles vertical flows and multi-path diagrams
"""

import re
import sys
from pathlib import Path
from typing import List, Tuple, Optional, Dict

def detect_diagram_type(content: str) -> str:
    """Detect the type of ASCII diagram"""
    # Check for directory tree structure (skip these)
    lines = content.split('\n')
    
    # If it's mostly tree characters and no complex flow, it's a directory tree
    tree_chars = sum(1 for line in lines if any(c in line for c in ['├──', '└──', '│']))
    flow_chars = sum(1 for line in lines if any(c in line for c in ['→', '↓', '┌─', '└─']))
    
    if tree_chars > 0 and flow_chars == 0:
        return 'directory_tree'
    
    # Check for flowchart with arrows
    if '→' in content or '↓' in content or '->' in content:
        return 'flowchart'
    
    # Check for box diagram
    if ('┌─' in content or '┌' in content) and ('└─' in content or '└' in content):
        return 'box'
    
    return 'unknown'

def parse_flowchart_structure(lines: List[str]) -> Tuple[Dict, List]:
    """Parse ASCII flowchart and extract nodes and connections"""
    node_counter = 1
    node_map = {}
    connections = []
    
    def clean_text(text: str) -> str:
        """Clean text from box drawing characters and extra spaces"""
        text = re.sub(r'[┌┐└┘─│]', '', text)
        text = re.sub(r'\s+', ' ', text)
        return text.strip()
    
    def get_node_id(text: str) -> Optional[str]:
        """Get or create node ID for given text"""
        nonlocal node_counter
        clean = clean_text(text)
        if not clean:
            return None
        if clean not in node_map:
            node_map[clean] = f'N{node_counter}'
            node_counter += 1
        return node_map[clean]
    
    def extract_nodes_with_positions(line: str) -> List[Tuple[str, int, int]]:
        """Extract nodes from a line with their positions"""
        nodes = []
        # Split by arrows but keep track of positions
        parts = re.split(r'(→|->|-->)', line)
        pos = 0
        
        for part in parts:
            if part not in ['→', '->', '-->']:
                text = clean_text(part)
                if text:
                    # Calculate approximate center position of this text in original line
                    start_pos = line.find(part, pos)
                    center_pos = start_pos + len(part) // 2
                    nodes.append((text, start_pos, center_pos))
            pos += len(part)
        
        return nodes
    
    # Parse all lines and track nodes with their positions
    lines_data = []
    
    for line_idx, line in enumerate(lines):
        # Skip pure box drawing lines
        if all(c in '┌┐└┘─│ \t' for c in line):
            continue
        
        line_info = {'type': None, 'nodes': [], 'arrows': []}
        
        # Handle horizontal arrows (→ or ->)
        if '→' in line or '->' in line:
            line_info['type'] = 'horizontal'
            nodes_with_pos = extract_nodes_with_positions(line)
            
            for i, (text, start, center) in enumerate(nodes_with_pos):
                node_id = get_node_id(text)
                if node_id:
                    line_info['nodes'].append({
                        'id': node_id,
                        'text': text,
                        'start': start,
                        'center': center
                    })
                    
                    # Create horizontal connection to next node
                    if i < len(nodes_with_pos) - 1:
                        next_text, _, _ = nodes_with_pos[i + 1]
                        next_id = get_node_id(next_text)
                        if next_id:
                            connections.append({
                                'from': node_id,
                                'from_text': text,
                                'to': next_id,
                                'to_text': next_text,
                                'type': 'horizontal',
                                'arrow': '-->'
                            })
        
        # Handle vertical arrows (↓)
        elif '↓' in line:
            line_info['type'] = 'arrows'
            # Find all arrow positions
            for match in re.finditer('↓', line):
                line_info['arrows'].append(match.start())
        
        # Handle text lines (potential nodes)
        elif line.strip() and not all(c in ' \t↓' for c in line):
            line_info['type'] = 'text'
            text = clean_text(line)
            if text:
                node_id = get_node_id(text)
                if node_id:
                    line_info['nodes'].append({
                        'id': node_id,
                        'text': text,
                        'start': 0,
                        'center': len(line) // 2
                    })
        
        if line_info['type']:
            lines_data.append((line_idx, line_info))
    
    # Second pass: create vertical connections
    for i in range(len(lines_data)):
        line_idx, line_info = lines_data[i]
        
        if line_info['type'] == 'arrows' and line_info['arrows']:
            # Find source nodes (previous line)
            source_nodes = None
            if i > 0:
                _, prev_info = lines_data[i - 1]
                if prev_info['nodes']:
                    source_nodes = prev_info['nodes']
            
            # Find target nodes (next line)
            target_nodes = None
            if i < len(lines_data) - 1:
                _, next_info = lines_data[i + 1]
                if next_info['nodes']:
                    target_nodes = next_info['nodes']
            
            # Create vertical connections based on arrow positions
            if source_nodes and target_nodes:
                for arrow_pos in line_info['arrows']:
                    # Find closest source node above this arrow
                    closest_source = min(source_nodes, 
                                       key=lambda n: abs(n['center'] - arrow_pos))
                    
                    # Find closest target node below this arrow
                    closest_target = min(target_nodes,
                                       key=lambda n: abs(n['center'] - arrow_pos))
                    
                    # Determine arrow type
                    is_failure = '(Fail)' in closest_target['text'] or 'fail' in closest_target['text'].lower()
                    arrow_type = '-.->|fail|' if is_failure else '-->'
                    
                    # For single arrow lines, connect the last node from previous line
                    # to the first node of next line (typical vertical flow)
                    if len(line_info['arrows']) == 1 and len(source_nodes) > 1:
                        # Use the last node from source line
                        source_node = source_nodes[-1]
                        # Use the first node from target line
                        target_node = target_nodes[0]
                    else:
                        source_node = closest_source
                        target_node = closest_target
                    
                    connections.append({
                        'from': source_node['id'],
                        'from_text': source_node['text'],
                        'to': target_node['id'],
                        'to_text': target_node['text'],
                        'type': 'vertical',
                        'arrow': arrow_type
                    })
    
    return node_map, connections

def convert_flowchart_to_mermaid(lines: List[str]) -> str:
    """Convert flowchart with arrows to Mermaid"""
    node_map, connections = parse_flowchart_structure(lines)
    
    if not connections:
        # No connections found, keep as text
        return '```text\n' + '\n'.join(lines) + '\n```'
    
    # Determine graph direction
    has_vertical = any(conn['type'] == 'vertical' for conn in connections)
    graph_direction = 'TD' if has_vertical else 'LR'
    
    mermaid_lines = ['```mermaid', f'graph {graph_direction}']
    
    # Add all nodes and connections
    added_nodes = set()
    
    for conn in connections:
        source_id = conn['from']
        target_id = conn['to']
        source_text = conn['from_text']
        target_text = conn['to_text']
        arrow = conn['arrow']
        
        # Add source node if not added
        if source_id not in added_nodes:
            mermaid_lines.append(f'    {source_id}["{source_text}"]')
            added_nodes.add(source_id)
        
        # Add target node if not added
        if target_id not in added_nodes:
            mermaid_lines.append(f'    {target_id}["{target_text}"]')
            added_nodes.add(target_id)
        
        # Add connection
        mermaid_lines.append(f'    {source_id} {arrow} {target_id}')
    
    mermaid_lines.append('```')
    return '\n'.join(mermaid_lines)

def convert_simple_structure(content: str) -> str:
    """Convert simple text structures to Mermaid"""
    lines = content.strip().split('\n')
    
    # Detect type
    diagram_type = detect_diagram_type(content)
    
    if diagram_type == 'directory_tree':
        # Keep directory trees as text
        return '```text\n' + content + '\n```'
    elif diagram_type == 'flowchart':
        return convert_flowchart_to_mermaid(lines)
    elif diagram_type == 'box':
        return convert_flowchart_to_mermaid(lines)
    else:
        # Keep as text if we can't convert
        return '```text\n' + content + '\n```'

def process_file(file_path: Path, dry_run: bool = False) -> Tuple[bool, str]:
    """Process a single markdown file"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # Find all ```text blocks with ASCII diagrams
        pattern = r'```text\n(.*?)\n```'
        
        def replace_diagram(match):
            diagram_content = match.group(1)
            
            # Check if it contains ASCII diagram characters
            if any(char in diagram_content for char in ['├', '└', '│', '┌', '─', '→', '↓', '▼']):
                # Try to convert
                return convert_simple_structure(diagram_content)
            else:
                # Keep as is
                return match.group(0)
        
        content = re.sub(pattern, replace_diagram, content, flags=re.DOTALL)
        
        # Only write if changed
        if content != original_content:
            if not dry_run:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
            return True, "Would convert" if dry_run else "Converted"
        else:
            return False, "No ASCII diagrams found"
            
    except Exception as e:
        return False, f"Error: {str(e)}"

def main():
    """Main function"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Convert ASCII diagrams to Mermaid')
    parser.add_argument('paths', nargs='+', help='Paths to markdown files or directories')
    parser.add_argument('--dry-run', action='store_true', help='Show what would be changed')
    
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
    
    converted_count = 0
    for file_path in files_to_process:
        changed, message = process_file(file_path, args.dry_run)
        if changed:
            print(f"{'[DRY RUN] ' if args.dry_run else ''}✓ {file_path}: {message}")
            converted_count += 1
    
    print(f"\n{'Would convert' if args.dry_run else 'Converted'} {converted_count} files")

if __name__ == '__main__':
    main()
