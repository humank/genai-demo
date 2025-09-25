#!/usr/bin/env python3
"""
Unit tests for DocumentationTranslator.

This module tests the main translation script functionality including
file management, directory scanning, and batch processing.
"""

import unittest
import tempfile
import os
import sys
import json
from pathlib import Path
from unittest.mock import Mock, patch, MagicMock

# Add scripts directory to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'scripts'))

from translate_docs import DocumentationTranslator

class TestDocumentationTranslator(unittest.TestCase):
    """Test cases for DocumentationTranslator class."""
    
    def setUp(self):
        """Set up test fixtures."""
        self.temp_dir = tempfile.mkdtemp()
        self.translator = DocumentationTranslator()
        
        # Create test files
        self.test_files = {
            'README.md': '# Test README\n\nThis is a test README file.',
            'docs/guide.md': '# User Guide\n\nThis is a user guide.',
            'docs/api.md': '# API Reference\n\nAPI documentation.',
            'src/code.py': 'print("This is not a markdown file")',
            'docs/existing.zh-TW.md': '# 現有中文文檔\n\n這是現有的中文文檔。',
            'node_modules/package.md': '# Package\n\nThis should be excluded.',
            '.git/config.md': '# Git Config\n\nThis should be excluded.'
        }
        
        # Create test directory structure and files
        for file_path, content in self.test_files.items():
            full_path = Path(self.temp_dir) / file_path
            full_path.parent.mkdir(parents=True, exist_ok=True)
            
            if file_path.endswith('.py'):
                with open(full_path, 'w', encoding='utf-8') as f:
                    f.write(content)
            else:
                with open(full_path, 'w', encoding='utf-8') as f:
                    f.write(content)
    
    def tearDown(self):
        """Clean up test fixtures."""
        import shutil
        shutil.rmtree(self.temp_dir, ignore_errors=True)
    
    def test_translator_initialization(self):
        """Test translator initialization."""
        translator = DocumentationTranslator()
        
        self.assertIsNotNone(translator.translator)
        self.assertIsNotNone(translator.config)
        self.assertIsInstance(translator.stats, dict)
        self.assertEqual(translator.stats['processed'], 0)
    
    def test_config_loading_default(self):
        """Test default configuration loading."""
        translator = DocumentationTranslator()
        
        self.assertIn('include_patterns', translator.config)
        self.assertIn('exclude_patterns', translator.config)
        self.assertIn('preserve_terms', translator.config)
        self.assertEqual(translator.config['include_patterns'], ['**/*.md'])
        self.assertIn('**/*.zh-TW.md', translator.config['exclude_patterns'])
    
    def test_config_loading_from_file(self):
        """Test configuration loading from file."""
        # Create test config file
        config_file = Path(self.temp_dir) / 'test_config.json'
        test_config = {
            'include_patterns': ['**/*.markdown'],
            'preserve_terms': ['TEST', 'CUSTOM']
        }
        
        with open(config_file, 'w', encoding='utf-8') as f:
            json.dump(test_config, f)
        
        translator = DocumentationTranslator(str(config_file))
        
        self.assertEqual(translator.config['include_patterns'], ['**/*.markdown'])
        self.assertIn('TEST', translator.config['preserve_terms'])
        self.assertIn('CUSTOM', translator.config['preserve_terms'])
    
    def test_config_loading_invalid_file(self):
        """Test configuration loading with invalid file."""
        # Test with non-existent file
        translator = DocumentationTranslator('non_existent_config.json')
        
        # Should fall back to default config
        self.assertEqual(translator.config['include_patterns'], ['**/*.md'])
    
    def test_scan_directory_basic(self):
        """Test basic directory scanning."""
        files = self.translator.scan_directory(self.temp_dir)
        
        # Should find markdown files but exclude Chinese and ignored files
        expected_files = [
            str(Path(self.temp_dir) / 'README.md'),
            str(Path(self.temp_dir) / 'docs' / 'guide.md'),
            str(Path(self.temp_dir) / 'docs' / 'api.md')
        ]
        
        self.assertEqual(len(files), 3)
        for expected_file in expected_files:
            self.assertIn(expected_file, files)
        
        # Should not include excluded files
        self.assertNotIn(str(Path(self.temp_dir) / 'docs' / 'existing.zh-TW.md'), files)
        self.assertNotIn(str(Path(self.temp_dir) / 'node_modules' / 'package.md'), files)
        self.assertNotIn(str(Path(self.temp_dir) / '.git' / 'config.md'), files)
        self.assertNotIn(str(Path(self.temp_dir) / 'src' / 'code.py'), files)
    
    def test_scan_directory_nonexistent(self):
        """Test directory scanning with non-existent directory."""
        with self.assertRaises(ValueError) as context:
            self.translator.scan_directory('/non/existent/directory')
        
        self.assertIn('Directory does not exist', str(context.exception))
    
    def test_needs_translation_new_file(self):
        """Test needs_translation with new file (no Chinese version)."""
        source_file = Path(self.temp_dir) / 'README.md'
        
        result = self.translator.needs_translation(str(source_file))
        
        self.assertTrue(result)
    
    def test_needs_translation_existing_newer(self):
        """Test needs_translation with existing Chinese file that's older."""
        source_file = Path(self.temp_dir) / 'README.md'
        chinese_file = Path(self.temp_dir) / 'README.zh-TW.md'
        
        # Create Chinese file first (older)
        with open(chinese_file, 'w', encoding='utf-8') as f:
            f.write('# 測試 README')
        
        # Touch source file to make it newer
        import time
        time.sleep(0.1)
        source_file.touch()
        
        result = self.translator.needs_translation(str(source_file))
        
        self.assertTrue(result)
    
    def test_needs_translation_existing_newer_chinese(self):
        """Test needs_translation with existing Chinese file that's newer."""
        source_file = Path(self.temp_dir) / 'README.md'
        chinese_file = Path(self.temp_dir) / 'README.zh-TW.md'
        
        # Create Chinese file after source file (newer)
        import time
        time.sleep(0.1)
        with open(chinese_file, 'w', encoding='utf-8') as f:
            f.write('# 測試 README')
        
        result = self.translator.needs_translation(str(source_file))
        
        self.assertFalse(result)
    
    @patch('translate_docs.DocumentationTranslator.translate_file')
    def test_translate_directory_success(self, mock_translate_file):
        """Test successful directory translation."""
        mock_translate_file.return_value = True
        
        result = self.translator.translate_directory(self.temp_dir)
        
        # Should process 3 markdown files
        self.assertEqual(self.translator.stats['processed'], 3)
        self.assertEqual(mock_translate_file.call_count, 3)
        
        # Check result structure
        self.assertIn('stats', result)
        self.assertIn('log', result)
        self.assertIn('timestamp', result)
    
    @patch('translate_docs.DocumentationTranslator.translate_file')
    def test_translate_directory_with_failures(self, mock_translate_file):
        """Test directory translation with some failures."""
        # Mock some successes and some failures
        mock_translate_file.side_effect = [True, False, True]
        
        result = self.translator.translate_directory(self.temp_dir)
        
        self.assertEqual(self.translator.stats['processed'], 3)
        self.assertEqual(self.translator.stats['successful'], 2)
        self.assertEqual(self.translator.stats['failed'], 1)
    
    @patch('translate_docs.KiroTranslator')
    def test_translate_file_success(self, mock_kiro_translator):
        """Test successful file translation."""
        # Setup mock
        mock_translator_instance = Mock()
        mock_translator_instance.translate_and_save.return_value = str(Path(self.temp_dir) / 'README.zh-TW.md')
        mock_kiro_translator.return_value = mock_translator_instance
        
        # Create fresh translator instance
        translator = DocumentationTranslator()
        
        source_file = str(Path(self.temp_dir) / 'README.md')
        result = translator.translate_file(source_file, force=True)
        
        self.assertTrue(result)
        self.assertEqual(translator.stats['successful'], 1)
        mock_translator_instance.translate_and_save.assert_called_once_with(source_file)
    
    @patch('translate_docs.KiroTranslator')
    def test_translate_file_failure(self, mock_kiro_translator):
        """Test file translation failure."""
        # Setup mock to raise exception
        mock_translator_instance = Mock()
        mock_translator_instance.translate_and_save.side_effect = Exception("Translation failed")
        mock_kiro_translator.return_value = mock_translator_instance
        
        # Create fresh translator instance
        translator = DocumentationTranslator()
        
        source_file = str(Path(self.temp_dir) / 'README.md')
        result = translator.translate_file(source_file, force=True)
        
        self.assertFalse(result)
        self.assertEqual(translator.stats['failed'], 1)
    
    def test_translate_file_skip_up_to_date(self):
        """Test skipping translation for up-to-date files."""
        source_file = Path(self.temp_dir) / 'README.md'
        chinese_file = Path(self.temp_dir) / 'README.zh-TW.md'
        
        # Create Chinese file that's newer
        import time
        time.sleep(0.1)
        with open(chinese_file, 'w', encoding='utf-8') as f:
            f.write('# 測試 README')
        
        result = self.translator.translate_file(str(source_file), force=False)
        
        self.assertTrue(result)  # Returns True for skipped files
        self.assertEqual(self.translator.stats['skipped'], 1)
    
    def test_translate_file_force_translation(self):
        """Test forced translation even when file is up-to-date."""
        source_file = Path(self.temp_dir) / 'README.md'
        chinese_file = Path(self.temp_dir) / 'README.zh-TW.md'
        
        # Create Chinese file that's newer
        import time
        time.sleep(0.1)
        with open(chinese_file, 'w', encoding='utf-8') as f:
            f.write('# 測試 README')
        
        with patch.object(self.translator.translator, 'translate_and_save') as mock_translate:
            mock_translate.return_value = str(chinese_file)
            
            result = self.translator.translate_file(str(source_file), force=True)
            
            self.assertTrue(result)
            mock_translate.assert_called_once()
    
    def test_create_backup(self):
        """Test backup creation."""
        source_file = Path(self.temp_dir) / 'README.md'
        
        # Enable backup in config
        self.translator.config['backup_enabled'] = True
        
        self.translator._create_backup(str(source_file))
        
        # Check backup directory and file exist
        backup_dir = source_file.parent / '.backup'
        self.assertTrue(backup_dir.exists())
        
        backup_files = list(backup_dir.glob('README_*.md'))
        self.assertEqual(len(backup_files), 1)
    
    def test_create_backup_disabled(self):
        """Test backup creation when disabled."""
        source_file = Path(self.temp_dir) / 'README.md'
        
        # Disable backup in config
        self.translator.config['backup_enabled'] = False
        
        self.translator._create_backup(str(source_file))
        
        # Check backup directory doesn't exist
        backup_dir = source_file.parent / '.backup'
        self.assertFalse(backup_dir.exists())
    
    def test_save_translation_log(self):
        """Test translation log saving."""
        # Add some log entries
        self.translator.translation_log = {
            'file1.md': {'status': 'success', 'timestamp': '2025-01-01T00:00:00'},
            'file2.md': {'status': 'failed', 'error': 'Test error'}
        }
        
        self.translator._save_translation_log()
        
        # Check log file exists
        log_file = Path(self.translator.config['log_file'])
        self.assertTrue(log_file.exists())
        
        # Check log content
        with open(log_file, 'r', encoding='utf-8') as f:
            log_data = json.load(f)
        
        self.assertIn('file1.md', log_data)
        self.assertIn('file2.md', log_data)
        self.assertEqual(log_data['file1.md']['status'], 'success')
        self.assertEqual(log_data['file2.md']['status'], 'failed')
        
        # Cleanup
        log_file.unlink()
    
    def test_get_results(self):
        """Test results generation."""
        # Set some stats
        self.translator.stats = {
            'processed': 5,
            'successful': 4,
            'failed': 1,
            'skipped': 0
        }
        
        results = self.translator._get_results()
        
        self.assertIn('stats', results)
        self.assertIn('log', results)
        self.assertIn('timestamp', results)
        self.assertEqual(results['stats']['processed'], 5)
        self.assertEqual(results['stats']['successful'], 4)
        self.assertEqual(results['stats']['failed'], 1)
    
    def test_print_summary(self):
        """Test summary printing."""
        # Set some stats and log entries
        self.translator.stats = {
            'processed': 3,
            'successful': 2,
            'failed': 1,
            'skipped': 0
        }
        
        self.translator.translation_log = {
            'failed_file.md': {
                'status': 'failed',
                'error': 'Test error message'
            }
        }
        
        # Capture output
        import io
        from contextlib import redirect_stdout
        
        output = io.StringIO()
        with redirect_stdout(output):
            self.translator.print_summary()
        
        output_text = output.getvalue()
        
        self.assertIn('TRANSLATION SUMMARY', output_text)
        self.assertIn('Files processed: 3', output_text)
        self.assertIn('Successful: 2', output_text)
        self.assertIn('Failed: 1', output_text)
        self.assertIn('failed_file.md', output_text)
        self.assertIn('Test error message', output_text)
    
    def test_dry_run_mode(self):
        """Test dry run mode."""
        self.translator.config['dry_run'] = True
        
        source_file = str(Path(self.temp_dir) / 'README.md')
        result = self.translator.translate_file(source_file, force=True)
        
        self.assertTrue(result)
        self.assertEqual(self.translator.stats['successful'], 1)
        
        # Chinese file should not be created in dry run mode
        chinese_file = Path(self.temp_dir) / 'README.zh-TW.md'
        self.assertFalse(chinese_file.exists())

class TestDocumentationTranslatorIntegration(unittest.TestCase):
    """Integration tests for DocumentationTranslator."""
    
    def setUp(self):
        """Set up test fixtures."""
        self.temp_dir = tempfile.mkdtemp()
        
        # Create a more complex directory structure
        self.test_structure = {
            'README.md': '# Project README\n\nMain project documentation.',
            'docs/user-guide.md': '# User Guide\n\nHow to use the system.',
            'docs/api/reference.md': '# API Reference\n\nAPI documentation.',
            'docs/tutorials/getting-started.md': '# Getting Started\n\nTutorial content.',
            'src/README.md': '# Source Code\n\nSource code documentation.',
            'tests/README.md': '# Tests\n\nTest documentation.',
            'config.json': '{"setting": "value"}',  # Non-markdown file
            'docs/existing.zh-TW.md': '# 現有文檔\n\n已存在的中文文檔。'
        }
        
        for file_path, content in self.test_structure.items():
            full_path = Path(self.temp_dir) / file_path
            full_path.parent.mkdir(parents=True, exist_ok=True)
            with open(full_path, 'w', encoding='utf-8') as f:
                f.write(content)
    
    def tearDown(self):
        """Clean up test fixtures."""
        import shutil
        shutil.rmtree(self.temp_dir, ignore_errors=True)
    
    @patch('translate_docs.KiroTranslator')
    def test_full_directory_translation(self, mock_kiro_translator):
        """Test full directory translation workflow."""
        # Setup mock translator
        mock_translator_instance = Mock()
        
        def mock_translate_and_save(source_file):
            # Generate Chinese filename
            source_path = Path(source_file)
            chinese_filename = source_path.stem + '.zh-TW' + source_path.suffix
            chinese_path = source_path.parent / chinese_filename
            
            # Create mock Chinese file
            with open(chinese_path, 'w', encoding='utf-8') as f:
                f.write(f'# 翻譯的 {source_path.stem}')
            
            return str(chinese_path)
        
        mock_translator_instance.translate_and_save.side_effect = mock_translate_and_save
        mock_kiro_translator.return_value = mock_translator_instance
        
        # Create translator and run
        translator = DocumentationTranslator()
        result = translator.translate_directory(self.temp_dir)
        
        # Verify results
        self.assertEqual(result['stats']['processed'], 5)  # 5 markdown files
        self.assertEqual(result['stats']['successful'], 5)
        self.assertEqual(result['stats']['failed'], 0)
        
        # Verify Chinese files were created
        expected_chinese_files = [
            'README.zh-TW.md',
            'docs/user-guide.zh-TW.md',
            'docs/api/reference.zh-TW.md',
            'docs/tutorials/getting-started.zh-TW.md',
            'src/README.zh-TW.md',
            'tests/README.zh-TW.md'
        ]
        
        for chinese_file in expected_chinese_files:
            chinese_path = Path(self.temp_dir) / chinese_file
            self.assertTrue(chinese_path.exists(), f"Chinese file not created: {chinese_file}")

if __name__ == '__main__':
    # Run tests with verbose output
    unittest.main(verbosity=2)