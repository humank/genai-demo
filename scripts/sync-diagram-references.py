#!/usr/bin/env python3
"""
Intelligent Diagram-Documentation Synchronization System

This script performs comprehensive synchronization between diagrams and documentation,
ensuring all references are accurate and up-to-date.
"""

import os
import re
import json
import argparse
from pathlib import Path
from typing import Dict, List, Set, Tuple, Optional
from dataclasses import dataclass
from collections import defaultdict
import urllib.parse

@dataclass
class DiagramInfo:
    """Information about a diagram file"""
    path: Path
    name: str
    category: str
    viewpoint: Optional[str] = None
    perspective: Optional[str] = None
    format: str = 'unknown'
    
@dataclass
class DocumentInfo:
    """Information about a documentation file"""
    path: Path
    name: str
    viewpoint: Optional[str] = None
    perspective: Optional[str] = None
    
@dataclass
class ReferenceInfo:
    """Information about a diagram reference in documentation"""
    doc_path: Path
    line_number: int
    original_text: str
    diagram_path: str
    alt_text: str
    is_valid: bool = True
    
class DiagramDocumentationSync:
    """Main synchronization class"""
    
    def __init__(self, project_root: str = "."):
        self.project_root = Path(project_root)
        self.diagrams_dir = self.project_root / "docs" / "diagrams"
        self.viewpoints_dir = self.project_root / "docs" / "viewpoints"
        self.perspectives_dir = self.project_root / "docs" / "perspectives"
        
        # Discovered data
        self.diagrams: List[DiagramInfo] = []
        self.documents: List[DocumentInfo] = []
        self.references: List[ReferenceInfo] = []
        
        # Analysis results
        self.broken_references: List[ReferenceInfo] = []
        self.orphaned_diagrams: List[DiagramInfo] = []
        self.missing_diagrams: List[str] = []
        self.updates_made: List[str] = []
        
    def discover_diagrams(self) -> None:
        """Discover all diagram files in the project"""
        print("🔍 Discovering diagrams...")
        
        # Patterns for different diagram types
        diagram_patterns = {
            'puml': '*.puml',
            'mmd': '*.mmd', 
            'png': '*.png',
            'svg': '*.svg',
            'excalidraw': '*.excalidraw'
        }
        
        for format_name, pattern in diagram_patterns.items():
            for diagram_path in self.diagrams_dir.rglob(pattern):
                if self._should_include_diagram(diagram_path):
                    diagram_info = self._analyze_diagram_path(diagram_path, format_name)
                    self.diagrams.append(diagram_info)
        
        print(f"   Found {len(self.diagrams)} diagrams")
        
    def discover_documents(self) -> None:
        """Discover all documentation files"""
        print("📄 Discovering documentation...")
        
        # Discover viewpoint documents
        for viewpoint_dir in self.viewpoints_dir.iterdir():
            if viewpoint_dir.is_dir() and not viewpoint_dir.name.startswith('.'):
                for doc_path in viewpoint_dir.rglob('*.md'):
                    doc_info = DocumentInfo(
                        path=doc_path,
                        name=doc_path.stem,
                        viewpoint=viewpoint_dir.name
                    )
                    self.documents.append(doc_info)
        
        # Discover perspective documents
        if self.perspectives_dir.exists():
            for perspective_dir in self.perspectives_dir.iterdir():
                if perspective_dir.is_dir() and not perspective_dir.name.startswith('.'):
                    for doc_path in perspective_dir.rglob('*.md'):
                        doc_info = DocumentInfo(
                            path=doc_path,
                            name=doc_path.stem,
                            perspective=perspective_dir.name
                        )
                        self.documents.append(doc_info)
        
        print(f"   Found {len(self.documents)} documentation files")
        
    def analyze_references(self) -> None:
        """Analyze existing diagram references in documentation"""
        print("🔗 Analyzing diagram references...")
        
        # Regex patterns for different reference formats
        patterns = [
            # Markdown image syntax: ![alt](path)
            r'!\[([^\]]*)\]\(([^)]+)\)',
            # HTML img tags: <img src="path" alt="alt">
            r'<img[^>]+src=["\']([^"\']+)["\'][^>]*alt=["\']([^"\']*)["\'][^>]*>',
            # HTML img tags (alt first): <img alt="alt" src="path">
            r'<img[^>]+alt=["\']([^"\']*)["\'][^>]+src=["\']([^"\']+)["\'][^>]*>'
        ]
        
        for doc_info in self.documents:
            try:
                with open(doc_info.path, 'r', encoding='utf-8') as f:
                    lines = f.readlines()
                
                for line_num, line in enumerate(lines, 1):
                    for pattern in patterns:
                        matches = re.finditer(pattern, line)
                        for match in matches:
                            if 'diagrams/' in match.group(0):
                                ref_info = self._parse_reference(
                                    doc_info.path, line_num, line, match, pattern
                                )
                                if ref_info:
                                    self.references.append(ref_info)
                                    
            except Exception as e:
                print(f"   ⚠️  Error reading {doc_info.path}: {e}")
        
        print(f"   Found {len(self.references)} diagram references")
        
    def validate_references(self) -> None:
        """Validate all diagram references"""
        print("✅ Validating references...")
        
        for ref in self.references:
            # Convert relative path to absolute
            doc_dir = ref.doc_path.parent
            diagram_path = (doc_dir / ref.diagram_path).resolve()
            
            if not diagram_path.exists():
                ref.is_valid = False
                self.broken_references.append(ref)
        
        print(f"   Found {len(self.broken_references)} broken references")
        
    def find_orphaned_diagrams(self) -> None:
        """Find diagrams not referenced by any documentation"""
        print("🔍 Finding orphaned diagrams...")
        
        referenced_paths = set()
        for ref in self.references:
            if ref.is_valid:
                doc_dir = ref.doc_path.parent
                diagram_path = (doc_dir / ref.diagram_path).resolve()
                referenced_paths.add(diagram_path)
        
        for diagram in self.diagrams:
            if diagram.path.resolve() not in referenced_paths:
                # Skip generated files that have source files
                if not self._has_source_file(diagram):
                    self.orphaned_diagrams.append(diagram)
        
        print(f"   Found {len(self.orphaned_diagrams)} orphaned diagrams")
        
    def fix_broken_references(self) -> None:
        """Fix broken diagram references"""
        print("🔧 Fixing broken references...")
        
        for ref in self.broken_references:
            fixed_path = self._find_correct_diagram_path(ref)
            if fixed_path:
                self._update_reference_in_file(ref, fixed_path)
                self.updates_made.append(f"Fixed reference in {ref.doc_path}: {ref.diagram_path} → {fixed_path}")
        
    def add_missing_references(self) -> None:
        """Add references to orphaned diagrams where appropriate"""
        print("➕ Adding missing references...")
        
        for diagram in self.orphaned_diagrams:
            target_docs = self._find_target_documents(diagram)
            for doc_info in target_docs:
                if self._should_add_reference(diagram, doc_info):
                    self._add_diagram_reference(diagram, doc_info)
                    self.updates_made.append(f"Added reference to {diagram.name} in {doc_info.path}")
        
    def generate_png_from_plantuml(self) -> None:
        """Generate PNG images from PlantUML files for better GitHub display"""
        print("🖼️  Generating PNG images from PlantUML...")
        
        plantuml_files = [d for d in self.diagrams if d.format == 'puml']
        if not plantuml_files:
            print("   No PlantUML files found")
            return
            
        # Use the existing diagram generation script
        import subprocess
        try:
            result = subprocess.run(
                ['./scripts/generate-diagrams.sh', '--format=png'],
                cwd=self.project_root,
                capture_output=True,
                text=True
            )
            if result.returncode == 0:
                print(f"   ✅ Generated PNG images for {len(plantuml_files)} PlantUML files")
            else:
                print(f"   ⚠️  Error generating images: {result.stderr}")
        except Exception as e:
            print(f"   ⚠️  Could not run diagram generation: {e}")
    
    def _should_include_diagram(self, path: Path) -> bool:
        """Check if a diagram should be included in analysis"""
        # Skip hidden files and certain directories
        if any(part.startswith('.') for part in path.parts):
            return False
        
        # Skip backup files
        if path.name.endswith('.bak') or path.name.endswith('~'):
            return False
            
        return True
    
    def _analyze_diagram_path(self, path: Path, format_name: str) -> DiagramInfo:
        """Analyze a diagram path to extract metadata"""
        relative_path = path.relative_to(self.diagrams_dir)
        parts = relative_path.parts
        
        # Determine category and viewpoint/perspective
        category = 'general'
        viewpoint = None
        perspective = None
        
        if len(parts) > 0:
            if parts[0] == 'viewpoints' and len(parts) > 1:
                category = 'viewpoint'
                viewpoint = parts[1]
            elif parts[0] == 'perspectives' and len(parts) > 1:
                category = 'perspective'
                perspective = parts[1]
            elif parts[0] == 'generated':
                category = 'generated'
                if len(parts) > 1:
                    if parts[1] in ['functional', 'information', 'deployment', 'development', 'operational', 'concurrency']:
                        viewpoint = parts[1]
            else:
                category = parts[0]
        
        return DiagramInfo(
            path=path,
            name=path.stem,
            category=category,
            viewpoint=viewpoint,
            perspective=perspective,
            format=format_name
        )
    
    def _parse_reference(self, doc_path: Path, line_num: int, line: str, match, pattern: str) -> Optional[ReferenceInfo]:
        """Parse a diagram reference from a regex match"""
        try:
            if r'!\[' in pattern:  # Markdown format
                alt_text = match.group(1)
                diagram_path = match.group(2)
            else:  # HTML format
                if 'alt.*src' in pattern:
                    alt_text = match.group(1)
                    diagram_path = match.group(2)
                else:
                    alt_text = match.group(2)
                    diagram_path = match.group(1)
            
            # Clean up the path
            diagram_path = diagram_path.strip()
            
            return ReferenceInfo(
                doc_path=doc_path,
                line_number=line_num,
                original_text=match.group(0),
                diagram_path=diagram_path,
                alt_text=alt_text
            )
        except Exception as e:
            print(f"   ⚠️  Error parsing reference: {e}")
            return None
    
    def _has_source_file(self, diagram: DiagramInfo) -> bool:
        """Check if a generated diagram has a corresponding source file"""
        if diagram.category != 'generated':
            return False
        
        # Look for corresponding .puml or .mmd files
        source_extensions = ['.puml', '.mmd']
        for ext in source_extensions:
            source_path = diagram.path.with_suffix(ext)
            if source_path.exists():
                return True
        
        # Look in viewpoints directory
        if diagram.viewpoint:
            viewpoint_dir = self.diagrams_dir / 'viewpoints' / diagram.viewpoint
            for ext in source_extensions:
                source_path = viewpoint_dir / f"{diagram.name}{ext}"
                if source_path.exists():
                    return True
        
        return False
    
    def _find_correct_diagram_path(self, ref: ReferenceInfo) -> Optional[str]:
        """Find the correct path for a broken reference"""
        # Extract the diagram name from the broken path
        broken_path = Path(ref.diagram_path)
        diagram_name = broken_path.stem
        
        # Look for diagrams with matching names
        candidates = []
        for diagram in self.diagrams:
            if diagram.name.lower() == diagram_name.lower():
                candidates.append(diagram)
        
        if not candidates:
            # Try fuzzy matching
            for diagram in self.diagrams:
                if diagram_name.lower() in diagram.name.lower() or diagram.name.lower() in diagram_name.lower():
                    candidates.append(diagram)
        
        if candidates:
            # Prefer PNG over SVG for GitHub display
            png_candidates = [c for c in candidates if c.format == 'png']
            if png_candidates:
                best_candidate = png_candidates[0]
            else:
                best_candidate = candidates[0]
            
            # Calculate relative path from document to diagram
            doc_dir = ref.doc_path.parent
            relative_path = os.path.relpath(best_candidate.path, doc_dir)
            return relative_path.replace('\\', '/')  # Use forward slashes
        
        return None
    
    def _update_reference_in_file(self, ref: ReferenceInfo, new_path: str) -> None:
        """Update a diagram reference in a file"""
        try:
            with open(ref.doc_path, 'r', encoding='utf-8') as f:
                lines = f.readlines()
            
            # Update the specific line
            old_line = lines[ref.line_number - 1]
            new_line = old_line.replace(ref.diagram_path, new_path)
            lines[ref.line_number - 1] = new_line
            
            with open(ref.doc_path, 'w', encoding='utf-8') as f:
                f.writelines(lines)
                
        except Exception as e:
            print(f"   ⚠️  Error updating {ref.doc_path}: {e}")
    
    def _find_target_documents(self, diagram: DiagramInfo) -> List[DocumentInfo]:
        """Find documents that should reference a diagram"""
        candidates = []
        
        # Match by viewpoint/perspective
        for doc in self.documents:
            if diagram.viewpoint and doc.viewpoint == diagram.viewpoint:
                candidates.append(doc)
            elif diagram.perspective and doc.perspective == diagram.perspective:
                candidates.append(doc)
        
        return candidates
    
    def _should_add_reference(self, diagram: DiagramInfo, doc: DocumentInfo) -> bool:
        """Check if a diagram reference should be added to a document"""
        # Don't add if already referenced
        for ref in self.references:
            if ref.doc_path == doc.path and diagram.name.lower() in ref.diagram_path.lower():
                return False
        
        # Add specific logic for different diagram types
        if diagram.name.lower().endswith('-overview') and doc.name == 'README':
            return True
        
        if 'aggregate-details' in diagram.name.lower() and doc.name == 'aggregates':
            return True
        
        return False
    
    def _add_diagram_reference(self, diagram: DiagramInfo, doc: DocumentInfo) -> None:
        """Add a diagram reference to a document"""
        try:
            with open(doc.path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Calculate relative path
            relative_path = os.path.relpath(diagram.path, doc.path.parent)
            relative_path = relative_path.replace('\\', '/')
            
            # Create reference text
            alt_text = diagram.name.replace('-', ' ').title()
            reference_text = f"\n![{alt_text}]({relative_path})\n"
            
            # Find appropriate insertion point
            insertion_point = self._find_insertion_point(content, diagram)
            
            # Insert the reference
            lines = content.split('\n')
            lines.insert(insertion_point, reference_text.strip())
            
            with open(doc.path, 'w', encoding='utf-8') as f:
                f.write('\n'.join(lines))
                
        except Exception as e:
            print(f"   ⚠️  Error adding reference to {doc.path}: {e}")
    
    def _find_insertion_point(self, content: str, diagram: DiagramInfo) -> int:
        """Find the best place to insert a diagram reference"""
        lines = content.split('\n')
        
        # Look for relevant sections
        for i, line in enumerate(lines):
            if diagram.name.lower() in line.lower():
                return i + 1
            if 'diagram' in line.lower() and '##' in line:
                return i + 1
        
        # Default to end of file
        return len(lines)
    
    def generate_report(self) -> Dict:
        """Generate a comprehensive synchronization report"""
        report = {
            'summary': {
                'diagrams_found': len(self.diagrams),
                'documents_found': len(self.documents),
                'references_found': len(self.references),
                'broken_references': len(self.broken_references),
                'orphaned_diagrams': len(self.orphaned_diagrams),
                'updates_made': len(self.updates_made)
            },
            'broken_references': [
                {
                    'document': str(ref.doc_path),
                    'line': ref.line_number,
                    'broken_path': ref.diagram_path,
                    'alt_text': ref.alt_text
                }
                for ref in self.broken_references
            ],
            'orphaned_diagrams': [
                {
                    'path': str(diagram.path),
                    'name': diagram.name,
                    'category': diagram.category,
                    'viewpoint': diagram.viewpoint,
                    'perspective': diagram.perspective
                }
                for diagram in self.orphaned_diagrams
            ],
            'updates_made': self.updates_made,
            'recommendations': self._generate_recommendations()
        }
        
        return report
    
    def _generate_recommendations(self) -> List[str]:
        """Generate recommendations for manual actions"""
        recommendations = []
        
        if self.broken_references:
            recommendations.append(f"Review {len(self.broken_references)} broken references that couldn't be automatically fixed")
        
        if self.orphaned_diagrams:
            recommendations.append(f"Consider adding references to {len(self.orphaned_diagrams)} orphaned diagrams")
        
        # Check for missing PNG files
        puml_without_png = []
        for diagram in self.diagrams:
            if diagram.format == 'puml':
                png_path = diagram.path.with_suffix('.png')
                if not png_path.exists():
                    puml_without_png.append(diagram)
        
        if puml_without_png:
            recommendations.append(f"Generate PNG images for {len(puml_without_png)} PlantUML files using: ./scripts/generate-diagrams.sh --format=png")
        
        return recommendations
    
    def run_comprehensive_sync(self) -> Dict:
        """Run the complete synchronization process"""
        print("🚀 Starting comprehensive diagram-documentation synchronization...")
        print()
        
        # Discovery phase
        self.discover_diagrams()
        self.discover_documents()
        
        # Analysis phase
        self.analyze_references()
        self.validate_references()
        self.find_orphaned_diagrams()
        
        # Fix phase
        self.generate_png_from_plantuml()
        self.fix_broken_references()
        self.add_missing_references()
        
        # Generate report
        report = self.generate_report()
        
        print()
        print("📊 Synchronization Summary:")
        print(f"   📁 Diagrams found: {report['summary']['diagrams_found']}")
        print(f"   📄 Documents found: {report['summary']['documents_found']}")
        print(f"   🔗 References found: {report['summary']['references_found']}")
        print(f"   ❌ Broken references: {report['summary']['broken_references']}")
        print(f"   🔍 Orphaned diagrams: {report['summary']['orphaned_diagrams']}")
        print(f"   ✅ Updates made: {report['summary']['updates_made']}")
        
        if report['updates_made']:
            print()
            print("🔧 Updates Made:")
            for update in report['updates_made']:
                print(f"   • {update}")
        
        if report['recommendations']:
            print()
            print("💡 Recommendations:")
            for rec in report['recommendations']:
                print(f"   • {rec}")
        
        return report

def main():
    parser = argparse.ArgumentParser(description='Intelligent Diagram-Documentation Synchronization')
    parser.add_argument('--mode', choices=['comprehensive', 'diagram-to-docs', 'docs-to-diagram'], 
                       default='comprehensive', help='Synchronization mode')
    parser.add_argument('--validate', action='store_true', help='Only validate, do not fix')
    parser.add_argument('--report', action='store_true', help='Generate detailed report')
    parser.add_argument('--output', help='Output file for report (JSON format)')
    
    args = parser.parse_args()
    
    sync = DiagramDocumentationSync()
    
    # Always run comprehensive sync for now
    report = sync.run_comprehensive_sync()
    
    if args.report or args.output:
        if args.output:
            with open(args.output, 'w') as f:
                json.dump(report, f, indent=2)
            print(f"\n📄 Report saved to: {args.output}")
        else:
            print(f"\n📄 Full Report:")
            print(json.dumps(report, indent=2))

if __name__ == '__main__':
    main()