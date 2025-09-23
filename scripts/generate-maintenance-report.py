#!/usr/bin/env python3
"""
Maintenance Report Generator
Generates comprehensive maintenance reports for viewpoint documentation.
"""

import os
import sys
import argparse
import subprocess
import json
from datetime import datetime, timedelta
from typing import Dict, List

class MaintenanceReportGenerator:
    def __init__(self, viewpoint: str):
        self.viewpoint = viewpoint
        self.report_data = {}
    
    def collect_maintenance_data(self, viewpoint_dir: str) -> Dict:
        """Collect all maintenance-related data."""
        print(f"Collecting maintenance data for {self.viewpoint} viewpoint...")
        
        data = {
            'timestamp': datetime.now().isoformat(),
            'viewpoint': self.viewpoint,
            'directory': viewpoint_dir,
            'maintenance_status': {},
            'recommendations': []
        }
        
        # 1. Run structure validation
        print("1. Checking structure validation...")
        try:
            result = subprocess.run([
                'python3', 'scripts/validate-viewpoint-structure.py',
                '--viewpoint', self.viewpoint
            ], capture_output=True, text=True, timeout=60)
            
            data['maintenance_status']['structure_validation'] = {
                'status': 'passed' if result.returncode == 0 else 'failed',
                'return_code': result.returncode,
                'summary': self._extract_summary(result.stdout)
            }
        except Exception as e:
            data['maintenance_status']['structure_validation'] = {
                'status': 'error',
                'error': str(e)
            }
        
        # 2. Check for outdated content
        print("2. Checking for outdated content...")
        try:
            result = subprocess.run([
                'python3', 'scripts/detect-outdated-content.py',
                '--viewpoint', self.viewpoint,
                '--threshold', '30'
            ], capture_output=True, text=True, timeout=120)
            
            data['maintenance_status']['outdated_content'] = {
                'status': 'passed' if result.returncode == 0 else 'needs_attention',
                'return_code': result.returncode,
                'summary': self._extract_summary(result.stdout)
            }
        except Exception as e:
            data['maintenance_status']['outdated_content'] = {
                'status': 'error',
                'error': str(e)
            }
        
        # 3. Monitor documentation usage
        print("3. Monitoring documentation usage...")
        try:
            result = subprocess.run([
                'python3', 'scripts/monitor-documentation-usage.py',
                '--viewpoint', self.viewpoint
            ], capture_output=True, text=True, timeout=90)
            
            data['maintenance_status']['usage_monitoring'] = {
                'status': 'completed',
                'return_code': result.returncode,
                'summary': self._extract_summary(result.stdout)
            }
        except Exception as e:
            data['maintenance_status']['usage_monitoring'] = {
                'status': 'error',
                'error': str(e)
            }
        
        # 4. Check content quality
        print("4. Checking content quality...")
        try:
            result = subprocess.run([
                'python3', 'scripts/assess-documentation-quality.py',
                '--viewpoint', self.viewpoint
            ], capture_output=True, text=True, timeout=120)
            
            data['maintenance_status']['content_quality'] = {
                'status': 'passed' if result.returncode == 0 else 'needs_improvement',
                'return_code': result.returncode,
                'summary': self._extract_summary(result.stdout)
            }
        except Exception as e:
            data['maintenance_status']['content_quality'] = {
                'status': 'error',
                'error': str(e)
            }
        
        # 5. Generate recommendations
        data['recommendations'] = self._generate_recommendations(data['maintenance_status'])
        
        return data
    
    def _extract_summary(self, output: str) -> str:
        """Extract key summary information from command output."""
        if not output:
            return "No output"
        
        lines = output.split('\n')
        summary_lines = []
        
        # Look for key indicators
        for line in lines:
            line = line.strip()
            if any(keyword in line.lower() for keyword in ['summary', 'total', 'found', 'score', 'grade', 'status']):
                summary_lines.append(line)
            elif line.startswith('✅') or line.startswith('❌') or line.startswith('⚠️'):
                summary_lines.append(line)
        
        if summary_lines:
            return ' | '.join(summary_lines[:3])  # First 3 relevant lines
        else:
            # Fallback to first few lines
            return ' | '.join(lines[:2])
    
    def _generate_recommendations(self, maintenance_status: Dict) -> List[Dict]:
        """Generate maintenance recommendations based on status."""
        recommendations = []
        
        for check_name, check_data in maintenance_status.items():
            status = check_data.get('status', 'unknown')
            
            if status == 'failed':
                recommendations.append({
                    'priority': 'high',
                    'category': check_name,
                    'action': f'Fix {check_name.replace("_", " ")} issues immediately',
                    'description': f'Critical issues found in {check_name.replace("_", " ")} that require immediate attention'
                })
            elif status == 'needs_attention' or status == 'needs_improvement':
                recommendations.append({
                    'priority': 'medium',
                    'category': check_name,
                    'action': f'Improve {check_name.replace("_", " ")}',
                    'description': f'Issues found in {check_name.replace("_", " ")} that should be addressed'
                })
            elif status == 'error':
                recommendations.append({
                    'priority': 'high',
                    'category': check_name,
                    'action': f'Fix {check_name.replace("_", " ")} monitoring',
                    'description': f'Error occurred while checking {check_name.replace("_", " ")} - investigate and fix'
                })
        
        # Add general maintenance recommendations
        recommendations.extend([
            {
                'priority': 'low',
                'category': 'general',
                'action': 'Schedule regular maintenance',
                'description': 'Set up automated weekly maintenance tasks'
            },
            {
                'priority': 'low',
                'category': 'general',
                'action': 'Monitor performance trends',
                'description': 'Track documentation performance metrics over time'
            }
        ])
        
        return recommendations
    
    def calculate_maintenance_score(self, maintenance_status: Dict) -> Dict:
        """Calculate overall maintenance score."""
        total_checks = len(maintenance_status)
        if total_checks == 0:
            return {'score': 0, 'grade': 'F', 'status': 'critical'}
        
        passed_checks = sum(1 for check in maintenance_status.values() if check.get('status') == 'passed')
        completed_checks = sum(1 for check in maintenance_status.values() if check.get('status') == 'completed')
        
        score = ((passed_checks + completed_checks) / total_checks) * 100
        
        if score >= 90:
            grade = 'A'
            status = 'excellent'
        elif score >= 80:
            grade = 'B'
            status = 'good'
        elif score >= 70:
            grade = 'C'
            status = 'satisfactory'
        elif score >= 60:
            grade = 'D'
            status = 'needs_improvement'
        else:
            grade = 'F'
            status = 'critical'
        
        return {
            'score': round(score, 1),
            'grade': grade,
            'status': status,
            'total_checks': total_checks,
            'passed_checks': passed_checks,
            'completed_checks': completed_checks
        }
    
    def generate_maintenance_report(self, data: Dict, output_file: str):
        """Generate comprehensive maintenance report."""
        maintenance_score = self.calculate_maintenance_score(data['maintenance_status'])
        
        report_lines = [
            f"# Daily Maintenance Report - {self.viewpoint.title()} Viewpoint",
            f"Generated: {data['timestamp']}",
            f"Directory: `{data['directory']}`",
            "",
            "## Executive Summary",
            f"- **Maintenance Score**: {maintenance_score['score']}/100 (Grade: {maintenance_score['grade']})",
            f"- **Status**: {maintenance_score['status'].replace('_', ' ').title()}",
            f"- **Checks Completed**: {maintenance_score['passed_checks'] + maintenance_score['completed_checks']}/{maintenance_score['total_checks']}",
            ""
        ]
        
        # Status indicator
        if maintenance_score['status'] == 'excellent':
            status_icon = "✅"
            status_message = "All systems operating normally"
        elif maintenance_score['status'] == 'good':
            status_icon = "🟢"
            status_message = "Minor issues detected, monitoring recommended"
        elif maintenance_score['status'] == 'satisfactory':
            status_icon = "🟡"
            status_message = "Some issues require attention"
        elif maintenance_score['status'] == 'needs_improvement':
            status_icon = "🟠"
            status_message = "Multiple issues need addressing"
        else:
            status_icon = "🔴"
            status_message = "Critical issues require immediate action"
        
        report_lines.extend([
            f"**Overall Status**: {status_icon} {status_message}",
            ""
        ])
        
        # Maintenance checks dashboard
        report_lines.extend([
            "## Maintenance Checks Dashboard",
            "",
            "| Check | Status | Summary |",
            "|-------|--------|---------|"
        ])
        
        for check_name, check_data in data['maintenance_status'].items():
            status = check_data.get('status', 'unknown')
            summary = check_data.get('summary', 'No summary available')
            
            status_icons = {
                'passed': '✅',
                'completed': '✅',
                'failed': '❌',
                'needs_attention': '⚠️',
                'needs_improvement': '⚠️',
                'error': '🔥'
            }
            
            status_icon = status_icons.get(status, '❓')
            check_display = check_name.replace('_', ' ').title()
            
            report_lines.append(f"| {check_display} | {status_icon} {status.title()} | {summary[:100]}{'...' if len(summary) > 100 else ''} |")
        
        report_lines.extend([
            "",
            "## Detailed Results",
            ""
        ])
        
        # Detailed results for each check
        for check_name, check_data in data['maintenance_status'].items():
            check_display = check_name.replace('_', ' ').title()
            status = check_data.get('status', 'unknown')
            
            report_lines.extend([
                f"### {check_display}",
                f"**Status**: {status.title()}",
                f"**Return Code**: {check_data.get('return_code', 'N/A')}",
                ""
            ])
            
            if check_data.get('error'):
                report_lines.extend([
                    "**Error Details**:",
                    f"```",
                    check_data['error'],
                    f"```",
                    ""
                ])
            elif check_data.get('summary'):
                report_lines.extend([
                    "**Summary**:",
                    check_data['summary'],
                    ""
                ])
            
            report_lines.append("---")
            report_lines.append("")
        
        # Recommendations
        if data['recommendations']:
            report_lines.extend([
                "## Maintenance Recommendations",
                ""
            ])
            
            # Group by priority
            high_priority = [r for r in data['recommendations'] if r['priority'] == 'high']
            medium_priority = [r for r in data['recommendations'] if r['priority'] == 'medium']
            low_priority = [r for r in data['recommendations'] if r['priority'] == 'low']
            
            if high_priority:
                report_lines.extend([
                    "### 🔴 High Priority",
                    ""
                ])
                for i, rec in enumerate(high_priority, 1):
                    report_lines.extend([
                        f"{i}. **{rec['action']}**",
                        f"   - Category: {rec['category'].replace('_', ' ').title()}",
                        f"   - Description: {rec['description']}",
                        ""
                    ])
            
            if medium_priority:
                report_lines.extend([
                    "### 🟡 Medium Priority",
                    ""
                ])
                for i, rec in enumerate(medium_priority, 1):
                    report_lines.extend([
                        f"{i}. **{rec['action']}**",
                        f"   - Category: {rec['category'].replace('_', ' ').title()}",
                        f"   - Description: {rec['description']}",
                        ""
                    ])
            
            if low_priority:
                report_lines.extend([
                    "### 🟢 Low Priority",
                    ""
                ])
                for i, rec in enumerate(low_priority, 1):
                    report_lines.extend([
                        f"{i}. **{rec['action']}**",
                        f"   - Category: {rec['category'].replace('_', ' ').title()}",
                        f"   - Description: {rec['description']}",
                        ""
                    ])
        
        # Next steps
        report_lines.extend([
            "## Next Steps",
            ""
        ])
        
        if maintenance_score['status'] in ['critical', 'needs_improvement']:
            report_lines.extend([
                "### Immediate Actions Required",
                "1. Address all high-priority recommendations",
                "2. Investigate and fix any errors",
                "3. Schedule follow-up maintenance within 24 hours",
                ""
            ])
        elif maintenance_score['status'] == 'satisfactory':
            report_lines.extend([
                "### Actions Recommended",
                "1. Address medium-priority recommendations",
                "2. Monitor trends over next few days",
                "3. Schedule next maintenance check in 3 days",
                ""
            ])
        else:
            report_lines.extend([
                "### Routine Maintenance",
                "1. Continue regular monitoring",
                "2. Address low-priority recommendations when convenient",
                "3. Schedule next maintenance check in 1 week",
                ""
            ])
        
        report_lines.extend([
            "## Maintenance Schedule",
            "",
            "- **Daily**: Automated maintenance checks",
            "- **Weekly**: Comprehensive quality review",
            "- **Monthly**: Structure and organization audit",
            "- **Quarterly**: Performance optimization review",
            ""
        ])
        
        report_content = '\n'.join(report_lines)
        
        # Save report
        os.makedirs(os.path.dirname(output_file), exist_ok=True)
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(report_content)
        
        # Save raw data as JSON
        json_file = output_file.replace('.md', '.json')
        with open(json_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2)
        
        print(f"Maintenance report saved to: {output_file}")
        print(f"Raw data saved to: {json_file}")
        
        return maintenance_score['score']

def main():
    parser = argparse.ArgumentParser(description='Generate maintenance report for viewpoint documentation')
    parser.add_argument('--viewpoint', required=True, help='Viewpoint to report on (e.g., development)')
    parser.add_argument('--output', help='Output directory for report')
    
    args = parser.parse_args()
    
    viewpoint_dir = f"docs/viewpoints/{args.viewpoint}"
    if not os.path.exists(viewpoint_dir):
        print(f"Error: Viewpoint directory '{viewpoint_dir}' does not exist")
        sys.exit(1)
    
    output_dir = args.output or "reports-summaries/task-execution"
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_file = os.path.join(output_dir, f"daily-maintenance-report-{args.viewpoint}-{timestamp}.md")
    
    generator = MaintenanceReportGenerator(args.viewpoint)
    data = generator.collect_maintenance_data(viewpoint_dir)
    score = generator.generate_maintenance_report(data, output_file)
    
    # Exit with appropriate code
    if score < 60:
        print(f"\n❌ Maintenance score is critical: {score}/100")
        sys.exit(2)
    elif score < 80:
        print(f"\n⚠️  Maintenance score needs attention: {score}/100")
        sys.exit(1)
    else:
        print(f"\n✅ Maintenance score is good: {score}/100")
        sys.exit(0)

if __name__ == '__main__':
    main()