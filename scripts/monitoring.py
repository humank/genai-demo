#!/usr/bin/env python3
"""
Monitoring and Metrics System for Translation Operations

This module provides comprehensive monitoring, logging, and metrics collection
for the automated documentation translation system.
"""

import os
import sys
import json
import logging
import time
import threading
from pathlib import Path
from typing import Dict, List, Optional, Any, Callable
from datetime import datetime, timedelta
from dataclasses import dataclass, field, asdict
from collections import defaultdict, deque
import sqlite3
from contextlib import contextmanager

# Configure structured logging
class StructuredFormatter(logging.Formatter):
    """Custom formatter for structured logging."""
    
    def format(self, record):
        log_entry = {
            'timestamp': datetime.fromtimestamp(record.created).isoformat(),
            'level': record.levelname,
            'logger': record.name,
            'message': record.getMessage(),
            'module': record.module,
            'function': record.funcName,
            'line': record.lineno
        }
        
        # Add extra fields if present
        if hasattr(record, 'extra_fields'):
            log_entry.update(record.extra_fields)
        
        return json.dumps(log_entry, ensure_ascii=False)

@dataclass
class TranslationMetrics:
    """Metrics for translation operations."""
    operation_id: str
    operation_type: str  # 'single_file', 'batch', 'directory'
    start_time: datetime
    end_time: Optional[datetime] = None
    files_processed: int = 0
    files_successful: int = 0
    files_failed: int = 0
    files_skipped: int = 0
    total_processing_time: float = 0.0
    average_time_per_file: float = 0.0
    errors: List[Dict] = field(default_factory=list)
    performance_data: Dict = field(default_factory=dict)
    
    @property
    def success_rate(self) -> float:
        """Calculate success rate percentage."""
        if self.files_processed == 0:
            return 0.0
        return (self.files_successful / self.files_processed) * 100
    
    @property
    def failure_rate(self) -> float:
        """Calculate failure rate percentage."""
        if self.files_processed == 0:
            return 0.0
        return (self.files_failed / self.files_processed) * 100
    
    @property
    def duration(self) -> Optional[timedelta]:
        """Calculate operation duration."""
        if self.end_time:
            return self.end_time - self.start_time
        return None

class MetricsCollector:
    """
    Centralized metrics collection system.
    
    This class collects, stores, and provides access to various metrics
    about translation operations and system performance.
    """
    
    def __init__(self, db_path: str = "translation_metrics.db"):
        """
        Initialize metrics collector.
        
        Args:
            db_path: Path to SQLite database for metrics storage
        """
        self.db_path = db_path
        self.active_operations = {}
        self.metrics_history = deque(maxlen=1000)  # Keep last 1000 operations
        self.performance_counters = defaultdict(int)
        self.timing_data = defaultdict(list)
        self._lock = threading.Lock()
        
        # Initialize database
        self._init_database()
    
    def _init_database(self):
        """Initialize SQLite database for metrics storage."""
        with sqlite3.connect(self.db_path) as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS translation_metrics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    operation_id TEXT UNIQUE,
                    operation_type TEXT,
                    start_time TEXT,
                    end_time TEXT,
                    files_processed INTEGER,
                    files_successful INTEGER,
                    files_failed INTEGER,
                    files_skipped INTEGER,
                    total_processing_time REAL,
                    average_time_per_file REAL,
                    success_rate REAL,
                    failure_rate REAL,
                    errors TEXT,
                    performance_data TEXT,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP
                )
            """)
            
            conn.execute("""
                CREATE TABLE IF NOT EXISTS system_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_type TEXT,
                    event_data TEXT,
                    timestamp TEXT DEFAULT CURRENT_TIMESTAMP
                )
            """)
            
            conn.execute("""
                CREATE INDEX IF NOT EXISTS idx_operation_type 
                ON translation_metrics(operation_type)
            """)
            
            conn.execute("""
                CREATE INDEX IF NOT EXISTS idx_start_time 
                ON translation_metrics(start_time)
            """)
    
    def start_operation(self, operation_id: str, operation_type: str) -> TranslationMetrics:
        """
        Start tracking a new operation.
        
        Args:
            operation_id: Unique identifier for the operation
            operation_type: Type of operation ('single_file', 'batch', 'directory')
            
        Returns:
            TranslationMetrics instance for the operation
        """
        with self._lock:
            metrics = TranslationMetrics(
                operation_id=operation_id,
                operation_type=operation_type,
                start_time=datetime.now()
            )
            
            self.active_operations[operation_id] = metrics
            self.performance_counters['operations_started'] += 1
            
            # Log operation start
            self._log_system_event('operation_started', {
                'operation_id': operation_id,
                'operation_type': operation_type,
                'start_time': metrics.start_time.isoformat()
            })
            
            return metrics
    
    def update_operation(self, operation_id: str, **kwargs):
        """
        Update metrics for an active operation.
        
        Args:
            operation_id: Operation identifier
            **kwargs: Metrics to update
        """
        with self._lock:
            if operation_id in self.active_operations:
                metrics = self.active_operations[operation_id]
                
                for key, value in kwargs.items():
                    if hasattr(metrics, key):
                        setattr(metrics, key, value)
    
    def add_error(self, operation_id: str, error_info: Dict):
        """
        Add error information to an operation.
        
        Args:
            operation_id: Operation identifier
            error_info: Error details dictionary
        """
        with self._lock:
            if operation_id in self.active_operations:
                metrics = self.active_operations[operation_id]
                error_entry = {
                    'timestamp': datetime.now().isoformat(),
                    **error_info
                }
                metrics.errors.append(error_entry)
                metrics.files_failed += 1
                
                # Update performance counters
                self.performance_counters['total_errors'] += 1
                error_type = error_info.get('type', 'unknown')
                self.performance_counters[f'errors_{error_type}'] += 1
    
    def complete_operation(self, operation_id: str) -> Optional[TranslationMetrics]:
        """
        Complete an operation and store its metrics.
        
        Args:
            operation_id: Operation identifier
            
        Returns:
            Completed TranslationMetrics or None if operation not found
        """
        with self._lock:
            if operation_id not in self.active_operations:
                return None
            
            metrics = self.active_operations.pop(operation_id)
            metrics.end_time = datetime.now()
            
            # Calculate derived metrics
            if metrics.files_processed > 0:
                metrics.average_time_per_file = metrics.total_processing_time / metrics.files_processed
            
            # Store in history
            self.metrics_history.append(metrics)
            
            # Store in database
            self._store_metrics_to_db(metrics)
            
            # Update performance counters
            self.performance_counters['operations_completed'] += 1
            self.performance_counters['total_files_processed'] += metrics.files_processed
            self.performance_counters['total_files_successful'] += metrics.files_successful
            self.performance_counters['total_files_failed'] += metrics.files_failed
            
            # Store timing data
            if metrics.duration:
                self.timing_data[metrics.operation_type].append(metrics.duration.total_seconds())
            
            # Log operation completion
            self._log_system_event('operation_completed', {
                'operation_id': operation_id,
                'operation_type': metrics.operation_type,
                'duration': metrics.duration.total_seconds() if metrics.duration else 0,
                'success_rate': metrics.success_rate,
                'files_processed': metrics.files_processed
            })
            
            return metrics
    
    def _store_metrics_to_db(self, metrics: TranslationMetrics):
        """Store metrics to database."""
        try:
            with sqlite3.connect(self.db_path) as conn:
                conn.execute("""
                    INSERT OR REPLACE INTO translation_metrics (
                        operation_id, operation_type, start_time, end_time,
                        files_processed, files_successful, files_failed, files_skipped,
                        total_processing_time, average_time_per_file,
                        success_rate, failure_rate, errors, performance_data
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    metrics.operation_id,
                    metrics.operation_type,
                    metrics.start_time.isoformat(),
                    metrics.end_time.isoformat() if metrics.end_time else None,
                    metrics.files_processed,
                    metrics.files_successful,
                    metrics.files_failed,
                    metrics.files_skipped,
                    metrics.total_processing_time,
                    metrics.average_time_per_file,
                    metrics.success_rate,
                    metrics.failure_rate,
                    json.dumps(metrics.errors),
                    json.dumps(metrics.performance_data)
                ))
        except Exception as e:
            logging.error(f"Failed to store metrics to database: {e}")
    
    def _log_system_event(self, event_type: str, event_data: Dict):
        """Log system event to database."""
        try:
            with sqlite3.connect(self.db_path) as conn:
                conn.execute("""
                    INSERT INTO system_events (event_type, event_data)
                    VALUES (?, ?)
                """, (event_type, json.dumps(event_data)))
        except Exception as e:
            logging.error(f"Failed to log system event: {e}")
    
    def get_performance_summary(self, hours: int = 24) -> Dict:
        """
        Get performance summary for the specified time period.
        
        Args:
            hours: Number of hours to look back
            
        Returns:
            Performance summary dictionary
        """
        cutoff_time = datetime.now() - timedelta(hours=hours)
        
        # Get recent metrics from database
        recent_metrics = []
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.execute("""
                    SELECT * FROM translation_metrics 
                    WHERE start_time >= ? 
                    ORDER BY start_time DESC
                """, (cutoff_time.isoformat(),))
                
                columns = [desc[0] for desc in cursor.description]
                for row in cursor.fetchall():
                    metric_dict = dict(zip(columns, row))
                    recent_metrics.append(metric_dict)
        except Exception as e:
            logging.error(f"Failed to query metrics from database: {e}")
        
        # Calculate summary statistics
        total_operations = len(recent_metrics)
        total_files = sum(m['files_processed'] for m in recent_metrics)
        successful_files = sum(m['files_successful'] for m in recent_metrics)
        failed_files = sum(m['files_failed'] for m in recent_metrics)
        
        avg_success_rate = sum(m['success_rate'] for m in recent_metrics) / total_operations if total_operations > 0 else 0
        avg_processing_time = sum(m['total_processing_time'] for m in recent_metrics) / total_operations if total_operations > 0 else 0
        
        return {
            'time_period_hours': hours,
            'total_operations': total_operations,
            'total_files_processed': total_files,
            'successful_files': successful_files,
            'failed_files': failed_files,
            'overall_success_rate': (successful_files / total_files * 100) if total_files > 0 else 0,
            'average_success_rate_per_operation': avg_success_rate,
            'average_processing_time': avg_processing_time,
            'operations_by_type': self._get_operations_by_type(recent_metrics),
            'error_analysis': self._analyze_errors(recent_metrics),
            'performance_trends': self._calculate_trends(recent_metrics)
        }
    
    def _get_operations_by_type(self, metrics: List[Dict]) -> Dict:
        """Analyze operations by type."""
        by_type = defaultdict(lambda: {'count': 0, 'files': 0, 'success_rate': 0})
        
        for metric in metrics:
            op_type = metric['operation_type']
            by_type[op_type]['count'] += 1
            by_type[op_type]['files'] += metric['files_processed']
            by_type[op_type]['success_rate'] += metric['success_rate']
        
        # Calculate averages
        for op_type, data in by_type.items():
            if data['count'] > 0:
                data['avg_success_rate'] = data['success_rate'] / data['count']
                data['avg_files_per_operation'] = data['files'] / data['count']
        
        return dict(by_type)
    
    def _analyze_errors(self, metrics: List[Dict]) -> Dict:
        """Analyze error patterns."""
        error_types = defaultdict(int)
        error_files = defaultdict(list)
        
        for metric in metrics:
            try:
                errors = json.loads(metric['errors']) if metric['errors'] else []
                for error in errors:
                    error_type = error.get('type', 'unknown')
                    error_types[error_type] += 1
                    
                    file_path = error.get('file_path', 'unknown')
                    error_files[error_type].append(file_path)
            except (json.JSONDecodeError, TypeError):
                continue
        
        return {
            'error_counts': dict(error_types),
            'most_common_errors': sorted(error_types.items(), key=lambda x: x[1], reverse=True)[:5],
            'error_files': {k: list(set(v))[:10] for k, v in error_files.items()}  # Unique files, max 10
        }
    
    def _calculate_trends(self, metrics: List[Dict]) -> Dict:
        """Calculate performance trends."""
        if len(metrics) < 2:
            return {'trend': 'insufficient_data'}
        
        # Sort by start time
        sorted_metrics = sorted(metrics, key=lambda x: x['start_time'])
        
        # Calculate trends for key metrics
        success_rates = [m['success_rate'] for m in sorted_metrics]
        processing_times = [m['average_time_per_file'] for m in sorted_metrics]
        
        # Simple trend calculation (comparing first half vs second half)
        mid_point = len(sorted_metrics) // 2
        
        early_success = sum(success_rates[:mid_point]) / mid_point if mid_point > 0 else 0
        late_success = sum(success_rates[mid_point:]) / (len(success_rates) - mid_point) if len(success_rates) > mid_point else 0
        
        early_time = sum(processing_times[:mid_point]) / mid_point if mid_point > 0 else 0
        late_time = sum(processing_times[mid_point:]) / (len(processing_times) - mid_point) if len(processing_times) > mid_point else 0
        
        return {
            'success_rate_trend': 'improving' if late_success > early_success else 'declining' if late_success < early_success else 'stable',
            'processing_time_trend': 'improving' if late_time < early_time else 'declining' if late_time > early_time else 'stable',
            'success_rate_change': late_success - early_success,
            'processing_time_change': late_time - early_time
        }

# Global metrics collector instance
_metrics_collector = None

def get_metrics_collector() -> MetricsCollector:
    """Get the global metrics collector instance."""
    global _metrics_collector
    if _metrics_collector is None:
        _metrics_collector = MetricsCollector()
    return _metrics_collector

@contextmanager
def track_operation(operation_id: str, operation_type: str):
    """
    Context manager for tracking operations.
    
    Usage:
        with track_operation('my_op', 'batch') as metrics:
            # Perform operations
            metrics.files_processed += 1
    """
    collector = get_metrics_collector()
    metrics = collector.start_operation(operation_id, operation_type)
    
    try:
        yield metrics
    except Exception as e:
        collector.add_error(operation_id, {
            'type': 'exception',
            'message': str(e),
            'exception_type': type(e).__name__
        })
        raise
    finally:
        collector.complete_operation(operation_id)

class TranslationLogger:
    """
    Enhanced logging system for translation operations.
    
    This class provides structured logging with automatic metrics collection
    and integration with the monitoring system.
    """
    
    def __init__(self, name: str = 'translation', log_file: str = 'translation.log'):
        """
        Initialize translation logger.
        
        Args:
            name: Logger name
            log_file: Log file path
        """
        self.logger = logging.getLogger(name)
        self.metrics_collector = get_metrics_collector()
        
        # Configure logger if not already configured
        if not self.logger.handlers:
            self._configure_logger(log_file)
    
    def _configure_logger(self, log_file: str):
        """Configure logger with structured formatting."""
        self.logger.setLevel(logging.INFO)
        
        # File handler with structured format
        file_handler = logging.FileHandler(log_file, encoding='utf-8')
        file_handler.setFormatter(StructuredFormatter())
        self.logger.addHandler(file_handler)
        
        # Console handler with simple format
        console_handler = logging.StreamHandler()
        console_formatter = logging.Formatter(
            '%(asctime)s - %(levelname)s - %(message)s'
        )
        console_handler.setFormatter(console_formatter)
        self.logger.addHandler(console_handler)
    
    def log_operation_start(self, operation_id: str, operation_type: str, details: Dict = None):
        """Log operation start."""
        extra_fields = {
            'operation_id': operation_id,
            'operation_type': operation_type,
            'event_type': 'operation_start'
        }
        if details:
            extra_fields.update(details)
        
        self.logger.info(
            f"Starting {operation_type} operation: {operation_id}",
            extra={'extra_fields': extra_fields}
        )
    
    def log_file_processed(self, operation_id: str, file_path: str, status: str, processing_time: float = None):
        """Log file processing result."""
        extra_fields = {
            'operation_id': operation_id,
            'file_path': file_path,
            'status': status,
            'event_type': 'file_processed'
        }
        if processing_time:
            extra_fields['processing_time'] = processing_time
        
        level = logging.INFO if status == 'success' else logging.WARNING
        self.logger.log(
            level,
            f"File {status}: {file_path}",
            extra={'extra_fields': extra_fields}
        )
    
    def log_error(self, operation_id: str, error_type: str, message: str, file_path: str = None, exception: Exception = None):
        """Log error with structured information."""
        extra_fields = {
            'operation_id': operation_id,
            'error_type': error_type,
            'event_type': 'error'
        }
        if file_path:
            extra_fields['file_path'] = file_path
        if exception:
            extra_fields['exception_type'] = type(exception).__name__
            extra_fields['exception_message'] = str(exception)
        
        self.logger.error(
            f"Error in operation {operation_id}: {message}",
            extra={'extra_fields': extra_fields},
            exc_info=exception
        )
        
        # Add error to metrics
        error_info = {
            'type': error_type,
            'message': message,
            'file_path': file_path
        }
        if exception:
            error_info['exception_type'] = type(exception).__name__
        
        self.metrics_collector.add_error(operation_id, error_info)
    
    def log_operation_complete(self, operation_id: str, summary: Dict):
        """Log operation completion with summary."""
        extra_fields = {
            'operation_id': operation_id,
            'event_type': 'operation_complete',
            **summary
        }
        
        self.logger.info(
            f"Operation completed: {operation_id} - "
            f"Processed: {summary.get('files_processed', 0)}, "
            f"Success: {summary.get('files_successful', 0)}, "
            f"Failed: {summary.get('files_failed', 0)}",
            extra={'extra_fields': extra_fields}
        )

# Global logger instance
_translation_logger = None

def get_translation_logger() -> TranslationLogger:
    """Get the global translation logger instance."""
    global _translation_logger
    if _translation_logger is None:
        _translation_logger = TranslationLogger()
    return _translation_logger

def setup_monitoring(log_file: str = 'translation.log', db_path: str = 'translation_metrics.db'):
    """
    Setup monitoring system with custom configuration.
    
    Args:
        log_file: Path to log file
        db_path: Path to metrics database
    """
    global _translation_logger, _metrics_collector
    
    _translation_logger = TranslationLogger(log_file=log_file)
    _metrics_collector = MetricsCollector(db_path=db_path)
    
    logging.info("Monitoring system initialized")

if __name__ == '__main__':
    # Example usage and testing
    setup_monitoring()
    
    # Test metrics collection
    with track_operation('test_op', 'batch') as metrics:
        time.sleep(0.1)  # Simulate work
        metrics.files_processed = 5
        metrics.files_successful = 4
        metrics.files_failed = 1
    
    # Test logging
    logger = get_translation_logger()
    logger.log_operation_start('test_op_2', 'single_file', {'file': 'test.md'})
    logger.log_file_processed('test_op_2', 'test.md', 'success', 1.5)
    logger.log_operation_complete('test_op_2', {
        'files_processed': 1,
        'files_successful': 1,
        'files_failed': 0
    })
    
    # Get performance summary
    collector = get_metrics_collector()
    summary = collector.get_performance_summary(24)
    print("Performance Summary:")
    print(json.dumps(summary, indent=2, ensure_ascii=False))