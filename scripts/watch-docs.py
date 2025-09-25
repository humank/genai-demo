#!/usr/bin/env python3
"""
Documentation File Watcher

This script monitors markdown files for changes and automatically triggers translation
when English documentation is created or modified.
"""

import os
import sys
import time
import logging
import argparse
import json
from pathlib import Path
from typing import List, Dict, Set
from datetime import datetime, timedelta
import threading
import queue

try:
    from watchdog.observers import Observer
    from watchdog.events import FileSystemEventHandler, FileModifiedEvent, FileCreatedEvent
except ImportError:
    print("Error: watchdog library is required. Install with: pip install watchdog")
    sys.exit(1)

# Import our translation modules
from translate_docs import DocumentationTranslator
from kiro_translator import TranslationError

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class DocumentationWatcher(FileSystemEventHandler):
    """
    File system event handler for documentation translation.
    
    This class monitors file system events and triggers translation for relevant
    markdown files while implementing debouncing to handle rapid successive changes.
    """
    
    def __init__(self, translator: DocumentationTranslator, config: Dict):
        """
        Initialize the documentation watcher.
        
        Args:
            translator: DocumentationTranslator instance
            config: Configuration dictionary
        """
        super().__init__()
        self.translator = translator
        self.config = config
        
        # Debouncing mechanism
        self.pending_files = {}  # file_path -> last_event_time
        self.debounce_delay = config.get('debounce_delay', 2.0)  # seconds
        
        # Processing queue
        self.translation_queue = queue.Queue()
        self.processing_thread = None
        self.stop_processing = threading.Event()
        
        # Statistics
        self.stats = {
            'events_received': 0,
            'files_processed': 0,
            'translations_successful': 0,
            'translations_failed': 0,
            'files_skipped': 0
        }
        
        # Start processing thread
        self._start_processing_thread()
    
    def on_modified(self, event):
        """Handle file modification events."""
        if not event.is_directory:
            self._handle_file_event(event.src_path, 'modified')
    
    def on_created(self, event):
        """Handle file creation events."""
        if not event.is_directory:
            self._handle_file_event(event.src_path, 'created')
    
    def _handle_file_event(self, file_path: str, event_type: str):
        """
        Handle file system events for markdown files.
        
        Args:
            file_path: Path to the file that changed
            event_type: Type of event ('created' or 'modified')
        """
        try:
            self.stats['events_received'] += 1
            
            # Check if this is a markdown file we should process
            if not self._should_process_file(file_path):
                return
            
            logger.debug(f"File {event_type}: {file_path}")
            
            # Implement debouncing
            current_time = datetime.now()
            self.pending_files[file_path] = current_time
            
            # Schedule processing after debounce delay
            threading.Timer(
                self.debounce_delay,
                self._process_debounced_file,
                args=[file_path, current_time]
            ).start()
            
        except Exception as e:
            logger.error(f"Error handling file event for {file_path}: {e}")
    
    def _should_process_file(self, file_path: str) -> bool:
        """
        Check if a file should be processed for translation.
        
        Args:
            file_path: Path to the file
            
        Returns:
            True if file should be processed
        """
        try:
            path = Path(file_path)
            
            # Must be a markdown file
            if not path.suffix.lower() == '.md':
                return False
            
            # Skip Chinese translation files
            if path.name.endswith('.zh-TW.md'):
                logger.debug(f"Skipping Chinese translation file: {file_path}")
                return False
            
            # Check include patterns
            relative_path = path.relative_to(Path.cwd())
            
            # Check exclude patterns
            for exclude_pattern in self.config.get('exclude_patterns', []):
                if path.match(exclude_pattern) or str(relative_path).startswith(exclude_pattern.replace('**/', '')):
                    logger.debug(f"File excluded by pattern {exclude_pattern}: {file_path}")
                    return False
            
            # Check include patterns
            include_patterns = self.config.get('include_patterns', ['**/*.md'])
            for include_pattern in include_patterns:
                if path.match(include_pattern.replace('**/', '*')):
                    return True
            
            return False
            
        except Exception as e:
            logger.error(f"Error checking if file should be processed: {e}")
            return False
    
    def _process_debounced_file(self, file_path: str, event_time: datetime):
        """
        Process a file after debounce delay.
        
        Args:
            file_path: Path to the file to process
            event_time: Time when the event was registered
        """
        try:
            # Check if this is still the latest event for this file
            if file_path in self.pending_files:
                latest_event_time = self.pending_files[file_path]
                if event_time < latest_event_time:
                    # A newer event exists, skip this one
                    logger.debug(f"Skipping outdated event for {file_path}")
                    return
                
                # Remove from pending files
                del self.pending_files[file_path]
            
            # Add to translation queue
            self.translation_queue.put({
                'file_path': file_path,
                'timestamp': datetime.now().isoformat()
            })
            
            logger.info(f"Queued for translation: {file_path}")
            
        except Exception as e:
            logger.error(f"Error processing debounced file {file_path}: {e}")
    
    def _start_processing_thread(self):
        """Start the background processing thread."""
        self.processing_thread = threading.Thread(
            target=self._process_translation_queue,
            daemon=True
        )
        self.processing_thread.start()
        logger.info("Started translation processing thread")
    
    def _process_translation_queue(self):
        """Process the translation queue in background thread."""
        while not self.stop_processing.is_set():
            try:
                # Get next item from queue with timeout
                try:
                    item = self.translation_queue.get(timeout=1.0)
                except queue.Empty:
                    continue
                
                file_path = item['file_path']
                
                # Check if file still exists
                if not os.path.exists(file_path):
                    logger.warning(f"File no longer exists: {file_path}")
                    self.stats['files_skipped'] += 1
                    continue
                
                # Process the translation
                self._translate_file(file_path)
                
                # Mark task as done
                self.translation_queue.task_done()
                
            except Exception as e:
                logger.error(f"Error in processing thread: {e}")
    
    def _translate_file(self, file_path: str):
        """
        Translate a single file.
        
        Args:
            file_path: Path to the file to translate
        """
        try:
            self.stats['files_processed'] += 1
            
            logger.info(f"Processing translation for: {file_path}")
            
            # Use the translator to process the file
            success = self.translator.translate_file(file_path, force=False)
            
            if success:
                self.stats['translations_successful'] += 1
                logger.info(f"Successfully translated: {file_path}")
            else:
                self.stats['translations_failed'] += 1
                logger.error(f"Failed to translate: {file_path}")
                
        except Exception as e:
            self.stats['translations_failed'] += 1
            logger.error(f"Error translating file {file_path}: {e}")
    
    def get_stats(self) -> Dict:
        """Get current statistics."""
        return self.stats.copy()
    
    def stop(self):
        """Stop the watcher and processing thread."""
        logger.info("Stopping documentation watcher...")
        self.stop_processing.set()
        
        # Wait for processing thread to finish
        if self.processing_thread and self.processing_thread.is_alive():
            self.processing_thread.join(timeout=5.0)
        
        logger.info("Documentation watcher stopped")


class DocumentationWatcherService:
    """
    Service class for managing the documentation watcher.
    
    This class provides a high-level interface for starting and managing
    the file watching service with proper configuration and error handling.
    """
    
    def __init__(self, config_file: str = None):
        """
        Initialize the watcher service.
        
        Args:
            config_file: Optional path to configuration file
        """
        self.config = self._load_config(config_file)
        self.translator = DocumentationTranslator()
        self.observer = Observer()
        self.event_handler = None
        self.running = False
    
    def _load_config(self, config_file: str = None) -> Dict:
        """Load configuration settings."""
        default_config = {
            'watch_paths': ['.'],
            'include_patterns': ['**/*.md'],
            'exclude_patterns': [
                '**/*.zh-TW.md',
                'node_modules/**',
                '.git/**',
                '.kiro/**',
                'build/**',
                'target/**',
                '.backup/**'
            ],
            'debounce_delay': 2.0,
            'recursive': True,
            'log_file': 'watcher.log'
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
    
    def start(self):
        """Start the file watching service."""
        try:
            if self.running:
                logger.warning("Watcher is already running")
                return
            
            logger.info("Starting documentation watcher service...")
            
            # Create event handler
            self.event_handler = DocumentationWatcher(self.translator, self.config)
            
            # Set up file system observers
            watch_paths = self.config.get('watch_paths', ['.'])
            for watch_path in watch_paths:
                if os.path.exists(watch_path):
                    self.observer.schedule(
                        self.event_handler,
                        watch_path,
                        recursive=self.config.get('recursive', True)
                    )
                    logger.info(f"Watching directory: {watch_path}")
                else:
                    logger.warning(f"Watch path does not exist: {watch_path}")
            
            # Start the observer
            self.observer.start()
            self.running = True
            
            logger.info("Documentation watcher service started successfully")
            
        except Exception as e:
            logger.error(f"Failed to start watcher service: {e}")
            raise
    
    def stop(self):
        """Stop the file watching service."""
        try:
            if not self.running:
                return
            
            logger.info("Stopping documentation watcher service...")
            
            # Stop the observer
            self.observer.stop()
            self.observer.join()
            
            # Stop the event handler
            if self.event_handler:
                self.event_handler.stop()
            
            self.running = False
            logger.info("Documentation watcher service stopped")
            
        except Exception as e:
            logger.error(f"Error stopping watcher service: {e}")
    
    def run(self):
        """Run the watcher service (blocking)."""
        try:
            self.start()
            
            logger.info("Documentation watcher is running. Press Ctrl+C to stop.")
            
            # Keep the main thread alive
            while self.running:
                time.sleep(1)
                
                # Print stats periodically
                if hasattr(self, '_last_stats_time'):
                    if datetime.now() - self._last_stats_time > timedelta(minutes=5):
                        self._print_stats()
                else:
                    self._last_stats_time = datetime.now()
                
        except KeyboardInterrupt:
            logger.info("Received interrupt signal")
        finally:
            self.stop()
    
    def _print_stats(self):
        """Print current statistics."""
        if self.event_handler:
            stats = self.event_handler.get_stats()
            logger.info(f"Stats: {stats}")
            self._last_stats_time = datetime.now()


def main():
    """Main function for command-line usage."""
    parser = argparse.ArgumentParser(
        description='Watch markdown files and automatically translate them',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Watch current directory
  python watch-docs.py

  # Watch specific directory
  python watch-docs.py --path docs

  # Watch multiple directories
  python watch-docs.py --path docs --path src

  # Use custom configuration
  python watch-docs.py --config watcher-config.json
        """
    )
    
    parser.add_argument('--path', action='append', dest='watch_paths',
                       help='Directory to watch (can be specified multiple times)')
    parser.add_argument('--config', metavar='CONFIG',
                       help='Configuration file path')
    parser.add_argument('--verbose', '-v', action='store_true',
                       help='Verbose output')
    parser.add_argument('--quiet', '-q', action='store_true',
                       help='Quiet output (errors only)')
    
    args = parser.parse_args()
    
    # Configure logging level
    if args.quiet:
        logging.getLogger().setLevel(logging.ERROR)
    elif args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    try:
        # Create watcher service
        service = DocumentationWatcherService(args.config)
        
        # Override watch paths if specified
        if args.watch_paths:
            service.config['watch_paths'] = args.watch_paths
        
        # Run the service
        service.run()
        
        return 0
        
    except KeyboardInterrupt:
        print("\nWatcher stopped by user")
        return 0
    except Exception as e:
        print(f"Watcher failed: {e}")
        if args.verbose:
            import traceback
            traceback.print_exc()
        return 1


if __name__ == '__main__':
    exit(main())