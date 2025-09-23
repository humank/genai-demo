#!/usr/bin/env python3
"""
Development Viewpoint Performance and Quality Testing Script

This script tests the performance and quality of the development viewpoint structure,
including document load times, search response times, diagram rendering, and content quality.
"""

import os
import time
import re
from pathlib import Path
from typing import Dict, List, Tuple, Set
import json
import hashlib

class PerformanceQualityTester:
    def __init__(self, base_path: str = "."):
        self.base_path = Path(base_path)
        self.development_viewpoint_path = self.base_path / "docs" / "viewpoints" / "development"
        self.diagrams_path = self.base_path / "docs" / "diagrams" / "viewpoints" / "development"
        self.test_results = {
            "document_load_times": {},
            "search_performance": {},
            "diagram_rendering": {},
            "content_quality": {},
            "summary": {}
        }
        
    def test_document_load_times(self) -> Dict[str, any]:
        """Test document load times to ensure they're under 2 seconds."""
        print("📄 Testing Document Load Times...")
        
        load_times = []
        slow_documents = []
        total_size = 0
        
        for file_path in self.development_viewpoint_path.rglob("*.md"):
            start_time = time.time()
            
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    file_size = len(content.encode('utf-8'))
                    total_size += file_size
                
                load_time = (time.time() - start_time) * 1000  # Convert to milliseconds
                load_times.append(load_time)
                
                relative_path = str(file_path.relative_to(self.development_viewpoint_path))
                
                if load_time > 2000:  # 2 seconds in milliseconds
                    slow_documents.append({
                        "file": relative_path,
                        "load_time_ms": load_time,
                        "size_bytes": file_size,
                        "size_kb": file_size / 1024
                    })
                    
            except Exception as e:
                slow_documents.append({
                    "file": str(file_path.relative_to(self.development_viewpoint_path)),
                    "load_time_ms": 9999,
                    "error": str(e)
                })
        
        avg_load_time = sum(load_times) / len(load_times) if load_times else 0
        max_load_time = max(load_times) if load_times else 0
        p95_load_time = sorted(load_times)[int(len(load_times) * 0.95)] if load_times else 0
        
        results = {
            "total_documents": len(load_times),
            "avg_load_time_ms": avg_load_time,
            "max_load_time_ms": max_load_time,
            "p95_load_time_ms": p95_load_time,
            "slow_documents": slow_documents,
            "slow_document_count": len(slow_documents),
            "total_size_mb": total_size / (1024 * 1024),
            "avg_size_kb": (total_size / len(load_times) / 1024) if load_times else 0,
            "passed": len(slow_documents) == 0 and p95_load_time < 2000
        }
        
        print(f"   Total Documents: {results['total_documents']}")
        print(f"   Average Load Time: {avg_load_time:.1f}ms")
        print(f"   95th Percentile: {p95_load_time:.1f}ms")
        print(f"   Slow Documents: {len(slow_documents)}")
        print(f"   Total Size: {results['total_size_mb']:.1f}MB")
        
        return results
    
    def test_search_performance(self) -> Dict[str, any]:
        """Test search performance by simulating content searches."""
        print("🔍 Testing Search Performance...")
        
        # Build search index
        search_index = {}
        index_build_start = time.time()
        
        for file_path in self.development_viewpoint_path.rglob("*.md"):
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read().lower()
                    words = re.findall(r'\b\w+\b', content)
                    
                    relative_path = str(file_path.relative_to(self.development_viewpoint_path))
                    
                    for word in words:
                        if len(word) > 3:  # Only index words longer than 3 characters
                            if word not in search_index:
                                search_index[word] = []
                            if relative_path not in search_index[word]:
                                search_index[word].append(relative_path)
            except:
                pass
        
        index_build_time = (time.time() - index_build_start) * 1000
        
        # Test search queries
        test_queries = [
            "spring boot",
            "testing",
            "architecture",
            "microservices",
            "ddd patterns",
            "hexagonal",
            "saga",
            "performance",
            "security",
            "gradle"
        ]
        
        search_times = []
        search_results = []
        
        for query in test_queries:
            start_time = time.time()
            
            # Simple search implementation
            query_words = query.lower().split()
            matching_files = set()
            
            for word in query_words:
                if word in search_index:
                    if not matching_files:
                        matching_files = set(search_index[word])
                    else:
                        matching_files = matching_files.intersection(set(search_index[word]))
            
            search_time = (time.time() - start_time) * 1000
            search_times.append(search_time)
            
            search_results.append({
                "query": query,
                "search_time_ms": search_time,
                "result_count": len(matching_files),
                "results": list(matching_files)[:5]  # Top 5 results
            })
        
        avg_search_time = sum(search_times) / len(search_times) if search_times else 0
        max_search_time = max(search_times) if search_times else 0
        
        results = {
            "index_build_time_ms": index_build_time,
            "index_size": len(search_index),
            "test_queries": len(test_queries),
            "avg_search_time_ms": avg_search_time,
            "max_search_time_ms": max_search_time,
            "search_results": search_results,
            "passed": avg_search_time < 1000 and max_search_time < 1000  # Under 1 second
        }
        
        print(f"   Index Build Time: {index_build_time:.1f}ms")
        print(f"   Index Size: {len(search_index)} words")
        print(f"   Average Search Time: {avg_search_time:.1f}ms")
        print(f"   Max Search Time: {max_search_time:.1f}ms")
        
        return results
    
    def test_diagram_rendering(self) -> Dict[str, any]:
        """Test diagram rendering performance by analyzing diagram complexity."""
        print("📊 Testing Diagram Rendering Performance...")
        
        diagram_files = []
        rendering_estimates = []
        complex_diagrams = []
        
        # Find all diagram files
        for file_path in self.diagrams_path.rglob("*.mmd"):
            diagram_files.append(file_path)
        
        for file_path in self.diagrams_path.rglob("*.puml"):
            diagram_files.append(file_path)
        
        for diagram_path in diagram_files:
            try:
                with open(diagram_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Estimate rendering complexity
                lines = len(content.split('\n'))
                nodes = len(re.findall(r'\b\w+\[', content)) + len(re.findall(r'\b\w+\(', content))
                connections = len(re.findall(r'-->', content)) + len(re.findall(r'->', content))
                
                # Simple complexity score
                complexity_score = lines + nodes * 2 + connections * 1.5
                
                # Estimate rendering time (very rough approximation)
                estimated_render_time = min(complexity_score * 10, 5000)  # Cap at 5 seconds
                
                rendering_estimates.append(estimated_render_time)
                
                relative_path = str(diagram_path.relative_to(self.diagrams_path))
                
                if estimated_render_time > 3000:  # Over 3 seconds
                    complex_diagrams.append({
                        "file": relative_path,
                        "estimated_render_time_ms": estimated_render_time,
                        "complexity_score": complexity_score,
                        "lines": lines,
                        "nodes": nodes,
                        "connections": connections
                    })
                    
            except Exception as e:
                complex_diagrams.append({
                    "file": str(diagram_path.relative_to(self.diagrams_path)),
                    "error": str(e)
                })
        
        avg_render_time = sum(rendering_estimates) / len(rendering_estimates) if rendering_estimates else 0
        max_render_time = max(rendering_estimates) if rendering_estimates else 0
        
        results = {
            "total_diagrams": len(diagram_files),
            "avg_estimated_render_time_ms": avg_render_time,
            "max_estimated_render_time_ms": max_render_time,
            "complex_diagrams": complex_diagrams,
            "complex_diagram_count": len(complex_diagrams),
            "passed": len(complex_diagrams) == 0 and max_render_time < 3000
        }
        
        print(f"   Total Diagrams: {len(diagram_files)}")
        print(f"   Average Estimated Render Time: {avg_render_time:.1f}ms")
        print(f"   Max Estimated Render Time: {max_render_time:.1f}ms")
        print(f"   Complex Diagrams: {len(complex_diagrams)}")
        
        return results
    
    def test_content_quality(self) -> Dict[str, any]:
        """Test content quality by analyzing various quality metrics."""
        print("📝 Testing Content Quality...")
        
        quality_issues = []
        total_files = 0
        total_words = 0
        total_lines = 0
        
        quality_metrics = {
            "spelling_issues": 0,
            "formatting_issues": 0,
            "structure_issues": 0,
            "consistency_issues": 0
        }
        
        for file_path in self.development_viewpoint_path.rglob("*.md"):
            total_files += 1
            file_issues = []
            
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    lines = content.split('\n')
                    total_lines += len(lines)
                    
                    # Count words
                    words = re.findall(r'\b\w+\b', content)
                    total_words += len(words)
                
                relative_path = str(file_path.relative_to(self.development_viewpoint_path))
                
                # Check for basic formatting issues
                if not content.startswith('#'):
                    file_issues.append({
                        "type": "structure",
                        "issue": "File doesn't start with a heading"
                    })
                    quality_metrics["structure_issues"] += 1
                
                # Check for empty files
                if len(content.strip()) < 50:
                    file_issues.append({
                        "type": "structure",
                        "issue": "File is too short (less than 50 characters)"
                    })
                    quality_metrics["structure_issues"] += 1
                
                # Check for broken markdown formatting
                if content.count('```') % 2 != 0:
                    file_issues.append({
                        "type": "formatting",
                        "issue": "Unmatched code block markers"
                    })
                    quality_metrics["formatting_issues"] += 1
                
                # Check for inconsistent heading levels
                headings = []
                for line_num, line in enumerate(lines, 1):
                    if line.startswith('#'):
                        level = len(line) - len(line.lstrip('#'))
                        headings.append((level, line_num))
                
                for i in range(1, len(headings)):
                    prev_level, prev_line = headings[i-1]
                    curr_level, curr_line = headings[i]
                    
                    if curr_level > prev_level + 1:
                        file_issues.append({
                            "type": "structure",
                            "issue": f"Heading level jump from {prev_level} to {curr_level} at line {curr_line}"
                        })
                        quality_metrics["structure_issues"] += 1
                
                # Check for common spelling/grammar issues (basic check)
                common_issues = [
                    (r'\bteh\b', 'the'),
                    (r'\badn\b', 'and'),
                    (r'\bfrom\s+form\b', 'from form'),
                    (r'\bthier\b', 'their'),
                    (r'\brecieve\b', 'receive')
                ]
                
                for pattern, correction in common_issues:
                    if re.search(pattern, content, re.IGNORECASE):
                        file_issues.append({
                            "type": "spelling",
                            "issue": f"Possible spelling error: '{pattern}' should be '{correction}'"
                        })
                        quality_metrics["spelling_issues"] += 1
                
                if file_issues:
                    quality_issues.append({
                        "file": relative_path,
                        "issues": file_issues,
                        "issue_count": len(file_issues)
                    })
                    
            except Exception as e:
                quality_issues.append({
                    "file": str(file_path.relative_to(self.development_viewpoint_path)),
                    "issues": [{"type": "error", "issue": f"Read error: {str(e)}"}],
                    "issue_count": 1
                })
        
        total_issues = sum(item["issue_count"] for item in quality_issues)
        avg_words_per_file = total_words / total_files if total_files > 0 else 0
        
        # Calculate quality score
        quality_score = max(0, 100 - (total_issues / total_files * 5)) if total_files > 0 else 0
        
        results = {
            "total_files": total_files,
            "total_words": total_words,
            "total_lines": total_lines,
            "avg_words_per_file": avg_words_per_file,
            "files_with_issues": len(quality_issues),
            "total_issues": total_issues,
            "quality_metrics": quality_metrics,
            "quality_score": quality_score,
            "quality_issues": quality_issues[:10],  # Show first 10 files with issues
            "passed": quality_score >= 80  # 80% quality threshold
        }
        
        print(f"   Total Files: {total_files}")
        print(f"   Total Words: {total_words:,}")
        print(f"   Average Words per File: {avg_words_per_file:.0f}")
        print(f"   Files with Issues: {len(quality_issues)}")
        print(f"   Quality Score: {quality_score:.1f}%")
        
        return results
    
    def run_all_tests(self) -> Dict[str, any]:
        """Run all performance and quality tests."""
        print("⚡ Starting Development Viewpoint Performance & Quality Testing...")
        print("=" * 70)
        
        start_time = time.time()
        
        # Run individual tests
        self.test_results["document_load_times"] = self.test_document_load_times()
        self.test_results["search_performance"] = self.test_search_performance()
        self.test_results["diagram_rendering"] = self.test_diagram_rendering()
        self.test_results["content_quality"] = self.test_content_quality()
        
        end_time = time.time()
        
        # Calculate overall results
        tests_passed = sum(1 for test in [
            self.test_results["document_load_times"]["passed"],
            self.test_results["search_performance"]["passed"],
            self.test_results["diagram_rendering"]["passed"],
            self.test_results["content_quality"]["passed"]
        ] if test)
        
        # Calculate overall performance score
        performance_score = (
            (100 if self.test_results["document_load_times"]["passed"] else 50) * 0.3 +
            (100 if self.test_results["search_performance"]["passed"] else 50) * 0.2 +
            (100 if self.test_results["diagram_rendering"]["passed"] else 50) * 0.2 +
            self.test_results["content_quality"]["quality_score"] * 0.3
        )
        
        self.test_results["summary"] = {
            "total_tests": 4,
            "tests_passed": tests_passed,
            "performance_score": performance_score,
            "execution_time": end_time - start_time,
            "passed": tests_passed >= 3 and performance_score >= 75,  # At least 3 tests pass and 75% score
            "recommendations": self.generate_recommendations()
        }
        
        return self.test_results
    
    def generate_recommendations(self) -> List[str]:
        """Generate recommendations based on test results."""
        recommendations = []
        
        # Document load time recommendations
        if not self.test_results["document_load_times"]["passed"]:
            recommendations.append("Optimize large documents by breaking them into smaller sections")
            recommendations.append("Consider lazy loading for heavy content")
        
        # Search performance recommendations
        if not self.test_results["search_performance"]["passed"]:
            recommendations.append("Implement search index optimization")
            recommendations.append("Consider using a dedicated search engine for large content")
        
        # Diagram rendering recommendations
        if not self.test_results["diagram_rendering"]["passed"]:
            recommendations.append("Simplify complex diagrams or break them into multiple diagrams")
            recommendations.append("Consider using SVG optimization for better rendering performance")
        
        # Content quality recommendations
        if not self.test_results["content_quality"]["passed"]:
            recommendations.append("Review and fix content quality issues")
            recommendations.append("Implement automated spell checking and grammar validation")
            recommendations.append("Standardize document structure and formatting")
        
        if not recommendations:
            recommendations.append("All performance and quality tests passed - maintain current standards")
        
        return recommendations
    
    def generate_report(self) -> str:
        """Generate a detailed performance and quality test report."""
        report = []
        report.append("# Development Viewpoint Performance & Quality Test Report")
        report.append(f"Generated: {time.strftime('%Y-%m-%d %H:%M:%S')}")
        report.append("")
        
        # Summary
        summary = self.test_results["summary"]
        report.append("## Executive Summary")
        report.append(f"- **Performance Score**: {summary['performance_score']:.1f}%")
        report.append(f"- **Tests Passed**: {summary['tests_passed']}/{summary['total_tests']}")
        report.append(f"- **Execution Time**: {summary['execution_time']:.2f} seconds")
        report.append(f"- **Status**: {'✅ PASSED' if summary['passed'] else '❌ FAILED'}")
        report.append("")
        
        # Individual test results
        report.append("## Test Results")
        
        # Document Load Times
        load_result = self.test_results["document_load_times"]
        report.append("### 📄 Document Load Time Test")
        report.append(f"- **Status**: {'✅ PASSED' if load_result['passed'] else '❌ FAILED'}")
        report.append(f"- **Average Load Time**: {load_result['avg_load_time_ms']:.1f}ms")
        report.append(f"- **95th Percentile**: {load_result['p95_load_time_ms']:.1f}ms")
        report.append(f"- **Slow Documents**: {load_result['slow_document_count']}")
        report.append(f"- **Total Size**: {load_result['total_size_mb']:.1f}MB")
        report.append("")
        
        # Search Performance
        search_result = self.test_results["search_performance"]
        report.append("### 🔍 Search Performance Test")
        report.append(f"- **Status**: {'✅ PASSED' if search_result['passed'] else '❌ FAILED'}")
        report.append(f"- **Average Search Time**: {search_result['avg_search_time_ms']:.1f}ms")
        report.append(f"- **Max Search Time**: {search_result['max_search_time_ms']:.1f}ms")
        report.append(f"- **Index Size**: {search_result['index_size']:,} words")
        report.append("")
        
        # Diagram Rendering
        diagram_result = self.test_results["diagram_rendering"]
        report.append("### 📊 Diagram Rendering Test")
        report.append(f"- **Status**: {'✅ PASSED' if diagram_result['passed'] else '❌ FAILED'}")
        report.append(f"- **Total Diagrams**: {diagram_result['total_diagrams']}")
        report.append(f"- **Average Render Time**: {diagram_result['avg_estimated_render_time_ms']:.1f}ms")
        report.append(f"- **Complex Diagrams**: {diagram_result['complex_diagram_count']}")
        report.append("")
        
        # Content Quality
        quality_result = self.test_results["content_quality"]
        report.append("### 📝 Content Quality Test")
        report.append(f"- **Status**: {'✅ PASSED' if quality_result['passed'] else '❌ FAILED'}")
        report.append(f"- **Quality Score**: {quality_result['quality_score']:.1f}%")
        report.append(f"- **Total Files**: {quality_result['total_files']}")
        report.append(f"- **Total Words**: {quality_result['total_words']:,}")
        report.append(f"- **Files with Issues**: {quality_result['files_with_issues']}")
        report.append("")
        
        # Recommendations
        report.append("## Recommendations")
        for i, rec in enumerate(summary["recommendations"], 1):
            report.append(f"{i}. {rec}")
        report.append("")
        
        return "\n".join(report)

def main():
    """Main function to run performance and quality testing."""
    tester = PerformanceQualityTester()
    
    print("⚡ Starting Development Viewpoint Performance & Quality Testing...")
    print("=" * 70)
    
    results = tester.run_all_tests()
    
    # Generate and save report
    report = tester.generate_report()
    report_path = Path("build/reports/development-viewpoint-performance-test.md")
    report_path.parent.mkdir(parents=True, exist_ok=True)
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    # Also save JSON results
    json_path = Path("build/reports/development-viewpoint-performance-test.json")
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    
    print("\n" + "=" * 70)
    print("📊 PERFORMANCE & QUALITY TEST RESULTS")
    print("=" * 70)
    summary = results["summary"]
    print(f"Performance Score: {summary['performance_score']:.1f}%")
    print(f"Tests Passed: {summary['tests_passed']}/{summary['total_tests']}")
    print(f"Status: {'✅ PASSED' if summary['passed'] else '❌ FAILED'}")
    
    print(f"\n📄 Detailed report saved to: {report_path}")
    print(f"📄 JSON data saved to: {json_path}")
    
    # Return appropriate exit code
    return 0 if summary['passed'] else 1

if __name__ == "__main__":
    import sys
    sys.exit(main())