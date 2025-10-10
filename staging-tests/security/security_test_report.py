#!/usr/bin/env python3
"""
Security Test Report Generator

This script generates comprehensive security test reports from test results.

Features:
1. HTML report generation with charts
2. JSON export for integration
3. CSV export for analysis
4. Executive summary generation
"""

import json
import csv
from datetime import datetime
from typing import List, Dict
from pathlib import Path
import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


class SecurityTestReportGenerator:
    """Generator for security test reports"""
    
    def __init__(self, output_dir: str = "staging-tests/reports/security"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
    
    def generate_html_report(self, test_results: Dict, output_file: str = "security-report.html"):
        """Generate HTML security report"""
        logger.info(f"Generating HTML report: {output_file}")
        
        html_content = f"""
<!DOCTYPE html>
<html>
<head>
    <title>Security Test Report</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; }}
        h1 {{ color: #333; }}
        .summary {{ background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0; }}
        .pass {{ color: green; font-weight: bold; }}
        .fail {{ color: red; font-weight: bold; }}
        table {{ border-collapse: collapse; width: 100%; margin: 20px 0; }}
        th, td {{ border: 1px solid #ddd; padding: 8px; text-align: left; }}
        th {{ background-color: #4CAF50; color: white; }}
        .critical {{ background-color: #ff4444; color: white; }}
        .high {{ background-color: #ff8800; color: white; }}
        .medium {{ background-color: #ffbb33; }}
        .low {{ background-color: #ffff00; }}
    </style>
</head>
<body>
    <h1>Security Test Report</h1>
    <div class="summary">
        <h2>Executive Summary</h2>
        <p><strong>Generated:</strong> {datetime.utcnow().isoformat()}</p>
        <p><strong>Total Tests:</strong> {test_results.get('total_tests', 0)}</p>
        <p><strong>Passed:</strong> <span class="pass">{test_results.get('passed', 0)}</span></p>
        <p><strong>Failed:</strong> <span class="fail">{test_results.get('failed', 0)}</span></p>
        <p><strong>Critical Vulnerabilities:</strong> {test_results.get('critical_vulns', 0)}</p>
        <p><strong>High Vulnerabilities:</strong> {test_results.get('high_vulns', 0)}</p>
    </div>
    
    <h2>Test Results by Category</h2>
    <table>
        <tr>
            <th>Test Category</th>
            <th>Status</th>
            <th>Issues Found</th>
            <th>Details</th>
        </tr>
        {self._generate_test_rows(test_results.get('tests', []))}
    </table>
    
    <h2>Vulnerabilities</h2>
    <table>
        <tr>
            <th>Severity</th>
            <th>Title</th>
            <th>Affected Component</th>
            <th>Remediation</th>
        </tr>
        {self._generate_vulnerability_rows(test_results.get('vulnerabilities', []))}
    </table>
    
    <h2>Recommendations</h2>
    <ul>
        {self._generate_recommendations(test_results)}
    </ul>
</body>
</html>
"""
        
        output_path = self.output_dir / output_file
        output_path.write_text(html_content)
        logger.info(f"HTML report saved to: {output_path}")
        
        return str(output_path)
    
    def generate_json_report(self, test_results: Dict, output_file: str = "security-report.json"):
        """Generate JSON security report"""
        logger.info(f"Generating JSON report: {output_file}")
        
        report = {
            "generated_at": datetime.utcnow().isoformat(),
            "summary": {
                "total_tests": test_results.get('total_tests', 0),
                "passed": test_results.get('passed', 0),
                "failed": test_results.get('failed', 0),
                "critical_vulnerabilities": test_results.get('critical_vulns', 0),
                "high_vulnerabilities": test_results.get('high_vulns', 0),
                "medium_vulnerabilities": test_results.get('medium_vulns', 0),
                "low_vulnerabilities": test_results.get('low_vulns', 0)
            },
            "tests": test_results.get('tests', []),
            "vulnerabilities": test_results.get('vulnerabilities', []),
            "recommendations": self._get_recommendations(test_results)
        }
        
        output_path = self.output_dir / output_file
        output_path.write_text(json.dumps(report, indent=2))
        logger.info(f"JSON report saved to: {output_path}")
        
        return str(output_path)
    
    def generate_csv_report(self, test_results: Dict, output_file: str = "security-report.csv"):
        """Generate CSV security report"""
        logger.info(f"Generating CSV report: {output_file}")
        
        output_path = self.output_dir / output_file
        
        with open(output_path, 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            
            # Write summary
            writer.writerow(['Security Test Report'])
            writer.writerow(['Generated', datetime.utcnow().isoformat()])
            writer.writerow([])
            
            # Write test results
            writer.writerow(['Test Category', 'Status', 'Issues Found', 'Region'])
            for test in test_results.get('tests', []):
                writer.writerow([
                    test.get('name', ''),
                    'PASS' if test.get('success') else 'FAIL',
                    len(test.get('issues', [])),
                    test.get('region', '')
                ])
            
            writer.writerow([])
            
            # Write vulnerabilities
            writer.writerow(['Severity', 'Title', 'Component', 'Region', 'Remediation'])
            for vuln in test_results.get('vulnerabilities', []):
                writer.writerow([
                    vuln.get('severity', ''),
                    vuln.get('title', ''),
                    vuln.get('component', ''),
                    vuln.get('region', ''),
                    vuln.get('remediation', '')
                ])
        
        logger.info(f"CSV report saved to: {output_path}")
        
        return str(output_path)
    
    def _generate_test_rows(self, tests: List[Dict]) -> str:
        """Generate HTML table rows for tests"""
        rows = []
        for test in tests:
            status = '<span class="pass">PASS</span>' if test.get('success') else '<span class="fail">FAIL</span>'
            rows.append(f"""
                <tr>
                    <td>{test.get('name', '')}</td>
                    <td>{status}</td>
                    <td>{len(test.get('issues', []))}</td>
                    <td>{test.get('region', '')}</td>
                </tr>
            """)
        return ''.join(rows)
    
    def _generate_vulnerability_rows(self, vulnerabilities: List[Dict]) -> str:
        """Generate HTML table rows for vulnerabilities"""
        rows = []
        for vuln in vulnerabilities:
            severity = vuln.get('severity', 'low')
            rows.append(f"""
                <tr class="{severity}">
                    <td>{severity.upper()}</td>
                    <td>{vuln.get('title', '')}</td>
                    <td>{vuln.get('component', '')}</td>
                    <td>{vuln.get('remediation', '')}</td>
                </tr>
            """)
        return ''.join(rows)
    
    def _generate_recommendations(self, test_results: Dict) -> str:
        """Generate HTML list of recommendations"""
        recommendations = self._get_recommendations(test_results)
        return ''.join(f"<li>{rec}</li>" for rec in recommendations)
    
    def _get_recommendations(self, test_results: Dict) -> List[str]:
        """Get recommendations based on test results"""
        recommendations = []
        
        if test_results.get('critical_vulns', 0) > 0:
            recommendations.append("⚠️ CRITICAL: Address all critical vulnerabilities immediately")
        
        if test_results.get('high_vulns', 0) > 0:
            recommendations.append("⚠️ HIGH: Remediate high-severity vulnerabilities within 7 days")
        
        if test_results.get('failed', 0) > 0:
            recommendations.append("Review and fix all failed security tests")
        
        if test_results.get('encryption_issues', 0) > 0:
            recommendations.append("Enable encryption for all data at rest and in transit")
        
        if test_results.get('compliance_issues', 0) > 0:
            recommendations.append("Address compliance violations to meet regulatory requirements")
        
        if not recommendations:
            recommendations.append("✅ All security tests passed. Continue monitoring.")
        
        return recommendations


def main():
    """Main entry point"""
    # Example usage
    test_results = {
        "total_tests": 20,
        "passed": 15,
        "failed": 5,
        "critical_vulns": 1,
        "high_vulns": 2,
        "medium_vulns": 3,
        "low_vulns": 4,
        "tests": [
            {
                "name": "Cross-Region Security",
                "success": True,
                "issues": [],
                "region": "us-east-1"
            },
            {
                "name": "Data Encryption",
                "success": False,
                "issues": ["RDS encryption not enabled"],
                "region": "us-west-2"
            }
        ],
        "vulnerabilities": [
            {
                "severity": "critical",
                "title": "Unauthenticated API Access",
                "component": "API Gateway",
                "region": "us-east-1",
                "remediation": "Implement authentication"
            }
        ]
    }
    
    generator = SecurityTestReportGenerator()
    
    # Generate all report formats
    generator.generate_html_report(test_results)
    generator.generate_json_report(test_results)
    generator.generate_csv_report(test_results)
    
    logger.info("All security reports generated successfully")


if __name__ == "__main__":
    main()
