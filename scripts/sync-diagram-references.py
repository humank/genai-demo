#!/usr/bin/env python3
"""
Diagram-Documentation Synchronization Script

This script provides intelligent synchronization between diagrams and documentation,
ensuring that all diagram references in documentation are accurate and up-to-date.

Usage:
    python3 scripts/sync-diagram-references.py --mode=diagram-to-docs
    python3 scripts/sync-diagram-references.py --mode=docs-to-diagram
    python3 scripts/sync-diagram-references.py --comprehensive --validate --report
"""

import os
import re
import json
import argparse
from pathlib import Path
from typing import Dict, List, Set, Tuple, Optional
from dataclasses import dataclass
from datetime import datetime

@dataclass
class DiagramReference:
    """Represents a diagram reference in documentation"""
    file_path: str
    line_number: int
    reference_text: str
    diagram_path: str
    description: str
    exists: bool = False

@dataclass
class DiagramFile:
    """Represents a diagram file"""
    path: str
    type: str  # puml, mmd, excalidraw
    category: str  # viewpoint, perspective, legacy, root
    last_modified: datetime
    referenced_by: List[str]

@dataclass
class SyncReport:
    """Synchronization report"""
    diagrams_analyzed: int = 0
    docs_analyzed: int = 0
    references_added: int = 0
    references_removed: int = 0
    references_fixed: int = 0
    orphaned_diagrams: List[str] = None
    missing_diagrams: List[str] = None
    broken_references: List[DiagramReference] = None
    conflicts: List[str] = None

    def __post_init__(self):
        if self.orphaned_diagrams is None:
            self.orphaned_diagrams = []
        if self.missing_diagrams is None:
            self.missing_diagrams = []
        if self.broken_references is None:
            self.broken_references = []
        if self.conflicts is None:
            self.conflicts = []

class DiagramDocumentationSync:
    """Main synchronization class"""
    
    def __init__(self, root_path: str = "."):
        self.root_path = Path(root_path)
        self.diagrams_path = self.root_path / "docs" / "diagrams"
        self.viewpoints_path = self.root_path / "docs" / "viewpoints"
        self.perspectives_path = self.root_path / "docs" / "perspectives"
        self.en_viewpoints_path = self.root_path / "docs" / "en" / "viewpoints"
        self.en_perspectives_path = self.root_path / "docs" / "en" / "perspectives"
        
        self.diagram_extensions = {'.puml', '.mmd', '.excalidraw'}
        self.doc_extensions = {'.md'}
        
        self.report = SyncReport()
    
    def scan_diagrams(self) -> Dict[str, DiagramFile]:
        """Scan all diagram files and categorize them"""
        diagrams = {}
        
        if not self.diagrams_path.exists():
            print(f"Warning: Diagrams path {self.diagrams_path} does not exist")
            return diagrams
        
        for file_path in self.diagrams_path.rglob("*"):
            if file_path.suffix in self.diagram_extensions:
                relative_path = file_path.relative_to(self.root_path)
                category = self._categorize_diagram(relative_path)
                
                diagram = DiagramFile(
                    path=str(relative_path),
                    type=file_path.suffix[1:],  # Remove the dot
                    category=category,
                    last_modified=datetime.fromtimestamp(file_path.stat().st_mtime),
                    referenced_by=[]
                )
                
                diagrams[str(relative_path)] = diagram
                self.report.diagrams_analyzed += 1
        
        return diagrams
    
    def _categorize_diagram(self, path: Path) -> str:
        """Categorize diagram based on its path"""
        parts = path.parts
        
        if 'viewpoints' in parts:
            return 'viewpoint'
        elif 'perspectives' in parts:
            return 'perspective'
        elif 'legacy' in parts or 'plantuml' in parts or 'mermaid' in parts:
            return 'legacy'
        elif len(parts) == 2 and parts[0] == 'docs' and parts[1].startswith('diagrams'):
            return 'root'
        else:
            return 'other'
    
    def scan_documentation(self) -> Dict[str, List[DiagramReference]]:
        """Scan documentation files for diagram references"""
        doc_references = {}
        
        # Scan viewpoints and perspectives
        for docs_path in [self.viewpoints_path, self.perspectives_path, 
                         self.en_viewpoints_path, self.en_perspectives_path]:
            if docs_path.exists():
                for file_path in docs_path.rglob("*.md"):
                    relative_path = file_path.relative_to(self.root_path)
                    references = self._extract_diagram_references(file_path)
                    if references:
                        doc_references[str(relative_path)] = references
                    self.report.docs_analyzed += 1
        
        return doc_references
    
    def _extract_diagram_references(self, file_path: Path) -> List[DiagramReference]:
        """Extract diagram references from a markdown file"""
        references = []
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                lines = f.readlines()
            
            for line_num, line in enumerate(lines, 1):
                # Match markdown links to diagram files
                pattern = r'\[([^\]]+)\]\(([^)]+\.(?:puml|mmd|excalidraw))\)'
                matches = re.finditer(pattern, line)
                
                for match in matches:
                    description = match.group(1)
                    diagram_path = match.group(2)
                    
                    # Convert relative path to absolute path from root
                    if diagram_path.startswith('../../'):
                        # Remove ../../ and make it relative to root
                        clean_path = diagram_path[6:]
                    else:
                        clean_path = diagram_path
                    
                    # Check if diagram exists
                    full_diagram_path = self.root_path / clean_path
                    exists = full_diagram_path.exists()
                    
                    reference = DiagramReference(
                        file_path=str(file_path.relative_to(self.root_path)),
                        line_number=line_num,
                        reference_text=match.group(0),
                        diagram_path=diagram_path,  # Keep original path for display
                        description=description,
                        exists=exists
                    )
                    
                    references.append(reference)
        
        except Exception as e:
            print(f"Error reading {file_path}: {e}")
        
        return references
    
    def sync_diagram_to_docs(self, diagrams: Dict[str, DiagramFile], 
                           doc_references: Dict[str, List[DiagramReference]]) -> None:
        """Synchronize diagrams to documentation (add missing references)"""
        
        # Find diagrams that should be referenced but aren't
        referenced_diagrams = set()
        for refs in doc_references.values():
            for ref in refs:
                referenced_diagrams.add(ref.diagram_path)
        
        # Find orphaned diagrams
        for diagram_path, diagram in diagrams.items():
            if diagram_path not in referenced_diagrams:
                if diagram.category in ['viewpoint', 'perspective']:
                    self.report.orphaned_diagrams.append(diagram_path)
        
        # Add missing references based on naming conventions
        self._add_missing_references(diagrams, doc_references)
    
    def _add_missing_references(self, diagrams: Dict[str, DiagramFile], 
                               doc_references: Dict[str, List[DiagramReference]]) -> None:
        """Add missing diagram references based on conventions"""
        
        # Define reference rules
        reference_rules = {
            'docs/viewpoints/functional/domain-model.md': [
                'docs/diagrams/viewpoints/functional/domain-model-overview.puml',
                'docs/diagrams/viewpoints/functional/bounded-contexts-overview.puml',
                'docs/diagrams/hexagonal_architecture.mmd',
                'docs/diagrams/viewpoints/functional/application-services-overview.puml'
            ],
            'docs/viewpoints/functional/bounded-contexts.md': [
                'docs/diagrams/viewpoints/functional/bounded-contexts-overview.puml',
                'docs/diagrams/viewpoints/functional/event-storming-big-picture.puml',
                'docs/diagrams/viewpoints/functional/event-storming-process-level.puml',
                'docs/diagrams/viewpoints/functional/business-process-flows.puml'
            ],
            'docs/viewpoints/functional/aggregates.md': [
                'docs/diagrams/viewpoints/functional/customer-aggregate-details.puml',
                'docs/diagrams/viewpoints/functional/order-aggregate-details.puml',
                'docs/diagrams/viewpoints/functional/product-aggregate-details.puml',
                'docs/diagrams/viewpoints/functional/seller-aggregate-details.puml',
                'docs/diagrams/viewpoints/functional/domain-model-overview.puml'
            ],
            'docs/viewpoints/information/domain-events.md': [
                'docs/diagrams/viewpoints/functional/event-storming-big-picture.puml',
                'docs/diagrams/viewpoints/functional/event-storming-process-level.puml',
                'docs/diagrams/viewpoints/functional/domain-events-flow.puml',
                'docs/diagrams/event_driven_architecture.mmd',
                'docs/diagrams/viewpoints/functional/application-services-overview.puml'
            ]
        }
        
        for doc_path, expected_diagrams in reference_rules.items():
            if doc_path in doc_references:
                current_refs = {ref.diagram_path for ref in doc_references[doc_path]}
                missing_refs = set(expected_diagrams) - current_refs
                
                for missing_diagram in missing_refs:
                    if missing_diagram in diagrams:
                        print(f"Would add reference to {missing_diagram} in {doc_path}")
                        self.report.references_added += 1
                    else:
                        self.report.missing_diagrams.append(missing_diagram)
    
    def validate_references(self, doc_references: Dict[str, List[DiagramReference]]) -> None:
        """Validate all diagram references"""
        
        for doc_path, references in doc_references.items():
            for ref in references:
                if not ref.exists:
                    self.report.broken_references.append(ref)
    
    def fix_broken_references(self, doc_references: Dict[str, List[DiagramReference]], 
                             diagrams: Dict[str, DiagramFile]) -> None:
        """Fix broken references by finding correct paths"""
        
        for doc_path, references in doc_references.items():
            doc_file_path = self.root_path / doc_path
            if not doc_file_path.exists():
                continue
                
            try:
                with open(doc_file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                updated_content = content
                changes_made = False
                
                for ref in references:
                    if not ref.exists:
                        # Try to find the correct diagram path
                        correct_path = self._find_correct_diagram_path(ref.diagram_path, diagrams)
                        if correct_path:
                            # Calculate relative path from doc to diagram
                            doc_dir = doc_file_path.parent
                            diagram_full_path = self.root_path / correct_path
                            try:
                                relative_path = os.path.relpath(diagram_full_path, doc_dir)
                                # Replace the broken reference with the correct one
                                old_reference = f"[{ref.description}]({ref.diagram_path})"
                                new_reference = f"[{ref.description}]({relative_path})"
                                updated_content = updated_content.replace(old_reference, new_reference)
                                changes_made = True
                                self.report.references_fixed += 1
                                print(f"Fixed reference in {doc_path}: {ref.diagram_path} -> {relative_path}")
                            except ValueError:
                                # Can't calculate relative path, skip
                                continue
                
                if changes_made:
                    with open(doc_file_path, 'w', encoding='utf-8') as f:
                        f.write(updated_content)
                        
            except Exception as e:
                print(f"Error fixing references in {doc_path}: {e}")
    
    def _find_correct_diagram_path(self, broken_path: str, diagrams: Dict[str, DiagramFile]) -> Optional[str]:
        """Find the correct path for a broken diagram reference"""
        
        # Extract the filename from the broken path
        filename = os.path.basename(broken_path)
        
        # Look for diagrams with matching filename
        for diagram_path, diagram in diagrams.items():
            if os.path.basename(diagram_path) == filename:
                return diagram_path
        
        # If exact match not found, try fuzzy matching
        base_name = os.path.splitext(filename)[0]
        for diagram_path, diagram in diagrams.items():
            diagram_base = os.path.splitext(os.path.basename(diagram_path))[0]
            if base_name.lower() in diagram_base.lower() or diagram_base.lower() in base_name.lower():
                return diagram_path
        
        return None
    
    def generate_report(self) -> str:
        """Generate a comprehensive synchronization report"""
        
        report_lines = [
            "# Diagram-Documentation Synchronization Report",
            f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
            "",
            "## Summary",
            f"- Diagrams analyzed: {self.report.diagrams_analyzed}",
            f"- Documentation files analyzed: {self.report.docs_analyzed}",
            f"- References added: {self.report.references_added}",
            f"- References removed: {self.report.references_removed}",
            f"- References fixed: {self.report.references_fixed}",
            "",
        ]
        
        if self.report.orphaned_diagrams:
            report_lines.extend([
                "## Orphaned Diagrams",
                "These diagrams exist but are not referenced by any documentation:",
                ""
            ])
            for diagram in self.report.orphaned_diagrams:
                report_lines.append(f"- {diagram}")
            report_lines.append("")
        
        if self.report.missing_diagrams:
            report_lines.extend([
                "## Missing Diagrams",
                "These diagrams are referenced but do not exist:",
                ""
            ])
            for diagram in self.report.missing_diagrams:
                report_lines.append(f"- {diagram}")
            report_lines.append("")
        
        if self.report.broken_references:
            report_lines.extend([
                "## Broken References",
                "These references point to non-existent diagrams:",
                ""
            ])
            for ref in self.report.broken_references:
                report_lines.append(f"- {ref.file_path}:{ref.line_number} -> {ref.diagram_path}")
            report_lines.append("")
        
        return "\n".join(report_lines)
    
    def run_comprehensive_sync(self, validate: bool = True, report: bool = True, fix_broken: bool = True) -> str:
        """Run comprehensive synchronization"""
        
        print("Starting comprehensive diagram-documentation synchronization...")
        
        # Scan diagrams and documentation
        diagrams = self.scan_diagrams()
        doc_references = self.scan_documentation()
        
        print(f"Found {len(diagrams)} diagrams and {len(doc_references)} documentation files")
        
        # Perform synchronization
        self.sync_diagram_to_docs(diagrams, doc_references)
        
        # Validate references if requested
        if validate:
            self.validate_references(doc_references)
        
        # Fix broken references if requested
        if fix_broken and self.report.broken_references:
            print(f"Attempting to fix {len(self.report.broken_references)} broken references...")
            self.fix_broken_references(doc_references, diagrams)
            
            # Re-scan after fixes to update the report
            doc_references = self.scan_documentation()
            self.report.broken_references = []  # Reset broken references
            self.validate_references(doc_references)
        
        # Generate report if requested
        if report:
            return self.generate_report()
        
        return "Synchronization completed successfully"

def main():
    parser = argparse.ArgumentParser(description='Synchronize diagrams and documentation')
    parser.add_argument('--mode', choices=['diagram-to-docs', 'docs-to-diagram'],
                       help='Synchronization mode')
    parser.add_argument('--comprehensive', action='store_true', help='Run comprehensive synchronization')
    parser.add_argument('--validate', action='store_true', help='Validate all references')
    parser.add_argument('--report', action='store_true', help='Generate detailed report')
    parser.add_argument('--root', default='.', help='Root directory path')
    
    args = parser.parse_args()
    
    sync = DiagramDocumentationSync(args.root)
    
    if args.comprehensive or not args.mode:
        result = sync.run_comprehensive_sync(validate=args.validate, report=args.report, fix_broken=True)
        print(result)
    else:
        print(f"Mode {args.mode} not yet implemented")

if __name__ == '__main__':
    main()