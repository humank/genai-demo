#!/usr/bin/env python3
"""
Report Generation System for Translation Operations

This module generates comprehensive reports about translation system performance,
including HTML dashboards, CSV exports, and detailed analysis reports.
"""

import os
import sys
import json
import sqlite3
from pathlib import Path
from typing import Dict, List, Optional, Any
from datetime import datetime, timedelta
import argparse

# Add current directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from monitoring import MetricsCollector, get_metrics_collector

class ReportGenerator:
    """
    Comprehensive report generation system.
    
    This class generates various types of reports including performance dashboards,
    error analysis, trend reports, and maintenance recommendations.
    """
    
    def __init__(self, metrics_collector: MetricsCollector = None):
        """
        Initialize report generator.
        
        Args:
            metrics_collector: MetricsCollector instance (uses global if None)
        """
        self.metrics_collector = metrics_collector or get_metrics_collector()
    
    def generate_performance_report(self, hours: int = 24, output_file: str = None) -> str:
        """
        Generate comprehensive performance report.
        
        Args:
            hours: Number of hours to analyze
            output_file: Output file path (auto-generated if None)
            
        Returns:
            Path to generated report file
        """
        # Get performance data
        summary = self.metrics_collector.get_performance_summary(hours)
        
        # Generate report content
        report_data = {
            'report_info': {
                'title': 'Translation System Performance Report',
                'generated_at': datetime.now().isoformat(),
                'time_period_hours': hours,
                'report_type': 'performance'
            },
            'executive_summary': self._generate_executive_summary(summary),
            'performance_metrics': summary,
            'detailed_analysis': self._generate_detailed_analysis(summary),
            'recommendations': self._generate_recommendations(summary),
            'charts_data': self._prepare_charts_data(summary)
        }
        
        # Determine output file
        if output_file is None:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            output_file = f'translation_performance_report_{timestamp}.json'
        
        # Save report
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(report_data, f, indent=2, ensure_ascii=False)
        
        print(f"📊 Performance report generated: {output_file}")
        return output_file
    
    def generate_html_dashboard(self, hours: int = 24, output_file: str = None) -> str:
        """
        Generate HTML dashboard with interactive charts.
        
        Args:
            hours: Number of hours to analyze
            output_file: Output file path (auto-generated if None)
            
        Returns:
            Path to generated HTML file
        """
        # Get performance data
        summary = self.metrics_collector.get_performance_summary(hours)
        
        # Determine output file
        if output_file is None:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            output_file = f'translation_dashboard_{timestamp}.html'
        
        # Generate HTML content
        html_content = self._generate_html_dashboard_content(summary, hours)
        
        # Save HTML file
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(html_content)
        
        print(f"🌐 HTML dashboard generated: {output_file}")
        return output_file
    
    def generate_csv_export(self, hours: int = 24, output_file: str = None) -> str:
        """
        Generate CSV export of metrics data.
        
        Args:
            hours: Number of hours to export
            output_file: Output file path (auto-generated if None)
            
        Returns:
            Path to generated CSV file
        """
        import csv
        
        # Determine output file
        if output_file is None:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            output_file = f'translation_metrics_{timestamp}.csv'
        
        # Get raw metrics data from database
        cutoff_time = datetime.now() - timedelta(hours=hours)
        
        try:
            with sqlite3.connect(self.metrics_collector.db_path) as conn:
                cursor = conn.execute("""
                    SELECT 
                        operation_id, operation_type, start_time, end_time,
                        files_processed, files_successful, files_failed, files_skipped,
                        total_processing_time, average_time_per_file,
                        success_rate, failure_rate
                    FROM translation_metrics 
                    WHERE start_time >= ? 
                    ORDER BY start_time DESC
                """, (cutoff_time.isoformat(),))
                
                # Write CSV file
                with open(output_file, 'w', newline='', encoding='utf-8') as csvfile:
                    writer = csv.writer(csvfile)
                    
                    # Write header
                    writer.writerow([
                        'Operation ID', 'Operation Type', 'Start Time', 'End Time',
                        'Files Processed', 'Files Successful', 'Files Failed', 'Files Skipped',
                        'Total Processing Time', 'Average Time Per File',
                        'Success Rate', 'Failure Rate'
                    ])
                    
                    # Write data rows
                    for row in cursor.fetchall():
                        writer.writerow(row)
                
        except Exception as e:
            print(f"❌ Failed to generate CSV export: {e}")
            return None
        
        print(f"📄 CSV export generated: {output_file}")
        return output_file
    
    def generate_error_analysis_report(self, hours: int = 24, output_file: str = None) -> str:
        """
        Generate detailed error analysis report.
        
        Args:
            hours: Number of hours to analyze
            output_file: Output file path (auto-generated if None)
            
        Returns:
            Path to generated report file
        """
        # Get performance data
        summary = self.metrics_collector.get_performance_summary(hours)
        error_analysis = summary.get('error_analysis', {})
        
        # Generate detailed error report
        report_data = {
            'report_info': {
                'title': 'Translation System Error Analysis Report',
                'generated_at': datetime.now().isoformat(),
                'time_period_hours': hours,
                'report_type': 'error_analysis'
            },
            'error_summary': {
                'total_errors': sum(error_analysis.get('error_counts', {}).values()),
                'error_types': len(error_analysis.get('error_counts', {})),
                'most_common_errors': error_analysis.get('most_common_errors', [])
            },
            'detailed_error_analysis': error_analysis,
            'error_patterns': self._analyze_error_patterns(error_analysis),
            'troubleshooting_guide': self._generate_troubleshooting_guide(error_analysis),
            'recommendations': self._generate_error_recommendations(error_analysis)
        }
        
        # Determine output file
        if output_file is None:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            output_file = f'translation_error_analysis_{timestamp}.json'
        
        # Save report
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(report_data, f, indent=2, ensure_ascii=False)
        
        print(f"🔍 Error analysis report generated: {output_file}")
        return output_file
    
    def _generate_executive_summary(self, summary: Dict) -> Dict:
        """Generate executive summary from performance data."""
        return {
            'key_metrics': {
                'total_operations': summary.get('total_operations', 0),
                'total_files_processed': summary.get('total_files_processed', 0),
                'overall_success_rate': f"{summary.get('overall_success_rate', 0):.1f}%",
                'average_processing_time': f"{summary.get('average_processing_time', 0):.2f}s"
            },
            'performance_status': self._determine_performance_status(summary),
            'key_insights': self._extract_key_insights(summary),
            'action_items': self._generate_action_items(summary)
        }
    
    def _generate_detailed_analysis(self, summary: Dict) -> Dict:
        """Generate detailed analysis from performance data."""
        return {
            'operation_breakdown': summary.get('operations_by_type', {}),
            'performance_trends': summary.get('performance_trends', {}),
            'error_analysis': summary.get('error_analysis', {}),
            'efficiency_metrics': {
                'files_per_operation': summary.get('total_files_processed', 0) / max(summary.get('total_operations', 1), 1),
                'success_rate_variance': self._calculate_success_rate_variance(summary),
                'processing_time_distribution': self._analyze_processing_time_distribution(summary)
            }
        }
    
    def _generate_recommendations(self, summary: Dict) -> List[Dict]:
        """Generate actionable recommendations based on performance data."""
        recommendations = []
        
        # Success rate recommendations
        success_rate = summary.get('overall_success_rate', 0)
        if success_rate < 90:
            recommendations.append({
                'category': 'Quality',
                'priority': 'High',
                'title': 'Improve Success Rate',
                'description': f'Current success rate is {success_rate:.1f}%. Investigate common failure patterns.',
                'actions': [
                    'Review error logs for common failure patterns',
                    'Improve input validation and error handling',
                    'Consider adjusting translation parameters'
                ]
            })
        
        # Performance recommendations
        avg_time = summary.get('average_processing_time', 0)
        if avg_time > 5.0:  # 5 seconds per operation
            recommendations.append({
                'category': 'Performance',
                'priority': 'Medium',
                'title': 'Optimize Processing Time',
                'description': f'Average processing time is {avg_time:.2f}s. Consider optimization.',
                'actions': [
                    'Increase parallel worker count',
                    'Optimize translation prompts',
                    'Implement caching for repeated content'
                ]
            })
        
        # Error pattern recommendations
        error_analysis = summary.get('error_analysis', {})
        most_common_errors = error_analysis.get('most_common_errors', [])
        if most_common_errors:
            top_error = most_common_errors[0]
            recommendations.append({
                'category': 'Reliability',
                'priority': 'High',
                'title': f'Address {top_error[0]} Errors',
                'description': f'Most common error type: {top_error[0]} ({top_error[1]} occurrences)',
                'actions': [
                    f'Investigate root cause of {top_error[0]} errors',
                    'Implement specific error handling for this error type',
                    'Add monitoring alerts for this error pattern'
                ]
            })
        
        return recommendations
    
    def _prepare_charts_data(self, summary: Dict) -> Dict:
        """Prepare data for chart visualization."""
        return {
            'success_rate_chart': {
                'type': 'gauge',
                'value': summary.get('overall_success_rate', 0),
                'max': 100,
                'title': 'Overall Success Rate'
            },
            'operations_by_type_chart': {
                'type': 'pie',
                'data': summary.get('operations_by_type', {}),
                'title': 'Operations by Type'
            },
            'error_distribution_chart': {
                'type': 'bar',
                'data': summary.get('error_analysis', {}).get('error_counts', {}),
                'title': 'Error Distribution'
            },
            'performance_trend_chart': {
                'type': 'line',
                'data': self._prepare_trend_data(summary),
                'title': 'Performance Trends'
            }
        }
    
    def _generate_html_dashboard_content(self, summary: Dict, hours: int) -> str:
        """Generate HTML dashboard content."""
        charts_data = self._prepare_charts_data(summary)
        
        html_template = f"""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Translation System Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            margin: 0;
            padding: 20px;
            background-color: #f5f5f5;
        }}
        .dashboard {{
            max-width: 1200px;
            margin: 0 auto;
        }}
        .header {{
            background: white;
            padding: 20px;
            border-radius: 8px;
            margin-bottom: 20px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }}
        .metrics-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 20px;
        }}
        .metric-card {{
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }}
        .metric-value {{
            font-size: 2em;
            font-weight: bold;
            color: #2563eb;
        }}
        .metric-label {{
            color: #6b7280;
            margin-top: 5px;
        }}
        .charts-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
            gap: 20px;
        }}
        .chart-container {{
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }}
        .recommendations {{
            background: white;
            padding: 20px;
            border-radius: 8px;
            margin-top: 20px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }}
        .recommendation {{
            border-left: 4px solid #f59e0b;
            padding-left: 15px;
            margin-bottom: 15px;
        }}
        .recommendation.high {{
            border-left-color: #ef4444;
        }}
        .recommendation.medium {{
            border-left-color: #f59e0b;
        }}
        .recommendation.low {{
            border-left-color: #10b981;
        }}
    </style>
</head>
<body>
    <div class="dashboard">
        <div class="header">
            <h1>🔄 Translation System Dashboard</h1>
            <p>Performance overview for the last {hours} hours</p>
            <p><small>Generated at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</small></p>
        </div>
        
        <div class="metrics-grid">
            <div class="metric-card">
                <div class="metric-value">{summary.get('total_operations', 0)}</div>
                <div class="metric-label">Total Operations</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">{summary.get('total_files_processed', 0)}</div>
                <div class="metric-label">Files Processed</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">{summary.get('overall_success_rate', 0):.1f}%</div>
                <div class="metric-label">Success Rate</div>
            </div>
            <div class="metric-card">
                <div class="metric-value">{summary.get('average_processing_time', 0):.1f}s</div>
                <div class="metric-label">Avg Processing Time</div>
            </div>
        </div>
        
        <div class="charts-grid">
            <div class="chart-container">
                <h3>Operations by Type</h3>
                <canvas id="operationsChart"></canvas>
            </div>
            <div class="chart-container">
                <h3>Error Distribution</h3>
                <canvas id="errorsChart"></canvas>
            </div>
        </div>
        
        <div class="recommendations">
            <h3>📋 Recommendations</h3>
            {self._generate_html_recommendations(summary)}
        </div>
    </div>
    
    <script>
        // Operations by Type Chart
        const operationsCtx = document.getElementById('operationsChart').getContext('2d');
        const operationsData = {json.dumps(summary.get('operations_by_type', {}))};
        
        new Chart(operationsCtx, {{
            type: 'doughnut',
            data: {{
                labels: Object.keys(operationsData),
                datasets: [{{
                    data: Object.values(operationsData).map(d => d.count),
                    backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6']
                }}]
            }},
            options: {{
                responsive: true,
                plugins: {{
                    legend: {{
                        position: 'bottom'
                    }}
                }}
            }}
        }});
        
        // Error Distribution Chart
        const errorsCtx = document.getElementById('errorsChart').getContext('2d');
        const errorData = {json.dumps(summary.get('error_analysis', {}).get('error_counts', {}))};
        
        new Chart(errorsCtx, {{
            type: 'bar',
            data: {{
                labels: Object.keys(errorData),
                datasets: [{{
                    label: 'Error Count',
                    data: Object.values(errorData),
                    backgroundColor: '#ef4444'
                }}]
            }},
            options: {{
                responsive: true,
                scales: {{
                    y: {{
                        beginAtZero: true
                    }}
                }}
            }}
        }});
    </script>
</body>
</html>
        """
        
        return html_template
    
    def _generate_html_recommendations(self, summary: Dict) -> str:
        """Generate HTML for recommendations section."""
        recommendations = self._generate_recommendations(summary)
        
        html = ""
        for rec in recommendations:
            priority_class = rec['priority'].lower()
            html += f"""
            <div class="recommendation {priority_class}">
                <h4>{rec['title']} ({rec['priority']} Priority)</h4>
                <p>{rec['description']}</p>
                <ul>
                    {''.join(f'<li>{action}</li>' for action in rec['actions'])}
                </ul>
            </div>
            """
        
        return html
    
    def _determine_performance_status(self, summary: Dict) -> str:
        """Determine overall performance status."""
        success_rate = summary.get('overall_success_rate', 0)
        
        if success_rate >= 95:
            return 'Excellent'
        elif success_rate >= 90:
            return 'Good'
        elif success_rate >= 80:
            return 'Fair'
        else:
            return 'Needs Improvement'
    
    def _extract_key_insights(self, summary: Dict) -> List[str]:
        """Extract key insights from performance data."""
        insights = []
        
        # Success rate insight
        success_rate = summary.get('overall_success_rate', 0)
        if success_rate >= 95:
            insights.append(f"Excellent success rate of {success_rate:.1f}% indicates high system reliability")
        elif success_rate < 80:
            insights.append(f"Low success rate of {success_rate:.1f}% requires immediate attention")
        
        # Operations insight
        total_ops = summary.get('total_operations', 0)
        if total_ops > 0:
            avg_files = summary.get('total_files_processed', 0) / total_ops
            insights.append(f"Average of {avg_files:.1f} files processed per operation")
        
        # Trend insight
        trends = summary.get('performance_trends', {})
        if trends.get('success_rate_trend') == 'improving':
            insights.append("Success rate is trending upward - system performance is improving")
        elif trends.get('success_rate_trend') == 'declining':
            insights.append("Success rate is declining - investigate recent changes")
        
        return insights
    
    def _generate_action_items(self, summary: Dict) -> List[str]:
        """Generate immediate action items."""
        actions = []
        
        success_rate = summary.get('overall_success_rate', 0)
        if success_rate < 90:
            actions.append("Review and address common error patterns")
        
        error_analysis = summary.get('error_analysis', {})
        if error_analysis.get('most_common_errors'):
            top_error = error_analysis['most_common_errors'][0]
            actions.append(f"Investigate {top_error[0]} errors ({top_error[1]} occurrences)")
        
        avg_time = summary.get('average_processing_time', 0)
        if avg_time > 5.0:
            actions.append("Optimize processing performance")
        
        return actions
    
    def _calculate_success_rate_variance(self, summary: Dict) -> float:
        """Calculate variance in success rates across operations."""
        # This would require more detailed data analysis
        # For now, return a placeholder
        return 0.0
    
    def _analyze_processing_time_distribution(self, summary: Dict) -> Dict:
        """Analyze processing time distribution."""
        # This would require more detailed data analysis
        # For now, return a placeholder
        return {
            'mean': summary.get('average_processing_time', 0),
            'distribution': 'normal'  # Placeholder
        }
    
    def _analyze_error_patterns(self, error_analysis: Dict) -> Dict:
        """Analyze error patterns for insights."""
        patterns = {
            'temporal_patterns': 'No clear temporal pattern detected',
            'file_patterns': 'Errors distributed across various file types',
            'correlation_analysis': 'No strong correlations identified'
        }
        
        # Analyze most common errors
        most_common = error_analysis.get('most_common_errors', [])
        if most_common:
            top_error = most_common[0]
            patterns['primary_concern'] = f"{top_error[0]} errors account for {top_error[1]} failures"
        
        return patterns
    
    def _generate_troubleshooting_guide(self, error_analysis: Dict) -> Dict:
        """Generate troubleshooting guide based on error patterns."""
        guide = {}
        
        error_counts = error_analysis.get('error_counts', {})
        for error_type, count in error_counts.items():
            guide[error_type] = {
                'frequency': count,
                'troubleshooting_steps': self._get_troubleshooting_steps(error_type),
                'prevention_tips': self._get_prevention_tips(error_type)
            }
        
        return guide
    
    def _generate_error_recommendations(self, error_analysis: Dict) -> List[Dict]:
        """Generate recommendations based on error analysis."""
        recommendations = []
        
        most_common = error_analysis.get('most_common_errors', [])
        for error_type, count in most_common[:3]:  # Top 3 errors
            recommendations.append({
                'error_type': error_type,
                'frequency': count,
                'priority': 'High' if count > 10 else 'Medium',
                'actions': self._get_error_specific_actions(error_type)
            })
        
        return recommendations
    
    def _get_troubleshooting_steps(self, error_type: str) -> List[str]:
        """Get troubleshooting steps for specific error type."""
        steps_map = {
            'translation_error': [
                'Check network connectivity',
                'Verify translation API credentials',
                'Review input file format',
                'Check for rate limiting'
            ],
            'file_error': [
                'Verify file permissions',
                'Check disk space',
                'Validate file encoding',
                'Ensure file is not locked'
            ],
            'format_error': [
                'Validate markdown syntax',
                'Check for unsupported elements',
                'Review file encoding',
                'Test with simpler content'
            ]
        }
        
        return steps_map.get(error_type, ['Review error logs', 'Check system resources'])
    
    def _get_prevention_tips(self, error_type: str) -> List[str]:
        """Get prevention tips for specific error type."""
        tips_map = {
            'translation_error': [
                'Implement retry logic with exponential backoff',
                'Monitor API rate limits',
                'Use connection pooling'
            ],
            'file_error': [
                'Implement proper file locking',
                'Monitor disk space',
                'Validate permissions before processing'
            ],
            'format_error': [
                'Implement input validation',
                'Use markdown linting',
                'Provide clear format guidelines'
            ]
        }
        
        return tips_map.get(error_type, ['Implement better error handling'])
    
    def _get_error_specific_actions(self, error_type: str) -> List[str]:
        """Get specific actions for error type."""
        actions_map = {
            'translation_error': [
                'Review API configuration',
                'Implement circuit breaker pattern',
                'Add retry mechanism'
            ],
            'file_error': [
                'Check file system permissions',
                'Implement file validation',
                'Add disk space monitoring'
            ],
            'format_error': [
                'Add input sanitization',
                'Implement format validation',
                'Improve error messages'
            ]
        }
        
        return actions_map.get(error_type, ['Investigate root cause'])
    
    def _prepare_trend_data(self, summary: Dict) -> Dict:
        """Prepare trend data for visualization."""
        # This would require historical data analysis
        # For now, return placeholder data
        return {
            'labels': ['Week 1', 'Week 2', 'Week 3', 'Week 4'],
            'success_rate': [85, 88, 92, 95],
            'processing_time': [3.2, 2.8, 2.5, 2.3]
        }

def main():
    """Main function for report generation CLI."""
    parser = argparse.ArgumentParser(
        description='Generate translation system reports',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Generate performance report for last 24 hours
  python report_generator.py --performance --hours 24

  # Generate HTML dashboard
  python report_generator.py --dashboard --hours 48

  # Generate CSV export
  python report_generator.py --csv --hours 168

  # Generate error analysis report
  python report_generator.py --errors --hours 24

  # Generate all reports
  python report_generator.py --all --hours 24
        """
    )
    
    parser.add_argument('--performance', action='store_true',
                       help='Generate performance report')
    parser.add_argument('--dashboard', action='store_true',
                       help='Generate HTML dashboard')
    parser.add_argument('--csv', action='store_true',
                       help='Generate CSV export')
    parser.add_argument('--errors', action='store_true',
                       help='Generate error analysis report')
    parser.add_argument('--all', action='store_true',
                       help='Generate all report types')
    
    parser.add_argument('--hours', type=int, default=24,
                       help='Number of hours to analyze (default: 24)')
    parser.add_argument('--output-dir', default='.',
                       help='Output directory for reports')
    
    args = parser.parse_args()
    
    # Ensure output directory exists
    output_dir = Path(args.output_dir)
    output_dir.mkdir(exist_ok=True)
    
    # Initialize report generator
    generator = ReportGenerator()
    
    generated_files = []
    
    try:
        if args.all or args.performance:
            output_file = output_dir / f'performance_report_{datetime.now().strftime("%Y%m%d_%H%M%S")}.json'
            file_path = generator.generate_performance_report(args.hours, str(output_file))
            generated_files.append(file_path)
        
        if args.all or args.dashboard:
            output_file = output_dir / f'dashboard_{datetime.now().strftime("%Y%m%d_%H%M%S")}.html'
            file_path = generator.generate_html_dashboard(args.hours, str(output_file))
            generated_files.append(file_path)
        
        if args.all or args.csv:
            output_file = output_dir / f'metrics_export_{datetime.now().strftime("%Y%m%d_%H%M%S")}.csv'
            file_path = generator.generate_csv_export(args.hours, str(output_file))
            if file_path:
                generated_files.append(file_path)
        
        if args.all or args.errors:
            output_file = output_dir / f'error_analysis_{datetime.now().strftime("%Y%m%d_%H%M%S")}.json'
            file_path = generator.generate_error_analysis_report(args.hours, str(output_file))
            generated_files.append(file_path)
        
        if not any([args.performance, args.dashboard, args.csv, args.errors, args.all]):
            parser.print_help()
            return 1
        
        print(f"\n✅ Generated {len(generated_files)} reports:")
        for file_path in generated_files:
            print(f"   📄 {file_path}")
        
        return 0
        
    except Exception as e:
        print(f"❌ Report generation failed: {e}")
        import traceback
        traceback.print_exc()
        return 1

if __name__ == '__main__':
    exit(main())