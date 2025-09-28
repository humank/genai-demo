# Automated Documentation Translation System - User Guide

## Overview

The Automated Documentation Translation System provides seamless translation of English documentation to Traditional Chinese (zh-TW) using Kiro's built-in AI capabilities. This system follows an English-first approach where all documentation is written in English and automatically translated to Chinese.

## Table of Contents

- Quick Start
- Installation
- Basic Usage
- Advanced Features
- Configuration
- Troubleshooting
- Best Practices

## Quick Start

### 1. Translate a Single File

```bash
python scripts/translation-cli.py translate --file README.md
```

This will create `README.zh-TW.md` with the Chinese translation.

### 2. Translate an Entire Directory

```bash
python scripts/translation-cli.py batch --input-dir docs/ --output-dir docs/
```

This will translate all markdown files in the `docs/` directory.

### 3. Enable Automatic Translation

```bash
python scripts/watch-docs.py --directory docs/
```

This will monitor the `docs/` directory and automatically translate files when they change.

## Installation

### Prerequisites

- Python 3.8 or higher
- Kiro environment with AI translation capabilities
- Write permissions for creating `.zh-TW.md` files

### Setup Steps

1. **Clone or navigate to your project directory**

2. **Install Python dependencies**
   ```bash
   pip install -r requirements.txt
   ```

3. **Configure the translation system**
   ```bash
   cp config/translation-config.example.json config/translation-config.json
   # Edit the configuration file as needed
   ```

4. **Test the installation**
   ```bash
   python tests/run_all_tests.py --category unit
   ```

## Basic Usage

### Command Line Interface

The system provides a comprehensive CLI through `scripts/translation-cli.py`:

#### Translate Command

Translate a single file:

```bash
python scripts/translation-cli.py translate --file <input-file> [--output <output-path>] [--force]
```

**Options:**
- `--file, -f`: Path to the file to translate (required)
- `--output, -o`: Output directory or file path (optional)
- `--force`: Overwrite existing files (optional)

**Examples:**
```bash
# Translate README.md to README.zh-TW.md in the same directory
python scripts/translation-cli.py translate --file README.md

# Translate to a specific output directory
python scripts/translation-cli.py translate --file docs/guide.md --output translated/

# Force overwrite existing translation
python scripts/translation-cli.py translate --file README.md --force
```

#### Batch Command

Translate multiple files:

```bash
python scripts/translation-cli.py batch --input-dir <input-directory> --output-dir <output-directory> [options]
```

**Options:**
- `--input-dir, -i`: Input directory containing files to translate (required)
- `--output-dir, -o`: Output directory for translated files (required)
- `--pattern, -p`: File pattern to match (default: `*.md`)
- `--recursive, -r`: Process directories recursively
- `--force`: Overwrite existing files

**Examples:**
```bash
# Translate all markdown files in docs/ directory
python scripts/translation-cli.py batch --input-dir docs/ --output-dir docs/

# Translate recursively with custom pattern
python scripts/translation-cli.py batch -i src/ -o src/ -p "*.md" --recursive

# Force overwrite all existing translations
python scripts/translation-cli.py batch -i docs/ -o docs/ --force
```

#### Validate Command

Validate translated files:

```bash
python scripts/translation-cli.py validate [--file <file>] [--dir <directory>] [--report <report-file>]
```

**Examples:**
```bash
# Validate a single file
python scripts/translation-cli.py validate --file README.zh-TW.md

# Validate all files in a directory
python scripts/translation-cli.py validate --dir docs/

# Generate validation report
python scripts/translation-cli.py validate --dir docs/ --report validation-report.json
```

#### Status Command

Check translation status:

```bash
python scripts/translation-cli.py status [--dir <directory>]
```

**Example:**
```bash
# Check status for current directory
python scripts/translation-cli.py status

# Check status for specific directory
python scripts/translation-cli.py status --dir docs/
```

### File Watcher

Enable automatic translation when files change:

```bash
python scripts/watch-docs.py [options]
```

**Options:**
- `--directory, -d`: Directory to watch (default: current directory)
- `--pattern, -p`: File pattern to watch (default: `*.md`)
- `--recursive, -r`: Watch subdirectories recursively
- `--debounce`: Debounce time in seconds (default: 2)

**Examples:**
```bash
# Watch current directory
python scripts/watch-docs.py

# Watch specific directory recursively
python scripts/watch-docs.py --directory docs/ --recursive

# Watch with custom pattern and debounce
python scripts/watch-docs.py --pattern "*.markdown" --debounce 5
```

## Advanced Features

### Migration Tool

Migrate existing Chinese documentation to English-first structure:

```bash
python scripts/translation-cli.py migrate --source <chinese-docs-dir> --target <english-docs-dir> [--dry-run]
```

**Examples:**
```bash
# Dry run to see what would be migrated
python scripts/translation-cli.py migrate --source docs/zh/ --target docs/ --dry-run

# Perform actual migration
python scripts/translation-cli.py migrate --source docs/zh/ --target docs/
```

### Batch Processing

For large document sets, use the batch processor:

```python
from scripts.batch_processor import BatchProcessor
from scripts.kiro_translator import KiroTranslator
from config.translation_config import TranslationConfig

# Initialize components
config = TranslationConfig()
translator = KiroTranslator(config)
processor = BatchProcessor(translator, max_workers=4)

# Create and process batch job
files = ['doc1.md', 'doc2.md', 'doc3.md']
job_id = processor.create_translation_job(files)
results = processor.process_job(job_id, show_progress=True)
```

### Quality Assurance

Run quality checks on translated files:

```python
from scripts.quality_assurance import QualityAssurance

qa = QualityAssurance()

# Validate single file
result = qa.validate_file('README.zh-TW.md')
if not result.is_valid:
    print("Issues found:", result.issues)

# Validate directory
results = qa.validate_directory('docs/')
qa.generate_report(results, 'qa-report.html')
```

## Configuration

### Configuration File

Create `config/translation-config.json`:

```json
{
  "translation_service": {
    "service_type": "openai",
    "model": "gpt-3.5-turbo",
    "max_tokens": 4000,
    "temperature": 0.3,
    "rate_limit_per_minute": 60
  },
  "processing": {
    "max_chunk_size": 3000,
    "max_workers": 4,
    "enable_caching": true,
    "backup_before_translation": true,
    "preserve_formatting": true,
    "validate_after_translation": true
  },
  "quality_assurance": {
    "enable_terminology_check": true,
    "enable_format_validation": true,
    "enable_cross_reference_check": true,
    "min_confidence_score": 0.7,
    "terminology_file": "config/terminology.json"
  },
  "file_management": {
    "backup_directory": "backups",
    "log_directory": "logs",
    "preserve_directory_structure": true,
    "file_patterns": ["*.md", "*.txt", "*.rst"]
  }
}
```

### Environment Variables

Set environment variables for sensitive configuration:

```bash
export OPENAI_API_KEY="your-api-key-here"
export TRANSLATION_SERVICE="openai"
export TARGET_LANGUAGE="zh-TW"
```

### Terminology Configuration

Create `config/terminology.json` to preserve technical terms:

```json
{
  "preserve_terms": [
    "API", "REST", "JSON", "HTTP", "HTTPS",
    "Docker", "Kubernetes", "Git", "GitHub",
    "DDD", "TDD", "BDD", "SOLID",
    "JavaScript", "TypeScript", "Python", "Java"
  ],
  "translation_overrides": {
    "API": "API",
    "endpoint": "端點",
    "authentication": "身份驗證",
    "authorization": "授權"
  }
}
```

## Troubleshooting

### Common Issues

#### 1. Translation API Errors

**Problem:** API timeout or rate limit errors

**Solution:**
```bash
# Check API configuration
python scripts/translation-cli.py config --show

# Adjust rate limits in config
# Reduce max_workers in batch processing
```

#### 2. File Permission Errors

**Problem:** Cannot create Chinese translation files

**Solution:**
```bash
# Check directory permissions
ls -la docs/

# Fix permissions
chmod 755 docs/
chmod 644 docs/*.md
```

#### 3. Encoding Issues

**Problem:** Special characters not displaying correctly

**Solution:**
- Ensure all files are saved in UTF-8 encoding
- Check terminal/editor encoding settings
- Verify system locale supports Chinese characters

#### 4. Memory Issues with Large Files

**Problem:** Out of memory errors with large documents

**Solution:**
```json
{
  "processing": {
    "max_chunk_size": 2000,
    "max_workers": 2
  }
}
```

### Debug Mode

Enable debug logging:

```bash
export LOG_LEVEL=DEBUG
python scripts/translation-cli.py translate --file README.md
```

### Log Files

Check log files for detailed error information:

```bash
# View translation logs
tail -f logs/translation.log

# View error logs
grep ERROR logs/translation.log
```

## Best Practices

### 1. File Organization

- Keep English files as the source of truth
- Use consistent naming: `filename.md` → `filename.zh-TW.md`
- Organize files in logical directory structures
- Use meaningful file and directory names

### 2. Content Guidelines

- Write clear, concise English documentation
- Use standard markdown formatting
- Include code examples with proper syntax highlighting
- Use consistent terminology throughout documents

### 3. Translation Quality

- Review translated files for accuracy
- Use terminology configuration for technical terms
- Run quality assurance checks regularly
- Keep translations up to date with source changes

### 4. Workflow Integration

- Set up automatic translation for active documentation
- Use batch processing for large document sets
- Integrate with version control systems
- Create backup strategies for important documents

### 5. Performance Optimization

- Use appropriate chunk sizes for large documents
- Configure rate limits based on API quotas
- Enable caching for frequently translated content
- Monitor system resources during batch operations

### 6. Maintenance

- Regularly update terminology configurations
- Monitor translation quality and accuracy
- Keep the system updated with latest improvements
- Backup configuration and translation memories

## Support and Resources

### Getting Help

1. **Check the logs**: Most issues are logged with detailed error messages
2. **Run diagnostics**: Use the status and validate commands
3. **Review configuration**: Ensure all settings are correct
4. **Test with simple files**: Isolate issues with minimal examples

### Additional Documentation

- [API Reference](translation-api-reference.md)
- [Troubleshooting Guide](translation-troubleshooting.md)
- Configuration Reference
- Developer Guide

### System Requirements

- **Python**: 3.8 or higher
- **Memory**: 2GB RAM minimum, 4GB recommended
- **Storage**: 1GB free space for logs and backups
- **Network**: Internet connection for translation API calls

This user guide provides comprehensive information for using the automated documentation translation system effectively. For additional help or advanced use cases, refer to the API reference and troubleshooting documentation.
