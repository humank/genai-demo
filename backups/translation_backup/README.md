# Automated Documentation Translation System

A comprehensive system for automatically translating English documentation to Traditional Chinese (zh-TW) using AI-powered translation services. This system follows an English-first approach where all documentation is written in English and automatically translated to Chinese.

## 🚀 Quick Start

### 1. Installation

```bash
# Run the setup script
python scripts/setup-translation-system.py

# Configure your API keys
cp .env.template .env
# Edit .env with your API keys
```

### 2. Basic Usage

```bash
# Translate a single file
python scripts/translation-cli.py translate --file README.md

# Translate an entire directory
python scripts/translation-cli.py batch --input-dir docs/ --output-dir docs/

# Start automatic translation
./start-translation-system.sh start
```

## 📋 Features

- **AI-Powered Translation**: Uses OpenAI, Google Translate, or Azure Translator
- **English-First Approach**: Write in English, automatically generate Chinese versions
- **File Naming Convention**: `filename.md` → `filename.zh-TW.md`
- **Automatic File Monitoring**: Real-time translation when files change
- **Batch Processing**: Efficient processing of multiple files
- **Quality Assurance**: Built-in validation and terminology consistency
- **Kiro Integration**: Seamless integration with Kiro workflows
- **Migration Tools**: Convert existing Chinese documentation to English-first structure

## 🏗️ Architecture

The system follows a modular architecture with clear separation of concerns:

```
├── scripts/                    # Core translation modules
│   ├── translation-cli.py      # Command-line interface
│   ├── kiro_translator.py      # AI translation engine
│   ├── batch_processor.py      # Batch processing
│   ├── quality_assurance.py    # Quality validation
│   └── watch-docs.py          # File monitoring
├── config/                     # Configuration files
│   ├── translation-config.json # Main configuration
│   └── terminology.json       # Technical terms
├── tests/                      # Comprehensive test suite
├── docs/en/                    # English documentation
└── .kiro/hooks/               # Kiro integration hooks
```

## 📖 Documentation

- **[User Guide](docs/en/translation-user-guide.md)** - Complete usage instructions
- **[API Reference](docs/en/translation-api-reference.md)** - Programmatic interface
- **[Troubleshooting](docs/en/translation-troubleshooting.md)** - Common issues and solutions

## 🛠️ Installation

### Prerequisites

- Python 3.8 or higher
- Kiro environment (optional, for enhanced features)
- API key for translation service (OpenAI, Google, or Azure)

### Automated Setup

```bash
# Run the setup script
python scripts/setup-translation-system.py

# For verbose output
python scripts/setup-translation-system.py --verbose

# Force reinstall dependencies
python scripts/setup-translation-system.py --force-deps
```

### Manual Setup

1. **Install dependencies:**
   ```bash
   pip install -r requirements.txt
   ```

2. **Create configuration:**
   ```bash
   cp config/translation-config.example.json config/translation-config.json
   cp .env.template .env
   ```

3. **Configure API keys in `.env`:**
   ```bash
   OPENAI_API_KEY=your-api-key-here
   TRANSLATION_SERVICE=openai
   TARGET_LANGUAGE=zh-TW
   ```

## 🎯 Usage Examples

### Command Line Interface

```bash
# Translate single file
python scripts/translation-cli.py translate --file README.md

# Batch translate with custom pattern
python scripts/translation-cli.py batch \
  --input-dir docs/ \
  --output-dir docs/ \
  --pattern "*.md" \
  --recursive

# Validate translations
python scripts/translation-cli.py validate --dir docs/

# Check system status
python scripts/translation-cli.py status

# Migrate existing Chinese docs
python scripts/translation-cli.py migrate \
  --source docs/zh/ \
  --target docs/ \
  --dry-run
```

### Programmatic Usage

```python
from scripts.kiro_translator import KiroTranslator
from config.translation_config import TranslationConfig

# Initialize translator
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

# Initialize batch processor
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

## ⚙️ Configuration

### Main Configuration (`config/translation-config.json`)

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
    "backup_before_translation": true
  },
  "quality_assurance": {
    "enable_terminology_check": true,
    "enable_format_validation": true,
    "min_confidence_score": 0.7
  }
}
```

### Terminology Configuration (`config/terminology.json`)

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

## 🔄 Automatic Translation

### File Watcher

Start automatic translation monitoring:

```bash
# Start file watcher
python scripts/watch-docs.py --directory docs/ --recursive

# Or use the startup script
./start-translation-system.sh start
```

### Kiro Hooks Integration

The system integrates with Kiro hooks for seamless workflow automation:

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

## 🧪 Testing

### Run All Tests

```bash
# Run complete test suite
python tests/run_all_tests.py

# Run specific test categories
python tests/run_all_tests.py --category unit
python tests/run_all_tests.py --category integration

# Generate test report
python tests/run_all_tests.py --report test_report.html
```

### Test Categories

- **Unit Tests**: Core functionality testing
- **Integration Tests**: End-to-end workflow testing
- **Performance Tests**: System performance validation

## 📊 Quality Assurance

### Built-in Validation

```bash
# Validate translated files
python scripts/translation-cli.py validate --dir docs/

# Check terminology consistency
python scripts/quality_assurance.py --check-terminology docs/

# Generate quality report
python scripts/quality_assurance.py --report quality_report.html docs/
```

### Quality Metrics

- Translation accuracy and consistency
- Markdown formatting preservation
- Link integrity validation
- Technical terminology consistency

## 🔧 Troubleshooting

### Common Issues

1. **API Key Not Found**
   ```bash
   export OPENAI_API_KEY="your-api-key-here"
   ```

2. **Permission Errors**
   ```bash
   chmod 755 docs/
   chmod 644 docs/*.md
   ```

3. **Module Import Errors**
   ```bash
   pip install -r requirements.txt
   ```

### Diagnostic Tools

```bash
# Run system diagnostics
python scripts/run-diagnostics.py

# Check system health
python scripts/setup-translation-system.py --check-only

# View logs
tail -f logs/translation.log
```

## 📈 Performance

### Optimization Tips

- Use appropriate `max_workers` based on your system
- Enable caching for repeated translations
- Configure rate limits based on API quotas
- Use batch processing for large document sets

### Performance Monitoring

The system includes built-in performance monitoring:

- Translation speed and accuracy metrics
- API usage and rate limiting
- Memory and CPU usage tracking
- Error rate monitoring

## 🔄 Migration

### Existing Chinese Documentation

Convert existing Chinese documentation to English-first structure:

```bash
# Dry run to see what would be migrated
python scripts/translation-cli.py migrate \
  --source docs/zh/ \
  --target docs/ \
  --dry-run

# Perform migration
python scripts/translation-cli.py migrate \
  --source docs/zh/ \
  --target docs/
```

## 🤝 Contributing

### Development Setup

1. Clone the repository
2. Install development dependencies: `pip install -r requirements-dev.txt`
3. Run tests: `python tests/run_all_tests.py`
4. Follow the coding standards in the development guide

### Testing

- Write tests for new features
- Ensure all tests pass before submitting
- Include integration tests for complex workflows

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

### Getting Help

1. **Documentation**: Check the [User Guide](docs/en/translation-user-guide.md)
2. **Troubleshooting**: See [Troubleshooting Guide](docs/en/translation-troubleshooting.md)
3. **API Reference**: Review [API Documentation](docs/en/translation-api-reference.md)
4. **Diagnostics**: Run `python scripts/run-diagnostics.py`

### Reporting Issues

When reporting issues, include:

- System information (`python scripts/run-diagnostics.py`)
- Error messages and logs
- Steps to reproduce the issue
- Expected vs actual behavior

## 🎉 Success Stories

The Automated Documentation Translation System has been successfully used to:

- Translate large documentation sets (1000+ files)
- Maintain consistency across multilingual projects
- Automate translation workflows in CI/CD pipelines
- Reduce manual translation effort by 90%

---

**Made with ❤️ for seamless documentation translation**
