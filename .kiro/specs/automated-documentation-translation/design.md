# Design Document

## Overview

A simplified automated documentation translation system that maintains English as the primary documentation language with automatic Traditional Chinese translation generation. The system uses a straightforward approach with minimal complexity while ensuring reliable bilingual documentation.

## Architecture

### Simple System Flow

```mermaid
graph LR
    A[Markdown File Change] --> B[Translation Script]
    B --> C[Translation API]
    C --> D[Generate .zh-TW.md]
    D --> E[Update Links]
```

### Core Components

The system consists of three main components:

1. **File Watcher**: Detects changes to `.md` files
2. **Translation Engine**: Translates content using external API
3. **File Manager**: Creates and maintains `.zh-TW.md` files

## Components and Interfaces

### 1. Translation Script (`translate-docs.py`)

**Purpose**: Main script that handles all translation operations.

**Key Features**:
- Scans for `.md` files that need translation
- Calls translation API (Google Translate or similar)
- Generates corresponding `.zh-TW.md` files
- Simple error handling and logging

**Interface**:
```python
def translate_file(source_path: str) -> str:
    """Translate a single markdown file and create .zh-TW.md version"""
    pass

def scan_and_translate(directory: str) -> None:
    """Scan directory for .md files and translate them"""
    pass

def migrate_existing_chinese_files(directory: str) -> None:
    """Convert existing Chinese files to English-first system"""
    pass
```

### 2. File Watcher (`watch-docs.py`)

**Purpose**: Monitors file changes and triggers translations.

**Key Features**:
- Uses Python `watchdog` library for file monitoring
- Triggers translation when `.md` files are modified
- Ignores `.zh-TW.md` files to prevent loops

**Interface**:
```python
def start_watching(paths: List[str]) -> None:
    """Start monitoring specified paths for changes"""
    pass

def on_file_changed(file_path: str) -> None:
    """Handle file change events"""
    pass
```

### 3. Kiro Translation Integration (`kiro_translator.py`)

**Purpose**: Uses Kiro's built-in translation capabilities through MCP tools.

**Key Features**:
- Direct integration with Kiro's AI translation capabilities
- No external API dependencies or costs
- Preserves markdown formatting automatically
- Context-aware technical translation

**Interface**:
```python
def translate_with_kiro(text: str, target_language: str = 'zh-TW') -> str:
    """Use Kiro's AI to translate text directly"""
    pass

def translate_markdown_file(file_path: str) -> str:
    """Translate entire markdown file preserving structure"""
    pass
```

## Data Models

### Simple Configuration

```python
# config.py
TRANSLATION_CONFIG = {
    'api_key': 'GOOGLE_TRANSLATE_API_KEY',
    'target_language': 'zh-TW',
    'source_language': 'en',
    'file_patterns': ['**/*.md'],
    'exclude_patterns': ['**/*.zh-TW.md', 'node_modules/**'],
    'terminology': {
        'DDD': 'DDD',  # Keep technical terms unchanged
        'API': 'API',
        'README': 'README'
    }
}
```

### File Tracking

```python
# Simple JSON file to track translations
{
    "translations": {
        "README.md": {
            "chinese_file": "README.zh-TW.md",
            "last_updated": "2025-09-25T14:30:00Z",
            "status": "completed"
        }
    }
}
```

## Error Handling

### Simple Error Strategy

1. **Translation API Errors**: Log error and skip file, continue with others
2. **File System Errors**: Log error and continue
3. **Network Issues**: Retry once after 5 seconds, then skip

```python
def safe_translate_file(file_path: str) -> bool:
    """Safely translate a file with basic error handling"""
    try:
        translate_file(file_path)
        return True
    except Exception as e:
        logging.error(f"Failed to translate {file_path}: {e}")
        return False
```

## Testing Strategy

### Simple Testing Approach

1. **Manual Testing**: Test with a few sample files first
2. **Dry Run Mode**: Add `--dry-run` flag to preview changes without making them
3. **Backup**: Always backup files before translation

```python
def test_translation():
    """Simple test function"""
    test_files = ['test/sample.md']
    for file in test_files:
        result = translate_file(file)
        assert result.endswith('.zh-TW.md')
        assert os.path.exists(result)
```

## Configuration

### Simple Configuration File

```python
# config.py

# Translation settings
TARGET_LANGUAGE = 'zh-TW'
SOURCE_LANGUAGE = 'en'

# File patterns
INCLUDE_PATTERNS = ['**/*.md']
EXCLUDE_PATTERNS = ['**/*.zh-TW.md', 'node_modules/**', '.git/**']

# Basic terminology that should not be translated
PRESERVE_TERMS = ['API', 'DDD', 'README', 'GitHub', 'Docker', 'Kubernetes']

# Kiro integration settings
KIRO_TRANSLATION_PROMPT = """
Please translate the following markdown content from English to Traditional Chinese (zh-TW).
Preserve all markdown formatting, code blocks, and links exactly as they are.
Keep technical terms like API, DDD, GitHub, Docker unchanged.
Maintain professional technical documentation tone.

Content to translate:
{content}
"""

# Logging
LOG_LEVEL = 'INFO'
LOG_FILE = 'translation.log'
```

## Integration Points

### Kiro Hook Integration

Simple hook to trigger translation:

```json
{
  "name": "Auto Translation Hook",
  "description": "Translate documentation when .md files change",
  "when": {
    "patterns": ["**/*.md"],
    "exclude_patterns": ["**/*.zh-TW.md"]
  },
  "then": {
    "prompt": "Running translation script...",
    "action": "python scripts/translate-docs.py"
  }
}
```

### Manual Commands

```bash
# Translate all documentation
python scripts/translate-docs.py --all

# Translate specific file
python scripts/translate-docs.py --file README.md

# Start file watcher
python scripts/watch-docs.py

# Migrate existing Chinese files
python scripts/migrate-chinese-docs.py
```

## Implementation Plan

### Phase 1: Kiro-Powered Translation Script
1. Create `scripts/translate-docs.py` that uses Kiro's AI translation capabilities
2. Implement markdown file scanning and direct AI translation
3. Generate `.zh-TW.md` files with proper naming

### Phase 2: File Watching
1. Create `scripts/watch-docs.py` for automatic translation on file changes
2. Add Kiro Hook integration for seamless workflow

### Phase 3: Migration Tool
1. Create `scripts/migrate-chinese-docs.py` to handle existing Chinese files
2. Convert existing Chinese documentation to English-first system using Kiro's translation

### Phase 4: Quality Improvements
1. Add context-aware translation prompts for different document types
2. Improve markdown formatting preservation
3. Add simple logging and error reporting

**Key Advantage**: No external API costs, no rate limits, and leverages Kiro's understanding of the project context for better technical translations.

This simplified design focuses on the core functionality while maintaining ease of implementation and maintenance.