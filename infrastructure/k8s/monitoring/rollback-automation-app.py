#!/usr/bin/env python3
"""
Rollback Automation Service for GenAI Demo
This service monitors deployment health metrics and triggers automated rollbacks
when thresholds are exceeded.
"""

import os
import time
import yaml
import logging
import requests
from datetime import datetime, timedelta
from typing import Dict, List, Optional
from dataclasses import dataclass
from kubernetes import client, config
from prometheus_api_client import PrometheusConnect

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

@dataclass
class MetricThreshold:
    name: str
    query: str
    threshold: float
    comparison: str = "less_than"  # less_than, greater_than

@dataclass
class Application:
    name: str
    type: str
    namespace: str
    deployment_strategy: str
    metrics: List[MetricThreshold]

class RollbackAutomation:
    def __init__(self, config_path: str):
        self.config_path = config_path
        self.config = self._load_config()
        self.prometheus = self._init_prometheus()
        self.k8s_client = self._init_kubernetes()
        self.failure_counts = {}
        self.last_rollback_time = {}
        
    def _load_config(self) -> Dict:
        """Load configuration from YAML file"""
        try:
            with open(self.config_path, 'r') as f:
                return yaml.safe_load(f)
        except Exception as e:
            logger.error(f"Failed to load config: {e}")
            raise
    
    def _init_prometheus(self) -> PrometheusConnect:
        """Initialize Prometheus connection"""
        prometheus_url = os.getenv('PROMETHEUS_URL', 'http://prometheus.monitoring.svc.cluster.local:9090')
        return PrometheusConnect(url=prometheus_url)
    
    def _init_kubernetes(self) -> client.ApiClient:
        """Initialize Kubernetes client"""
        try:
            config.load_incluster_config()
        except:
            config.load_kube_config()
        return client.ApiClient()
    
    def check_metric(self, metric: MetricThreshold) -> tuple[bool, float]:
        """Check if a metric exceeds its threshold"""
        try:
            result = self.prometheus.custom_query(metric.query)
            if not result:
                logger.warning(f"No data returned for metric: {metric.name}")
                return False, 0.0
            
            value = float(result[0]['value'][1])
            
            if metric.comparison == "less_than":
                threshold_exceeded = value < metric.threshold
            else:  # greater_than
                threshold_exceeded = value > metric.threshold
            
            logger.debug(f"Metric {metric.name}: {value} (threshold: {metric.threshold}, exceeded: {threshold_exceeded})")
            return threshold_exceeded, value
            
        except Exception as e:
            logger.error(f"Failed to check metric {metric.name}: {e}")
            return False, 0.0
    
    def should_rollback(self, app: Application) -> tuple[bool, str]:
        """Determine if an application should be rolled back"""
        app_key = f"{app.namespace}/{app.name}"
        
        # Check cooldown period
        cooldown_minutes = self.config['rollback']['intervals'].get('cooldown_period', '10m')
        cooldown_seconds = self._parse_duration(cooldown_minutes)
        
        if app_key in self.last_rollback_time:
            time_since_last = datetime.now() - self.last_rollback_time[app_key]
            if time_since_last.total_seconds() < cooldown_seconds:
                logger.debug(f"Application {app_key} in cooldown period")
                return False, "cooldown"
        
        # Initialize failure count if not exists
        if app_key not in self.failure_counts:
            self.failure_counts[app_key] = 0
        
        # Check metrics
        failed_metrics = []
        for metric in app.metrics:
            threshold_exceeded, value = self.check_metric(metric)
            if threshold_exceeded:
                failed_metrics.append(f"{metric.name}={value}")
        
        if failed_metrics:
            self.failure_counts[app_key] += 1
            consecutive_failures = self.config['rollback']['triggers']['consecutive_failures']
            
            if self.failure_counts[app_key] >= consecutive_failures:
                reason = f"Consecutive failures: {', '.join(failed_metrics)}"
                return True, reason
        else:
            # Reset failure count on success
            self.failure_counts[app_key] = 0
        
        return False, "healthy"
    
    def trigger_rollback(self, app: Application, reason: str) -> bool:
        """Trigger rollback for an application"""
        app_key = f"{app.namespace}/{app.name}"
        
        try:
            if app.type == "rollout":
                success = self._rollback_rollout(app, reason)
            else:
                success = self._rollback_deployment(app, reason)
            
            if success:
                self.last_rollback_time[app_key] = datetime.now()
                self.failure_counts[app_key] = 0
                self._send_notification(app, reason, "rollback_triggered")
                logger.info(f"Successfully triggered rollback for {app_key}: {reason}")
            else:
                logger.error(f"Failed to trigger rollback for {app_key}")
            
            return success
            
        except Exception as e:
            logger.error(f"Error triggering rollback for {app_key}: {e}")
            return False
    
    def _rollback_rollout(self, app: Application, reason: str) -> bool:
        """Rollback an Argo Rollout"""
        try:
            # Use kubectl command to abort rollout
            import subprocess
            cmd = [
                "kubectl", "argo", "rollouts", "abort", app.name,
                "-n", app.namespace
            ]
            result = subprocess.run(cmd, capture_output=True, text=True)
            
            if result.returncode == 0:
                logger.info(f"Aborted rollout {app.name} in {app.namespace}")
                return True
            else:
                logger.error(f"Failed to abort rollout: {result.stderr}")
                return False
                
        except Exception as e:
            logger.error(f"Error aborting rollout: {e}")
            return False
    
    def _rollback_deployment(self, app: Application, reason: str) -> bool:
        """Rollback a standard Kubernetes deployment"""
        try:
            apps_v1 = client.AppsV1Api(self.k8s_client)
            
            # Get deployment
            deployment = apps_v1.read_namespaced_deployment(
                name=app.name,
                namespace=app.namespace
            )
            
            # Trigger rollback by updating annotation
            if not deployment.metadata.annotations:
                deployment.metadata.annotations = {}
            
            deployment.metadata.annotations['deployment.kubernetes.io/rollback-reason'] = reason
            deployment.metadata.annotations['deployment.kubernetes.io/rollback-timestamp'] = datetime.now().isoformat()
            
            # Update deployment
            apps_v1.patch_namespaced_deployment(
                name=app.name,
                namespace=app.namespace,
                body=deployment
            )
            
            logger.info(f"Triggered rollback for deployment {app.name} in {app.namespace}")
            return True
            
        except Exception as e:
            logger.error(f"Error rolling back deployment: {e}")
            return False
    
    def _send_notification(self, app: Application, reason: str, event_type: str):
        """Send notification about rollback event"""
        try:
            slack_webhook = os.getenv('SLACK_WEBHOOK_URL')
            if not slack_webhook:
                logger.debug("No Slack webhook configured")
                return
            
            message = {
                "text": f"🔄 Automated Rollback Triggered",
                "attachments": [
                    {
                        "color": "warning",
                        "fields": [
                            {"title": "Application", "value": app.name, "short": True},
                            {"title": "Namespace", "value": app.namespace, "short": True},
                            {"title": "Strategy", "value": app.deployment_strategy, "short": True},
                            {"title": "Reason", "value": reason, "short": False},
                            {"title": "Timestamp", "value": datetime.now().isoformat(), "short": True}
                        ]
                    }
                ]
            }
            
            response = requests.post(slack_webhook, json=message, timeout=10)
            if response.status_code == 200:
                logger.info("Notification sent successfully")
            else:
                logger.warning(f"Failed to send notification: {response.status_code}")
                
        except Exception as e:
            logger.error(f"Error sending notification: {e}")
    
    def _parse_duration(self, duration_str: str) -> int:
        """Parse duration string (e.g., '5m', '30s') to seconds"""
        if duration_str.endswith('s'):
            return int(duration_str[:-1])
        elif duration_str.endswith('m'):
            return int(duration_str[:-1]) * 60
        elif duration_str.endswith('h'):
            return int(duration_str[:-1]) * 3600
        else:
            return int(duration_str)
    
    def run_monitoring_loop(self):
        """Main monitoring loop"""
        logger.info("Starting rollback automation monitoring...")
        
        check_interval = self._parse_duration(
            self.config['rollback']['intervals'].get('check_interval', '30s')
        )
        
        while True:
            try:
                for app_config in self.config['rollback']['applications']:
                    app = Application(
                        name=app_config['name'],
                        type=app_config['type'],
                        namespace=app_config['namespace'],
                        deployment_strategy=app_config['deployment_strategy'],
                        metrics=[
                            MetricThreshold(
                                name=m['name'],
                                query=m['query'],
                                threshold=m['threshold'],
                                comparison="greater_than" if "error" in m['name'] else "less_than"
                            )
                            for m in app_config['metrics']
                        ]
                    )
                    
                    should_rollback, reason = self.should_rollback(app)
                    if should_rollback:
                        logger.warning(f"Triggering rollback for {app.name}: {reason}")
                        self.trigger_rollback(app, reason)
                
                time.sleep(check_interval)
                
            except KeyboardInterrupt:
                logger.info("Received interrupt signal, shutting down...")
                break
            except Exception as e:
                logger.error(f"Error in monitoring loop: {e}")
                time.sleep(check_interval)

def main():
    """Main entry point"""
    config_path = os.getenv('CONFIG_PATH', '/etc/config/config.yaml')
    
    try:
        automation = RollbackAutomation(config_path)
        automation.run_monitoring_loop()
    except Exception as e:
        logger.error(f"Failed to start rollback automation: {e}")
        exit(1)

if __name__ == "__main__":
    main()