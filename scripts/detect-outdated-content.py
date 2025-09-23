#!/usr/bin/env python3
"""
Outdated Content Detection Script
Detects outdated content in documentation based on modification dates and code changes.
"""

import os
import sys
import argparse
import subprocess
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Tuple
import re

class OutdatedContentDetector:
    def __init__(self, viewpoint: str, threshold_days: int = 30):
        self.viewpoint = viewpoint
        self.threshold_days = threshold_days
        self.threshold_date = datetime.now() - timedelta(days=threshold_days)
        self.outdated_files = []
        self.stale_references = []
        
    def get_file_last_modified(self, file_path: str) -> datetime:
        """Get last modification date of a file."""
        try:
            timestamp = os.path.getmtime(file_path)
            return datetime.fromtimestamp(timestamp)
        except:
            return datetime.min
    
    def get_git_last_modified(self, file_path: str) -> datetime:
        """Get last modification date from git history."""
        try:
            result = subprocess.run([
                'git', 'log', '-1', '--format=%ct', file_path
            ], capture_output=True, text=True, cwd='.')
            
            if result.returncode == 0 and result.stdout.strip():
                timestamp = int(result.stdout.strip())
                return datetime.fromtimestamp(timestamp)
        except:
            pass
        
        return self.get_file_last_modified(file_path)
    
    def analyze_java_code_changes(self) -> Dict[str, datetime]:
        """Analyze recent changes in Java code that might affect documentation."""
        java_changes = {}
        
        try:
            # Get Java files changed in the last threshold period
            since_date = self.threshold_date.strftime('%Y-%m-%d')
            result = subprocess.run([
                'git', 'log', '--since', since_date, '--name-only', '--pretty=format:', 
                '--', 'app/src/main/java/**/*.java'
            ], capture_output=True, text=True, cwd='.')
            
            if result.returncode == 0:
                changed_files = [f.strip() for f in result.stdout.split('\n') if f.strip().endswith('.java')]
                
                for java_file in changed_files:
                    if os.path.exists(java_file):
                        last_modified = self.get_git_last_modified(java_file)
                        java_changes[java_file] = last_modified
        except Exception as e:
            print(f"Warning: Could not analyze Java changes: {e}")
        
        return java_changes
    
    def analyze_feature_file_changes(self) -> Dict[str, datetime]:
        """Analyze recent changes in BDD feature files."""
        feature_changes = {}
        
        try:
            since_date = self.threshold_date.strftime('%Y-%m-%d')
            result = subprocess.run([
                'git', 'log', '--since', since_date, '--name-only', '--pretty=format:', 
                '--', 'app/src/test/resources/features/**/*.feature'
            ], capture_output=True, text=True, cwd='.')
            
            if result.returncode == 0:
                changed_files = [f.strip() for f in result.stdout.split('\n') if f.strip().endswith('.feature')]
                
                for feature_file in changed_files:
                    if os.path.exists(feature_file):
                        last_modified = self.get_git_last_modified(feature_file)
                        feature_changes[feature_file] = last_modified
        except Exception as e:
            print(f"Warning: Could not analyze feature file changes: {e}")
        
        return feature_changes
    
    def find_related_documentation(self, java_file: str) -> List[str]:
        """Find documentation files that might be related to a Java file."""
        related_docs = []
        
        # Extract class name and package
        try:
            with open(java_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Find class name
            class_match = re.search(r'public\s+class\s+(\w+)', content)
            if class_match:
                class_name = class_match.group(1)
                
                # Look for documentation mentioning this class
                viewpoint_dir = f"docs/viewpoints/{self.viewpoint}"
                if os.path.exists(viewpoint_dir):
                    for root, dirs, files in os.walk(viewpoint_dir):
                        for file in files:
                            if file.endswith('.md'):
                                doc_path = os.path.join(root, file)
                                try:
                                    with open(doc_path, 'r', encoding='utf-8') as doc_f:
                                        doc_content = doc_f.read()
                                    
                                    if class_name.lower() in doc_content.lower():
                                        related_docs.append(doc_path)
                                except:
                                    continue
        except:
            pass
        
        return related_docs
    
    def detect_outdated_content(self, viewpoint_dir: str) -> Dict:
        """Detect outdated content in the viewpoint directory."""
        print(f"Detecting outdated content in: {viewpoint_dir}")
        
        outdated_files = []
        stale_references = []
        
        # Analyze code changes
        java_changes = self.analyze_java_code_changes()
        feature_changes = self.analyze_feature_file_changes()
        
        # Scan documentation files
        for root, dirs, files in os.walk(viewpoint_dir):
            dirs[:] = [d for d in dirs if d not in ['.git', '.kiro', 'node_modules', 'generated']]
            
            for file in files:
                if file.endswith('.md'):
                    file_path = os.path.join(root, file)
                    relative_path = os.path.relpath(file_path, viewpoint_dir)
                    
                    last_modified = self.get_git_last_modified(file_path)
                    
                    # Check if file is outdated
                    if last_modified < self.threshold_date:
                        # Check if related code has changed
                        related_code_changed = False
                        
                        try:
                            with open(file_path, 'r', encoding='utf-8') as f:
                                content = f.read()
                            
                            # Look for Java class references
                            java_classes = re.findall(r'@(\w+Root|\w+Object|\w+Service|\w+Entity)', content)
                            java_classes.extend(re.findall(r'class\s+(\w+)', content))
                            
                            for java_file, change_date in java_changes.items():
                                java_content = ""
                                try:
                                    with open(java_file, 'r', encoding='utf-8') as jf:
                                        java_content = jf.read()
                                except:
                                    continue
                                
                                for class_name in java_classes:
                                    if class_name in java_content:
                                        related_code_changed = True
                                        break
                                
                                if related_code_changed:
                                    break
                            
                        except:
                            pass
                        
                        outdated_files.append({
                            'file': relative_path,
                            'last_modified': last_modified.isoformat(),
                            'days_old': (datetime.now() - last_modified).days,
                            'related_code_changed': related_code_changed,
                            'priority': 'high' if related_code_changed else 'medium'
                        })
        
        # Check for stale references to moved or deleted files
        stale_references = self.find_stale_references(viewpoint_dir)
        
        return {
            'outdated_files': outdated_files,
            'stale_references': stale_references,
            'java_changes': len(java_changes),
            'feature_changes': len(feature_changes),
            'threshold_days': self.threshold_days
        }
    
    def find_stale_references(self, viewpoint_dir: str) -> List[Dict]:
        """Find references to files that no longer exist."""
        stale_refs = []
        
        for root, dirs, files in os.walk(viewpoint_dir):
            dirs[:] = [d for d in dirs if d not in ['.git', '.kiro', 'node_modules', 'generated']]
            
            for file in files:
                if file.endswith('.md'):
                    file_path = os.path.join(root, file)
                    
                    try:
                        with open(file_path, 'r', encoding='utf-8') as f:
                            content = f.read()
                        
                        # Find markdown links
                        links = re.findall(r'\[([^\]]+)\]\(([^)]+)\)', content)
                        
                        for link_text, link_url in links:
                            if not link_url.startswith('http') and not link_url.startswith('#'):
                                # Resolve relative path
                                if link_url.startswith('./'):
                                    link_url = link_url[2:]
                                elif link_url.startswith('../'):
                                    # Handle relative paths
                                    base_dir = os.path.dirname(file_path)
                                    target_path = os.path.normpath(os.path.join(base_dir, link_url))
                                else:
                                    # Assume relative to project root
                                    target_path = link_url
                                
                                if not os.path.exists(target_path):
                                    stale_refs.append({
                                        'file': os.path.relpath(file_path, viewpoint_dir),
                                        'link_text': link_text,
                                        'link_url': link_url,
                                        'target_path': target_path
                                    })
                    except:
                        continue
        
        return stale_refs
    
    def generate_report(self, analysis: Dict, output_file: str = None):
        """Generate outdated content report."""
        outdated_files = analysis['outdated_files']
        stale_references = analysis['stale_references']
        
        report_lines = [
            f"# Outdated Content Detection Report - {self.viewpoint.title()} Viewpoint",
            f"Generated: {datetime.now().isoformat()}",
            f"Threshold: {self.threshold_days} days",
            "",
            "## Summary",
            f"- **Outdated Files**: {len(outdated_files)}",
            f"- **High Priority** (related code changed): {len([f for f in outdated_files if f['priority'] == 'high'])}",
            f"- **Medium Priority**: {len([f for f in outdated_files if f['priority'] == 'medium'])}",
            f"- **Stale References**: {len(stale_references)}",
            f"- **Recent Java Changes**: {analysis['java_changes']}",
            f"- **Recent Feature Changes**: {analysis['feature_changes']}",
            ""
        ]
        
        if outdated_files:
            report_lines.extend([
                "## Outdated Files",
                ""
            ])
            
            # Sort by priority and age
            sorted_files = sorted(outdated_files, key=lambda x: (x['priority'] == 'medium', x['days_old']), reverse=True)
            
            for file_info in sorted_files:
                priority_icon = "🔴" if file_info['priority'] == 'high' else "🟡"
                code_changed = " (Related code changed)" if file_info['related_code_changed'] else ""
                
                report_lines.extend([
                    f"### {priority_icon} {file_info['file']}",
                    f"- **Last Modified**: {file_info['last_modified']}",
                    f"- **Days Old**: {file_info['days_old']}",
                    f"- **Priority**: {file_info['priority'].title()}{code_changed}",
                    ""
                ])
        
        if stale_references:
            report_lines.extend([
                "## Stale References",
                ""
            ])
            
            for ref in stale_references:
                report_lines.extend([
                    f"### {ref['file']}",
                    f"- **Link Text**: {ref['link_text']}",
                    f"- **Broken URL**: `{ref['link_url']}`",
                    f"- **Target Path**: `{ref['target_path']}`",
                    ""
                ])
        
        if not outdated_files and not stale_references:
            report_lines.extend([
                "## ✅ No Issues Found",
                "All content appears to be up-to-date and references are valid.",
                ""
            ])
        
        report_lines.extend([
            "## Recommendations",
            ""
        ])
        
        if outdated_files:
            high_priority = [f for f in outdated_files if f['priority'] == 'high']
            if high_priority:
                report_lines.extend([
                    "### High Priority Actions",
                    "1. **Review High Priority Files**: Update documentation for files with related code changes",
                    "2. **Verify Accuracy**: Ensure documentation reflects current implementation",
                    "3. **Update Examples**: Refresh code examples and usage patterns",
                    ""
                ])
            
            report_lines.extend([
                "### General Maintenance",
                "1. **Regular Reviews**: Schedule monthly reviews of documentation",
                "2. **Automated Updates**: Consider automated documentation generation where possible",
                "3. **Version Control**: Use git hooks to remind about documentation updates",
                ""
            ])
        
        if stale_references:
            report_lines.extend([
                "### Fix Broken Links",
                "1. **Update References**: Fix or remove broken links",
                "2. **Redirect Pages**: Create redirect pages for moved content",
                "3. **Link Validation**: Implement automated link checking",
                ""
            ])
        
        report_content = '\n'.join(report_lines)
        
        if output_file:
            os.makedirs(os.path.dirname(output_file), exist_ok=True)
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write(report_content)
            print(f"Outdated content report saved to: {output_file}")
        else:
            print(report_content)

def main():
    parser = argparse.ArgumentParser(description='Detect outdated content in documentation')
    parser.add_argument('--viewpoint', required=True, help='Viewpoint to analyze (e.g., development)')
    parser.add_argument('--threshold', type=int, default=30, help='Days threshold for outdated content')
    parser.add_argument('--output', help='Output file for report')
    
    args = parser.parse_args()
    
    viewpoint_dir = f"docs/viewpoints/{args.viewpoint}"
    if not os.path.exists(viewpoint_dir):
        print(f"Error: Viewpoint directory '{viewpoint_dir}' does not exist")
        sys.exit(1)
    
    detector = OutdatedContentDetector(args.viewpoint, args.threshold)
    analysis = detector.detect_outdated_content(viewpoint_dir)
    
    output_file = args.output
    if not output_file:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        output_file = f"reports-summaries/task-execution/outdated-content-{args.viewpoint}-{timestamp}.md"
    
    detector.generate_report(analysis, output_file)
    
    # Exit with appropriate code
    outdated_count = len(analysis['outdated_files'])
    stale_count = len(analysis['stale_references'])
    
    if outdated_count > 10 or stale_count > 5:
        print(f"\n❌ Many outdated items found: {outdated_count} files, {stale_count} stale references")
        sys.exit(2)
    elif outdated_count > 0 or stale_count > 0:
        print(f"\n⚠️  Some outdated items found: {outdated_count} files, {stale_count} stale references")
        sys.exit(1)
    else:
        print(f"\n✅ All content appears up-to-date")
        sys.exit(0)

if __name__ == '__main__':
    main()