#!/usr/bin/env python3
"""
Viewpoint Structure Validation Script
Validates that viewpoint directories follow Rozanski & Woods standards.
"""

import os
import sys
import argparse
from pathlib import Path
from typing import Dict, List, Set

class ViewpointStructureValidator:
    def __init__(self, viewpoint: str):
        self.viewpoint = viewpoint
        self.issues = []
        self.warnings = []
        
        # Define expected structure for development viewpoint
        self.expected_structure = {
            'development': {
                'required_dirs': [
                    'getting-started',
                    'architecture',
                    'coding-standards',
                    'testing',
                    'build-system',
                    'quality-assurance',
                    'tools-and-environment',
                    'workflows'
                ],
                'required_files': [
                    'README.md'
                ],
                'subdirs': {
                    'architecture': {
                        'required_dirs': [
                            'ddd-patterns',
                            'hexagonal-architecture',
                            'microservices',
                            'saga-patterns'
                        ],
                        'required_files': ['README.md']
                    },
                    'testing': {
                        'required_dirs': [
                            'tdd-practices',
                            'bdd-practices'
                        ],
                        'required_files': ['README.md']
                    },
                    'tools-and-environment': {
                        'required_dirs': [
                            'technology-stack'
                        ],
                        'required_files': ['README.md']
                    }
                }
            }
        }
    
    def validate_directory_structure(self, base_path: str) -> Dict:
        """Validate the directory structure against expected standards."""
        print(f"Validating {self.viewpoint} viewpoint structure at: {base_path}")
        
        if self.viewpoint not in self.expected_structure:
            self.issues.append(f"No validation rules defined for viewpoint: {self.viewpoint}")
            return {'valid': False, 'issues': self.issues}
        
        expected = self.expected_structure[self.viewpoint]
        
        # Check if base directory exists
        if not os.path.exists(base_path):
            self.issues.append(f"Base directory does not exist: {base_path}")
            return {'valid': False, 'issues': self.issues}
        
        # Validate top-level structure
        self._validate_level(base_path, expected)
        
        # Validate subdirectories
        if 'subdirs' in expected:
            for subdir, subdir_expected in expected['subdirs'].items():
                subdir_path = os.path.join(base_path, subdir)
                if os.path.exists(subdir_path):
                    self._validate_level(subdir_path, subdir_expected, parent=subdir)
                else:
                    self.issues.append(f"Required subdirectory missing: {subdir}")
        
        # Check for unexpected directories
        self._check_unexpected_items(base_path, expected)
        
        return {
            'valid': len(self.issues) == 0,
            'issues': self.issues,
            'warnings': self.warnings
        }
    
    def _validate_level(self, path: str, expected: Dict, parent: str = None):
        """Validate a single directory level."""
        prefix = f"{parent}/" if parent else ""
        
        # Check required directories
        if 'required_dirs' in expected:
            for req_dir in expected['required_dirs']:
                dir_path = os.path.join(path, req_dir)
                if not os.path.exists(dir_path):
                    self.issues.append(f"Required directory missing: {prefix}{req_dir}")
                elif not os.path.isdir(dir_path):
                    self.issues.append(f"Expected directory but found file: {prefix}{req_dir}")
        
        # Check required files
        if 'required_files' in expected:
            for req_file in expected['required_files']:
                file_path = os.path.join(path, req_file)
                if not os.path.exists(file_path):
                    self.issues.append(f"Required file missing: {prefix}{req_file}")
                elif not os.path.isfile(file_path):
                    self.issues.append(f"Expected file but found directory: {prefix}{req_file}")
                else:
                    # Check if README files have content
                    if req_file == 'README.md':
                        self._validate_readme(file_path, prefix)
    
    def _validate_readme(self, readme_path: str, prefix: str):
        """Validate README file content."""
        try:
            with open(readme_path, 'r', encoding='utf-8') as f:
                content = f.read().strip()
            
            if not content:
                self.issues.append(f"README file is empty: {prefix}README.md")
            elif len(content) < 50:
                self.warnings.append(f"README file is very short: {prefix}README.md")
            
            # Check for basic structure
            if '# ' not in content:
                self.warnings.append(f"README missing main header: {prefix}README.md")
                
        except Exception as e:
            self.issues.append(f"Could not read README file: {prefix}README.md - {e}")
    
    def _check_unexpected_items(self, path: str, expected: Dict):
        """Check for unexpected files or directories."""
        if not os.path.exists(path):
            return
        
        expected_dirs = set(expected.get('required_dirs', []))
        expected_files = set(expected.get('required_files', []))
        
        # Add known optional items
        expected_files.update(['.gitkeep', '.DS_Store'])
        
        actual_items = set(os.listdir(path))
        
        for item in actual_items:
            item_path = os.path.join(path, item)
            
            if os.path.isdir(item_path):
                if item not in expected_dirs and not item.startswith('.'):
                    # Check if it's a subdirectory we know about
                    if 'subdirs' in expected and item in expected['subdirs']:
                        continue
                    self.warnings.append(f"Unexpected directory: {item}")
            else:
                if item not in expected_files and not item.startswith('.'):
                    # Allow markdown files in general
                    if not item.endswith('.md'):
                        self.warnings.append(f"Unexpected file: {item}")
    
    def validate_naming_conventions(self, base_path: str) -> List[str]:
        """Validate naming conventions."""
        naming_issues = []
        
        for root, dirs, files in os.walk(base_path):
            # Check directory names
            for dir_name in dirs:
                if not self._is_valid_directory_name(dir_name):
                    rel_path = os.path.relpath(os.path.join(root, dir_name), base_path)
                    naming_issues.append(f"Invalid directory name: {rel_path}")
            
            # Check file names
            for file_name in files:
                if not self._is_valid_file_name(file_name):
                    rel_path = os.path.relpath(os.path.join(root, file_name), base_path)
                    naming_issues.append(f"Invalid file name: {rel_path}")
        
        return naming_issues
    
    def _is_valid_directory_name(self, name: str) -> bool:
        """Check if directory name follows conventions."""
        # Allow kebab-case and lowercase
        if name.startswith('.'):
            return True  # Hidden directories are OK
        
        # Should be lowercase with hyphens
        return name.islower() and all(c.isalnum() or c in '-_' for c in name)
    
    def _is_valid_file_name(self, name: str) -> bool:
        """Check if file name follows conventions."""
        if name.startswith('.'):
            return True  # Hidden files are OK
        
        # Special cases for standard files
        standard_files = ['README.md', 'CHANGELOG.md', 'LICENSE', 'CONTRIBUTING.md', 'QUICK_START_GUIDE.md']
        if name in standard_files:
            return True
        
        # Allow common file extensions and naming patterns
        allowed_extensions = ['.md', '.txt', '.json', '.yaml', '.yml', '.png', '.svg', '.jpg', '.jpeg']
        
        if any(name.endswith(ext) for ext in allowed_extensions):
            base_name = name.rsplit('.', 1)[0]
            return base_name.islower() and all(c.isalnum() or c in '-_' for c in base_name)
        
        return False
    
    def generate_report(self, validation_result: Dict, output_file: str = None):
        """Generate validation report."""
        report_lines = [
            f"# Viewpoint Structure Validation Report - {self.viewpoint.title()}",
            f"Generated: {os.popen('date').read().strip()}",
            "",
            "## Validation Summary",
            f"- **Status**: {'✅ VALID' if validation_result['valid'] else '❌ INVALID'}",
            f"- **Issues Found**: {len(validation_result['issues'])}",
            f"- **Warnings**: {len(validation_result['warnings'])}",
            ""
        ]
        
        if validation_result['issues']:
            report_lines.extend([
                "## Critical Issues",
                ""
            ])
            for issue in validation_result['issues']:
                report_lines.append(f"- ❌ {issue}")
            report_lines.append("")
        
        if validation_result['warnings']:
            report_lines.extend([
                "## Warnings",
                ""
            ])
            for warning in validation_result['warnings']:
                report_lines.append(f"- ⚠️ {warning}")
            report_lines.append("")
        
        # Add expected structure documentation
        report_lines.extend([
            "## Expected Structure",
            "",
            f"The {self.viewpoint} viewpoint should follow this structure:",
            ""
        ])
        
        if self.viewpoint in self.expected_structure:
            expected = self.expected_structure[self.viewpoint]
            self._add_structure_to_report(report_lines, expected, "")
        
        report_lines.extend([
            "",
            "## Recommendations",
            ""
        ])
        
        if validation_result['issues']:
            report_lines.extend([
                "### Fix Critical Issues",
                "1. Create missing required directories and files",
                "2. Ensure README files have meaningful content",
                "3. Follow naming conventions (lowercase, kebab-case)",
                ""
            ])
        
        if validation_result['warnings']:
            report_lines.extend([
                "### Address Warnings",
                "1. Review unexpected files and directories",
                "2. Consider organizing content better",
                "3. Ensure consistent naming patterns",
                ""
            ])
        
        report_lines.extend([
            "### General Improvements",
            "1. Regular structure validation in CI/CD",
            "2. Documentation templates for consistency",
            "3. Automated structure generation tools",
            ""
        ])
        
        report_content = '\n'.join(report_lines)
        
        if output_file:
            os.makedirs(os.path.dirname(output_file), exist_ok=True)
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write(report_content)
            print(f"Validation report saved to: {output_file}")
        else:
            print(report_content)
    
    def _add_structure_to_report(self, report_lines: List[str], structure: Dict, indent: str):
        """Add structure information to report."""
        if 'required_dirs' in structure:
            for dir_name in structure['required_dirs']:
                report_lines.append(f"{indent}- 📁 {dir_name}/")
                
                # Add subdirectory structure if exists
                if 'subdirs' in structure and dir_name in structure['subdirs']:
                    substructure = structure['subdirs'][dir_name]
                    self._add_structure_to_report(report_lines, substructure, indent + "  ")
        
        if 'required_files' in structure:
            for file_name in structure['required_files']:
                report_lines.append(f"{indent}- 📄 {file_name}")

def main():
    parser = argparse.ArgumentParser(description='Validate viewpoint directory structure')
    parser.add_argument('--viewpoint', required=True, help='Viewpoint to validate (e.g., development)')
    parser.add_argument('--output', help='Output file for report')
    
    args = parser.parse_args()
    
    # Determine directory based on viewpoint
    viewpoint_dir = f"docs/viewpoints/{args.viewpoint}"
    
    validator = ViewpointStructureValidator(args.viewpoint)
    validation_result = validator.validate_directory_structure(viewpoint_dir)
    
    # Also check naming conventions
    naming_issues = validator.validate_naming_conventions(viewpoint_dir)
    if naming_issues:
        validation_result['issues'].extend(naming_issues)
        validation_result['valid'] = False
    
    output_file = args.output
    if not output_file:
        timestamp = os.popen('date +%Y%m%d_%H%M%S').read().strip()
        output_file = f"reports-summaries/quality-ux/viewpoint-structure-validation-{args.viewpoint}-{timestamp}.md"
    
    validator.generate_report(validation_result, output_file)
    
    # Exit with appropriate code
    if not validation_result['valid']:
        print(f"\n❌ Viewpoint structure validation failed")
        sys.exit(1)
    elif validation_result['warnings']:
        print(f"\n⚠️  Viewpoint structure has warnings")
        sys.exit(0)
    else:
        print(f"\n✅ Viewpoint structure is valid")
        sys.exit(0)

if __name__ == '__main__':
    main()