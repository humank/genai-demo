#!/usr/bin/env python3
"""
File Management System for Documentation Translation

This module provides comprehensive file management capabilities for the translation system,
including backup creation, atomic operations, metadata preservation, and file organization.
"""

import os
import shutil
import json
import logging
from pathlib import Path
from typing import Dict, Optional, List, Tuple
from datetime import datetime
import tempfile
import hashlib

logger = logging.getLogger(__name__)

class FileManager:
    """
    Handles all file operations for the translation system.
    
    This class provides safe file operations with backup support, atomic writes,
    and metadata preservation to ensure data integrity during translation operations.
    """
    
    def __init__(self, backup_enabled: bool = True, backup_dir: str = '.backup'):
        """
        Initialize the file manager.
        
        Args:
            backup_enabled: Whether to create backups before modifications
            backup_dir: Directory name for backups (relative to each file's directory)
        """
        self.backup_enabled = backup_enabled
        self.backup_dir = backup_dir
        self.operation_log = []
    
    def create_backup(self, file_path: str) -> Optional[str]:
        """
        Create a backup of the specified file.
        
        Args:
            file_path: Path to the file to backup
            
        Returns:
            Path to the backup file, or None if backup failed
        """
        if not self.backup_enabled:
            return None
        
        try:
            source_path = Path(file_path)
            if not source_path.exists():
                logger.warning(f"Cannot backup non-existent file: {file_path}")
                return None
            
            # Create backup directory
            backup_dir_path = source_path.parent / self.backup_dir
            backup_dir_path.mkdir(exist_ok=True)
            
            # Generate backup filename with timestamp
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            backup_filename = f"{source_path.stem}_{timestamp}{source_path.suffix}"
            backup_path = backup_dir_path / backup_filename
            
            # Copy file with metadata preservation
            shutil.copy2(file_path, backup_path)
            
            logger.debug(f"Created backup: {backup_path}")
            
            # Log the backup operation
            self.operation_log.append({
                'operation': 'backup',
                'source': str(source_path),
                'backup': str(backup_path),
                'timestamp': datetime.now().isoformat()
            })
            
            return str(backup_path)
            
        except Exception as e:
            logger.error(f"Failed to create backup for {file_path}: {e}")
            return None
    
    def atomic_write(self, file_path: str, content: str, encoding: str = 'utf-8') -> bool:
        """
        Write content to file atomically to prevent corruption.
        
        Args:
            file_path: Target file path
            content: Content to write
            encoding: File encoding
            
        Returns:
            True if write was successful, False otherwise
        """
        try:
            target_path = Path(file_path)
            
            # Create parent directories if they don't exist
            target_path.parent.mkdir(parents=True, exist_ok=True)
            
            # Write to temporary file first
            with tempfile.NamedTemporaryFile(
                mode='w',
                encoding=encoding,
                dir=target_path.parent,
                prefix=f".{target_path.name}.",
                suffix='.tmp',
                delete=False
            ) as temp_file:
                temp_file.write(content)
                temp_path = temp_file.name
            
            # Preserve original file metadata if it exists
            if target_path.exists():
                original_stat = target_path.stat()
                os.utime(temp_path, (original_stat.st_atime, original_stat.st_mtime))
                os.chmod(temp_path, original_stat.st_mode)
            
            # Atomic move
            shutil.move(temp_path, target_path)
            
            logger.debug(f"Atomically wrote file: {file_path}")
            
            # Log the write operation
            self.operation_log.append({
                'operation': 'write',
                'file': str(target_path),
                'size': len(content),
                'timestamp': datetime.now().isoformat()
            })
            
            return True
            
        except Exception as e:
            logger.error(f"Failed to write file {file_path}: {e}")
            # Clean up temporary file if it exists
            try:
                if 'temp_path' in locals():
                    os.unlink(temp_path)
            except:
                pass
            return False
    
    def generate_chinese_filename(self, english_filename: str) -> str:
        """
        Generate Chinese filename from English filename.
        
        Args:
            english_filename: Original English filename
            
        Returns:
            Chinese filename with .zh-TW.md extension
        """
        if not english_filename.endswith('.md'):
            raise ValueError("Only markdown files (.md) are supported")
        
        base_name = english_filename[:-3]  # Remove .md extension
        return f"{base_name}.zh-TW.md"
    
    def get_chinese_file_path(self, english_file_path: str) -> str:
        """
        Get the full path for the Chinese translation file.
        
        Args:
            english_file_path: Path to the English file
            
        Returns:
            Full path to the Chinese translation file
        """
        english_path = Path(english_file_path)
        chinese_filename = self.generate_chinese_filename(english_path.name)
        return str(english_path.parent / chinese_filename)
    
    def preserve_metadata(self, source_file: str, target_file: str) -> bool:
        """
        Preserve file metadata from source to target file.
        
        Args:
            source_file: Source file path
            target_file: Target file path
            
        Returns:
            True if metadata was preserved successfully
        """
        try:
            source_path = Path(source_file)
            target_path = Path(target_file)
            
            if not source_path.exists():
                logger.warning(f"Source file does not exist: {source_file}")
                return False
            
            if not target_path.exists():
                logger.warning(f"Target file does not exist: {target_file}")
                return False
            
            # Get source file metadata
            source_stat = source_path.stat()
            
            # Preserve timestamps and permissions
            os.utime(target_file, (source_stat.st_atime, source_stat.st_mtime))
            os.chmod(target_file, source_stat.st_mode)
            
            logger.debug(f"Preserved metadata from {source_file} to {target_file}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to preserve metadata: {e}")
            return False
    
    def calculate_file_hash(self, file_path: str) -> Optional[str]:
        """
        Calculate SHA-256 hash of a file.
        
        Args:
            file_path: Path to the file
            
        Returns:
            SHA-256 hash string, or None if calculation failed
        """
        try:
            hash_sha256 = hashlib.sha256()
            with open(file_path, 'rb') as f:
                for chunk in iter(lambda: f.read(4096), b""):
                    hash_sha256.update(chunk)
            return hash_sha256.hexdigest()
        except Exception as e:
            logger.error(f"Failed to calculate hash for {file_path}: {e}")
            return None
    
    def verify_file_integrity(self, file_path: str, expected_hash: str) -> bool:
        """
        Verify file integrity using hash comparison.
        
        Args:
            file_path: Path to the file to verify
            expected_hash: Expected SHA-256 hash
            
        Returns:
            True if file integrity is verified
        """
        actual_hash = self.calculate_file_hash(file_path)
        return actual_hash == expected_hash if actual_hash else False
    
    def create_file_metadata(self, file_path: str) -> Dict:
        """
        Create metadata record for a file.
        
        Args:
            file_path: Path to the file
            
        Returns:
            Dictionary containing file metadata
        """
        try:
            path = Path(file_path)
            if not path.exists():
                return {}
            
            stat = path.stat()
            
            metadata = {
                'path': str(path),
                'size': stat.st_size,
                'created': datetime.fromtimestamp(stat.st_ctime).isoformat(),
                'modified': datetime.fromtimestamp(stat.st_mtime).isoformat(),
                'hash': self.calculate_file_hash(file_path),
                'encoding': self._detect_encoding(file_path)
            }
            
            return metadata
            
        except Exception as e:
            logger.error(f"Failed to create metadata for {file_path}: {e}")
            return {}
    
    def _detect_encoding(self, file_path: str) -> str:
        """
        Detect file encoding.
        
        Args:
            file_path: Path to the file
            
        Returns:
            Detected encoding string
        """
        try:
            import chardet
            with open(file_path, 'rb') as f:
                raw_data = f.read()
                result = chardet.detect(raw_data)
                return result['encoding'] or 'utf-8'
        except ImportError:
            # Fallback if chardet is not available
            return 'utf-8'
        except Exception:
            return 'utf-8'
    
    def cleanup_backups(self, directory: str, keep_count: int = 5) -> int:
        """
        Clean up old backup files, keeping only the most recent ones.
        
        Args:
            directory: Directory to clean up
            keep_count: Number of backup files to keep per original file
            
        Returns:
            Number of backup files removed
        """
        try:
            backup_dir_path = Path(directory) / self.backup_dir
            if not backup_dir_path.exists():
                return 0
            
            # Group backup files by original filename
            backup_groups = {}
            for backup_file in backup_dir_path.glob('*_*.*'):
                # Extract original filename (remove timestamp)
                parts = backup_file.stem.split('_')
                if len(parts) >= 2:
                    original_name = '_'.join(parts[:-1])  # Remove timestamp part
                    if original_name not in backup_groups:
                        backup_groups[original_name] = []
                    backup_groups[original_name].append(backup_file)
            
            removed_count = 0
            
            # Clean up each group
            for original_name, backup_files in backup_groups.items():
                # Sort by modification time (newest first)
                backup_files.sort(key=lambda x: x.stat().st_mtime, reverse=True)
                
                # Remove old backups
                for backup_file in backup_files[keep_count:]:
                    try:
                        backup_file.unlink()
                        removed_count += 1
                        logger.debug(f"Removed old backup: {backup_file}")
                    except Exception as e:
                        logger.warning(f"Failed to remove backup {backup_file}: {e}")
            
            logger.info(f"Cleaned up {removed_count} old backup files")
            return removed_count
            
        except Exception as e:
            logger.error(f"Failed to cleanup backups in {directory}: {e}")
            return 0
    
    def get_operation_log(self) -> List[Dict]:
        """
        Get the operation log.
        
        Returns:
            List of operation records
        """
        return self.operation_log.copy()
    
    def save_operation_log(self, log_file: str) -> bool:
        """
        Save the operation log to a file.
        
        Args:
            log_file: Path to the log file
            
        Returns:
            True if log was saved successfully
        """
        try:
            with open(log_file, 'w', encoding='utf-8') as f:
                json.dump(self.operation_log, f, indent=2, ensure_ascii=False)
            logger.info(f"Operation log saved to: {log_file}")
            return True
        except Exception as e:
            logger.error(f"Failed to save operation log: {e}")
            return False
    
    def rollback_operations(self, operations_to_rollback: List[Dict]) -> bool:
        """
        Rollback specified operations.
        
        Args:
            operations_to_rollback: List of operations to rollback
            
        Returns:
            True if rollback was successful
        """
        try:
            for operation in reversed(operations_to_rollback):
                if operation['operation'] == 'write':
                    # Find corresponding backup
                    backup_op = None
                    for op in self.operation_log:
                        if (op['operation'] == 'backup' and 
                            op['source'] == operation['file'] and
                            op['timestamp'] <= operation['timestamp']):
                            backup_op = op
                    
                    if backup_op:
                        # Restore from backup
                        shutil.copy2(backup_op['backup'], operation['file'])
                        logger.info(f"Restored {operation['file']} from backup")
                    else:
                        logger.warning(f"No backup found for {operation['file']}")
            
            return True
            
        except Exception as e:
            logger.error(f"Rollback failed: {e}")
            return False


def main():
    """Main function for testing file manager functionality."""
    import argparse
    
    parser = argparse.ArgumentParser(description='File Manager Utilities')
    parser.add_argument('--test', action='store_true', help='Run basic tests')
    parser.add_argument('--cleanup', metavar='DIR', help='Cleanup backups in directory')
    parser.add_argument('--keep', type=int, default=5, help='Number of backups to keep')
    
    args = parser.parse_args()
    
    if args.test:
        # Basic functionality test
        fm = FileManager()
        print("File Manager initialized successfully")
        
        # Test filename generation
        chinese_name = fm.generate_chinese_filename("README.md")
        print(f"Chinese filename: {chinese_name}")
        
        return 0
    
    if args.cleanup:
        fm = FileManager()
        removed = fm.cleanup_backups(args.cleanup, args.keep)
        print(f"Removed {removed} old backup files")
        return 0
    
    parser.print_help()
    return 1


if __name__ == '__main__':
    exit(main())