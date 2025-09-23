#!/usr/bin/env python3
"""
Development Viewpoint User Experience Testing Script

This script tests the user experience of the development viewpoint structure,
including navigation depth, content discoverability, and mobile compatibility.
"""

import os
import re
import time
from pathlib import Path
from typing import Dict, List, Tuple, Set
import json

class UXTester:
    def __init__(self, base_path: str = "."):
        self.base_path = Path(base_path)
        self.development_viewpoint_path = self.base_path / "docs" / "viewpoints" / "development"
        self.test_results = {
            "navigation_depth": {},
            "content_discovery": {},
            "mobile_compatibility": {},
            "user_journeys": {},
            "summary": {}
        }
        
    def test_navigation_depth(self) -> Dict[str, any]:
        """Test that navigation depth doesn't exceed 3 layers."""
        print("🧭 Testing Navigation Depth...")
        
        depth_violations = []
        max_depth = 0
        total_files = 0
        
        for file_path in self.development_viewpoint_path.rglob("*.md"):
            # Calculate depth from development viewpoint root
            relative_path = file_path.relative_to(self.development_viewpoint_path)
            depth = len(relative_path.parts) - 1  # Subtract 1 for the file itself
            
            total_files += 1
            max_depth = max(max_depth, depth)
            
            if depth > 3:
                depth_violations.append({
                    "file": str(relative_path),
                    "depth": depth,
                    "path_parts": list(relative_path.parts)
                })
        
        results = {
            "max_depth": max_depth,
            "total_files": total_files,
            "violations": depth_violations,
            "violation_count": len(depth_violations),
            "compliance_rate": ((total_files - len(depth_violations)) / total_files * 100) if total_files > 0 else 0,
            "passed": len(depth_violations) == 0
        }
        
        print(f"   Max Depth: {max_depth}")
        print(f"   Total Files: {total_files}")
        print(f"   Violations: {len(depth_violations)}")
        print(f"   Compliance Rate: {results['compliance_rate']:.1f}%")
        
        return results
    
    def test_content_discovery(self) -> Dict[str, any]:
        """Test content discoverability through clear navigation and indexing."""
        print("🔍 Testing Content Discovery...")
        
        discovery_issues = []
        readme_files = []
        orphaned_files = []
        
        # Find all README files
        for readme_path in self.development_viewpoint_path.rglob("README.md"):
            readme_files.append(readme_path)
        
        # Check if each directory has a README
        directories = set()
        for file_path in self.development_viewpoint_path.rglob("*.md"):
            directories.add(file_path.parent)
        
        for directory in directories:
            readme_path = directory / "README.md"
            if not readme_path.exists():
                discovery_issues.append({
                    "type": "missing_readme",
                    "directory": str(directory.relative_to(self.development_viewpoint_path)),
                    "issue": "Directory lacks README.md for navigation"
                })
        
        # Check for orphaned files (files not referenced in any README)
        all_md_files = set()
        referenced_files = set()
        
        for file_path in self.development_viewpoint_path.rglob("*.md"):
            all_md_files.add(file_path)
        
        # Extract references from README files
        for readme_path in readme_files:
            try:
                with open(readme_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    
                # Find markdown links
                link_pattern = r'\[([^\]]*)\]\(([^)]+)\)'
                for match in re.finditer(link_pattern, content):
                    link_url = match.group(2)
                    if not link_url.startswith(('http', '#')):
                        # Resolve relative path
                        try:
                            resolved_path = (readme_path.parent / link_url).resolve()
                            if resolved_path.exists() and resolved_path.suffix == '.md':
                                referenced_files.add(resolved_path)
                        except:
                            pass
            except Exception as e:
                discovery_issues.append({
                    "type": "readme_read_error",
                    "file": str(readme_path.relative_to(self.development_viewpoint_path)),
                    "error": str(e)
                })
        
        # Find orphaned files
        for file_path in all_md_files:
            if file_path not in referenced_files and file_path.name != "README.md":
                orphaned_files.append(str(file_path.relative_to(self.development_viewpoint_path)))
        
        results = {
            "readme_count": len(readme_files),
            "directory_count": len(directories),
            "discovery_issues": discovery_issues,
            "orphaned_files": orphaned_files,
            "orphaned_count": len(orphaned_files),
            "discoverability_score": max(0, 100 - len(discovery_issues) * 10 - len(orphaned_files) * 5),
            "passed": len(discovery_issues) == 0 and len(orphaned_files) < 5
        }
        
        print(f"   README Files: {len(readme_files)}")
        print(f"   Directories: {len(directories)}")
        print(f"   Discovery Issues: {len(discovery_issues)}")
        print(f"   Orphaned Files: {len(orphaned_files)}")
        print(f"   Discoverability Score: {results['discoverability_score']:.1f}%")
        
        return results
    
    def test_mobile_compatibility(self) -> Dict[str, any]:
        """Test mobile compatibility by checking content structure and formatting."""
        print("📱 Testing Mobile Compatibility...")
        
        compatibility_issues = []
        total_files = 0
        
        for file_path in self.development_viewpoint_path.rglob("*.md"):
            total_files += 1
            issues = []
            
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    lines = content.split('\n')
                
                # Check for overly long lines (mobile readability)
                for line_num, line in enumerate(lines, 1):
                    if len(line) > 120 and not line.startswith(('```', '    ', '|')):
                        issues.append({
                            "type": "long_line",
                            "line": line_num,
                            "length": len(line),
                            "content": line[:50] + "..." if len(line) > 50 else line
                        })
                
                # Check for proper heading structure
                headings = []
                for line_num, line in enumerate(lines, 1):
                    if line.startswith('#'):
                        level = len(line) - len(line.lstrip('#'))
                        headings.append((level, line_num))
                
                # Check for heading level jumps (bad for mobile navigation)
                for i in range(1, len(headings)):
                    prev_level, prev_line = headings[i-1]
                    curr_level, curr_line = headings[i]
                    
                    if curr_level > prev_level + 1:
                        issues.append({
                            "type": "heading_jump",
                            "line": curr_line,
                            "from_level": prev_level,
                            "to_level": curr_level
                        })
                
                # Check for wide tables (mobile unfriendly)
                for line_num, line in enumerate(lines, 1):
                    if '|' in line and line.count('|') > 6:
                        issues.append({
                            "type": "wide_table",
                            "line": line_num,
                            "columns": line.count('|') - 1
                        })
                
                if issues:
                    compatibility_issues.append({
                        "file": str(file_path.relative_to(self.development_viewpoint_path)),
                        "issues": issues,
                        "issue_count": len(issues)
                    })
                    
            except Exception as e:
                compatibility_issues.append({
                    "file": str(file_path.relative_to(self.development_viewpoint_path)),
                    "issues": [{"type": "read_error", "error": str(e)}],
                    "issue_count": 1
                })
        
        total_issues = sum(item["issue_count"] for item in compatibility_issues)
        
        results = {
            "total_files": total_files,
            "files_with_issues": len(compatibility_issues),
            "total_issues": total_issues,
            "compatibility_score": max(0, 100 - (total_issues / total_files * 10)) if total_files > 0 else 0,
            "issues": compatibility_issues,
            "passed": len(compatibility_issues) < total_files * 0.2  # Allow 20% of files to have minor issues
        }
        
        print(f"   Total Files: {total_files}")
        print(f"   Files with Issues: {len(compatibility_issues)}")
        print(f"   Total Issues: {total_issues}")
        print(f"   Compatibility Score: {results['compatibility_score']:.1f}%")
        
        return results
    
    def test_user_journeys(self) -> Dict[str, any]:
        """Test common user journeys through the development viewpoint."""
        print("🚶 Testing User Journeys...")
        
        journeys = [
            {
                "name": "New Developer Onboarding",
                "path": [
                    "README.md",
                    "getting-started/README.md",
                    "getting-started/environment-setup.md",
                    "getting-started/first-contribution.md"
                ],
                "description": "New developer getting started"
            },
            {
                "name": "Architecture Understanding",
                "path": [
                    "README.md",
                    "architecture/README.md",
                    "architecture/ddd-patterns/README.md",
                    "architecture/hexagonal-architecture/README.md"
                ],
                "description": "Understanding system architecture"
            },
            {
                "name": "Testing Implementation",
                "path": [
                    "README.md",
                    "testing/README.md",
                    "testing/tdd-practices/README.md",
                    "testing/bdd-practices/README.md"
                ],
                "description": "Implementing testing strategies"
            },
            {
                "name": "Technology Stack Reference",
                "path": [
                    "README.md",
                    "tools-and-environment/README.md",
                    "tools-and-environment/technology-stack/README.md",
                    "tools-and-environment/technology-stack/backend-stack.md"
                ],
                "description": "Understanding technology choices"
            }
        ]
        
        journey_results = []
        
        for journey in journeys:
            result = {
                "name": journey["name"],
                "description": journey["description"],
                "path": journey["path"],
                "steps": [],
                "success": True,
                "broken_links": []
            }
            
            for step in journey["path"]:
                step_path = self.development_viewpoint_path / step
                step_result = {
                    "file": step,
                    "exists": step_path.exists(),
                    "readable": False,
                    "has_content": False
                }
                
                if step_path.exists():
                    try:
                        with open(step_path, 'r', encoding='utf-8') as f:
                            content = f.read()
                            step_result["readable"] = True
                            step_result["has_content"] = len(content.strip()) > 0
                    except:
                        step_result["readable"] = False
                else:
                    result["success"] = False
                    result["broken_links"].append(step)
                
                result["steps"].append(step_result)
            
            journey_results.append(result)
        
        successful_journeys = sum(1 for j in journey_results if j["success"])
        
        results = {
            "total_journeys": len(journeys),
            "successful_journeys": successful_journeys,
            "success_rate": (successful_journeys / len(journeys) * 100) if journeys else 0,
            "journeys": journey_results,
            "passed": successful_journeys >= len(journeys) * 0.8  # 80% success rate required
        }
        
        print(f"   Total Journeys: {len(journeys)}")
        print(f"   Successful Journeys: {successful_journeys}")
        print(f"   Success Rate: {results['success_rate']:.1f}%")
        
        return results
    
    def run_all_tests(self) -> Dict[str, any]:
        """Run all UX tests and generate comprehensive results."""
        print("🎯 Starting Development Viewpoint UX Testing...")
        print("=" * 60)
        
        start_time = time.time()
        
        # Run individual tests
        self.test_results["navigation_depth"] = self.test_navigation_depth()
        self.test_results["content_discovery"] = self.test_content_discovery()
        self.test_results["mobile_compatibility"] = self.test_mobile_compatibility()
        self.test_results["user_journeys"] = self.test_user_journeys()
        
        end_time = time.time()
        
        # Calculate overall results
        tests_passed = sum(1 for test in [
            self.test_results["navigation_depth"]["passed"],
            self.test_results["content_discovery"]["passed"],
            self.test_results["mobile_compatibility"]["passed"],
            self.test_results["user_journeys"]["passed"]
        ] if test)
        
        overall_score = (
            self.test_results["navigation_depth"]["compliance_rate"] * 0.25 +
            self.test_results["content_discovery"]["discoverability_score"] * 0.25 +
            self.test_results["mobile_compatibility"]["compatibility_score"] * 0.25 +
            self.test_results["user_journeys"]["success_rate"] * 0.25
        )
        
        self.test_results["summary"] = {
            "total_tests": 4,
            "tests_passed": tests_passed,
            "overall_score": overall_score,
            "execution_time": end_time - start_time,
            "passed": tests_passed >= 3,  # At least 3 out of 4 tests must pass
            "recommendations": self.generate_recommendations()
        }
        
        return self.test_results
    
    def generate_recommendations(self) -> List[str]:
        """Generate recommendations based on test results."""
        recommendations = []
        
        # Navigation depth recommendations
        if not self.test_results["navigation_depth"]["passed"]:
            recommendations.append("Reduce navigation depth by reorganizing deeply nested content")
        
        # Content discovery recommendations
        if not self.test_results["content_discovery"]["passed"]:
            recommendations.append("Add README files to directories lacking navigation")
            if self.test_results["content_discovery"]["orphaned_count"] > 0:
                recommendations.append("Link orphaned files from appropriate README files")
        
        # Mobile compatibility recommendations
        if not self.test_results["mobile_compatibility"]["passed"]:
            recommendations.append("Break up long lines for better mobile readability")
            recommendations.append("Fix heading structure to improve mobile navigation")
            recommendations.append("Consider responsive table designs for mobile")
        
        # User journey recommendations
        if not self.test_results["user_journeys"]["passed"]:
            recommendations.append("Fix broken links in critical user journeys")
            recommendations.append("Ensure all essential files exist and are accessible")
        
        if not recommendations:
            recommendations.append("All UX tests passed - maintain current structure quality")
        
        return recommendations
    
    def generate_report(self) -> str:
        """Generate a detailed UX test report."""
        report = []
        report.append("# Development Viewpoint UX Test Report")
        report.append(f"Generated: {time.strftime('%Y-%m-%d %H:%M:%S')}")
        report.append("")
        
        # Summary
        summary = self.test_results["summary"]
        report.append("## Executive Summary")
        report.append(f"- **Overall Score**: {summary['overall_score']:.1f}%")
        report.append(f"- **Tests Passed**: {summary['tests_passed']}/{summary['total_tests']}")
        report.append(f"- **Execution Time**: {summary['execution_time']:.2f} seconds")
        report.append(f"- **Status**: {'✅ PASSED' if summary['passed'] else '❌ FAILED'}")
        report.append("")
        
        # Individual test results
        report.append("## Test Results")
        
        # Navigation Depth
        nav_result = self.test_results["navigation_depth"]
        report.append("### 🧭 Navigation Depth Test")
        report.append(f"- **Status**: {'✅ PASSED' if nav_result['passed'] else '❌ FAILED'}")
        report.append(f"- **Max Depth**: {nav_result['max_depth']} layers")
        report.append(f"- **Compliance Rate**: {nav_result['compliance_rate']:.1f}%")
        if nav_result["violations"]:
            report.append("- **Violations**:")
            for violation in nav_result["violations"][:5]:  # Show first 5
                report.append(f"  - `{violation['file']}` (depth: {violation['depth']})")
        report.append("")
        
        # Content Discovery
        disc_result = self.test_results["content_discovery"]
        report.append("### 🔍 Content Discovery Test")
        report.append(f"- **Status**: {'✅ PASSED' if disc_result['passed'] else '❌ FAILED'}")
        report.append(f"- **Discoverability Score**: {disc_result['discoverability_score']:.1f}%")
        report.append(f"- **README Files**: {disc_result['readme_count']}")
        report.append(f"- **Orphaned Files**: {disc_result['orphaned_count']}")
        if disc_result["discovery_issues"]:
            report.append("- **Issues**:")
            for issue in disc_result["discovery_issues"][:3]:
                report.append(f"  - {issue['type']}: {issue.get('directory', issue.get('file', 'N/A'))}")
        report.append("")
        
        # Mobile Compatibility
        mobile_result = self.test_results["mobile_compatibility"]
        report.append("### 📱 Mobile Compatibility Test")
        report.append(f"- **Status**: {'✅ PASSED' if mobile_result['passed'] else '❌ FAILED'}")
        report.append(f"- **Compatibility Score**: {mobile_result['compatibility_score']:.1f}%")
        report.append(f"- **Files with Issues**: {mobile_result['files_with_issues']}/{mobile_result['total_files']}")
        report.append(f"- **Total Issues**: {mobile_result['total_issues']}")
        report.append("")
        
        # User Journeys
        journey_result = self.test_results["user_journeys"]
        report.append("### 🚶 User Journey Test")
        report.append(f"- **Status**: {'✅ PASSED' if journey_result['passed'] else '❌ FAILED'}")
        report.append(f"- **Success Rate**: {journey_result['success_rate']:.1f}%")
        report.append(f"- **Successful Journeys**: {journey_result['successful_journeys']}/{journey_result['total_journeys']}")
        
        if any(not j["success"] for j in journey_result["journeys"]):
            report.append("- **Failed Journeys**:")
            for journey in journey_result["journeys"]:
                if not journey["success"]:
                    report.append(f"  - {journey['name']}: {len(journey['broken_links'])} broken links")
        report.append("")
        
        # Recommendations
        report.append("## Recommendations")
        for i, rec in enumerate(summary["recommendations"], 1):
            report.append(f"{i}. {rec}")
        report.append("")
        
        return "\n".join(report)

def main():
    """Main function to run UX testing."""
    tester = UXTester()
    
    print("🎯 Starting Development Viewpoint UX Testing...")
    print("=" * 60)
    
    results = tester.run_all_tests()
    
    # Generate and save report
    report = tester.generate_report()
    report_path = Path("build/reports/development-viewpoint-ux-test.md")
    report_path.parent.mkdir(parents=True, exist_ok=True)
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    # Also save JSON results
    json_path = Path("build/reports/development-viewpoint-ux-test.json")
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    
    print("\n" + "=" * 60)
    print("📊 UX TEST RESULTS")
    print("=" * 60)
    summary = results["summary"]
    print(f"Overall Score: {summary['overall_score']:.1f}%")
    print(f"Tests Passed: {summary['tests_passed']}/{summary['total_tests']}")
    print(f"Status: {'✅ PASSED' if summary['passed'] else '❌ FAILED'}")
    
    print(f"\n📄 Detailed report saved to: {report_path}")
    print(f"📄 JSON data saved to: {json_path}")
    
    # Return appropriate exit code
    return 0 if summary['passed'] else 1

if __name__ == "__main__":
    import sys
    sys.exit(main())