#!/usr/bin/env python3
"""
Batch Translation Command Line Tool

This script provides a dedicated command-line interface for batch processing
large sets of documentation files with advanced features like job management,
progress monitoring, and detailed reporting.
"""

import os
import sys
import argparse
import json
import logging
from pathlib import Path
from typing import List, Dict, Optional

# Add current directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from batch_processor import BatchProcessor, BatchJob
from translate_docs import DocumentationTranslator

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


def scan_files_for_batch(directories: List[str], patterns: List[str] = None) -> List[str]:
    """
    Scan multiple directories for files to process.
    
    Args:
        directories: List of directories to scan
        patterns: Optional file patterns to match
        
    Returns:
        List of file paths
    """
    if patterns is None:
        patterns = ['**/*.md']
    
    all_files = []
    translator = DocumentationTranslator()
    
    for directory in directories:
        if not os.path.exists(directory):
            logger.warning(f"Directory not found: {directory}")
            continue
        
        try:
            files = translator.scan_directory(directory)
            all_files.extend(files)
            logger.info(f"Found {len(files)} files in {directory}")
        except Exception as e:
            logger.error(f"Failed to scan {directory}: {e}")
    
    # Remove duplicates while preserving order
    seen = set()
    unique_files = []
    for file_path in all_files:
        if file_path not in seen:
            seen.add(file_path)
            unique_files.append(file_path)
    
    return unique_files


def create_batch_job(args) -> str:
    """Create a new batch job based on command line arguments."""
    # Initialize batch processor
    batch_processor = BatchProcessor(
        max_workers=args.workers,
        config={'dry_run': args.dry_run}
    )
    
    # Determine files to process
    if args.files:
        # Process specific files
        files_to_process = []
        for file_path in args.files:
            if os.path.exists(file_path):
                files_to_process.append(file_path)
            else:
                logger.warning(f"File not found: {file_path}")
        
    elif args.directories:
        # Process directories
        files_to_process = scan_files_for_batch(args.directories)
        
    else:
        # Process current directory
        files_to_process = scan_files_for_batch(['.'])
    
    if not files_to_process:
        print("❌ No files found to process")
        return None
    
    # Filter files that need translation (unless force is specified)
    if not args.force:
        translator = DocumentationTranslator()
        files_needing_translation = [
            f for f in files_to_process 
            if translator.needs_translation(f)
        ]
    else:
        files_needing_translation = files_to_process
    
    if not files_needing_translation:
        print("✅ All files are up to date")
        return None
    
    # Create batch job
    job_id = args.job_id or None
    job_id = batch_processor.create_translation_job(files_needing_translation, job_id)
    
    print(f"📋 Created batch job: {job_id}")
    print(f"📁 Files to process: {len(files_needing_translation)}")
    
    return job_id


def process_batch_job(job_id: str, args) -> int:
    """Process a batch job."""
    batch_processor = BatchProcessor(
        max_workers=args.workers,
        config={'dry_run': args.dry_run}
    )
    
    try:
        print(f"🚀 Processing batch job: {job_id}")
        
        # Process the job
        results = batch_processor.process_job(job_id, show_progress=True)
        
        # Print summary
        batch_processor.print_job_summary(job_id)
        
        # Generate report if requested
        if args.report:
            report_file = batch_processor.generate_batch_report(job_id, args.report)
            print(f"📊 Detailed report saved to: {report_file}")
        
        # Save job state
        if args.save_state:
            save_job_state(batch_processor, args.save_state)
        
        # Return appropriate exit code
        failed_count = len(results.get('failed', []))
        return 0 if failed_count == 0 else 1
        
    except Exception as e:
        print(f"❌ Batch processing failed: {e}")
        logger.error(f"Batch processing error: {e}", exc_info=True)
        return 1


def list_jobs(args) -> int:
    """List all batch jobs."""
    batch_processor = BatchProcessor()
    
    # Load job state if available
    if args.load_state and os.path.exists(args.load_state):
        load_job_state(batch_processor, args.load_state)
    
    jobs = batch_processor.list_jobs()
    
    if not jobs:
        print("📋 No batch jobs found")
        return 0
    
    print(f"📋 Found {len(jobs)} batch jobs:")
    print("-" * 80)
    
    for job in jobs:
        status_emoji = {
            'pending': '⏳',
            'running': '🔄',
            'completed': '✅',
            'failed': '❌',
            'cancelled': '🚫'
        }.get(job['status'], '❓')
        
        print(f"{status_emoji} {job['job_id']}")
        print(f"   Status: {job['status']}")
        print(f"   Files: {job['files_count']}")
        print(f"   Created: {job['created_at']}")
        
        if job['results']:
            successful = len(job['results'].get('successful', []))
            failed = len(job['results'].get('failed', []))
            skipped = len(job['results'].get('skipped', []))
            print(f"   Results: ✅{successful} ❌{failed} ⏭️{skipped}")
        
        print()
    
    return 0


def save_job_state(batch_processor: BatchProcessor, state_file: str):
    """Save batch processor state to file."""
    try:
        state_data = {
            'jobs': {job_id: {
                'job_id': job.job_id,
                'files': job.files,
                'operation': job.operation,
                'config': job.config,
                'created_at': job.created_at,
                'status': job.status,
                'progress': job.progress,
                'results': job.results
            } for job_id, job in batch_processor.jobs.items()},
            'stats': batch_processor.stats
        }
        
        with open(state_file, 'w', encoding='utf-8') as f:
            json.dump(state_data, f, indent=2, ensure_ascii=False)
        
        print(f"💾 Job state saved to: {state_file}")
        
    except Exception as e:
        logger.error(f"Failed to save job state: {e}")


def load_job_state(batch_processor: BatchProcessor, state_file: str):
    """Load batch processor state from file."""
    try:
        with open(state_file, 'r', encoding='utf-8') as f:
            state_data = json.load(f)
        
        # Restore jobs
        for job_id, job_data in state_data.get('jobs', {}).items():
            job = BatchJob(
                job_id=job_data['job_id'],
                files=job_data['files'],
                operation=job_data['operation'],
                config=job_data.get('config', {}),
                created_at=job_data['created_at'],
                status=job_data['status'],
                progress=job_data['progress'],
                results=job_data.get('results', {})
            )
            batch_processor.jobs[job_id] = job
        
        # Restore stats
        if 'stats' in state_data:
            batch_processor.stats.update(state_data['stats'])
        
        print(f"📂 Job state loaded from: {state_file}")
        
    except Exception as e:
        logger.error(f"Failed to load job state: {e}")


def main():
    """Main function for batch translation CLI."""
    parser = argparse.ArgumentParser(
        description='Batch translation tool for documentation',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Create and process a batch job for current directory
  python batch-translate.py create --process

  # Create batch job with specific job ID
  python batch-translate.py create --job-id my-translation-job

  # Process existing job
  python batch-translate.py process my-translation-job

  # List all jobs
  python batch-translate.py list

  # Process multiple directories with 6 workers
  python batch-translate.py create --directories docs src --workers 6 --process

  # Create job for specific files
  python batch-translate.py create --files README.md docs/guide.md --process
        """
    )
    
    # Subcommands
    subparsers = parser.add_subparsers(dest='command', help='Available commands')
    
    # Create job command
    create_parser = subparsers.add_parser('create', help='Create a new batch job')
    create_parser.add_argument('--job-id', help='Custom job ID')
    create_parser.add_argument('--directories', nargs='+', help='Directories to process')
    create_parser.add_argument('--files', nargs='+', help='Specific files to process')
    create_parser.add_argument('--workers', type=int, default=3, help='Number of parallel workers')
    create_parser.add_argument('--force', action='store_true', help='Force translation of all files')
    create_parser.add_argument('--dry-run', action='store_true', help='Dry run mode')
    create_parser.add_argument('--process', action='store_true', help='Process the job immediately after creation')
    create_parser.add_argument('--report', help='Generate detailed report to specified file')
    create_parser.add_argument('--save-state', help='Save job state to specified file')
    
    # Process job command
    process_parser = subparsers.add_parser('process', help='Process an existing batch job')
    process_parser.add_argument('job_id', help='Job ID to process')
    process_parser.add_argument('--workers', type=int, default=3, help='Number of parallel workers')
    process_parser.add_argument('--report', help='Generate detailed report to specified file')
    process_parser.add_argument('--save-state', help='Save job state to specified file')
    process_parser.add_argument('--dry-run', action='store_true', help='Dry run mode')
    
    # List jobs command
    list_parser = subparsers.add_parser('list', help='List all batch jobs')
    list_parser.add_argument('--load-state', help='Load job state from specified file')
    
    # Status command
    status_parser = subparsers.add_parser('status', help='Show status of a specific job')
    status_parser.add_argument('job_id', help='Job ID to check')
    status_parser.add_argument('--load-state', help='Load job state from specified file')
    
    # Global options
    parser.add_argument('--verbose', '-v', action='store_true', help='Verbose output')
    parser.add_argument('--quiet', '-q', action='store_true', help='Quiet output')
    
    args = parser.parse_args()
    
    # Configure logging
    if args.quiet:
        logging.getLogger().setLevel(logging.ERROR)
    elif args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Handle commands
    try:
        if args.command == 'create':
            job_id = create_batch_job(args)
            if job_id and args.process:
                return process_batch_job(job_id, args)
            return 0 if job_id else 1
            
        elif args.command == 'process':
            return process_batch_job(args.job_id, args)
            
        elif args.command == 'list':
            return list_jobs(args)
            
        elif args.command == 'status':
            batch_processor = BatchProcessor()
            if args.load_state and os.path.exists(args.load_state):
                load_job_state(batch_processor, args.load_state)
            
            status = batch_processor.get_job_status(args.job_id)
            if status:
                print(f"📋 Job Status: {args.job_id}")
                print(f"Status: {status['status']}")
                print(f"Progress: {status['progress']:.1f}%")
                print(f"Files: {status['files_count']}")
                print(f"Created: {status['created_at']}")
                return 0
            else:
                print(f"❌ Job not found: {args.job_id}")
                return 1
        
        else:
            parser.print_help()
            return 1
            
    except KeyboardInterrupt:
        print("\n🛑 Operation cancelled by user")
        return 1
    except Exception as e:
        print(f"❌ Error: {e}")
        if args.verbose:
            import traceback
            traceback.print_exc()
        return 1


if __name__ == '__main__':
    exit(main())