#!/usr/bin/env python3
"""
Unit tests for Kiro AI translation integration.

This module tests the core translation functionality including
AI integration, markdown formatting preservation, and error handling.
"""

import unittest
import tempfile
import os
import sys
from pathlib import Path
from unittest.mock import Mock, patch, MagicMock

# Add scripts directory to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'scripts'))

from kiro_translator import KiroTranslator, TranslationError

class TestKiroTranslator(unittest.TestCase):
    """Test cases for KiroTranslator class."""
    
    def setUp(self):
        """Set up test fixtures."""
        self.translator = KiroTranslator()
        self.temp_dir = tempfile.mkdtemp()
        
        # Sample markdown content for testing
        self.sample_markdown = """# Test Document

This is a test document for translation testing.

## Features

- Feature 1
- Feature 2
- Feature 3

### Code Example

```python
def hello_world():
    print("Hello, World!")
    return True
```

### Table Example

| Column 1 | Column 2 | Column 3 |
|----------|----------|----------|
| Value 1  | Value 2  | Value 3  |
| Data A   | Data B   | Data C   |

**Bold text** and *italic text*.

[Link to documentation](https://example.com/docs)
"""
    
    def tearDown(self):
        """Clean up test fixtures."""
        import shutil
        shutil.rmtree(self.temp_dir, ignore_errors=True)
    
    def test_translator_initialization(self):
        """Test translator initialization."""
        translator = KiroTranslator()
        
        self.assertIsNotNone(translator.config)
        self.assertIn('target_language', translator.config)
        self.assertEqual(translator.config['target_language'], 'zh-TW')
        self.assertIn('preserve_terms', translator.config)
    
    def test_create_chinese_filename(self):
        """Test Chinese filename generation."""
        test_cases = [
            ('README.md', 'README.zh-TW.md'),
            ('guide.md', 'guide.zh-TW.md'),
            ('api-reference.md', 'api-reference.zh-TW.md'),
            ('file.with.dots.md', 'file.with.dots.zh-TW.md'),
            ('no-extension', 'no-extension.zh-TW'),
        ]
        
        for input_filename, expected_output in test_cases:
            with self.subTest(input_filename=input_filename):
                result = self.translator.create_chinese_filename(input_filename)
                self.assertEqual(result, expected_output)
    
    def test_extract_markdown_content(self):
        """Test markdown content extraction."""
        # Create test file
        test_file = Path(self.temp_dir) / 'test.md'
        with open(test_file, 'w', encoding='utf-8') as f:
            f.write(self.sample_markdown)
        
        content = self.translator.extract_markdown_content(str(test_file))
        
        self.assertIn('# Test Document', content)
        self.assertIn('def hello_world():', content)
        self.assertIn('| Column 1 | Column 2 | Column 3 |', content)
        self.assertIn('**Bold text**', content)
    
    def test_extract_markdown_content_file_not_found(self):
        """Test markdown content extraction with non-existent file."""
        with self.assertRaises(FileNotFoundError):
            self.translator.extract_markdown_content('non_existent_file.md')
    
    def test_extract_markdown_content_encoding_error(self):
        """Test markdown content extraction with encoding issues."""
        # Create file with invalid UTF-8
        test_file = Path(self.temp_dir) / 'invalid_encoding.md'
        with open(test_file, 'wb') as f:
            f.write(b'\xff\xfe# Invalid encoding\n')
        
        # Should handle encoding gracefully
        content = self.translator.extract_markdown_content(str(test_file))
        self.assertIsInstance(content, str)
    
    def test_preserve_code_blocks(self):
        """Test code block preservation during translation."""
        content_with_code = """# Test

Here is some code:

```python
def test_function():
    return "Hello, World!"
```

And inline code: `print("test")`
"""
        
        # Mock translation to verify code blocks are preserved
        with patch.object(self.translator, '_translate_with_kiro') as mock_translate:
            mock_translate.return_value = "# 測試\n\n這是一些代碼：\n\n```python\ndef test_function():\n    return \"Hello, World!\"\n```\n\n和內聯代碼：`print(\"test\")`"
            
            result = self.translator._translate_with_kiro(content_with_code)
            
            # Verify code blocks are preserved
            self.assertIn('```python', result)
            self.assertIn('def test_function():', result)
            self.assertIn('`print("test")`', result)
    
    def test_preserve_links(self):
        """Test link preservation during translation."""
        content_with_links = """# Test Document

Visit [our website](https://example.com) for more information.

See also: [API Reference](../api/reference.md)
"""
        
        with patch.object(self.translator, '_translate_with_kiro') as mock_translate:
            mock_translate.return_value = "# 測試文檔\n\n訪問[我們的網站](https://example.com)獲取更多信息。\n\n另請參閱：[API 參考](../api/reference.md)"
            
            result = self.translator._translate_with_kiro(content_with_links)
            
            # Verify links are preserved
            self.assertIn('[我們的網站](https://example.com)', result)
            self.assertIn('[API 參考](../api/reference.md)', result)
    
    def test_preserve_technical_terms(self):
        """Test technical term preservation."""
        content_with_terms = """# API Documentation

This API uses REST principles and returns JSON responses.

## Docker Setup

Use Docker and Kubernetes for deployment.
"""
        
        with patch.object(self.translator, '_translate_with_kiro') as mock_translate:
            # Mock should preserve technical terms
            mock_translate.return_value = "# API 文檔\n\n此 API 使用 REST 原則並返回 JSON 響應。\n\n## Docker 設置\n\n使用 Docker 和 Kubernetes 進行部署。"
            
            result = self.translator._translate_with_kiro(content_with_terms)
            
            # Verify technical terms are preserved
            self.assertIn('API', result)
            self.assertIn('REST', result)
            self.assertIn('JSON', result)
            self.assertIn('Docker', result)
            self.assertIn('Kubernetes', result)
    
    @patch('kiro_translator.KiroTranslator._translate_with_kiro')
    def test_translate_and_save_success(self, mock_translate):
        """Test successful translation and file saving."""
        # Setup
        source_file = Path(self.temp_dir) / 'source.md'
        with open(source_file, 'w', encoding='utf-8') as f:
            f.write(self.sample_markdown)
        
        mock_translate.return_value = "# 測試文檔\n\n這是一個用於翻譯測試的測試文檔。"
        
        # Execute
        result_file = self.translator.translate_and_save(str(source_file))
        
        # Verify
        expected_file = Path(self.temp_dir) / 'source.zh-TW.md'
        self.assertEqual(result_file, str(expected_file))
        self.assertTrue(expected_file.exists())
        
        # Verify content
        with open(expected_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        self.assertIn('# 測試文檔', content)
        mock_translate.assert_called_once()
    
    @patch('kiro_translator.KiroTranslator._translate_with_kiro')
    def test_translate_and_save_translation_error(self, mock_translate):
        """Test translation error handling."""
        # Setup
        source_file = Path(self.temp_dir) / 'source.md'
        with open(source_file, 'w', encoding='utf-8') as f:
            f.write(self.sample_markdown)
        
        mock_translate.side_effect = TranslationError("API timeout")
        
        # Execute and verify exception
        with self.assertRaises(TranslationError) as context:
            self.translator.translate_and_save(str(source_file))
        
        self.assertIn("API timeout", str(context.exception))
    
    def test_translate_and_save_file_not_found(self):
        """Test translation with non-existent source file."""
        with self.assertRaises(FileNotFoundError):
            self.translator.translate_and_save('non_existent_file.md')
    
    @patch('kiro_translator.KiroTranslator._translate_with_kiro')
    def test_translate_and_save_permission_error(self, mock_translate):
        """Test translation with permission errors."""
        # Setup
        source_file = Path(self.temp_dir) / 'source.md'
        with open(source_file, 'w', encoding='utf-8') as f:
            f.write(self.sample_markdown)
        
        mock_translate.return_value = "# 測試文檔"
        
        # Mock permission error on file write
        with patch('builtins.open', side_effect=PermissionError("Permission denied")):
            with self.assertRaises(PermissionError):
                self.translator.translate_and_save(str(source_file))
    
    def test_validate_markdown_structure(self):
        """Test markdown structure validation."""
        valid_markdown = """# Title

## Section

- List item
- Another item

```python
code block
```
"""
        
        invalid_markdown = """# Title
## Missing newline
- List item
```python
unclosed code block
"""
        
        # Valid markdown should pass
        self.assertTrue(self.translator.validate_markdown_structure(valid_markdown))
        
        # Invalid markdown should fail
        self.assertFalse(self.translator.validate_markdown_structure(invalid_markdown))
    
    def test_config_loading(self):
        """Test configuration loading."""
        # Test default config
        translator = KiroTranslator()
        
        self.assertEqual(translator.config['target_language'], 'zh-TW')
        self.assertIn('API', translator.config['preserve_terms'])
        self.assertIn('DDD', translator.config['preserve_terms'])
    
    def test_error_handling_network_timeout(self):
        """Test network timeout error handling."""
        with patch.object(self.translator, '_translate_with_kiro') as mock_translate:
            mock_translate.side_effect = TranslationError("Network timeout")
            
            with self.assertRaises(TranslationError) as context:
                self.translator._translate_with_kiro("test content")
            
            self.assertIn("Network timeout", str(context.exception))
    
    def test_error_handling_api_rate_limit(self):
        """Test API rate limit error handling."""
        with patch.object(self.translator, '_translate_with_kiro') as mock_translate:
            mock_translate.side_effect = TranslationError("Rate limit exceeded")
            
            with self.assertRaises(TranslationError) as context:
                self.translator._translate_with_kiro("test content")
            
            self.assertIn("Rate limit exceeded", str(context.exception))
    
    def test_large_content_handling(self):
        """Test handling of large content."""
        # Create large content (simulate large document)
        large_content = "# Large Document\n\n" + "This is a test paragraph. " * 1000
        
        with patch.object(self.translator, '_translate_with_kiro') as mock_translate:
            mock_translate.return_value = "# 大型文檔\n\n" + "這是一個測試段落。" * 1000
            
            result = self.translator._translate_with_kiro(large_content)
            
            self.assertIn('# 大型文檔', result)
            self.assertTrue(len(result) > 1000)
    
    def test_empty_content_handling(self):
        """Test handling of empty content."""
        empty_content = ""
        
        with patch.object(self.translator, '_translate_with_kiro') as mock_translate:
            mock_translate.return_value = ""
            
            result = self.translator._translate_with_kiro(empty_content)
            
            self.assertEqual(result, "")
    
    def test_special_characters_handling(self):
        """Test handling of special characters."""
        content_with_special_chars = """# Test with Special Characters

Emoji: 🚀 🎉 ✅ ❌

Special symbols: © ® ™ § ¶

Unicode: αβγδε 中文测试 العربية

Math symbols: ∑ ∏ ∫ √ ∞
"""
        
        with patch.object(self.translator, '_translate_with_kiro') as mock_translate:
            mock_translate.return_value = """# 特殊字符測試

表情符號：🚀 🎉 ✅ ❌

特殊符號：© ® ™ § ¶

Unicode：αβγδε 中文测试 العربية

數學符號：∑ ∏ ∫ √ ∞
"""
            
            result = self.translator._translate_with_kiro(content_with_special_chars)
            
            # Verify special characters are preserved
            self.assertIn('🚀', result)
            self.assertIn('©', result)
            self.assertIn('αβγδε', result)
            self.assertIn('∑', result)

class TestTranslationError(unittest.TestCase):
    """Test cases for TranslationError exception."""
    
    def test_translation_error_creation(self):
        """Test TranslationError creation."""
        error = TranslationError("Test error message")
        
        self.assertEqual(str(error), "Test error message")
        self.assertIsInstance(error, Exception)
    
    def test_translation_error_with_cause(self):
        """Test TranslationError with underlying cause."""
        original_error = ValueError("Original error")
        error = TranslationError("Translation failed", original_error)
        
        self.assertEqual(str(error), "Translation failed")
        self.assertEqual(error.__cause__, original_error)

if __name__ == '__main__':
    # Run tests with verbose output
    unittest.main(verbosity=2)