#!/usr/bin/env python3
"""
Gatling Performance Report Generator

This script generates consolidated performance reports from Gatling test results,
combining multiple test runs and system metrics into comprehensive HTML reports.
"""

import argparse
import json
import logging
import os
import re
import sys
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Any
import jinja2
import pandas as pd
import plotly.graph_objects as go
import plotly.express as px
from plotly.subplots import make_subplots
import plotly.offline as pyo

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class GatlingReportGenerator:
    """
    Generates consolidated performance reports from Gatling results and system metrics.
    """
    
    def __init__(self, results_dir: Path, output_file: Path):
        self.results_dir = Path(results_dir)
        self.output_file = Path(output_file)
        self.test_results: List[Dict[str, Any]] = []
        self.system_metrics: Optional[Dict[str, Any]] = None
        
        # Ensure output directory exists
        self.output_file.parent.mkdir(parents=True, exist_ok=True)
    
    def parse_gatling_results(self) -> List[Dict[str, Any]]:
        """Parse Gatling simulation.log files to extract performance data."""
        results = []
        
        # Find all Gatling result directories
        for result_dir in self.results_dir.iterdir():
            if not result_dir.is_dir():
                continue
            
            simulation_log = result_dir / "simulation.log"
            if not simulation_log.exists():
                continue
            
            logger.info(f"Parsing Gatling results from {result_dir}")
            
            try:
                test_data = self._parse_simulation_log(simulation_log)
                test_data["test_name"] = result_dir.name
                test_data["result_dir"] = str(result_dir)
                results.append(test_data)
            except Exception as e:
                logger.error(f"Error parsing {simulation_log}: {e}")
        
        return results
    
    def _parse_simulation_log(self, log_file: Path) -> Dict[str, Any]:
        """Parse a single Gatling simulation.log file."""
        requests = []
        users = []
        groups = []
        
        with open(log_file, 'r') as f:
            for line in f:
                parts = line.strip().split('\t')
                if len(parts) < 3:
                    continue
                
                record_type = parts[0]
                
                if record_type == "REQUEST":
                    # REQUEST	scenario	userId	requestName	start	end	status	message
                    if len(parts) >= 8:
                        requests.append({
                            "scenario": parts[1],
                            "user_id": parts[2],
                            "request_name": parts[3],
                            "start_time": int(parts[4]),
                            "end_time": int(parts[5]),
                            "status": parts[6],
                            "message": parts[7] if len(parts) > 7 else "",
                            "response_time": int(parts[5]) - int(parts[4])
                        })
                
                elif record_type == "USER":
                    # USER	scenario	userId	START/END	timestamp
                    if len(parts) >= 5:
                        users.append({
                            "scenario": parts[1],
                            "user_id": parts[2],
                            "action": parts[3],
                            "timestamp": int(parts[4])
                        })
                
                elif record_type == "GROUP":
                    # GROUP	scenario	userId	groupName	start	end	status
                    if len(parts) >= 7:
                        groups.append({
                            "scenario": parts[1],
                            "user_id": parts[2],
                            "group_name": parts[3],
                            "start_time": int(parts[4]),
                            "end_time": int(parts[5]),
                            "status": parts[6],
                            "duration": int(parts[5]) - int(parts[4])
                        })
        
        return {
            "requests": requests,
            "users": users,
            "groups": groups,
            "summary": self._calculate_summary_stats(requests)
        }
    
    def _calculate_summary_stats(self, requests: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Calculate summary statistics from request data."""
        if not requests:
            return {}
        
        response_times = [req["response_time"] for req in requests]
        successful_requests = [req for req in requests if req["status"] == "OK"]
        failed_requests = [req for req in requests if req["status"] != "OK"]
        
        # Calculate percentiles
        response_times.sort()
        n = len(response_times)
        
        def percentile(data, p):
            if not data:
                return 0
            k = (len(data) - 1) * p / 100
            f = int(k)
            c = k - f
            if f == len(data) - 1:
                return data[f]
            return data[f] * (1 - c) + data[f + 1] * c
        
        # Calculate request rate
        if requests:
            start_time = min(req["start_time"] for req in requests)
            end_time = max(req["end_time"] for req in requests)
            duration_seconds = (end_time - start_time) / 1000
            request_rate = len(requests) / duration_seconds if duration_seconds > 0 else 0
        else:
            request_rate = 0
        
        return {
            "total_requests": len(requests),
            "successful_requests": len(successful_requests),
            "failed_requests": len(failed_requests),
            "success_rate": (len(successful_requests) / len(requests)) * 100 if requests else 0,
            "response_time": {
                "min": min(response_times) if response_times else 0,
                "max": max(response_times) if response_times else 0,
                "mean": sum(response_times) / len(response_times) if response_times else 0,
                "p50": percentile(response_times, 50),
                "p75": percentile(response_times, 75),
                "p95": percentile(response_times, 95),
                "p99": percentile(response_times, 99)
            },
            "request_rate": request_rate,
            "duration_seconds": duration_seconds if 'duration_seconds' in locals() else 0
        }
    
    def load_system_metrics(self, metrics_file: Optional[Path] = None) -> Optional[Dict[str, Any]]:
        """Load system metrics from monitoring data."""
        if metrics_file is None:
            # Look for system metrics in the results directory
            metrics_file = self.results_dir / "system-metrics.json"
        
        if not metrics_file.exists():
            logger.warning(f"System metrics file not found: {metrics_file}")
            return None
        
        try:
            with open(metrics_file, 'r') as f:
                return json.load(f)
        except Exception as e:
            logger.error(f"Error loading system metrics: {e}")
            return None
    
    def generate_charts(self) -> Dict[str, str]:
        """Generate performance charts using Plotly."""
        charts = {}
        
        if not self.test_results:
            return charts
        
        try:
            # Response time comparison chart
            charts["response_time_comparison"] = self._create_response_time_chart()
            
            # Throughput comparison chart
            charts["throughput_comparison"] = self._create_throughput_chart()
            
            # Success rate comparison chart
            charts["success_rate_comparison"] = self._create_success_rate_chart()
            
            # System metrics chart (if available)
            if self.system_metrics:
                charts["system_metrics"] = self._create_system_metrics_chart()
            
            # Request timeline chart
            charts["request_timeline"] = self._create_request_timeline_chart()
            
        except Exception as e:
            logger.error(f"Error generating charts: {e}")
        
        return charts
    
    def _create_response_time_chart(self) -> str:
        """Create response time comparison chart."""
        fig = make_subplots(
            rows=2, cols=2,
            subplot_titles=("Mean Response Time", "95th Percentile", "99th Percentile", "Max Response Time"),
            specs=[[{"secondary_y": False}, {"secondary_y": False}],
                   [{"secondary_y": False}, {"secondary_y": False}]]
        )
        
        test_names = []
        mean_times = []
        p95_times = []
        p99_times = []
        max_times = []
        
        for result in self.test_results:
            test_names.append(result["test_name"])
            summary = result.get("summary", {})
            rt = summary.get("response_time", {})
            mean_times.append(rt.get("mean", 0))
            p95_times.append(rt.get("p95", 0))
            p99_times.append(rt.get("p99", 0))
            max_times.append(rt.get("max", 0))
        
        # Add traces
        fig.add_trace(go.Bar(x=test_names, y=mean_times, name="Mean"), row=1, col=1)
        fig.add_trace(go.Bar(x=test_names, y=p95_times, name="95th Percentile"), row=1, col=2)
        fig.add_trace(go.Bar(x=test_names, y=p99_times, name="99th Percentile"), row=2, col=1)
        fig.add_trace(go.Bar(x=test_names, y=max_times, name="Max"), row=2, col=2)
        
        fig.update_layout(
            title="Response Time Comparison Across Tests",
            showlegend=False,
            height=600
        )
        
        return pyo.plot(fig, output_type='div', include_plotlyjs=False)
    
    def _create_throughput_chart(self) -> str:
        """Create throughput comparison chart."""
        test_names = [result["test_name"] for result in self.test_results]
        request_rates = [result.get("summary", {}).get("request_rate", 0) for result in self.test_results]
        
        fig = go.Figure(data=[
            go.Bar(x=test_names, y=request_rates, name="Requests/Second")
        ])
        
        fig.update_layout(
            title="Throughput Comparison (Requests per Second)",
            xaxis_title="Test Scenario",
            yaxis_title="Requests per Second",
            height=400
        )
        
        return pyo.plot(fig, output_type='div', include_plotlyjs=False)
    
    def _create_success_rate_chart(self) -> str:
        """Create success rate comparison chart."""
        test_names = [result["test_name"] for result in self.test_results]
        success_rates = [result.get("summary", {}).get("success_rate", 0) for result in self.test_results]
        
        fig = go.Figure(data=[
            go.Bar(x=test_names, y=success_rates, name="Success Rate (%)")
        ])
        
        fig.update_layout(
            title="Success Rate Comparison",
            xaxis_title="Test Scenario",
            yaxis_title="Success Rate (%)",
            yaxis=dict(range=[0, 100]),
            height=400
        )
        
        return pyo.plot(fig, output_type='div', include_plotlyjs=False)
    
    def _create_system_metrics_chart(self) -> str:
        """Create system metrics chart."""
        if not self.system_metrics or "metrics" not in self.system_metrics:
            return ""
        
        metrics_data = self.system_metrics["metrics"]
        
        # Extract time series data
        timestamps = []
        cpu_usage = []
        memory_usage = []
        
        for metric in metrics_data:
            if "timestamp" in metric and "system" in metric:
                timestamps.append(metric["timestamp"])
                system = metric["system"]
                cpu_usage.append(system.get("cpu", {}).get("percent", 0))
                memory_usage.append(system.get("memory", {}).get("percent", 0))
        
        fig = make_subplots(
            rows=2, cols=1,
            subplot_titles=("CPU Usage (%)", "Memory Usage (%)"),
            shared_xaxes=True
        )
        
        fig.add_trace(
            go.Scatter(x=timestamps, y=cpu_usage, mode='lines', name='CPU Usage'),
            row=1, col=1
        )
        
        fig.add_trace(
            go.Scatter(x=timestamps, y=memory_usage, mode='lines', name='Memory Usage'),
            row=2, col=1
        )
        
        fig.update_layout(
            title="System Resource Usage During Tests",
            height=600,
            showlegend=False
        )
        
        return pyo.plot(fig, output_type='div', include_plotlyjs=False)
    
    def _create_request_timeline_chart(self) -> str:
        """Create request timeline chart showing request distribution over time."""
        if not self.test_results:
            return ""
        
        # Use the first test result for timeline
        result = self.test_results[0]
        requests = result.get("requests", [])
        
        if not requests:
            return ""
        
        # Group requests by time buckets (10-second intervals)
        time_buckets = {}
        start_time = min(req["start_time"] for req in requests)
        
        for req in requests:
            bucket = ((req["start_time"] - start_time) // 10000) * 10  # 10-second buckets
            if bucket not in time_buckets:
                time_buckets[bucket] = {"successful": 0, "failed": 0}
            
            if req["status"] == "OK":
                time_buckets[bucket]["successful"] += 1
            else:
                time_buckets[bucket]["failed"] += 1
        
        times = sorted(time_buckets.keys())
        successful_counts = [time_buckets[t]["successful"] for t in times]
        failed_counts = [time_buckets[t]["failed"] for t in times]
        
        fig = go.Figure()
        
        fig.add_trace(go.Scatter(
            x=times, y=successful_counts,
            mode='lines+markers',
            name='Successful Requests',
            line=dict(color='green')
        ))
        
        fig.add_trace(go.Scatter(
            x=times, y=failed_counts,
            mode='lines+markers',
            name='Failed Requests',
            line=dict(color='red')
        ))
        
        fig.update_layout(
            title=f"Request Timeline - {result['test_name']}",
            xaxis_title="Time (seconds from start)",
            yaxis_title="Requests per 10-second interval",
            height=400
        )
        
        return pyo.plot(fig, output_type='div', include_plotlyjs=False)
    
    def generate_html_report(self, include_system_metrics: bool = False) -> str:
        """Generate comprehensive HTML report."""
        logger.info("Generating HTML performance report...")
        
        # Load test results
        self.test_results = self.parse_gatling_results()
        
        if not self.test_results:
            logger.warning("No Gatling test results found")
            return ""
        
        # Load system metrics if requested
        if include_system_metrics:
            self.system_metrics = self.load_system_metrics()
        
        # Generate charts
        charts = self.generate_charts()
        
        # Prepare template data
        template_data = {
            "report_title": "Gatling Performance Test Report",
            "generation_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "test_results": self.test_results,
            "system_metrics": self.system_metrics,
            "charts": charts,
            "summary": self._generate_overall_summary()
        }
        
        # Generate HTML report
        html_content = self._render_html_template(template_data)
        
        # Save report
        with open(self.output_file, 'w') as f:
            f.write(html_content)
        
        logger.info(f"HTML report generated: {self.output_file}")
        return str(self.output_file)
    
    def _generate_overall_summary(self) -> Dict[str, Any]:
        """Generate overall summary statistics across all tests."""
        if not self.test_results:
            return {}
        
        total_requests = sum(result.get("summary", {}).get("total_requests", 0) for result in self.test_results)
        total_successful = sum(result.get("summary", {}).get("successful_requests", 0) for result in self.test_results)
        total_failed = sum(result.get("summary", {}).get("failed_requests", 0) for result in self.test_results)
        
        # Calculate average response times
        mean_response_times = [result.get("summary", {}).get("response_time", {}).get("mean", 0) for result in self.test_results]
        avg_mean_response_time = sum(mean_response_times) / len(mean_response_times) if mean_response_times else 0
        
        return {
            "total_tests": len(self.test_results),
            "total_requests": total_requests,
            "total_successful": total_successful,
            "total_failed": total_failed,
            "overall_success_rate": (total_successful / total_requests * 100) if total_requests > 0 else 0,
            "average_mean_response_time": avg_mean_response_time
        }
    
    def _render_html_template(self, data: Dict[str, Any]) -> str:
        """Render HTML template with performance data."""
        template_str = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{ report_title }}</title>
    <script src="https://cdn.plot.ly/plotly-latest.min.js"></script>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .header { text-align: center; margin-bottom: 30px; }
        .summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px; }
        .summary-card { background-color: #f8f9fa; padding: 20px; border-radius: 8px; text-align: center; }
        .summary-card h3 { margin: 0 0 10px 0; color: #333; }
        .summary-card .value { font-size: 2em; font-weight: bold; color: #007bff; }
        .chart-container { margin: 30px 0; }
        .test-results { margin-top: 30px; }
        .test-result { background-color: #f8f9fa; margin: 20px 0; padding: 20px; border-radius: 8px; }
        .test-result h3 { margin-top: 0; color: #333; }
        .metrics-table { width: 100%; border-collapse: collapse; margin-top: 10px; }
        .metrics-table th, .metrics-table td { padding: 8px 12px; text-align: left; border-bottom: 1px solid #ddd; }
        .metrics-table th { background-color: #e9ecef; }
        .success { color: #28a745; }
        .warning { color: #ffc107; }
        .danger { color: #dc3545; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>{{ report_title }}</h1>
            <p>Generated on {{ generation_time }}</p>
        </div>
        
        {% if summary %}
        <div class="summary-grid">
            <div class="summary-card">
                <h3>Total Tests</h3>
                <div class="value">{{ summary.total_tests }}</div>
            </div>
            <div class="summary-card">
                <h3>Total Requests</h3>
                <div class="value">{{ "{:,}".format(summary.total_requests) }}</div>
            </div>
            <div class="summary-card">
                <h3>Success Rate</h3>
                <div class="value {% if summary.overall_success_rate >= 95 %}success{% elif summary.overall_success_rate >= 90 %}warning{% else %}danger{% endif %}">
                    {{ "%.1f"|format(summary.overall_success_rate) }}%
                </div>
            </div>
            <div class="summary-card">
                <h3>Avg Response Time</h3>
                <div class="value">{{ "%.0f"|format(summary.average_mean_response_time) }}ms</div>
            </div>
        </div>
        {% endif %}
        
        {% for chart_name, chart_html in charts.items() %}
        <div class="chart-container">
            {{ chart_html|safe }}
        </div>
        {% endfor %}
        
        <div class="test-results">
            <h2>Test Results Details</h2>
            {% for result in test_results %}
            <div class="test-result">
                <h3>{{ result.test_name }}</h3>
                {% if result.summary %}
                <table class="metrics-table">
                    <tr>
                        <th>Metric</th>
                        <th>Value</th>
                    </tr>
                    <tr>
                        <td>Total Requests</td>
                        <td>{{ "{:,}".format(result.summary.total_requests) }}</td>
                    </tr>
                    <tr>
                        <td>Success Rate</td>
                        <td class="{% if result.summary.success_rate >= 95 %}success{% elif result.summary.success_rate >= 90 %}warning{% else %}danger{% endif %}">
                            {{ "%.1f"|format(result.summary.success_rate) }}%
                        </td>
                    </tr>
                    <tr>
                        <td>Mean Response Time</td>
                        <td>{{ "%.0f"|format(result.summary.response_time.mean) }}ms</td>
                    </tr>
                    <tr>
                        <td>95th Percentile</td>
                        <td>{{ "%.0f"|format(result.summary.response_time.p95) }}ms</td>
                    </tr>
                    <tr>
                        <td>99th Percentile</td>
                        <td>{{ "%.0f"|format(result.summary.response_time.p99) }}ms</td>
                    </tr>
                    <tr>
                        <td>Max Response Time</td>
                        <td>{{ "%.0f"|format(result.summary.response_time.max) }}ms</td>
                    </tr>
                    <tr>
                        <td>Request Rate</td>
                        <td>{{ "%.1f"|format(result.summary.request_rate) }} req/s</td>
                    </tr>
                </table>
                {% endif %}
            </div>
            {% endfor %}
        </div>
    </div>
</body>
</html>
        """
        
        template = jinja2.Template(template_str)
        return template.render(**data)

def main():
    """Main entry point for the report generator."""
    parser = argparse.ArgumentParser(description="Generate consolidated Gatling performance reports")
    parser.add_argument("--results-dir", required=True,
                       help="Directory containing Gatling test results")
    parser.add_argument("--output", required=True,
                       help="Output HTML file path")
    parser.add_argument("--include-system-metrics", action="store_true",
                       help="Include system metrics in the report")
    parser.add_argument("--verbose", "-v", action="store_true",
                       help="Enable verbose logging")
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Create report generator
    generator = GatlingReportGenerator(
        results_dir=Path(args.results_dir),
        output_file=Path(args.output)
    )
    
    try:
        report_path = generator.generate_html_report(
            include_system_metrics=args.include_system_metrics
        )
        
        if report_path:
            print(f"Performance report generated successfully: {report_path}")
        else:
            print("Failed to generate performance report")
            sys.exit(1)
            
    except Exception as e:
        logger.error(f"Failed to generate report: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()