# Translation System API Reference

## Overview

This document provides comprehensive API reference for the Automated Documentation Translation System. The system is built with modular components that can be used programmatically or through the command-line interface.

## Core Modules

### KiroTranslator

The main translation engine that handles AI-powered translation.

#### Class: `KiroTranslator`

```python
from scripts.kiro_translator import KiroTranslator
from config.translation_config import TranslationConfig

config = TranslationConfig()
translator = KiroTranslator(config)
```

##### Methods

###### `__init__(config: TranslationConfig)`

Initialize the translator with configuration.

**Parameters:**
- `config` (TranslationConfig): Configuration object

**Example:**
```python
config = TranslationConfig()
translator = KiroTranslator(config)
```

###### `translate_text(text: str, use_cache: bool = True) -> TranslationResult`

Translate a piece of text.

**Parameters:**
- `text` (str): Text to translate
- `use_cache` (bool): Whether to use translation cache (default: True)

**Returns:**
- `TranslationResult`: Object containing translation results

**Example:**
```python
result = translator.translate_text("Hello, World!")
if result.success:
    print(result.translated_text)
else:
    print(f"Translation failed: {result.error_message}")
```

###### `translate_file(input_file: str, output_file: str) -> bool`

Translate a markdown file.

**Parameters:**
- `input_file` (str): Path to input file
- `output_file` (str): Path to output file

**Returns:**
- `bool`: True if translation was successful, False otherwise

**Example:**
```python
success = translator.translate_file('README.md', 'README.zh-TW.md')
if success:
    print("Translation completed successfully")
```

###### `get_translation_stats() -> Dict[str, any]`

Get translation statistics.

**Returns:**
- `dict`: Dictionary with translation statistics

**Example:**
```python
stats = translator.get_translation_stats()
print(f"API calls made: {stats['api_calls_made']}")
print(f"Cache size: {stats['cache_size']}")
```

#### Class: `TranslationResult`

Data class representing translation results.

**Attributes:**
- `success` (bool): Whether translation was successful
- `original_text` (str): Original text
- `translated_text` (str): Translated text (if successful)
- `error_message` (str): Error message (if failed)
- `confidence_score` (float): Translation confidence score
- `processing_time` (float): Time taken for translation

### BatchProcessor

Handles batch processing of multiple files with parallel execution.

#### Class: `BatchProcessor`

```python
from scripts.batch_processor import BatchProcessor
from scripts.kiro_translator import KiroTranslator

translator = KiroTranslator(config)
processor = BatchProcessor(translator, max_workers=4)
```

##### Methods

###### `__init__(translator, max_workers: int = 4)`

Initialize the batch processor.

**Parameters:**
- `translator`: Translator instance to use
- `max_workers` (int): Maximum number of parallel workers

###### `process_directory(input_dir: str, output_dir: str, **kwargs) -> BatchResult`

Process all files in a directory.

**Parameters:**
- `input_dir` (str): Input directory path
- `output_dir` (str): Output directory path
- `pattern` (str): File pattern to match (default: "*.md")
- `recursive` (bool): Whether to process recursively (default: True)
- `force` (bool): Whether to overwrite existing files (default: False)
- `parallel` (bool): Whether to use parallel processing (default: True)

**Returns:**
- `BatchResult`: Object containing processing results

**Example:**
```python
result = processor.process_directory(
    'docs/',
    'docs/',
    pattern='*.md',
    recursive=True,
    force=False
)

print(f"Processed {result.files_processed} files")
print(f"Errors: {len(result.errors)}")
```

#### Class: `BatchResult`

Data class representing batch processing results.

**Attributes:**
- `success` (bool): Whether batch processing was successful
- `files_processed` (int): Number of files processed
- `files_skipped` (int): Number of files skipped
- `errors` (List[str]): List of error messages
- `processing_time` (float): Total processing time
- `details` (Dict): Additional processing details

### DocumentationTranslator

High-level interface for documentation translation workflows.

#### Class: `DocumentationTranslator`

```python
from scripts.translate_docs import DocumentationTranslator

translator = DocumentationTranslator()
```

##### Methods

###### `__init__(config_file: str = None)`

Initialize the documentation translator.

**Parameters:**
- `config_file` (str): Path to configuration file (optional)

###### `scan_directory(directory: str) -> List[str]`

Scan directory for markdown files to translate.

**Parameters:**
- `directory` (str): Directory to scan

**Returns:**
- `List[str]`: List of file paths to translate

**Example:**
```python
files = translator.scan_directory('docs/')
print(f"Found {len(files)} files to translate")
```

###### `translate_file(file_path: str, force: bool = False) -> bool`

Translate a single file.

**Parameters:**
- `file_path` (str): Path to file to translate
- `force` (bool): Force translation even if up-to-date

**Returns:**
- `bool`: True if successful, False otherwise

###### `translate_directory(directory: str) -> Dict[str, any]`

Translate all files in a directory.

**Parameters:**
- `directory` (str): Directory to translate

**Returns:**
- `dict`: Translation results and statistics

**Example:**
```python
result = translator.translate_directory('docs/')
print(f"Processed: {result['stats']['processed']}")
print(f"Successful: {result['stats']['successful']}")
print(f"Failed: {result['stats']['failed']}")
```

###### `needs_translation(file_path: str) -> bool`

Check if a file needs translation.

**Parameters:**
- `file_path` (str): Path to source file

**Returns:**
- `bool`: True if translation is needed

### QualityAssurance

Provides quality assurance and validation for translated content.

#### Class: `QualityAssurance`

```python
from scripts.quality_assurance import QualityAssurance

qa = QualityAssurance()
```

##### Methods

###### `validate_file(file_path: str) -> ValidationResult`

Validate a single translated file.

**Parameters:**
- `file_path` (str): Path to file to validate

**Returns:**
- `ValidationResult`: Validation results

**Example:**
```python
result = qa.validate_file('README.zh-TW.md')
if result.is_valid:
    print("File passed validation")
else:
    print("Issues found:")
    for issue in result.issues:
        print(f"  - {issue}")
```

###### `validate_directory(directory: str) -> List[ValidationResult]`

Validate all files in a directory.

**Parameters:**
- `directory` (str): Directory to validate

**Returns:**
- `List[ValidationResult]`: List of validation results

###### `generate_report(results: List[ValidationResult], output_file: str)`

Generate a validation report.

**Parameters:**
- `results` (List[ValidationResult]): Validation results
- `output_file` (str): Path to output report file

#### Class: `ValidationResult`

Data class representing validation results.

**Attributes:**
- `file_path` (str): Path to validated file
- `is_valid` (bool): Whether file passed validation
- `issues` (List[str]): List of validation issues
- `score` (float): Quality score (0-1)

### Configuration

Configuration management for the translation system.

#### Class: `TranslationConfig`

```python
from config.translation_config import TranslationConfig

config = TranslationConfig()
```

##### Methods

###### `__init__(config_file: str = None)`

Initialize configuration.

**Parameters:**
- `config_file` (str): Path to configuration file (optional)

###### `get(key: str, default=None) -> any`

Get configuration value.

**Parameters:**
- `key` (str): Configuration key
- `default`: Default value if key not found

**Returns:**
- Configuration value or default

**Example:**
```python
service_type = config.get('translation_service', 'openai')
max_tokens = config.get('max_tokens', 4000)
```

###### `set_value(key: str, value: any)`

Set configuration value.

**Parameters:**
- `key` (str): Configuration key
- `value`: Value to set

###### `save()`

Save configuration to file.

###### `to_dict() -> Dict[str, any]`

Convert configuration to dictionary.

**Returns:**
- `dict`: Configuration as dictionary

## Command Line Interface

### translation-cli.py

Main CLI script for translation operations.

#### Commands

##### translate

Translate a single file.

```bash
python scripts/translation-cli.py translate --file <file> [options]
```

**Options:**
- `--file, -f`: File to translate (required)
- `--output, -o`: Output path (optional)
- `--force`: Overwrite existing files

##### batch

Batch translate multiple files.

```bash
python scripts/translation-cli.py batch --input-dir <dir> --output-dir <dir> [options]
```

**Options:**
- `--input-dir, -i`: Input directory (required)
- `--output-dir, -o`: Output directory (required)
- `--pattern, -p`: File pattern (default: *.md)
- `--recursive, -r`: Process recursively
- `--force`: Overwrite existing files

##### validate

Validate translated files.

```bash
python scripts/translation-cli.py validate [--file <file>] [--dir <dir>] [options]
```

**Options:**
- `--file, -f`: Single file to validate
- `--dir, -d`: Directory to validate
- `--report, -r`: Generate report file

##### migrate

Migrate Chinese documentation to English-first structure.

```bash
python scripts/translation-cli.py migrate --source <dir> --target <dir> [options]
```

**Options:**
- `--source, -s`: Source directory (required)
- `--target, -t`: Target directory (required)
- `--dry-run`: Show what would be done

##### status

Show translation status.

```bash
python scripts/translation-cli.py status [--dir <dir>]
```

**Options:**
- `--dir, -d`: Directory to check (default: current)

##### config

Manage configuration.

```bash
python scripts/translation-cli.py config [--show] [--set <key> <value>]
```

**Options:**
- `--show`: Show current configuration
- `--set`: Set configuration value

## Error Handling

### Exception Classes

#### `TranslationError`

Base exception for translation errors.

```python
from scripts.kiro_translator import TranslationError

try:
    result = translator.translate_text("Hello")
except TranslationError as e:
    print(f"Translation failed: {e}")
```

#### `ConfigurationError`

Exception for configuration-related errors.

```python
from config.translation_config import ConfigurationError

try:
    config = TranslationConfig('invalid-config.json')
except ConfigurationError as e:
    print(f"Configuration error: {e}")
```

### Error Codes

Common error codes returned by the system:

- `TRANSLATION_API_ERROR`: API service error
- `TRANSLATION_TIMEOUT`: Request timeout
- `TRANSLATION_RATE_LIMIT`: Rate limit exceeded
- `FILE_NOT_FOUND`: Input file not found
- `PERMISSION_DENIED`: File permission error
- `INVALID_FORMAT`: Invalid file format
- `VALIDATION_FAILED`: Content validation failed

## Usage Examples

### Basic Translation

```python
from scripts.kiro_translator import KiroTranslator
from config.translation_config import TranslationConfig

# Initialize
config = TranslationConfig()
translator = KiroTranslator(config)

# Translate text
result = translator.translate_text("Hello, World!")
print(result.translated_text)

# Translate file
success = translator.translate_file('README.md', 'README.zh-TW.md')
```

### Batch Processing

```python
from scripts.batch_processor import BatchProcessor
from scripts.kiro_translator import KiroTranslator

# Initialize
translator = KiroTranslator(config)
processor = BatchProcessor(translator, max_workers=4)

# Process directory
result = processor.process_directory(
    'docs/',
    'docs/',
    recursive=True,
    force=False
)

print(f"Processed {result.files_processed} files")
```

### Quality Assurance

```python
from scripts.quality_assurance import QualityAssurance

# Initialize
qa = QualityAssurance()

# Validate files
results = qa.validate_directory('docs/')

# Generate report
qa.generate_report(results, 'validation-report.html')

# Check specific file
result = qa.validate_file('README.zh-TW.md')
if not result.is_valid:
    print("Issues:", result.issues)
```

### Configuration Management

```python
from config.translation_config import TranslationConfig

# Load configuration
config = TranslationConfig('my-config.json')

# Get values
service = config.get('translation_service')
max_tokens = config.get('max_tokens', 4000)

# Set values
config.set_value('max_workers', 8)
config.save()

# Convert to dictionary
config_dict = config.to_dict()
```

## Integration Examples

### Custom Translation Pipeline

```python
from scripts.kiro_translator import KiroTranslator
from scripts.quality_assurance import QualityAssurance
from config.translation_config import TranslationConfig

class CustomTranslationPipeline:
    def __init__(self):
        self.config = TranslationConfig()
        self.translator = KiroTranslator(self.config)
        self.qa = QualityAssurance()
    
    def translate_with_validation(self, input_file, output_file):
        # Translate
        success = self.translator.translate_file(input_file, output_file)
        if not success:
            return False
        
        # Validate
        result = self.qa.validate_file(output_file)
        if not result.is_valid:
            print(f"Validation issues: {result.issues}")
            return False
        
        return True

# Usage
pipeline = CustomTranslationPipeline()
success = pipeline.translate_with_validation('README.md', 'README.zh-TW.md')
```

### File Watcher Integration

```python
import time
from pathlib import Path
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler
from scripts.translate_docs import DocumentationTranslator

class TranslationHandler(FileSystemEventHandler):
    def __init__(self):
        self.translator = DocumentationTranslator()
    
    def on_modified(self, event):
        if event.is_directory or not event.src_path.endswith('.md'):
            return
        
        if '.zh-TW.md' in event.src_path:
            return  # Skip Chinese files
        
        print(f"Translating {event.src_path}")
        self.translator.translate_file(event.src_path, force=True)

# Usage
handler = TranslationHandler()
observer = Observer()
observer.schedule(handler, 'docs/', recursive=True)
observer.start()

try:
    while True:
        time.sleep(1)
except KeyboardInterrupt:
    observer.stop()
observer.join()
```

This API reference provides comprehensive documentation for programmatic use of the translation system. For additional examples and use cases, refer to the user guide and troubleshooting documentation.