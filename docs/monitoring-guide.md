# Monitoring and Reporting Guide

## Overview

The monitoring and reporting system provides comprehensive visibility into translation system performance, including real-time metrics collection, structured logging, performance analysis, and interactive dashboards.

## Features

### Core Monitoring Capabilities
- **Real-time Metrics**: Live performance tracking with automatic data collection
- **Structured Logging**: Comprehensive logging with JSON format for easy analysis
- **Performance Analytics**: Detailed analysis of success rates, processing times, and trends
- **Error Tracking**: Automatic error categorization and pattern analysis
- **Historical Data**: SQLite database storage for long-term trend analysis

### Reporting and Visualization
- **Interactive Dashboards**: Both terminal-based and web-based dashboards
- **Automated Reports**: JSON, HTML, and CSV report generation
- **Performance Trends**: Historical analysis and trend detection
- **Error Analysis**: Detailed error pattern analysis and troubleshooting guides

## Quick Start

### Basic Monitoring Setup

The monitoring system is automatically initialized when you use the translation scripts:

```bash
# Monitoring is automatically enabled
python scripts/translate-docs.py --all

# View real-time dashboard
python scripts/dashboard.py

# Start web dashboard
python scripts/web_dashboard.py
```

### Generate Reports

```bash
# Generate performance report
python scripts/report_generator.py --performance --hours 24

# Generate HTML dashboard
python scripts/report_generator.py --dashboard --hours 48

# Generate all report types
python scripts/report_generator.py --all --hours 168
```

## Monitoring Components

### 1. Metrics Collector

The `MetricsCollector` automatically tracks:
- Operation start/end times
- File processing counts (successful, failed, skipped)
- Processing times and performance metrics
- Error information and categorization
- System resource usage

```python
from monitoring import track_operation

# Automatic tracking with context manager
with track_operation('my_operation', 'batch') as metrics:
    # Your translation operations
    metrics.files_processed += 1
    metrics.files_successful += 1
```

### 2. Structured Logging

The `TranslationLogger` provides structured logging:

```python
from monitoring import get_translation_logger

logger = get_translation_logger()

# Log operation events
logger.log_operation_start('op_123', 'single_file', {'file': 'README.md'})
logger.log_file_processed('op_123', 'README.md', 'success', 1.5)
logger.log_error('op_123', 'translation_error', 'API timeout', 'README.md')
logger.log_operation_complete('op_123', summary_data)
```

### 3. Database Storage

Metrics are automatically stored in SQLite database (`translation_metrics.db`):

- **translation_metrics**: Operation-level metrics and performance data
- **system_events**: System events and state changes

## Dashboard Options

### 1. Terminal Dashboard

Interactive terminal-based dashboard with real-time updates:

```bash
python scripts/dashboard.py
```

**Features:**
- Real-time metrics display
- Multiple views (Overview, Errors, Performance)
- Keyboard navigation
- Report generation

**Controls:**
- `Q` or `ESC`: Quit
- `1-3`: Switch views
- `R`: Force refresh
- `G`: Generate report
- `↑/↓`: Scroll

### 2. Web Dashboard

Modern web-based dashboard with interactive charts:

```bash
# Start web server
python scripts/web_dashboard.py --port 8080

# Access dashboard
open http://localhost:8080
```

**Features:**
- Interactive charts and visualizations
- Auto-refresh every 30 seconds
- Responsive design
- API endpoints for integration

**Endpoints:**
- `/`: Main dashboard
- `/api/metrics?hours=24`: JSON metrics API
- `/api/report?type=html&hours=24`: Generate reports

## Report Types

### 1. Performance Report (JSON)

Comprehensive performance analysis with metrics, trends, and recommendations:

```bash
python scripts/report_generator.py --performance --hours 24
```

**Contents:**
- Executive summary with key insights
- Detailed performance metrics
- Error analysis and patterns
- Actionable recommendations
- Performance trends and forecasts

### 2. HTML Dashboard Report

Interactive HTML report with charts and visualizations:

```bash
python scripts/report_generator.py --dashboard --hours 48
```

**Features:**
- Interactive charts (Chart.js)
- Responsive design
- Performance metrics visualization
- Error distribution analysis

### 3. CSV Export

Raw metrics data export for external analysis:

```bash
python scripts/report_generator.py --csv --hours 168
```

**Data Includes:**
- Operation details and timing
- Success/failure rates
- Processing times
- File counts and statistics

### 4. Error Analysis Report

Detailed error analysis with troubleshooting guidance:

```bash
python scripts/report_generator.py --errors --hours 24
```

**Analysis Includes:**
- Error type distribution
- Most common error patterns
- Affected files and patterns
- Troubleshooting recommendations

## Performance Metrics

### Key Performance Indicators (KPIs)

1. **Success Rate**: Percentage of successful translations
   - Target: ≥95% (Excellent), ≥90% (Good), ≥80% (Fair)

2. **Processing Time**: Average time per file/operation
   - Target: ≤2s (Excellent), ≤5s (Good), ≤10s (Fair)

3. **Throughput**: Files processed per minute
   - Varies by file size and complexity

4. **Error Rate**: Percentage of failed operations
   - Target: ≤5% (Excellent), ≤10% (Good), ≤20% (Fair)

### Performance Trends

The system tracks trends in:
- Success rate changes over time
- Processing time improvements/degradation
- Error pattern evolution
- System resource utilization

## Error Monitoring

### Error Categories

1. **Translation Errors**: API failures, network issues
2. **File Errors**: Permission issues, disk space, encoding
3. **Format Errors**: Invalid markdown, unsupported elements
4. **System Errors**: Resource exhaustion, timeouts

### Error Analysis Features

- **Pattern Detection**: Identify recurring error patterns
- **Root Cause Analysis**: Detailed error context and stack traces
- **Troubleshooting Guides**: Automated recommendations based on error types
- **Impact Assessment**: Understand error impact on overall performance

## Integration Examples

### CI/CD Integration

```yaml
# GitHub Actions example
- name: Run Translation with Monitoring
  run: |
    python scripts/translate-docs.py --all --batch
    python scripts/report_generator.py --performance --hours 1 --output-dir reports/
    
- name: Upload Performance Report
  uses: actions/upload-artifact@v3
  with:
    name: translation-performance-report
    path: reports/
```

### Custom Monitoring Integration

```python
from monitoring import get_metrics_collector, get_translation_logger

# Custom metrics collection
collector = get_metrics_collector()
logger = get_translation_logger()

# Start custom operation tracking
with track_operation('custom_batch', 'batch') as metrics:
    for file_path in files_to_process:
        try:
            # Your custom translation logic
            result = custom_translate(file_path)
            
            if result.success:
                metrics.files_successful += 1
                logger.log_file_processed(metrics.operation_id, file_path, 'success')
            else:
                metrics.files_failed += 1
                logger.log_error(metrics.operation_id, 'custom_error', result.error, file_path)
                
        except Exception as e:
            metrics.files_failed += 1
            logger.log_error(metrics.operation_id, 'exception', str(e), file_path, e)
```

### Alert Integration

```python
from monitoring import get_metrics_collector

def check_system_health():
    collector = get_metrics_collector()
    summary = collector.get_performance_summary(1)  # Last hour
    
    # Check success rate
    if summary['overall_success_rate'] < 80:
        send_alert(f"Low success rate: {summary['overall_success_rate']:.1f}%")
    
    # Check error patterns
    error_analysis = summary.get('error_analysis', {})
    most_common_errors = error_analysis.get('most_common_errors', [])
    
    if most_common_errors and most_common_errors[0][1] > 10:  # More than 10 errors
        send_alert(f"High error rate: {most_common_errors[0][0]} ({most_common_errors[0][1]} errors)")

# Run health check periodically
import schedule
schedule.every(15).minutes.do(check_system_health)
```

## Configuration

### Monitoring Configuration

```python
from monitoring import setup_monitoring

# Custom monitoring setup
setup_monitoring(
    log_file='custom_translation.log',
    db_path='custom_metrics.db'
)
```

### Dashboard Configuration

```bash
# Terminal dashboard with custom update interval
python scripts/dashboard.py --update-interval 5.0

# Web dashboard on custom host/port
python scripts/web_dashboard.py --host 0.0.0.0 --port 9090
```

### Report Configuration

```bash
# Generate reports for custom time periods
python scripts/report_generator.py --all --hours 720 --output-dir weekly-reports/

# Generate specific report types
python scripts/report_generator.py --performance --errors --hours 24
```

## Troubleshooting

### Common Issues

#### High Memory Usage
```bash
# Check system resource usage
python scripts/dashboard.py
# Look for memory usage patterns in Performance view
```

#### Database Issues
```bash
# Check database file
ls -la translation_metrics.db

# Reset database if corrupted
rm translation_metrics.db
# Database will be recreated automatically
```

#### Dashboard Not Loading
```bash
# Check if metrics collector is initialized
python -c "from monitoring import get_metrics_collector; print('OK')"

# Check web dashboard port availability
netstat -an | grep 8080
```

### Performance Optimization

1. **Database Maintenance**: Regularly clean old metrics data
2. **Log Rotation**: Implement log rotation for large log files
3. **Resource Monitoring**: Monitor system resources during large operations
4. **Alert Tuning**: Adjust alert thresholds based on system performance

## Best Practices

### Monitoring Best Practices

1. **Regular Review**: Review performance reports weekly
2. **Trend Analysis**: Monitor long-term trends, not just current metrics
3. **Error Investigation**: Investigate error patterns promptly
4. **Capacity Planning**: Use metrics for system capacity planning

### Dashboard Usage

1. **Real-time Monitoring**: Use terminal dashboard for active monitoring
2. **Historical Analysis**: Use web dashboard for trend analysis
3. **Report Sharing**: Generate HTML reports for stakeholder communication
4. **Data Export**: Use CSV exports for detailed analysis

### Integration Guidelines

1. **Automated Reporting**: Set up automated report generation
2. **Alert Configuration**: Configure alerts for critical metrics
3. **CI/CD Integration**: Include performance checks in deployment pipeline
4. **Custom Metrics**: Add custom metrics for specific use cases

This comprehensive monitoring and reporting system provides complete visibility into your translation system's performance, enabling proactive maintenance and continuous improvement.