#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Link Redirection Script
Creates redirect mechanisms for old links to new Viewpoints & Perspectives structure
"""

import os
import re
from pathlib import Path
from typing import Dict, List, Tuple

class LinkRedirectCreator:
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.docs_dir = self.project_root / "docs"
        self.en_docs_dir = self.project_root / "docs" / "en"
        self.redirect_mappings = self.create_redirect_mappings()
        
    def create_redirect_mappings(self) -> Dict[str, str]:
        """Create mapping of old paths to new paths"""
        return {
            # Design documents moved to viewpoints/functional
            "docs/design/ddd-guide.md": "docs/viewpoints/functional/domain-model.md",
            "docs/en/design/ddd-guide.md": "docs/en/viewpoints/functional/domain-model.md",
            
            # Architecture documents moved to viewpoints/development
            "docs/architecture/hexagonal-architecture.md": "docs/viewpoints/development/hexagonal-architecture.md",
            "docs/en/architecture/hexagonal-architecture.md": "docs/en/viewpoints/development/hexagonal-architecture.md",
            
            # Development documents moved to viewpoints/development
            "docs/development/README.md": "docs/viewpoints/development/README.md",
            "docs/en/development/README.md": "docs/en/viewpoints/development/README.md",
            
            # Deployment documents moved to viewpoints/deployment
            "docs/deployment/README.md": "docs/viewpoints/deployment/README.md",
            "docs/en/deployment/README.md": "docs/en/viewpoints/deployment/README.md",
            "docs/deployment/docker-guide.md": "docs/viewpoints/deployment/docker-guide.md",
            "docs/en/deployment/docker-guide.md": "docs/en/viewpoints/deployment/docker-guide.md",
            
            # Observability documents moved to viewpoints/operational
            "docs/observability/README.md": "docs/viewpoints/operational/README.md",
            "docs/en/observability/README.md": "docs/en/viewpoints/operational/README.md",
            "docs/observability/configuration-guide.md": "docs/viewpoints/operational/configuration-guide.md",
            "docs/en/observability/configuration-guide.md": "docs/en/viewpoints/operational/configuration-guide.md",
            
            # Testing documents moved to viewpoints/development
            "docs/testing/README.md": "docs/viewpoints/development/testing-strategy.md",
            "docs/en/testing/README.md": "docs/en/viewpoints/development/testing-strategy.md",
        }
    
    def create_redirect_file(self, old_path: str, new_path: str):
        """Create a redirect file at the old location"""
        old_file_path = self.project_root / old_path
        
        # Create directory if it doesn't exist
        old_file_path.parent.mkdir(parents=True, exist_ok=True)
        
        # Calculate relative path from old location to new location
        try:
            relative_new_path = os.path.relpath(
                self.project_root / new_path,
                old_file_path.parent
            )
        except ValueError:
            # If relative path calculation fails, use absolute path
            relative_new_path = f"/{new_path}"
        
        # Create redirect content
        redirect_content = f"""# Document Moved

> **⚠️ This document has been moved as part of the Rozanski & Woods Viewpoints & Perspectives restructure.**

## New Location

This document is now located at: **[{new_path}]({relative_new_path})**

## Why was this moved?

As part of our documentation restructure to follow Rozanski & Woods architecture methodology, we've reorganized our documentation into:

- **7 Architectural Viewpoints**: Functional, Information, Concurrency, Development, Deployment, Operational
- **8 Architectural Perspectives**: Security, Performance, Availability, Evolution, Usability, Regulation, Location, Cost

This provides a more systematic and comprehensive approach to architecture documentation.

## Quick Navigation

- 📚 [Documentation Center](../README.md)
- 🏗️ [Viewpoints Overview](../viewpoints/README.md)  
- 👁️ [Perspectives Overview](../perspectives/README.md)
- 🔗 [Viewpoint-Perspective Matrix](../viewpoint-perspective-matrix.md)

---

**Please update your bookmarks and links to point to the new location.**
"""
        
        # Write redirect file
        old_file_path.write_text(redirect_content, encoding='utf-8')
        print(f"  ✅ Created redirect: {old_path} → {new_path}")
    
    def create_all_redirects(self):
        """Create all redirect files"""
        print("🔗 Creating link redirects for moved documents...")
        
        created_count = 0
        for old_path, new_path in self.redirect_mappings.items():
            # Only create redirect if the new file exists and old file doesn't exist
            new_file = self.project_root / new_path
            old_file = self.project_root / old_path
            
            if new_file.exists() and not old_file.exists():
                self.create_redirect_file(old_path, new_path)
                created_count += 1
            elif old_file.exists():
                print(f"  ℹ️  Skipping {old_path} (file already exists)")
            else:
                print(f"  ⚠️  Skipping {old_path} (target {new_path} doesn't exist)")
        
        print(f"✅ Created {created_count} redirect files")
    
    def update_internal_links(self):
        """Update internal links in existing files"""
        print("🔗 Updating internal links in documentation...")
        
        # Find all markdown files
        md_files = list(self.docs_dir.rglob("*.md"))
        updated_count = 0
        
        for md_file in md_files:
            if md_file.is_file():
                try:
                    content = md_file.read_text(encoding='utf-8')
                    original_content = content
                    
                    # Update links based on redirect mappings
                    for old_path, new_path in self.redirect_mappings.items():
                        # Convert to relative paths for link updates
                        old_link_patterns = [
                            f"]({old_path})",
                            f"](./{old_path})",
                            f"](../{old_path})",
                            f"](../../{old_path})",
                        ]
                        
                        for pattern in old_link_patterns:
                            if pattern in content:
                                # Calculate relative path from current file to new location
                                try:
                                    relative_new_path = os.path.relpath(
                                        self.project_root / new_path,
                                        md_file.parent
                                    )
                                    new_pattern = f"]({relative_new_path})"
                                    content = content.replace(pattern, new_pattern)
                                except ValueError:
                                    # Fallback to absolute path
                                    content = content.replace(pattern, f"]({new_path})")
                    
                    # Write back if changed
                    if content != original_content:
                        md_file.write_text(content, encoding='utf-8')
                        updated_count += 1
                        print(f"  ✅ Updated links in: {md_file.relative_to(self.project_root)}")
                        
                except Exception as e:
                    print(f"  ❌ Error updating {md_file}: {e}")
        
        print(f"✅ Updated links in {updated_count} files")
    
    def run(self):
        """Run the complete link redirect creation process"""
        print("🔗 Starting Link Redirect Creation...")
        
        # Create redirect files
        self.create_all_redirects()
        
        # Update internal links
        self.update_internal_links()
        
        print("\n🎉 Link Redirect Creation Complete!")
        print("\n📋 Summary:")
        print("- Created redirect files for moved documents")
        print("- Updated internal links to point to new locations")
        print("- Old bookmarks will now show redirect messages")
        print("\n💡 Recommendations:")
        print("- Test a few redirect links to ensure they work correctly")
        print("- Update any external documentation that links to moved files")
        print("- Consider adding redirects to web server configuration if hosting online")

def main():
    project_root = os.getcwd()
    creator = LinkRedirectCreator(project_root)
    creator.run()

if __name__ == "__main__":
    main()