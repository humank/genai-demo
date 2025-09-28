# Translation System Troubleshooting Guide (繁體中文版)

> **注意**: 此文件需要翻譯。原始英文版本請參考對應的英文文件。

# Translation System Troubleshooting Guide

## Overview

This guide provides solutions to common issues encountered when using the Automated Documentation Translation System. Issues are organized by category with step-by-step troubleshooting instructions.

## Table of Contents

- Installation Issues
- Translation API Errors
- File System Issues
- Performance Problems
- Quality Issues
- Configuration Problems
- Integration Issues
- Debugging Tools

## Installation Issues

### Python Dependencies

#### Problem: Module Import Errors

```
ImportError: No module named 'openai'
ModuleNotFoundError: No module named 'watchdog'
```

**Solution:**
```bash
# Install required dependencies
pip install -r requirements.txt

# Or install individual packages
pip install openai watchdog pyyaml requests
```

#### Problem: Python Version Compatibility

```
SyntaxError: invalid syntax (match statement requires Python 3.10+)
```

**Solution:**
```bash
# Check Python version
python --version

# Upgrade Python to 3.8 or higher
# On Ubuntu/Debian:
sudo apt update
sudo apt install python3.8

# On macOS with Homebrew:
brew install python@3.8

# On Windows: Download from python.org
```

### Environment Setup

#### Problem: Kiro Environment Not Found

```
Error: Kiro environment not detected
```

**Solution:**
1. Ensure you're running within a Kiro workspace
2. Check that Kiro is properly installed and configured
3. Verify environment variables are set correctly

```bash
# Check Kiro installation
kiro --version

# Verify workspace
ls -la .kiro/
```

## Translation API Errors

### OpenAI API Issues

#### Problem: API Key Not Found

```
Error: OpenAI API key not configured
```

**Solution:**
```bash
# Set environment variable
export OPENAI_API_KEY="your-api-key-here"

# Or add to configuration file
{
  "translation_service": {
    "api_key": "your-api-key-here"
  }
}
```

#### Problem: API Rate Limit Exceeded

```
Error: Rate limit exceeded. Please try again later.
```

**Solution:**
1. **Reduce request rate:**
   ```json
   {
     "translation_service": {
       "rate_limit_per_minute": 30
     },
     "processing": {
       "max_workers": 2
     }
   }
   ```

2. **Add retry logic:**
   ```bash
   # Use CLI with built-in retry
   python scripts/translation-cli.py translate --file README.md --retry 3
   ```

3. **Upgrade API plan** if consistently hitting limits

#### Problem: API Timeout

```
Error: Request timeout after 30 seconds
```

**Solution:**
1. **Increase timeout:**
   ```json
   {
     "translation_service": {
       "timeout_seconds": 60
     }
   }
   ```

2. **Reduce chunk size:**
   ```json
   {
     "processing": {
       "max_chunk_size": 2000
     }
   }
   ```

3. **Check network connectivity:**
   ```bash
   curl -I https://api.openai.com/v1/models
   ```

### Google Translate API Issues

#### Problem: Authentication Failed

```
Error: Google Cloud authentication failed
```

**Solution:**
```bash
# Set up Google Cloud credentials
export GOOGLE_APPLICATION_CREDENTIALS="path/to/service-account-key.json"

# Or use gcloud auth
gcloud auth application-default login
```

#### Problem: Quota Exceeded

```
Error: Quota exceeded for Google Translate API
```

**Solution:**
1. Check quota usage in Google Cloud Console
2. Increase quota limits or upgrade plan
3. Implement request throttling:
   ```json
   {
     "translation_service": {
       "service_type": "google",
       "rate_limit_per_minute": 100
     }
   }
   ```

## File System Issues

### Permission Errors

#### Problem: Cannot Create Translation Files

```
PermissionError: [Errno 13] Permission denied: 'README.zh-TW.md'
```

**Solution:**
```bash
# Check file permissions
ls -la README.md

# Fix directory permissions
chmod 755 docs/
chmod 644 docs/*.md

# Check disk space
df -h

# Ensure write permissions for user
sudo chown -R $USER:$USER docs/
```

### File Encoding Issues

#### Problem: Unicode Decode Error

```
UnicodeDecodeError: 'utf-8' codec can't decode byte 0xff in position 0
```

**Solution:**
1. **Convert file to UTF-8:**
   ```bash
   # Check file encoding
   file -bi README.md
   
   # Convert to UTF-8
   iconv -f ISO-8859-1 -t UTF-8 README.md > README_utf8.md
   mv README_utf8.md README.md
   ```

2. **Handle encoding in code:**
   ```python
   # Force UTF-8 encoding
   with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
       content = f.read()
   ```

### File Path Issues

#### Problem: File Not Found

```
FileNotFoundError: [Errno 2] No such file or directory: 'docs/guide.md'
```

**Solution:**
```bash
# Check file exists
ls -la docs/guide.md

# Check current directory
pwd

# Use absolute paths
python scripts/translation-cli.py translate --file /full/path/to/docs/guide.md

# Check for hidden characters in filename
ls -la docs/ | cat -A
```

## Performance Problems

### Slow Translation Speed

#### Problem: Translation Takes Too Long

**Symptoms:**
- Single file translation takes > 30 seconds
- Batch processing is extremely slow

**Solution:**
1. **Optimize chunk size:**
   ```json
   {
     "processing": {
       "max_chunk_size": 2500,
       "max_workers": 4
     }
   }
   ```

2. **Enable caching:**
   ```json
   {
     "processing": {
       "enable_caching": true
     }
   }
   ```

3. **Use parallel processing:**
   ```bash
   python scripts/translation-cli.py batch --input-dir docs/ --output-dir docs/ --parallel
   ```

4. **Monitor system resources:**
   ```bash
   # Check CPU and memory usage
   top
   htop
   
   # Check network latency
   ping api.openai.com
   ```

### Memory Issues

#### Problem: Out of Memory Errors

```
MemoryError: Unable to allocate array
```

**Solution:**
1. **Reduce batch size:**
   ```json
   {
     "processing": {
       "max_workers": 2,
       "max_chunk_size": 1500
     }
   }
   ```

2. **Process files sequentially:**
   ```bash
   python scripts/translation-cli.py batch --input-dir docs/ --output-dir docs/ --no-parallel
   ```

3. **Monitor memory usage:**
   ```bash
   # Check memory usage
   free -h
   
   # Monitor process memory
   ps aux | grep python
   ```

## Quality Issues

### Translation Quality Problems

#### Problem: Poor Translation Quality

**Symptoms:**
- Technical terms incorrectly translated
- Context lost in translation
- Formatting broken

**Solution:**
1. **Configure terminology preservation:**
   ```json
   {
     "quality_assurance": {
       "terminology_file": "config/terminology.json"
     }
   }
   ```

2. **Create terminology file:**
   ```json
   {
     "preserve_terms": [
       "API", "REST", "JSON", "HTTP",
       "Docker", "Kubernetes", "Git"
     ],
     "translation_overrides": {
       "endpoint": "端點",
       "authentication": "身份驗證"
     }
   }
   ```

3. **Adjust translation parameters:**
   ```json
   {
     "translation_service": {
       "temperature": 0.1,
       "model": "gpt-4"
     }
   }
   ```

#### Problem: Inconsistent Terminology

**Solution:**
1. **Run terminology check:**
   ```bash
   python scripts/translation-cli.py validate --dir docs/ --check-terminology
   ```

2. **Generate consistency report:**
   ```python
   from scripts.quality_assurance import QualityAssurance
   
   qa = QualityAssurance()
   report = qa.check_terminology_consistency('docs/')
   print(report['inconsistencies'])
   ```

### Formatting Issues

#### Problem: Markdown Formatting Broken

**Symptoms:**
- Code blocks not preserved
- Links broken
- Tables malformed

**Solution:**
1. **Enable format preservation:**
   ```json
   {
     "processing": {
       "preserve_formatting": true
     }
   }
   ```

2. **Validate after translation:**
   ```bash
   python scripts/translation-cli.py validate --file README.zh-TW.md --check-format
   ```

3. **Manual format fixes:**
   ```bash
   # Check markdown syntax
   markdownlint README.zh-TW.md
   
   # Fix common issues
   sed -i 's/\*\* /\*\*/g' README.zh-TW.md  # Fix bold formatting
   sed -i 's/ \*\*/\*\*/g' README.zh-TW.md
   ```

## Configuration Problems

### Configuration File Issues

#### Problem: Configuration Not Loaded

```
Warning: Using default configuration
```

**Solution:**
1. **Check configuration file location:**
   ```bash
   # Expected locations
   ls -la config/translation-config.json
   ls -la .kiro/config/translation.json
   ls -la ~/.kiro/translation-config.json
   ```

2. **Validate JSON syntax:**
   ```bash
   # Check JSON validity
   python -m json.tool config/translation-config.json
   
   # Or use jq
   jq . config/translation-config.json
   ```

3. **Specify configuration explicitly:**
   ```bash
   python scripts/translation-cli.py --config config/my-config.json translate --file README.md
   ```

#### Problem: Invalid Configuration Values

```
ConfigurationError: Invalid value for 'max_workers': must be positive integer
```

**Solution:**
1. **Validate configuration:**
   ```bash
   python scripts/translation-cli.py config --validate
   ```

2. **Check value types:**
   ```json
   {
     "processing": {
       "max_workers": 4,          // integer, not string
       "enable_caching": true,    // boolean, not string
       "max_chunk_size": 3000     // integer
     }
   }
   ```

## Integration Issues

### Kiro Hooks Integration

#### Problem: Hook Not Triggering

**Solution:**
1. **Check hook configuration:**
   ```bash
   ls -la .kiro/hooks/
   cat .kiro/hooks/auto-translation.kiro.hook
   ```

2. **Verify hook syntax:**
   ```json
   {
     "name": "Auto Translation",
     "when": {
       "patterns": ["**/*.md"],
       "exclude_patterns": ["**/*.zh-TW.md"]
     },
     "then": {
       "command": "python scripts/translate-docs.py --file {file}"
     }
   }
   ```

3. **Test hook manually:**
   ```bash
   python scripts/test-hook-functionality.py
   ```

### File Watcher Issues

#### Problem: File Watcher Not Detecting Changes

**Solution:**
1. **Check file watcher process:**
   ```bash
   ps aux | grep watch-docs
   ```

2. **Increase inotify limits (Linux):**
   ```bash
   # Check current limits
   cat /proc/sys/fs/inotify/max_user_watches
   
   # Increase limits
   echo fs.inotify.max_user_watches=524288 | sudo tee -a /etc/sysctl.conf
   sudo sysctl -p
   ```

3. **Use polling mode:**
   ```bash
   python scripts/watch-docs.py --directory docs/ --polling
   ```

## Debugging Tools

### Enable Debug Logging

```bash
# Set debug level
export LOG_LEVEL=DEBUG

# Run with verbose output
python scripts/translation-cli.py translate --file README.md --verbose

# Check log files
tail -f logs/translation.log
```

### Diagnostic Commands

```bash
# Check system status
python scripts/translation-cli.py status --dir docs/

# Validate configuration
python scripts/translation-cli.py config --show --validate

# Test translation API
python scripts/test-translation-api.py

# Run system diagnostics
python scripts/run-diagnostics.py
```

### Performance Profiling

```python
import cProfile
import pstats
from scripts.translate_docs import DocumentationTranslator

# Profile translation
profiler = cProfile.Profile()
profiler.enable()

translator = DocumentationTranslator()
translator.translate_file('README.md')

profiler.disable()
stats = pstats.Stats(profiler)
stats.sort_stats('cumulative')
stats.print_stats(10)
```

### Memory Profiling

```python
from memory_profiler import profile
from scripts.kiro_translator import KiroTranslator

@profile
def translate_large_file():
    translator = KiroTranslator()
    translator.translate_file('large-document.md', 'large-document.zh-TW.md')

translate_large_file()
```

## Common Error Messages

### Error: "Translation service not available"

**Cause:** API service is down or unreachable

**Solution:**
1. Check service status
2. Verify network connectivity
3. Try alternative translation service
4. Wait and retry later

### Error: "File already exists"

**Cause:** Target translation file already exists

**Solution:**
```bash
# Force overwrite
python scripts/translation-cli.py translate --file README.md --force

# Or remove existing file
rm README.zh-TW.md
```

### Error: "Invalid markdown format"

**Cause:** Source file has malformed markdown

**Solution:**
1. Validate source markdown:
   ```bash
   markdownlint README.md
   ```

2. Fix formatting issues
3. Use format-tolerant mode:
   ```json
   {
     "processing": {
       "strict_format_validation": false
     }
   }
   ```

## Getting Additional Help

### Log Analysis

```bash
# Search for specific errors
grep -i "error" logs/translation.log

# Check recent activity
tail -n 100 logs/translation.log

# Monitor in real-time
tail -f logs/translation.log
```

### System Information

```bash
# Collect system info for support
python scripts/collect-system-info.py > system-info.txt
```

### Support Resources

1. **Check documentation:** Review user guide and API reference
2. **Search logs:** Look for specific error messages
3. **Test with minimal example:** Isolate the issue
4. **Check system resources:** Ensure adequate memory and disk space
5. **Verify configuration:** Validate all settings

### Reporting Issues

When reporting issues, include:

1. **Error message:** Full error text and stack trace
2. **System information:** OS, Python version, dependencies
3. **Configuration:** Relevant configuration settings
4. **Steps to reproduce:** Minimal example that reproduces the issue
5. **Log files:** Relevant log entries
6. **Expected vs actual behavior:** What should happen vs what happens

This troubleshooting guide covers the most common issues encountered with the translation system. For additional help with specific problems, refer to the system logs and diagnostic tools.

---
*此文件由自動翻譯系統生成，可能需要人工校對。*
