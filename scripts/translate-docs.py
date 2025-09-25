#!/usr/bin/env python3
"""
Main Documentation Translation Script

This script provides comprehensive functionality for translating markdown documentation
from English to Traditional Chinese using Kiro's AI capabilities.
"""

import os
import sys
import argparse
import logging
import json
import time
from pathlib import Path
from typing import List, Dict, Optional, Set
import fnmatch
from datetime import datetime

# Import our Kiro translator and monitoring
from kiro_translator import KiroTranslator, TranslationError
from monitoring import get_translation_logger, track_operation

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class DocumentationTranslator:
    """
    Main class for handling documentation translation operations.
    
    This class provides functionality to scan directories, identify files that need
    translation, and manage the translation process with proper error handling.
    """
    
    def __init__(self, config_file: Optional[str] = None):
        """
        Initialize the documentation translator.
        
        Args:
            config_file: Optional path to configuration file
        """
        self.translator = KiroTranslator()
        self.config = self._load_config(config_file)
        self.translation_log = {}
        self.stats = {
            'processed': 0,
            'successful': 0,
            'failed': 0,
            'skipped': 0
        }
        self.logger = get_translation_logger()
    
    def _load_config(self, config_file: Optional[str] = None) -> Dict:
        """Load configuration settings."""
        default_config = {
            'include_patterns': ['**/*.md'],
            'exclude_patterns': [
                '**/*.zh-TW.md',
                'node_modules/**',
                '.git/**',
                '.kiro/**',
                'build/**',
                'target/**'
            ],
            'preserve_terms': [
                'API', 'DDD', 'README', 'GitHub', 'Docker', 'Kubernetes',
                'Spring Boot', 'JPA', 'Hibernate', 'PostgreSQL', 'Redis',
                'AWS', 'CDK', 'Lambda', 'S3', 'RDS', 'EKS', 'VPC'
            ],
            'backup_enabled': True,
            'dry_run': False,
            'log_file': 'translation.log'
        }
        
        if config_file and os.path.exists(config_file):
            try:
                with open(config_file, 'r', encoding='utf-8') as f:
                    user_config = json.load(f)
                default_config.update(user_config)
                logger.info(f"Loaded configuration from {config_file}")
            except Exception as e:
                logger.warning(f"Failed to load config file {config_file}: {e}")
        
        return default_config
    
    def scan_directory(self, directory: str) -> List[str]:
        """
        Scan directory for markdown files that need translation.
        
        Args:
            directory: Directory path to scan
            
        Returns:
            List of file paths that need translation
        """
        directory_path = Path(directory)
        if not directory_path.exists():
            raise ValueError(f"Directory does not exist: {directory}")
        
        markdown_files = []
        
        # Find all markdown files
        for pattern in self.config['include_patterns']:
            for file_path in directory_path.rglob(pattern.replace('**/', '')):
                if file_path.is_file():
                    # Check if file should be excluded
                    relative_path = file_path.relative_to(directory_path)
                    should_exclude = False
                    
                    for exclude_pattern in self.config['exclude_patterns']:
                        if fnmatch.fnmatch(str(relative_path), exclude_pattern):
                            should_exclude = True
                            break
                    
                    if not should_exclude:
                        markdown_files.append(str(file_path))
        
        logger.info(f"Found {len(markdown_files)} markdown files to process")
        return sorted(markdown_files)
    
    def needs_translation(self, source_file: str) -> bool:
        """
        Check if a file needs translation.
        
        Args:
            source_file: Path to the source file
            
        Returns:
            True if translation is needed, False otherwise
        """
        source_path = Path(source_file)
        chinese_filename = self.translator.create_chinese_filename(source_path.name)
        chinese_path = source_path.parent / chinese_filename
        
        # If Chinese file doesn't exist, translation is needed
        if not chinese_path.exists():
            return True
        
        # If source file is newer than Chinese file, translation is needed
        source_mtime = source_path.stat().st_mtime
        chinese_mtime = chinese_path.stat().st_mtime
        
        return source_mtime > chinese_mtime
    
    def translate_file(self, source_file: str, force: bool = False) -> bool:
        """
        Translate a single file.
        
        Args:
            source_file: Path to the source file
            force: Force translation even if not needed
            
        Returns:
            True if translation was successful, False otherwise
        """
        try:
            source_path = Path(source_file)
            
            # Check if translation is needed
            if not force and not self.needs_translation(source_file):
                logger.info(f"Skipping {source_file} - translation up to date")
                self.stats['skipped'] += 1
                return True
            
            # Create backup if enabled
            if self.config['backup_enabled']:
                self._create_backup(source_file)
            
            # Perform translation
            if self.config['dry_run']:
                logger.info(f"[DRY RUN] Would translate: {source_file}")
                self.stats['successful'] += 1
                return True
            
            logger.info(f"Translating: {source_file}")
            target_file = self.translator.translate_and_save(source_file)
            
            # Log the translation
            self.translation_log[source_file] = {
                'target_file': target_file,
                'timestamp': datetime.now().isoformat(),
                'status': 'success'
            }
            
            self.stats['successful'] += 1
            logger.info(f"Successfully translated: {source_file} -> {target_file}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to translate {source_file}: {e}")
            self.translation_log[source_file] = {
                'timestamp': datetime.now().isoformat(),
                'status': 'failed',
                'error': str(e)
            }
            self.stats['failed'] += 1
            return False
    
    def _create_backup(self, source_file: str) -> None:
        """Create a backup of the source file."""
        try:
            source_path = Path(source_file)
            backup_dir = source_path.parent / '.backup'
            backup_dir.mkdir(exist_ok=True)
            
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            backup_filename = f"{source_path.stem}_{timestamp}{source_path.suffix}"
            backup_path = backup_dir / backup_filename
            
            import shutil
            shutil.copy2(source_file, backup_path)
            logger.debug(f"Created backup: {backup_path}")
            
        except Exception as e:
            logger.warning(f"Failed to create backup for {source_file}: {e}")
    
    def translate_directory(self, directory: str, force: bool = False) -> Dict:
        """
        Translate all markdown files in a directory.
        
        Args:
            directory: Directory path to process
            force: Force translation of all files
            
        Returns:
            Dictionary with translation results
        """
        operation_id = f"dir_translate_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        with track_operation(operation_id, 'directory') as metrics:
            logger.info(f"Starting translation of directory: {directory}")
            self.logger.log_operation_start(operation_id, 'directory', {'directory': directory})
            
            # Scan for files
            files_to_translate = self.scan_directory(directory)
            
            if not files_to_translate:
                logger.info("No files found to translate")
                return self._get_results()
            
            metrics.files_processed = len(files_to_translate)
            start_time = time.time()
            
            # Process each file
            for file_path in files_to_translate:
                self.stats['processed'] += 1
                file_start_time = time.time()
                
                success = self.translate_file(file_path, force)
                
                file_processing_time = time.time() - file_start_time
                status = 'success' if success else 'failed'
                
                self.logger.log_file_processed(operation_id, file_path, status, file_processing_time)
                
                if success:
                    metrics.files_successful += 1
                else:
                    metrics.files_failed += 1
            
            metrics.total_processing_time = time.time() - start_time
            
            # Save translation log
            self._save_translation_log()
            
            # Log operation completion
            summary = {
                'files_processed': self.stats['processed'],
                'files_successful': self.stats['successful'],
                'files_failed': self.stats['failed'],
                'files_skipped': self.stats['skipped']
            }
            self.logger.log_operation_complete(operation_id, summary)
            
            return self._get_results()
    
    def _save_translation_log(self) -> None:
        """Save the translation log to file."""
        try:
            log_file = self.config['log_file']
            with open(log_file, 'w', encoding='utf-8') as f:
                json.dump(self.translation_log, f, indent=2, ensure_ascii=False)
            logger.info(f"Translation log saved to: {log_file}")
        except Exception as e:
            logger.warning(f"Failed to save translation log: {e}")
    
    def _get_results(self) -> Dict:
        """Get translation results summary."""
        return {
            'stats': self.stats.copy(),
            'log': self.translation_log.copy(),
            'timestamp': datetime.now().isoformat()
        }
    
    def print_summary(self) -> None:
        """Print a summary of translation results."""
        print("\n" + "="*50)
        print("TRANSLATION SUMMARY")
        print("="*50)
        print(f"Files processed: {self.stats['processed']}")
        print(f"Successful: {self.stats['successful']}")
        print(f"Failed: {self.stats['failed']}")
        print(f"Skipped: {self.stats['skipped']}")
        
        if self.stats['failed'] > 0:
            print("\nFailed files:")
            for file_path, log_entry in self.translation_log.items():
                if log_entry['status'] == 'failed':
                    print(f"  - {file_path}: {log_entry.get('error', 'Unknown error')}")
        
        print("="*50)


def _handle_batch_processing(args, translator: DocumentationTranslator) -> int:
    """
    Handle batch processing operations.
    
    Args:
        args: Command line arguments
        translator: DocumentationTranslator instance
        
    Returns:
        Exit code
    """
    try:
        # Import batch processor
        from batch_processor import BatchProcessor
        
        # Initialize batch processor
        batch_processor = BatchProcessor(
            max_workers=args.workers,
            config=translator.config
        )
        
        # Determine directory to process
        directory = args.directory if args.directory else '.'
        
        # Scan for files to process
        files_to_process = translator.scan_directory(directory)
        
        if not files_to_process:
            print("No files found to process")
            return 0
        
        # Filter files that need translation (unless force is specified)
        if not args.force:
            files_needing_translation = [
                f for f in files_to_process 
                if translator.needs_translation(f)
            ]
        else:
            files_needing_translation = files_to_process
        
        if not files_needing_translation:
            print("All files are up to date")
            return 0
        
        print(f"🚀 Starting batch processing of {len(files_needing_translation)} files with {args.workers} workers")
        
        # Create and process batch job
        job_id = batch_processor.create_translation_job(files_needing_translation)
        results = batch_processor.process_job(job_id, show_progress=args.progress)
        
        # Print job summary
        batch_processor.print_job_summary(job_id)
        
        # Generate report if requested
        if args.report:
            report_file = batch_processor.generate_batch_report(job_id, args.report)
            print(f"📊 Detailed report saved to: {report_file}")
        
        # Return appropriate exit code
        failed_count = len(results.get('failed', []))
        return 0 if failed_count == 0 else 1
        
    except ImportError:
        print("❌ Batch processing requires batch_processor.py module")
        print("   Falling back to single-threaded processing...")
        
        # Fall back to regular processing
        if args.all:
            results = translator.translate_directory('.', args.force)
        elif args.directory:
            results = translator.translate_directory(args.directory, args.force)
        
        translator.print_summary()
        return 0 if results['stats']['failed'] == 0 else 1
        
    except Exception as e:
        print(f"❌ Batch processing failed: {e}")
        if args.verbose:
            import traceback
            traceback.print_exc()
        return 1


def main():
    """Main function for command-line usage."""
    parser = argparse.ArgumentParser(
        description='Translate markdown documentation using Kiro AI',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Translate all files in current directory
  python translate-docs.py --all

  # Translate specific file
  python translate-docs.py --file README.md

  # Translate directory with force update
  python translate-docs.py --directory docs --force

  # Dry run to see what would be translated
  python translate-docs.py --all --dry-run

  # Batch processing with parallel workers
  python translate-docs.py --all --batch --workers 4

  # Batch processing with progress reporting
  python translate-docs.py --directory docs --batch --progress
        """
    )
    
    # Main operation modes
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--all', action='store_true', 
                      help='Translate all markdown files in current directory')
    group.add_argument('--file', metavar='FILE', 
                      help='Translate a specific file')
    group.add_argument('--directory', metavar='DIR', 
                      help='Translate all files in specified directory')
    
    # Options
    parser.add_argument('--force', action='store_true',
                       help='Force translation even if files are up to date')
    parser.add_argument('--dry-run', action='store_true',
                       help='Show what would be translated without doing it')
    parser.add_argument('--config', metavar='CONFIG',
                       help='Configuration file path')
    parser.add_argument('--verbose', '-v', action='store_true',
                       help='Verbose output')
    parser.add_argument('--quiet', '-q', action='store_true',
                       help='Quiet output (errors only)')
    
    # Batch processing options
    parser.add_argument('--batch', action='store_true',
                       help='Use batch processing with parallel workers')
    parser.add_argument('--workers', type=int, default=3, metavar='N',
                       help='Number of parallel workers for batch processing (default: 3)')
    parser.add_argument('--progress', action='store_true',
                       help='Show detailed progress bar during batch processing')
    parser.add_argument('--report', metavar='FILE',
                       help='Generate detailed batch report to specified file')
    
    args = parser.parse_args()
    
    # Configure logging level
    if args.quiet:
        logging.getLogger().setLevel(logging.ERROR)
    elif args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    try:
        # Initialize translator
        translator = DocumentationTranslator(args.config)
        
        # Set dry run mode
        if args.dry_run:
            translator.config['dry_run'] = True
            print("DRY RUN MODE - No files will be modified")
        
        # Check if batch processing is requested
        if args.batch and (args.all or args.directory):
            return _handle_batch_processing(args, translator)
        
        # Execute based on mode (traditional single-threaded processing)
        if args.all:
            results = translator.translate_directory('.', args.force)
        elif args.directory:
            results = translator.translate_directory(args.directory, args.force)
        elif args.file:
            if not os.path.exists(args.file):
                print(f"Error: File not found: {args.file}")
                return 1
            
            success = translator.translate_file(args.file, args.force)
            results = translator._get_results()
            
            if not success:
                print(f"Failed to translate: {args.file}")
                return 1
        
        # Print summary
        translator.print_summary()
        
        # Return appropriate exit code
        return 0 if results['stats']['failed'] == 0 else 1
        
    except KeyboardInterrupt:
        print("\nTranslation interrupted by user")
        return 1
    except Exception as e:
        print(f"Translation failed: {e}")
        if args.verbose:
            import traceback
            traceback.print_exc()
        return 1


if __name__ == '__main__':
    exit(main())