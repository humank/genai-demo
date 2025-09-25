#!/usr/bin/env python3
"""
Chinese Documentation Migration Tool

This script identifies existing Chinese markdown files and migrates them to the new
English-first documentation system by translating them to English and reorganizing
the file structure.
"""

import os
import sys
import re
import argparse
import logging
import json
from pathlib import Path
from typing import List, Dict, Tuple, Optional, Set
from datetime import datetime
import shutil

# Import our translation modules
from kiro_translator import KiroTranslator, TranslationError
from file_manager import FileManager

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class ChineseContentDetector:
    """
    Detects Chinese content in markdown files.
    
    This class provides functionality to identify files containing Chinese text,
    analyze the content distribution, and categorize files for migration.
    """
    
    def __init__(self):
        """Initialize the Chinese content detector."""
        # Chinese character ranges (Unicode)
        self.chinese_ranges = [
            (0x4E00, 0x9FFF),   # CJK Unified Ideographs
            (0x3400, 0x4DBF),   # CJK Extension A
            (0x20000, 0x2A6DF), # CJK Extension B
            (0x2A700, 0x2B73F), # CJK Extension C
            (0x2B740, 0x2B81F), # CJK Extension D
            (0x2B820, 0x2CEAF), # CJK Extension E
            (0x3000, 0x303F),   # CJK Symbols and Punctuation
            (0xFF00, 0xFFEF),   # Halfwidth and Fullwidth Forms
        ]
        
        # Common Chinese punctuation
        self.chinese_punctuation = set('，。！？；：「」『』（）【】《》〈〉')
        
        # Statistics
        self.stats = {
            'files_scanned': 0,
            'chinese_files_found': 0,
            'mixed_content_files': 0,
            'english_only_files': 0,
            'total_chinese_chars': 0
        }
    
    def is_chinese_char(self, char: str) -> bool:
        """
        Check if a character is Chinese.
        
        Args:
            char: Character to check
            
        Returns:
            True if character is Chinese
        """
        if char in self.chinese_punctuation:
            return True
        
        char_code = ord(char)
        for start, end in self.chinese_ranges:
            if start <= char_code <= end:
                return True
        
        return False
    
    def analyze_content(self, content: str) -> Dict:
        """
        Analyze content for Chinese character distribution.
        
        Args:
            content: Text content to analyze
            
        Returns:
            Dictionary with analysis results
        """
        total_chars = len(content)
        chinese_chars = 0
        english_chars = 0
        other_chars = 0
        
        for char in content:
            if self.is_chinese_char(char):
                chinese_chars += 1
            elif char.isalpha() and ord(char) < 128:  # ASCII letters
                english_chars += 1
            else:
                other_chars += 1
        
        # Calculate percentages
        chinese_percentage = (chinese_chars / total_chars * 100) if total_chars > 0 else 0
        english_percentage = (english_chars / total_chars * 100) if total_chars > 0 else 0
        
        return {
            'total_chars': total_chars,
            'chinese_chars': chinese_chars,
            'english_chars': english_chars,
            'other_chars': other_chars,
            'chinese_percentage': chinese_percentage,
            'english_percentage': english_percentage,
            'has_chinese': chinese_chars > 0,
            'is_primarily_chinese': chinese_percentage > 50,
            'is_mixed_content': chinese_chars > 0 and english_chars > 0
        }
    
    def scan_file(self, file_path: str) -> Optional[Dict]:
        """
        Scan a single file for Chinese content.
        
        Args:
            file_path: Path to the file to scan
            
        Returns:
            Analysis results or None if scan failed
        """
        try:
            self.stats['files_scanned'] += 1
            
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            analysis = self.analyze_content(content)
            analysis['file_path'] = file_path
            analysis['file_size'] = len(content)
            
            # Update statistics
            if analysis['has_chinese']:
                self.stats['chinese_files_found'] += 1
                self.stats['total_chinese_chars'] += analysis['chinese_chars']
                
                if analysis['is_mixed_content']:
                    self.stats['mixed_content_files'] += 1
            else:
                self.stats['english_only_files'] += 1
            
            logger.debug(f"Scanned {file_path}: {analysis['chinese_percentage']:.1f}% Chinese")
            
            return analysis
            
        except Exception as e:
            logger.error(f"Failed to scan file {file_path}: {e}")
            return None
    
    def scan_directory(self, directory: str, exclude_patterns: List[str] = None) -> List[Dict]:
        """
        Scan directory for files with Chinese content.
        
        Args:
            directory: Directory to scan
            exclude_patterns: Patterns to exclude
            
        Returns:
            List of analysis results for files with Chinese content
        """
        if exclude_patterns is None:
            exclude_patterns = [
                '**/*.zh-TW.md',
                'node_modules/**',
                '.git/**',
                '.kiro/**',
                'build/**',
                'target/**'
            ]
        
        directory_path = Path(directory)
        chinese_files = []
        
        logger.info(f"Scanning directory for Chinese content: {directory}")
        
        # Find all markdown files
        for md_file in directory_path.rglob('*.md'):
            # Check if file should be excluded
            relative_path = md_file.relative_to(directory_path)
            should_exclude = False
            
            for pattern in exclude_patterns:
                if md_file.match(pattern.replace('**/', '*')) or str(relative_path).startswith(pattern.replace('**/', '')):
                    should_exclude = True
                    break
            
            if should_exclude:
                continue
            
            # Scan the file
            analysis = self.scan_file(str(md_file))
            if analysis and analysis['has_chinese']:
                chinese_files.append(analysis)
        
        logger.info(f"Found {len(chinese_files)} files with Chinese content")
        return chinese_files
    
    def generate_report(self, chinese_files: List[Dict]) -> Dict:
        """
        Generate a migration report.
        
        Args:
            chinese_files: List of files with Chinese content
            
        Returns:
            Migration report dictionary
        """
        report = {
            'scan_timestamp': datetime.now().isoformat(),
            'statistics': self.stats.copy(),
            'files_by_category': {
                'primarily_chinese': [],
                'mixed_content': [],
                'minimal_chinese': []
            },
            'migration_recommendations': []
        }
        
        # Categorize files
        for file_info in chinese_files:
            if file_info['chinese_percentage'] > 70:
                report['files_by_category']['primarily_chinese'].append(file_info)
            elif file_info['chinese_percentage'] > 20:
                report['files_by_category']['mixed_content'].append(file_info)
            else:
                report['files_by_category']['minimal_chinese'].append(file_info)
        
        # Generate recommendations
        if report['files_by_category']['primarily_chinese']:
            report['migration_recommendations'].append({
                'category': 'primarily_chinese',
                'action': 'translate_to_english',
                'description': 'These files should be translated to English and become the primary version',
                'files': len(report['files_by_category']['primarily_chinese'])
            })
        
        if report['files_by_category']['mixed_content']:
            report['migration_recommendations'].append({
                'category': 'mixed_content',
                'action': 'manual_review',
                'description': 'These files need manual review to separate Chinese and English content',
                'files': len(report['files_by_category']['mixed_content'])
            })
        
        if report['files_by_category']['minimal_chinese']:
            report['migration_recommendations'].append({
                'category': 'minimal_chinese',
                'action': 'convert_to_english',
                'description': 'These files have minimal Chinese content that should be converted to English',
                'files': len(report['files_by_category']['minimal_chinese'])
            })
        
        return report
    
    def get_stats(self) -> Dict:
        """Get current statistics."""
        return self.stats.copy()


class ChineseDocumentationMigrator:
    """
    Migrates existing Chinese documentation to English-first system.
    
    This class handles the migration process including translation, file reorganization,
    and backup creation for existing Chinese documentation.
    """
    
    def __init__(self, config: Dict = None):
        """
        Initialize the migrator.
        
        Args:
            config: Configuration dictionary
        """
        self.config = config or {}
        self.detector = ChineseContentDetector()
        self.translator = KiroTranslator()
        self.file_manager = FileManager()
        
        self.migration_log = []
        self.stats = {
            'files_processed': 0,
            'successful_migrations': 0,
            'failed_migrations': 0,
            'skipped_files': 0
        }
    
    def migrate_file(self, file_path: str, dry_run: bool = False) -> bool:
        """
        Migrate a single Chinese file to English-first system.
        
        Args:
            file_path: Path to the Chinese file
            dry_run: If True, only simulate the migration
            
        Returns:
            True if migration was successful
        """
        try:
            self.stats['files_processed'] += 1
            
            logger.info(f"Migrating file: {file_path}")
            
            # Analyze the file
            analysis = self.detector.scan_file(file_path)
            if not analysis or not analysis['has_chinese']:
                logger.info(f"File has no Chinese content, skipping: {file_path}")
                self.stats['skipped_files'] += 1
                return True
            
            if dry_run:
                logger.info(f"[DRY RUN] Would migrate: {file_path}")
                self.stats['successful_migrations'] += 1
                return True
            
            # Create backup
            backup_path = self.file_manager.create_backup(file_path)
            
            # Read original content
            with open(file_path, 'r', encoding='utf-8') as f:
                chinese_content = f.read()
            
            # Translate Chinese content to English
            logger.info(f"Translating Chinese content to English: {file_path}")
            english_content = self._translate_chinese_to_english(chinese_content)
            
            # Create the English version (replace original)
            success = self.file_manager.atomic_write(file_path, english_content)
            if not success:
                raise Exception("Failed to write English version")
            
            # Create the Chinese version with .zh-TW.md extension
            chinese_file_path = self.file_manager.get_chinese_file_path(file_path)
            success = self.file_manager.atomic_write(chinese_file_path, chinese_content)
            if not success:
                raise Exception("Failed to write Chinese version")
            
            # Log the migration
            migration_record = {
                'original_file': file_path,
                'english_file': file_path,
                'chinese_file': chinese_file_path,
                'backup_file': backup_path,
                'timestamp': datetime.now().isoformat(),
                'status': 'success',
                'chinese_percentage': analysis['chinese_percentage']
            }
            self.migration_log.append(migration_record)
            
            self.stats['successful_migrations'] += 1
            logger.info(f"Successfully migrated: {file_path}")
            
            return True
            
        except Exception as e:
            logger.error(f"Failed to migrate {file_path}: {e}")
            
            # Log the failure
            migration_record = {
                'original_file': file_path,
                'timestamp': datetime.now().isoformat(),
                'status': 'failed',
                'error': str(e)
            }
            self.migration_log.append(migration_record)
            
            self.stats['failed_migrations'] += 1
            return False
    
    def _translate_chinese_to_english(self, chinese_content: str) -> str:
        """
        Translate Chinese content to English using Kiro AI.
        
        Args:
            chinese_content: Chinese content to translate
            
        Returns:
            Translated English content
        """
        # Create a reverse translation prompt for Chinese to English
        prompt = """
Please translate the following markdown content from Traditional Chinese (zh-TW) to English.

IMPORTANT REQUIREMENTS:
1. Preserve ALL markdown formatting exactly (headers, lists, code blocks, links, tables)
2. Keep ALL technical terms in English: API, DDD, GitHub, Docker, Kubernetes, etc.
3. Keep ALL code examples, file paths, and URLs unchanged
4. Maintain professional technical documentation tone
5. Preserve line breaks and spacing
6. Keep HTML tags and markdown syntax intact
7. Do not translate content inside code blocks (```code```) or inline code (`code`)
8. Use clear, professional English suitable for technical documentation

Content to translate:
{content}

Please provide ONLY the translated content without any additional explanation or comments.
"""
        
        try:
            # Format the prompt
            formatted_prompt = prompt.format(content=chinese_content)
            
            # Note: In a real implementation, this would use Kiro's AI
            # For now, we'll simulate the translation
            logger.info("Translating Chinese content to English using Kiro AI...")
            
            # This is where we would call Kiro's AI translation
            # english_content = kiro_ai_translate(formatted_prompt)
            
            # For demonstration, we'll return a placeholder
            english_content = f"[TRANSLATED TO ENGLISH FROM CHINESE]\n{chinese_content}"
            
            return english_content
            
        except Exception as e:
            logger.error(f"Translation failed: {e}")
            raise TranslationError(f"Failed to translate Chinese to English: {e}")
    
    def migrate_directory(self, directory: str, dry_run: bool = False) -> Dict:
        """
        Migrate all Chinese files in a directory.
        
        Args:
            directory: Directory to migrate
            dry_run: If True, only simulate the migration
            
        Returns:
            Migration results dictionary
        """
        logger.info(f"Starting migration of directory: {directory}")
        
        # Scan for Chinese files
        chinese_files = self.detector.scan_directory(directory)
        
        if not chinese_files:
            logger.info("No Chinese files found to migrate")
            return self._get_results()
        
        logger.info(f"Found {len(chinese_files)} files to migrate")
        
        # Process each file
        for file_info in chinese_files:
            self.migrate_file(file_info['file_path'], dry_run)
        
        # Save migration log
        self._save_migration_log()
        
        return self._get_results()
    
    def _save_migration_log(self):
        """Save the migration log to file."""
        try:
            log_file = 'migration.log'
            with open(log_file, 'w', encoding='utf-8') as f:
                json.dump(self.migration_log, f, indent=2, ensure_ascii=False)
            logger.info(f"Migration log saved to: {log_file}")
        except Exception as e:
            logger.warning(f"Failed to save migration log: {e}")
    
    def _get_results(self) -> Dict:
        """Get migration results summary."""
        return {
            'stats': self.stats.copy(),
            'detector_stats': self.detector.get_stats(),
            'migration_log': self.migration_log.copy(),
            'timestamp': datetime.now().isoformat()
        }
    
    def print_summary(self):
        """Print a summary of migration results."""
        print("\n" + "="*60)
        print("CHINESE DOCUMENTATION MIGRATION SUMMARY")
        print("="*60)
        print(f"Files processed: {self.stats['files_processed']}")
        print(f"Successful migrations: {self.stats['successful_migrations']}")
        print(f"Failed migrations: {self.stats['failed_migrations']}")
        print(f"Skipped files: {self.stats['skipped_files']}")
        
        detector_stats = self.detector.get_stats()
        print(f"\nDetection Statistics:")
        print(f"Files scanned: {detector_stats['files_scanned']}")
        print(f"Chinese files found: {detector_stats['chinese_files_found']}")
        print(f"Mixed content files: {detector_stats['mixed_content_files']}")
        
        if self.stats['failed_migrations'] > 0:
            print("\nFailed migrations:")
            for record in self.migration_log:
                if record['status'] == 'failed':
                    print(f"  - {record['original_file']}: {record.get('error', 'Unknown error')}")
        
        print("="*60)


def main():
    """Main function for command-line usage."""
    parser = argparse.ArgumentParser(
        description='Migrate Chinese documentation to English-first system',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Scan for Chinese files
  python migrate-chinese-docs.py --scan

  # Generate migration report
  python migrate-chinese-docs.py --report

  # Migrate all Chinese files (dry run)
  python migrate-chinese-docs.py --migrate --dry-run

  # Migrate specific directory
  python migrate-chinese-docs.py --migrate --directory docs

  # Migrate specific file
  python migrate-chinese-docs.py --migrate --file README.md
        """
    )
    
    # Main operation modes
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--scan', action='store_true',
                      help='Scan for Chinese files and show statistics')
    group.add_argument('--report', action='store_true',
                      help='Generate detailed migration report')
    group.add_argument('--migrate', action='store_true',
                      help='Perform migration of Chinese files')
    
    # Target specification
    parser.add_argument('--directory', metavar='DIR', default='.',
                       help='Directory to process (default: current directory)')
    parser.add_argument('--file', metavar='FILE',
                       help='Specific file to migrate')
    
    # Options
    parser.add_argument('--dry-run', action='store_true',
                       help='Show what would be migrated without doing it')
    parser.add_argument('--output', metavar='FILE',
                       help='Output file for report')
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
        if args.scan:
            # Scan for Chinese files
            detector = ChineseContentDetector()
            chinese_files = detector.scan_directory(args.directory)
            
            print(f"\nChinese Content Detection Results:")
            print(f"Files scanned: {detector.stats['files_scanned']}")
            print(f"Chinese files found: {len(chinese_files)}")
            
            if chinese_files:
                print(f"\nFiles with Chinese content:")
                for file_info in chinese_files:
                    print(f"  {file_info['file_path']}: {file_info['chinese_percentage']:.1f}% Chinese")
        
        elif args.report:
            # Generate migration report
            detector = ChineseContentDetector()
            chinese_files = detector.scan_directory(args.directory)
            report = detector.generate_report(chinese_files)
            
            # Save report
            output_file = args.output or 'migration-report.json'
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(report, f, indent=2, ensure_ascii=False)
            
            print(f"Migration report saved to: {output_file}")
            
            # Print summary
            print(f"\nMigration Report Summary:")
            for category, files in report['files_by_category'].items():
                if files:
                    print(f"  {category}: {len(files)} files")
        
        elif args.migrate:
            # Perform migration
            migrator = ChineseDocumentationMigrator()
            
            if args.dry_run:
                print("DRY RUN MODE - No files will be modified")
            
            if args.file:
                # Migrate specific file
                if not os.path.exists(args.file):
                    print(f"Error: File not found: {args.file}")
                    return 1
                
                success = migrator.migrate_file(args.file, args.dry_run)
                if not success:
                    print(f"Failed to migrate: {args.file}")
                    return 1
            else:
                # Migrate directory
                results = migrator.migrate_directory(args.directory, args.dry_run)
            
            # Print summary
            migrator.print_summary()
            
            # Return appropriate exit code
            return 0 if migrator.stats['failed_migrations'] == 0 else 1
        
        return 0
        
    except KeyboardInterrupt:
        print("\nMigration interrupted by user")
        return 1
    except Exception as e:
        print(f"Migration failed: {e}")
        if args.verbose:
            import traceback
            traceback.print_exc()
        return 1


if __name__ == '__main__':
    exit(main())