#!/usr/bin/env python3
"""
Batch Processing System for Translation

This module provides efficient batch processing capabilities for translating
large sets of documentation files with progress reporting and parallel processing.
"""

import os
import sys
import json
import logging
import threading
import queue
import time
from pathlib import Path
from typing import List, Dict, Optional, Callable, Any
from datetime import datetime, timedelta
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field

# Import our translation modules
from scripts.kiro_translator import KiroTranslator, TranslationError

logger = logging.getLogger(__name__)

@dataclass
class BatchJob:
    """Represents a batch processing job."""
    job_id: str
    files: List[str]
    operation: str  # 'translate', 'migrate', 'validate'
    config: Dict = field(default_factory=dict)
    created_at: str = field(default_factory=lambda: datetime.now().isoformat())
    status: str = 'pending'  # 'pending', 'running', 'completed', 'failed'
    progress: float = 0.0
    results: Dict = field(default_factory=dict)

@dataclass
class BatchProgress:
    """Progress tracking for batch operations."""
    total_files: int
    processed_files: int = 0
    successful_files: int = 0
    failed_files: int = 0
    skipped_files: int = 0
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    
    @property
    def progress_percentage(self) -> float:
        """Calculate progress percentage."""
        return (self.processed_files / self.total_files * 100) if self.total_files > 0 else 0.0
    
    @property
    def elapsed_time(self) -> Optional[timedelta]:
        """Calculate elapsed time."""
        if self.start_time:
            end = self.end_time or datetime.now()
            return end - self.start_time
        return None
    
    @property
    def estimated_remaining_time(self) -> Optional[timedelta]:
        """Estimate remaining time based on current progress."""
        if not self.start_time or self.processed_files == 0:
            return None
        
        elapsed = self.elapsed_time
        if not elapsed:
            return None
        
        remaining_files = self.total_files - self.processed_files
        if remaining_files <= 0:
            return timedelta(0)
        
        time_per_file = elapsed / self.processed_files
        return time_per_file * remaining_files

class ProgressReporter:
    """
    Progress reporting system for batch operations.
    
    This class provides real-time progress reporting with visual indicators,
    time estimates, and performance metrics.
    """
    
    def __init__(self, total_items: int, update_interval: float = 1.0):
        """
        Initialize the progress reporter.
        
        Args:
            total_items: Total number of items to process
            update_interval: Update interval in seconds
        """
        self.progress = BatchProgress(total_items)
        self.update_interval = update_interval
        self.last_update = 0
        self.callbacks = []
    
    def start(self):
        """Start progress tracking."""
        self.progress.start_time = datetime.now()
        self._report_progress()
    
    def update(self, processed: int = 1, successful: int = 0, failed: int = 0, skipped: int = 0):
        """
        Update progress counters.
        
        Args:
            processed: Number of items processed
            successful: Number of successful items
            failed: Number of failed items
            skipped: Number of skipped items
        """
        self.progress.processed_files += processed
        self.progress.successful_files += successful
        self.progress.failed_files += failed
        self.progress.skipped_files += skipped
        
        # Report progress if enough time has passed
        current_time = time.time()
        if current_time - self.last_update >= self.update_interval:
            self._report_progress()
            self.last_update = current_time
    
    def finish(self):
        """Finish progress tracking."""
        self.progress.end_time = datetime.now()
        self._report_progress(force=True)
    
    def _report_progress(self, force: bool = False):
        """Report current progress."""
        if not force and not self._should_update():
            return
        
        # Create progress bar
        progress_bar = self._create_progress_bar()
        
        # Format time information
        elapsed_str = self._format_timedelta(self.progress.elapsed_time) if self.progress.elapsed_time else "00:00"
        remaining_str = self._format_timedelta(self.progress.estimated_remaining_time) if self.progress.estimated_remaining_time else "??:??"
        
        # Print progress line
        print(f"\r{progress_bar} {self.progress.progress_percentage:5.1f}% | "
              f"✅{self.progress.successful_files} ❌{self.progress.failed_files} ⏭️{self.progress.skipped_files} | "
              f"⏱️{elapsed_str} / ~{remaining_str}", end='', flush=True)
        
        # Call registered callbacks
        for callback in self.callbacks:
            try:
                callback(self.progress)
            except Exception as e:
                logger.warning(f"Progress callback failed: {e}")
    
    def _should_update(self) -> bool:
        """Check if progress should be updated."""
        return time.time() - self.last_update >= self.update_interval
    
    def _create_progress_bar(self, width: int = 30) -> str:
        """Create a visual progress bar."""
        filled = int(width * self.progress.progress_percentage / 100)
        bar = '█' * filled + '░' * (width - filled)
        return f"[{bar}]"
    
    def _format_timedelta(self, td: Optional[timedelta]) -> str:
        """Format timedelta as MM:SS."""
        if not td:
            return "00:00"
        
        total_seconds = int(td.total_seconds())
        minutes = total_seconds // 60
        seconds = total_seconds % 60
        return f"{minutes:02d}:{seconds:02d}"
    
    def add_callback(self, callback: Callable[[BatchProgress], None]):
        """Add a progress callback function."""
        self.callbacks.append(callback)

class BatchProcessor:
    """
    Main batch processing system.
    
    This class provides efficient batch processing with parallel execution,
    progress reporting, and comprehensive error handling.
    """
    
    def __init__(self, max_workers: int = 3, config: Dict = None):
        """
        Initialize the batch processor.
        
        Args:
            max_workers: Maximum number of parallel workers
            config: Configuration dictionary
        """
        self.max_workers = max_workers
        self.config = config or {}
        self.translator = KiroTranslator(config)
        
        # Job management
        self.jobs = {}
        self.current_job_id = None
        
        # Statistics
        self.stats = {
            'jobs_created': 0,
            'jobs_completed': 0,
            'jobs_failed': 0,
            'total_files_processed': 0,
            'total_processing_time': 0.0
        }
    
    def create_translation_job(self, files: List[str], job_id: Optional[str] = None) -> str:
        """
        Create a new translation job.
        
        Args:
            files: List of files to translate
            job_id: Optional job ID (generated if not provided)
            
        Returns:
            Job ID
        """
        if job_id is None:
            job_id = f"translate_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        job = BatchJob(
            job_id=job_id,
            files=files,
            operation='translate',
            config=self.config.copy()
        )
        
        self.jobs[job_id] = job
        self.stats['jobs_created'] += 1
        
        logger.info(f"Created translation job {job_id} with {len(files)} files")
        return job_id
    
    def process_job(self, job_id: str, show_progress: bool = True) -> Dict:
        """
        Process a batch job.
        
        Args:
            job_id: Job ID to process
            show_progress: Whether to show progress bar
            
        Returns:
            Job results dictionary
        """
        if job_id not in self.jobs:
            raise ValueError(f"Job not found: {job_id}")
        
        job = self.jobs[job_id]
        self.current_job_id = job_id
        
        logger.info(f"Starting batch job: {job_id}")
        job.status = 'running'
        
        # Initialize progress reporter
        progress_reporter = None
        if show_progress:
            progress_reporter = ProgressReporter(len(job.files))
            progress_reporter.start()
        
        try:
            # Process files in parallel
            results = self._process_files_parallel(job.files, progress_reporter)
            
            # Update job with results
            job.results = results
            job.status = 'completed'
            job.progress = 100.0
            
            # Update statistics
            self.stats['jobs_completed'] += 1
            self.stats['total_files_processed'] += len(job.files)
            
            if progress_reporter:
                progress_reporter.finish()
                print()  # New line after progress bar
            
            logger.info(f"Batch job {job_id} completed successfully")
            
        except Exception as e:
            job.status = 'failed'
            job.results = {'error': str(e)}
            self.stats['jobs_failed'] += 1
            
            if progress_reporter:
                progress_reporter.finish()
                print()  # New line after progress bar
            
            logger.error(f"Batch job {job_id} failed: {e}")
            raise
        
        return job.results
    
    def _process_files_parallel(self, files: List[str], progress_reporter: Optional[ProgressReporter] = None) -> Dict:
        """
        Process files in parallel.
        
        Args:
            files: List of files to process
            progress_reporter: Optional progress reporter
            
        Returns:
            Processing results
        """
        results = {
            'successful': [],
            'failed': [],
            'skipped': [],
            'total_time': 0.0,
            'average_time_per_file': 0.0
        }
        
        start_time = time.time()
        
        # Process files using thread pool
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            # Submit all tasks
            future_to_file = {
                executor.submit(self._process_single_file, file_path): file_path
                for file_path in files
            }
            
            # Process completed tasks
            for future in as_completed(future_to_file):
                file_path = future_to_file[future]
                
                try:
                    result = future.result()
                    
                    if result['status'] == 'success':
                        results['successful'].append(result)
                        if progress_reporter:
                            progress_reporter.update(processed=1, successful=1)
                    elif result['status'] == 'skipped':
                        results['skipped'].append(result)
                        if progress_reporter:
                            progress_reporter.update(processed=1, skipped=1)
                    else:
                        results['failed'].append(result)
                        if progress_reporter:
                            progress_reporter.update(processed=1, failed=1)
                    
                except Exception as e:
                    error_result = {
                        'file_path': file_path,
                        'status': 'failed',
                        'error': str(e),
                        'timestamp': datetime.now().isoformat()
                    }
                    results['failed'].append(error_result)
                    
                    if progress_reporter:
                        progress_reporter.update(processed=1, failed=1)
                    
                    logger.error(f"Failed to process {file_path}: {e}")
        
        # Calculate timing statistics
        end_time = time.time()
        results['total_time'] = end_time - start_time
        results['average_time_per_file'] = results['total_time'] / len(files) if files else 0.0
        
        return results
    
    def _process_single_file(self, file_path: str) -> Dict:
        """
        Process a single file.
        
        Args:
            file_path: Path to the file to process
            
        Returns:
            Processing result dictionary
        """
        try:
            start_time = time.time()
            
            # Check if translation is needed
            if not self.translator.needs_translation(file_path):
                return {
                    'file_path': file_path,
                    'status': 'skipped',
                    'reason': 'Translation up to date',
                    'processing_time': time.time() - start_time,
                    'timestamp': datetime.now().isoformat()
                }
            
            # Perform translation
            success = self.translator.translate_file(file_path, force=False)
            
            processing_time = time.time() - start_time
            
            if success:
                return {
                    'file_path': file_path,
                    'status': 'success',
                    'processing_time': processing_time,
                    'timestamp': datetime.now().isoformat()
                }
            else:
                return {
                    'file_path': file_path,
                    'status': 'failed',
                    'error': 'Translation returned false',
                    'processing_time': processing_time,
                    'timestamp': datetime.now().isoformat()
                }
            
        except Exception as e:
            return {
                'file_path': file_path,
                'status': 'failed',
                'error': str(e),
                'processing_time': time.time() - start_time,
                'timestamp': datetime.now().isoformat()
            }
    
    def get_job_status(self, job_id: str) -> Optional[Dict]:
        """
        Get status of a specific job.
        
        Args:
            job_id: Job ID
            
        Returns:
            Job status dictionary or None if job not found
        """
        if job_id not in self.jobs:
            return None
        
        job = self.jobs[job_id]
        return {
            'job_id': job.job_id,
            'status': job.status,
            'progress': job.progress,
            'files_count': len(job.files),
            'operation': job.operation,
            'created_at': job.created_at,
            'results': job.results
        }
    
    def list_jobs(self) -> List[Dict]:
        """List all jobs."""
        return [self.get_job_status(job_id) for job_id in self.jobs.keys()]
    
    def cancel_job(self, job_id: str) -> bool:
        """
        Cancel a running job.
        
        Args:
            job_id: Job ID to cancel
            
        Returns:
            True if job was cancelled
        """
        if job_id not in self.jobs:
            return False
        
        job = self.jobs[job_id]
        if job.status == 'running':
            job.status = 'cancelled'
            logger.info(f"Job {job_id} cancelled")
            return True
        
        return False
    
    def cleanup_completed_jobs(self, max_age_hours: int = 24) -> int:
        """
        Clean up old completed jobs.
        
        Args:
            max_age_hours: Maximum age in hours for completed jobs
            
        Returns:
            Number of jobs cleaned up
        """
        cutoff_time = datetime.now() - timedelta(hours=max_age_hours)
        jobs_to_remove = []
        
        for job_id, job in self.jobs.items():
            if job.status in ['completed', 'failed', 'cancelled']:
                job_time = datetime.fromisoformat(job.created_at)
                if job_time < cutoff_time:
                    jobs_to_remove.append(job_id)
        
        # Remove old jobs
        for job_id in jobs_to_remove:
            del self.jobs[job_id]
        
        logger.info(f"Cleaned up {len(jobs_to_remove)} old jobs")
        return len(jobs_to_remove)
    
    def generate_batch_report(self, job_id: str, output_file: Optional[str] = None) -> str:
        """
        Generate a detailed batch processing report.
        
        Args:
            job_id: Job ID to report on
            output_file: Optional output file path
            
        Returns:
            Path to the generated report
        """
        if job_id not in self.jobs:
            raise ValueError(f"Job not found: {job_id}")
        
        job = self.jobs[job_id]
        
        # Create comprehensive report
        report = {
            'job_info': {
                'job_id': job.job_id,
                'operation': job.operation,
                'created_at': job.created_at,
                'status': job.status,
                'total_files': len(job.files)
            },
            'results': job.results,
            'performance_metrics': self._calculate_performance_metrics(job),
            'file_details': self._get_file_details(job),
            'recommendations': self._generate_batch_recommendations(job)
        }
        
        # Save report
        if output_file is None:
            output_file = f"batch-report-{job_id}.json"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        
        logger.info(f"Batch report saved to: {output_file}")
        return output_file
    
    def _calculate_performance_metrics(self, job: BatchJob) -> Dict:
        """Calculate performance metrics for a job."""
        if not job.results:
            return {}
        
        total_time = job.results.get('total_time', 0.0)
        avg_time = job.results.get('average_time_per_file', 0.0)
        
        successful_count = len(job.results.get('successful', []))
        failed_count = len(job.results.get('failed', []))
        total_count = len(job.files)
        
        return {
            'total_processing_time': total_time,
            'average_time_per_file': avg_time,
            'success_rate': (successful_count / total_count * 100) if total_count > 0 else 0.0,
            'failure_rate': (failed_count / total_count * 100) if total_count > 0 else 0.0,
            'throughput_files_per_minute': (total_count / (total_time / 60)) if total_time > 0 else 0.0
        }
    
    def _get_file_details(self, job: BatchJob) -> Dict:
        """Get detailed file processing information."""
        details = {
            'successful_files': [],
            'failed_files': [],
            'skipped_files': []
        }
        
        if job.results:
            details['successful_files'] = job.results.get('successful', [])
            details['failed_files'] = job.results.get('failed', [])
            details['skipped_files'] = job.results.get('skipped', [])
        
        return details
    
    def _generate_batch_recommendations(self, job: BatchJob) -> List[str]:
        """Generate recommendations based on batch processing results."""
        recommendations = []
        
        if not job.results:
            return recommendations
        
        failed_count = len(job.results.get('failed', []))
        total_count = len(job.files)
        failure_rate = (failed_count / total_count * 100) if total_count > 0 else 0.0
        
        if failure_rate > 10:
            recommendations.append("High failure rate detected. Review error logs and consider adjusting configuration.")
        
        avg_time = job.results.get('average_time_per_file', 0.0)
        if avg_time > 30:  # 30 seconds per file
            recommendations.append("Processing time is high. Consider optimizing translation prompts or increasing parallel workers.")
        
        if job.results.get('total_time', 0.0) > 1800:  # 30 minutes
            recommendations.append("Total processing time is high. Consider breaking large jobs into smaller batches.")
        
        return recommendations
    
    def print_job_summary(self, job_id: str):
        """Print a summary of job results."""
        if job_id not in self.jobs:
            print(f"❌ Job not found: {job_id}")
            return
        
        job = self.jobs[job_id]
        
        print(f"\n📊 Batch Job Summary: {job_id}")
        print("=" * 50)
        print(f"Operation: {job.operation}")
        print(f"Status: {job.status}")
        print(f"Total Files: {len(job.files)}")
        
        if job.results:
            successful = len(job.results.get('successful', []))
            failed = len(job.results.get('failed', []))
            skipped = len(job.results.get('skipped', []))
            
            print(f"✅ Successful: {successful}")
            print(f"❌ Failed: {failed}")
            print(f"⏭️  Skipped: {skipped}")
            
            total_time = job.results.get('total_time', 0.0)
            avg_time = job.results.get('average_time_per_file', 0.0)
            
            print(f"⏱️  Total Time: {total_time:.1f}s")
            print(f"📈 Avg Time/File: {avg_time:.1f}s")
            
            if failed > 0:
                print(f"\n❌ Failed Files:")
                for result in job.results.get('failed', [])[:5]:  # Show first 5
                    print(f"  - {result['file_path']}: {result.get('error', 'Unknown error')}")
                
                if len(job.results.get('failed', [])) > 5:
                    print(f"  ... and {len(job.results.get('failed', [])) - 5} more")
        
        print("=" * 50)
    
    def process_directory(self, source_dir: str, output_dir: str, pattern: str = "*.md", max_workers: int = None) -> List[Dict]:
        """
        Process all files in a directory.
        
        Args:
            source_dir: Source directory path
            output_dir: Output directory path
            pattern: File pattern to match
            max_workers: Number of parallel workers
            
        Returns:
            List of processing results
        """
        # Find all matching files
        source_path = Path(source_dir)
        if not source_path.exists():
            raise FileNotFoundError(f"Source directory not found: {source_dir}")
        
        files = list(source_path.rglob(pattern))
        if not files:
            return []
        
        # Create output directory
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)
        
        # Process files
        results = []
        workers = max_workers or self.max_workers
        
        with ThreadPoolExecutor(max_workers=workers) as executor:
            # Submit all translation tasks
            future_to_file = {}
            for file_path in files:
                # Calculate output path
                relative_path = file_path.relative_to(source_path)
                output_file = output_path / relative_path.with_suffix('.zh-TW.md')
                output_file.parent.mkdir(parents=True, exist_ok=True)
                
                # Submit translation task
                future = executor.submit(self._translate_single_file, str(file_path), str(output_file))
                future_to_file[future] = str(file_path)
            
            # Collect results
            for future in as_completed(future_to_file):
                file_path = future_to_file[future]
                try:
                    result = future.result()
                    results.append(result)
                    
                    if result.get('success'):
                        print(f"✅ Translated: {file_path}")
                    else:
                        print(f"❌ Failed: {file_path} - {result.get('error', 'Unknown error')}")
                        
                except Exception as e:
                    results.append({
                        'file': file_path,
                        'success': False,
                        'error': str(e)
                    })
                    print(f"❌ Error: {file_path} - {e}")
        
        return results
    
    def _translate_single_file(self, input_file: str, output_file: str) -> Dict:
        """Translate a single file."""
        try:
            result = self.translator.translate_file(input_file, output_file)
            return result
        except Exception as e:
            return {
                'file': input_file,
                'success': False,
                'error': str(e)
            }

def main():
    """Main function for testing batch processor."""
    import sys
    
    if len(sys.argv) < 3:
        print("Usage: python batch_processor.py <source_dir> <output_dir> [pattern]")
        sys.exit(1)
    
    source_dir = sys.argv[1]
    output_dir = sys.argv[2]
    pattern = sys.argv[3] if len(sys.argv) > 3 else "*.md"
    
    # Setup logging
    logging.basicConfig(level=logging.INFO)
    
    # Create processor
    processor = BatchProcessor()
    
    # Process directory
    results = processor.process_directory(source_dir, output_dir, pattern)
    
    # Show summary
    successful = sum(1 for r in results if r.get('success', False))
    total = len(results)
    
    print(f"\n📊 Batch processing completed: {successful}/{total} files successful")

if __name__ == "__main__":
    main()