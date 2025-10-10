#!/usr/bin/env python3
"""
Performance Test Report Generator

This script generates comprehensive performance test reports from test results.
It creates HTML and JSON reports with charts and analysis.
"""

import json
import logging
from datetime import datetime
from pathlib import Path
from typing import Dict, List
import matplotlib.pyplot as plt
import matplotlib
matplotlib.use('Agg')  # Non-interactive backend

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


class PerformanceReportGenerator:
    """Generator for performance test reports"""
    
    def __init__(self, output_dir: str = "reports/performance"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
    def generate_report(self, test_results: Dict, report_name: str = "performance_report"):
        """Generate comprehensive performance report"""
        logger.info(f"Generating performance report: {report_name}")
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # Generate JSON report
        json_path = self.output_dir / f"{report_name}_{timestamp}.json"
        with open(json_path, 'w') as f:
            json.dump(test_results, f, indent=2)
        logger.info(f"JSON report saved: {json_path}")
        
        # Generate HTML report
        html_path = self.output_dir / f"{report_name}_{timestamp}.html"
        html_content = self._generate_html_report(test_results, timestamp)
        with open(html_path, 'w') as f:
            f.write(html_content)
        logger.info(f"HTML report saved: {html_path}")
        
        # Generate charts
        self._generate_charts(test_results, timestamp)
        
        logger.info("Report generation complete")
        
    def _generate_html_report(self, results: Dict, timestamp: str) -> str:
        """Generate HTML report"""
        html = f"""
<!DOCTYPE html>
<html>
<head>
    <title>Performance Test Report - {timestamp}</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; }}
        h1 {{ color: #333; }}
        .summary {{ background: #f0f0f0; padding: 15px; border-radius: 5px; }}
        .test-result {{ margin: 20px 0; padding: 15px; border: 1px solid #ddd; }}
        .pass {{ background: #d4edda; }}
        .fail {{ background: #f8d7da; }}
        table {{ border-collapse: collapse; width: 100%; }}
        th, td {{ border: 1px solid #ddd; padding: 8px; text-align: left; }}
        th {{ background: #4CAF50; color: white; }}
    </style>
</head>
<body>
    <h1>Performance Test Report</h1>
    <p>Generated: {timestamp}</p>
    
    <div class="summary">
        <h2>Summary</h2>
        <p>Total Tests: {len(results.get('tests', []))}</p>
        <p>Passed: {sum(1 for t in results.get('tests', []) if t.get('success'))}</p>
        <p>Failed: {sum(1 for t in results.get('tests', []) if not t.get('success'))}</p>
    </div>
    
    <h2>Test Results</h2>
"""
        
        for test in results.get('tests', []):
            status_class = "pass" if test.get('success') else "fail"
            status_text = "✓ PASS" if test.get('success') else "✗ FAIL"
            
            html += f"""
    <div class="test-result {status_class}">
        <h3>{status_text} - {test.get('test_name', 'Unknown')}</h3>
        <table>
            <tr><th>Metric</th><th>Value</th></tr>
"""
            
            for key, value in test.items():
                if key not in ['test_name', 'success']:
                    html += f"<tr><td>{key}</td><td>{value}</td></tr>\n"
            
            html += """
        </table>
    </div>
"""
        
        html += """
</body>
</html>
"""
        return html
    
    def _generate_charts(self, results: Dict, timestamp: str):
        """Generate performance charts"""
        tests = results.get('tests', [])
        
        # Latency chart
        test_names = [t.get('test_name', 'Unknown') for t in tests if 'avg_latency_ms' in t]
        latencies = [t.get('avg_latency_ms', 0) for t in tests if 'avg_latency_ms' in t]
        
        if test_names and latencies:
            plt.figure(figsize=(10, 6))
            plt.bar(test_names, latencies)
            plt.xlabel('Test')
            plt.ylabel('Latency (ms)')
            plt.title('Average Latency by Test')
            plt.xticks(rotation=45, ha='right')
            plt.tight_layout()
            plt.savefig(self.output_dir / f"latency_chart_{timestamp}.png")
            plt.close()
            logger.info("Latency chart generated")


def main():
    """Main entry point"""
    # Example usage
    sample_results = {
        "timestamp": datetime.now().isoformat(),
        "tests": [
            {
                "test_name": "concurrent_users",
                "success": True,
                "avg_latency_ms": 150.5,
                "p95_latency_ms": 250.0,
                "throughput_rps": 1000.0
            },
            {
                "test_name": "cross_region_latency",
                "success": True,
                "avg_latency_ms": 85.2,
                "p95_latency_ms": 180.0
            }
        ]
    }
    
    generator = PerformanceReportGenerator()
    generator.generate_report(sample_results)


if __name__ == "__main__":
    main()
