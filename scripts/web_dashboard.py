#!/usr/bin/env python3
"""
Web Dashboard Server for Translation System

This script provides a simple web-based dashboard for monitoring
translation system performance with real-time updates.
"""

import os
import sys
import json
import threading
import time
from datetime import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
import argparse

# Add current directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from monitoring import get_metrics_collector
from report_generator import ReportGenerator

class DashboardHandler(BaseHTTPRequestHandler):
    """HTTP request handler for the web dashboard."""
    
    def __init__(self, *args, metrics_collector=None, **kwargs):
        self.metrics_collector = metrics_collector or get_metrics_collector()
        self.report_generator = ReportGenerator(self.metrics_collector)
        super().__init__(*args, **kwargs)
    
    def do_GET(self):
        """Handle GET requests."""
        parsed_path = urlparse(self.path)
        path = parsed_path.path
        
        try:
            if path == '/' or path == '/dashboard':
                self._serve_dashboard()
            elif path == '/api/metrics':
                self._serve_metrics_api()
            elif path == '/api/report':
                self._serve_report_api()
            elif path.startswith('/static/'):
                self._serve_static_file(path)
            else:
                self._send_404()
        except Exception as e:
            self._send_error(500, str(e))
    
    def _serve_dashboard(self):
        """Serve the main dashboard HTML."""
        html_content = self._generate_dashboard_html()
        
        self.send_response(200)
        self.send_header('Content-type', 'text/html; charset=utf-8')
        self.send_header('Cache-Control', 'no-cache')
        self.end_headers()
        
        self.wfile.write(html_content.encode('utf-8'))
    
    def _serve_metrics_api(self):
        """Serve metrics data as JSON API."""
        parsed_path = urlparse(self.path)
        query_params = parse_qs(parsed_path.query)
        
        hours = int(query_params.get('hours', [24])[0])
        summary = self.metrics_collector.get_performance_summary(hours)
        
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Cache-Control', 'no-cache')
        self.end_headers()
        
        self.wfile.write(json.dumps(summary, ensure_ascii=False).encode('utf-8'))
    
    def _serve_report_api(self):
        """Generate and serve performance report."""
        parsed_path = urlparse(self.path)
        query_params = parse_qs(parsed_path.query)
        
        hours = int(query_params.get('hours', [24])[0])
        report_type = query_params.get('type', ['performance'])[0]
        
        if report_type == 'html':
            # Generate HTML dashboard
            html_file = self.report_generator.generate_html_dashboard(hours)
            with open(html_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            self.send_response(200)
            self.send_header('Content-type', 'text/html; charset=utf-8')
            self.end_headers()
            self.wfile.write(content.encode('utf-8'))
            
            # Cleanup
            os.remove(html_file)
        else:
            # Generate JSON report
            report_file = self.report_generator.generate_performance_report(hours)
            with open(report_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(content.encode('utf-8'))
            
            # Cleanup
            os.remove(report_file)
    
    def _serve_static_file(self, path):
        """Serve static files (placeholder for future use)."""
        self._send_404()
    
    def _send_404(self):
        """Send 404 Not Found response."""
        self.send_response(404)
        self.send_header('Content-type', 'text/html')
        self.end_headers()
        self.wfile.write(b'<h1>404 Not Found</h1>')
    
    def _send_error(self, code, message):
        """Send error response."""
        self.send_response(code)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        
        error_response = {'error': message, 'code': code}
        self.wfile.write(json.dumps(error_response).encode('utf-8'))
    
    def _generate_dashboard_html(self):
        """Generate the dashboard HTML page."""
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Translation System Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: #f8fafc;
            color: #334155;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 2rem;
            text-align: center;
        }
        .container { max-width: 1200px; margin: 0 auto; padding: 2rem; }
        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 1.5rem;
            margin-bottom: 2rem;
        }
        .metric-card {
            background: white;
            padding: 1.5rem;
            border-radius: 12px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
            border: 1px solid #e2e8f0;
        }
        .metric-value {
            font-size: 2.5rem;
            font-weight: 700;
            color: #3b82f6;
            margin-bottom: 0.5rem;
        }
        .metric-label {
            color: #64748b;
            font-size: 0.875rem;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }
        .charts-section {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
            gap: 2rem;
            margin-bottom: 2rem;
        }
        .chart-card {
            background: white;
            padding: 1.5rem;
            border-radius: 12px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
            border: 1px solid #e2e8f0;
        }
        .chart-title {
            font-size: 1.125rem;
            font-weight: 600;
            margin-bottom: 1rem;
            color: #1e293b;
        }
        .status-section {
            background: white;
            padding: 1.5rem;
            border-radius: 12px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
            border: 1px solid #e2e8f0;
        }
        .status-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0.75rem 0;
            border-bottom: 1px solid #f1f5f9;
        }
        .status-item:last-child { border-bottom: none; }
        .status-good { color: #059669; }
        .status-warning { color: #d97706; }
        .status-error { color: #dc2626; }
        .refresh-btn {
            position: fixed;
            bottom: 2rem;
            right: 2rem;
            background: #3b82f6;
            color: white;
            border: none;
            padding: 1rem;
            border-radius: 50%;
            cursor: pointer;
            box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
            font-size: 1.25rem;
        }
        .refresh-btn:hover { background: #2563eb; }
        .loading { opacity: 0.6; pointer-events: none; }
        .last-updated {
            text-align: center;
            color: #64748b;
            font-size: 0.875rem;
            margin-top: 1rem;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>🔄 Translation System Dashboard</h1>
        <p>Real-time monitoring and performance metrics</p>
    </div>
    
    <div class="container">
        <div id="metrics-grid" class="metrics-grid">
            <!-- Metrics will be loaded here -->
        </div>
        
        <div class="charts-section">
            <div class="chart-card">
                <h3 class="chart-title">Operations by Type</h3>
                <canvas id="operationsChart"></canvas>
            </div>
            <div class="chart-card">
                <h3 class="chart-title">Success Rate Trend</h3>
                <canvas id="successChart"></canvas>
            </div>
        </div>
        
        <div class="status-section">
            <h3 class="chart-title">System Status</h3>
            <div id="status-list">
                <!-- Status items will be loaded here -->
            </div>
        </div>
        
        <div class="last-updated" id="lastUpdated">
            Last updated: Loading...
        </div>
    </div>
    
    <button class="refresh-btn" onclick="refreshData()" title="Refresh Data">
        🔄
    </button>
    
    <script>
        let operationsChart, successChart;
        
        async function loadMetrics() {
            try {
                document.body.classList.add('loading');
                
                const response = await fetch('/api/metrics?hours=24');
                const data = await response.json();
                
                updateMetricsCards(data);
                updateCharts(data);
                updateStatus(data);
                
                document.getElementById('lastUpdated').textContent = 
                    `Last updated: ${new Date().toLocaleString()}`;
                
            } catch (error) {
                console.error('Failed to load metrics:', error);
                document.getElementById('lastUpdated').textContent = 
                    `Error loading data: ${error.message}`;
            } finally {
                document.body.classList.remove('loading');
            }
        }
        
        function updateMetricsCards(data) {
            const metricsGrid = document.getElementById('metrics-grid');
            
            const metrics = [
                { label: 'Total Operations', value: data.total_operations || 0 },
                { label: 'Files Processed', value: data.total_files_processed || 0 },
                { label: 'Success Rate', value: `${(data.overall_success_rate || 0).toFixed(1)}%` },
                { label: 'Avg Processing Time', value: `${(data.average_processing_time || 0).toFixed(2)}s` }
            ];
            
            metricsGrid.innerHTML = metrics.map(metric => `
                <div class="metric-card">
                    <div class="metric-value">${metric.value}</div>
                    <div class="metric-label">${metric.label}</div>
                </div>
            `).join('');
        }
        
        function updateCharts(data) {
            // Operations by Type Chart
            const operationsCtx = document.getElementById('operationsChart').getContext('2d');
            const operationsData = data.operations_by_type || {};
            
            if (operationsChart) operationsChart.destroy();
            
            operationsChart = new Chart(operationsCtx, {
                type: 'doughnut',
                data: {
                    labels: Object.keys(operationsData),
                    datasets: [{
                        data: Object.values(operationsData).map(d => d.count || 0),
                        backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6']
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        legend: { position: 'bottom' }
                    }
                }
            });
            
            // Success Rate Chart (placeholder - would need historical data)
            const successCtx = document.getElementById('successChart').getContext('2d');
            
            if (successChart) successChart.destroy();
            
            successChart = new Chart(successCtx, {
                type: 'line',
                data: {
                    labels: ['6h ago', '4h ago', '2h ago', 'Now'],
                    datasets: [{
                        label: 'Success Rate %',
                        data: [92, 94, 96, data.overall_success_rate || 0],
                        borderColor: '#10b981',
                        backgroundColor: 'rgba(16, 185, 129, 0.1)',
                        tension: 0.4
                    }]
                },
                options: {
                    responsive: true,
                    scales: {
                        y: { beginAtZero: true, max: 100 }
                    }
                }
            });
        }
        
        function updateStatus(data) {
            const statusList = document.getElementById('status-list');
            const trends = data.performance_trends || {};
            
            const statusItems = [
                {
                    label: 'Success Rate Trend',
                    value: (trends.success_rate_trend || 'stable').charAt(0).toUpperCase() + (trends.success_rate_trend || 'stable').slice(1),
                    status: trends.success_rate_trend === 'improving' ? 'good' : 
                           trends.success_rate_trend === 'declining' ? 'error' : 'warning'
                },
                {
                    label: 'Processing Time Trend',
                    value: (trends.processing_time_trend || 'stable').charAt(0).toUpperCase() + (trends.processing_time_trend || 'stable').slice(1),
                    status: trends.processing_time_trend === 'improving' ? 'good' : 
                           trends.processing_time_trend === 'declining' ? 'error' : 'warning'
                },
                {
                    label: 'System Health',
                    value: data.overall_success_rate >= 95 ? 'Excellent' : 
                           data.overall_success_rate >= 90 ? 'Good' : 
                           data.overall_success_rate >= 80 ? 'Fair' : 'Needs Attention',
                    status: data.overall_success_rate >= 90 ? 'good' : 
                           data.overall_success_rate >= 80 ? 'warning' : 'error'
                }
            ];
            
            statusList.innerHTML = statusItems.map(item => `
                <div class="status-item">
                    <span>${item.label}</span>
                    <span class="status-${item.status}">${item.value}</span>
                </div>
            `).join('');
        }
        
        function refreshData() {
            loadMetrics();
        }
        
        // Auto-refresh every 30 seconds
        setInterval(loadMetrics, 30000);
        
        // Initial load
        loadMetrics();
    </script>
</body>
</html>
        """
    
    def log_message(self, format, *args):
        """Override to customize logging."""
        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        print(f"[{timestamp}] {format % args}")

class WebDashboardServer:
    """Web dashboard server for translation system monitoring."""
    
    def __init__(self, host='localhost', port=8080):
        """
        Initialize web dashboard server.
        
        Args:
            host: Server host address
            port: Server port number
        """
        self.host = host
        self.port = port
        self.server = None
        self.metrics_collector = get_metrics_collector()
    
    def start(self):
        """Start the web dashboard server."""
        def handler(*args, **kwargs):
            return DashboardHandler(*args, metrics_collector=self.metrics_collector, **kwargs)
        
        self.server = HTTPServer((self.host, self.port), handler)
        
        print(f"🌐 Starting web dashboard server...")
        print(f"📊 Dashboard URL: http://{self.host}:{self.port}")
        print(f"🔗 API Endpoints:")
        print(f"   - Metrics: http://{self.host}:{self.port}/api/metrics")
        print(f"   - Reports: http://{self.host}:{self.port}/api/report")
        print(f"💡 Press Ctrl+C to stop the server")
        
        try:
            self.server.serve_forever()
        except KeyboardInterrupt:
            print(f"\n🛑 Shutting down web dashboard server...")
            self.stop()
    
    def stop(self):
        """Stop the web dashboard server."""
        if self.server:
            self.server.shutdown()
            self.server.server_close()
            print("✅ Web dashboard server stopped")

def main():
    """Main function for web dashboard server."""
    parser = argparse.ArgumentParser(
        description='Web dashboard server for translation system',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Available Endpoints:
  /                 - Main dashboard page
  /dashboard        - Main dashboard page (alias)
  /api/metrics      - JSON metrics API (supports ?hours=N parameter)
  /api/report       - Generate and download reports (supports ?type=html&hours=N)

Examples:
  # Start server on default port
  python web_dashboard.py

  # Start server on custom port
  python web_dashboard.py --port 9090

  # Start server accessible from network
  python web_dashboard.py --host 0.0.0.0 --port 8080
        """
    )
    
    parser.add_argument('--host', default='localhost',
                       help='Server host address (default: localhost)')
    parser.add_argument('--port', type=int, default=8080,
                       help='Server port number (default: 8080)')
    
    args = parser.parse_args()
    
    try:
        # Initialize and start server
        server = WebDashboardServer(args.host, args.port)
        server.start()
        
        return 0
        
    except KeyboardInterrupt:
        print("\n👋 Server interrupted")
        return 0
    except Exception as e:
        print(f"❌ Server failed: {e}")
        import traceback
        traceback.print_exc()
        return 1

if __name__ == '__main__':
    exit(main())