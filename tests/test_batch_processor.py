#!/usr/bin/env python3
"""
Unit tests for BatchProcessor.

This module tests the batch processing functionality including
parallel processing, job management, and progress reporting.
"""

import unittest
import tempfile
import os
import sys
import time
from pathlib import Path
from unittest.mock import Mock, patch, MagicMock

# Add scripts directory to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'scripts'))

from batch_processor import BatchProcessor, BatchJob, BatchProgress, ProgressReporter

class TestBatchJob(unittest.TestCase):
    """Test cases for BatchJob dataclass."""
    
    def test_batch_job_creation(self):
        """Test BatchJob creation."""
        files = ['file1.md', 'file2.md']
        job = BatchJob(
            job_id='test_job',
            files=files,
            operation='translate'
        )
        
        self.assertEqual(job.job_id, 'test_job')
        self.assertEqual(job.files, files)
        self.assertEqual(job.operation, 'translate')
        self.assertEqual(job.status, 'pending')
        self.assertEqual(job.progress, 0.0)
        self.assertIsInstance(job.config, dict)
        self.assertIsInstance(job.results, dict)

class TestBatchProgress(unittest.TestCase):
    """Test cases for BatchProgress dataclass."""
    
    def test_batch_progress_creation(self):
        """Test BatchProgress creation."""
        progress = BatchProgress(total_files=10)
        
        self.assertEqual(progress.total_files, 10)
        self.assertEqual(progress.processed_files, 0)
        self.assertEqual(progress.successful_files, 0)
        self.assertEqual(progress.failed_files, 0)
        self.assertEqual(progress.skipped_files, 0)
        self.assertIsNone(progress.start_time)
        self.assertIsNone(progress.end_time)
    
    def test_progress_percentage_calculation(self):
        """Test progress percentage calculation."""
        progress = BatchProgress(total_files=10)
        
        # Initial progress
        self.assertEqual(progress.progress_percentage, 0.0)
        
        # Partial progress
        progress.processed_files = 5
        self.assertEqual(progress.progress_percentage, 50.0)
        
        # Complete progress
        progress.processed_files = 10
        self.assertEqual(progress.progress_percentage, 100.0)
        
        # Edge case: zero total files
        progress_zero = BatchProgress(total_files=0)
        self.assertEqual(progress_zero.progress_percentage, 0.0)
    
    def test_elapsed_time_calculation(self):
        """Test elapsed time calculation."""
        from datetime import datetime, timedelta
        
        progress = BatchProgress(total_files=10)
        
        # No start time
        self.assertIsNone(progress.elapsed_time)
        
        # With start time, no end time
        start_time = datetime.now()
        progress.start_time = start_time
        
        elapsed = progress.elapsed_time
        self.assertIsNotNone(elapsed)
        self.assertIsInstance(elapsed, timedelta)
        
        # With both start and end time
        progress.end_time = start_time + timedelta(seconds=30)
        elapsed = progress.elapsed_time
        self.assertEqual(elapsed.total_seconds(), 30.0)
    
    def test_estimated_remaining_time(self):
        """Test estimated remaining time calculation."""
        from datetime import datetime, timedelta
        
        progress = BatchProgress(total_files=10)
        
        # No start time
        self.assertIsNone(progress.estimated_remaining_time)
        
        # With start time but no processed files
        progress.start_time = datetime.now() - timedelta(seconds=10)
        self.assertIsNone(progress.estimated_remaining_time)
        
        # With processed files
        progress.processed_files = 5
        remaining = progress.estimated_remaining_time
        
        self.assertIsNotNone(remaining)
        self.assertIsInstance(remaining, timedelta)
        
        # All files processed
        progress.processed_files = 10
        remaining = progress.estimated_remaining_time
        self.assertEqual(remaining.total_seconds(), 0.0)

class TestProgressReporter(unittest.TestCase):
    """Test cases for ProgressReporter class."""
    
    def test_progress_reporter_initialization(self):
        """Test ProgressReporter initialization."""
        reporter = ProgressReporter(total_items=20, update_interval=0.5)
        
        self.assertEqual(reporter.progress.total_files, 20)
        self.assertEqual(reporter.update_interval, 0.5)
        self.assertEqual(reporter.last_update, 0)
        self.assertEqual(len(reporter.callbacks), 0)
    
    def test_progress_update(self):
        """Test progress update functionality."""
        reporter = ProgressReporter(total_items=10)
        
        # Update progress
        reporter.update(processed=2, successful=2, failed=0, skipped=0)
        
        self.assertEqual(reporter.progress.processed_files, 2)
        self.assertEqual(reporter.progress.successful_files, 2)
        self.assertEqual(reporter.progress.failed_files, 0)
        self.assertEqual(reporter.progress.skipped_files, 0)
        self.assertEqual(reporter.progress.progress_percentage, 20.0)
    
    def test_progress_callbacks(self):
        """Test progress callback functionality."""
        reporter = ProgressReporter(total_items=10)
        
        # Add callback
        callback_called = []
        def test_callback(progress):
            callback_called.append(progress.progress_percentage)
        
        reporter.add_callback(test_callback)
        
        # Start and update (should trigger callback)
        reporter.start()
        reporter.update(processed=5, successful=5)
        
        # Force update to trigger callback
        reporter._report_progress(force=True)
        
        self.assertTrue(len(callback_called) > 0)
    
    def test_create_progress_bar(self):
        """Test progress bar creation."""
        reporter = ProgressReporter(total_items=10)
        
        # Test different progress levels
        test_cases = [
            (0, 0.0),
            (5, 50.0),
            (10, 100.0)
        ]
        
        for processed, expected_percentage in test_cases:
            reporter.progress.processed_files = processed
            progress_bar = reporter._create_progress_bar(width=20)
            
            self.assertIsInstance(progress_bar, str)
            self.assertTrue(progress_bar.startswith('['))
            self.assertTrue(progress_bar.endswith(']'))
            self.assertEqual(len(progress_bar), 22)  # [20 chars]

class TestBatchProcessor(unittest.TestCase):
    """Test cases for BatchProcessor class."""
    
    def setUp(self):
        """Set up test fixtures."""
        self.temp_dir = tempfile.mkdtemp()
        self.processor = BatchProcessor(max_workers=2)
        
        # Create test files
        self.test_files = []
        for i in range(5):
            file_path = Path(self.temp_dir) / f'test_{i}.md'
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(f'# Test Document {i}\n\nContent for document {i}.')
            self.test_files.append(str(file_path))
    
    def tearDown(self):
        """Clean up test fixtures."""
        import shutil
        shutil.rmtree(self.temp_dir, ignore_errors=True)
    
    def test_batch_processor_initialization(self):
        """Test BatchProcessor initialization."""
        processor = BatchProcessor(max_workers=4, config={'test': 'value'})
        
        self.assertEqual(processor.max_workers, 4)
        self.assertEqual(processor.config['test'], 'value')
        self.assertIsInstance(processor.jobs, dict)
        self.assertIsInstance(processor.stats, dict)
        self.assertEqual(processor.stats['jobs_created'], 0)
    
    def test_create_translation_job(self):
        """Test translation job creation."""
        files = ['file1.md', 'file2.md']
        job_id = self.processor.create_translation_job(files)
        
        self.assertIsInstance(job_id, str)
        self.assertIn(job_id, self.processor.jobs)
        
        job = self.processor.jobs[job_id]
        self.assertEqual(job.files, files)
        self.assertEqual(job.operation, 'translate')
        self.assertEqual(job.status, 'pending')
        self.assertEqual(self.processor.stats['jobs_created'], 1)
    
    def test_create_translation_job_custom_id(self):
        """Test translation job creation with custom ID."""
        files = ['file1.md', 'file2.md']
        custom_id = 'my_custom_job'
        
        job_id = self.processor.create_translation_job(files, custom_id)
        
        self.assertEqual(job_id, custom_id)
        self.assertIn(custom_id, self.processor.jobs)
    
    def test_get_job_status(self):
        """Test job status retrieval."""
        files = ['file1.md', 'file2.md']
        job_id = self.processor.create_translation_job(files)
        
        status = self.processor.get_job_status(job_id)
        
        self.assertIsNotNone(status)
        self.assertEqual(status['job_id'], job_id)
        self.assertEqual(status['status'], 'pending')
        self.assertEqual(status['files_count'], 2)
        self.assertEqual(status['operation'], 'translate')
    
    def test_get_job_status_not_found(self):
        """Test job status retrieval for non-existent job."""
        status = self.processor.get_job_status('non_existent_job')
        
        self.assertIsNone(status)
    
    def test_list_jobs(self):
        """Test job listing."""
        # Create multiple jobs
        job1_id = self.processor.create_translation_job(['file1.md'])
        job2_id = self.processor.create_translation_job(['file2.md', 'file3.md'])
        
        jobs = self.processor.list_jobs()
        
        self.assertEqual(len(jobs), 2)
        
        job_ids = [job['job_id'] for job in jobs]
        self.assertIn(job1_id, job_ids)
        self.assertIn(job2_id, job_ids)
    
    def test_cancel_job(self):
        """Test job cancellation."""
        files = ['file1.md', 'file2.md']
        job_id = self.processor.create_translation_job(files)
        
        # Set job to running status
        self.processor.jobs[job_id].status = 'running'
        
        result = self.processor.cancel_job(job_id)
        
        self.assertTrue(result)
        self.assertEqual(self.processor.jobs[job_id].status, 'cancelled')
    
    def test_cancel_job_not_running(self):
        """Test cancelling job that's not running."""
        files = ['file1.md', 'file2.md']
        job_id = self.processor.create_translation_job(files)
        
        # Job is in 'pending' status
        result = self.processor.cancel_job(job_id)
        
        self.assertFalse(result)
        self.assertEqual(self.processor.jobs[job_id].status, 'pending')
    
    def test_cancel_job_not_found(self):
        """Test cancelling non-existent job."""
        result = self.processor.cancel_job('non_existent_job')
        
        self.assertFalse(result)
    
    def test_cleanup_completed_jobs(self):
        """Test cleanup of old completed jobs."""
        from datetime import datetime, timedelta
        
        # Create jobs with different ages
        old_time = (datetime.now() - timedelta(hours=25)).isoformat()
        recent_time = (datetime.now() - timedelta(hours=1)).isoformat()
        
        # Create old completed job
        old_job = BatchJob(
            job_id='old_job',
            files=['file1.md'],
            operation='translate',
            created_at=old_time
        )
        old_job.status = 'completed'
        self.processor.jobs['old_job'] = old_job
        
        # Create recent job
        recent_job = BatchJob(
            job_id='recent_job',
            files=['file2.md'],
            operation='translate',
            created_at=recent_time
        )
        recent_job.status = 'completed'
        self.processor.jobs['recent_job'] = recent_job
        
        # Create running job (should not be cleaned up)
        running_job = BatchJob(
            job_id='running_job',
            files=['file3.md'],
            operation='translate',
            created_at=old_time
        )
        running_job.status = 'running'
        self.processor.jobs['running_job'] = running_job
        
        # Cleanup jobs older than 24 hours
        cleaned_count = self.processor.cleanup_completed_jobs(max_age_hours=24)
        
        self.assertEqual(cleaned_count, 1)
        self.assertNotIn('old_job', self.processor.jobs)
        self.assertIn('recent_job', self.processor.jobs)
        self.assertIn('running_job', self.processor.jobs)  # Running jobs not cleaned
    
    @patch('batch_processor.DocumentationTranslator')
    def test_process_single_file_success(self, mock_translator_class):
        """Test successful single file processing."""
        # Setup mock
        mock_translator = Mock()
        mock_translator.needs_translation.return_value = True
        mock_translator.translate_file.return_value = True
        mock_translator_class.return_value = mock_translator
        
        # Create fresh processor
        processor = BatchProcessor()
        
        result = processor._process_single_file('test.md')
        
        self.assertEqual(result['status'], 'success')
        self.assertEqual(result['file_path'], 'test.md')
        self.assertIn('processing_time', result)
        self.assertIn('timestamp', result)
    
    @patch('batch_processor.DocumentationTranslator')
    def test_process_single_file_skipped(self, mock_translator_class):
        """Test skipped file processing."""
        # Setup mock
        mock_translator = Mock()
        mock_translator.needs_translation.return_value = False
        mock_translator_class.return_value = mock_translator
        
        # Create fresh processor
        processor = BatchProcessor()
        
        result = processor._process_single_file('test.md')
        
        self.assertEqual(result['status'], 'skipped')
        self.assertEqual(result['reason'], 'Translation up to date')
        self.assertIn('processing_time', result)
    
    @patch('batch_processor.DocumentationTranslator')
    def test_process_single_file_failure(self, mock_translator_class):
        """Test failed file processing."""
        # Setup mock
        mock_translator = Mock()
        mock_translator.needs_translation.return_value = True
        mock_translator.translate_file.side_effect = Exception("Translation failed")
        mock_translator_class.return_value = mock_translator
        
        # Create fresh processor
        processor = BatchProcessor()
        
        result = processor._process_single_file('test.md')
        
        self.assertEqual(result['status'], 'failed')
        self.assertEqual(result['error'], 'Translation failed')
        self.assertIn('processing_time', result)
    
    @patch('batch_processor.DocumentationTranslator')
    def test_process_files_parallel(self, mock_translator_class):
        """Test parallel file processing."""
        # Setup mock
        mock_translator = Mock()
        mock_translator.needs_translation.return_value = True
        mock_translator.translate_file.return_value = True
        mock_translator_class.return_value = mock_translator
        
        # Create fresh processor
        processor = BatchProcessor(max_workers=2)
        
        files = ['file1.md', 'file2.md', 'file3.md']
        results = processor._process_files_parallel(files)
        
        self.assertIn('successful', results)
        self.assertIn('failed', results)
        self.assertIn('skipped', results)
        self.assertIn('total_time', results)
        self.assertIn('average_time_per_file', results)
        
        # All files should be successful
        self.assertEqual(len(results['successful']), 3)
        self.assertEqual(len(results['failed']), 0)
        self.assertEqual(len(results['skipped']), 0)
    
    def test_generate_batch_report(self):
        """Test batch report generation."""
        # Create job with results
        job_id = self.processor.create_translation_job(['file1.md', 'file2.md'])
        job = self.processor.jobs[job_id]
        job.status = 'completed'
        job.results = {
            'successful': [{'file_path': 'file1.md', 'status': 'success'}],
            'failed': [{'file_path': 'file2.md', 'status': 'failed', 'error': 'Test error'}],
            'skipped': [],
            'total_time': 10.5,
            'average_time_per_file': 5.25
        }
        
        # Generate report
        report_file = self.processor.generate_batch_report(job_id)
        
        self.assertTrue(os.path.exists(report_file))
        
        # Verify report content
        with open(report_file, 'r', encoding='utf-8') as f:
            report_data = json.load(f)
        
        self.assertIn('job_info', report_data)
        self.assertIn('results', report_data)
        self.assertIn('performance_metrics', report_data)
        self.assertIn('recommendations', report_data)
        
        self.assertEqual(report_data['job_info']['job_id'], job_id)
        self.assertEqual(report_data['job_info']['total_files'], 2)
        
        # Cleanup
        os.remove(report_file)
    
    def test_generate_batch_report_job_not_found(self):
        """Test batch report generation for non-existent job."""
        with self.assertRaises(ValueError) as context:
            self.processor.generate_batch_report('non_existent_job')
        
        self.assertIn('Job not found', str(context.exception))
    
    def test_calculate_performance_metrics(self):
        """Test performance metrics calculation."""
        # Create job with results
        job = BatchJob(
            job_id='test_job',
            files=['file1.md', 'file2.md', 'file3.md'],
            operation='translate'
        )
        job.results = {
            'successful': [{'file_path': 'file1.md'}, {'file_path': 'file2.md'}],
            'failed': [{'file_path': 'file3.md'}],
            'skipped': [],
            'total_time': 15.0,
            'average_time_per_file': 5.0
        }
        
        metrics = self.processor._calculate_performance_metrics(job)
        
        self.assertEqual(metrics['total_processing_time'], 15.0)
        self.assertEqual(metrics['average_time_per_file'], 5.0)
        self.assertAlmostEqual(metrics['success_rate'], 66.67, places=1)  # 2/3 * 100
        self.assertAlmostEqual(metrics['failure_rate'], 33.33, places=1)  # 1/3 * 100
        self.assertEqual(metrics['throughput_files_per_minute'], 12.0)  # 3 files / (15s / 60)
    
    def test_generate_batch_recommendations(self):
        """Test batch recommendation generation."""
        # Create job with high failure rate
        job = BatchJob(
            job_id='test_job',
            files=['file1.md', 'file2.md'],
            operation='translate'
        )
        job.results = {
            'successful': [],
            'failed': [{'file_path': 'file1.md'}, {'file_path': 'file2.md'}],
            'skipped': [],
            'total_time': 5.0,
            'average_time_per_file': 2.5
        }
        
        recommendations = self.processor._generate_batch_recommendations(job)
        
        self.assertIsInstance(recommendations, list)
        # Should recommend investigating high failure rate
        self.assertTrue(any('failure rate' in rec.lower() for rec in recommendations))
    
    def test_generate_batch_recommendations_slow_processing(self):
        """Test recommendations for slow processing."""
        # Create job with slow processing
        job = BatchJob(
            job_id='test_job',
            files=['file1.md'],
            operation='translate'
        )
        job.results = {
            'successful': [{'file_path': 'file1.md'}],
            'failed': [],
            'skipped': [],
            'total_time': 35.0,  # 35 seconds for 1 file
            'average_time_per_file': 35.0
        }
        
        recommendations = self.processor._generate_batch_recommendations(job)
        
        self.assertIsInstance(recommendations, list)
        # Should recommend optimizing processing time
        self.assertTrue(any('processing time' in rec.lower() for rec in recommendations))

class TestBatchProcessorIntegration(unittest.TestCase):
    """Integration tests for BatchProcessor."""
    
    def setUp(self):
        """Set up test fixtures."""
        self.temp_dir = tempfile.mkdtemp()
        
        # Create test files
        self.test_files = []
        for i in range(3):
            file_path = Path(self.temp_dir) / f'test_{i}.md'
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(f'# Test Document {i}\n\nContent for document {i}.')
            self.test_files.append(str(file_path))
    
    def tearDown(self):
        """Clean up test fixtures."""
        import shutil
        shutil.rmtree(self.temp_dir, ignore_errors=True)
    
    @patch('batch_processor.DocumentationTranslator')
    def test_full_batch_processing_workflow(self, mock_translator_class):
        """Test complete batch processing workflow."""
        # Setup mock translator
        mock_translator = Mock()
        mock_translator.needs_translation.return_value = True
        mock_translator.translate_file.return_value = True
        mock_translator_class.return_value = mock_translator
        
        # Create processor
        processor = BatchProcessor(max_workers=2)
        
        # Create and process job
        job_id = processor.create_translation_job(self.test_files)
        results = processor.process_job(job_id, show_progress=False)
        
        # Verify job completion
        job = processor.jobs[job_id]
        self.assertEqual(job.status, 'completed')
        self.assertEqual(job.progress, 100.0)
        
        # Verify results
        self.assertIn('successful', results)
        self.assertIn('failed', results)
        self.assertIn('skipped', results)
        self.assertEqual(len(results['successful']), 3)
        self.assertEqual(len(results['failed']), 0)
        
        # Verify stats
        self.assertEqual(processor.stats['jobs_completed'], 1)
        self.assertEqual(processor.stats['total_files_processed'], 3)

if __name__ == '__main__':
    # Run tests with verbose output
    unittest.main(verbosity=2)