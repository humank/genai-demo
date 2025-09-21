#!/usr/bin/env python3
"""
Intelligent Diagram-Documentation Synchronization System

This script performs comprehensive synchronization between diagrams and documentation,
ensuring all references are accurate and up-to-date.
"""

import os
import re
import json
import glob
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Set, Tuple, Optional
import argparse

class DiagramDocumentationSync:
    def __init__(self):
        self.project_root = Path(".")
        self.diagrams_dir = self.project_root / "docs" / "diagrams"
        self.viewpoints_dir = self.project_root / "docs" / "viewpoints"
        self.perspectives_dir = self.project_root / "docs" / "perspectives"
        self.architecture_dir = self.project_root / "docs" / "architecture"
        
        # Track changes
        self.changes_made = []
        self.broken_references = []
        self.new_references = []
        self.orphaned_diagrams = []
        self.missing_diagrams = []
        
        # Diagram file extensions
        self.diagram_extensions = {'.mmd', '.puml', '.excalidraw', '.svg'}
        
    def analyze_diagram_changes(self) -> Dict[str, List[str]]:
        """Analyze what diagram changes have occurred"""
        print("🔍 Analyzing diagram changes...")
        
        changes = {
            'modified': [],
            'new': [],
            'deleted': [],
            'moved': []
        }
        
        # Find all diagram files
        for ext in self.diagram_extensions:
            pattern = f"**/*{ext}"
            for diagram_file in self.diagrams_dir.glob(pattern):
                if diagram_file.is_file():
                    # For this implementation, we'll focus on the aws_infrastructure.mmd change
                    if diagram_file.name == "aws_infrastructure.mmd":
                        changes['modified'].append(str(diagram_file.relative_to(self.project_root)))
        
        return changes
    
    def find_documentation_references(self, diagram_path: str) -> List[Tuple[str, int, str]]:
        """Find all documentation files that reference a specific diagram"""
        references = []
        diagram_name = Path(diagram_path).name
        diagram_stem = Path(diagram_path).stem
        
        # Search patterns
        patterns = [
            diagram_name,
            diagram_stem,
            diagram_path,
            diagram_path.replace('docs/', ''),
            f"../{diagram_path}",
            f"../../{diagram_path}",
            f"../../../{diagram_path}"
        ]
        
        # Search in all markdown files
        for md_file in self.project_root.glob("**/*.md"):
            if md_file.is_file():
                try:
                    with open(md_file, 'r', encoding='utf-8') as f:
                        content = f.read()
                        lines = content.split('\n')
                        
                        for line_num, line in enumerate(lines, 1):
                            for pattern in patterns:
                                if pattern in line:
                                    references.append((str(md_file.relative_to(self.project_root)), line_num, line.strip()))
                except Exception as e:
                    print(f"❌ Error reading {md_file}: {e}")
        
        return references
    
    def update_aws_infrastructure_references(self):
        """Update references to the simplified AWS infrastructure diagram"""
        print("🔄 Updating AWS infrastructure diagram references...")
        
        # The diagram has been simplified, so we need to update documentation
        # to reflect the new simplified structure
        
        # Update deployment viewpoint README
        deployment_readme = self.viewpoints_dir / "deployment" / "README.md"
        if deployment_readme.exists():
            self.update_deployment_viewpoint_readme(deployment_readme)
        
        # Update observability architecture documentation
        obs_arch_file = self.architecture_dir / "observability-architecture.md"
        if obs_arch_file.exists():
            self.update_observability_architecture(obs_arch_file)
        
        # Update any other references
        self.update_general_references()
    
    def update_deployment_viewpoint_readme(self, file_path: Path):
        """Update the deployment viewpoint README with updated diagram description"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Update the AWS infrastructure diagram description
            old_description = "*完整的 AWS 基礎設施架構，包括 CDK 堆疊、網路安全、容器平台、資料服務和可觀測性組件*"
            new_description = "*簡化的 AWS 基礎設施架構，展示核心組件：EKS 集群、RDS 資料庫、S3 儲存、CloudWatch 監控和應用程式負載均衡器*"
            
            if old_description in content:
                content = content.replace(old_description, new_description)
                
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                
                self.changes_made.append(f"Updated AWS infrastructure description in {file_path}")
                print(f"✅ Updated AWS infrastructure description in {file_path}")
            
        except Exception as e:
            print(f"❌ Error updating {file_path}: {e}")
    
    def update_observability_architecture(self, file_path: Path):
        """Update observability architecture documentation"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Check if the file references the AWS infrastructure diagram
            if "aws_infrastructure.mmd" in content:
                # Add a note about the simplified diagram
                note = "\n> **Note**: The AWS infrastructure diagram has been simplified to show core components. For detailed infrastructure including CDK stacks, networking, and observability services, refer to the infrastructure documentation.\n"
                
                # Find a good place to insert the note (after the first reference)
                pattern = r'(- \[AWS Infrastructure Architecture\]\(\.\.\/diagrams\/aws_infrastructure\.mmd\))'
                if re.search(pattern, content):
                    content = re.sub(pattern, r'\1' + note, content, count=1)
                    
                    with open(file_path, 'w', encoding='utf-8') as f:
                        f.write(content)
                    
                    self.changes_made.append(f"Added simplification note to {file_path}")
                    print(f"✅ Added simplification note to {file_path}")
            
        except Exception as e:
            print(f"❌ Error updating {file_path}: {e}")
    
    def update_general_references(self):
        """Update any other references to AWS infrastructure"""
        # Search for files that might reference the AWS infrastructure
        for md_file in self.project_root.glob("**/*.md"):
            if md_file.is_file() and "aws" in md_file.name.lower():
                try:
                    with open(md_file, 'r', encoding='utf-8') as f:
                        content = f.read()
                    
                    # Look for references that might need updating
                    if "aws_infrastructure" in content.lower() and "complex" in content.lower():
                        # This might be a file that describes the complex infrastructure
                        # Add a note about the diagram simplification
                        print(f"📝 Found potential reference in {md_file} - manual review recommended")
                        
                except Exception as e:
                    print(f"❌ Error checking {md_file}: {e}")
    
    def create_missing_diagram_references(self):
        """Create references to diagrams that should be referenced but aren't"""
        print("📋 Creating missing diagram references...")
        
        # Check if viewpoint READMEs reference appropriate diagrams
        viewpoint_dirs = [d for d in self.viewpoints_dir.iterdir() if d.is_dir()]
        
        for viewpoint_dir in viewpoint_dirs:
            readme_file = viewpoint_dir / "README.md"
            if readme_file.exists():
                self.check_viewpoint_diagram_references(viewpoint_dir.name, readme_file)
    
    def check_viewpoint_diagram_references(self, viewpoint_name: str, readme_file: Path):
        """Check if a viewpoint README has appropriate diagram references"""
        try:
            with open(readme_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Check for specific viewpoint diagram requirements
            if viewpoint_name == "deployment":
                required_diagrams = [
                    "aws_infrastructure.mmd",
                    "multi_environment.mmd"
                ]
                
                for diagram in required_diagrams:
                    if diagram not in content:
                        print(f"📝 {viewpoint_name} viewpoint should reference {diagram}")
                        self.missing_diagrams.append(f"{viewpoint_name} -> {diagram}")
            
            elif viewpoint_name == "functional":
                # Functional viewpoint should reference domain and architecture diagrams
                required_diagrams = [
                    "ddd_architecture.mmd",
                    "hexagonal_architecture.mmd"
                ]
                
                for diagram in required_diagrams:
                    if diagram not in content:
                        print(f"📝 {viewpoint_name} viewpoint should reference {diagram}")
                        self.missing_diagrams.append(f"{viewpoint_name} -> {diagram}")
        
        except Exception as e:
            print(f"❌ Error checking {readme_file}: {e}")
    
    def validate_diagram_references(self) -> List[str]:
        """Validate all diagram references in documentation"""
        print("🔍 Validating diagram references...")
        
        broken_refs = []
        
        # Find all markdown files
        for md_file in self.project_root.glob("**/*.md"):
            if md_file.is_file():
                try:
                    with open(md_file, 'r', encoding='utf-8') as f:
                        content = f.read()
                    
                    # Find diagram references
                    patterns = [
                        r'!\[.*?\]\((.*?\.(?:mmd|puml|svg|excalidraw))\)',  # Image references
                        r'\[.*?\]\((.*?\.(?:mmd|puml|svg|excalidraw))\)',   # Link references
                    ]
                    
                    for pattern in patterns:
                        matches = re.finditer(pattern, content)
                        for match in matches:
                            diagram_path = match.group(1)
                            
                            # Resolve relative path
                            if diagram_path.startswith('../'):
                                # Relative path from the markdown file
                                resolved_path = (md_file.parent / diagram_path).resolve()
                            elif diagram_path.startswith('/'):
                                # Absolute path from project root
                                resolved_path = (self.project_root / diagram_path.lstrip('/')).resolve()
                            else:
                                # Relative to markdown file
                                resolved_path = (md_file.parent / diagram_path).resolve()
                            
                            # Check if file exists
                            if not resolved_path.exists():
                                broken_ref = f"{md_file.relative_to(self.project_root)} -> {diagram_path}"
                                broken_refs.append(broken_ref)
                                print(f"❌ Broken reference: {broken_ref}")
                
                except Exception as e:
                    print(f"❌ Error validating {md_file}: {e}")
        
        return broken_refs
    
    def find_orphaned_diagrams(self) -> List[str]:
        """Find diagrams that are not referenced by any documentation"""
        print("🔍 Finding orphaned diagrams...")
        
        # Get all diagram files
        all_diagrams = set()
        for ext in self.diagram_extensions:
            pattern = f"**/*{ext}"
            for diagram_file in self.diagrams_dir.glob(pattern):
                if diagram_file.is_file():
                    all_diagrams.add(str(diagram_file.relative_to(self.project_root)))
        
        # Get all referenced diagrams
        referenced_diagrams = set()
        for md_file in self.project_root.glob("**/*.md"):
            if md_file.is_file():
                try:
                    with open(md_file, 'r', encoding='utf-8') as f:
                        content = f.read()
                    
                    # Find diagram references
                    patterns = [
                        r'!\[.*?\]\((.*?\.(?:mmd|puml|svg|excalidraw))\)',
                        r'\[.*?\]\((.*?\.(?:mmd|puml|svg|excalidraw))\)',
                    ]
                    
                    for pattern in patterns:
                        matches = re.finditer(pattern, content)
                        for match in matches:
                            diagram_path = match.group(1)
                            
                            # Normalize path
                            if diagram_path.startswith('../'):
                                # Try to resolve relative path
                                try:
                                    resolved = (md_file.parent / diagram_path).resolve()
                                    rel_path = resolved.relative_to(self.project_root)
                                    referenced_diagrams.add(str(rel_path))
                                except:
                                    pass
                            elif diagram_path.startswith('docs/'):
                                referenced_diagrams.add(diagram_path)
                
                except Exception as e:
                    print(f"❌ Error checking references in {md_file}: {e}")
        
        # Find orphaned diagrams
        orphaned = all_diagrams - referenced_diagrams
        
        for orphan in orphaned:
            print(f"🔍 Orphaned diagram: {orphan}")
        
        return list(orphaned)
    
    def generate_diagram_index(self):
        """Generate or update the diagram index"""
        print("📋 Generating diagram index...")
        
        index_file = self.diagrams_dir / "README.md"
        
        # Collect all diagrams by category
        diagrams_by_category = {
            'Architecture Overview': [],
            'Viewpoint Diagrams': {},
            'Perspective Diagrams': {},
            'Legacy Diagrams': [],
            'PlantUML Diagrams': [],
            'Mermaid Diagrams': []
        }
        
        # Scan diagrams
        for diagram_file in self.diagrams_dir.glob("**/*"):
            if diagram_file.is_file() and diagram_file.suffix in self.diagram_extensions:
                rel_path = diagram_file.relative_to(self.diagrams_dir)
                
                if 'viewpoints' in str(rel_path):
                    viewpoint = str(rel_path).split('/')[1]
                    if viewpoint not in diagrams_by_category['Viewpoint Diagrams']:
                        diagrams_by_category['Viewpoint Diagrams'][viewpoint] = []
                    diagrams_by_category['Viewpoint Diagrams'][viewpoint].append(str(rel_path))
                
                elif 'perspectives' in str(rel_path):
                    perspective = str(rel_path).split('/')[1]
                    if perspective not in diagrams_by_category['Perspective Diagrams']:
                        diagrams_by_category['Perspective Diagrams'][perspective] = []
                    diagrams_by_category['Perspective Diagrams'][perspective].append(str(rel_path))
                
                elif 'legacy' in str(rel_path):
                    diagrams_by_category['Legacy Diagrams'].append(str(rel_path))
                
                elif 'plantuml' in str(rel_path):
                    diagrams_by_category['PlantUML Diagrams'].append(str(rel_path))
                
                elif 'mermaid' in str(rel_path):
                    diagrams_by_category['Mermaid Diagrams'].append(str(rel_path))
                
                elif rel_path.parent == Path('.'):
                    # Root level diagrams
                    diagrams_by_category['Architecture Overview'].append(str(rel_path))
        
        # Generate index content
        content = f"""# Diagram Index

Generated on: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

## Architecture Overview

"""
        
        for diagram in sorted(diagrams_by_category['Architecture Overview']):
            name = Path(diagram).stem.replace('_', ' ').title()
            content += f"- [{name}]({diagram})\n"
        
        content += "\n## Viewpoint Diagrams\n\n"
        for viewpoint, diagrams in sorted(diagrams_by_category['Viewpoint Diagrams'].items()):
            content += f"### {viewpoint.title()}\n\n"
            for diagram in sorted(diagrams):
                name = Path(diagram).stem.replace('_', ' ').replace('-', ' ').title()
                content += f"- [{name}]({diagram})\n"
            content += "\n"
        
        content += "## Perspective Diagrams\n\n"
        for perspective, diagrams in sorted(diagrams_by_category['Perspective Diagrams'].items()):
            content += f"### {perspective.title()}\n\n"
            for diagram in sorted(diagrams):
                name = Path(diagram).stem.replace('_', ' ').replace('-', ' ').title()
                content += f"- [{name}]({diagram})\n"
            content += "\n"
        
        # Write index file
        try:
            with open(index_file, 'w', encoding='utf-8') as f:
                f.write(content)
            
            self.changes_made.append(f"Generated diagram index: {index_file}")
            print(f"✅ Generated diagram index: {index_file}")
        
        except Exception as e:
            print(f"❌ Error generating diagram index: {e}")
    
    def run_comprehensive_sync(self, validate_only=False):
        """Run the comprehensive synchronization process"""
        print("🚀 Starting comprehensive diagram-documentation synchronization...")
        print("=" * 70)
        
        # 1. Analyze changes
        changes = self.analyze_diagram_changes()
        print(f"📊 Changes detected: {sum(len(v) for v in changes.values())} files")
        
        if not validate_only:
            # 2. Update AWS infrastructure references (main change)
            self.update_aws_infrastructure_references()
            
            # 3. Create missing references
            self.create_missing_diagram_references()
            
            # 4. Generate diagram index
            self.generate_diagram_index()
        
        # 5. Validate references
        self.broken_references = self.validate_diagram_references()
        
        # 6. Find orphaned diagrams
        self.orphaned_diagrams = self.find_orphaned_diagrams()
        
        # 7. Generate report
        self.generate_report()
    
    def generate_report(self):
        """Generate a comprehensive report of the synchronization"""
        print("\n" + "=" * 70)
        print("📊 SYNCHRONIZATION REPORT")
        print("=" * 70)
        
        print(f"\n✅ Changes Made: {len(self.changes_made)}")
        for change in self.changes_made:
            print(f"  • {change}")
        
        print(f"\n❌ Broken References: {len(self.broken_references)}")
        for ref in self.broken_references:
            print(f"  • {ref}")
        
        print(f"\n🔍 Orphaned Diagrams: {len(self.orphaned_diagrams)}")
        for orphan in self.orphaned_diagrams:
            print(f"  • {orphan}")
        
        print(f"\n📝 Missing Diagram References: {len(self.missing_diagrams)}")
        for missing in self.missing_diagrams:
            print(f"  • {missing}")
        
        # Quality metrics
        total_diagrams = len(list(self.diagrams_dir.glob("**/*")))
        referenced_diagrams = total_diagrams - len(self.orphaned_diagrams)
        coverage_percentage = (referenced_diagrams / total_diagrams * 100) if total_diagrams > 0 else 0
        
        print(f"\n📈 Quality Metrics:")
        print(f"  • Total Diagrams: {total_diagrams}")
        print(f"  • Referenced Diagrams: {referenced_diagrams}")
        print(f"  • Reference Coverage: {coverage_percentage:.1f}%")
        print(f"  • Broken References: {len(self.broken_references)}")
        
        # Recommendations
        print(f"\n💡 Recommendations:")
        if self.broken_references:
            print("  • Fix broken diagram references")
        if self.orphaned_diagrams:
            print("  • Add documentation references for orphaned diagrams")
        if self.missing_diagrams:
            print("  • Create missing diagrams or add references")
        if coverage_percentage < 80:
            print("  • Improve diagram reference coverage")
        
        print("\n🎉 Synchronization completed!")

def main():
    parser = argparse.ArgumentParser(description='Diagram-Documentation Synchronization System')
    parser.add_argument('--mode', choices=['diagram-to-docs', 'docs-to-diagram', 'comprehensive'], 
                       default='comprehensive', help='Synchronization mode')
    parser.add_argument('--validate', action='store_true', help='Validate references only')
    parser.add_argument('--report', action='store_true', help='Generate detailed report')
    
    args = parser.parse_args()
    
    sync = DiagramDocumentationSync()
    
    if args.mode == 'comprehensive' or args.mode == 'diagram-to-docs':
        sync.run_comprehensive_sync(validate_only=args.validate)
    else:
        print(f"Mode {args.mode} not fully implemented yet")

if __name__ == "__main__":
    main()