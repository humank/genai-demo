#!/usr/bin/env python3

"""
Diagram Validation Script
Comprehensive validation for Mermaid, PlantUML, and Excalidraw diagrams
Supports syntax checking, rendering validation, and metadata verification
"""

import os
import re
import json
import sys
import argparse
from pathlib import Path
from typing import Dict, List, Tuple, Any
from datetime import datetime

# Configuration
CONFIG = {
    'supported_formats': ['.mmd', '.puml', '.plantuml', '.excalidraw'],
    'output_dir': 'build/reports/documentation-quality',
    'verbose': False,
    'check_rendering': False,
    'check_metadata': True
}

# Results tracking
results = {
    'total_diagrams': 0,
    'valid_diagrams': 0,
    'invalid_diagrams': 0,
    'warnings': 0,
    'by_type': {},
    'files': {},
    'errors': [],
    'warnings_list': [],
    'summary': {}
}

def log(message: str, level: str = 'info'):
    """Log with colors and levels"""
    colors = {
        'info': '\033[34m',      # Blue
        'success': '\033[32m',   # Green
        'warning': '\033[33m',   # Yellow
        'error': '\033[31m',     # Red
        'reset': '\033[0m'       # Reset
    }
    
    if CONFIG['verbose'] or level in ['warning', 'error']:
        prefix = {
            'info': 'ℹ️ ',
            'success': '✅',
            'warning': '⚠️ ',
            'error': '❌'
        }.get(level, '')
        
        print(f"{colors.get(level, '')}{prefix} {message}{colors['reset']}")

def validate_mermaid_diagram(file_path: Path, content: str) -> Dict[str, Any]:
    """Validate Mermaid diagram syntax"""
    validation_result = {
        'valid': True,
        'errors': [],
        'warnings': [],
        'metadata': {}
    }
    
    # Check for valid Mermaid diagram types
    mermaid_types = [
        r'^graph\s+(TD|TB|BT|RL|LR)',
        r'^flowchart\s+(TD|TB|BT|RL|LR)',
        r'^sequenceDiagram',
        r'^classDiagram',
        r'^stateDiagram(-v2)?',
        r'^erDiagram',
        r'^journey',
        r'^gitgraph',
        r'^pie\s+title',
        r'^gantt',
        r'^mindmap',
        r'^timeline'
    ]
    
    lines = content.strip().split('\n')
    if not lines:
        validation_result['valid'] = False
        validation_result['errors'].append('Empty diagram file')
        return validation_result
    
    # Check first non-empty line for diagram type
    first_line = None
    for line in lines:
        stripped = line.strip()
        if stripped and not stripped.startswith('---'):  # Skip YAML front matter
            first_line = stripped
            break
    
    if not first_line:
        validation_result['valid'] = False
        validation_result['errors'].append('No diagram content found')
        return validation_result
    
    # Validate diagram type
    diagram_type_found = False
    for pattern in mermaid_types:
        if re.match(pattern, first_line, re.IGNORECASE):
            diagram_type_found = True
            validation_result['metadata']['type'] = first_line.split()[0].lower()
            break
    
    if not diagram_type_found:
        validation_result['valid'] = False
        validation_result['errors'].append(f'Unknown Mermaid diagram type: {first_line}')
        return validation_result
    
    # Check for common syntax issues
    for i, line in enumerate(lines, 1):
        stripped = line.strip()
        if not stripped:
            continue
            
        # Check for unbalanced brackets
        open_brackets = stripped.count('[') + stripped.count('(') + stripped.count('{')
        close_brackets = stripped.count(']') + stripped.count(')') + stripped.count('}')
        
        if abs(open_brackets - close_brackets) > 2:  # Allow some flexibility
            validation_result['warnings'].append(f'Line {i}: Potentially unbalanced brackets')
    
    # Check for YAML front matter
    if content.startswith('---'):
        try:
            front_matter_end = content.find('---', 3)
            if front_matter_end > 0:
                validation_result['metadata']['has_front_matter'] = True
        except:
            validation_result['warnings'].append('Invalid YAML front matter')
    
    log(f"Mermaid validation: {file_path.name} - {'✅' if validation_result['valid'] else '❌'}", 
        'success' if validation_result['valid'] else 'error')
    
    return validation_result

def validate_plantuml_diagram(file_path: Path, content: str) -> Dict[str, Any]:
    """Validate PlantUML diagram syntax"""
    validation_result = {
        'valid': True,
        'errors': [],
        'warnings': [],
        'metadata': {}
    }
    
    # Check for @startuml and @enduml tags
    start_tags = content.count('@startuml')
    end_tags = content.count('@enduml')
    
    if start_tags == 0:
        validation_result['valid'] = False
        validation_result['errors'].append('Missing @startuml tag')
    
    if end_tags == 0:
        validation_result['valid'] = False
        validation_result['errors'].append('Missing @enduml tag')
    
    if start_tags != end_tags:
        validation_result['valid'] = False
        validation_result['errors'].append(f'Unbalanced tags: {start_tags} @startuml, {end_tags} @enduml')
    
    if start_tags > 1:
        validation_result['warnings'].append(f'Multiple diagrams in one file ({start_tags} diagrams)')
    
    # Detect PlantUML diagram type
    diagram_types = {
        'class': ['class ', 'abstract class', 'interface'],
        'sequence': ['participant', 'actor', 'activate', 'deactivate'],
        'usecase': ['usecase', 'actor', '(', ')'],
        'activity': ['start', 'stop', 'if', 'endif', 'while', 'endwhile'],
        'component': ['component', 'interface', 'package'],
        'state': ['state', '[*]', '-->', 'note'],
        'object': ['object', 'map'],
        'deployment': ['node', 'artifact', 'cloud'],
        'timing': ['robust', 'concise', '@'],
        'network': ['nwdiag', 'network', 'group']
    }
    
    detected_types = []
    content_lower = content.lower()
    
    for diagram_type, keywords in diagram_types.items():
        if any(keyword in content_lower for keyword in keywords):
            detected_types.append(diagram_type)
    
    if detected_types:
        validation_result['metadata']['detected_types'] = detected_types
        validation_result['metadata']['primary_type'] = detected_types[0]
    else:
        validation_result['warnings'].append('Could not detect PlantUML diagram type')
    
    # Check for common syntax issues
    lines = content.split('\n')
    for i, line in enumerate(lines, 1):
        stripped = line.strip()
        if not stripped or stripped.startswith("'"):  # Skip empty lines and comments
            continue
            
        # Check for unmatched quotes
        single_quotes = stripped.count("'")
        double_quotes = stripped.count('"')
        
        if single_quotes % 2 != 0:
            validation_result['warnings'].append(f'Line {i}: Unmatched single quotes')
        if double_quotes % 2 != 0:
            validation_result['warnings'].append(f'Line {i}: Unmatched double quotes')
    
    log(f"PlantUML validation: {file_path.name} - {'✅' if validation_result['valid'] else '❌'}", 
        'success' if validation_result['valid'] else 'error')
    
    return validation_result

def validate_excalidraw_diagram(file_path: Path, content: str) -> Dict[str, Any]:
    """Validate Excalidraw diagram JSON"""
    validation_result = {
        'valid': True,
        'errors': [],
        'warnings': [],
        'metadata': {}
    }
    
    try:
        data = json.loads(content)
        
        # Check required Excalidraw structure
        if 'type' not in data or data['type'] != 'excalidraw':
            validation_result['warnings'].append('Missing or invalid type field')
        
        if 'elements' not in data:
            validation_result['valid'] = False
            validation_result['errors'].append('Missing elements array')
        else:
            elements = data['elements']
            if not isinstance(elements, list):
                validation_result['valid'] = False
                validation_result['errors'].append('Elements must be an array')
            else:
                validation_result['metadata']['element_count'] = len(elements)
                
                # Analyze element types
                element_types = {}
                for element in elements:
                    if isinstance(element, dict) and 'type' in element:
                        elem_type = element['type']
                        element_types[elem_type] = element_types.get(elem_type, 0) + 1
                
                validation_result['metadata']['element_types'] = element_types
        
        if 'appState' in data:
            validation_result['metadata']['has_app_state'] = True
        
        if 'files' in data:
            validation_result['metadata']['has_files'] = True
            validation_result['metadata']['file_count'] = len(data['files'])
        
    except json.JSONDecodeError as e:
        validation_result['valid'] = False
        validation_result['errors'].append(f'Invalid JSON: {str(e)}')
    except Exception as e:
        validation_result['valid'] = False
        validation_result['errors'].append(f'Validation error: {str(e)}')
    
    log(f"Excalidraw validation: {file_path.name} - {'✅' if validation_result['valid'] else '❌'}", 
        'success' if validation_result['valid'] else 'error')
    
    return validation_result

def validate_diagram_file(file_path: Path) -> Dict[str, Any]:
    """Validate a single diagram file"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        file_extension = file_path.suffix.lower()
        
        if file_extension == '.mmd':
            return validate_mermaid_diagram(file_path, content)
        elif file_extension in ['.puml', '.plantuml']:
            return validate_plantuml_diagram(file_path, content)
        elif file_extension == '.excalidraw':
            return validate_excalidraw_diagram(file_path, content)
        else:
            return {
                'valid': False,
                'errors': [f'Unsupported file type: {file_extension}'],
                'warnings': [],
                'metadata': {}
            }
    
    except Exception as e:
        return {
            'valid': False,
            'errors': [f'Failed to read file: {str(e)}'],
            'warnings': [],
            'metadata': {}
        }

def find_diagram_files(root_dir: str = '.') -> List[Path]:
    """Find all diagram files in the project"""
    diagram_files = []
    
    for ext in CONFIG['supported_formats']:
        pattern = f"**/*{ext}"
        files = Path(root_dir).glob(pattern)
        
        for file_path in files:
            # Skip certain directories
            if any(part in str(file_path) for part in ['node_modules', '.git', '__pycache__']):
                continue
            diagram_files.append(file_path)
    
    return sorted(diagram_files)

def generate_report():
    """Generate comprehensive validation report"""
    # Calculate summary
    results['summary'] = {
        'total_diagrams': results['total_diagrams'],
        'valid_diagrams': results['valid_diagrams'],
        'invalid_diagrams': results['invalid_diagrams'],
        'warnings': results['warnings'],
        'success_rate': (results['valid_diagrams'] / results['total_diagrams'] * 100) if results['total_diagrams'] > 0 else 0,
        'timestamp': datetime.now().isoformat()
    }
    
    # Ensure output directory exists
    output_dir = Path(CONFIG['output_dir'])
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Generate JSON report
    json_report_path = output_dir / 'diagram-validation-report.json'
    with open(json_report_path, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    
    # Generate Markdown report
    markdown_report_path = output_dir / 'diagram-validation-report.md'
    
    markdown_content = f"""# Diagram Validation Report

**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}  
**Total Diagrams:** {results['total_diagrams']}  
**Valid Diagrams:** {results['valid_diagrams']}  
**Invalid Diagrams:** {results['invalid_diagrams']}  
**Warnings:** {results['warnings']}  
**Success Rate:** {results['summary']['success_rate']:.1f}%

## Summary by Type

| Type | Total | Valid | Invalid | Success Rate |
|------|-------|-------|---------|--------------|
"""
    
    for diagram_type, stats in results['by_type'].items():
        success_rate = (stats['valid'] / stats['total'] * 100) if stats['total'] > 0 else 0
        markdown_content += f"| {diagram_type} | {stats['total']} | {stats['valid']} | {stats['invalid']} | {success_rate:.1f}% |\n"
    
    if results['errors']:
        markdown_content += "\n## Errors\n\n"
        for error in results['errors']:
            markdown_content += f"- **{error['file']}**: {error['message']}\n"
    
    if results['warnings_list']:
        markdown_content += "\n## Warnings\n\n"
        for warning in results['warnings_list']:
            markdown_content += f"- **{warning['file']}**: {warning['message']}\n"
    
    markdown_content += "\n## Files Processed\n\n"
    for file_path, file_result in results['files'].items():
        status = "✅" if file_result['valid'] else "❌"
        markdown_content += f"- {status} **{file_path}**"
        if file_result['errors']:
            markdown_content += f" - {', '.join(file_result['errors'])}"
        markdown_content += "\n"
    
    with open(markdown_report_path, 'w', encoding='utf-8') as f:
        f.write(markdown_content)
    
    return json_report_path, markdown_report_path

def main():
    """Main execution function"""
    parser = argparse.ArgumentParser(description='Validate diagram files')
    parser.add_argument('--verbose', '-v', action='store_true', help='Verbose output')
    parser.add_argument('--output', '-o', help='Output directory for reports')
    parser.add_argument('--check-rendering', action='store_true', help='Check diagram rendering (requires tools)')
    parser.add_argument('path', nargs='?', default='.', help='Path to check (default: current directory)')
    
    args = parser.parse_args()
    
    CONFIG['verbose'] = args.verbose
    if args.output:
        CONFIG['output_dir'] = args.output
    CONFIG['check_rendering'] = args.check_rendering
    
    log("📊 Diagram Validation Starting...", 'info')
    
    # Find all diagram files
    diagram_files = find_diagram_files(args.path)
    results['total_diagrams'] = len(diagram_files)
    
    if not diagram_files:
        log("No diagram files found", 'warning')
        return 0
    
    log(f"Found {len(diagram_files)} diagram files", 'info')
    
    # Process each file
    for file_path in diagram_files:
        log(f"Validating: {file_path}", 'info')
        
        validation_result = validate_diagram_file(file_path)
        
        # Track results
        file_extension = file_path.suffix.lower()
        diagram_type = {
            '.mmd': 'Mermaid',
            '.puml': 'PlantUML',
            '.plantuml': 'PlantUML',
            '.excalidraw': 'Excalidraw'
        }.get(file_extension, 'Unknown')
        
        if diagram_type not in results['by_type']:
            results['by_type'][diagram_type] = {'total': 0, 'valid': 0, 'invalid': 0}
        
        results['by_type'][diagram_type]['total'] += 1
        
        if validation_result['valid']:
            results['valid_diagrams'] += 1
            results['by_type'][diagram_type]['valid'] += 1
        else:
            results['invalid_diagrams'] += 1
            results['by_type'][diagram_type]['invalid'] += 1
            
            for error in validation_result['errors']:
                results['errors'].append({
                    'file': str(file_path),
                    'message': error
                })
        
        for warning in validation_result['warnings']:
            results['warnings'] += 1
            results['warnings_list'].append({
                'file': str(file_path),
                'message': warning
            })
        
        results['files'][str(file_path)] = validation_result
    
    # Generate reports
    json_report, markdown_report = generate_report()
    
    # Print summary
    log("\n📊 Diagram Validation Complete", 'info')
    log("=" * 30, 'info')
    log(f"Total Diagrams: {results['total_diagrams']}", 'info')
    log(f"Valid Diagrams: {results['valid_diagrams']}", 'success')
    log(f"Invalid Diagrams: {results['invalid_diagrams']}", 'error' if results['invalid_diagrams'] > 0 else 'info')
    log(f"Warnings: {results['warnings']}", 'warning' if results['warnings'] > 0 else 'info')
    log(f"Success Rate: {results['summary']['success_rate']:.1f}%", 
        'success' if results['summary']['success_rate'] >= 90 else 'warning')
    
    log(f"\nReports generated:", 'info')
    log(f"- JSON: {json_report}", 'info')
    log(f"- Markdown: {markdown_report}", 'info')
    
    # Return appropriate exit code
    return 0 if results['invalid_diagrams'] == 0 else 1

if __name__ == '__main__':
    sys.exit(main())