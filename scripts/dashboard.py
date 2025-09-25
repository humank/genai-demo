#!/usr/bin/env python3
"""
Real-time Dashboard for Translation System

This script provides a real-time command-line dashboard for monitoring
translation system performance, showing live metrics and status updates.
"""

import os
import sys
import time
import json
import curses
import threading
from datetime import datetime, timedelta
from typing import Dict, List, Optional

# Add current directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from monitoring import get_metrics_collector, MetricsCollector
from report_generator import ReportGenerator

class DashboardDisplay:
    """
    Real-time dashboard display using curses.
    
    This class provides a terminal-based dashboard with live updates
    showing system performance metrics and status information.
    """
    
    def __init__(self, metrics_collector: MetricsCollector = None):
        """
        Initialize dashboard display.
        
        Args:
            metrics_collector: MetricsCollector instance
        """
        self.metrics_collector = metrics_collector or get_metrics_collector()
        self.report_generator = ReportGenerator(self.metrics_collector)
        self.running = False
        self.update_interval = 2.0  # seconds
        self.last_update = 0
        
        # Dashboard state
        self.current_view = 'overview'  # 'overview', 'errors', 'performance'
        self.scroll_position = 0
        
        # Colors
        self.colors = {}
    
    def start(self):
        """Start the dashboard display."""
        try:
            curses.wrapper(self._main_loop)
        except KeyboardInterrupt:
            pass
        finally:
            self.running = False
    
    def _main_loop(self, stdscr):
        """Main dashboard loop."""
        self.stdscr = stdscr
        self.running = True
        
        # Initialize colors
        self._init_colors()
        
        # Configure screen
        curses.curs_set(0)  # Hide cursor
        stdscr.nodelay(1)   # Non-blocking input
        stdscr.timeout(100) # 100ms timeout
        
        while self.running:
            try:
                # Handle input
                key = stdscr.getch()
                if key != -1:
                    self._handle_input(key)
                
                # Update display
                current_time = time.time()
                if current_time - self.last_update >= self.update_interval:
                    self._update_display()
                    self.last_update = current_time
                
                time.sleep(0.1)
                
            except Exception as e:
                # Error handling - show error and continue
                self._show_error(str(e))
                time.sleep(1)
    
    def _init_colors(self):
        """Initialize color pairs."""
        if curses.has_colors():
            curses.start_color()
            curses.use_default_colors()
            
            # Define color pairs
            curses.init_pair(1, curses.COLOR_GREEN, -1)   # Success
            curses.init_pair(2, curses.COLOR_RED, -1)     # Error
            curses.init_pair(3, curses.COLOR_YELLOW, -1)  # Warning
            curses.init_pair(4, curses.COLOR_BLUE, -1)    # Info
            curses.init_pair(5, curses.COLOR_CYAN, -1)    # Highlight
            curses.init_pair(6, curses.COLOR_MAGENTA, -1) # Special
            
            self.colors = {
                'success': curses.color_pair(1),
                'error': curses.color_pair(2),
                'warning': curses.color_pair(3),
                'info': curses.color_pair(4),
                'highlight': curses.color_pair(5),
                'special': curses.color_pair(6)
            }
    
    def _handle_input(self, key):
        """Handle keyboard input."""
        if key == ord('q') or key == 27:  # 'q' or ESC
            self.running = False
        elif key == ord('1'):
            self.current_view = 'overview'
        elif key == ord('2'):
            self.current_view = 'errors'
        elif key == ord('3'):
            self.current_view = 'performance'
        elif key == ord('r'):
            # Force refresh
            self.last_update = 0
        elif key == curses.KEY_UP:
            self.scroll_position = max(0, self.scroll_position - 1)
        elif key == curses.KEY_DOWN:
            self.scroll_position += 1
        elif key == ord('g'):
            # Generate report
            self._generate_report()
    
    def _update_display(self):
        """Update the dashboard display."""
        try:
            self.stdscr.clear()
            
            # Get current metrics
            summary = self.metrics_collector.get_performance_summary(24)
            
            # Display header
            self._draw_header()
            
            # Display content based on current view
            if self.current_view == 'overview':
                self._draw_overview(summary)
            elif self.current_view == 'errors':
                self._draw_errors(summary)
            elif self.current_view == 'performance':
                self._draw_performance(summary)
            
            # Display footer
            self._draw_footer()
            
            self.stdscr.refresh()
            
        except Exception as e:
            self._show_error(f"Display update failed: {e}")
    
    def _draw_header(self):
        """Draw dashboard header."""
        height, width = self.stdscr.getmaxyx()
        
        # Title
        title = "🔄 Translation System Dashboard"
        self.stdscr.addstr(0, (width - len(title)) // 2, title, 
                          self.colors.get('highlight', curses.A_BOLD))
        
        # Current time
        current_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        self.stdscr.addstr(1, width - len(current_time) - 1, current_time)
        
        # View tabs
        tabs = [
            ("1", "Overview", self.current_view == 'overview'),
            ("2", "Errors", self.current_view == 'errors'),
            ("3", "Performance", self.current_view == 'performance')
        ]
        
        tab_line = "  ".join([
            f"[{key}] {name}" + ("*" if active else "")
            for key, name, active in tabs
        ])
        
        self.stdscr.addstr(3, 2, tab_line)
        
        # Separator line
        self.stdscr.addstr(4, 0, "─" * width)
    
    def _draw_overview(self, summary: Dict):
        """Draw overview dashboard."""
        start_row = 6
        
        # Key metrics
        metrics = [
            ("Total Operations", summary.get('total_operations', 0)),
            ("Files Processed", summary.get('total_files_processed', 0)),
            ("Success Rate", f"{summary.get('overall_success_rate', 0):.1f}%"),
            ("Avg Processing Time", f"{summary.get('average_processing_time', 0):.2f}s")
        ]
        
        self.stdscr.addstr(start_row, 2, "📊 Key Metrics (Last 24 Hours)", 
                          self.colors.get('info', curses.A_BOLD))
        
        for i, (label, value) in enumerate(metrics):
            row = start_row + 2 + i
            self.stdscr.addstr(row, 4, f"{label}:")
            self.stdscr.addstr(row, 25, str(value), self.colors.get('success'))
        
        # Operations by type
        operations_by_type = summary.get('operations_by_type', {})
        if operations_by_type:
            self.stdscr.addstr(start_row + 7, 2, "🔧 Operations by Type", 
                              self.colors.get('info', curses.A_BOLD))
            
            for i, (op_type, data) in enumerate(operations_by_type.items()):
                row = start_row + 9 + i
                count = data.get('count', 0)
                avg_success = data.get('avg_success_rate', 0)
                self.stdscr.addstr(row, 4, f"{op_type}:")
                self.stdscr.addstr(row, 20, f"{count} ops, {avg_success:.1f}% success")
        
        # Recent activity
        self.stdscr.addstr(start_row + 13, 2, "📈 System Status", 
                          self.colors.get('info', curses.A_BOLD))
        
        # Performance trends
        trends = summary.get('performance_trends', {})
        success_trend = trends.get('success_rate_trend', 'stable')
        time_trend = trends.get('processing_time_trend', 'stable')
        
        trend_color = {
            'improving': self.colors.get('success'),
            'declining': self.colors.get('error'),
            'stable': self.colors.get('info')
        }
        
        self.stdscr.addstr(start_row + 15, 4, "Success Rate Trend:")
        self.stdscr.addstr(start_row + 15, 25, success_trend.title(), 
                          trend_color.get(success_trend, curses.A_NORMAL))
        
        self.stdscr.addstr(start_row + 16, 4, "Processing Time Trend:")
        self.stdscr.addstr(start_row + 16, 25, time_trend.title(), 
                          trend_color.get(time_trend, curses.A_NORMAL))
    
    def _draw_errors(self, summary: Dict):
        """Draw errors dashboard."""
        start_row = 6
        
        error_analysis = summary.get('error_analysis', {})
        error_counts = error_analysis.get('error_counts', {})
        most_common = error_analysis.get('most_common_errors', [])
        
        self.stdscr.addstr(start_row, 2, "❌ Error Analysis (Last 24 Hours)", 
                          self.colors.get('error', curses.A_BOLD))
        
        if not error_counts:
            self.stdscr.addstr(start_row + 2, 4, "No errors detected! 🎉", 
                              self.colors.get('success'))
            return
        
        # Total errors
        total_errors = sum(error_counts.values())
        self.stdscr.addstr(start_row + 2, 4, f"Total Errors: {total_errors}")
        
        # Most common errors
        self.stdscr.addstr(start_row + 4, 2, "🔥 Most Common Errors", 
                          self.colors.get('warning', curses.A_BOLD))
        
        for i, (error_type, count) in enumerate(most_common[:10]):
            row = start_row + 6 + i
            if row < self.stdscr.getmaxyx()[0] - 3:  # Leave space for footer
                percentage = (count / total_errors * 100) if total_errors > 0 else 0
                self.stdscr.addstr(row, 4, f"{error_type}:")
                self.stdscr.addstr(row, 25, f"{count} ({percentage:.1f}%)", 
                                  self.colors.get('error'))
        
        # Error files (if available)
        error_files = error_analysis.get('error_files', {})
        if error_files and most_common:
            top_error_type = most_common[0][0]
            files = error_files.get(top_error_type, [])
            
            if files:
                files_row = start_row + 17
                if files_row < self.stdscr.getmaxyx()[0] - 5:
                    self.stdscr.addstr(files_row, 2, f"📁 Files with {top_error_type} errors:", 
                                      self.colors.get('info', curses.A_BOLD))
                    
                    for i, file_path in enumerate(files[:3]):  # Show first 3 files
                        file_row = files_row + 2 + i
                        if file_row < self.stdscr.getmaxyx()[0] - 3:
                            # Truncate long file paths
                            max_width = self.stdscr.getmaxyx()[1] - 8
                            display_path = file_path if len(file_path) <= max_width else f"...{file_path[-(max_width-3):]}"
                            self.stdscr.addstr(file_row, 4, display_path)
    
    def _draw_performance(self, summary: Dict):
        """Draw performance dashboard."""
        start_row = 6
        
        self.stdscr.addstr(start_row, 2, "⚡ Performance Metrics (Last 24 Hours)", 
                          self.colors.get('info', curses.A_BOLD))
        
        # Performance summary
        total_ops = summary.get('total_operations', 0)
        total_files = summary.get('total_files_processed', 0)
        avg_time = summary.get('average_processing_time', 0)
        success_rate = summary.get('overall_success_rate', 0)
        
        metrics = [
            ("Operations", total_ops),
            ("Files Processed", total_files),
            ("Files per Operation", f"{total_files / max(total_ops, 1):.1f}"),
            ("Average Processing Time", f"{avg_time:.2f}s"),
            ("Overall Success Rate", f"{success_rate:.1f}%")
        ]
        
        for i, (label, value) in enumerate(metrics):
            row = start_row + 2 + i
            self.stdscr.addstr(row, 4, f"{label}:")
            
            # Color code based on performance
            color = curses.A_NORMAL
            if "Success Rate" in label:
                if isinstance(value, str) and value.replace('%', '').replace('.', '').isdigit():
                    rate = float(value.replace('%', ''))
                    color = self.colors.get('success') if rate >= 90 else self.colors.get('warning') if rate >= 80 else self.colors.get('error')
            elif "Processing Time" in label:
                if isinstance(value, str) and 's' in value:
                    time_val = float(value.replace('s', ''))
                    color = self.colors.get('success') if time_val <= 2.0 else self.colors.get('warning') if time_val <= 5.0 else self.colors.get('error')
            
            self.stdscr.addstr(row, 30, str(value), color)
        
        # Performance by operation type
        operations_by_type = summary.get('operations_by_type', {})
        if operations_by_type:
            self.stdscr.addstr(start_row + 9, 2, "📊 Performance by Operation Type", 
                              self.colors.get('info', curses.A_BOLD))
            
            for i, (op_type, data) in enumerate(operations_by_type.items()):
                row = start_row + 11 + i
                if row < self.stdscr.getmaxyx()[0] - 3:
                    count = data.get('count', 0)
                    avg_files = data.get('avg_files_per_operation', 0)
                    avg_success = data.get('avg_success_rate', 0)
                    
                    self.stdscr.addstr(row, 4, f"{op_type}:")
                    self.stdscr.addstr(row, 20, f"{count} ops, {avg_files:.1f} files/op, {avg_success:.1f}% success")
        
        # Performance trends
        trends = summary.get('performance_trends', {})
        if trends:
            trends_row = start_row + 16
            if trends_row < self.stdscr.getmaxyx()[0] - 5:
                self.stdscr.addstr(trends_row, 2, "📈 Performance Trends", 
                                  self.colors.get('info', curses.A_BOLD))
                
                success_trend = trends.get('success_rate_trend', 'stable')
                time_trend = trends.get('processing_time_trend', 'stable')
                
                trend_symbols = {
                    'improving': '📈',
                    'declining': '📉',
                    'stable': '➡️'
                }
                
                self.stdscr.addstr(trends_row + 2, 4, f"Success Rate: {trend_symbols.get(success_trend, '?')} {success_trend.title()}")
                self.stdscr.addstr(trends_row + 3, 4, f"Processing Time: {trend_symbols.get(time_trend, '?')} {time_trend.title()}")
    
    def _draw_footer(self):
        """Draw dashboard footer."""
        height, width = self.stdscr.getmaxyx()
        footer_row = height - 2
        
        # Controls
        controls = "[Q]uit | [1]Overview [2]Errors [3]Performance | [R]efresh | [G]enerate Report"
        if len(controls) <= width:
            self.stdscr.addstr(footer_row, (width - len(controls)) // 2, controls, 
                              self.colors.get('info'))
        else:
            # Truncated version for narrow terminals
            short_controls = "[Q]uit | [1-3]Views | [R]efresh | [G]Report"
            self.stdscr.addstr(footer_row, (width - len(short_controls)) // 2, short_controls, 
                              self.colors.get('info'))
        
        # Status line
        status = f"View: {self.current_view.title()} | Update: {self.update_interval}s"
        self.stdscr.addstr(footer_row + 1, 2, status)
    
    def _show_error(self, error_message: str):
        """Show error message."""
        try:
            height, width = self.stdscr.getmaxyx()
            error_row = height // 2
            
            self.stdscr.addstr(error_row, 2, f"Error: {error_message}", 
                              self.colors.get('error', curses.A_BOLD))
            self.stdscr.refresh()
        except:
            pass  # Ignore errors in error display
    
    def _generate_report(self):
        """Generate and save performance report."""
        try:
            # Show generating message
            height, width = self.stdscr.getmaxyx()
            msg_row = height // 2
            
            self.stdscr.addstr(msg_row, 2, "Generating report...", 
                              self.colors.get('info', curses.A_BOLD))
            self.stdscr.refresh()
            
            # Generate report
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            report_file = f'dashboard_report_{timestamp}.json'
            
            self.report_generator.generate_performance_report(24, report_file)
            
            # Show success message
            self.stdscr.addstr(msg_row + 1, 2, f"Report saved: {report_file}", 
                              self.colors.get('success'))
            self.stdscr.refresh()
            
            time.sleep(2)  # Show message for 2 seconds
            
        except Exception as e:
            self._show_error(f"Report generation failed: {e}")

def main():
    """Main function for dashboard CLI."""
    import argparse
    
    parser = argparse.ArgumentParser(
        description='Real-time translation system dashboard',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Dashboard Controls:
  Q or ESC    - Quit dashboard
  1           - Overview view
  2           - Errors view  
  3           - Performance view
  R           - Force refresh
  G           - Generate report
  ↑/↓         - Scroll (where applicable)

Views:
  Overview    - Key metrics and system status
  Errors      - Error analysis and troubleshooting
  Performance - Detailed performance metrics and trends
        """
    )
    
    parser.add_argument('--update-interval', type=float, default=2.0,
                       help='Update interval in seconds (default: 2.0)')
    
    args = parser.parse_args()
    
    try:
        # Initialize dashboard
        dashboard = DashboardDisplay()
        dashboard.update_interval = args.update_interval
        
        print("🚀 Starting Translation System Dashboard...")
        print("Press 'Q' or ESC to quit")
        time.sleep(1)
        
        # Start dashboard
        dashboard.start()
        
        print("\n👋 Dashboard closed")
        return 0
        
    except KeyboardInterrupt:
        print("\n👋 Dashboard interrupted")
        return 0
    except Exception as e:
        print(f"❌ Dashboard failed: {e}")
        import traceback
        traceback.print_exc()
        return 1

if __name__ == '__main__':
    exit(main())