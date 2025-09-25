#!/usr/bin/env python3
"""
Quality Assurance System for Translation

This module provides comprehensive quality assurance features for the automated
documentation translation system including validation, consistency checks, and reporting.
"""

import os
import re
import json
import logging
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Set
from datetime import datetime
from dataclasses import dataclass, field
import hashlib

logger = logging.getLogger(__name__)

@dataclass
class QualityIssue:
    """Represents a quality issue found during validation."""
    severity: str  # 'error', 'warning', 'info'
    category: str  # 'structure', 'links', 'terminology', 'formatting'
    message: str
    file_path: str
    line_number: Optional[int] = None
    suggestion: Optional[str] = None

@dataclass
class QualityReport:
    """Quality assessment report."""
    file_path: str
    timestamp: str
    overall_score: float
    issues: List[QualityIssue] = field(default_factory=list)
    metrics: Dict[str, float] = field(default_factory=dict)
    
    def add_issue(self, issue: QualityIssue):
        """Add a quality issue to the report."""
        self.issues.append(issue)
    
    def get_issues_by_severity(self, severity: str) -> List[QualityIssue]:
        """Get issues by severity level."""
        return [issue for issue in self.issues if issue.severity == severity]
    
    def calculate_score(self) -> float:
        """Calculate overall quality score."""
        if not self.issues:
            return 100.0
        
        error_count = len(self.get_issues_by_severity('error'))
        warning_count = len(self.get_issues_by_severity('warning'))
        info_count = len(self.get_issues_by_severity('info'))
        
        # Scoring: errors -10, warnings -5, info -1
        penalty = (error_count * 10) + (warning_count * 5) + (info_count * 1)
        score = max(0.0, 100.0 - penalty)
        
        self.overall_score = score
        return score
class Ma
rkdownValidator:
    """
    Validates markdown structure and formatting.
    
    This class provides comprehensive validation of markdown files to ensure
    proper structure, formatting, and compliance with documentation standards.
    """
    
    def __init__(self):
        """Initialize the markdown validator."""
        self.heading_pattern = re.compile(r'^(#{1,6})\s+(.+)$', re.MULTILINE)
        self.code_block_pattern = re.compile(r'```[\s\S]*?```', re.MULTILINE)
        self.inline_code_pattern = re.compile(r'`[^`\n]+`')
        self.link_pattern = re.compile(r'\[([^\]]+)\]\(([^)]+)\)')
        self.image_pattern = re.compile(r'!\[([^\]]*)\]\(([^)]+)\)')
    
    def validate_structure(self, content: str, file_path: str) -> List[QualityIssue]:
        """
        Validate markdown structure.
        
        Args:
            content: Markdown content to validate
            file_path: Path to the file being validated
            
        Returns:
            List of quality issues found
        """
        issues = []
        lines = content.split('\n')
        
        # Check for proper heading hierarchy
        issues.extend(self._check_heading_hierarchy(lines, file_path))
        
        # Check for empty sections
        issues.extend(self._check_empty_sections(lines, file_path))
        
        # Check code block formatting
        issues.extend(self._check_code_blocks(content, file_path))
        
        # Check list formatting
        issues.extend(self._check_list_formatting(lines, file_path))
        
        return issues
    
    def _check_heading_hierarchy(self, lines: List[str], file_path: str) -> List[QualityIssue]:
        """Check heading hierarchy for proper structure."""
        issues = []
        heading_levels = []
        
        for i, line in enumerate(lines, 1):
            heading_match = self.heading_pattern.match(line)
            if heading_match:
                level = len(heading_match.group(1))
                heading_levels.append((level, i))
                
                # Check for skipped levels
                if len(heading_levels) > 1:
                    prev_level = heading_levels[-2][0]
                    if level > prev_level + 1:
                        issues.append(QualityIssue(
                            severity='warning',
                            category='structure',
                            message=f'Heading level skipped (h{prev_level} to h{level})',
                            file_path=file_path,
                            line_number=i,
                            suggestion=f'Consider using h{prev_level + 1} instead of h{level}'
                        ))
        
        return issues
    
    def _check_empty_sections(self, lines: List[str], file_path: str) -> List[QualityIssue]:
        """Check for empty sections between headings."""
        issues = []
        heading_indices = []
        
        for i, line in enumerate(lines):
            if self.heading_pattern.match(line):
                heading_indices.append(i)
        
        # Check content between headings
        for i in range(len(heading_indices) - 1):
            start = heading_indices[i]
            end = heading_indices[i + 1]
            
            # Check if section has meaningful content
            section_content = '\n'.join(lines[start + 1:end]).strip()
            if not section_content:
                issues.append(QualityIssue(
                    severity='warning',
                    category='structure',
                    message='Empty section found',
                    file_path=file_path,
                    line_number=start + 1,
                    suggestion='Add content to this section or remove the heading'
                ))
        
        return issues    def 
_check_code_blocks(self, content: str, file_path: str) -> List[QualityIssue]:
        """Check code block formatting and syntax."""
        issues = []
        
        # Find all code blocks
        code_blocks = self.code_block_pattern.findall(content)
        
        for block in code_blocks:
            lines = block.split('\n')
            if len(lines) < 2:
                continue
            
            # Check for language specification
            first_line = lines[0].strip()
            if first_line == '```':
                # Find line number
                line_num = content[:content.find(block)].count('\n') + 1
                issues.append(QualityIssue(
                    severity='info',
                    category='formatting',
                    message='Code block without language specification',
                    file_path=file_path,
                    line_number=line_num,
                    suggestion='Add language identifier (e.g., ```python, ```java)'
                ))
        
        return issues
    
    def _check_list_formatting(self, lines: List[str], file_path: str) -> List[QualityIssue]:
        """Check list formatting consistency."""
        issues = []
        
        for i, line in enumerate(lines, 1):
            stripped = line.strip()
            
            # Check for inconsistent list markers
            if stripped.startswith('*') or stripped.startswith('-') or stripped.startswith('+'):
                # Check indentation
                indent = len(line) - len(line.lstrip())
                if indent % 2 != 0:
                    issues.append(QualityIssue(
                        severity='warning',
                        category='formatting',
                        message='Inconsistent list indentation',
                        file_path=file_path,
                        line_number=i,
                        suggestion='Use consistent 2-space indentation for nested lists'
                    ))
        
        return issues


class LinkValidator:
    """
    Validates links in markdown files.
    
    This class checks for broken links, validates link formats, and ensures
    link integrity across the documentation.
    """
    
    def __init__(self):
        """Initialize the link validator."""
        self.link_pattern = re.compile(r'\[([^\]]+)\]\(([^)]+)\)')
        self.image_pattern = re.compile(r'!\[([^\]]*)\]\(([^)]+)\)')
        self.reference_pattern = re.compile(r'\[([^\]]+)\]:\s*(.+)')
    
    def validate_links(self, content: str, file_path: str, base_dir: str = '.') -> List[QualityIssue]:
        """
        Validate all links in the content.
        
        Args:
            content: Markdown content to validate
            file_path: Path to the file being validated
            base_dir: Base directory for relative path resolution
            
        Returns:
            List of quality issues found
        """
        issues = []
        
        # Find all links
        links = self.link_pattern.findall(content)
        images = self.image_pattern.findall(content)
        
        # Validate regular links
        for link_text, link_url in links:
            issues.extend(self._validate_single_link(link_text, link_url, file_path, base_dir))
        
        # Validate image links
        for alt_text, image_url in images:
            issues.extend(self._validate_single_link(alt_text, image_url, file_path, base_dir, is_image=True))
        
        return issues
    
    def _validate_single_link(self, text: str, url: str, file_path: str, base_dir: str, is_image: bool = False) -> List[QualityIssue]:
        """Validate a single link."""
        issues = []
        link_type = 'image' if is_image else 'link'
        
        # Skip external URLs (http/https)
        if url.startswith(('http://', 'https://', 'mailto:', 'tel:')):
            return issues
        
        # Skip anchors
        if url.startswith('#'):
            return issues
        
        # Validate local file links
        if not url.startswith(('http', 'mailto', 'tel')):
            # Resolve relative path
            if url.startswith('/'):
                # Absolute path from project root
                full_path = Path(base_dir) / url[1:]
            else:
                # Relative path from current file
                current_dir = Path(file_path).parent
                full_path = current_dir / url
            
            # Remove fragment identifier
            if '#' in url:
                full_path = Path(str(full_path).split('#')[0])
            
            # Check if file exists
            if not full_path.exists():
                issues.append(QualityIssue(
                    severity='error',
                    category='links',
                    message=f'Broken {link_type}: {url}',
                    file_path=file_path,
                    suggestion=f'Check if the target file exists: {full_path}'
                ))
        
        return issuesc
lass TerminologyValidator:
    """
    Validates terminology consistency in translations.
    
    This class ensures that technical terms are consistently translated
    and that important terms are preserved correctly.
    """
    
    def __init__(self, preserve_terms: List[str] = None, custom_translations: Dict[str, str] = None):
        """
        Initialize the terminology validator.
        
        Args:
            preserve_terms: List of terms that should not be translated
            custom_translations: Dictionary of custom term translations
        """
        self.preserve_terms = set(preserve_terms or [])
        self.custom_translations = custom_translations or {}
        
        # Common terminology patterns
        self.technical_patterns = {
            'api_endpoints': re.compile(r'/api/[a-zA-Z0-9/_-]+'),
            'file_paths': re.compile(r'[a-zA-Z0-9._/-]+\.(md|json|yaml|yml|js|ts|py|java)'),
            'code_identifiers': re.compile(r'[a-zA-Z_][a-zA-Z0-9_]*\(\)'),
            'environment_vars': re.compile(r'[A-Z_][A-Z0-9_]*='),
        }
    
    def validate_terminology(self, original_content: str, translated_content: str, file_path: str) -> List[QualityIssue]:
        """
        Validate terminology consistency between original and translated content.
        
        Args:
            original_content: Original English content
            translated_content: Translated content
            file_path: Path to the file being validated
            
        Returns:
            List of quality issues found
        """
        issues = []
        
        # Check preserved terms
        issues.extend(self._check_preserved_terms(original_content, translated_content, file_path))
        
        # Check technical patterns
        issues.extend(self._check_technical_patterns(original_content, translated_content, file_path))
        
        # Check code blocks preservation
        issues.extend(self._check_code_blocks_preservation(original_content, translated_content, file_path))
        
        return issues
    
    def _check_preserved_terms(self, original: str, translated: str, file_path: str) -> List[QualityIssue]:
        """Check that preserved terms are not translated."""
        issues = []
        
        for term in self.preserve_terms:
            original_count = original.count(term)
            translated_count = translated.count(term)
            
            if original_count > 0 and translated_count != original_count:
                issues.append(QualityIssue(
                    severity='error',
                    category='terminology',
                    message=f'Preserved term "{term}" count mismatch: {original_count} -> {translated_count}',
                    file_path=file_path,
                    suggestion=f'Ensure "{term}" appears exactly {original_count} times in translation'
                ))
        
        return issues
    
    def _check_technical_patterns(self, original: str, translated: str, file_path: str) -> List[QualityIssue]:
        """Check that technical patterns are preserved."""
        issues = []
        
        for pattern_name, pattern in self.technical_patterns.items():
            original_matches = set(pattern.findall(original))
            translated_matches = set(pattern.findall(translated))
            
            missing_matches = original_matches - translated_matches
            if missing_matches:
                issues.append(QualityIssue(
                    severity='warning',
                    category='terminology',
                    message=f'Missing {pattern_name}: {", ".join(missing_matches)}',
                    file_path=file_path,
                    suggestion=f'Ensure all {pattern_name} are preserved in translation'
                ))
        
        return issues
    
    def _check_code_blocks_preservation(self, original: str, translated: str, file_path: str) -> List[QualityIssue]:
        """Check that code blocks are preserved exactly."""
        issues = []
        
        # Extract code blocks
        code_block_pattern = re.compile(r'```[\s\S]*?```', re.MULTILINE)
        original_blocks = code_block_pattern.findall(original)
        translated_blocks = code_block_pattern.findall(translated)
        
        if len(original_blocks) != len(translated_blocks):
            issues.append(QualityIssue(
                severity='error',
                category='terminology',
                message=f'Code block count mismatch: {len(original_blocks)} -> {len(translated_blocks)}',
                file_path=file_path,
                suggestion='Ensure all code blocks are preserved in translation'
            ))
        else:
            # Check individual blocks
            for i, (orig_block, trans_block) in enumerate(zip(original_blocks, translated_blocks)):
                if orig_block != trans_block:
                    issues.append(QualityIssue(
                        severity='error',
                        category='terminology',
                        message=f'Code block {i+1} modified in translation',
                        file_path=file_path,
                        suggestion='Code blocks should be preserved exactly'
                    ))
        
        return issuesclass
 QualityAssuranceManager:
    """
    Main quality assurance manager.
    
    This class orchestrates all quality assurance activities including validation,
    reporting, and automated fixes for common issues.
    """
    
    def __init__(self, config: Dict = None):
        """
        Initialize the quality assurance manager.
        
        Args:
            config: Configuration dictionary
        """
        self.config = config or {}
        self.markdown_validator = MarkdownValidator()
        self.link_validator = LinkValidator()
        self.terminology_validator = TerminologyValidator(
            preserve_terms=self.config.get('preserve_terms', []),
            custom_translations=self.config.get('custom_translations', {})
        )
        
        self.reports = []
        self.stats = {
            'files_validated': 0,
            'total_issues': 0,
            'errors': 0,
            'warnings': 0,
            'info': 0
        }
    
    def validate_file(self, file_path: str, original_content: str = None, translated_content: str = None) -> QualityReport:
        """
        Validate a single file comprehensively.
        
        Args:
            file_path: Path to the file to validate
            original_content: Original English content (for translation validation)
            translated_content: Translated content (for translation validation)
            
        Returns:
            Quality report for the file
        """
        report = QualityReport(
            file_path=file_path,
            timestamp=datetime.now().isoformat(),
            overall_score=0.0
        )
        
        try:
            # Read file content if not provided
            if translated_content is None:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
            else:
                content = translated_content
            
            # Validate markdown structure
            if self.config.get('validate_markdown_structure', True):
                structure_issues = self.markdown_validator.validate_structure(content, file_path)
                for issue in structure_issues:
                    report.add_issue(issue)
            
            # Validate links
            if self.config.get('check_link_integrity', True):
                link_issues = self.link_validator.validate_links(content, file_path)
                for issue in link_issues:
                    report.add_issue(issue)
            
            # Validate terminology (if we have original content)
            if original_content and self.config.get('terminology_consistency_check', True):
                terminology_issues = self.terminology_validator.validate_terminology(
                    original_content, content, file_path
                )
                for issue in terminology_issues:
                    report.add_issue(issue)
            
            # Calculate quality score
            report.calculate_score()
            
            # Update statistics
            self.stats['files_validated'] += 1
            self.stats['total_issues'] += len(report.issues)
            self.stats['errors'] += len(report.get_issues_by_severity('error'))
            self.stats['warnings'] += len(report.get_issues_by_severity('warning'))
            self.stats['info'] += len(report.get_issues_by_severity('info'))
            
            # Store report
            self.reports.append(report)
            
            logger.info(f"Validated {file_path}: {report.overall_score:.1f}% quality score")
            
        except Exception as e:
            logger.error(f"Failed to validate {file_path}: {e}")
            report.add_issue(QualityIssue(
                severity='error',
                category='validation',
                message=f'Validation failed: {e}',
                file_path=file_path
            ))
        
        return report
    
    def validate_directory(self, directory: str, exclude_patterns: List[str] = None) -> List[QualityReport]:
        """
        Validate all markdown files in a directory.
        
        Args:
            directory: Directory to validate
            exclude_patterns: Patterns to exclude
            
        Returns:
            List of quality reports
        """
        if exclude_patterns is None:
            exclude_patterns = ['**/*.zh-TW.md', 'node_modules/**', '.git/**']
        
        directory_path = Path(directory)
        reports = []
        
        logger.info(f"Starting quality validation of directory: {directory}")
        
        # Find all markdown files
        for md_file in directory_path.rglob('*.md'):
            # Check if file should be excluded
            relative_path = md_file.relative_to(directory_path)
            should_exclude = False
            
            for pattern in exclude_patterns:
                if md_file.match(pattern.replace('**/', '*')) or str(relative_path).startswith(pattern.replace('**/', '')):
                    should_exclude = True
                    break
            
            if should_exclude:
                continue
            
            # Validate the file
            report = self.validate_file(str(md_file))
            reports.append(report)
        
        logger.info(f"Validation complete. Processed {len(reports)} files.")
        return reports
    
    def generate_summary_report(self) -> Dict:
        """Generate a summary report of all validations."""
        if not self.reports:
            return {'message': 'No validation reports available'}
        
        # Calculate overall statistics
        total_score = sum(report.overall_score for report in self.reports)
        average_score = total_score / len(self.reports)
        
        # Find files with issues
        files_with_errors = [r for r in self.reports if r.get_issues_by_severity('error')]
        files_with_warnings = [r for r in self.reports if r.get_issues_by_severity('warning')]
        
        # Category breakdown
        category_stats = {}
        for report in self.reports:
            for issue in report.issues:
                category = issue.category
                if category not in category_stats:
                    category_stats[category] = {'errors': 0, 'warnings': 0, 'info': 0}
                category_stats[category][issue.severity] += 1
        
        summary = {
            'timestamp': datetime.now().isoformat(),
            'overall_statistics': self.stats.copy(),
            'average_quality_score': round(average_score, 2),
            'files_with_errors': len(files_with_errors),
            'files_with_warnings': len(files_with_warnings),
            'category_breakdown': category_stats,
            'top_issues': self._get_top_issues(),
            'recommendations': self._generate_recommendations()
        }
        
        return summary
    
    def _get_top_issues(self, limit: int = 10) -> List[Dict]:
        """Get the most common issues."""
        issue_counts = {}
        
        for report in self.reports:
            for issue in report.issues:
                key = f"{issue.category}: {issue.message}"
                if key not in issue_counts:
                    issue_counts[key] = {
                        'message': issue.message,
                        'category': issue.category,
                        'severity': issue.severity,
                        'count': 0,
                        'files': set()
                    }
                issue_counts[key]['count'] += 1
                issue_counts[key]['files'].add(issue.file_path)
        
        # Convert sets to lists for JSON serialization
        for issue_data in issue_counts.values():
            issue_data['files'] = list(issue_data['files'])
        
        # Sort by count and return top issues
        sorted_issues = sorted(issue_counts.values(), key=lambda x: x['count'], reverse=True)
        return sorted_issues[:limit]
    
    def _generate_recommendations(self) -> List[str]:
        """Generate recommendations based on validation results."""
        recommendations = []
        
        if self.stats['errors'] > 0:
            recommendations.append("Fix all error-level issues before proceeding with translations")
        
        if self.stats['warnings'] > self.stats['files_validated'] * 2:
            recommendations.append("Consider reviewing documentation standards to reduce warnings")
        
        # Check for common issues
        structure_issues = sum(1 for report in self.reports 
                             for issue in report.issues 
                             if issue.category == 'structure')
        if structure_issues > 0:
            recommendations.append("Review markdown structure guidelines and ensure consistent heading hierarchy")
        
        link_issues = sum(1 for report in self.reports 
                         for issue in report.issues 
                         if issue.category == 'links')
        if link_issues > 0:
            recommendations.append("Fix broken links and validate all internal references")
        
        terminology_issues = sum(1 for report in self.reports 
                                for issue in report.issues 
                                if issue.category == 'terminology')
        if terminology_issues > 0:
            recommendations.append("Review terminology preservation settings and ensure technical terms are not translated")
        
        return recommendations
    
    def save_reports(self, output_file: str = 'quality-reports.json'):
        """Save all reports to a file."""
        try:
            # Convert reports to dictionaries
            reports_data = []
            for report in self.reports:
                report_dict = {
                    'file_path': report.file_path,
                    'timestamp': report.timestamp,
                    'overall_score': report.overall_score,
                    'issues': [
                        {
                            'severity': issue.severity,
                            'category': issue.category,
                            'message': issue.message,
                            'file_path': issue.file_path,
                            'line_number': issue.line_number,
                            'suggestion': issue.suggestion
                        }
                        for issue in report.issues
                    ],
                    'metrics': report.metrics
                }
                reports_data.append(report_dict)
            
            # Save to file
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump({
                    'summary': self.generate_summary_report(),
                    'reports': reports_data
                }, f, indent=2, ensure_ascii=False)
            
            logger.info(f"Quality reports saved to {output_file}")
            
        except Exception as e:
            logger.error(f"Failed to save reports: {e}")
    
    def print_summary(self):
        """Print a summary of validation results."""
        summary = self.generate_summary_report()
        
        print("\n" + "="*60)
        print("QUALITY ASSURANCE SUMMARY")
        print("="*60)
        print(f"Files Validated: {self.stats['files_validated']}")
        print(f"Average Quality Score: {summary['average_quality_score']}%")
        print(f"Total Issues: {self.stats['total_issues']}")
        print(f"  - Errors: {self.stats['errors']}")
        print(f"  - Warnings: {self.stats['warnings']}")
        print(f"  - Info: {self.stats['info']}")
        
        if summary['top_issues']:
            print(f"\nTop Issues:")
            for i, issue in enumerate(summary['top_issues'][:5], 1):
                print(f"  {i}. {issue['message']} ({issue['count']} occurrences)")
        
        if summary['recommendations']:
            print(f"\nRecommendations:")
            for i, rec in enumerate(summary['recommendations'], 1):
                print(f"  {i}. {rec}")
        
        print("="*60)def main
():
    """Main function for command-line usage."""
    import argparse
    
    parser = argparse.ArgumentParser(
        description='Quality Assurance for Translation System',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Validate single file
  python quality_assurance.py --file README.md

  # Validate directory
  python quality_assurance.py --directory docs

  # Generate quality report
  python quality_assurance.py --directory . --report quality-report.json

  # Validate with custom config
  python quality_assurance.py --directory docs --config qa-config.json
        """
    )
    
    # Main operation modes
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--file', metavar='FILE', help='Validate specific file')
    group.add_argument('--directory', metavar='DIR', help='Validate directory')
    
    # Options
    parser.add_argument('--config', metavar='CONFIG', help='Configuration file')
    parser.add_argument('--report', metavar='REPORT', help='Output report file')
    parser.add_argument('--verbose', '-v', action='store_true', help='Verbose output')
    parser.add_argument('--quiet', '-q', action='store_true', help='Quiet output')
    
    args = parser.parse_args()
    
    # Configure logging
    if args.quiet:
        logging.getLogger().setLevel(logging.ERROR)
    elif args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    try:
        # Load configuration
        config = {}
        if args.config and os.path.exists(args.config):
            with open(args.config, 'r', encoding='utf-8') as f:
                config = json.load(f)
        
        # Create QA manager
        qa_manager = QualityAssuranceManager(config)
        
        # Execute validation
        if args.file:
            if not os.path.exists(args.file):
                print(f"Error: File not found: {args.file}")
                return 1
            
            report = qa_manager.validate_file(args.file)
            
            print(f"\nValidation Results for {args.file}:")
            print(f"Quality Score: {report.overall_score:.1f}%")
            print(f"Issues Found: {len(report.issues)}")
            
            if report.issues:
                print("\nIssues:")
                for issue in report.issues:
                    line_info = f" (line {issue.line_number})" if issue.line_number else ""
                    print(f"  [{issue.severity.upper()}] {issue.message}{line_info}")
                    if issue.suggestion:
                        print(f"    Suggestion: {issue.suggestion}")
        
        elif args.directory:
            if not os.path.exists(args.directory):
                print(f"Error: Directory not found: {args.directory}")
                return 1
            
            reports = qa_manager.validate_directory(args.directory)
            qa_manager.print_summary()
        
        # Save report if requested
        if args.report:
            qa_manager.save_reports(args.report)
            print(f"Detailed report saved to: {args.report}")
        
        # Return appropriate exit code
        return 0 if qa_manager.stats['errors'] == 0 else 1
        
    except KeyboardInterrupt:
        print("\nValidation interrupted by user")
        return 1
    except Exception as e:
        print(f"Validation failed: {e}")
        if args.verbose:
            import traceback
            traceback.print_exc()
        return 1


if __name__ == '__main__':
    exit(main())