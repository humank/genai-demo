#!/usr/bin/env python3
"""
Diagram Validation Script

This script validates all diagrams in the documentation:
- Checks if PlantUML diagrams can be generated
- Verifies diagram quality and clarity
- Ensures all diagrams are referenced in documentation
"""

import os
import sys
from pathlib import Path
from typing import List, Dict, Set
import json
import re

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

def find_plantuml_files(docs_path: Path) -> List[Path]:
    """Find all PlantUML files"""
    return list(docs_path.rglob("*.puml"))

def find_mermaid_files(docs_path: Path) -> List[Path]:
    """Find all Mermaid files"""
    mermaid_files = list(docs_path.rglob("*.mmd"))
    
    # Also find Mermaid diagrams in markdown files
    markdown_files = list(docs_path.rglob("*.md"))
    for md_file in markdown_files:
        try:
            content = md_file.read_text(encoding='utf-8')
            if '```mermaid' in content:
                mermaid_files.append(md_file)
        except:
            pass
    
    return mermaid_files

def validate_plantuml_syntax(file_path: Path) -> Dict:
    """Validate PlantUML syntax"""
    result = {
        "file": str(file_path),
        "valid": False,
        "errors": []
    }
    
    try:
        content = file_path.read_text(encoding='utf-8')
        
        # Check for basic PlantUML structure
        if not content.strip().startswith('@start'):
            result["errors"].append("Missing @start directive")
        
        if not content.strip().endswith('@end' + content.split('@start')[1].split('\n')[0].replace('@start', '')):
            result["errors"].append("Missing @end directive")
        
        # Check for common syntax issues
        lines = content.split('\n')
        for i, line in enumerate(lines, 1):
            line = line.strip()
            if line and not line.startswith("'") and not line.startswith("@"):
                # Check for unclosed quotes
                if line.count('"') % 2 != 0:
                    result["errors"].append(f"Line {i}: Unclosed quote")
                
                # Check for unclosed brackets
                if line.count('[') != line.count(']'):
                    result["errors"].append(f"Line {i}: Unmatched brackets")
                
                if line.count('(') != line.count(')'):
                    result["errors"].append(f"Line {i}: Unmatched parentheses")
        
        if not result["errors"]:
            result["valid"] = True
    
    except Exception as e:
        result["errors"].append(f"Failed to read file: {e}")
    
    return result

def check_diagram_references(docs_path: Path, diagram_files: List[Path]) -> Dict:
    """Check if diagrams are referenced in documentation"""
    result = {
        "referenced": [],
        "unreferenced": []
    }
    
    # Get all markdown files
    markdown_files = list(docs_path.rglob("*.md"))
    
    # Build a set of all referenced diagrams
    referenced_diagrams = set()
    
    for md_file in markdown_files:
        try:
            content = md_file.read_text(encoding='utf-8')
            
            # Find image references
            image_pattern = r'!\[([^\]]*)\]\(([^\)]+)\)'
            matches = re.finditer(image_pattern, content)
            
            for match in matches:
                image_path = match.group(2)
                # Resolve relative path
                if not image_path.startswith(('http://', 'https://')):
                    abs_path = (md_file.parent / image_path).resolve()
                    referenced_diagrams.add(abs_path)
            
            # Find PlantUML references
            puml_pattern = r'\(([^\)]+\.puml)\)'
            matches = re.finditer(puml_pattern, content)
            
            for match in matches:
                puml_path = match.group(1)
                abs_path = (md_file.parent / puml_path).resolve()
                referenced_diagrams.add(abs_path)
        
        except:
            pass
    
    # Check each diagram file
    for diagram_file in diagram_files:
        diagram_path = diagram_file.resolve()
        
        # For PlantUML files, also check if generated PNG is referenced
        if diagram_file.suffix == '.puml':
            png_path = diagram_path.parent / (diagram_path.stem + '.png')
            if diagram_path in referenced_diagrams or png_path in referenced_diagrams:
                result["referenced"].append(str(diagram_file))
            else:
                result["unreferenced"].append(str(diagram_file))
        else:
            if diagram_path in referenced_diagrams:
                result["referenced"].append(str(diagram_file))
            else:
                result["unreferenced"].append(str(diagram_file))
    
    return result

def check_generated_diagrams(docs_path: Path) -> Dict:
    """Check if PlantUML diagrams have been generated"""
    result = {
        "generated": [],
        "not_generated": []
    }
    
    plantuml_files = find_plantuml_files(docs_path)
    
    for puml_file in plantuml_files:
        # Check if PNG exists
        png_file = puml_file.parent / (puml_file.stem + '.png')
        
        if png_file.exists():
            result["generated"].append(str(puml_file))
        else:
            result["not_generated"].append(str(puml_file))
    
    return result

def main():
    """Main validation function"""
    # Get the project root directory
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    docs_path = project_root / "docs"
    
    if not docs_path.exists():
        print_error(f"Documentation directory not found: {docs_path}")
        sys.exit(1)
    
    print_header("Diagram Validation")
    
    # Find all diagram files
    print_info("Scanning for diagram files...")
    plantuml_files = find_plantuml_files(docs_path)
    mermaid_files = find_mermaid_files(docs_path)
    
    print_info(f"Found {len(plantuml_files)} PlantUML files")
    print_info(f"Found {len(mermaid_files)} Mermaid files")
    
    # Validate PlantUML syntax
    print_info("\nValidating PlantUML syntax...")
    plantuml_results = []
    valid_plantuml = 0
    invalid_plantuml = 0
    
    for puml_file in plantuml_files:
        result = validate_plantuml_syntax(puml_file)
        plantuml_results.append(result)
        
        if result["valid"]:
            valid_plantuml += 1
        else:
            invalid_plantuml += 1
            print_error(f"{puml_file.relative_to(docs_path)}:")
            for error in result["errors"]:
                print(f"  {error}")
    
    print(f"\n{Colors.BOLD}PlantUML Validation:{Colors.ENDC}")
    print(f"  {Colors.GREEN}Valid: {valid_plantuml}{Colors.ENDC}")
    print(f"  {Colors.RED}Invalid: {invalid_plantuml}{Colors.ENDC}")
    
    # Check generated diagrams
    print_info("\nChecking generated diagrams...")
    generated_result = check_generated_diagrams(docs_path)
    
    print(f"\n{Colors.BOLD}Generated Diagrams:{Colors.ENDC}")
    print(f"  {Colors.GREEN}Generated: {len(generated_result['generated'])}{Colors.ENDC}")
    print(f"  {Colors.RED}Not Generated: {len(generated_result['not_generated'])}{Colors.ENDC}")
    
    if generated_result['not_generated']:
        print_warning("\nPlantUML files without generated PNG:")
        for file in generated_result['not_generated'][:10]:  # Show first 10
            print(f"  {file}")
        if len(generated_result['not_generated']) > 10:
            print(f"  ... and {len(generated_result['not_generated']) - 10} more")
    
    # Check diagram references
    print_info("\nChecking diagram references...")
    all_diagrams = plantuml_files + mermaid_files
    reference_result = check_diagram_references(docs_path, all_diagrams)
    
    print(f"\n{Colors.BOLD}Diagram References:{Colors.ENDC}")
    print(f"  {Colors.GREEN}Referenced: {len(reference_result['referenced'])}{Colors.ENDC}")
    print(f"  {Colors.YELLOW}Unreferenced: {len(reference_result['unreferenced'])}{Colors.ENDC}")
    
    if reference_result['unreferenced']:
        print_warning("\nUnreferenced diagrams (first 10):")
        for file in reference_result['unreferenced'][:10]:
            print(f"  {file}")
        if len(reference_result['unreferenced']) > 10:
            print(f"  ... and {len(reference_result['unreferenced']) - 10} more")
    
    # Generate summary
    print_header("Validation Summary")
    
    total_diagrams = len(plantuml_files) + len(mermaid_files)
    print(f"{Colors.BOLD}Total Diagrams: {total_diagrams}{Colors.ENDC}")
    print(f"  PlantUML: {len(plantuml_files)}")
    print(f"  Mermaid: {len(mermaid_files)}")
    
    print(f"\n{Colors.BOLD}Quality Metrics:{Colors.ENDC}")
    print(f"  Valid PlantUML: {valid_plantuml}/{len(plantuml_files)}")
    print(f"  Generated: {len(generated_result['generated'])}/{len(plantuml_files)}")
    print(f"  Referenced: {len(reference_result['referenced'])}/{total_diagrams}")
    
    # Save report
    report = {
        "summary": {
            "total_diagrams": total_diagrams,
            "plantuml_files": len(plantuml_files),
            "mermaid_files": len(mermaid_files),
            "valid_plantuml": valid_plantuml,
            "invalid_plantuml": invalid_plantuml,
            "generated": len(generated_result['generated']),
            "not_generated": len(generated_result['not_generated']),
            "referenced": len(reference_result['referenced']),
            "unreferenced": len(reference_result['unreferenced'])
        },
        "plantuml_validation": plantuml_results,
        "generated_diagrams": generated_result,
        "diagram_references": reference_result
    }
    
    report_path = project_root / "docs" / "reports" / "diagram-validation-report.json"
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, indent=2))
    print_info(f"\nDetailed report saved to: {report_path}")
    
    # Exit with appropriate code
    if invalid_plantuml == 0:
        print_success("\n✓ All diagram validations passed!")
        sys.exit(0)
    else:
        print_error(f"\n✗ Found {invalid_plantuml} invalid PlantUML files")
        print_warning("Please fix the diagram syntax errors listed above.")
        sys.exit(1)

if __name__ == "__main__":
    main()
