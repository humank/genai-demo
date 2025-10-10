#!/usr/bin/env python3
"""
Performance Trend Analysis Script

This script analyzes performance trends over time by comparing historical
test results and identifying patterns, improvements, and regressions.
"""

import argparse
import json
import logging
import statistics
import sys
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
from scipy import stats

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class PerformanceTrendAnalyzer:
    """
    Analyzes performance trends over time and generates insights.
    """
    
    def __init__(self, results_directory: Path):
        self.results_directory = Path(results_directory)
        self.historical_data: List[Dict[str, Any]] = []
        self.trend_analysis: Dict[str, Any] = {}
    
    def load_historical_data(self, test_scenario: str, days_back: int = 30) -> List[Dict[str, Any]]:
        """Load historical performance data for analysis."""
        logger.info(f"Loading historical data for {test_scenario} (last {days_back} days)")
        
        historical_data = []
        cutoff_date = datetime.now() - timedelta(days=days_back)
        
        # Search for result files in the results directory
        pattern = f"*{test_scenario}*results.json"
        result_files = list(self.results_directory.glob(f"**/{pattern}"))
        
        for result_file in result_files:
            try:
                # Extract timestamp from directory name or file modification time
                timestamp = self._extract_timestamp(result_file)
                
                if timestamp and timestamp >= cutoff_date:
                    with open(result_file, 'r') as f:
                        data = json.load(f)
                        data['timestamp'] = timestamp.isoformat()
                        data['file_path'] = str(result_file)
                        historical_data.append(data)
            except Exception as e:
                logger.warning(f"Failed to load {result_file}: {e}")
        
        # Sort by timestamp
        historical_data.sort(key=lambda x: x['timestamp'])
        
        logger.info(f"Loaded {len(historical_data)} historical data points")
        self.historical_data = historical_data
        return historical_data
    
    def _extract_timestamp(self, file_path: Path) -> Optional[datetime]:
        """Extract timestamp from file path or modification time."""
        # Try to extract from parent directory name (format: YYYYMMDD_HHMMSS)
        parent_name = file_path.parent.name
        try:
            return datetime.strptime(parent_name, "%Y%m%d_%H%M%S")
        except ValueError:
            pass
        
        # Fall back to file modification time
        try:
            return datetime.fromtimestamp(file_path.stat().st_mtime)
        except Exception:
            return None
    
    def analyze_trends(self, metrics: List[str] = None) -> Dict[str, Any]:
        """Analyze performance trends for specified metrics."""
        if not self.historical_data:
            logger.error("No historical data available for trend analysis")
            return {}
        
        if metrics is None:
            metrics = ['mean_response_time', 'p95_response_time', 'throughput', 'success_rate']
        
        logger.info(f"Analyzing trends for metrics: {metrics}")
        
        trend_analysis = {
            'analysis_timestamp': datetime.now().isoformat(),
            'data_points': len(self.historical_data),
            'time_range': {
                'start': self.historical_data[0]['timestamp'],
                'end': self.historical_data[-1]['timestamp']
            },
            'metrics_analysis': {},
            'overall_trends': {},
            'anomalies': [],
            'recommendations': []
        }
        
        for metric in metrics:
            metric_analysis = self._analyze_metric_trend(metric)
            if metric_analysis:
                trend_analysis['metrics_analysis'][metric] = metric_analysis
        
        # Generate overall trend summary
        trend_analysis['overall_trends'] = self._generate_overall_trends()
        
        # Detect anomalies
        trend_analysis['anomalies'] = self._detect_anomalies()
        
        # Generate recommendations
        trend_analysis['recommendations'] = self._generate_recommendations()
        
        self.trend_analysis = trend_analysis
        return trend_analysis
    
    def _analyze_metric_trend(self, metric: str) -> Optional[Dict[str, Any]]:
        """Analyze trend for a specific metric."""
        values = []
        timestamps = []
        
        for data_point in self.historical_data:
            value = self._extract_metric_value(data_point, metric)
            if value is not None:
                values.append(value)
                timestamps.append(datetime.fromisoformat(data_point['timestamp']))
        
        if len(values) < 2:
            logger.warning(f"Insufficient data points for {metric} trend analysis")
            return None
        
        # Convert timestamps to numeric values for regression
        timestamp_numeric = [(ts - timestamps[0]).total_seconds() for ts in timestamps]
        
        # Perform linear regression
        slope, intercept, r_value, p_value, std_err = stats.linregress(timestamp_numeric, values)
        
        # Calculate trend statistics
        trend_analysis = {
            'metric_name': metric,
            'data_points': len(values),
            'current_value': values[-1],
            'first_value': values[0],
            'min_value': min(values),
            'max_value': max(values),
            'mean_value': statistics.mean(values),
            'median_value': statistics.median(values),
            'std_deviation': statistics.stdev(values) if len(values) > 1 else 0,
            'trend': {
                'slope': slope,
                'direction': 'improving' if self._is_improving_trend(metric, slope) else 'degrading' if slope != 0 else 'stable',
                'strength': abs(r_value),
                'confidence': 1 - p_value if p_value < 0.05 else 0,
                'r_squared': r_value ** 2
            },
            'change_percentage': ((values[-1] - values[0]) / values[0] * 100) if values[0] != 0 else 0,
            'volatility': statistics.stdev(values) / statistics.mean(values) if statistics.mean(values) != 0 else 0
        }
        
        # Detect trend significance
        if p_value < 0.05:
            trend_analysis['trend']['significance'] = 'significant'
        elif p_value < 0.1:
            trend_analysis['trend']['significance'] = 'moderate'
        else:
            trend_analysis['trend']['significance'] = 'not_significant'
        
        return trend_analysis
    
    def _extract_metric_value(self, data_point: Dict[str, Any], metric: str) -> Optional[float]:
        """Extract metric value from data point."""
        summary = data_point.get('summary', {})
        
        if metric == 'mean_response_time':
            return summary.get('response_time', {}).get('mean')
        elif metric == 'p95_response_time':
            return summary.get('response_time', {}).get('p95')
        elif metric == 'p99_response_time':
            return summary.get('response_time', {}).get('p99')
        elif metric == 'throughput':
            return summary.get('request_rate')
        elif metric == 'success_rate':
            return summary.get('success_rate')
        elif metric == 'total_requests':
            return summary.get('total_requests')
        elif metric == 'failed_requests':
            return summary.get('failed_requests')
        else:
            # Try to find metric in nested structure
            return self._find_nested_value(summary, metric)
    
    def _find_nested_value(self, data: Dict[str, Any], key: str) -> Optional[float]:
        """Find value in nested dictionary structure."""
        if isinstance(data, dict):
            if key in data:
                return data[key]
            for value in data.values():
                result = self._find_nested_value(value, key)
                if result is not None:
                    return result
        return None
    
    def _is_improving_trend(self, metric: str, slope: float) -> bool:
        """Determine if trend direction is improving based on metric type."""
        # For these metrics, lower values are better
        lower_is_better = ['mean_response_time', 'p95_response_time', 'p99_response_time', 'failed_requests']
        
        # For these metrics, higher values are better
        higher_is_better = ['throughput', 'success_rate', 'total_requests']
        
        if metric in lower_is_better:
            return slope < 0  # Decreasing trend is improving
        elif metric in higher_is_better:
            return slope > 0  # Increasing trend is improving
        else:
            return False  # Unknown metric, assume no improvement
    
    def _generate_overall_trends(self) -> Dict[str, Any]:
        """Generate overall trend summary across all metrics."""
        if not self.trend_analysis.get('metrics_analysis'):
            return {}
        
        improving_metrics = []
        degrading_metrics = []
        stable_metrics = []
        
        for metric, analysis in self.trend_analysis['metrics_analysis'].items():
            direction = analysis['trend']['direction']
            if direction == 'improving':
                improving_metrics.append(metric)
            elif direction == 'degrading':
                degrading_metrics.append(metric)
            else:
                stable_metrics.append(metric)
        
        # Calculate overall performance score
        total_metrics = len(self.trend_analysis['metrics_analysis'])
        performance_score = (len(improving_metrics) * 2 + len(stable_metrics)) / (total_metrics * 2) * 100
        
        return {
            'improving_metrics': improving_metrics,
            'degrading_metrics': degrading_metrics,
            'stable_metrics': stable_metrics,
            'performance_score': performance_score,
            'overall_direction': self._determine_overall_direction(improving_metrics, degrading_metrics, stable_metrics)
        }
    
    def _determine_overall_direction(self, improving: List[str], degrading: List[str], stable: List[str]) -> str:
        """Determine overall performance direction."""
        if len(improving) > len(degrading):
            return 'improving'
        elif len(degrading) > len(improving):
            return 'degrading'
        else:
            return 'stable'
    
    def _detect_anomalies(self) -> List[Dict[str, Any]]:
        """Detect performance anomalies in the data."""
        anomalies = []
        
        for metric, analysis in self.trend_analysis.get('metrics_analysis', {}).items():
            # High volatility anomaly
            if analysis['volatility'] > 0.3:  # 30% coefficient of variation
                anomalies.append({
                    'type': 'high_volatility',
                    'metric': metric,
                    'severity': 'medium',
                    'description': f'High volatility detected in {metric} (CV: {analysis["volatility"]:.2f})',
                    'recommendation': f'Investigate causes of {metric} variability'
                })
            
            # Significant degradation anomaly
            if analysis['trend']['direction'] == 'degrading' and analysis['trend']['significance'] == 'significant':
                severity = 'high' if abs(analysis['change_percentage']) > 20 else 'medium'
                anomalies.append({
                    'type': 'performance_degradation',
                    'metric': metric,
                    'severity': severity,
                    'description': f'Significant degradation in {metric} ({analysis["change_percentage"]:.1f}% change)',
                    'recommendation': f'Urgent investigation required for {metric} degradation'
                })
        
        return anomalies
    
    def _generate_recommendations(self) -> List[Dict[str, Any]]:
        """Generate recommendations based on trend analysis."""
        recommendations = []
        
        overall_trends = self.trend_analysis.get('overall_trends', {})
        degrading_metrics = overall_trends.get('degrading_metrics', [])
        
        if degrading_metrics:
            recommendations.append({
                'category': 'performance_degradation',
                'priority': 'high',
                'title': 'Address Performance Degradation',
                'description': f'Multiple metrics showing degradation: {", ".join(degrading_metrics)}',
                'actions': [
                    'Conduct root cause analysis for degrading metrics',
                    'Review recent code changes and deployments',
                    'Check system resource utilization trends',
                    'Validate test environment consistency'
                ]
            })
        
        # Check for high volatility
        high_volatility_metrics = []
        for metric, analysis in self.trend_analysis.get('metrics_analysis', {}).items():
            if analysis['volatility'] > 0.2:
                high_volatility_metrics.append(metric)
        
        if high_volatility_metrics:
            recommendations.append({
                'category': 'stability',
                'priority': 'medium',
                'title': 'Improve Performance Stability',
                'description': f'High variability in: {", ".join(high_volatility_metrics)}',
                'actions': [
                    'Implement consistent test data and environment setup',
                    'Review system resource allocation and scaling',
                    'Add performance monitoring and alerting',
                    'Consider load balancing and caching improvements'
                ]
            })
        
        # Performance score recommendations
        performance_score = overall_trends.get('performance_score', 0)
        if performance_score < 70:
            recommendations.append({
                'category': 'overall_performance',
                'priority': 'high',
                'title': 'Comprehensive Performance Improvement',
                'description': f'Overall performance score is low ({performance_score:.1f}/100)',
                'actions': [
                    'Conduct comprehensive performance audit',
                    'Implement performance optimization roadmap',
                    'Establish performance baselines and monitoring',
                    'Consider infrastructure scaling and optimization'
                ]
            })
        
        return recommendations
    
    def generate_trend_report(self, output_file: Optional[Path] = None) -> str:
        """Generate comprehensive trend analysis report."""
        if not self.trend_analysis:
            logger.error("No trend analysis available. Run analyze_trends() first.")
            return ""
        
        if output_file is None:
            output_file = Path(f"staging-tests/reports/trend-analysis-{datetime.now().strftime('%Y%m%d_%H%M%S')}.md")
        
        output_file.parent.mkdir(parents=True, exist_ok=True)
        
        # Generate report content
        content = self._generate_report_content()
        
        # Save report
        with open(output_file, 'w') as f:
            f.write(content)
        
        logger.info(f"Trend analysis report saved to {output_file}")
        return str(output_file)
    
    def _generate_report_content(self) -> str:
        """Generate the content for the trend analysis report."""
        analysis = self.trend_analysis
        
        content = f"""# Performance Trend Analysis Report

## Executive Summary

**Generated**: {analysis['analysis_timestamp']}
**Data Points**: {analysis['data_points']}
**Time Range**: {analysis['time_range']['start']} to {analysis['time_range']['end']}
**Overall Performance Score**: {analysis['overall_trends'].get('performance_score', 0):.1f}/100

### Key Findings

"""
        
        overall_trends = analysis['overall_trends']
        if overall_trends.get('improving_metrics'):
            content += f"✅ **Improving Metrics**: {', '.join(overall_trends['improving_metrics'])}\\n"
        
        if overall_trends.get('degrading_metrics'):
            content += f"⚠️ **Degrading Metrics**: {', '.join(overall_trends['degrading_metrics'])}\\n"
        
        if overall_trends.get('stable_metrics'):
            content += f"➡️ **Stable Metrics**: {', '.join(overall_trends['stable_metrics'])}\\n"
        
        content += f"\\n**Overall Direction**: {overall_trends.get('overall_direction', 'unknown').title()}\\n\\n"
        
        # Metrics Analysis
        content += "## Detailed Metrics Analysis\\n\\n"
        
        for metric, metric_analysis in analysis['metrics_analysis'].items():
            content += f"### {metric.replace('_', ' ').title()}\\n\\n"
            content += f"- **Current Value**: {metric_analysis['current_value']:.2f}\\n"
            content += f"- **Trend Direction**: {metric_analysis['trend']['direction'].title()}\\n"
            content += f"- **Change**: {metric_analysis['change_percentage']:.1f}%\\n"
            content += f"- **Volatility**: {metric_analysis['volatility']:.2f}\\n"
            content += f"- **Trend Strength**: {metric_analysis['trend']['strength']:.2f}\\n"
            content += f"- **Statistical Significance**: {metric_analysis['trend']['significance'].replace('_', ' ').title()}\\n\\n"
        
        # Anomalies
        if analysis['anomalies']:
            content += "## Detected Anomalies\\n\\n"
            for anomaly in analysis['anomalies']:
                content += f"### {anomaly['type'].replace('_', ' ').title()} ({anomaly['severity'].title()} Severity)\\n\\n"
                content += f"**Metric**: {anomaly['metric']}\\n"
                content += f"**Description**: {anomaly['description']}\\n"
                content += f"**Recommendation**: {anomaly['recommendation']}\\n\\n"
        
        # Recommendations
        if analysis['recommendations']:
            content += "## Recommendations\\n\\n"
            for i, rec in enumerate(analysis['recommendations'], 1):
                content += f"### {i}. {rec['title']} ({rec['priority'].title()} Priority)\\n\\n"
                content += f"**Category**: {rec['category'].replace('_', ' ').title()}\\n"
                content += f"**Description**: {rec['description']}\\n\\n"
                content += "**Recommended Actions**:\\n"
                for action in rec['actions']:
                    content += f"- {action}\\n"
                content += "\\n"
        
        return content
    
    def generate_trend_charts(self, output_dir: Optional[Path] = None) -> List[str]:
        """Generate trend charts for visualization."""
        if not self.historical_data:
            logger.error("No historical data available for chart generation")
            return []
        
        if output_dir is None:
            output_dir = Path("staging-tests/reports/charts")
        
        output_dir.mkdir(parents=True, exist_ok=True)
        
        chart_files = []
        
        # Prepare data for plotting
        timestamps = [datetime.fromisoformat(dp['timestamp']) for dp in self.historical_data]
        
        metrics_to_plot = ['mean_response_time', 'p95_response_time', 'throughput', 'success_rate']
        
        for metric in metrics_to_plot:
            values = []
            valid_timestamps = []
            
            for i, data_point in enumerate(self.historical_data):
                value = self._extract_metric_value(data_point, metric)
                if value is not None:
                    values.append(value)
                    valid_timestamps.append(timestamps[i])
            
            if len(values) < 2:
                continue
            
            # Create chart
            plt.figure(figsize=(12, 6))
            plt.plot(valid_timestamps, values, marker='o', linewidth=2, markersize=4)
            
            # Add trend line
            if len(values) > 2:
                timestamp_numeric = [(ts - valid_timestamps[0]).total_seconds() for ts in valid_timestamps]
                slope, intercept, _, _, _ = stats.linregress(timestamp_numeric, values)
                trend_line = [slope * x + intercept for x in timestamp_numeric]
                plt.plot(valid_timestamps, trend_line, '--', alpha=0.7, color='red', label='Trend')
            
            plt.title(f'{metric.replace("_", " ").title()} Trend Over Time')
            plt.xlabel('Time')
            plt.ylabel(metric.replace('_', ' ').title())
            plt.xticks(rotation=45)
            plt.grid(True, alpha=0.3)
            plt.legend()
            plt.tight_layout()
            
            # Save chart
            chart_file = output_dir / f'{metric}_trend.png'
            plt.savefig(chart_file, dpi=300, bbox_inches='tight')
            plt.close()
            
            chart_files.append(str(chart_file))
            logger.info(f"Generated chart: {chart_file}")
        
        return chart_files

def main():
    """Main entry point for the trend analysis script."""
    parser = argparse.ArgumentParser(description="Performance Trend Analysis")
    parser.add_argument("--results-dir", required=True,
                       help="Directory containing historical test results")
    parser.add_argument("--test-scenario", required=True,
                       help="Test scenario to analyze (e.g., normal-load, peak-load)")
    parser.add_argument("--days-back", type=int, default=30,
                       help="Number of days of history to analyze (default: 30)")
    parser.add_argument("--metrics", nargs='+',
                       default=['mean_response_time', 'p95_response_time', 'throughput', 'success_rate'],
                       help="Metrics to analyze")
    parser.add_argument("--output-report",
                       help="Output file for trend analysis report")
    parser.add_argument("--generate-charts", action="store_true",
                       help="Generate trend charts")
    parser.add_argument("--charts-dir",
                       help="Directory for chart output")
    parser.add_argument("--verbose", "-v", action="store_true",
                       help="Enable verbose logging")
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    try:
        # Create analyzer
        analyzer = PerformanceTrendAnalyzer(args.results_dir)
        
        # Load historical data
        historical_data = analyzer.load_historical_data(args.test_scenario, args.days_back)
        
        if not historical_data:
            logger.error(f"No historical data found for {args.test_scenario}")
            sys.exit(1)
        
        # Analyze trends
        trend_analysis = analyzer.analyze_trends(args.metrics)
        
        # Generate report
        report_file = analyzer.generate_trend_report(
            Path(args.output_report) if args.output_report else None
        )
        
        # Generate charts if requested
        if args.generate_charts:
            charts_dir = Path(args.charts_dir) if args.charts_dir else None
            chart_files = analyzer.generate_trend_charts(charts_dir)
            print(f"Generated {len(chart_files)} trend charts")
        
        # Print summary
        overall_trends = trend_analysis['overall_trends']
        print(f"\\nTrend Analysis Summary:")
        print(f"Performance Score: {overall_trends['performance_score']:.1f}/100")
        print(f"Overall Direction: {overall_trends['overall_direction'].title()}")
        print(f"Report: {report_file}")
        
        # Exit with appropriate code
        if overall_trends['performance_score'] < 60:
            sys.exit(1)  # Poor performance trends
        elif len(overall_trends.get('degrading_metrics', [])) > 0:
            sys.exit(2)  # Some degradation detected
        else:
            sys.exit(0)  # Good trends
    
    except Exception as e:
        logger.error(f"Failed to analyze trends: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()