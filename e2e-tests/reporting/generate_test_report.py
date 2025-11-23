"""
Comprehensive Test Report Generator

Generates HTML reports with charts and analytics for test results.
Requirements: 12.28, 12.29
Implementation: Python using pytest-html, matplotlib, jinja2
"""

import json
import os
from datetime import datetime
from pathlib import Path
from typing import Dict, List
import matplotlib.pyplot as plt
import matplotlib
matplotlib.use('Agg')  # Non-interactive backend


class TestReportGenerator:
    """Generate comprehensive test reports with analytics."""
    
    def __init__(self, output_dir: str = 'reports'):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)
    
    def generate_html_report(self, test_results: Dict):
        """Generate HTML report with test results and charts."""
        timestamp = datetime.now().strftime('%Y%m%d-%H%M%S')
        report_file = self.output_dir / f'test-report-{timestamp}.html'
        
        # Generate charts
        self._generate_charts(test_results)
        
        # Generate HTML
        html_content = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <title>Staging Test Report</title>
            <style>
                body {{ font-family: Arial, sans-serif; margin: 20px; }}
                .summary {{ background: #f0f0f0; padding: 15px; border-radius: 5px; }}
                .passed {{ color: green; }}
                .failed {{ color: red; }}
                table {{ border-collapse: collapse; width: 100%; margin-top: 20px; }}
                th, td {{ border: 1px solid #ddd; padding: 8px; text-align: left; }}
                th {{ background-color: #4CAF50; color: white; }}
            </style>
        </head>
        <body>
            <h1>Staging Test Report</h1>
            <div class="summary">
                <h2>Summary</h2>
                <p>Total Tests: {test_results.get('total', 0)}</p>
                <p class="passed">Passed: {test_results.get('passed', 0)}</p>
                <p class="failed">Failed: {test_results.get('failed', 0)}</p>
                <p>Success Rate: {test_results.get('success_rate', 0):.2f}%</p>
            </div>
            <h2>Test Results</h2>
            <img src="test-results-chart.png" alt="Test Results Chart" style="max-width: 800px;">
        </body>
        </html>
        """
        
        report_file.write_text(html_content)
        print(f"✓ Report generated: {report_file}")
        return str(report_file)
    
    def _generate_charts(self, test_results: Dict):
        """Generate charts for test results."""
        # Pie chart for test results
        labels = ['Passed', 'Failed', 'Skipped']
        sizes = [
            test_results.get('passed', 0),
            test_results.get('failed', 0),
            test_results.get('skipped', 0)
        ]
        colors = ['#4CAF50', '#f44336', '#FFC107']
        
        plt.figure(figsize=(10, 6))
        plt.pie(sizes, labels=labels, colors=colors, autopct='%1.1f%%', startangle=90)
        plt.axis('equal')
        plt.title('Test Results Distribution')
        plt.savefig(self.output_dir / 'test-results-chart.png')
        plt.close()


if __name__ == "__main__":
    # Example usage
    generator = TestReportGenerator()
    test_results = {
        'total': 100,
        'passed': 85,
        'failed': 10,
        'skipped': 5,
        'success_rate': 85.0
    }
    generator.generate_html_report(test_results)
