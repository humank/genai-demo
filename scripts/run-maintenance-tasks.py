#!/usr/bin/env python3
"""
Maintenance Tasks Runner
Runs automated maintenance tasks for viewpoint documentation.
"""

import os
import sys
import argparse
import subprocess
import json
from datetime import datetime
from typing import Dict, List
import shutil

class MaintenanceTaskRunner:
    def __init__(self, viewpoint: str):
        self.viewpoint = viewpoint
        self.results = {
            'timestamp': datetime.now().isoformat(),
            'viewpoint': viewpoint,
            'tasks': {}
        }
    
    def run_cleanup_tasks(self, viewpoint_dir: str) -> Dict:
        """Run cleanup maintenance tasks."""
        print("Running cleanup tasks...")
        
        cleanup_results = {
            'status': 'success',
            'actions': [],
            'errors': []
        }
        
        try:
            # 1. Remove empty directories
            empty_dirs = self._find_empty_directories(viewpoint_dir)
            for empty_dir in empty_dirs:
                try:
                    os.rmdir(empty_dir)
                    cleanup_results['actions'].append(f"Removed empty directory: {empty_dir}")
                except Exception as e:
                    cleanup_results['errors'].append(f"Could not remove {empty_dir}: {e}")
            
            # 2. Clean up temporary files
            temp_files = self._find_temp_files(viewpoint_dir)
            for temp_file in temp_files:
                try:
                    os.remove(temp_file)
                    cleanup_results['actions'].append(f"Removed temporary file: {temp_file}")
                except Exception as e:
                    cleanup_results['errors'].append(f"Could not remove {temp_file}: {e}")
            
            # 3. Fix file permissions
            permission_fixes = self._fix_file_permissions(viewpoint_dir)
            cleanup_results['actions'].extend(permission_fixes)
            
            # 4. Normalize line endings
            line_ending_fixes = self._normalize_line_endings(viewpoint_dir)
            cleanup_results['actions'].extend(line_ending_fixes)
            
        except Exception as e:
            cleanup_results['status'] = 'error'
            cleanup_results['errors'].append(f"Cleanup task failed: {e}")
        
        return cleanup_results
    
    def run_update_tasks(self, viewpoint_dir: str) -> Dict:
        """Run update maintenance tasks."""
        print("Running update tasks...")
        
        update_results = {
            'status': 'success',
            'actions': [],
            'errors': []
        }
        
        try:
            # 1. Update README files with missing content
            readme_updates = self._update_readme_files(viewpoint_dir)
            update_results['actions'].extend(readme_updates)
            
            # 2. Update last modified timestamps in frontmatter
            timestamp_updates = self._update_timestamps(viewpoint_dir)
            update_results['actions'].extend(timestamp_updates)
            
            # 3. Fix common formatting issues
            formatting_fixes = self._fix_formatting_issues(viewpoint_dir)
            update_results['actions'].extend(formatting_fixes)
            
            # 4. Update cross-references
            cross_ref_updates = self._update_cross_references(viewpoint_dir)
            update_results['actions'].extend(cross_ref_updates)
            
        except Exception as e:
            update_results['status'] = 'error'
            update_results['errors'].append(f"Update task failed: {e}")
        
        return update_results
    
    def run_validation_tasks(self, viewpoint_dir: str) -> Dict:
        """Run validation maintenance tasks."""
        print("Running validation tasks...")
        
        validation_results = {
            'status': 'success',
            'actions': [],
            'errors': [],
            'warnings': []
        }
        
        try:
            # 1. Validate markdown syntax
            markdown_issues = self._validate_markdown_syntax(viewpoint_dir)
            if markdown_issues:
                validation_results['warnings'].extend(markdown_issues)
            
            # 2. Validate internal links
            link_issues = self._validate_internal_links(viewpoint_dir)
            if link_issues:
                validation_results['errors'].extend(link_issues)
            
            # 3. Validate image references
            image_issues = self._validate_image_references(viewpoint_dir)
            if image_issues:
                validation_results['warnings'].extend(image_issues)
            
            # 4. Check for duplicate content
            duplicate_issues = self._check_duplicate_content(viewpoint_dir)
            if duplicate_issues:
                validation_results['warnings'].extend(duplicate_issues)
            
        except Exception as e:
            validation_results['status'] = 'error'
            validation_results['errors'].append(f"Validation task failed: {e}")
        
        return validation_results
    
    def _find_empty_directories(self, directory: str) -> List[str]:
        """Find empty directories."""
        empty_dirs = []
        
        for root, dirs, files in os.walk(directory, topdown=False):
            # Skip certain directories
            if any(skip in root for skip in ['.git', '.kiro', 'node_modules']):
                continue
            
            for dir_name in dirs:
                dir_path = os.path.join(root, dir_name)
                try:
                    if not os.listdir(dir_path):
                        empty_dirs.append(dir_path)
                except:
                    continue
        
        return empty_dirs
    
    def _find_temp_files(self, directory: str) -> List[str]:
        """Find temporary files."""
        temp_files = []
        temp_patterns = ['.DS_Store', 'Thumbs.db', '*.tmp', '*.bak', '*~']
        
        for root, dirs, files in os.walk(directory):
            dirs[:] = [d for d in dirs if d not in ['.git', '.kiro', 'node_modules']]
            
            for file in files:
                if any(file.endswith(pattern.replace('*', '')) or file == pattern for pattern in temp_patterns):
                    temp_files.append(os.path.join(root, file))
        
        return temp_files
    
    def _fix_file_permissions(self, directory: str) -> List[str]:
        """Fix file permissions."""
        actions = []
        
        for root, dirs, files in os.walk(directory):
            dirs[:] = [d for d in dirs if d not in ['.git', '.kiro', 'node_modules']]
            
            for file in files:
                if file.endswith('.md'):
                    file_path = os.path.join(root, file)
                    try:
                        # Ensure markdown files are readable
                        current_mode = os.stat(file_path).st_mode
                        if not (current_mode & 0o444):  # Not readable
                            os.chmod(file_path, 0o644)
                            actions.append(f"Fixed permissions for: {file_path}")
                    except:
                        continue
        
        return actions
    
    def _normalize_line_endings(self, directory: str) -> List[str]:
        """Normalize line endings to Unix format."""
        actions = []
        
        for root, dirs, files in os.walk(directory):
            dirs[:] = [d for d in dirs if d not in ['.git', '.kiro', 'node_modules']]
            
            for file in files:
                if file.endswith('.md'):
                    file_path = os.path.join(root, file)
                    try:
                        with open(file_path, 'rb') as f:
                            content = f.read()
                        
                        # Check if file has Windows line endings
                        if b'\r\n' in content:
                            # Convert to Unix line endings
                            content = content.replace(b'\r\n', b'\n')
                            with open(file_path, 'wb') as f:
                                f.write(content)
                            actions.append(f"Normalized line endings: {file_path}")
                    except:
                        continue
        
        return actions
    
    def _update_readme_files(self, directory: str) -> List[str]:
        """Update README files with missing content."""
        actions = []
        
        for root, dirs, files in os.walk(directory):
            dirs[:] = [d for d in dirs if d not in ['.git', '.kiro', 'node_modules']]
            
            if 'README.md' in files:
                readme_path = os.path.join(root, 'README.md')
                try:
                    with open(readme_path, 'r', encoding='utf-8') as f:
                        content = f.read().strip()
                    
                    if not content or len(content) < 50:
                        # Generate basic README content
                        dir_name = os.path.basename(root)
                        new_content = self._generate_readme_content(dir_name, root, directory)
                        
                        with open(readme_path, 'w', encoding='utf-8') as f:
                            f.write(new_content)
                        
                        actions.append(f"Updated README: {readme_path}")
                except:
                    continue
        
        return actions
    
    def _generate_readme_content(self, dir_name: str, dir_path: str, base_dir: str) -> str:
        """Generate basic README content for a directory."""
        relative_path = os.path.relpath(dir_path, base_dir)
        
        # Get list of files in directory
        try:
            files = [f for f in os.listdir(dir_path) if f.endswith('.md') and f != 'README.md']
            subdirs = [d for d in os.listdir(dir_path) if os.path.isdir(os.path.join(dir_path, d))]
        except:
            files = []
            subdirs = []
        
        content_lines = [
            f"# {dir_name.replace('-', ' ').title()}",
            "",
            f"This section contains documentation for {dir_name.replace('-', ' ')} in the {self.viewpoint} viewpoint.",
            ""
        ]
        
        if files:
            content_lines.extend([
                "## Documentation",
                ""
            ])
            for file in sorted(files):
                file_title = file.replace('.md', '').replace('-', ' ').title()
                content_lines.append(f"- [{file_title}]({file})")
            content_lines.append("")
        
        if subdirs:
            content_lines.extend([
                "## Subsections",
                ""
            ])
            for subdir in sorted(subdirs):
                subdir_title = subdir.replace('-', ' ').title()
                content_lines.append(f"- [{subdir_title}]({subdir}/)")
            content_lines.append("")
        
        content_lines.extend([
            "## Related Documentation",
            "",
            f"- [Development Viewpoint Overview](../../README.md)",
            f"- [Architecture Documentation](../architecture/README.md)",
            ""
        ])
        
        return '\n'.join(content_lines)
    
    def _update_timestamps(self, directory: str) -> List[str]:
        """Update last modified timestamps."""
        actions = []
        
        for root, dirs, files in os.walk(directory):
            dirs[:] = [d for d in dirs if d not in ['.git', '.kiro', 'node_modules']]
            
            for file in files:
                if file.endswith('.md'):
                    file_path = os.path.join(root, file)
                    try:
                        with open(file_path, 'r', encoding='utf-8') as f:
                            content = f.read()
                        
                        # Check if file has frontmatter
                        if content.startswith('---\n'):
                            # Update last_modified in frontmatter
                            lines = content.split('\n')
                            frontmatter_end = -1
                            
                            for i, line in enumerate(lines[1:], 1):
                                if line.strip() == '---':
                                    frontmatter_end = i
                                    break
                            
                            if frontmatter_end > 0:
                                # Update or add last_modified
                                current_time = datetime.now().isoformat()
                                updated = False
                                
                                for i in range(1, frontmatter_end):
                                    if lines[i].startswith('last_modified:'):
                                        lines[i] = f'last_modified: {current_time}'
                                        updated = True
                                        break
                                
                                if not updated:
                                    lines.insert(frontmatter_end, f'last_modified: {current_time}')
                                
                                new_content = '\n'.join(lines)
                                with open(file_path, 'w', encoding='utf-8') as f:
                                    f.write(new_content)
                                
                                actions.append(f"Updated timestamp: {file_path}")
                    except:
                        continue
        
        return actions
    
    def _fix_formatting_issues(self, directory: str) -> List[str]:
        """Fix common formatting issues."""
        actions = []
        
        for root, dirs, files in os.walk(directory):
            dirs[:] = [d for d in dirs if d not in ['.git', '.kiro', 'node_modules']]
            
            for file in files:
                if file.endswith('.md'):
                    file_path = os.path.join(root, file)
                    try:
                        with open(file_path, 'r', encoding='utf-8') as f:
                            content = f.read()
                        
                        original_content = content
                        
                        # Fix common issues
                        # 1. Multiple consecutive blank lines
                        content = re.sub(r'\n{3,}', '\n\n', content)
                        
                        # 2. Trailing whitespace
                        lines = content.split('\n')
                        lines = [line.rstrip() for line in lines]
                        content = '\n'.join(lines)
                        
                        # 3. Ensure file ends with newline
                        if content and not content.endswith('\n'):
                            content += '\n'
                        
                        if content != original_content:
                            with open(file_path, 'w', encoding='utf-8') as f:
                                f.write(content)
                            actions.append(f"Fixed formatting: {file_path}")
                    except:
                        continue
        
        return actions
    
    def _update_cross_references(self, directory: str) -> List[str]:
        """Update cross-references between documents."""
        actions = []
        # This is a placeholder for more sophisticated cross-reference updating
        # Could be implemented to automatically update links when files are moved
        return actions
    
    def _validate_markdown_syntax(self, directory: str) -> List[str]:
        """Validate markdown syntax."""
        issues = []
        
        for root, dirs, files in os.walk(directory):
            dirs[:] = [d for d in dirs if d not in ['.git', '.kiro', 'node_modules']]
            
            for file in files:
                if file.endswith('.md'):
                    file_path = os.path.join(root, file)
                    try:
                        with open(file_path, 'r', encoding='utf-8') as f:
                            content = f.read()
                        
                        # Basic syntax checks
                        lines = content.split('\n')
                        for i, line in enumerate(lines, 1):
                            # Check for unmatched brackets
                            if line.count('[') != line.count(']'):
                                issues.append(f"{file_path}:{i} - Unmatched brackets")
                            
                            # Check for unmatched parentheses in links
                            if '[' in line and '](' in line:
                                bracket_count = line.count('(') - line.count(')')
                                if bracket_count != 0:
                                    issues.append(f"{file_path}:{i} - Unmatched parentheses in link")
                    except:
                        continue
        
        return issues
    
    def _validate_internal_links(self, directory: str) -> List[str]:
        """Validate internal links."""
        issues = []
        
        for root, dirs, files in os.walk(directory):
            dirs[:] = [d for d in dirs if d not in ['.git', '.kiro', 'node_modules']]
            
            for file in files:
                if file.endswith('.md'):
                    file_path = os.path.join(root, file)
                    try:
                        with open(file_path, 'r', encoding='utf-8') as f:
                            content = f.read()
                        
                        # Find markdown links
                        import re
                        links = re.findall(r'\[([^\]]+)\]\(([^)]+)\)', content)
                        
                        for link_text, link_url in links:
                            if not link_url.startswith('http') and not link_url.startswith('#'):
                                # Check if internal link exists
                                if link_url.startswith('./'):
                                    target_path = os.path.join(os.path.dirname(file_path), link_url[2:])
                                elif link_url.startswith('../'):
                                    target_path = os.path.normpath(os.path.join(os.path.dirname(file_path), link_url))
                                else:
                                    target_path = os.path.join(directory, link_url)
                                
                                if not os.path.exists(target_path):
                                    issues.append(f"{file_path} - Broken link: {link_url}")
                    except:
                        continue
        
        return issues
    
    def _validate_image_references(self, directory: str) -> List[str]:
        """Validate image references."""
        issues = []
        
        for root, dirs, files in os.walk(directory):
            dirs[:] = [d for d in dirs if d not in ['.git', '.kiro', 'node_modules']]
            
            for file in files:
                if file.endswith('.md'):
                    file_path = os.path.join(root, file)
                    try:
                        with open(file_path, 'r', encoding='utf-8') as f:
                            content = f.read()
                        
                        # Find image references
                        import re
                        images = re.findall(r'!\[([^\]]*)\]\(([^)]+)\)', content)
                        
                        for alt_text, img_url in images:
                            if not img_url.startswith('http'):
                                # Check if image exists
                                if img_url.startswith('./'):
                                    target_path = os.path.join(os.path.dirname(file_path), img_url[2:])
                                elif img_url.startswith('../'):
                                    target_path = os.path.normpath(os.path.join(os.path.dirname(file_path), img_url))
                                else:
                                    target_path = os.path.join(directory, img_url)
                                
                                if not os.path.exists(target_path):
                                    issues.append(f"{file_path} - Missing image: {img_url}")
                    except:
                        continue
        
        return issues
    
    def _check_duplicate_content(self, directory: str) -> List[str]:
        """Check for duplicate content (simplified)."""
        issues = []
        # This is a simplified version - could use the full duplication detector
        return issues
    
    def run_maintenance(self, viewpoint_dir: str, tasks: List[str]) -> Dict:
        """Run specified maintenance tasks."""
        print(f"Running maintenance tasks for {self.viewpoint} viewpoint: {', '.join(tasks)}")
        
        if 'cleanup' in tasks:
            self.results['tasks']['cleanup'] = self.run_cleanup_tasks(viewpoint_dir)
        
        if 'update' in tasks:
            self.results['tasks']['update'] = self.run_update_tasks(viewpoint_dir)
        
        if 'validate' in tasks:
            self.results['tasks']['validate'] = self.run_validation_tasks(viewpoint_dir)
        
        return self.results
    
    def generate_report(self, results: Dict, output_file: str = None):
        """Generate maintenance report."""
        report_lines = [
            f"# Maintenance Tasks Report - {self.viewpoint.title()} Viewpoint",
            f"Generated: {results['timestamp']}",
            "",
            "## Summary",
            f"- **Tasks Run**: {', '.join(results['tasks'].keys())}",
            ""
        ]
        
        # Task results
        for task_name, task_result in results['tasks'].items():
            report_lines.extend([
                f"## {task_name.title()} Task Results",
                f"**Status**: {'✅ Success' if task_result['status'] == 'success' else '❌ Failed'}",
                ""
            ])
            
            if task_result.get('actions'):
                report_lines.extend([
                    "### Actions Performed",
                    ""
                ])
                for action in task_result['actions']:
                    report_lines.append(f"- {action}")
                report_lines.append("")
            
            if task_result.get('errors'):
                report_lines.extend([
                    "### Errors",
                    ""
                ])
                for error in task_result['errors']:
                    report_lines.append(f"- ❌ {error}")
                report_lines.append("")
            
            if task_result.get('warnings'):
                report_lines.extend([
                    "### Warnings",
                    ""
                ])
                for warning in task_result['warnings']:
                    report_lines.append(f"- ⚠️ {warning}")
                report_lines.append("")
        
        report_lines.extend([
            "## Next Steps",
            "",
            "1. Review any errors or warnings above",
            "2. Schedule next maintenance run",
            "3. Monitor documentation quality metrics",
            ""
        ])
        
        report_content = '\n'.join(report_lines)
        
        if output_file:
            os.makedirs(os.path.dirname(output_file), exist_ok=True)
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write(report_content)
            
            # Save raw results as JSON
            json_file = output_file.replace('.md', '.json')
            with open(json_file, 'w', encoding='utf-8') as f:
                json.dump(results, f, indent=2)
            
            print(f"Maintenance report saved to: {output_file}")
            print(f"Raw results saved to: {json_file}")
        else:
            print(report_content)

def main():
    parser = argparse.ArgumentParser(description='Run maintenance tasks for viewpoint documentation')
    parser.add_argument('--viewpoint', required=True, help='Viewpoint to maintain (e.g., development)')
    parser.add_argument('--tasks', required=True, help='Comma-separated list of tasks: cleanup,update,validate')
    parser.add_argument('--output', help='Output file for report')
    
    args = parser.parse_args()
    
    viewpoint_dir = f"docs/viewpoints/{args.viewpoint}"
    if not os.path.exists(viewpoint_dir):
        print(f"Error: Viewpoint directory '{viewpoint_dir}' does not exist")
        sys.exit(1)
    
    tasks = [task.strip() for task in args.tasks.split(',')]
    valid_tasks = ['cleanup', 'update', 'validate']
    
    for task in tasks:
        if task not in valid_tasks:
            print(f"Error: Invalid task '{task}'. Valid tasks: {', '.join(valid_tasks)}")
            sys.exit(1)
    
    runner = MaintenanceTaskRunner(args.viewpoint)
    results = runner.run_maintenance(viewpoint_dir, tasks)
    
    output_file = args.output
    if not output_file:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        output_file = f"reports-summaries/task-execution/maintenance-tasks-{args.viewpoint}-{timestamp}.md"
    
    runner.generate_report(results, output_file)
    
    # Exit based on results
    has_errors = any(task.get('errors') for task in results['tasks'].values())
    if has_errors:
        print(f"\n❌ Maintenance completed with errors")
        sys.exit(1)
    else:
        print(f"\n✅ Maintenance completed successfully")
        sys.exit(0)

if __name__ == '__main__':
    main()