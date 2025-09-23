#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Update Report Links Script
Updates links in documentation files to point to the new reports-summaries directory
"""

import os
import re
from pathlib import Path
from typing import List, Tuple

class ReportLinkUpdater:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.reports_dir = self.project_root / "reports-summaries"
        self.updated_files = []
        
        # Create mapping of old paths to new paths
        self.path_mappings = self.create_path_mappings()
    
    def create_path_mappings(self) -> dict:
        """Create mapping from old file paths to new paths in reports-summaries"""
        mappings = {}
        
        # Scan reports-summaries directory to build mappings
        for category_dir in self.reports_dir.iterdir():
            if category_dir.is_dir():
                for report_file in category_dir.glob("*.md"):
                    # Remove version suffixes (_1, _2, etc.) to get original name
                    original_name = re.sub(r'_\d+\.md$', '.md', report_file.name)
                    
                    # Map various possible old paths to new path
                    new_path = f"reports-summaries/{category_dir.name}/{report_file.name}"
                    
                    # Common old locations
                    old_paths = [
                        original_name,
                        f"./{original_name}",
                        f"../{original_name}",
                        f"../../{original_name}",
                        f"docs/{original_name}",
                        f"./docs/{original_name}",
                        f"../docs/{original_name}",
                    ]
                    
                    for old_path in old_paths:
                        mappings[old_path] = new_path
        
        return mappings
    
    def update_links_in_file(self, file_path: Path) -> bool:
        """Update links in a single file"""
        try:
            content = file_path.read_text(encoding='utf-8')
            original_content = content
            
            # Update markdown links
            for old_path, new_path in self.path_mappings.items():
                # Match markdown links: [text](path)
                pattern = rf'\]\({re.escape(old_path)}\)'
                replacement = f']({new_path})'
                content = re.sub(pattern, replacement, content)
                
                # Match relative paths in documentation
                if old_path.startswith('./') or old_path.startswith('../'):
                    continue  # Already handled above
                
                # Handle direct file references
                if old_path in content and not old_path.startswith('http'):
                    content = content.replace(old_path, new_path)
            
            # Write back if changed
            if content != original_content:
                file_path.write_text(content, encoding='utf-8')
                self.updated_files.append(file_path)
                return True
            
            return False
            
        except Exception as e:
            print(f"  ❌ Error updating {file_path}: {e}")
            return False
    
    def scan_and_update_files(self):
        """Scan all documentation files and update links"""
        print("🔍 Scanning documentation files for report links...")
        
        # Find all markdown files in docs and other relevant directories
        search_patterns = [
            "docs/**/*.md",
            "*.md",
            "scripts/**/*.md",
            ".kiro/**/*.md"
        ]
        
        files_to_check = set()
        for pattern in search_patterns:
            files_to_check.update(self.project_root.glob(pattern))
        
        # Exclude files in reports-summaries directory itself
        files_to_check = [f for f in files_to_check if 'reports-summaries' not in str(f)]
        
        print(f"📊 Found {len(files_to_check)} files to check")
        
        updated_count = 0
        for file_path in files_to_check:
            if self.update_links_in_file(file_path):
                print(f"  ✅ Updated: {file_path.relative_to(self.project_root)}")
                updated_count += 1
        
        print(f"\n📈 Summary: Updated {updated_count} files")
    
    def run(self):
        """Run the link update process"""
        print("🔗 Starting Report Link Update...")
        print("=" * 50)
        
        if not self.reports_dir.exists():
            print("❌ reports-summaries directory not found!")
            return
        
        print(f"📁 Reports directory: {self.reports_dir}")
        print(f"🗺️  Created {len(self.path_mappings)} path mappings")
        
        # Update links
        self.scan_and_update_files()
        
        print("\n🎉 Link update complete!")
        print("\n💡 Recommendations:")
        print("   1. Review updated files to ensure links are correct")
        print("   2. Test navigation to verify all links work")
        print("   3. Update any remaining manual references")

def main():
    project_root = os.getcwd()
    updater = ReportLinkUpdater(project_root)
    updater.run()

if __name__ == "__main__":
    main()