#!/usr/bin/env python3
"""
Documentation Quality Assessment Script
Assesses the quality and completeness of documentation for specific viewpoints.
"""

import os
import sys
import argparse
import re
from pathlib import Path
from typing import Dict, List, Tuple, Set
import json

class DocumentationQualityAssessor:
    def __init__(self, viewpoint: str):
        self.viewpoint = viewpoint
        self.quality_metrics = {
            'completeness': 0,
            'readability': 0,
            'structure': 0,
            'links': 0,
            'examples': 0,
            'overall': 0
        }
        self.issues = []
        self.recommendations = []
        
    def assess_file_completeness(self, file_path: str, content: str) -> Dict:
        """Assess completeness of a documentation file."""
        issues = []
        score = 100
        
        # Check for essential sections
        essential_sections = ['overview', 'introduction', 'usage', 'example']
        found_sections = []
        
        headers = re.findall(r'^#{1,6}\s+(.+)', content, re.MULTILINE)
        header_text = ' '.join(headers).lower()
        
        for section in essential_sections:
            if section in header_text:
                found_sections.append(section)
        
        missing_sections = set(essential_sections) - set(found_sections)
        if missing_sections:
            score -= len(missing_sections) * 15
            issues.append(f"Missing essential sections: {', '.join(missing_sections)}")
        
        # Check content length
        word_count = len(content.split())
        if word_count < 100:
            score -= 20
            issues.append(f"Content too short ({word_count} words)")
        elif word_count < 200:
            score -= 10
            issues.append(f"Content could be more detailed ({word_count} words)")
        
        # Check for TODO/FIXME markers
        todo_markers = re.findall(r'(TODO|FIXME|XXX|HACK)', content, re.IGNORECASE)
        if todo_markers:
            score -= len(todo_markers) * 5
            issues.append(f"Contains {len(todo_markers)} TODO/FIXME markers")
        
        # Check for placeholder content
        placeholders = re.findall(r'(\[.*?\]|\{.*?\}|placeholder|lorem ipsum)', content, re.IGNORECASE)
        if placeholders:
            score -= len(placeholders) * 10
            issues.append(f"Contains {len(placeholders)} placeholders")
        
        return {
            'score': max(0, score),
            'issues': issues,
            'word_count': word_count,
            'sections': len(headers)
        }
    
    def assess_readability(self, content: str) -> Dict:
        """Assess readability of content."""
        issues = []
        score = 100
        
        # Check sentence length
        sentences = re.split(r'[.!?]+', content)
        long_sentences = [s for s in sentences if len(s.split()) > 25]
        if long_sentences:
            score -= min(30, len(long_sentences) * 5)
            issues.append(f"{len(long_sentences)} sentences are too long (>25 words)")
        
        # Check paragraph length
        paragraphs = [p.strip() for p in content.split('\n\n') if p.strip()]
        long_paragraphs = [p for p in paragraphs if len(p.split()) > 150]
        if long_paragraphs:
            score -= min(20, len(long_paragraphs) * 5)
            issues.append(f"{len(long_paragraphs)} paragraphs are too long (>150 words)")
        
        # Check for passive voice (simple heuristic)
        passive_indicators = re.findall(r'\b(is|are|was|were|been|being)\s+\w+ed\b', content, re.IGNORECASE)
        if len(passive_indicators) > len(sentences) * 0.3:
            score -= 15
            issues.append("High use of passive voice detected")
        
        # Check for jargon without explanation
        technical_terms = re.findall(r'\b[A-Z]{2,}\b', content)
        if len(technical_terms) > 10:
            score -= 10
            issues.append(f"Many technical terms ({len(technical_terms)}) - consider explanations")
        
        return {
            'score': max(0, score),
            'issues': issues,
            'sentences': len(sentences),
            'paragraphs': len(paragraphs)
        }
    
    def assess_structure(self, content: str) -> Dict:
        """Assess document structure."""
        issues = []
        score = 100
        
        # Check header hierarchy
        headers = re.findall(r'^(#{1,6})\s+(.+)', content, re.MULTILINE)
        if not headers:
            score -= 30
            issues.append("No headers found")
            return {'score': score, 'issues': issues}
        
        # Check for proper header progression
        header_levels = [len(h[0]) for h in headers]
        for i in range(1, len(header_levels)):
            if header_levels[i] > header_levels[i-1] + 1:
                score -= 10
                issues.append(f"Header level jumps from {header_levels[i-1]} to {header_levels[i]}")
        
        # Check for consistent formatting
        code_blocks = re.findall(r'```[\s\S]*?```', content)
        inline_code = re.findall(r'`[^`]+`', content)
        
        # Check for lists
        bullet_lists = re.findall(r'^\s*[-*+]\s+', content, re.MULTILINE)
        numbered_lists = re.findall(r'^\s*\d+\.\s+', content, re.MULTILINE)
        
        structure_elements = len(code_blocks) + len(bullet_lists) + len(numbered_lists)
        if structure_elements == 0:
            score -= 15
            issues.append("No structured elements (lists, code blocks) found")
        
        return {
            'score': max(0, score),
            'issues': issues,
            'headers': len(headers),
            'code_blocks': len(code_blocks),
            'lists': len(bullet_lists) + len(numbered_lists)
        }
    
    def assess_links(self, content: str) -> Dict:
        """Assess link quality and presence."""
        issues = []
        score = 100
        
        # Find all markdown links
        links = re.findall(r'\[([^\]]+)\]\(([^)]+)\)', content)
        
        if not links:
            score -= 20
            issues.append("No links found - consider adding cross-references")
        
        # Check for broken link patterns
        broken_patterns = [
            r'\[([^\]]+)\]\(\)',  # Empty links
            r'\[([^\]]+)\]\(#\)',  # Empty anchors
            r'\[([^\]]+)\]\(TODO\)',  # TODO links
        ]
        
        for pattern in broken_patterns:
            broken_links = re.findall(pattern, content)
            if broken_links:
                score -= len(broken_links) * 10
                issues.append(f"{len(broken_links)} broken or incomplete links")
        
        # Check for relative vs absolute links
        external_links = [link for _, link in links if link.startswith('http')]
        internal_links = [link for _, link in links if not link.startswith('http')]
        
        return {
            'score': max(0, score),
            'issues': issues,
            'total_links': len(links),
            'external_links': len(external_links),
            'internal_links': len(internal_links)
        }
    
    def assess_examples(self, content: str) -> Dict:
        """Assess presence and quality of examples."""
        issues = []
        score = 100
        
        # Find code examples
        code_blocks = re.findall(r'```(\w+)?\n([\s\S]*?)```', content)
        inline_code = re.findall(r'`([^`]+)`', content)
        
        if not code_blocks and not inline_code:
            score -= 30
            issues.append("No code examples found")
        
        # Check for example sections
        example_sections = re.findall(r'#{1,6}\s+.*example.*', content, re.IGNORECASE)
        if not example_sections and len(code_blocks) == 0:
            score -= 20
            issues.append("No dedicated example sections")
        
        # Check code block languages
        unlabeled_blocks = [1 for lang, _ in code_blocks if not lang]
        if unlabeled_blocks:
            score -= len(unlabeled_blocks) * 5
            issues.append(f"{len(unlabeled_blocks)} code blocks without language labels")
        
        return {
            'score': max(0, score),
            'issues': issues,
            'code_blocks': len(code_blocks),
            'inline_code': len(inline_code),
            'example_sections': len(example_sections)
        }
    
    def assess_directory(self, directory: str) -> Dict:
        """Assess all documentation in a directory."""
        print(f"Assessing documentation quality in: {directory}")
        
        file_assessments = {}
        total_files = 0
        
        for root, dirs, files in os.walk(directory):
            # Skip certain directories
            dirs[:] = [d for d in dirs if d not in ['.git', '.kiro', 'node_modules', 'generated']]
            
            for file in files:
                if file.endswith('.md'):
                    file_path = os.path.join(root, file)
                    relative_path = os.path.relpath(file_path, directory)
                    
                    try:
                        with open(file_path, 'r', encoding='utf-8') as f:
                            content = f.read()
                        
                        assessment = {}
                        try:
                            assessment['completeness'] = self.assess_file_completeness(file_path, content)
                        except Exception as e:
                            raise Exception(f"Error in completeness assessment: {e}")
                        
                        try:
                            assessment['readability'] = self.assess_readability(content)
                        except Exception as e:
                            raise Exception(f"Error in readability assessment: {e}")
                        
                        try:
                            assessment['structure'] = self.assess_structure(content)
                        except Exception as e:
                            raise Exception(f"Error in structure assessment: {e}")
                        
                        try:
                            assessment['links'] = self.assess_links(content)
                        except Exception as e:
                            raise Exception(f"Error in links assessment: {e}")
                        
                        try:
                            assessment['examples'] = self.assess_examples(content)
                        except Exception as e:
                            raise Exception(f"Error in examples assessment: {e}")
                        
                        # Calculate overall score
                        overall_score = sum(a['score'] for a in assessment.values()) / len(assessment)
                        assessment['overall'] = {'score': overall_score}
                        
                        file_assessments[relative_path] = assessment
                        total_files += 1
                        
                    except Exception as e:
                        print(f"Warning: Could not assess {file_path}: {e}")
        
        return {
            'files': file_assessments,
            'total_files': total_files,
            'summary': self.calculate_summary(file_assessments)
        }
    
    def calculate_summary(self, file_assessments: Dict) -> Dict:
        """Calculate summary statistics."""
        if not file_assessments:
            return {'average_score': 0, 'total_issues': 0}
        
        all_scores = []
        all_issues = []
        
        for file_data in file_assessments.values():
            all_scores.append(file_data['overall']['score'])
            for category, data in file_data.items():
                if category != 'overall' and 'issues' in data:
                    all_issues.extend(data['issues'])
        
        return {
            'average_score': sum(all_scores) / len(all_scores) if all_scores else 0,
            'total_issues': len(all_issues),
            'files_assessed': len(file_assessments)
        }
    
    def generate_report(self, assessment: Dict, output_file: str = None):
        """Generate quality assessment report."""
        summary = assessment['summary']
        
        report_lines = [
            f"# Documentation Quality Assessment Report - {self.viewpoint.title()} Viewpoint",
            f"Generated: {os.popen('date').read().strip()}",
            "",
            "## Executive Summary",
            f"- **Files Assessed**: {summary['files_assessed']}",
            f"- **Average Quality Score**: {summary['average_score']:.1f}/100",
            f"- **Total Issues Found**: {summary['total_issues']}",
            "",
            "## Quality Grade",
        ]
        
        # Determine grade
        avg_score = summary['average_score']
        if avg_score >= 90:
            grade = "A (Excellent)"
        elif avg_score >= 80:
            grade = "B (Good)"
        elif avg_score >= 70:
            grade = "C (Satisfactory)"
        elif avg_score >= 60:
            grade = "D (Needs Improvement)"
        else:
            grade = "F (Poor)"
        
        report_lines.append(f"**Overall Grade**: {grade}")
        report_lines.append("")
        
        # File-by-file breakdown
        report_lines.extend([
            "## File-by-File Assessment",
            ""
        ])
        
        for file_path, file_data in assessment['files'].items():
            overall_score = file_data['overall']['score']
            report_lines.extend([
                f"### {file_path}",
                f"**Overall Score**: {overall_score:.1f}/100",
                ""
            ])
            
            for category, data in file_data.items():
                if category != 'overall':
                    score = data['score']
                    issues = data.get('issues', [])
                    
                    report_lines.append(f"- **{category.title()}**: {score}/100")
                    if issues:
                        for issue in issues:
                            report_lines.append(f"  - ⚠️ {issue}")
                    report_lines.append("")
            
            report_lines.append("---")
            report_lines.append("")
        
        # Recommendations
        report_lines.extend([
            "## Recommendations",
            ""
        ])
        
        if avg_score < 70:
            report_lines.extend([
                "### High Priority",
                "1. **Address Content Gaps**: Focus on files with completeness scores < 60",
                "2. **Improve Structure**: Add proper headers and organize content logically",
                "3. **Add Examples**: Include practical code examples and use cases",
                ""
            ])
        
        if avg_score < 85:
            report_lines.extend([
                "### Medium Priority",
                "1. **Enhance Readability**: Break up long sentences and paragraphs",
                "2. **Fix Links**: Resolve broken or incomplete links",
                "3. **Add Cross-References**: Link related sections and documents",
                ""
            ])
        
        report_lines.extend([
            "### General Improvements",
            "1. **Regular Reviews**: Schedule quarterly documentation reviews",
            "2. **Style Guide**: Establish and follow consistent writing style",
            "3. **User Feedback**: Collect feedback from documentation users",
            "4. **Automation**: Use tools to maintain link integrity and formatting",
            ""
        ])
        
        report_content = '\n'.join(report_lines)
        
        if output_file:
            os.makedirs(os.path.dirname(output_file), exist_ok=True)
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write(report_content)
            print(f"Quality assessment report saved to: {output_file}")
        else:
            print(report_content)

def main():
    parser = argparse.ArgumentParser(description='Assess documentation quality')
    parser.add_argument('--viewpoint', required=True, help='Viewpoint to assess (e.g., development)')
    parser.add_argument('--output', help='Output file for report')
    
    args = parser.parse_args()
    
    # Determine directory based on viewpoint
    viewpoint_dir = f"docs/viewpoints/{args.viewpoint}"
    if not os.path.exists(viewpoint_dir):
        print(f"Error: Viewpoint directory '{viewpoint_dir}' does not exist")
        sys.exit(1)
    
    assessor = DocumentationQualityAssessor(args.viewpoint)
    assessment = assessor.assess_directory(viewpoint_dir)
    
    output_file = args.output
    if not output_file:
        timestamp = os.popen('date +%Y%m%d_%H%M%S').read().strip()
        output_file = f"reports-summaries/quality-ux/documentation-quality-{args.viewpoint}-{timestamp}.md"
    
    assessor.generate_report(assessment, output_file)
    
    # Exit with appropriate code based on quality
    avg_score = assessment['summary']['average_score']
    if avg_score < 60:
        print(f"\n❌ Documentation quality is poor (score: {avg_score:.1f})")
        sys.exit(2)
    elif avg_score < 80:
        print(f"\n⚠️  Documentation quality needs improvement (score: {avg_score:.1f})")
        sys.exit(1)
    else:
        print(f"\n✅ Documentation quality is good (score: {avg_score:.1f})")
        sys.exit(0)

if __name__ == '__main__':
    main()