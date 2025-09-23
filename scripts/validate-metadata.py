#!/usr/bin/env python3

"""
Document Metadata Validation Script
Validates YAML front matter and document metadata for Viewpoints & Perspectives documentation
Ensures consistency and completeness of document metadata
"""

import os
import re
import yaml
import json
import sys
import argparse
from pathlib import Path
from typing import Dict, List, Any, Optional
from datetime import datetime

# Configuration
CONFIG = {
    'required_fields': {
        'viewpoints': ['title', 'viewpoint', 'description', 'stakeholders'],
        'perspectives': ['title', 'perspective', 'description', 'quality_attributes'],
        'templates': ['title', 'type', 'description', 'usage'],
        'general': ['title', 'description']
    },
    'optional_fields': [
        'author', 'version', 'last_updated', 'tags', 'related_documents',
        'diagrams', 'references', 'status', 'review_date'
    ],
    'valid_viewpoints': [
        'functional', 'information', 'concurrency', 'development', 
        'deployment', 'operational', 'context'
    ],
    'valid_perspectives': [
        'security', 'performance', 'availability', 'evolution',
        'usability', 'regulation', 'location', 'cost'
    ],
    'valid_stakeholders': [
        'architect', 'developer', 'operator', 'security-engineer',
        'business-analyst', 'product-manager', 'test-engineer',
        'devops-engineer', 'end-user', 'customer'
    ],
    'output_dir': 'build/reports/documentation-quality',
    'verbose': False
}

# Results tracking
results = {
    'total_files': 0,
    'files_with_metadata': 0,
    'files_without_metadata': 0,
    'valid_metadata': 0,
    'invalid_metadata': 0,
    'warnings': 0,
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

def extract_front_matter(content: str) -> tuple[Optional[Dict], str]:
    """Extract YAML front matter from markdown content"""
    if not content.startswith('---'):
        return None, content
    
    try:
        # Find the end of front matter
        end_marker = content.find('\n---\n', 3)
        if end_marker == -1:
            end_marker = content.find('\n---', 3)
            if end_marker == -1:
                return None, content
        
        # Extract and parse YAML
        yaml_content = content[3:end_marker].strip()
        metadata = yaml.safe_load(yaml_content)
        
        # Return metadata and remaining content
        remaining_content = content[end_marker + 4:].strip()
        return metadata, remaining_content
        
    except yaml.YAMLError as e:
        log(f"YAML parsing error: {e}", 'error')
        return None, content
    except Exception as e:
        log(f"Front matter extraction error: {e}", 'error')
        return None, content

def determine_document_type(file_path: Path) -> str:
    """Determine document type based on path"""
    path_str = str(file_path).lower()
    
    if 'viewpoints' in path_str:
        return 'viewpoints'
    elif 'perspectives' in path_str:
        return 'perspectives'
    elif 'templates' in path_str:
        return 'templates'
    else:
        return 'general'

def validate_metadata_fields(metadata: Dict, doc_type: str, file_path: Path) -> Dict[str, Any]:
    """Validate metadata fields based on document type"""
    validation_result = {
        'valid': True,
        'errors': [],
        'warnings': [],
        'missing_required': [],
        'invalid_values': [],
        'suggestions': []
    }
    
    # Get required fields for document type
    required_fields = CONFIG['required_fields'].get(doc_type, CONFIG['required_fields']['general'])
    
    # Check required fields
    for field in required_fields:
        if field not in metadata:
            validation_result['missing_required'].append(field)
            validation_result['errors'].append(f"Missing required field: {field}")
            validation_result['valid'] = False
        elif not metadata[field] or (isinstance(metadata[field], str) and not metadata[field].strip()):
            validation_result['errors'].append(f"Empty required field: {field}")
            validation_result['valid'] = False
    
    # Validate specific field values
    if 'viewpoint' in metadata:
        viewpoint = metadata['viewpoint']
        if isinstance(viewpoint, str):
            if viewpoint.lower() not in CONFIG['valid_viewpoints']:
                validation_result['invalid_values'].append(f"Invalid viewpoint: {viewpoint}")
                validation_result['warnings'].append(f"Viewpoint '{viewpoint}' not in standard list")
        elif isinstance(viewpoint, list):
            for vp in viewpoint:
                if vp.lower() not in CONFIG['valid_viewpoints']:
                    validation_result['invalid_values'].append(f"Invalid viewpoint: {vp}")
                    validation_result['warnings'].append(f"Viewpoint '{vp}' not in standard list")
    
    if 'perspective' in metadata:
        perspective = metadata['perspective']
        if isinstance(perspective, str):
            if perspective.lower() not in CONFIG['valid_perspectives']:
                validation_result['invalid_values'].append(f"Invalid perspective: {perspective}")
                validation_result['warnings'].append(f"Perspective '{perspective}' not in standard list")
        elif isinstance(perspective, list):
            for pp in perspective:
                if pp.lower() not in CONFIG['valid_perspectives']:
                    validation_result['invalid_values'].append(f"Invalid perspective: {pp}")
                    validation_result['warnings'].append(f"Perspective '{pp}' not in standard list")
    
    if 'stakeholders' in metadata:
        stakeholders = metadata['stakeholders']
        if isinstance(stakeholders, list):
            for stakeholder in stakeholders:
                if stakeholder.lower() not in CONFIG['valid_stakeholders']:
                    validation_result['warnings'].append(f"Stakeholder '{stakeholder}' not in standard list")
        else:
            validation_result['warnings'].append("Stakeholders should be a list")
    
    # Check for recommended fields
    recommended_fields = ['author', 'version', 'last_updated']
    for field in recommended_fields:
        if field not in metadata:
            validation_result['suggestions'].append(f"Consider adding recommended field: {field}")
    
    # Validate date fields
    date_fields = ['last_updated', 'review_date', 'created_date']
    for field in date_fields:
        if field in metadata:
            try:
                # Try to parse as ISO date
                datetime.fromisoformat(str(metadata[field]).replace('Z', '+00:00'))
            except ValueError:
                validation_result['warnings'].append(f"Invalid date format in {field}: {metadata[field]}")
    
    # Check for cross-references
    if 'related_documents' in metadata:
        related_docs = metadata['related_documents']
        if isinstance(related_docs, list):
            for doc in related_docs:
                if isinstance(doc, str) and doc.endswith('.md'):
                    # Check if referenced document exists
                    doc_path = file_path.parent / doc
                    if not doc_path.exists():
                        # Try relative to docs root
                        docs_root = Path('docs')
                        alt_path = docs_root / doc
                        if not alt_path.exists():
                            validation_result['warnings'].append(f"Referenced document not found: {doc}")
    
    return validation_result

def validate_document_file(file_path: Path) -> Dict[str, Any]:
    """Validate a single document file"""
    file_result = {
        'path': str(file_path),
        'has_metadata': False,
        'metadata_valid': False,
        'document_type': determine_document_type(file_path),
        'metadata': {},
        'validation': {},
        'errors': [],
        'warnings': []
    }
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Extract front matter
        metadata, remaining_content = extract_front_matter(content)
        
        if metadata is None:
            file_result['has_metadata'] = False
            file_result['errors'].append('No YAML front matter found')
            return file_result
        
        file_result['has_metadata'] = True
        file_result['metadata'] = metadata
        
        # Validate metadata
        validation_result = validate_metadata_fields(metadata, file_result['document_type'], file_path)
        file_result['validation'] = validation_result
        file_result['metadata_valid'] = validation_result['valid']
        file_result['errors'].extend(validation_result['errors'])
        file_result['warnings'].extend(validation_result['warnings'])
        
        log(f"Metadata validation: {file_path.name} - {'✅' if validation_result['valid'] else '❌'}", 
            'success' if validation_result['valid'] else 'error')
        
        return file_result
        
    except Exception as e:
        file_result['errors'].append(f'Failed to process file: {str(e)}')
        log(f"Error processing {file_path}: {e}", 'error')
        return file_result

def find_documentation_files(root_dir: str = '.') -> List[Path]:
    """Find all documentation markdown files"""
    doc_files = []
    
    # Focus on key documentation directories
    key_dirs = [
        'docs/viewpoints',
        'docs/perspectives', 
        'docs/templates',
        'docs/api',
        'docs/architecture',
        'docs/design',
        'docs/development',
        'docs/deployment',
        'docs/observability'
    ]
    
    for dir_path in key_dirs:
        dir_full_path = Path(root_dir) / dir_path
        if dir_full_path.exists():
            for md_file in dir_full_path.rglob('*.md'):
                if not any(part.startswith('.') for part in md_file.parts):
                    doc_files.append(md_file)
    
    # Also check root docs directory for important files
    docs_root = Path(root_dir) / 'docs'
    if docs_root.exists():
        for md_file in docs_root.glob('*.md'):
            if md_file.name not in ['README.md']:  # Skip general README
                doc_files.append(md_file)
    
    return sorted(doc_files)

def generate_metadata_template(doc_type: str) -> str:
    """Generate a metadata template for a document type"""
    templates = {
        'viewpoints': """---
title: "[Viewpoint Name] Viewpoint"
viewpoint: "functional"  # functional, information, concurrency, development, deployment, operational
description: "Brief description of this viewpoint"
stakeholders:
  - "architect"
  - "developer"
author: "Architecture Team"
version: "1.0"
last_updated: "2025-01-21"
related_documents:
  - "related-doc.md"
diagrams:
  - "diagram.mmd"
tags:
  - "architecture"
  - "viewpoint"
---""",
        'perspectives': """---
title: "[Perspective Name] Perspective"
perspective: "security"  # security, performance, availability, evolution, usability, regulation, location, cost
description: "Brief description of this perspective"
quality_attributes:
  - "security"
  - "reliability"
author: "Architecture Team"
version: "1.0"
last_updated: "2025-01-21"
related_documents:
  - "related-doc.md"
tags:
  - "architecture"
  - "perspective"
---""",
        'templates': """---
title: "Template Name"
type: "template"
description: "Brief description of this template"
usage: "How to use this template"
author: "Documentation Team"
version: "1.0"
last_updated: "2025-01-21"
tags:
  - "template"
  - "documentation"
---""",
        'general': """---
title: "Document Title"
description: "Brief description of this document"
author: "Team Name"
version: "1.0"
last_updated: "2025-01-21"
tags:
  - "documentation"
---"""
    }
    
    return templates.get(doc_type, templates['general'])

def generate_report():
    """Generate comprehensive metadata validation report"""
    # Calculate summary
    results['summary'] = {
        'total_files': results['total_files'],
        'files_with_metadata': results['files_with_metadata'],
        'files_without_metadata': results['files_without_metadata'],
        'valid_metadata': results['valid_metadata'],
        'invalid_metadata': results['invalid_metadata'],
        'warnings': results['warnings'],
        'metadata_coverage': (results['files_with_metadata'] / results['total_files'] * 100) if results['total_files'] > 0 else 0,
        'metadata_quality': (results['valid_metadata'] / results['files_with_metadata'] * 100) if results['files_with_metadata'] > 0 else 0,
        'timestamp': datetime.now().isoformat()
    }
    
    # Ensure output directory exists
    output_dir = Path(CONFIG['output_dir'])
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Generate JSON report
    json_report_path = output_dir / 'metadata-validation-report.json'
    with open(json_report_path, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    
    # Generate Markdown report
    markdown_report_path = output_dir / 'metadata-validation-report.md'
    
    markdown_content = f"""# Document Metadata Validation Report

**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}  
**Total Files:** {results['total_files']}  
**Files with Metadata:** {results['files_with_metadata']}  
**Files without Metadata:** {results['files_without_metadata']}  
**Valid Metadata:** {results['valid_metadata']}  
**Invalid Metadata:** {results['invalid_metadata']}  
**Warnings:** {results['warnings']}  
**Metadata Coverage:** {results['summary']['metadata_coverage']:.1f}%  
**Metadata Quality:** {results['summary']['metadata_quality']:.1f}%

## Summary by Document Type

| Type | Total | With Metadata | Valid | Coverage | Quality |
|------|-------|---------------|-------|----------|---------|
"""
    
    # Calculate stats by document type
    type_stats = {}
    for file_path, file_result in results['files'].items():
        doc_type = file_result['document_type']
        if doc_type not in type_stats:
            type_stats[doc_type] = {'total': 0, 'with_metadata': 0, 'valid': 0}
        
        type_stats[doc_type]['total'] += 1
        if file_result['has_metadata']:
            type_stats[doc_type]['with_metadata'] += 1
            if file_result['metadata_valid']:
                type_stats[doc_type]['valid'] += 1
    
    for doc_type, stats in type_stats.items():
        coverage = (stats['with_metadata'] / stats['total'] * 100) if stats['total'] > 0 else 0
        quality = (stats['valid'] / stats['with_metadata'] * 100) if stats['with_metadata'] > 0 else 0
        markdown_content += f"| {doc_type} | {stats['total']} | {stats['with_metadata']} | {stats['valid']} | {coverage:.1f}% | {quality:.1f}% |\n"
    
    # Files without metadata
    if results['files_without_metadata'] > 0:
        markdown_content += "\n## Files Without Metadata\n\n"
        for file_path, file_result in results['files'].items():
            if not file_result['has_metadata']:
                doc_type = file_result['document_type']
                markdown_content += f"- **{file_path}** ({doc_type})\n"
                markdown_content += f"  - Template: \n```yaml\n{generate_metadata_template(doc_type)}\n```\n\n"
    
    # Validation errors
    if results['errors']:
        markdown_content += "\n## Validation Errors\n\n"
        for error in results['errors']:
            markdown_content += f"- **{error['file']}**: {error['message']}\n"
    
    # Warnings
    if results['warnings_list']:
        markdown_content += "\n## Warnings\n\n"
        for warning in results['warnings_list']:
            markdown_content += f"- **{warning['file']}**: {warning['message']}\n"
    
    # Recommendations
    markdown_content += "\n## Recommendations\n\n"
    if results['files_without_metadata'] > 0:
        markdown_content += f"1. Add metadata to {results['files_without_metadata']} files without front matter\n"
    if results['invalid_metadata'] > 0:
        markdown_content += f"2. Fix validation errors in {results['invalid_metadata']} files with invalid metadata\n"
    if results['warnings'] > 0:
        markdown_content += f"3. Address {results['warnings']} warnings to improve metadata quality\n"
    
    markdown_content += "4. Use the provided templates to ensure consistent metadata structure\n"
    markdown_content += "5. Regularly validate metadata as part of documentation review process\n"
    
    with open(markdown_report_path, 'w', encoding='utf-8') as f:
        f.write(markdown_content)
    
    return json_report_path, markdown_report_path

def main():
    """Main execution function"""
    parser = argparse.ArgumentParser(description='Validate document metadata')
    parser.add_argument('--verbose', '-v', action='store_true', help='Verbose output')
    parser.add_argument('--output', '-o', help='Output directory for reports')
    parser.add_argument('--generate-templates', action='store_true', help='Generate metadata templates')
    parser.add_argument('path', nargs='?', default='.', help='Path to check (default: current directory)')
    
    args = parser.parse_args()
    
    CONFIG['verbose'] = args.verbose
    if args.output:
        CONFIG['output_dir'] = args.output
    
    if args.generate_templates:
        log("📋 Generating metadata templates...", 'info')
        output_dir = Path(CONFIG['output_dir'])
        output_dir.mkdir(parents=True, exist_ok=True)
        
        for doc_type in CONFIG['required_fields'].keys():
            template_content = generate_metadata_template(doc_type)
            template_path = output_dir / f'metadata-template-{doc_type}.md'
            with open(template_path, 'w', encoding='utf-8') as f:
                f.write(template_content)
            log(f"Generated template: {template_path}", 'success')
        
        return 0
    
    log("📋 Document Metadata Validation Starting...", 'info')
    
    # Find all documentation files
    doc_files = find_documentation_files(args.path)
    results['total_files'] = len(doc_files)
    
    if not doc_files:
        log("No documentation files found", 'warning')
        return 0
    
    log(f"Found {len(doc_files)} documentation files", 'info')
    
    # Process each file
    for file_path in doc_files:
        log(f"Validating: {file_path}", 'info')
        
        file_result = validate_document_file(file_path)
        results['files'][str(file_path)] = file_result
        
        # Update counters
        if file_result['has_metadata']:
            results['files_with_metadata'] += 1
            if file_result['metadata_valid']:
                results['valid_metadata'] += 1
            else:
                results['invalid_metadata'] += 1
        else:
            results['files_without_metadata'] += 1
        
        # Collect errors and warnings
        for error in file_result['errors']:
            results['errors'].append({
                'file': str(file_path),
                'message': error
            })
        
        for warning in file_result['warnings']:
            results['warnings'] += 1
            results['warnings_list'].append({
                'file': str(file_path),
                'message': warning
            })
    
    # Generate reports
    json_report, markdown_report = generate_report()
    
    # Print summary
    log("\n📋 Metadata Validation Complete", 'info')
    log("=" * 30, 'info')
    log(f"Total Files: {results['total_files']}", 'info')
    log(f"Files with Metadata: {results['files_with_metadata']}", 'success')
    log(f"Files without Metadata: {results['files_without_metadata']}", 'warning' if results['files_without_metadata'] > 0 else 'info')
    log(f"Valid Metadata: {results['valid_metadata']}", 'success')
    log(f"Invalid Metadata: {results['invalid_metadata']}", 'error' if results['invalid_metadata'] > 0 else 'info')
    log(f"Warnings: {results['warnings']}", 'warning' if results['warnings'] > 0 else 'info')
    log(f"Metadata Coverage: {results['summary']['metadata_coverage']:.1f}%", 
        'success' if results['summary']['metadata_coverage'] >= 80 else 'warning')
    log(f"Metadata Quality: {results['summary']['metadata_quality']:.1f}%", 
        'success' if results['summary']['metadata_quality'] >= 90 else 'warning')
    
    log(f"\nReports generated:", 'info')
    log(f"- JSON: {json_report}", 'info')
    log(f"- Markdown: {markdown_report}", 'info')
    
    # Return appropriate exit code
    return 0 if results['invalid_metadata'] == 0 and results['files_without_metadata'] == 0 else 1

if __name__ == '__main__':
    sys.exit(main())