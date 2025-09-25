#!/usr/bin/env python3
"""
Integration tests for the automated documentation translation system.

This module tests complete workflows from file change to output,
including Kiro Hook integration and end-to-end translation processes.
"""

import unittest
import tempfile
import os
import sys
import json
import time
import subprocess
from pathlib import Path
from unittest.mock import Mock, patch, MagicMock

# Add scripts directory to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'scripts'))

from translate_docs import DocumentationTranslator
from batch_processor import BatchProcessor
from kiro_translator import KiroTranslator
from migration_workflow import MigrationWorkflow
from quality_assurance import QualityAssurance

class TestEndToEndTranslationWorkflow(unittest.TestCase):
    """Test complete translation workflows."""
    
    def setUp(self):
        """Set up test fixtures."""
        self.temp_dir = tempfile.mkdtemp()
        self.test_project_structure = {
            'README.md': '''# Test Project

This is a test project for translation testing.

## Features

- Feature 1: Basic functionality
- Feature 2: Advanced features
- Feature 3: Integration capabilities

## Installation

```bash
npm install test-project
```

## Usage

```python
from test_project import TestClass

test = TestClass()
test.run()
```

## API Reference

See [API documentation](docs/api.md) for details.
''',
            'docs/user-guide.md': '''# User Guide

Welcome to the user guide for our test project.

## Getting Started

1. Install the project
2. Configure your settings
3. Run the application

### Configuration

Edit the `config.json` file:

```json
{
  "setting1": "value1",
  "setting2": "value2"
}
```

### Running

Execute the following command:

```bash
python main.py --config config.json
```
''',
            'docs/api.md': '''# API Reference

## Classes

### TestClass

Main class for testing functionality.

#### Methods

- `run()`: Execute the test
- `configure(options)`: Set configuration options
- `get_results()`: Retrieve test results

## Functions

### helper_function(param1, param2)

Utility function for processing data.

**Parameters:**
- `param1` (str): First parameter
- `param2` (int): Second parameter

**Returns:**
- `dict`: Processed results
''',
            'docs/tutorials/getting-started.md': '''# Getting Started Tutorial

This tutorial will guide you through the basics.

## Prerequisites

- Python 3.8+
- Node.js 14+
- Git

## Step 1: Installation

Clone the repository:

```bash
git clone https://github.com/example/test-project.git
cd test-project
```

Install dependencies:

```bash
pip install -r requirements.txt
npm install
```

## Step 2: Configuration

Create a configuration file:

```yaml
database:
  host: localhost
  port: 5432
  name: testdb

api:
  key: your-api-key
  endpoint: https://api.example.com
```

## Step 3: Running Tests

Execute the test suite:

```bash
python -m pytest tests/
```
'''
        }
        
        # Create test project structure
        for file_path, content in self.test_project_structure.items():
            full_path = Path(self.temp_dir) / file_path
            full_path.parent.mkdir(parents=True, exist_ok=True)
            with open(full_path, 'w', encoding='utf-8') as f:
                f.write(content)
    
    def tearDown(self):
        """Clean up test fixtures."""
        import shutil
        shutil.rmtree(self.temp_dir, ignore_errors=True)
    
    @patch('kiro_translator.KiroTranslator._translate_with_kiro')
    def test_complete_translation_workflow(self, mock_translate):
        """Test complete translation workflow from English to Chinese."""
        # Setup mock translation responses
        def mock_translation_response(content):
            # Simple mock translation - replace key English terms with Chinese
            translations = {
                'Test Project': '測試專案',
                'Features': '功能',
                'Installation': '安裝',
                'Usage': '使用方法',
                'API Reference': 'API 參考',
                'User Guide': '使用者指南',
                'Getting Started': '入門指南',
                'Configuration': '配置',
                'Prerequisites': '先決條件',
                'Tutorial': '教學'
            }
            
            translated = content
            for english, chinese in translations.items():
                translated = translated.replace(english, chinese)
            
            return translated
        
        mock_translate.side_effect = mock_translation_response
        
        # Create translator and process directory
        translator = DocumentationTranslator()
        result = translator.translate_directory(self.temp_dir)
        
        # Verify translation results
        self.assertEqual(result['stats']['processed'], 4)  # 4 markdown files
        self.assertEqual(result['stats']['successful'], 4)
        self.assertEqual(result['stats']['failed'], 0)
        
        # Verify Chinese files were created with correct content
        expected_files = [
            ('README.zh-TW.md', '測試專案'),
            ('docs/user-guide.zh-TW.md', '使用者指南'),
            ('docs/api.zh-TW.md', 'API 參考'),
            ('docs/tutorials/getting-started.zh-TW.md', '入門指南')
        ]
        
        for chinese_file, expected_content in expected_files:
            chinese_path = Path(self.temp_dir) / chinese_file
            self.assertTrue(chinese_path.exists(), f"Chinese file not created: {chinese_file}")
            
            with open(chinese_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            self.assertIn(expected_content, content, f"Expected content not found in {chinese_file}")
            
            # Verify code blocks are preserved
            if '```' in self.test_project_structure.get(chinese_file.replace('.zh-TW', ''), ''):
                self.assertIn('```', content, f"Code blocks not preserved in {chinese_file}")
    
    @patch('batch_processor.DocumentationTranslator')
    def test_batch_processing_workflow(self, mock_doc_translator):
        """Test batch processing workflow."""
        # Setup mock
        mock_translator_instance = Mock()
        mock_translator_instance.needs_translation.return_value = True
        mock_translator_instance.translate_file.return_value = True
        mock_doc_translator.return_value = mock_translator_instance
        
        # Create batch processor
        processor = BatchProcessor(max_workers=2)
        
        # Get list of markdown files
        markdown_files = []
        for file_path in self.test_project_structure.keys():
            if file_path.endswith('.md'):
                markdown_files.append(str(Path(self.temp_dir) / file_path))
        
        # Create and process batch job
        job_id = processor.create_translation_job(markdown_files)
        results = processor.process_job(job_id, show_progress=False)
        
        # Verify batch processing results
        self.assertIn('successful', results)
        self.assertIn('failed', results)
        self.assertIn('skipped', results)
        self.assertEqual(len(results['successful']), 4)
        self.assertEqual(len(results['failed']), 0)
        
        # Verify job status
        job_status = processor.get_job_status(job_id)
        self.assertEqual(job_status['status'], 'completed')
        self.assertEqual(job_status['progress'], 100.0)
    
    def test_file_watcher_integration(self):
        """Test file watcher integration workflow."""
        # This test simulates the file watcher workflow
        # In a real scenario, this would test the watch-docs.py script
        
        # Create a new markdown file
        new_file = Path(self.temp_dir) / 'new-document.md'
        with open(new_file, 'w', encoding='utf-8') as f:
            f.write('# New Document\n\nThis is a new document.')
        
        # Simulate file watcher detecting the change
        with patch('kiro_translator.KiroTranslator._translate_with_kiro') as mock_translate:
            mock_translate.return_value = '# 新文檔\n\n這是一個新文檔。'
            
            translator = DocumentationTranslator()
            result = translator.translate_file(str(new_file), force=True)
            
            self.assertTrue(result)
            
            # Verify Chinese file was created
            chinese_file = Path(self.temp_dir) / 'new-document.zh-TW.md'
            self.assertTrue(chinese_file.exists())
            
            with open(chinese_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            self.assertIn('新文檔', content)
    
    @patch('migration_workflow.KiroTranslator')
    def test_migration_workflow(self, mock_kiro_translator):
        """Test migration workflow for existing Chinese documentation."""
        # Setup mock translator
        mock_translator_instance = Mock()
        mock_translator_instance.translate_and_save.return_value = 'translated_file.md'
        mock_kiro_translator.return_value = mock_translator_instance
        
        # Create existing Chinese documentation
        chinese_docs = {
            'docs/中文指南.md': '''# 中文指南

這是一個中文文檔的範例。

## 功能

- 功能一：基本功能
- 功能二：進階功能

## 安裝

```bash
npm install 專案
```
''',
            'docs/API文檔.md': '''# API 文檔

## 類別

### 測試類別

主要的測試功能類別。

#### 方法

- `執行()`: 執行測試
- `配置(選項)`: 設定配置選項
'''
        }
        
        # Create Chinese documentation files
        for file_path, content in chinese_docs.items():
            full_path = Path(self.temp_dir) / file_path
            full_path.parent.mkdir(parents=True, exist_ok=True)
            with open(full_path, 'w', encoding='utf-8') as f:
                f.write(content)
        
        # Create migration workflow
        from file_manager import FileManager
        file_manager = FileManager()
        qa_system = QualityAssurance()
        
        workflow = MigrationWorkflow(
            mock_translator_instance,
            file_manager,
            qa_system
        )
        
        # Execute migration (dry run)
        plan = workflow.create_migration_plan(self.temp_dir, self.temp_dir)
        
        # Verify migration plan
        self.assertIsNotNone(plan)
        self.assertTrue(len(plan.operations) > 0)
        
        # Verify Chinese files were identified
        chinese_files = [op.source for op in plan.operations if '中文' in op.source or 'API文檔' in op.source]
        self.assertTrue(len(chinese_files) > 0)

class TestQualityAssuranceIntegration(unittest.TestCase):
    """Test quality assurance integration."""
    
    def setUp(self):
        """Set up test fixtures."""
        self.temp_dir = tempfile.mkdtemp()
        self.qa_system = QualityAssurance()
    
    def tearDown(self):
        """Clean up test fixtures."""
        import shutil
        shutil.rmtree(self.temp_dir, ignore_errors=True)
    
    def test_translation_quality_validation(self):
        """Test translation quality validation workflow."""
        # Create test files with various quality issues
        test_files = {
            'good-translation.zh-TW.md': '''# 良好的翻譯

這是一個高品質的翻譯範例。

## 功能

- 功能一：基本功能
- 功能二：進階功能

## 程式碼範例

```python
def hello_world():
    print("Hello, World!")
```

[連結到文檔](../docs/api.md)
''',
            'poor-translation.zh-TW.md': '''# Bad Translation

This file has mixed languages and poor formatting.

## 功能

- Feature 1: Basic functionality
- 功能二：進階功能

```python
def hello_world()
    print("Hello, World!"  # Missing closing parenthesis
```

[Broken link](../docs/nonexistent.md)
''',
            'empty-translation.zh-TW.md': '',
            'malformed-markdown.zh-TW.md': '''# Title
## Missing newline before header
- List item
```python
unclosed code block
'''
        }
        
        # Create test files
        for file_path, content in test_files.items():
            full_path = Path(self.temp_dir) / file_path
            with open(full_path, 'w', encoding='utf-8') as f:
                f.write(content)
        
        # Run quality assurance validation
        results = self.qa_system.validate_directory(self.temp_dir)
        
        # Verify results
        self.assertEqual(len(results), 4)
        
        # Check individual file results
        results_by_file = {r.file_path: r for r in results}
        
        # Good translation should pass
        good_result = results_by_file[str(Path(self.temp_dir) / 'good-translation.zh-TW.md')]
        self.assertTrue(good_result.is_valid)
        
        # Poor translation should fail
        poor_result = results_by_file[str(Path(self.temp_dir) / 'poor-translation.zh-TW.md')]
        self.assertFalse(poor_result.is_valid)
        self.assertTrue(len(poor_result.issues) > 0)
        
        # Empty file should fail
        empty_result = results_by_file[str(Path(self.temp_dir) / 'empty-translation.zh-TW.md')]
        self.assertFalse(empty_result.is_valid)
        
        # Malformed markdown should fail
        malformed_result = results_by_file[str(Path(self.temp_dir) / 'malformed-markdown.zh-TW.md')]
        self.assertFalse(malformed_result.is_valid)
    
    def test_terminology_consistency_check(self):
        """Test terminology consistency checking."""
        # Create files with inconsistent terminology
        test_files = {
            'file1.zh-TW.md': '''# API 文檔

這個 API 提供基本功能。
''',
            'file2.zh-TW.md': '''# 應用程式介面文檔

這個應用程式介面提供基本功能。
''',
            'file3.zh-TW.md': '''# API 文檔

這個 API 提供基本功能。
'''
        }
        
        # Create test files
        for file_path, content in test_files.items():
            full_path = Path(self.temp_dir) / file_path
            with open(full_path, 'w', encoding='utf-8') as f:
                f.write(content)
        
        # Run terminology consistency check
        consistency_report = self.qa_system.check_terminology_consistency(self.temp_dir)
        
        # Verify inconsistencies were detected
        self.assertIn('inconsistencies', consistency_report)
        self.assertTrue(len(consistency_report['inconsistencies']) > 0)
        
        # Should detect API vs 應用程式介面 inconsistency
        api_inconsistency = any(
            'API' in inconsistency['terms'] or '應用程式介面' in inconsistency['terms']
            for inconsistency in consistency_report['inconsistencies']
        )
        self.assertTrue(api_inconsistency)

class TestSystemIntegration(unittest.TestCase):
    """Test system-wide integration scenarios."""
    
    def setUp(self):
        """Set up test fixtures."""
        self.temp_dir = tempfile.mkdtemp()
    
    def tearDown(self):
        """Clean up test fixtures."""
        import shutil
        shutil.rmtree(self.temp_dir, ignore_errors=True)
    
    def test_cli_integration(self):
        """Test CLI integration workflow."""
        # Create test file
        test_file = Path(self.temp_dir) / 'test.md'
        with open(test_file, 'w', encoding='utf-8') as f:
            f.write('# Test Document\n\nThis is a test.')
        
        # Test CLI command execution (mock the actual CLI call)
        with patch('subprocess.run') as mock_run:
            mock_run.return_value.returncode = 0
            mock_run.return_value.stdout = 'Translation completed successfully'
            
            # Simulate CLI call
            result = subprocess.run([
                'python', 'scripts/translation-cli.py',
                'translate', '--file', str(test_file)
            ], capture_output=True, text=True)
            
            # Verify CLI was called correctly
            mock_run.assert_called_once()
            call_args = mock_run.call_args[0][0]
            self.assertIn('translation-cli.py', ' '.join(call_args))
            self.assertIn('translate', call_args)
            self.assertIn(str(test_file), call_args)
    
    def test_configuration_integration(self):
        """Test configuration system integration."""
        # Create test configuration
        config_file = Path(self.temp_dir) / 'test-config.json'
        test_config = {
            'translation_service': 'openai',
            'target_language': 'zh-TW',
            'preserve_terms': ['API', 'REST', 'JSON'],
            'include_patterns': ['**/*.md'],
            'exclude_patterns': ['**/*.zh-TW.md', '**/node_modules/**'],
            'backup_enabled': True,
            'quality_checks_enabled': True
        }
        
        with open(config_file, 'w', encoding='utf-8') as f:
            json.dump(test_config, f, indent=2)
        
        # Test configuration loading
        translator = DocumentationTranslator(str(config_file))
        
        # Verify configuration was loaded correctly
        self.assertEqual(translator.config['target_language'], 'zh-TW')
        self.assertIn('API', translator.config['preserve_terms'])
        self.assertTrue(translator.config['backup_enabled'])
        self.assertTrue(translator.config['quality_checks_enabled'])
    
    def test_error_recovery_workflow(self):
        """Test error recovery and resilience."""
        # Create test files
        test_files = {
            'good-file.md': '# Good File\n\nThis file should translate successfully.',
            'problematic-file.md': '# Problematic File\n\nThis file will cause translation errors.',
            'another-good-file.md': '# Another Good File\n\nThis should also work.'
        }
        
        for file_path, content in test_files.items():
            full_path = Path(self.temp_dir) / file_path
            with open(full_path, 'w', encoding='utf-8') as f:
                f.write(content)
        
        # Mock translator to fail on problematic file
        with patch('kiro_translator.KiroTranslator._translate_with_kiro') as mock_translate:
            def mock_translation(content):
                if 'Problematic' in content:
                    raise Exception("Translation API error")
                return content.replace('Good', '良好').replace('Another', '另一個')
            
            mock_translate.side_effect = mock_translation
            
            # Run translation with error recovery
            translator = DocumentationTranslator()
            result = translator.translate_directory(self.temp_dir)
            
            # Verify partial success
            self.assertEqual(result['stats']['processed'], 3)
            self.assertEqual(result['stats']['successful'], 2)
            self.assertEqual(result['stats']['failed'], 1)
            
            # Verify good files were translated
            good_file_chinese = Path(self.temp_dir) / 'good-file.zh-TW.md'
            another_good_file_chinese = Path(self.temp_dir) / 'another-good-file.zh-TW.md'
            
            self.assertTrue(good_file_chinese.exists())
            self.assertTrue(another_good_file_chinese.exists())
            
            # Verify problematic file was not translated
            problematic_file_chinese = Path(self.temp_dir) / 'problematic-file.zh-TW.md'
            self.assertFalse(problematic_file_chinese.exists())
    
    def test_performance_under_load(self):
        """Test system performance under load."""
        # Create many test files
        num_files = 20
        for i in range(num_files):
            test_file = Path(self.temp_dir) / f'test-file-{i:03d}.md'
            with open(test_file, 'w', encoding='utf-8') as f:
                f.write(f'# Test File {i}\n\nContent for test file number {i}.')
        
        # Mock fast translation
        with patch('kiro_translator.KiroTranslator._translate_with_kiro') as mock_translate:
            mock_translate.side_effect = lambda content: content.replace('Test', '測試').replace('Content', '內容')
            
            # Measure translation time
            start_time = time.time()
            
            translator = DocumentationTranslator()
            result = translator.translate_directory(self.temp_dir)
            
            end_time = time.time()
            processing_time = end_time - start_time
            
            # Verify all files were processed
            self.assertEqual(result['stats']['processed'], num_files)
            self.assertEqual(result['stats']['successful'], num_files)
            self.assertEqual(result['stats']['failed'], 0)
            
            # Verify reasonable performance (should complete within reasonable time)
            # This is a rough benchmark - adjust based on system capabilities
            self.assertLess(processing_time, 30.0, "Translation took too long")
            
            # Verify all Chinese files were created
            chinese_files = list(Path(self.temp_dir).glob('*.zh-TW.md'))
            self.assertEqual(len(chinese_files), num_files)

if __name__ == '__main__':
    # Run tests with verbose output
    unittest.main(verbosity=2)