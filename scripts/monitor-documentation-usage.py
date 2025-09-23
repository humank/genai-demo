#!/usr/bin/env python3
"""
Documentation Usage Monitoring Script
Monitors documentation access patterns and performance metrics.
"""

import os
import sys
import argparse
import subprocess
import json
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List
import re

class DocumentationUsageMonitor:
    def __init__(self, viewpoint: str):
        self.viewpoint = viewpoint
        self.metrics = {
            'file_sizes': {},
            'link_counts': {},
            'complexity_scores': {},
            'git_activity': {},
            'performance_metrics': {}
        }
    
    def analyze_file_sizes(self, viewpoint_dir: str) -> Dict:
        """Analyze file sizes to identify potential performance issues."""
        file_sizes = {}
        
        for root, dirs, files in os.walk(viewpoint_dir):
            dirs[:] = [d for d in dirs if d not in ['.git', '.kiro', 'node_modules', 'generated']]
            
            for file in files:
                if file.endswith('.md'):
                    file_path = os.path.join(root, file)
                    relative_path = os.path.relpath(file_path, viewpoint_dir)
                    
                    try:
                        size = os.path.getsize(file_path)
                        file_sizes[relative_path] = {
                            'size_bytes': size,
                            'size_kb': round(size / 1024, 2),
                            'category': self._categorize_file_size(size)
                        }
                    except:
                        continue
        
        return file_sizes
    
    def _categorize_file_size(self, size_bytes: int) -> str:
        """Categorize file size for performance analysis."""
        if size_bytes < 5000:  # < 5KB
            return "small"
        elif size_bytes < 20000:  # < 20KB
            return "medium"
        elif size_bytes < 50000:  # < 50KB
            return "large"
        else:  # >= 50KB
            return "very_large"
    
    def analyze_link_density(self, viewpoint_dir: str) -> Dict:
        """Analyze link density and complexity."""
        link_analysis = {}
        
        for root, dirs, files in os.walk(viewpoint_dir):
            dirs[:] = [d for d in dirs if d not in ['.git', '.kiro', 'node_modules', 'generated']]
            
            for file in files:
                if file.endswith('.md'):
                    file_path = os.path.join(root, file)
                    relative_path = os.path.relpath(file_path, viewpoint_dir)
                    
                    try:
                        with open(file_path, 'r', encoding='utf-8') as f:
                            content = f.read()
                        
                        # Count different types of links
                        markdown_links = re.findall(r'\[([^\]]+)\]\(([^)]+)\)', content)
                        internal_links = [link for _, url in markdown_links if not url.startswith('http')]
                        external_links = [link for _, url in markdown_links if url.startswith('http')]
                        
                        # Count images
                        images = re.findall(r'!\[([^\]]*)\]\(([^)]+)\)', content)
                        
                        # Count code blocks
                        code_blocks = re.findall(r'```[\s\S]*?```', content)
                        
                        word_count = len(content.split())
                        
                        link_analysis[relative_path] = {
                            'total_links': len(markdown_links),
                            'internal_links': len(internal_links),
                            'external_links': len(external_links),
                            'images': len(images),
                            'code_blocks': len(code_blocks),
                            'word_count': word_count,
                            'link_density': len(markdown_links) / max(word_count, 1) * 100,
                            'complexity_score': self._calculate_complexity_score(content)
                        }
                    except:
                        continue
        
        return link_analysis
    
    def _calculate_complexity_score(self, content: str) -> float:
        """Calculate content complexity score."""
        score = 0
        
        # Headers (structure)
        headers = re.findall(r'^#{1,6}\s+', content, re.MULTILINE)
        score += len(headers) * 0.5
        
        # Lists
        lists = re.findall(r'^\s*[-*+]\s+', content, re.MULTILINE)
        score += len(lists) * 0.2
        
        # Code blocks
        code_blocks = re.findall(r'```[\s\S]*?```', content)
        score += len(code_blocks) * 2
        
        # Tables
        tables = re.findall(r'\|.*\|', content)
        score += len(tables) * 0.3
        
        # Links
        links = re.findall(r'\[([^\]]+)\]\(([^)]+)\)', content)
        score += len(links) * 0.1
        
        return round(score, 2)
    
    def analyze_git_activity(self, viewpoint_dir: str) -> Dict:
        """Analyze git activity for documentation files."""
        git_activity = {}
        
        try:
            # Get commit activity for the last 30 days
            since_date = (datetime.now() - timedelta(days=30)).strftime('%Y-%m-%d')
            
            result = subprocess.run([
                'git', 'log', '--since', since_date, '--name-only', '--pretty=format:%H|%an|%ad|%s',
                '--date=iso', '--', f'{viewpoint_dir}/**/*.md'
            ], capture_output=True, text=True, cwd='.')
            
            if result.returncode == 0:
                lines = result.stdout.strip().split('\n')
                current_commit = None
                
                for line in lines:
                    if '|' in line:  # Commit info line
                        parts = line.split('|', 3)
                        if len(parts) == 4:
                            current_commit = {
                                'hash': parts[0],
                                'author': parts[1],
                                'date': parts[2],
                                'message': parts[3],
                                'files': []
                            }
                    elif line.strip() and current_commit:  # File line
                        if line.strip().endswith('.md') and viewpoint_dir in line:
                            current_commit['files'].append(line.strip())
                            
                            relative_path = os.path.relpath(line.strip(), viewpoint_dir)
                            if relative_path not in git_activity:
                                git_activity[relative_path] = {
                                    'commits': [],
                                    'total_commits': 0,
                                    'authors': set(),
                                    'last_modified': None
                                }
                            
                            git_activity[relative_path]['commits'].append(current_commit)
                            git_activity[relative_path]['total_commits'] += 1
                            git_activity[relative_path]['authors'].add(current_commit['author'])
                            
                            if not git_activity[relative_path]['last_modified']:
                                git_activity[relative_path]['last_modified'] = current_commit['date']
                
                # Convert sets to lists for JSON serialization
                for file_data in git_activity.values():
                    file_data['authors'] = list(file_data['authors'])
                    file_data['author_count'] = len(file_data['authors'])
        
        except Exception as e:
            print(f"Warning: Could not analyze git activity: {e}")
        
        return git_activity
    
    def estimate_performance_metrics(self, viewpoint_dir: str) -> Dict:
        """Estimate performance metrics for documentation."""
        performance = {
            'total_files': 0,
            'total_size_kb': 0,
            'average_file_size_kb': 0,
            'large_files': [],
            'estimated_load_time': 0,
            'complexity_distribution': {
                'simple': 0,
                'moderate': 0,
                'complex': 0
            }
        }
        
        file_sizes = self.metrics.get('file_sizes', {})
        link_analysis = self.metrics.get('link_counts', {})
        
        total_size = 0
        file_count = 0
        
        for file_path, size_info in file_sizes.items():
            file_count += 1
            total_size += size_info['size_kb']
            
            if size_info['size_kb'] > 20:  # Files larger than 20KB
                performance['large_files'].append({
                    'file': file_path,
                    'size_kb': size_info['size_kb']
                })
        
        performance['total_files'] = file_count
        performance['total_size_kb'] = round(total_size, 2)
        performance['average_file_size_kb'] = round(total_size / max(file_count, 1), 2)
        
        # Estimate load time (rough calculation)
        # Assume 1KB = 10ms load time (conservative estimate)
        performance['estimated_load_time'] = round(total_size * 0.01, 2)
        
        # Complexity distribution
        for file_path, analysis in link_analysis.items():
            complexity = analysis.get('complexity_score', 0)
            if complexity < 5:
                performance['complexity_distribution']['simple'] += 1
            elif complexity < 15:
                performance['complexity_distribution']['moderate'] += 1
            else:
                performance['complexity_distribution']['complex'] += 1
        
        return performance
    
    def monitor_usage(self, viewpoint_dir: str) -> Dict:
        """Monitor documentation usage and performance."""
        print(f"Monitoring documentation usage for: {viewpoint_dir}")
        
        # Collect all metrics
        self.metrics['file_sizes'] = self.analyze_file_sizes(viewpoint_dir)
        self.metrics['link_counts'] = self.analyze_link_density(viewpoint_dir)
        self.metrics['git_activity'] = self.analyze_git_activity(viewpoint_dir)
        self.metrics['performance_metrics'] = self.estimate_performance_metrics(viewpoint_dir)
        
        return {
            'timestamp': datetime.now().isoformat(),
            'viewpoint': self.viewpoint,
            'directory': viewpoint_dir,
            'metrics': self.metrics
        }
    
    def generate_report(self, monitoring_data: Dict, output_file: str = None):
        """Generate usage monitoring report."""
        metrics = monitoring_data['metrics']
        perf = metrics['performance_metrics']
        
        report_lines = [
            f"# Documentation Usage Monitoring Report - {self.viewpoint.title()} Viewpoint",
            f"Generated: {monitoring_data['timestamp']}",
            f"Directory: `{monitoring_data['directory']}`",
            "",
            "## Performance Overview",
            f"- **Total Files**: {perf['total_files']}",
            f"- **Total Size**: {perf['total_size_kb']} KB",
            f"- **Average File Size**: {perf['average_file_size_kb']} KB",
            f"- **Estimated Load Time**: {perf['estimated_load_time']} seconds",
            f"- **Large Files (>20KB)**: {len(perf['large_files'])}",
            ""
        ]
        
        # Performance status
        if perf['estimated_load_time'] > 5:
            status = "❌ SLOW"
            recommendations = ["Optimize large files", "Consider splitting content", "Compress images"]
        elif perf['estimated_load_time'] > 2:
            status = "⚠️ MODERATE"
            recommendations = ["Monitor file sizes", "Optimize where possible"]
        else:
            status = "✅ FAST"
            recommendations = ["Maintain current performance"]
        
        report_lines.extend([
            f"**Performance Status**: {status}",
            "",
            "## Complexity Distribution",
            f"- **Simple** (score < 5): {perf['complexity_distribution']['simple']} files",
            f"- **Moderate** (score 5-15): {perf['complexity_distribution']['moderate']} files",
            f"- **Complex** (score > 15): {perf['complexity_distribution']['complex']} files",
            ""
        ])
        
        # Large files analysis
        if perf['large_files']:
            report_lines.extend([
                "## Large Files Analysis",
                ""
            ])
            
            sorted_large = sorted(perf['large_files'], key=lambda x: x['size_kb'], reverse=True)
            for file_info in sorted_large[:10]:  # Top 10 largest
                report_lines.append(f"- **{file_info['file']}**: {file_info['size_kb']} KB")
            
            report_lines.append("")
        
        # Git activity summary
        git_activity = metrics.get('git_activity', {})
        if git_activity:
            active_files = len(git_activity)
            total_commits = sum(data['total_commits'] for data in git_activity.values())
            
            report_lines.extend([
                "## Recent Activity (Last 30 Days)",
                f"- **Active Files**: {active_files}",
                f"- **Total Commits**: {total_commits}",
                f"- **Average Commits per File**: {round(total_commits / max(active_files, 1), 1)}",
                ""
            ])
            
            # Most active files
            most_active = sorted(git_activity.items(), key=lambda x: x[1]['total_commits'], reverse=True)[:5]
            if most_active:
                report_lines.extend([
                    "### Most Active Files",
                    ""
                ])
                for file_path, activity in most_active:
                    report_lines.append(f"- **{file_path}**: {activity['total_commits']} commits by {activity['author_count']} authors")
                report_lines.append("")
        
        # Link analysis summary
        link_analysis = metrics.get('link_counts', {})
        if link_analysis:
            total_links = sum(data['total_links'] for data in link_analysis.values())
            avg_link_density = sum(data['link_density'] for data in link_analysis.values()) / len(link_analysis)
            
            report_lines.extend([
                "## Link Analysis",
                f"- **Total Links**: {total_links}",
                f"- **Average Link Density**: {round(avg_link_density, 1)}% (links per 100 words)",
                ""
            ])
            
            # Files with high link density
            high_density = [(f, data) for f, data in link_analysis.items() if data['link_density'] > 10]
            if high_density:
                report_lines.extend([
                    "### High Link Density Files (>10%)",
                    ""
                ])
                for file_path, data in sorted(high_density, key=lambda x: x[1]['link_density'], reverse=True)[:5]:
                    report_lines.append(f"- **{file_path}**: {round(data['link_density'], 1)}% ({data['total_links']} links)")
                report_lines.append("")
        
        # Recommendations
        report_lines.extend([
            "## Recommendations",
            ""
        ])
        
        for i, rec in enumerate(recommendations, 1):
            report_lines.append(f"{i}. {rec}")
        
        report_lines.extend([
            "",
            "### Performance Optimization",
            "1. **Monitor File Sizes**: Keep individual files under 20KB when possible",
            "2. **Optimize Images**: Compress images and use appropriate formats",
            "3. **Split Large Files**: Break large documents into smaller, focused sections",
            "4. **Link Management**: Maintain reasonable link density for readability",
            "",
            "### Usage Insights",
            "1. **Active Content**: Focus maintenance on frequently updated files",
            "2. **Complexity Management**: Simplify overly complex documents",
            "3. **Performance Monitoring**: Regular monitoring to catch performance issues early",
            ""
        ])
        
        report_content = '\n'.join(report_lines)
        
        if output_file:
            os.makedirs(os.path.dirname(output_file), exist_ok=True)
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write(report_content)
            
            # Also save raw data as JSON
            json_file = output_file.replace('.md', '.json')
            with open(json_file, 'w', encoding='utf-8') as f:
                json.dump(monitoring_data, f, indent=2, default=str)
            
            print(f"Usage monitoring report saved to: {output_file}")
            print(f"Raw data saved to: {json_file}")
        else:
            print(report_content)

def main():
    parser = argparse.ArgumentParser(description='Monitor documentation usage and performance')
    parser.add_argument('--viewpoint', required=True, help='Viewpoint to monitor (e.g., development)')
    parser.add_argument('--output', help='Output file for report')
    
    args = parser.parse_args()
    
    viewpoint_dir = f"docs/viewpoints/{args.viewpoint}"
    if not os.path.exists(viewpoint_dir):
        print(f"Error: Viewpoint directory '{viewpoint_dir}' does not exist")
        sys.exit(1)
    
    monitor = DocumentationUsageMonitor(args.viewpoint)
    monitoring_data = monitor.monitor_usage(viewpoint_dir)
    
    output_file = args.output
    if not output_file:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        output_file = f"reports-summaries/task-execution/documentation-usage-{args.viewpoint}-{timestamp}.md"
    
    monitor.generate_report(monitoring_data, output_file)
    
    # Exit based on performance
    perf = monitoring_data['metrics']['performance_metrics']
    if perf['estimated_load_time'] > 5:
        print(f"\n❌ Documentation performance is slow ({perf['estimated_load_time']}s)")
        sys.exit(2)
    elif perf['estimated_load_time'] > 2:
        print(f"\n⚠️  Documentation performance is moderate ({perf['estimated_load_time']}s)")
        sys.exit(1)
    else:
        print(f"\n✅ Documentation performance is good ({perf['estimated_load_time']}s)")
        sys.exit(0)

if __name__ == '__main__':
    main()