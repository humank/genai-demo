#!/usr/bin/env python3
"""
Development Viewpoint Link Validation Script

This script validates all links within the development viewpoint structure,
including internal links, diagram references, and cross-references.
"""

import os
import re
import sys
from pathlib import Path
from typing import Dict, List, Set, Tuple
from urllib.parse import urlparse
import json

class LinkValidator:
    def __init__(self, base_path: str = "."):
        self.base_path = Path(base_path)
        self.development_viewpoint_path = self.base_path / "docs" / "viewpoints" / "development"
        self.diagrams_path = self.base_path / "docs" / "diagrams" / "viewpoints" / "development"
        self.broken_links = []
        self.valid_links = []
        self.diagram_references = []
        self.external_links = []
        
    def find_markdown_files(self, directory: Path) -> List[Path]:
        """Find all markdown files in the given directory and subdirectories."""
        markdown_files = []
        if directory.exists():
            for file_path in directory.rglob("*.md"):
                markdown_files.append(file_path)
        return markdown_files
    
    def extract_links_from_file(self, file_path: Path) -> List[Tuple[str, int, str]]:
        """Extract all links from a markdown file."""
        links = []
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                
            # Find markdown links [text](url)
            markdown_link_pattern = r'\[([^\]]*)\]\(([^)]+)\)'
            for line_num, line in enumerate(content.split('\n'), 1):
                for match in re.finditer(markdown_link_pattern, line):
                    link_text = match.group(1)
                    link_url = match.group(2)
                    links.append((link_url, line_num, link_text))
                    
            # Find HTML img tags
            img_pattern = r'<img[^>]+src=["\']([^"\']+)["\'][^>]*>'
            for line_num, line in enumerate(content.split('\n'), 1):
                for match in re.finditer(img_pattern, line):
                    img_src = match.group(1)
                    links.append((img_src, line_num, "HTML img"))
                    
            # Find mermaid diagram references
            mermaid_pattern = r'```mermaid\s*\n(.*?)\n```'
            for match in re.finditer(mermaid_pattern, content, re.DOTALL):
                # This is an inline mermaid diagram, not a link
                pass
                
        except Exception as e:
            print(f"Error reading file {file_path}: {e}")
            
        return links
    
    def is_external_link(self, url: str) -> bool:
        """Check if a link is external (starts with http/https)."""
        return url.startswith(('http://', 'https://'))
    
    def is_anchor_link(self, url: str) -> bool:
        """Check if a link is an anchor link (starts with #)."""
        return url.startswith('#')
    
    def resolve_relative_path(self, base_file: Path, relative_url: str) -> Path:
        """Resolve a relative URL to an absolute path."""
        # Remove anchor fragments
        url_without_anchor = relative_url.split('#')[0]
        if not url_without_anchor:
            return base_file  # Pure anchor link
            
        # Resolve relative to the base file's directory
        base_dir = base_file.parent
        resolved_path = (base_dir / url_without_anchor).resolve()
        return resolved_path
    
    def validate_file_link(self, base_file: Path, link_url: str) -> bool:
        """Validate that a file link exists."""
        if self.is_external_link(link_url) or self.is_anchor_link(link_url):
            return True
            
        resolved_path = self.resolve_relative_path(base_file, link_url)
        return resolved_path.exists()
    
    def validate_diagram_references(self) -> Dict[str, List[str]]:
        """Validate that all diagram references point to existing files."""
        diagram_issues = {}
        
        # Check for diagram files that should exist
        expected_diagrams = [
            "architecture/microservices-overview.mmd",
            "architecture/saga-orchestration.mmd", 
            "architecture/distributed-system.mmd",
            "architecture/circuit-breaker-pattern.mmd",
            "workflows/development-workflow.mmd",
            "workflows/tdd-cycle.mmd",
            "workflows/bdd-process.mmd",
            "workflows/code-review-process.mmd",
            "testing/test-pyramid.mmd",
            "testing/performance-testing.mmd",
            "infrastructure/ci-cd-pipeline.mmd",
            "infrastructure/monitoring-architecture.mmd"
        ]
        
        for diagram in expected_diagrams:
            diagram_path = self.diagrams_path / diagram
            if not diagram_path.exists():
                if "missing_diagrams" not in diagram_issues:
                    diagram_issues["missing_diagrams"] = []
                diagram_issues["missing_diagrams"].append(str(diagram))
        
        return diagram_issues
    
    def validate_all_links(self) -> Dict[str, any]:
        """Validate all links in the development viewpoint."""
        results = {
            "total_files_checked": 0,
            "total_links_checked": 0,
            "broken_links": [],
            "valid_links": [],
            "external_links": [],
            "diagram_issues": {},
            "summary": {}
        }
        
        # Find all markdown files in development viewpoint
        markdown_files = self.find_markdown_files(self.development_viewpoint_path)
        results["total_files_checked"] = len(markdown_files)
        
        print(f"Validating links in {len(markdown_files)} markdown files...")
        
        for file_path in markdown_files:
            print(f"Checking: {file_path.relative_to(self.base_path)}")
            links = self.extract_links_from_file(file_path)
            
            for link_url, line_num, link_text in links:
                results["total_links_checked"] += 1
                
                if self.is_external_link(link_url):
                    results["external_links"].append({
                        "file": str(file_path.relative_to(self.base_path)),
                        "line": line_num,
                        "url": link_url,
                        "text": link_text
                    })
                elif self.is_anchor_link(link_url):
                    # For now, assume anchor links are valid
                    results["valid_links"].append({
                        "file": str(file_path.relative_to(self.base_path)),
                        "line": line_num,
                        "url": link_url,
                        "text": link_text
                    })
                else:
                    # Validate internal file links
                    if self.validate_file_link(file_path, link_url):
                        results["valid_links"].append({
                            "file": str(file_path.relative_to(self.base_path)),
                            "line": line_num,
                            "url": link_url,
                            "text": link_text
                        })
                    else:
                        results["broken_links"].append({
                            "file": str(file_path.relative_to(self.base_path)),
                            "line": line_num,
                            "url": link_url,
                            "text": link_text,
                            "resolved_path": str(self.resolve_relative_path(file_path, link_url))
                        })
        
        # Validate diagram references
        results["diagram_issues"] = self.validate_diagram_references()
        
        # Generate summary
        results["summary"] = {
            "total_files": results["total_files_checked"],
            "total_links": results["total_links_checked"],
            "broken_count": len(results["broken_links"]),
            "valid_count": len(results["valid_links"]),
            "external_count": len(results["external_links"]),
            "success_rate": (len(results["valid_links"]) / results["total_links_checked"] * 100) if results["total_links_checked"] > 0 else 0
        }
        
        return results
    
    def generate_report(self, results: Dict[str, any]) -> str:
        """Generate a detailed validation report."""
        report = []
        report.append("# Development Viewpoint Link Validation Report")
        report.append(f"Generated: {Path.cwd()}")
        report.append("")
        
        # Summary
        summary = results["summary"]
        report.append("## Summary")
        report.append(f"- **Total Files Checked**: {summary['total_files']}")
        report.append(f"- **Total Links Checked**: {summary['total_links']}")
        report.append(f"- **Valid Links**: {summary['valid_count']}")
        report.append(f"- **Broken Links**: {summary['broken_count']}")
        report.append(f"- **External Links**: {summary['external_count']}")
        report.append(f"- **Success Rate**: {summary['success_rate']:.1f}%")
        report.append("")
        
        # Broken Links
        if results["broken_links"]:
            report.append("## ❌ Broken Links")
            for link in results["broken_links"]:
                report.append(f"- **File**: `{link['file']}`")
                report.append(f"  - **Line**: {link['line']}")
                report.append(f"  - **URL**: `{link['url']}`")
                report.append(f"  - **Text**: {link['text']}")
                report.append(f"  - **Resolved Path**: `{link['resolved_path']}`")
                report.append("")
        else:
            report.append("## ✅ No Broken Links Found")
            report.append("")
        
        # Diagram Issues
        if results["diagram_issues"]:
            report.append("## 📊 Diagram Issues")
            for issue_type, issues in results["diagram_issues"].items():
                if issue_type == "missing_diagrams":
                    report.append("### Missing Diagrams")
                    for diagram in issues:
                        report.append(f"- `{diagram}`")
                    report.append("")
        
        # External Links
        if results["external_links"]:
            report.append("## 🌐 External Links")
            report.append("*Note: External links are not automatically validated*")
            for link in results["external_links"][:10]:  # Show first 10
                report.append(f"- `{link['url']}` in `{link['file']}:{link['line']}`")
            if len(results["external_links"]) > 10:
                report.append(f"- ... and {len(results['external_links']) - 10} more")
            report.append("")
        
        return "\n".join(report)

def main():
    """Main function to run link validation."""
    validator = LinkValidator()
    
    print("🔍 Starting Development Viewpoint Link Validation...")
    print("=" * 60)
    
    results = validator.validate_all_links()
    
    # Generate and save report
    report = validator.generate_report(results)
    report_path = Path("build/reports/development-viewpoint-link-validation.md")
    report_path.parent.mkdir(parents=True, exist_ok=True)
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    # Also save JSON results
    json_path = Path("build/reports/development-viewpoint-link-validation.json")
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    
    print("\n" + "=" * 60)
    print("📊 VALIDATION RESULTS")
    print("=" * 60)
    print(f"Total Files Checked: {results['summary']['total_files']}")
    print(f"Total Links Checked: {results['summary']['total_links']}")
    print(f"Valid Links: {results['summary']['valid_count']}")
    print(f"Broken Links: {results['summary']['broken_count']}")
    print(f"External Links: {results['summary']['external_count']}")
    print(f"Success Rate: {results['summary']['success_rate']:.1f}%")
    
    if results["broken_links"]:
        print("\n❌ BROKEN LINKS FOUND:")
        for link in results["broken_links"]:
            print(f"  - {link['file']}:{link['line']} -> {link['url']}")
    
    if results["diagram_issues"]:
        print("\n📊 DIAGRAM ISSUES:")
        for issue_type, issues in results["diagram_issues"].items():
            print(f"  {issue_type}: {len(issues)} issues")
    
    print(f"\n📄 Detailed report saved to: {report_path}")
    print(f"📄 JSON data saved to: {json_path}")
    
    # Return appropriate exit code
    if results["broken_links"] or results["diagram_issues"]:
        print("\n⚠️  Validation completed with issues!")
        return 1
    else:
        print("\n✅ All links validated successfully!")
        return 0

if __name__ == "__main__":
    sys.exit(main())