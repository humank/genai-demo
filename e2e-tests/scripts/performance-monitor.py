#!/usr/bin/env python3
"""
Performance Monitoring Script for Gatling Load Tests

This script monitors system performance metrics during load testing,
collecting real-time data on CPU, memory, network, and application-specific metrics.
"""

import argparse
import json
import logging
import psutil
import requests
import signal
import sys
import threading
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional, Any
import docker
import subprocess

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class PerformanceMonitor:
    """
    Real-time system and application performance monitor for load testing.
    """
    
    def __init__(self, target_host: str = "localhost:8080", output_file: str = "system-metrics.json"):
        self.target_host = target_host
        self.output_file = Path(output_file)
        self.monitoring = False
        self.metrics_data: List[Dict[str, Any]] = []
        self.docker_client = None
        self.container_names = [
            "postgresql", "redis", "kafka", "zookeeper", 
            "prometheus", "grafana", "localstack"
        ]
        
        # Initialize Docker client
        try:
            self.docker_client = docker.from_env()
            logger.info("Docker client initialized successfully")
        except Exception as e:
            logger.warning(f"Could not initialize Docker client: {e}")
        
        # Setup signal handlers for graceful shutdown
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)
    
    def _signal_handler(self, signum, frame):
        """Handle shutdown signals gracefully."""
        logger.info(f"Received signal {signum}, shutting down gracefully...")
        self.stop_monitoring()
        sys.exit(0)
    
    def collect_system_metrics(self) -> Dict[str, Any]:
        """Collect system-level performance metrics."""
        try:
            # CPU metrics
            cpu_percent = psutil.cpu_percent(interval=1)
            cpu_count = psutil.cpu_count()
            cpu_freq = psutil.cpu_freq()
            
            # Memory metrics
            memory = psutil.virtual_memory()
            swap = psutil.swap_memory()
            
            # Disk metrics
            disk_usage = psutil.disk_usage('/')
            disk_io = psutil.disk_io_counters()
            
            # Network metrics
            network_io = psutil.net_io_counters()
            
            # Load average (Unix-like systems)
            load_avg = None
            try:
                load_avg = psutil.getloadavg()
            except AttributeError:
                # Windows doesn't have load average
                pass
            
            return {
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "cpu": {
                    "percent": cpu_percent,
                    "count": cpu_count,
                    "frequency": {
                        "current": cpu_freq.current if cpu_freq else None,
                        "min": cpu_freq.min if cpu_freq else None,
                        "max": cpu_freq.max if cpu_freq else None
                    }
                },
                "memory": {
                    "total": memory.total,
                    "available": memory.available,
                    "percent": memory.percent,
                    "used": memory.used,
                    "free": memory.free,
                    "buffers": getattr(memory, 'buffers', 0),
                    "cached": getattr(memory, 'cached', 0)
                },
                "swap": {
                    "total": swap.total,
                    "used": swap.used,
                    "free": swap.free,
                    "percent": swap.percent
                },
                "disk": {
                    "usage": {
                        "total": disk_usage.total,
                        "used": disk_usage.used,
                        "free": disk_usage.free,
                        "percent": (disk_usage.used / disk_usage.total) * 100
                    },
                    "io": {
                        "read_count": disk_io.read_count if disk_io else 0,
                        "write_count": disk_io.write_count if disk_io else 0,
                        "read_bytes": disk_io.read_bytes if disk_io else 0,
                        "write_bytes": disk_io.write_bytes if disk_io else 0
                    }
                },
                "network": {
                    "bytes_sent": network_io.bytes_sent,
                    "bytes_recv": network_io.bytes_recv,
                    "packets_sent": network_io.packets_sent,
                    "packets_recv": network_io.packets_recv,
                    "errin": network_io.errin,
                    "errout": network_io.errout,
                    "dropin": network_io.dropin,
                    "dropout": network_io.dropout
                },
                "load_average": load_avg
            }
        except Exception as e:
            logger.error(f"Error collecting system metrics: {e}")
            return {"error": str(e), "timestamp": datetime.now(timezone.utc).isoformat()}
    
    def collect_application_metrics(self) -> Dict[str, Any]:
        """Collect application-specific metrics from Spring Boot Actuator."""
        metrics = {}
        
        try:
            # Health check
            health_response = requests.get(
                f"http://{self.target_host}/actuator/health",
                timeout=5
            )
            metrics["health"] = {
                "status": health_response.status_code,
                "response_time_ms": health_response.elapsed.total_seconds() * 1000,
                "data": health_response.json() if health_response.status_code == 200 else None
            }
        except Exception as e:
            metrics["health"] = {"error": str(e)}
        
        # Collect specific metrics
        metric_endpoints = [
            "jvm.memory.used",
            "jvm.memory.max",
            "jvm.gc.pause",
            "system.cpu.usage",
            "process.cpu.usage",
            "hikaricp.connections.active",
            "hikaricp.connections.max",
            "http.server.requests",
            "tomcat.sessions.active.current",
            "tomcat.threads.busy",
            "tomcat.threads.config.max"
        ]
        
        for metric in metric_endpoints:
            try:
                response = requests.get(
                    f"http://{self.target_host}/actuator/metrics/{metric}",
                    timeout=5
                )
                if response.status_code == 200:
                    metrics[metric.replace(".", "_")] = response.json()
            except Exception as e:
                metrics[metric.replace(".", "_")] = {"error": str(e)}
        
        return metrics
    
    def collect_container_metrics(self) -> Dict[str, Any]:
        """Collect Docker container performance metrics."""
        if not self.docker_client:
            return {"error": "Docker client not available"}
        
        container_metrics = {}
        
        try:
            containers = self.docker_client.containers.list()
            
            for container in containers:
                container_name = container.name
                
                # Only monitor specific containers
                if not any(name in container_name.lower() for name in self.container_names):
                    continue
                
                try:
                    # Get container stats
                    stats = container.stats(stream=False)
                    
                    # Calculate CPU usage percentage
                    cpu_delta = stats['cpu_stats']['cpu_usage']['total_usage'] - \
                               stats['precpu_stats']['cpu_usage']['total_usage']
                    system_delta = stats['cpu_stats']['system_cpu_usage'] - \
                                  stats['precpu_stats']['system_cpu_usage']
                    
                    cpu_percent = 0.0
                    if system_delta > 0 and cpu_delta > 0:
                        cpu_percent = (cpu_delta / system_delta) * \
                                     len(stats['cpu_stats']['cpu_usage']['percpu_usage']) * 100.0
                    
                    # Memory usage
                    memory_usage = stats['memory_stats'].get('usage', 0)
                    memory_limit = stats['memory_stats'].get('limit', 0)
                    memory_percent = (memory_usage / memory_limit * 100) if memory_limit > 0 else 0
                    
                    # Network I/O
                    network_rx = 0
                    network_tx = 0
                    if 'networks' in stats:
                        for interface in stats['networks'].values():
                            network_rx += interface.get('rx_bytes', 0)
                            network_tx += interface.get('tx_bytes', 0)
                    
                    # Block I/O
                    block_read = 0
                    block_write = 0
                    if 'blkio_stats' in stats and 'io_service_bytes_recursive' in stats['blkio_stats']:
                        for io_stat in stats['blkio_stats']['io_service_bytes_recursive']:
                            if io_stat['op'] == 'Read':
                                block_read += io_stat['value']
                            elif io_stat['op'] == 'Write':
                                block_write += io_stat['value']
                    
                    container_metrics[container_name] = {
                        "status": container.status,
                        "cpu_percent": round(cpu_percent, 2),
                        "memory": {
                            "usage_bytes": memory_usage,
                            "limit_bytes": memory_limit,
                            "percent": round(memory_percent, 2)
                        },
                        "network": {
                            "rx_bytes": network_rx,
                            "tx_bytes": network_tx
                        },
                        "block_io": {
                            "read_bytes": block_read,
                            "write_bytes": block_write
                        }
                    }
                    
                except Exception as e:
                    container_metrics[container_name] = {"error": str(e)}
        
        except Exception as e:
            logger.error(f"Error collecting container metrics: {e}")
            return {"error": str(e)}
        
        return container_metrics
    
    def collect_database_metrics(self) -> Dict[str, Any]:
        """Collect database-specific performance metrics."""
        db_metrics = {}
        
        # PostgreSQL metrics (if available)
        try:
            # Try to get PostgreSQL metrics via application endpoint
            response = requests.get(
                f"http://{self.target_host}/actuator/metrics/hikaricp.connections.active",
                timeout=5
            )
            if response.status_code == 200:
                db_metrics["postgresql"] = {
                    "connection_pool": response.json()
                }
        except Exception as e:
            db_metrics["postgresql"] = {"error": str(e)}
        
        # Redis metrics (if available)
        try:
            response = requests.get(
                f"http://{self.target_host}/actuator/health/redis",
                timeout=5
            )
            if response.status_code == 200:
                db_metrics["redis"] = response.json()
        except Exception as e:
            db_metrics["redis"] = {"error": str(e)}
        
        return db_metrics
    
    def collect_all_metrics(self) -> Dict[str, Any]:
        """Collect all performance metrics."""
        return {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "system": self.collect_system_metrics(),
            "application": self.collect_application_metrics(),
            "containers": self.collect_container_metrics(),
            "database": self.collect_database_metrics()
        }
    
    def start_monitoring(self, duration: int = 3600, interval: int = 5):
        """Start performance monitoring for specified duration."""
        logger.info(f"Starting performance monitoring for {duration} seconds with {interval}s interval")
        logger.info(f"Target host: {self.target_host}")
        logger.info(f"Output file: {self.output_file}")
        
        self.monitoring = True
        start_time = time.time()
        
        # Create output directory if it doesn't exist
        self.output_file.parent.mkdir(parents=True, exist_ok=True)
        
        try:
            while self.monitoring and (time.time() - start_time) < duration:
                metrics = self.collect_all_metrics()
                self.metrics_data.append(metrics)
                
                # Log summary every 30 seconds
                if len(self.metrics_data) % (30 // interval) == 0:
                    self._log_metrics_summary(metrics)
                
                # Save metrics periodically (every 60 seconds)
                if len(self.metrics_data) % (60 // interval) == 0:
                    self._save_metrics()
                
                time.sleep(interval)
        
        except KeyboardInterrupt:
            logger.info("Monitoring interrupted by user")
        except Exception as e:
            logger.error(f"Error during monitoring: {e}")
        finally:
            self.stop_monitoring()
    
    def stop_monitoring(self):
        """Stop monitoring and save final metrics."""
        if self.monitoring:
            logger.info("Stopping performance monitoring...")
            self.monitoring = False
            self._save_metrics()
            self._generate_summary_report()
    
    def _log_metrics_summary(self, metrics: Dict[str, Any]):
        """Log a summary of current metrics."""
        try:
            system = metrics.get("system", {})
            app_health = metrics.get("application", {}).get("health", {})
            
            logger.info(
                f"System: CPU {system.get('cpu', {}).get('percent', 0):.1f}%, "
                f"Memory {system.get('memory', {}).get('percent', 0):.1f}%, "
                f"App Health: {app_health.get('status', 'Unknown')}"
            )
        except Exception as e:
            logger.error(f"Error logging metrics summary: {e}")
    
    def _save_metrics(self):
        """Save collected metrics to file."""
        try:
            with open(self.output_file, 'w') as f:
                json.dump({
                    "monitoring_session": {
                        "start_time": self.metrics_data[0]["timestamp"] if self.metrics_data else None,
                        "end_time": self.metrics_data[-1]["timestamp"] if self.metrics_data else None,
                        "total_samples": len(self.metrics_data),
                        "target_host": self.target_host
                    },
                    "metrics": self.metrics_data
                }, f, indent=2)
            
            logger.info(f"Saved {len(self.metrics_data)} metric samples to {self.output_file}")
        except Exception as e:
            logger.error(f"Error saving metrics: {e}")
    
    def _generate_summary_report(self):
        """Generate a summary report of the monitoring session."""
        if not self.metrics_data:
            return
        
        try:
            summary_file = self.output_file.with_suffix('.summary.json')
            
            # Calculate summary statistics
            cpu_values = []
            memory_values = []
            response_times = []
            
            for metric in self.metrics_data:
                system = metric.get("system", {})
                if "cpu" in system and "percent" in system["cpu"]:
                    cpu_values.append(system["cpu"]["percent"])
                
                if "memory" in system and "percent" in system["memory"]:
                    memory_values.append(system["memory"]["percent"])
                
                app = metric.get("application", {})
                if "health" in app and "response_time_ms" in app["health"]:
                    response_times.append(app["health"]["response_time_ms"])
            
            def calculate_stats(values):
                if not values:
                    return {}
                return {
                    "min": min(values),
                    "max": max(values),
                    "avg": sum(values) / len(values),
                    "count": len(values)
                }
            
            summary = {
                "session_info": {
                    "start_time": self.metrics_data[0]["timestamp"],
                    "end_time": self.metrics_data[-1]["timestamp"],
                    "duration_minutes": len(self.metrics_data) * 5 / 60,  # Assuming 5s intervals
                    "total_samples": len(self.metrics_data),
                    "target_host": self.target_host
                },
                "performance_summary": {
                    "cpu_percent": calculate_stats(cpu_values),
                    "memory_percent": calculate_stats(memory_values),
                    "health_check_response_time_ms": calculate_stats(response_times)
                }
            }
            
            with open(summary_file, 'w') as f:
                json.dump(summary, f, indent=2)
            
            logger.info(f"Generated summary report: {summary_file}")
            
        except Exception as e:
            logger.error(f"Error generating summary report: {e}")

def main():
    """Main entry point for the performance monitor."""
    parser = argparse.ArgumentParser(description="Performance Monitor for Load Testing")
    parser.add_argument("--host", default="localhost:8080", 
                       help="Target host for application metrics (default: localhost:8080)")
    parser.add_argument("--duration", type=int, default=3600,
                       help="Monitoring duration in seconds (default: 3600)")
    parser.add_argument("--interval", type=int, default=5,
                       help="Metrics collection interval in seconds (default: 5)")
    parser.add_argument("--output", default="system-metrics.json",
                       help="Output file for metrics data (default: system-metrics.json)")
    parser.add_argument("--verbose", "-v", action="store_true",
                       help="Enable verbose logging")
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Create and start the performance monitor
    monitor = PerformanceMonitor(target_host=args.host, output_file=args.output)
    
    try:
        monitor.start_monitoring(duration=args.duration, interval=args.interval)
    except Exception as e:
        logger.error(f"Failed to start monitoring: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()