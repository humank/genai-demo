#!/usr/bin/env python3
"""
Documentation Completeness Validation Script

This script validates that all required documentation is present according to
the Rozanski & Woods methodology and project requirements.

Requirements validated:
- All 7 viewpoints are documented
- All 8 perspectives are documented
- All bounded contexts are documented
- All API endpoints are documented
"""

import os
import sys
from pathlib import Path
from typing import List, Dict, Tuple
import json

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

# Required viewpoints according to Rozanski & Woods
REQUIRED_VIEWPOINTS = [
    "functional",
    "information",
    "concurrency",
    "development",
    "deployment",
    "operational",
    "context"
]

# Required perspectives according to Rozanski & Woods
REQUIRED_PERSPECTIVES = [
    "security",
    "performance",
    "availability",
    "evolution",
    "accessibility",
    "development-resource",
    "internationalization",
    "location"
]

# Bounded contexts in the system
BOUNDED_CONTEXTS = [
    "customer",
    "order",
    "product",
    "inventory",
    "payment",
    "shipping",
    "promotion",
    "notification",
    "review",
    "shopping-cart",
    "pricing",
    "seller",
    "delivery"
]

# Required API endpoint categories
API_ENDPOINT_CATEGORIES = [
    "customers",
    "orders",
    "products",
    "payments",
    "shopping-cart",
    "promotions",
    "inventory",
    "logistics",
    "notifications"
]

def check_viewpoints(docs_path: Path) -> Tuple[List[str], List[str]]:
    """Check if all required viewpoints are documented"""
    viewpoints_path = docs_path / "viewpoints"
    found = []
    missing = []
    
    for viewpoint in REQUIRED_VIEWPOINTS:
        viewpoint_dir = viewpoints_path / viewpoint
        if viewpoint_dir.exists() and viewpoint_dir.is_dir():
            # Check if README.md exists
            readme = viewpoint_dir / "README.md"
            if readme.exists():
                found.append(viewpoint)
            else:
                missing.append(f"{viewpoint} (directory exists but no README.md)")
        else:
            missing.append(viewpoint)
    
    return found, missing

def check_perspectives(docs_path: Path) -> Tuple[List[str], List[str]]:
    """Check if all required perspectives are documented"""
    perspectives_path = docs_path / "perspectives"
    found = []
    missing = []
    
    for perspective in REQUIRED_PERSPECTIVES:
        perspective_dir = perspectives_path / perspective
        if perspective_dir.exists() and perspective_dir.is_dir():
            # Check if README.md exists
            readme = perspective_dir / "README.md"
            if readme.exists():
                found.append(perspective)
            else:
                missing.append(f"{perspective} (directory exists but no README.md)")
        else:
            missing.append(perspective)
    
    return found, missing

def check_bounded_contexts(docs_path: Path) -> Tuple[List[str], List[str]]:
    """Check if all bounded contexts are documented"""
    functional_path = docs_path / "viewpoints" / "functional"
    found = []
    missing = []
    
    # Check if bounded-contexts.md exists
    bounded_contexts_doc = functional_path / "bounded-contexts.md"
    if not bounded_contexts_doc.exists():
        print_error("bounded-contexts.md not found in functional viewpoint")
        return found, BOUNDED_CONTEXTS
    
    # Read the file and check for each context
    try:
        content = bounded_contexts_doc.read_text()
        for context in BOUNDED_CONTEXTS:
            # Check if context is mentioned (case-insensitive)
            if context.lower() in content.lower() or context.replace("-", " ").lower() in content.lower():
                found.append(context)
            else:
                missing.append(context)
    except Exception as e:
        print_error(f"Error reading bounded-contexts.md: {e}")
        return found, BOUNDED_CONTEXTS
    
    return found, missing

def check_api_endpoints(docs_path: Path) -> Tuple[List[str], List[str]]:
    """Check if all API endpoint categories are documented"""
    api_rest_path = docs_path / "api" / "rest" / "endpoints"
    found = []
    missing = []
    
    if not api_rest_path.exists():
        print_error("API REST endpoints directory not found")
        return found, API_ENDPOINT_CATEGORIES
    
    for category in API_ENDPOINT_CATEGORIES:
        endpoint_doc = api_rest_path / f"{category}.md"
        if endpoint_doc.exists():
            found.append(category)
        else:
            missing.append(category)
    
    return found, missing

def check_additional_documentation(docs_path: Path) -> Dict[str, bool]:
    """Check for additional required documentation"""
    checks = {}
    
    # Check for main README
    checks["Main README"] = (docs_path / "README.md").exists()
    
    # Check for API documentation
    checks["API REST README"] = (docs_path / "api" / "rest" / "README.md").exists()
    checks["API Events README"] = (docs_path / "api" / "events" / "README.md").exists()
    
    # Check for development guides
    checks["Development Setup Guide"] = (docs_path / "development" / "setup" / "local-environment.md").exists()
    checks["Testing Strategy"] = (docs_path / "development" / "testing" / "testing-strategy.md").exists()
    
    # Check for operational documentation
    checks["Deployment Process"] = (docs_path / "operations" / "deployment" / "deployment-process.md").exists()
    checks["Monitoring Strategy"] = (docs_path / "operations" / "monitoring" / "monitoring-strategy.md").exists()
    checks["Runbooks Index"] = (docs_path / "operations" / "runbooks" / "README.md").exists()
    
    # Check for templates
    checks["Viewpoint Template"] = (docs_path / "templates" / "viewpoint-template.md").exists()
    checks["Perspective Template"] = (docs_path / "templates" / "perspective-template.md").exists()
    checks["ADR Template"] = (docs_path / "templates" / "adr-template.md").exists()
    checks["Runbook Template"] = (docs_path / "templates" / "runbook-template.md").exists()
    
    return checks

def generate_report(results: Dict) -> str:
    """Generate a JSON report of the validation results"""
    return json.dumps(results, indent=2)

def main():
    """Main validation function"""
    # Get the project root directory
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    docs_path = project_root / "docs"
    
    if not docs_path.exists():
        print_error(f"Documentation directory not found: {docs_path}")
        sys.exit(1)
    
    print_header("Documentation Completeness Validation")
    
    # Initialize results
    results = {
        "viewpoints": {},
        "perspectives": {},
        "bounded_contexts": {},
        "api_endpoints": {},
        "additional_docs": {},
        "summary": {}
    }
    
    # Check viewpoints
    print_info("Checking Viewpoints (7 required)...")
    found_viewpoints, missing_viewpoints = check_viewpoints(docs_path)
    results["viewpoints"]["found"] = found_viewpoints
    results["viewpoints"]["missing"] = missing_viewpoints
    results["viewpoints"]["count"] = len(found_viewpoints)
    results["viewpoints"]["total"] = len(REQUIRED_VIEWPOINTS)
    
    for viewpoint in found_viewpoints:
        print_success(f"Viewpoint documented: {viewpoint}")
    for viewpoint in missing_viewpoints:
        print_error(f"Viewpoint missing: {viewpoint}")
    
    print(f"\n{Colors.BOLD}Viewpoints: {len(found_viewpoints)}/{len(REQUIRED_VIEWPOINTS)} documented{Colors.ENDC}")
    
    # Check perspectives
    print_info("\nChecking Perspectives (8 required)...")
    found_perspectives, missing_perspectives = check_perspectives(docs_path)
    results["perspectives"]["found"] = found_perspectives
    results["perspectives"]["missing"] = missing_perspectives
    results["perspectives"]["count"] = len(found_perspectives)
    results["perspectives"]["total"] = len(REQUIRED_PERSPECTIVES)
    
    for perspective in found_perspectives:
        print_success(f"Perspective documented: {perspective}")
    for perspective in missing_perspectives:
        print_error(f"Perspective missing: {perspective}")
    
    print(f"\n{Colors.BOLD}Perspectives: {len(found_perspectives)}/{len(REQUIRED_PERSPECTIVES)} documented{Colors.ENDC}")
    
    # Check bounded contexts
    print_info("\nChecking Bounded Contexts (13 required)...")
    found_contexts, missing_contexts = check_bounded_contexts(docs_path)
    results["bounded_contexts"]["found"] = found_contexts
    results["bounded_contexts"]["missing"] = missing_contexts
    results["bounded_contexts"]["count"] = len(found_contexts)
    results["bounded_contexts"]["total"] = len(BOUNDED_CONTEXTS)
    
    for context in found_contexts:
        print_success(f"Bounded context documented: {context}")
    for context in missing_contexts:
        print_error(f"Bounded context missing: {context}")
    
    print(f"\n{Colors.BOLD}Bounded Contexts: {len(found_contexts)}/{len(BOUNDED_CONTEXTS)} documented{Colors.ENDC}")
    
    # Check API endpoints
    print_info("\nChecking API Endpoints (9 categories required)...")
    found_endpoints, missing_endpoints = check_api_endpoints(docs_path)
    results["api_endpoints"]["found"] = found_endpoints
    results["api_endpoints"]["missing"] = missing_endpoints
    results["api_endpoints"]["count"] = len(found_endpoints)
    results["api_endpoints"]["total"] = len(API_ENDPOINT_CATEGORIES)
    
    for endpoint in found_endpoints:
        print_success(f"API endpoint category documented: {endpoint}")
    for endpoint in missing_endpoints:
        print_error(f"API endpoint category missing: {endpoint}")
    
    print(f"\n{Colors.BOLD}API Endpoints: {len(found_endpoints)}/{len(API_ENDPOINT_CATEGORIES)} documented{Colors.ENDC}")
    
    # Check additional documentation
    print_info("\nChecking Additional Required Documentation...")
    additional_checks = check_additional_documentation(docs_path)
    results["additional_docs"] = additional_checks
    
    for doc_name, exists in additional_checks.items():
        if exists:
            print_success(f"{doc_name}")
        else:
            print_error(f"{doc_name}")
    
    # Calculate summary
    total_checks = (
        len(REQUIRED_VIEWPOINTS) +
        len(REQUIRED_PERSPECTIVES) +
        len(BOUNDED_CONTEXTS) +
        len(API_ENDPOINT_CATEGORIES) +
        len(additional_checks)
    )
    
    passed_checks = (
        len(found_viewpoints) +
        len(found_perspectives) +
        len(found_contexts) +
        len(found_endpoints) +
        sum(1 for v in additional_checks.values() if v)
    )
    
    results["summary"]["total_checks"] = total_checks
    results["summary"]["passed_checks"] = passed_checks
    results["summary"]["failed_checks"] = total_checks - passed_checks
    results["summary"]["completion_percentage"] = round((passed_checks / total_checks) * 100, 2)
    
    # Print summary
    print_header("Validation Summary")
    print(f"{Colors.BOLD}Total Checks: {total_checks}{Colors.ENDC}")
    print(f"{Colors.GREEN}Passed: {passed_checks}{Colors.ENDC}")
    print(f"{Colors.RED}Failed: {total_checks - passed_checks}{Colors.ENDC}")
    print(f"{Colors.BOLD}Completion: {results['summary']['completion_percentage']}%{Colors.ENDC}")
    
    # Save report
    report_path = project_root / "docs" / "reports" / "completeness-validation-report.json"
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(generate_report(results))
    print_info(f"\nDetailed report saved to: {report_path}")
    
    # Exit with appropriate code
    if results["summary"]["completion_percentage"] == 100:
        print_success("\n✓ All documentation completeness checks passed!")
        sys.exit(0)
    else:
        print_error(f"\n✗ Documentation is {results['summary']['completion_percentage']}% complete")
        print_warning("Please address the missing documentation items listed above.")
        sys.exit(1)

if __name__ == "__main__":
    main()
