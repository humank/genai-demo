#!/usr/bin/env python3
"""
Real-time Performance Monitoring Dashboard

This script provides a real-time monitoring dashboard for performance tests,
displaying live metrics and system status during test execution.
"""

import argparse
import json
import logging
import signal
import sys
import threading
import time
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Any
import requests
import psutil
import os

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class RealtimeMonitor:
    """
    Real-time performance monitoring dashboard for load testing.
    """
    
    def __init__(self, target_host: str = "localhost:8080", refresh_interval: int = 5):
        self.target_host = target_host
        self.refresh_interval = refresh_interval
        self.monitoring = False
        self.metrics_history: List[Dict[str, Any]] = []
        self.max_history = 100  # Keep last 100 data points
        
        # Setup signal handlers
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)
    
    def _signal_handler(self, signum, frame):
        """Handle shutdown signals gracefully."""
        logger.info(f"Received signal {signum}, shutting down...")
        self.stop_monitoring()
        sys.exit(0)
    
    def collect_current_metrics(self) -> Dict[str, Any]:
        """Collect current system and application metrics."""
        metrics = {
            "timestamp": datetime.now().isoformat(),
            "system": self._get_system_metrics(),
            "application": self._get_application_metrics()
        }
        
        # Add to history
        self.metrics_history.append(metrics)
        if len(self.metrics_history) > self.max_history:
            self.metrics_history.pop(0)
        
        return metrics
    
    def _get_system_metrics(self) -> Dict[str, Any]:
        """Get current system metrics."""
        try:
            cpu_percent = psutil.cpu_percent(interval=1)
            memory = psutil.virtual_memory()
            disk = psutil.disk_usage('/')
            network = psutil.net_io_counters()
            
            return {
                "cpu_percent": cpu_percent,
                "memory_percent": memory.percent,
                "memory_used_gb": memory.used / (1024**3),
                "memory_total_gb": memory.total / (1024**3),
                "disk_percent": (disk.used / disk.total) * 100,
                "disk_used_gb": disk.used / (1024**3),
                "disk_total_gb": disk.total / (1024**3),
                "network_bytes_sent": network.bytes_sent,
                "network_bytes_recv": network.bytes_recv
            }
        except Exception as e:
            logger.error(f"Error collecting system metrics: {e}")
            return {"error": str(e)}
    
    def _get_application_metrics(self) -> Dict[str, Any]:
        """Get current application metrics."""
        metrics = {}
        
        try:
            # Health check
            health_response = requests.get(
                f"http://{self.target_host}/actuator/health",
                timeout=3
            )
            metrics["health_status"] = health_response.status_code
            metrics["health_response_time"] = health_response.elapsed.total_seconds() * 1000
            
            if health_response.status_code == 200:
                health_data = health_response.json()
                metrics["health_data"] = health_data
        except Exception as e:
            metrics["health_error"] = str(e)
        
        # JVM metrics
        jvm_metrics = [
            "jvm.memory.used",
            "jvm.memory.max",
            "system.cpu.usage",
            "process.cpu.usage",
            "hikaricp.connections.active",
            "tomcat.sessions.active.current"
        ]
        
        for metric in jvm_metrics:
            try:
                response = requests.get(
                    f"http://{self.target_host}/actuator/metrics/{metric}",
                    timeout=3
                )
                if response.status_code == 200:
                    data = response.json()
                    if "measurements" in data and data["measurements"]:
                        metrics[metric.replace(".", "_")] = data["measurements"][0]["value"]
            except Exception:
                pass  # Ignore individual metric failures
        
        return metrics
    
    def display_dashboard(self):
        """Display the real-time monitoring dashboard."""
        while self.monitoring:
            try:
                # Clear screen
                os.system('clear' if os.name == 'posix' else 'cls')
                
                # Collect current metrics
                current_metrics = self.collect_current_metrics()
                
                # Display header
                print("=" * 80)
                print(f"REAL-TIME PERFORMANCE MONITOR - {current_metrics['timestamp']}")
                print(f"Target: {self.target_host} | Refresh: {self.refresh_interval}s")
                print("=" * 80)
                
                # Display system metrics
                self._display_system_section(current_metrics.get("system", {}))
                
                # Display application metrics
                self._display_application_section(current_metrics.get("application", {}))
                
                # Display trends
                self._display_trends_section()
                
                # Display alerts
                self._display_alerts_section(current_metrics)
                
                print("=" * 80)
                print("Press Ctrl+C to stop monitoring")
                
                time.sleep(self.refresh_interval)
                
            except KeyboardInterrupt:
                break
            except Exception as e:
                logger.error(f"Error in dashboard display: {e}")
                time.sleep(self.refresh_interval)
    
    def _display_system_section(self, system_metrics: Dict[str, Any]):
        """Display system metrics section."""
        print("\n📊 SYSTEM METRICS")
        print("-" * 40)
        
        if "error" in system_metrics:
            print(f"❌ Error: {system_metrics['error']}")
            return
        
        cpu = system_metrics.get("cpu_percent", 0)
        memory = system_metrics.get("memory_percent", 0)
        disk = system_metrics.get("disk_percent", 0)
        
        # CPU
        cpu_bar = self._create_progress_bar(cpu, 100)
        cpu_status = self._get_status_emoji(cpu, 70, 90)
        print(f"CPU Usage:    {cpu_status} {cpu:5.1f}% {cpu_bar}")
        
        # Memory
        memory_bar = self._create_progress_bar(memory, 100)
        memory_status = self._get_status_emoji(memory, 70, 85)
        memory_used = system_metrics.get("memory_used_gb", 0)
        memory_total = system_metrics.get("memory_total_gb", 0)
        print(f"Memory Usage: {memory_status} {memory:5.1f}% {memory_bar} ({memory_used:.1f}/{memory_total:.1f} GB)")
        
        # Disk
        disk_bar = self._create_progress_bar(disk, 100)
        disk_status = self._get_status_emoji(disk, 80, 90)
        disk_used = system_metrics.get("disk_used_gb", 0)
        disk_total = system_metrics.get("disk_total_gb", 0)
        print(f"Disk Usage:   {disk_status} {disk:5.1f}% {disk_bar} ({disk_used:.1f}/{disk_total:.1f} GB)")
    
    def _display_application_section(self, app_metrics: Dict[str, Any]):
        """Display application metrics section."""
        print("\n🚀 APPLICATION METRICS")
        print("-" * 40)
        
        # Health status
        health_status = app_metrics.get("health_status", 0)
        health_response_time = app_metrics.get("health_response_time", 0)
        
        if health_status == 200:
            print(f"Health Check:  ✅ UP ({health_response_time:.0f}ms)")
        else:
            print(f"Health Check:  ❌ DOWN (Status: {health_status})")
        
        # JVM Memory
        jvm_memory_used = app_metrics.get("jvm_memory_used", 0)
        jvm_memory_max = app_metrics.get("jvm_memory_max", 1)
        if jvm_memory_max > 0:
            jvm_memory_percent = (jvm_memory_used / jvm_memory_max) * 100
            jvm_memory_bar = self._create_progress_bar(jvm_memory_percent, 100)
            jvm_status = self._get_status_emoji(jvm_memory_percent, 70, 85)
            print(f"JVM Memory:    {jvm_status} {jvm_memory_percent:5.1f}% {jvm_memory_bar} ({jvm_memory_used/(1024**3):.1f}/{jvm_memory_max/(1024**3):.1f} GB)")
        
        # CPU Usage
        system_cpu = app_metrics.get("system_cpu_usage", 0) * 100
        process_cpu = app_metrics.get("process_cpu_usage", 0) * 100
        if system_cpu > 0:
            cpu_bar = self._create_progress_bar(system_cpu, 100)
            cpu_status = self._get_status_emoji(system_cpu, 70, 90)
            print(f"App CPU:       {cpu_status} {system_cpu:5.1f}% {cpu_bar} (Process: {process_cpu:.1f}%)")
        
        # Database connections
        db_connections = app_metrics.get("hikaricp_connections_active", 0)
        if db_connections > 0:
            print(f"DB Connections: 🔗 {db_connections} active")
        
        # Active sessions
        active_sessions = app_metrics.get("tomcat_sessions_active_current", 0)
        if active_sessions > 0:
            print(f"Active Sessions: 👥 {active_sessions}")
    
    def _display_trends_section(self):
        """Display trends based on historical data."""
        if len(self.metrics_history) < 2:
            return
        
        print("\n📈 TRENDS (Last 5 minutes)")
        print("-" * 40)
        
        # Calculate trends
        recent_metrics = self.metrics_history[-min(10, len(self.metrics_history)):]
        
        # CPU trend
        cpu_values = [m.get("system", {}).get("cpu_percent", 0) for m in recent_metrics]
        cpu_trend = self._calculate_trend(cpu_values)
        print(f"CPU Trend:     {self._get_trend_emoji(cpu_trend)} {cpu_trend:+.1f}%/min")
        
        # Memory trend
        memory_values = [m.get("system", {}).get("memory_percent", 0) for m in recent_metrics]
        memory_trend = self._calculate_trend(memory_values)
        print(f"Memory Trend:  {self._get_trend_emoji(memory_trend)} {memory_trend:+.1f}%/min")
        
        # Response time trend
        response_times = [m.get("application", {}).get("health_response_time", 0) for m in recent_metrics]
        response_trend = self._calculate_trend(response_times)
        print(f"Response Time: {self._get_trend_emoji(response_trend)} {response_trend:+.1f}ms/min")
    
    def _display_alerts_section(self, current_metrics: Dict[str, Any]):
        """Display alerts based on current metrics."""
        alerts = []
        
        system = current_metrics.get("system", {})
        app = current_metrics.get("application", {})
        
        # System alerts
        if system.get("cpu_percent", 0) > 90:
            alerts.append("🚨 HIGH CPU USAGE (>90%)")
        elif system.get("cpu_percent", 0) > 70:
            alerts.append("⚠️  Elevated CPU usage (>70%)")
        
        if system.get("memory_percent", 0) > 85:
            alerts.append("🚨 HIGH MEMORY USAGE (>85%)")
        elif system.get("memory_percent", 0) > 70:
            alerts.append("⚠️  Elevated memory usage (>70%)")
        
        # Application alerts
        if app.get("health_status", 200) != 200:
            alerts.append("🚨 APPLICATION HEALTH CHECK FAILED")
        
        if app.get("health_response_time", 0) > 5000:
            alerts.append("🚨 SLOW HEALTH CHECK RESPONSE (>5s)")
        elif app.get("health_response_time", 0) > 2000:
            alerts.append("⚠️  Slow health check response (>2s)")
        
        # JVM alerts
        jvm_memory_used = app.get("jvm_memory_used", 0)
        jvm_memory_max = app.get("jvm_memory_max", 1)
        if jvm_memory_max > 0:
            jvm_memory_percent = (jvm_memory_used / jvm_memory_max) * 100
            if jvm_memory_percent > 85:
                alerts.append("🚨 HIGH JVM MEMORY USAGE (>85%)")
            elif jvm_memory_percent > 70:
                alerts.append("⚠️  Elevated JVM memory usage (>70%)")
        
        # Display alerts
        if alerts:
            print("\n🚨 ALERTS")
            print("-" * 40)
            for alert in alerts:
                print(alert)
        else:
            print("\n✅ NO ALERTS - System operating normally")
    
    def _create_progress_bar(self, value: float, max_value: float, width: int = 20) -> str:
        """Create a text-based progress bar."""
        if max_value == 0:
            return "█" * width
        
        filled = int((value / max_value) * width)
        bar = "█" * filled + "░" * (width - filled)
        return f"[{bar}]"
    
    def _get_status_emoji(self, value: float, warning_threshold: float, critical_threshold: float) -> str:
        """Get status emoji based on value and thresholds."""
        if value >= critical_threshold:
            return "🔴"
        elif value >= warning_threshold:
            return "🟡"
        else:
            return "🟢"
    
    def _calculate_trend(self, values: List[float]) -> float:
        """Calculate trend (change per minute) from a list of values."""
        if len(values) < 2:
            return 0.0
        
        # Simple linear trend calculation
        n = len(values)
        x_sum = sum(range(n))
        y_sum = sum(values)
        xy_sum = sum(i * values[i] for i in range(n))
        x2_sum = sum(i * i for i in range(n))
        
        if n * x2_sum - x_sum * x_sum == 0:
            return 0.0
        
        slope = (n * xy_sum - x_sum * y_sum) / (n * x2_sum - x_sum * x_sum)
        
        # Convert to per-minute rate (assuming 5-second intervals)
        return slope * 12  # 12 intervals per minute
    
    def _get_trend_emoji(self, trend: float) -> str:
        """Get trend emoji based on trend value."""
        if trend > 1:
            return "📈"
        elif trend < -1:
            return "📉"
        else:
            return "➡️"
    
    def start_monitoring(self):
        """Start the real-time monitoring dashboard."""
        logger.info(f"Starting real-time monitoring for {self.target_host}")
        self.monitoring = True
        
        try:
            self.display_dashboard()
        except KeyboardInterrupt:
            logger.info("Monitoring interrupted by user")
        finally:
            self.stop_monitoring()
    
    def stop_monitoring(self):
        """Stop the monitoring dashboard."""
        self.monitoring = False
        logger.info("Real-time monitoring stopped")

def main():
    """Main entry point for the real-time monitor."""
    parser = argparse.ArgumentParser(description="Real-time Performance Monitoring Dashboard")
    parser.add_argument("--host", default="localhost:8080",
                       help="Target host for monitoring (default: localhost:8080)")
    parser.add_argument("--refresh", type=int, default=5,
                       help="Refresh interval in seconds (default: 5)")
    parser.add_argument("--verbose", "-v", action="store_true",
                       help="Enable verbose logging")
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Create and start the monitor
    monitor = RealtimeMonitor(target_host=args.host, refresh_interval=args.refresh)
    
    try:
        monitor.start_monitoring()
    except Exception as e:
        logger.error(f"Failed to start monitoring: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()