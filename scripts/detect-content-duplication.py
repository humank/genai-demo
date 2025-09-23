#!/usr/bin/env python3
"""
Content Duplication Detection Script
Detects duplicate content across development documentation files.
"""

import os
import sys
import argparse
import hashlib
from pathlib import Path
from difflib import SequenceMatcher
import re
from typing import List, Dict, Tuple, Set

class ContentDuplicationDetector:
    def __init__(self, threshold: float = 0.8):
        self.threshold = threshold
        self.duplicates = []
        self.processed_files = set()
        
    def normalize_content(self, content: str) -> str:
        """Normalize content for comparison by removing formatting and whitespace."""
        # Remove markdown formatting
        content = re.sub(r'[#*`_\-=]', '', content)
        # Remove extra whitespace
        content = re.sub(r'\s+', ' ', content)
        # Convert to lowercase
        content = content.lower().strip()
        return content
    
    def extract_sections(self, content: str) -> List[str]:
        """Extract meaningful sections from markdown content."""
        sections = []
        
        # Split by headers
        header_pattern = r'^#{1,6}\s+(.+)$'
        current_section = []
        
        for line in content.split('\n'):
            if re.match(header_pattern, line):
                if current_section:
                    sections.append('\n'.join(current_section))
                current_section = [line]
            else:
                current_section.append(line)
        
        if current_section:
            sections.append('\n'.join(current_section))
        
        return sections
    
    def calculate_similarity(self, text1: str, text2: str) -> float:
        """Calculate similarity between two text strings."""
        normalized1 = self.normalize_content(text1)
        normalized2 = self.normalize_content(text2)
        
        if len(normalized1) < 50 or len(normalized2) < 50:
            return 0.0  # Skip very short content
        
        return SequenceMatcher(None, normalized1, normalized2).ratio()
    
    def scan_directory(self, directory: str) -> Dict[str, str]:
        """Scan directory for markdown files and return content."""
        files_content = {}
        
        for root, dirs, files in os.walk(directory):
            # Skip certain directories
            dirs[:] = [d for d in dirs if d not in ['.git', '.kiro', 'node_modules', 'generated']]
            
            for file in files:
                if file.endswith('.md'):
                    file_path = os.path.join(root, file)
                    try:
                        with open(file_path, 'r', encoding='utf-8') as f:
                            content = f.read()
                            files_content[file_path] = content
                    except Exception as e:
                        print(f"Warning: Could not read {file_path}: {e}")
        
        return files_content
    
    def detect_duplicates(self, source_dir: str) -> List[Dict]:
        """Detect duplicate content in the source directory."""
        print(f"Scanning for duplicate content in: {source_dir}")
        
        files_content = self.scan_directory(source_dir)
        duplicates = []
        
        file_paths = list(files_content.keys())
        
        for i, file1 in enumerate(file_paths):
            for j, file2 in enumerate(file_paths[i+1:], i+1):
                content1 = files_content[file1]
                content2 = files_content[file2]
                
                # Check overall similarity
                overall_similarity = self.calculate_similarity(content1, content2)
                
                if overall_similarity >= self.threshold:
                    duplicates.append({
                        'file1': file1,
                        'file2': file2,
                        'similarity': overall_similarity,
                        'type': 'full_document'
                    })
                
                # Check section-level similarity
                sections1 = self.extract_sections(content1)
                sections2 = self.extract_sections(content2)
                
                for idx1, section1 in enumerate(sections1):
                    for idx2, section2 in enumerate(sections2):
                        section_similarity = self.calculate_similarity(section1, section2)
                        
                        if section_similarity >= self.threshold and len(self.normalize_content(section1)) > 100:
                            duplicates.append({
                                'file1': file1,
                                'file2': file2,
                                'similarity': section_similarity,
                                'type': 'section',
                                'section1_index': idx1,
                                'section2_index': idx2,
                                'section1_preview': section1[:100] + '...',
                                'section2_preview': section2[:100] + '...'
                            })
        
        return duplicates
    
    def generate_report(self, duplicates: List[Dict], output_file: str = None):
        """Generate a duplication report."""
        report_lines = [
            "# Content Duplication Detection Report",
            f"Generated: {os.popen('date').read().strip()}",
            f"Threshold: {self.threshold * 100}%",
            "",
            f"## Summary",
            f"- Total duplicates found: {len(duplicates)}",
            f"- Full document duplicates: {len([d for d in duplicates if d['type'] == 'full_document'])}",
            f"- Section duplicates: {len([d for d in duplicates if d['type'] == 'section'])}",
            ""
        ]
        
        if duplicates:
            report_lines.extend([
                "## Detailed Findings",
                ""
            ])
            
            for i, duplicate in enumerate(duplicates, 1):
                report_lines.extend([
                    f"### Duplicate {i}: {duplicate['type'].replace('_', ' ').title()}",
                    f"**Similarity**: {duplicate['similarity']:.2%}",
                    f"**File 1**: `{duplicate['file1']}`",
                    f"**File 2**: `{duplicate['file2']}`",
                    ""
                ])
                
                if duplicate['type'] == 'section':
                    report_lines.extend([
                        f"**Section 1 Preview**: {duplicate['section1_preview']}",
                        f"**Section 2 Preview**: {duplicate['section2_preview']}",
                        ""
                    ])
                
                report_lines.append("---")
                report_lines.append("")
        else:
            report_lines.extend([
                "## No Duplicates Found",
                "✅ No content duplication detected above the threshold.",
                ""
            ])
        
        report_lines.extend([
            "## Recommendations",
            "",
            "1. **Review High Similarity Content**: Examine content with >90% similarity",
            "2. **Consolidate Duplicates**: Merge similar sections where appropriate",
            "3. **Create Cross-References**: Use links instead of duplicating content",
            "4. **Establish Single Source of Truth**: Designate authoritative sources for common topics",
            ""
        ])
        
        report_content = '\n'.join(report_lines)
        
        if output_file:
            os.makedirs(os.path.dirname(output_file), exist_ok=True)
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write(report_content)
            print(f"Report saved to: {output_file}")
        else:
            print(report_content)

def main():
    parser = argparse.ArgumentParser(description='Detect content duplication in documentation')
    parser.add_argument('--source', required=True, help='Source directory to scan')
    parser.add_argument('--threshold', type=float, default=0.8, help='Similarity threshold (0.0-1.0)')
    parser.add_argument('--output', help='Output file for report')
    
    args = parser.parse_args()
    
    if not os.path.exists(args.source):
        print(f"Error: Source directory '{args.source}' does not exist")
        sys.exit(1)
    
    detector = ContentDuplicationDetector(threshold=args.threshold)
    duplicates = detector.detect_duplicates(args.source)
    
    output_file = args.output
    if not output_file:
        timestamp = os.popen('date +%Y%m%d_%H%M%S').read().strip()
        output_file = f"reports-summaries/quality-ux/content-duplication-report-{timestamp}.md"
    
    detector.generate_report(duplicates, output_file)
    
    # Exit with error code if duplicates found
    if duplicates:
        print(f"\n⚠️  Found {len(duplicates)} potential duplicates")
        sys.exit(1)
    else:
        print("\n✅ No content duplication detected")
        sys.exit(0)

if __name__ == '__main__':
    main()