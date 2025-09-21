#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Reports and Summaries Organization Script
Moves all summary and report files to a centralized directory structure
"""

import os
import shutil
from pathlib import Path
from typing import List, Tuple

class ReportSummaryOrganizer:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.target_dir = self.project_root / "reports-summaries"
        self.moved_files = []
        self.skipped_files = []
        
    def is_report_or_summary_file(self, file_path: Path) -> bool:
        """Check if a file is a report or summary file"""
        name = file_path.name.lower()
        return (
            'summary' in name or 
            'report' in name or
            name.endswith('_summary.md') or
            name.endswith('_report.md') or
            name.endswith('-summary.md') or
            name.endswith('-report.md')
        )
    
    def get_category_from_path(self, file_path: Path) -> str:
        """Determine the category based on file path and name"""
        path_str = str(file_path).lower()
        name = file_path.name.lower()
        
        # Task execution reports (from our work)
        if any(keyword in name for keyword in ['task', 'execution', 'completion', 'hook', 'automation']):
            return 'task-execution'
        
        # Architecture and design reports
        if any(keyword in path_str for keyword in ['architecture', 'adr', 'design', 'ddd', 'hexagonal']):
            return 'architecture-design'
        
        # Diagram related reports
        if any(keyword in name for keyword in ['diagram', 'excalidraw', 'svg', 'sync']):
            return 'diagrams'
        
        # Infrastructure and deployment
        if any(keyword in path_str for keyword in ['infrastructure', 'deployment', 'cdk', 'aws']):
            return 'infrastructure'
        
        # Frontend related
        if any(keyword in path_str for keyword in ['frontend', 'ui', 'cmc', 'consumer']):
            return 'frontend'
        
        # Testing related
        if any(keyword in name for keyword in ['test', 'testing', 'optimization']):
            return 'testing'
        
        # Translation related
        if any(keyword in name for keyword in ['translation', 'translate']):
            return 'translation'
        
        # Project management and general
        if any(keyword in name for keyword in ['project', 'refactoring', 'cleanup', 'status', 'structure']):
            return 'project-management'
        
        # User experience and quality
        if any(keyword in name for keyword in ['user-experience', 'quality', 'documentation']):
            return 'quality-ux'
        
        # Default category
        return 'general'
    
    def create_target_structure(self):
        """Create the target directory structure"""
        categories = [
            'task-execution',
            'architecture-design', 
            'diagrams',
            'infrastructure',
            'frontend',
            'testing',
            'translation',
            'project-management',
            'quality-ux',
            'general'
        ]
        
        for category in categories:
            (self.target_dir / category).mkdir(parents=True, exist_ok=True)
        
        print(f"📁 Created target directory structure at: {self.target_dir}")
    
    def should_skip_file(self, file_path: Path) -> bool:
        """Check if file should be skipped"""
        path_str = str(file_path)
        
        # Skip files in node_modules, .git, build outputs
        skip_patterns = [
            'node_modules',
            '.git',
            'build/reports/test-results',
            'build/test-results',
            'target/',
            '.gradle'
        ]
        
        for pattern in skip_patterns:
            if pattern in path_str:
                return True
        
        # Skip if already in target directory
        if str(self.target_dir) in path_str:
            return True
            
        return False
    
    def move_file(self, source_path: Path, category: str) -> bool:
        """Move a file to the appropriate category directory"""
        try:
            target_path = self.target_dir / category / source_path.name
            
            # Handle name conflicts
            counter = 1
            original_target = target_path
            while target_path.exists():
                stem = original_target.stem
                suffix = original_target.suffix
                target_path = original_target.parent / f"{stem}_{counter}{suffix}"
                counter += 1
            
            # Move the file
            shutil.move(str(source_path), str(target_path))
            
            self.moved_files.append((source_path, target_path))
            print(f"  ✅ Moved: {source_path.relative_to(self.project_root)} → {target_path.relative_to(self.project_root)}")
            return True
            
        except Exception as e:
            print(f"  ❌ Error moving {source_path}: {e}")
            return False
    
    def find_and_move_files(self):
        """Find all report/summary files and move them"""
        print("🔍 Scanning for report and summary files...")
        
        # Find all markdown files
        md_files = list(self.project_root.rglob("*.md"))
        
        report_files = []
        for md_file in md_files:
            if self.is_report_or_summary_file(md_file) and not self.should_skip_file(md_file):
                report_files.append(md_file)
        
        print(f"📊 Found {len(report_files)} report/summary files to organize")
        
        # Group by category and move
        categories = {}
        for file_path in report_files:
            category = self.get_category_from_path(file_path)
            if category not in categories:
                categories[category] = []
            categories[category].append(file_path)
        
        # Move files by category
        for category, files in categories.items():
            print(f"\n📂 Processing category: {category} ({len(files)} files)")
            for file_path in files:
                self.move_file(file_path, category)
    
    def create_index_file(self):
        """Create an index file for the organized reports"""
        index_content = f"""# Reports and Summaries Index

This directory contains all project reports and summaries organized by category.

## Directory Structure

"""
        
        # Add category descriptions
        category_descriptions = {
            'task-execution': 'Task execution reports, completion reports, and automation results',
            'architecture-design': 'Architecture decisions, design documents, and DDD-related reports',
            'diagrams': 'Diagram generation, synchronization, and visualization reports',
            'infrastructure': 'Infrastructure deployment, CDK, and AWS-related reports',
            'frontend': 'Frontend development, UI improvements, and dashboard reports',
            'testing': 'Testing optimization, performance, and quality reports',
            'translation': 'Translation system reports and language processing results',
            'project-management': 'Project status, refactoring, and cleanup reports',
            'quality-ux': 'User experience testing and documentation quality reports',
            'general': 'General reports that don\'t fit other categories'
        }
        
        for category_dir in sorted(self.target_dir.iterdir()):
            if category_dir.is_dir():
                category = category_dir.name
                description = category_descriptions.get(category, 'Miscellaneous reports')
                
                files = list(category_dir.glob("*.md"))
                index_content += f"### {category.replace('-', ' ').title()}\n"
                index_content += f"{description}\n\n"
                
                if files:
                    for file_path in sorted(files):
                        index_content += f"- [{file_path.name}]({category}/{file_path.name})\n"
                else:
                    index_content += "- (No files in this category)\n"
                index_content += "\n"
        
        index_content += f"""## Summary

- **Total files organized**: {len(self.moved_files)}
- **Categories**: {len([d for d in self.target_dir.iterdir() if d.is_dir()])}
- **Last updated**: {os.popen('date').read().strip()}

## Usage

These reports document various aspects of the project development:

- **Task Execution**: Results from specific development tasks and automation
- **Architecture**: Design decisions and architectural analysis
- **Quality**: Testing, documentation, and user experience validation
- **Infrastructure**: Deployment and infrastructure management
- **Development**: Frontend, translation, and general development activities

---

*This index is automatically generated by the report organization script.*
"""
        
        index_path = self.target_dir / "README.md"
        index_path.write_text(index_content, encoding='utf-8')
        print(f"📋 Created index file: {index_path}")
    
    def generate_summary(self):
        """Generate a summary of the organization process"""
        print(f"\n🎉 Organization Complete!")
        print(f"📊 Summary:")
        print(f"   ✅ Files moved: {len(self.moved_files)}")
        print(f"   ⏭️  Files skipped: {len(self.skipped_files)}")
        print(f"   📁 Target directory: {self.target_dir}")
        
        if self.moved_files:
            print(f"\n📋 Moved files by category:")
            categories = {}
            for source, target in self.moved_files:
                category = target.parent.name
                if category not in categories:
                    categories[category] = 0
                categories[category] += 1
            
            for category, count in sorted(categories.items()):
                print(f"   📂 {category}: {count} files")
    
    def run(self):
        """Run the complete organization process"""
        print("📁 Starting Reports and Summaries Organization...")
        print("=" * 60)
        
        # Create target structure
        self.create_target_structure()
        
        # Find and move files
        self.find_and_move_files()
        
        # Create index
        self.create_index_file()
        
        # Generate summary
        self.generate_summary()
        
        print("\n💡 Next steps:")
        print("   1. Review the organized files in reports-summaries/")
        print("   2. Update any links that reference the moved files")
        print("   3. Add reports-summaries/ to your documentation navigation")

def main():
    project_root = os.getcwd()
    organizer = ReportSummaryOrganizer(project_root)
    organizer.run()

if __name__ == "__main__":
    main()