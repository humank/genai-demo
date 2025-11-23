#!/usr/bin/env python3
"""
Performance Baseline and Regression Detection System

This script manages performance baselines and detects regressions by comparing
current test results against established baselines.
"""

import argparse
import json
import logging
import statistics
import sys
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple
import yaml

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class PerformanceBaseline:
    """
    Manages performance baselines and regression detection.
    """
    
    def __init__(self, baseline_dir: Path = Path("staging-tests/performance/baselines")):
        self.baseline_dir = Path(baseline_dir)
        self.baseline_dir.mkdir(parents=True, exist_ok=True)
        
        # Default thresholds for regression detection
        self.default_thresholds = {
            "response_time": {
                "mean": 0.20,      # 20% increase is a regression
                "p95": 0.25,       # 25% increase in 95th percentile
                "p99": 0.30,       # 30% increase in 99th percentile
                "max": 0.50        # 50% increase in max response time
            },
            "throughput": {
                "requests_per_second": -0.15  # 15% decrease is a regression
            },
            "success_rate": {
                "percentage": -0.05  # 5% decrease is a regression
            },
            "resource_usage": {
                "cpu_percent": 0.30,     # 30% increase in CPU usage
                "memory_percent": 0.25,  # 25% increase in memory usage
                "jvm_memory_percent": 0.20  # 20% increase in JVM memory
            }
        }
    
    def create_baseline(self, test_name: str, test_results: Dict[str, Any], 
                       description: str = "", force: bool = False) -> bool:
        """Create a new performance baseline from test results."""
        baseline_file = self.baseline_dir / f"{test_name}.json"
        
        if baseline_file.exists() and not force:
            logger.warning(f"Baseline already exists for {test_name}. Use --force to overwrite.")
            return False
        
        # Extract key metrics from test results
        baseline_data = {
            "test_name": test_name,
            "created_at": datetime.now().isoformat(),
            "description": description,
            "baseline_metrics": self._extract_baseline_metrics(test_results),
            "test_configuration": self._extract_test_configuration(test_results),
            "thresholds": self.default_thresholds.copy()
        }
        
        # Save baseline
        with open(baseline_file, 'w') as f:
            json.dump(baseline_data, f, indent=2)
        
        logger.info(f"Created baseline for {test_name}: {baseline_file}")
        return True
    
    def update_baseline(self, test_name: str, test_results: List[Dict[str, Any]], 
                       strategy: str = "rolling_average") -> bool:
        """Update existing baseline using multiple test results."""
        baseline_file = self.baseline_dir / f"{test_name}.json"
        
        if not baseline_file.exists():
            logger.error(f"No baseline exists for {test_name}. Create one first.")
            return False
        
        # Load existing baseline
        with open(baseline_file, 'r') as f:
            baseline_data = json.load(f)
        
        # Update baseline metrics based on strategy
        if strategy == "rolling_average":
            updated_metrics = self._calculate_rolling_average_baseline(
                baseline_data["baseline_metrics"], test_results
            )
        elif strategy == "best_performance":
            updated_metrics = self._calculate_best_performance_baseline(
                baseline_data["baseline_metrics"], test_results
            )
        else:
            logger.error(f"Unknown baseline update strategy: {strategy}")
            return False
        
        # Update baseline
        baseline_data["baseline_metrics"] = updated_metrics
        baseline_data["updated_at"] = datetime.now().isoformat()
        baseline_data["update_strategy"] = strategy
        
        with open(baseline_file, 'w') as f:
            json.dump(baseline_data, f, indent=2)
        
        logger.info(f"Updated baseline for {test_name} using {strategy} strategy")
        return True
    
    def detect_regressions(self, test_name: str, current_results: Dict[str, Any]) -> Dict[str, Any]:
        """Detect performance regressions by comparing against baseline."""
        baseline_file = self.baseline_dir / f"{test_name}.json"
        
        if not baseline_file.exists():
            logger.warning(f"No baseline exists for {test_name}. Cannot detect regressions.")
            return {
                "has_baseline": False,
                "regressions": [],
                "improvements": [],
                "summary": "No baseline available for comparison"
            }
        
        # Load baseline
        with open(baseline_file, 'r') as f:
            baseline_data = json.load(f)
        
        baseline_metrics = baseline_data["baseline_metrics"]
        thresholds = baseline_data.get("thresholds", self.default_thresholds)
        current_metrics = self._extract_baseline_metrics(current_results)
        
        # Compare metrics and detect regressions
        regressions = []
        improvements = []
        
        # Response time comparisons
        rt_regressions, rt_improvements = self._compare_response_times(
            baseline_metrics.get("response_time", {}),
            current_metrics.get("response_time", {}),
            thresholds["response_time"]
        )
        regressions.extend(rt_regressions)
        improvements.extend(rt_improvements)
        
        # Throughput comparisons
        tp_regressions, tp_improvements = self._compare_throughput(
            baseline_metrics.get("throughput", {}),
            current_metrics.get("throughput", {}),
            thresholds["throughput"]
        )
        regressions.extend(tp_regressions)
        improvements.extend(tp_improvements)
        
        # Success rate comparisons
        sr_regressions, sr_improvements = self._compare_success_rate(
            baseline_metrics.get("success_rate", {}),
            current_metrics.get("success_rate", {}),
            thresholds["success_rate"]
        )
        regressions.extend(sr_regressions)
        improvements.extend(sr_improvements)
        
        # Resource usage comparisons
        ru_regressions, ru_improvements = self._compare_resource_usage(
            baseline_metrics.get("resource_usage", {}),
            current_metrics.get("resource_usage", {}),
            thresholds["resource_usage"]
        )
        regressions.extend(ru_regressions)
        improvements.extend(ru_improvements)
        
        # Generate summary
        summary = self._generate_regression_summary(regressions, improvements)
        
        return {
            "has_baseline": True,
            "baseline_created": baseline_data.get("created_at"),
            "baseline_updated": baseline_data.get("updated_at"),
            "test_name": test_name,
            "comparison_timestamp": datetime.now().isoformat(),
            "regressions": regressions,
            "improvements": improvements,
            "summary": summary,
            "baseline_metrics": baseline_metrics,
            "current_metrics": current_metrics
        }
    
    def generate_trend_analysis(self, test_name: str, results_history: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Generate trend analysis from historical test results."""
        if len(results_history) < 3:
            return {
                "error": "Insufficient data for trend analysis (minimum 3 results required)"
            }
        
        # Extract metrics from all results
        metrics_timeline = []
        for result in results_history:
            metrics = self._extract_baseline_metrics(result)
            metrics["timestamp"] = result.get("timestamp", datetime.now().isoformat())
            metrics_timeline.append(metrics)
        
        # Calculate trends
        trends = {
            "response_time_trends": self._calculate_metric_trends(
                metrics_timeline, ["response_time", "mean"]
            ),
            "throughput_trends": self._calculate_metric_trends(
                metrics_timeline, ["throughput", "requests_per_second"]
            ),
            "success_rate_trends": self._calculate_metric_trends(
                metrics_timeline, ["success_rate", "percentage"]
            ),
            "resource_usage_trends": {
                "cpu": self._calculate_metric_trends(
                    metrics_timeline, ["resource_usage", "cpu_percent"]
                ),
                "memory": self._calculate_metric_trends(
                    metrics_timeline, ["resource_usage", "memory_percent"]
                )
            }
        }
        
        # Identify concerning trends
        alerts = self._identify_trend_alerts(trends)
        
        return {
            "test_name": test_name,
            "analysis_period": {
                "start": metrics_timeline[0]["timestamp"],
                "end": metrics_timeline[-1]["timestamp"],
                "data_points": len(metrics_timeline)
            },
            "trends": trends,
            "alerts": alerts,
            "recommendations": self._generate_trend_recommendations(trends, alerts)
        }
    
    def _extract_baseline_metrics(self, test_results: Dict[str, Any]) -> Dict[str, Any]:
        """Extract key metrics from test results for baseline comparison."""
        summary = test_results.get("summary", {})
        
        # Response time metrics
        response_time = summary.get("response_time", {})
        
        # Throughput metrics
        throughput = {
            "requests_per_second": summary.get("request_rate", 0),
            "total_requests": summary.get("total_requests", 0)
        }
        
        # Success rate metrics
        success_rate = {
            "percentage": summary.get("success_rate", 0),
            "successful_requests": summary.get("successful_requests", 0),
            "failed_requests": summary.get("failed_requests", 0)
        }
        
        # Resource usage metrics (if available)
        resource_usage = {}
        if "system_metrics" in test_results:
            system = test_results["system_metrics"]
            if isinstance(system, dict):
                resource_usage = {
                    "cpu_percent": system.get("cpu_percent", 0),
                    "memory_percent": system.get("memory_percent", 0),
                    "jvm_memory_percent": system.get("jvm_memory_percent", 0)
                }
        
        return {
            "response_time": response_time,
            "throughput": throughput,
            "success_rate": success_rate,
            "resource_usage": resource_usage
        }
    
    def _extract_test_configuration(self, test_results: Dict[str, Any]) -> Dict[str, Any]:
        """Extract test configuration for baseline context."""
        return {
            "test_type": test_results.get("test_type", "unknown"),
            "duration": test_results.get("duration", 0),
            "concurrent_users": test_results.get("users", 0),
            "target_host": test_results.get("target_host", "unknown")
        }
    
    def _compare_response_times(self, baseline: Dict[str, Any], current: Dict[str, Any], 
                               thresholds: Dict[str, float]) -> Tuple[List[Dict], List[Dict]]:
        """Compare response time metrics."""
        regressions = []
        improvements = []
        
        metrics = ["mean", "p95", "p99", "max"]
        
        for metric in metrics:
            baseline_value = baseline.get(metric, 0)
            current_value = current.get(metric, 0)
            
            if baseline_value > 0:  # Avoid division by zero
                change_percent = (current_value - baseline_value) / baseline_value
                threshold = thresholds.get(metric, 0.20)
                
                if change_percent > threshold:
                    regressions.append({
                        "metric": f"response_time.{metric}",
                        "baseline_value": baseline_value,
                        "current_value": current_value,
                        "change_percent": change_percent * 100,
                        "threshold_percent": threshold * 100,
                        "severity": self._calculate_severity(change_percent, threshold)
                    })
                elif change_percent < -0.05:  # 5% improvement threshold
                    improvements.append({
                        "metric": f"response_time.{metric}",
                        "baseline_value": baseline_value,
                        "current_value": current_value,
                        "improvement_percent": abs(change_percent) * 100
                    })
        
        return regressions, improvements
    
    def _compare_throughput(self, baseline: Dict[str, Any], current: Dict[str, Any], 
                           thresholds: Dict[str, float]) -> Tuple[List[Dict], List[Dict]]:
        """Compare throughput metrics."""
        regressions = []
        improvements = []
        
        baseline_rps = baseline.get("requests_per_second", 0)
        current_rps = current.get("requests_per_second", 0)
        
        if baseline_rps > 0:
            change_percent = (current_rps - baseline_rps) / baseline_rps
            threshold = thresholds.get("requests_per_second", -0.15)
            
            if change_percent < threshold:  # Negative threshold for throughput
                regressions.append({
                    "metric": "throughput.requests_per_second",
                    "baseline_value": baseline_rps,
                    "current_value": current_rps,
                    "change_percent": change_percent * 100,
                    "threshold_percent": threshold * 100,
                    "severity": self._calculate_severity(abs(change_percent), abs(threshold))
                })
            elif change_percent > 0.05:  # 5% improvement threshold
                improvements.append({
                    "metric": "throughput.requests_per_second",
                    "baseline_value": baseline_rps,
                    "current_value": current_rps,
                    "improvement_percent": change_percent * 100
                })
        
        return regressions, improvements
    
    def _compare_success_rate(self, baseline: Dict[str, Any], current: Dict[str, Any], 
                             thresholds: Dict[str, float]) -> Tuple[List[Dict], List[Dict]]:
        """Compare success rate metrics."""
        regressions = []
        improvements = []
        
        baseline_rate = baseline.get("percentage", 0)
        current_rate = current.get("percentage", 0)
        
        if baseline_rate > 0:
            change_percent = (current_rate - baseline_rate) / baseline_rate
            threshold = thresholds.get("percentage", -0.05)
            
            if change_percent < threshold:  # Negative threshold for success rate
                regressions.append({
                    "metric": "success_rate.percentage",
                    "baseline_value": baseline_rate,
                    "current_value": current_rate,
                    "change_percent": change_percent * 100,
                    "threshold_percent": threshold * 100,
                    "severity": self._calculate_severity(abs(change_percent), abs(threshold))
                })
            elif change_percent > 0.01:  # 1% improvement threshold
                improvements.append({
                    "metric": "success_rate.percentage",
                    "baseline_value": baseline_rate,
                    "current_value": current_rate,
                    "improvement_percent": change_percent * 100
                })
        
        return regressions, improvements
    
    def _compare_resource_usage(self, baseline: Dict[str, Any], current: Dict[str, Any], 
                               thresholds: Dict[str, float]) -> Tuple[List[Dict], List[Dict]]:
        """Compare resource usage metrics."""
        regressions = []
        improvements = []
        
        metrics = ["cpu_percent", "memory_percent", "jvm_memory_percent"]
        
        for metric in metrics:
            baseline_value = baseline.get(metric, 0)
            current_value = current.get(metric, 0)
            
            if baseline_value > 0:
                change_percent = (current_value - baseline_value) / baseline_value
                threshold = thresholds.get(metric, 0.25)
                
                if change_percent > threshold:
                    regressions.append({
                        "metric": f"resource_usage.{metric}",
                        "baseline_value": baseline_value,
                        "current_value": current_value,
                        "change_percent": change_percent * 100,
                        "threshold_percent": threshold * 100,
                        "severity": self._calculate_severity(change_percent, threshold)
                    })
                elif change_percent < -0.10:  # 10% improvement threshold
                    improvements.append({
                        "metric": f"resource_usage.{metric}",
                        "baseline_value": baseline_value,
                        "current_value": current_value,
                        "improvement_percent": abs(change_percent) * 100
                    })
        
        return regressions, improvements
    
    def _calculate_severity(self, change_percent: float, threshold: float) -> str:
        """Calculate regression severity based on change magnitude."""
        if change_percent >= threshold * 2:
            return "critical"
        elif change_percent >= threshold * 1.5:
            return "high"
        elif change_percent >= threshold:
            return "medium"
        else:
            return "low"
    
    def _generate_regression_summary(self, regressions: List[Dict], improvements: List[Dict]) -> str:
        """Generate a human-readable summary of regression analysis."""
        if not regressions and not improvements:
            return "No significant performance changes detected."
        
        summary_parts = []
        
        if regressions:
            critical_count = sum(1 for r in regressions if r["severity"] == "critical")
            high_count = sum(1 for r in regressions if r["severity"] == "high")
            medium_count = sum(1 for r in regressions if r["severity"] == "medium")
            
            if critical_count > 0:
                summary_parts.append(f"🚨 {critical_count} critical regression(s)")
            if high_count > 0:
                summary_parts.append(f"⚠️ {high_count} high severity regression(s)")
            if medium_count > 0:
                summary_parts.append(f"⚠️ {medium_count} medium severity regression(s)")
        
        if improvements:
            summary_parts.append(f"✅ {len(improvements)} performance improvement(s)")
        
        return "; ".join(summary_parts)
    
    def _calculate_rolling_average_baseline(self, current_baseline: Dict[str, Any], 
                                          new_results: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Calculate new baseline using rolling average of recent results."""
        # Implementation for rolling average baseline update
        # This is a simplified version - in practice, you'd want more sophisticated averaging
        return current_baseline  # Placeholder
    
    def _calculate_best_performance_baseline(self, current_baseline: Dict[str, Any], 
                                           new_results: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Calculate new baseline using best performance from recent results."""
        # Implementation for best performance baseline update
        return current_baseline  # Placeholder
    
    def _calculate_metric_trends(self, metrics_timeline: List[Dict[str, Any]], 
                                metric_path: List[str]) -> Dict[str, Any]:
        """Calculate trend for a specific metric over time."""
        values = []
        for metrics in metrics_timeline:
            value = metrics
            for key in metric_path:
                value = value.get(key, {})
                if not isinstance(value, dict):
                    break
            if isinstance(value, (int, float)):
                values.append(value)
        
        if len(values) < 2:
            return {"trend": "insufficient_data", "values": values}
        
        # Simple linear trend calculation
        n = len(values)
        x_sum = sum(range(n))
        y_sum = sum(values)
        xy_sum = sum(i * values[i] for i in range(n))
        x2_sum = sum(i * i for i in range(n))
        
        if n * x2_sum - x_sum * x_sum == 0:
            return {"trend": "stable", "values": values, "slope": 0}
        
        slope = (n * xy_sum - x_sum * y_sum) / (n * x2_sum - x_sum * x_sum)
        
        # Determine trend direction
        if abs(slope) < 0.01:
            trend = "stable"
        elif slope > 0:
            trend = "increasing"
        else:
            trend = "decreasing"
        
        return {
            "trend": trend,
            "slope": slope,
            "values": values,
            "min": min(values),
            "max": max(values),
            "mean": statistics.mean(values),
            "stdev": statistics.stdev(values) if len(values) > 1 else 0
        }
    
    def _identify_trend_alerts(self, trends: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Identify concerning trends that require attention."""
        alerts = []
        
        # Check response time trends
        rt_trend = trends.get("response_time_trends", {})
        if rt_trend.get("trend") == "increasing" and rt_trend.get("slope", 0) > 10:
            alerts.append({
                "type": "trend_alert",
                "severity": "warning",
                "metric": "response_time",
                "message": "Response time showing increasing trend",
                "slope": rt_trend.get("slope", 0)
            })
        
        # Check throughput trends
        tp_trend = trends.get("throughput_trends", {})
        if tp_trend.get("trend") == "decreasing" and tp_trend.get("slope", 0) < -5:
            alerts.append({
                "type": "trend_alert",
                "severity": "warning",
                "metric": "throughput",
                "message": "Throughput showing decreasing trend",
                "slope": tp_trend.get("slope", 0)
            })
        
        return alerts
    
    def _generate_trend_recommendations(self, trends: Dict[str, Any], alerts: List[Dict[str, Any]]) -> List[str]:
        """Generate recommendations based on trend analysis."""
        recommendations = []
        
        if any(alert["metric"] == "response_time" for alert in alerts):
            recommendations.append("Consider investigating response time degradation - check for resource bottlenecks")
        
        if any(alert["metric"] == "throughput" for alert in alerts):
            recommendations.append("Throughput decline detected - review system capacity and scaling policies")
        
        # Add more recommendation logic based on trends
        
        return recommendations

def main():
    """Main entry point for the performance baseline tool."""
    parser = argparse.ArgumentParser(description="Performance Baseline and Regression Detection")
    parser.add_argument("command", choices=["create", "update", "compare", "trend"],
                       help="Command to execute")
    parser.add_argument("--test-name", required=True,
                       help="Name of the test scenario")
    parser.add_argument("--results-file", 
                       help="Path to test results JSON file")
    parser.add_argument("--baseline-dir", default="staging-tests/performance/baselines",
                       help="Directory to store baselines")
    parser.add_argument("--description", default="",
                       help="Description for the baseline")
    parser.add_argument("--force", action="store_true",
                       help="Force overwrite existing baseline")
    parser.add_argument("--strategy", choices=["rolling_average", "best_performance"], 
                       default="rolling_average",
                       help="Baseline update strategy")
    parser.add_argument("--output", 
                       help="Output file for comparison results")
    parser.add_argument("--verbose", "-v", action="store_true",
                       help="Enable verbose logging")
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Create baseline manager
    baseline_manager = PerformanceBaseline(baseline_dir=Path(args.baseline_dir))
    
    try:
        if args.command == "create":
            if not args.results_file:
                logger.error("--results-file is required for create command")
                sys.exit(1)
            
            with open(args.results_file, 'r') as f:
                test_results = json.load(f)
            
            success = baseline_manager.create_baseline(
                args.test_name, test_results, args.description, args.force
            )
            sys.exit(0 if success else 1)
        
        elif args.command == "compare":
            if not args.results_file:
                logger.error("--results-file is required for compare command")
                sys.exit(1)
            
            with open(args.results_file, 'r') as f:
                current_results = json.load(f)
            
            comparison = baseline_manager.detect_regressions(args.test_name, current_results)
            
            # Output results
            if args.output:
                with open(args.output, 'w') as f:
                    json.dump(comparison, f, indent=2)
                logger.info(f"Comparison results saved to {args.output}")
            else:
                print(json.dumps(comparison, indent=2))
            
            # Exit with error code if regressions found
            if comparison.get("regressions"):
                sys.exit(1)
        
        else:
            logger.error(f"Command {args.command} not fully implemented yet")
            sys.exit(1)
    
    except Exception as e:
        logger.error(f"Failed to execute {args.command}: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()