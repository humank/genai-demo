#!/usr/bin/env python3
"""
Performance Alerting System

This script monitors performance test results and sends alerts when
thresholds are violated or regressions are detected.
"""

import argparse
import json
import logging
import smtplib
import sys
from datetime import datetime
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from pathlib import Path
from typing import Dict, List, Optional, Any
import requests
import yaml

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class PerformanceAlerting:
    """
    Performance alerting system for load testing results.
    """
    
    def __init__(self, config_file: Optional[Path] = None):
        self.config = self._load_config(config_file)
        self.alert_history: List[Dict[str, Any]] = []
    
    def _load_config(self, config_file: Optional[Path]) -> Dict[str, Any]:
        """Load alerting configuration."""
        default_config = {
            "thresholds": {
                "response_time": {
                    "mean_ms": 2000,
                    "p95_ms": 5000,
                    "p99_ms": 10000,
                    "max_ms": 30000
                },
                "success_rate": {
                    "min_percentage": 95.0
                },
                "throughput": {
                    "min_requests_per_second": 50
                },
                "resource_usage": {
                    "max_cpu_percent": 80,
                    "max_memory_percent": 85,
                    "max_jvm_memory_percent": 80
                }
            },
            "alerting": {
                "enabled": True,
                "channels": ["console", "file"],
                "email": {
                    "enabled": False,
                    "smtp_server": "localhost",
                    "smtp_port": 587,
                    "username": "",
                    "password": "",
                    "from_address": "alerts@example.com",
                    "to_addresses": []
                },
                "webhook": {
                    "enabled": False,
                    "url": "",
                    "headers": {}
                },
                "slack": {
                    "enabled": False,
                    "webhook_url": "",
                    "channel": "#performance-alerts"
                }
            },
            "suppression": {
                "duplicate_alert_window_minutes": 30,
                "max_alerts_per_hour": 10
            }
        }
        
        if config_file and config_file.exists():
            try:
                with open(config_file, 'r') as f:
                    user_config = yaml.safe_load(f)
                    # Merge user config with defaults
                    self._deep_merge(default_config, user_config)
            except Exception as e:
                logger.error(f"Error loading config file {config_file}: {e}")
        
        return default_config
    
    def _deep_merge(self, base_dict: Dict, update_dict: Dict):
        """Deep merge two dictionaries."""
        for key, value in update_dict.items():
            if key in base_dict and isinstance(base_dict[key], dict) and isinstance(value, dict):
                self._deep_merge(base_dict[key], value)
            else:
                base_dict[key] = value
    
    def check_thresholds(self, test_name: str, test_results: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Check test results against configured thresholds."""
        alerts = []
        thresholds = self.config["thresholds"]
        summary = test_results.get("summary", {})
        
        # Response time threshold checks
        rt_thresholds = thresholds["response_time"]
        response_time = summary.get("response_time", {})
        
        if response_time.get("mean", 0) > rt_thresholds["mean_ms"]:
            alerts.append({
                "type": "threshold_violation",
                "severity": "warning",
                "metric": "response_time.mean",
                "current_value": response_time["mean"],
                "threshold": rt_thresholds["mean_ms"],
                "message": f"Mean response time ({response_time['mean']:.0f}ms) exceeds threshold ({rt_thresholds['mean_ms']}ms)"
            })
        
        if response_time.get("p95", 0) > rt_thresholds["p95_ms"]:
            alerts.append({
                "type": "threshold_violation",
                "severity": "warning",
                "metric": "response_time.p95",
                "current_value": response_time["p95"],
                "threshold": rt_thresholds["p95_ms"],
                "message": f"95th percentile response time ({response_time['p95']:.0f}ms) exceeds threshold ({rt_thresholds['p95_ms']}ms)"
            })
        
        if response_time.get("p99", 0) > rt_thresholds["p99_ms"]:
            alerts.append({
                "type": "threshold_violation",
                "severity": "critical",
                "metric": "response_time.p99",
                "current_value": response_time["p99"],
                "threshold": rt_thresholds["p99_ms"],
                "message": f"99th percentile response time ({response_time['p99']:.0f}ms) exceeds threshold ({rt_thresholds['p99_ms']}ms)"
            })
        
        # Success rate threshold checks
        success_rate = summary.get("success_rate", 0)
        min_success_rate = thresholds["success_rate"]["min_percentage"]
        
        if success_rate < min_success_rate:
            severity = "critical" if success_rate < min_success_rate - 10 else "warning"
            alerts.append({
                "type": "threshold_violation",
                "severity": severity,
                "metric": "success_rate",
                "current_value": success_rate,
                "threshold": min_success_rate,
                "message": f"Success rate ({success_rate:.1f}%) below threshold ({min_success_rate}%)"
            })
        
        # Throughput threshold checks
        request_rate = summary.get("request_rate", 0)
        min_throughput = thresholds["throughput"]["min_requests_per_second"]
        
        if request_rate < min_throughput:
            alerts.append({
                "type": "threshold_violation",
                "severity": "warning",
                "metric": "throughput",
                "current_value": request_rate,
                "threshold": min_throughput,
                "message": f"Throughput ({request_rate:.1f} req/s) below threshold ({min_throughput} req/s)"
            })
        
        # Add test context to alerts
        for alert in alerts:
            alert["test_name"] = test_name
            alert["timestamp"] = datetime.now().isoformat()
        
        return alerts
    
    def check_regressions(self, test_name: str, regression_results: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Convert regression detection results into alerts."""
        alerts = []
        
        if not regression_results.get("has_baseline", False):
            return alerts
        
        regressions = regression_results.get("regressions", [])
        
        for regression in regressions:
            severity_map = {
                "critical": "critical",
                "high": "critical",
                "medium": "warning",
                "low": "info"
            }
            
            alerts.append({
                "type": "performance_regression",
                "severity": severity_map.get(regression["severity"], "warning"),
                "metric": regression["metric"],
                "current_value": regression["current_value"],
                "baseline_value": regression["baseline_value"],
                "change_percent": regression["change_percent"],
                "threshold_percent": regression["threshold_percent"],
                "message": f"Performance regression detected in {regression['metric']}: "
                          f"{regression['change_percent']:+.1f}% change "
                          f"(threshold: {regression['threshold_percent']:+.1f}%)",
                "test_name": test_name,
                "timestamp": datetime.now().isoformat()
            })
        
        return alerts
    
    def send_alerts(self, alerts: List[Dict[str, Any]]) -> bool:
        """Send alerts through configured channels."""
        if not alerts or not self.config["alerting"]["enabled"]:
            return True
        
        # Filter alerts based on suppression rules
        filtered_alerts = self._filter_suppressed_alerts(alerts)
        
        if not filtered_alerts:
            logger.info("All alerts suppressed due to suppression rules")
            return True
        
        success = True
        channels = self.config["alerting"]["channels"]
        
        # Console alerts
        if "console" in channels:
            self._send_console_alerts(filtered_alerts)
        
        # File alerts
        if "file" in channels:
            success &= self._send_file_alerts(filtered_alerts)
        
        # Email alerts
        if "email" in channels and self.config["alerting"]["email"]["enabled"]:
            success &= self._send_email_alerts(filtered_alerts)
        
        # Webhook alerts
        if "webhook" in channels and self.config["alerting"]["webhook"]["enabled"]:
            success &= self._send_webhook_alerts(filtered_alerts)
        
        # Slack alerts
        if "slack" in channels and self.config["alerting"]["slack"]["enabled"]:
            success &= self._send_slack_alerts(filtered_alerts)
        
        # Record alerts in history
        self.alert_history.extend(filtered_alerts)
        
        return success
    
    def _filter_suppressed_alerts(self, alerts: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Filter out suppressed alerts based on suppression rules."""
        suppression = self.config["suppression"]
        window_minutes = suppression["duplicate_alert_window_minutes"]
        max_per_hour = suppression["max_alerts_per_hour"]
        
        # Check for recent similar alerts
        cutoff_time = datetime.now() - timedelta(minutes=window_minutes)
        recent_alerts = [
            alert for alert in self.alert_history
            if datetime.fromisoformat(alert["timestamp"]) > cutoff_time
        ]
        
        # Check hourly limit
        hourly_cutoff = datetime.now() - timedelta(hours=1)
        hourly_alerts = [
            alert for alert in self.alert_history
            if datetime.fromisoformat(alert["timestamp"]) > hourly_cutoff
        ]
        
        if len(hourly_alerts) >= max_per_hour:
            logger.warning(f"Alert rate limit reached ({max_per_hour}/hour). Suppressing alerts.")
            return []
        
        # Filter duplicate alerts
        filtered_alerts = []
        for alert in alerts:
            # Check for duplicate alerts in recent history
            is_duplicate = any(
                recent["metric"] == alert["metric"] and 
                recent["test_name"] == alert["test_name"] and
                recent["type"] == alert["type"]
                for recent in recent_alerts
            )
            
            if not is_duplicate:
                filtered_alerts.append(alert)
            else:
                logger.debug(f"Suppressing duplicate alert for {alert['metric']}")
        
        return filtered_alerts
    
    def _send_console_alerts(self, alerts: List[Dict[str, Any]]):
        """Send alerts to console."""
        print("\n" + "="*60)
        print("🚨 PERFORMANCE ALERTS")
        print("="*60)
        
        for alert in alerts:
            severity_emoji = {
                "critical": "🚨",
                "warning": "⚠️",
                "info": "ℹ️"
            }.get(alert["severity"], "⚠️")
            
            print(f"{severity_emoji} [{alert['severity'].upper()}] {alert['message']}")
            print(f"   Test: {alert['test_name']}")
            print(f"   Time: {alert['timestamp']}")
            print()
    
    def _send_file_alerts(self, alerts: List[Dict[str, Any]]) -> bool:
        """Send alerts to file."""
        try:
            alerts_file = Path("staging-tests/reports/performance-alerts.json")
            alerts_file.parent.mkdir(parents=True, exist_ok=True)
            
            # Load existing alerts
            existing_alerts = []
            if alerts_file.exists():
                with open(alerts_file, 'r') as f:
                    existing_alerts = json.load(f)
            
            # Append new alerts
            existing_alerts.extend(alerts)
            
            # Keep only last 1000 alerts
            if len(existing_alerts) > 1000:
                existing_alerts = existing_alerts[-1000:]
            
            # Save alerts
            with open(alerts_file, 'w') as f:
                json.dump(existing_alerts, f, indent=2)
            
            logger.info(f"Saved {len(alerts)} alerts to {alerts_file}")
            return True
        
        except Exception as e:
            logger.error(f"Error saving alerts to file: {e}")
            return False
    
    def _send_email_alerts(self, alerts: List[Dict[str, Any]]) -> bool:
        """Send alerts via email."""
        try:
            email_config = self.config["alerting"]["email"]
            
            # Create email message
            msg = MIMEMultipart()
            msg['From'] = email_config["from_address"]
            msg['To'] = ", ".join(email_config["to_addresses"])
            msg['Subject'] = f"Performance Alert - {len(alerts)} issue(s) detected"
            
            # Create email body
            body = self._create_email_body(alerts)
            msg.attach(MIMEText(body, 'html'))
            
            # Send email
            server = smtplib.SMTP(email_config["smtp_server"], email_config["smtp_port"])
            if email_config["username"]:
                server.starttls()
                server.login(email_config["username"], email_config["password"])
            
            server.send_message(msg)
            server.quit()
            
            logger.info(f"Sent email alerts to {len(email_config['to_addresses'])} recipients")
            return True
        
        except Exception as e:
            logger.error(f"Error sending email alerts: {e}")
            return False
    
    def _send_webhook_alerts(self, alerts: List[Dict[str, Any]]) -> bool:
        """Send alerts via webhook."""
        try:
            webhook_config = self.config["alerting"]["webhook"]
            
            payload = {
                "timestamp": datetime.now().isoformat(),
                "alert_count": len(alerts),
                "alerts": alerts
            }
            
            response = requests.post(
                webhook_config["url"],
                json=payload,
                headers=webhook_config.get("headers", {}),
                timeout=10
            )
            
            response.raise_for_status()
            logger.info(f"Sent webhook alerts to {webhook_config['url']}")
            return True
        
        except Exception as e:
            logger.error(f"Error sending webhook alerts: {e}")
            return False
    
    def _send_slack_alerts(self, alerts: List[Dict[str, Any]]) -> bool:
        """Send alerts to Slack."""
        try:
            slack_config = self.config["alerting"]["slack"]
            
            # Create Slack message
            message = self._create_slack_message(alerts)
            
            payload = {
                "channel": slack_config["channel"],
                "text": f"Performance Alert - {len(alerts)} issue(s) detected",
                "attachments": [
                    {
                        "color": "danger" if any(a["severity"] == "critical" for a in alerts) else "warning",
                        "text": message,
                        "ts": int(datetime.now().timestamp())
                    }
                ]
            }
            
            response = requests.post(
                slack_config["webhook_url"],
                json=payload,
                timeout=10
            )
            
            response.raise_for_status()
            logger.info(f"Sent Slack alerts to {slack_config['channel']}")
            return True
        
        except Exception as e:
            logger.error(f"Error sending Slack alerts: {e}")
            return False
    
    def _create_email_body(self, alerts: List[Dict[str, Any]]) -> str:
        """Create HTML email body for alerts."""
        html = """
        <html>
        <head>
            <style>
                body { font-family: Arial, sans-serif; }
                .alert { margin: 10px 0; padding: 10px; border-left: 4px solid; }
                .critical { border-color: #dc3545; background-color: #f8d7da; }
                .warning { border-color: #ffc107; background-color: #fff3cd; }
                .info { border-color: #17a2b8; background-color: #d1ecf1; }
                .metric { font-weight: bold; }
                .timestamp { color: #666; font-size: 0.9em; }
            </style>
        </head>
        <body>
            <h2>Performance Alerts</h2>
            <p>The following performance issues were detected:</p>
        """
        
        for alert in alerts:
            severity_class = alert["severity"]
            html += f"""
            <div class="alert {severity_class}">
                <div class="metric">{alert['metric']}</div>
                <div>{alert['message']}</div>
                <div class="timestamp">Test: {alert['test_name']} | Time: {alert['timestamp']}</div>
            </div>
            """
        
        html += """
        </body>
        </html>
        """
        
        return html
    
    def _create_slack_message(self, alerts: List[Dict[str, Any]]) -> str:
        """Create Slack message for alerts."""
        message_parts = []
        
        for alert in alerts:
            severity_emoji = {
                "critical": "🚨",
                "warning": "⚠️",
                "info": "ℹ️"
            }.get(alert["severity"], "⚠️")
            
            message_parts.append(
                f"{severity_emoji} *{alert['metric']}*: {alert['message']}\n"
                f"   Test: `{alert['test_name']}` | Time: {alert['timestamp']}"
            )
        
        return "\n\n".join(message_parts)
    
    def generate_performance_recommendations(self, alerts: List[Dict[str, Any]]) -> List[str]:
        """Generate performance optimization recommendations based on alerts."""
        recommendations = []
        
        # Group alerts by metric type
        alert_types = {}
        for alert in alerts:
            metric_type = alert["metric"].split(".")[0]
            if metric_type not in alert_types:
                alert_types[metric_type] = []
            alert_types[metric_type].append(alert)
        
        # Generate recommendations based on alert patterns
        if "response_time" in alert_types:
            recommendations.append(
                "🔧 Response Time Issues Detected:\n"
                "   • Review database query performance and add missing indexes\n"
                "   • Consider implementing caching for frequently accessed data\n"
                "   • Check for N+1 query problems in ORM mappings\n"
                "   • Review application thread pool configuration"
            )
        
        if "success_rate" in alert_types:
            recommendations.append(
                "🔧 Success Rate Issues Detected:\n"
                "   • Review application error logs for failure patterns\n"
                "   • Check database connection pool configuration\n"
                "   • Verify external service dependencies and timeouts\n"
                "   • Consider implementing circuit breaker patterns"
            )
        
        if "throughput" in alert_types:
            recommendations.append(
                "🔧 Throughput Issues Detected:\n"
                "   • Review application server thread pool settings\n"
                "   • Check for resource bottlenecks (CPU, memory, I/O)\n"
                "   • Consider horizontal scaling or load balancing\n"
                "   • Review database connection pool sizing"
            )
        
        if "resource_usage" in alert_types:
            recommendations.append(
                "🔧 Resource Usage Issues Detected:\n"
                "   • Review JVM heap size and garbage collection settings\n"
                "   • Check for memory leaks in application code\n"
                "   • Consider CPU-intensive operations optimization\n"
                "   • Review container resource limits and requests"
            )
        
        return recommendations
    
    def create_alert_report(self, test_name: str, alerts: List[Dict[str, Any]], 
                           recommendations: List[str]) -> str:
        """Create a comprehensive alert report."""
        report_file = Path(f"staging-tests/reports/alert-report-{test_name}-{datetime.now().strftime('%Y%m%d_%H%M%S')}.md")
        report_file.parent.mkdir(parents=True, exist_ok=True)
        
        # Generate report content
        content = f"""# Performance Alert Report

## Test Information
- **Test Name**: {test_name}
- **Report Generated**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
- **Total Alerts**: {len(alerts)}

## Alert Summary

"""
        
        # Group alerts by severity
        critical_alerts = [a for a in alerts if a["severity"] == "critical"]
        warning_alerts = [a for a in alerts if a["severity"] == "warning"]
        info_alerts = [a for a in alerts if a["severity"] == "info"]
        
        if critical_alerts:
            content += f"### 🚨 Critical Alerts ({len(critical_alerts)})\n\n"
            for alert in critical_alerts:
                content += f"- **{alert['metric']}**: {alert['message']}\n"
            content += "\n"
        
        if warning_alerts:
            content += f"### ⚠️ Warning Alerts ({len(warning_alerts)})\n\n"
            for alert in warning_alerts:
                content += f"- **{alert['metric']}**: {alert['message']}\n"
            content += "\n"
        
        if info_alerts:
            content += f"### ℹ️ Info Alerts ({len(info_alerts)})\n\n"
            for alert in info_alerts:
                content += f"- **{alert['metric']}**: {alert['message']}\n"
            content += "\n"
        
        # Add recommendations
        if recommendations:
            content += "## Recommendations\n\n"
            for rec in recommendations:
                content += f"{rec}\n\n"
        
        # Add detailed alert information
        content += "## Detailed Alert Information\n\n"
        for i, alert in enumerate(alerts, 1):
            content += f"### Alert {i}: {alert['metric']}\n\n"
            content += f"- **Severity**: {alert['severity']}\n"
            content += f"- **Type**: {alert['type']}\n"
            content += f"- **Message**: {alert['message']}\n"
            content += f"- **Timestamp**: {alert['timestamp']}\n"
            
            if "current_value" in alert:
                content += f"- **Current Value**: {alert['current_value']}\n"
            if "threshold" in alert:
                content += f"- **Threshold**: {alert['threshold']}\n"
            if "baseline_value" in alert:
                content += f"- **Baseline Value**: {alert['baseline_value']}\n"
            
            content += "\n"
        
        # Save report
        with open(report_file, 'w') as f:
            f.write(content)
        
        logger.info(f"Alert report saved to {report_file}")
        return str(report_file)

def main():
    """Main entry point for the performance alerting system."""
    parser = argparse.ArgumentParser(description="Performance Alerting System")
    parser.add_argument("--test-name", required=True,
                       help="Name of the test scenario")
    parser.add_argument("--results-file", required=True,
                       help="Path to test results JSON file")
    parser.add_argument("--regression-file",
                       help="Path to regression analysis results")
    parser.add_argument("--config", 
                       help="Path to alerting configuration file")
    parser.add_argument("--output-report",
                       help="Path to save alert report")
    parser.add_argument("--verbose", "-v", action="store_true",
                       help="Enable verbose logging")
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Create alerting system
    alerting = PerformanceAlerting(config_file=Path(args.config) if args.config else None)
    
    try:
        # Load test results
        with open(args.results_file, 'r') as f:
            test_results = json.load(f)
        
        # Check thresholds
        threshold_alerts = alerting.check_thresholds(args.test_name, test_results)
        
        # Check regressions if regression file provided
        regression_alerts = []
        if args.regression_file:
            with open(args.regression_file, 'r') as f:
                regression_results = json.load(f)
            regression_alerts = alerting.check_regressions(args.test_name, regression_results)
        
        # Combine all alerts
        all_alerts = threshold_alerts + regression_alerts
        
        if all_alerts:
            logger.info(f"Found {len(all_alerts)} performance alerts")
            
            # Send alerts
            success = alerting.send_alerts(all_alerts)
            
            # Generate recommendations
            recommendations = alerting.generate_performance_recommendations(all_alerts)
            
            # Create alert report
            if args.output_report or recommendations:
                report_file = alerting.create_alert_report(args.test_name, all_alerts, recommendations)
                if args.output_report:
                    # Copy to specified location
                    import shutil
                    shutil.copy2(report_file, args.output_report)
            
            # Exit with error code if critical alerts found
            critical_alerts = [a for a in all_alerts if a["severity"] == "critical"]
            if critical_alerts:
                logger.error(f"Found {len(critical_alerts)} critical performance issues")
                sys.exit(1)
            else:
                logger.warning(f"Found {len(all_alerts)} performance warnings")
                sys.exit(0)
        
        else:
            logger.info("No performance alerts detected")
            sys.exit(0)
    
    except Exception as e:
        logger.error(f"Failed to process performance alerts: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()