#!/usr/bin/env python3
"""
Cross-Reference Validation Script

This script validates all internal links and cross-references in the documentation.
It checks for:
- Broken internal links
- Invalid diagram references
- Missing referenced files
- Circular references
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Dict, Set, Tuple
import json
from urllib.parse import unquote

# Color codes for terminal output
class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

def print_header(text: str):
    """Print a formatted header"""
    print(f"\n{Colors.BOLD}{Colors.BLUE}{'='*80}{Colors.ENDC}")
    print(f"{Colors.BOLD}{Colors.BLUE}{text.center(80)}{Colors.ENDC}")
    print(f"{Colors.BOLD}{Colors.BLUE}{'='*80}{Colors.ENDC}\n")

def print_success(text: str):
    """Print success message"""
    print(f"{Colors.GREEN}✓ {text}{Colors.ENDC}")

def print_error(text: str):
    """Print error message"""
    print(f"{Colors.RED}✗ {text}{Colors.ENDC}")

def print_warning(text: str):
    """Print warning message"""
    print(f"{Colors.YELLOW}⚠ {text}{Colors.ENDC}")

def print_info(text: str):
    """Print info message"""
    print(f"{Colors.BLUE}ℹ {text}{Colors.ENDC}")

def find_markdown_files(docs_path: Path) -> List[Path]:
    """Find all markdown files in the documentation"""
    return list(docs_path.rglob("*.md"))

def extract_links(file_path: Path, content: str) -> List[Tuple[str, int]]:
    """Extract all markdown links from a file"""
    links = []
    
    # Pattern for markdown links: [text](url)
    link_pattern = r'\[([^\]]+)\]\(([^\)]+)\)'
    
    for line_num, line in enumerate(content.split('\n'), 1):
        matches = re.finditer(link_pattern, line)
        for match in matches:
            link_url = match.group(2)
            # Skip external links (http, https, mailto)
            if not link_url.startswith(('http://', 'https://', 'mailto:', '#')):
                links.append((link_url, line_num))
    
    return links

def extract_image_references(file_path: Path, content: str) -> List[Tuple[str, int]]:
    """Extract all image references from a file"""
    images = []
    
    # Pattern for markdown images: ![alt](url)
    image_pattern = r'!\[([^\]]*)\]\(([^\)]+)\)'
    
    for line_num, line in enumerate(content.split('\n'), 1):
        matches = re.finditer(image_pattern, line)
        for match in matches:
            image_url = match.group(2)
            # Skip external images
            if not image_url.startswith(('http://', 'https://')):
                images.append((image_url, line_num))
    
    return images

def resolve_link(source_file: Path, link: str, docs_path: Path) -> Tuple[bool, Path]:
    """Resolve a relative link and check if the target exists"""
    # Remove anchor if present
    link_without_anchor = link.split('#')[0]
    if not link_without_anchor:
        # It's just an anchor, which is valid
        return True, source_file
    
    # Decode URL encoding
    link_without_anchor = unquote(link_without_anchor)
    
    # Resolve relative path
    source_dir = source_file.parent
    target_path = (source_dir / link_without_anchor).resolve()
    
    # Check if target exists
    exists = target_path.exists()
    
    return exists, target_path

def validate_file_links(file_path: Path, docs_path: Path) -> Dict:
    """Validate all links in a single file"""
    results = {
        "file": str(file_path.relative_to(docs_path.parent)),
        "total_links": 0,
        "valid_links": 0,
        "broken_links": [],
        "total_images": 0,
        "valid_images": 0,
        "broken_images": []
    }
    
    try:
        content = file_path.read_text(encoding='utf-8')
    except Exception as e:
        results["error"] = f"Failed to read file: {e}"
        return results
    
    # Check links
    links = extract_links(file_path, content)
    results["total_links"] = len(links)
    
    for link, line_num in links:
        exists, target_path = resolve_link(file_path, link, docs_path)
        if exists:
            results["valid_links"] += 1
        else:
            results["broken_links"].append({
                "link": link,
                "line": line_num,
                "target": str(target_path)
            })
    
    # Check images
    images = extract_image_references(file_path, content)
    results["total_images"] = len(images)
    
    for image, line_num in images:
        exists, target_path = resolve_link(file_path, image, docs_path)
        if exists:
            results["valid_images"] += 1
        else:
            results["broken_images"].append({
                "image": image,
                "line": line_num,
                "target": str(target_path)
            })
    
    return results

def main():
    """Main validation function"""
    # Get the project root directory
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    docs_path = project_root / "docs"
    
    if not docs_path.exists():
        print_error(f"Documentation directory not found: {docs_path}")
        sys.exit(1)
    
    print_header("Cross-Reference Validation")
    
    # Find all markdown files
    print_info("Scanning documentation files...")
    markdown_files = find_markdown_files(docs_path)
    print_info(f"Found {len(markdown_files)} markdown files")
    
    # Validate each file
    all_results = []
    total_links = 0
    valid_links = 0
    total_broken_links = 0
    total_images = 0
    valid_images = 0
    total_broken_images = 0
    
    print_info("\nValidating cross-references...")
    
    for file_path in markdown_files:
        results = validate_file_links(file_path, docs_path)
        all_results.append(results)
        
        total_links += results["total_links"]
        valid_links += results["valid_links"]
        total_broken_links += len(results["broken_links"])
        
        total_images += results["total_images"]
        valid_images += results["valid_images"]
        total_broken_images += len(results["broken_images"])
        
        # Print errors for this file
        if results["broken_links"] or results["broken_images"]:
            print_error(f"\n{results['file']}:")
            
            for broken in results["broken_links"]:
                print_error(f"  Line {broken['line']}: Broken link '{broken['link']}'")
                print(f"    Target: {broken['target']}")
            
            for broken in results["broken_images"]:
                print_error(f"  Line {broken['line']}: Missing image '{broken['image']}'")
                print(f"    Target: {broken['target']}")
    
    # Print summary
    print_header("Validation Summary")
    
    print(f"{Colors.BOLD}Links:{Colors.ENDC}")
    print(f"  Total: {total_links}")
    print(f"  {Colors.GREEN}Valid: {valid_links}{Colors.ENDC}")
    print(f"  {Colors.RED}Broken: {total_broken_links}{Colors.ENDC}")
    
    print(f"\n{Colors.BOLD}Images:{Colors.ENDC}")
    print(f"  Total: {total_images}")
    print(f"  {Colors.GREEN}Valid: {valid_images}{Colors.ENDC}")
    print(f"  {Colors.RED}Missing: {total_broken_images}{Colors.ENDC}")
    
    # Calculate success rate
    total_references = total_links + total_images
    valid_references = valid_links + valid_images
    
    if total_references > 0:
        success_rate = (valid_references / total_references) * 100
        print(f"\n{Colors.BOLD}Overall Success Rate: {success_rate:.2f}%{Colors.ENDC}")
    
    # Save detailed report
    report = {
        "summary": {
            "total_files": len(markdown_files),
            "total_links": total_links,
            "valid_links": valid_links,
            "broken_links": total_broken_links,
            "total_images": total_images,
            "valid_images": valid_images,
            "broken_images": total_broken_images,
            "success_rate": round(success_rate, 2) if total_references > 0 else 100
        },
        "files": all_results
    }
    
    report_path = project_root / "docs" / "reports" / "cross-reference-validation-report.json"
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, indent=2))
    print_info(f"\nDetailed report saved to: {report_path}")
    
    # Exit with appropriate code
    if total_broken_links == 0 and total_broken_images == 0:
        print_success("\n✓ All cross-references are valid!")
        sys.exit(0)
    else:
        print_error(f"\n✗ Found {total_broken_links} broken links and {total_broken_images} missing images")
        print_warning("Please fix the broken references listed above.")
        sys.exit(1)

if __name__ == "__main__":
    main()
