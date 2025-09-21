
# Documentation Quality Check Scripts

This directory contains comprehensive documentation quality assurance tools for the Viewpoints & Perspectives documentation structure.

## Overview

The documentation quality check system implements **Requirement 6: 文件維護的自動化** from the documentation restructure specification. It provides automated validation for:

- ✅ **Markdown syntax checking** using markdownlint
- ✅ **Link validity verification** for internal and external links  
- ✅ **Diagram rendering validation** for Mermaid, PlantUML, and Excalidraw
- ✅ **Translation synchronization** between Chinese and English versions
- ✅ **Document metadata validation** with YAML front matter checking
- ✅ **Structure consistency** for Viewpoints & Perspectives organization

## Scripts

### Main Quality Check Script

**`check-documentation-quality.sh`** - Comprehensive quality check runner
```bash
# Run all quality checks
bash scripts/check-documentation-quality.sh

# Or use npm script
npm run docs:quality
```

**Features:**
- Runs all quality checks in sequence
- Generates comprehensive reports in `build/reports/documentation-quality/`
- Provides colored output with pass/fail status
- Creates summary report with recommendations

### Individual Quality Check Tools

#### 1. Advanced Link Checker
**`check-links-advanced.js`** - Node.js-based link validation
```bash
# Check internal links only (fast)
node scripts/check-links-advanced.js

# Check internal and external links (slower)
node scripts/check-links-advanced.js --external

# Verbose output with progress
node scripts/check-links-advanced.js --verbose

# Custom output file
node scripts/check-links-advanced.js --output reports/links.json

# NPM scripts
npm run docs:links          # Internal links only
npm run docs:links:external # Include external links
```

**Features:**
- Validates markdown `\1` and HTML `<a href="">` links
- Supports both internal file links and external HTTP/HTTPS links
- Generates JSON and Markdown reports
- Handles relative paths and anchor links
- Configurable timeout and user agent
- Excludes localhost and example domains

#### 2. Diagram Validator
**`validate-diagrams.py`** - Python-based diagram syntax validation
```bash
# Validate all diagrams
python3 scripts/validate-diagrams.py

# Verbose output
python3 scripts/validate-diagrams.py --verbose

# Custom output directory
python3 scripts/validate-diagrams.py --output reports/

# Validate specific directory
python3 scripts/validate-diagrams.py docs/diagrams/

# NPM script
npm run docs:diagrams
```

**Supported Formats:**
- **Mermaid** (`.mmd`): Validates diagram types, syntax structure
- **PlantUML** (`.puml`, `.plantuml`): Checks `@startuml`/`@enduml` tags, balance
- **Excalidraw** (`.excalidraw`): Validates JSON structure, required fields

**Features:**
- Detects diagram types automatically
- Validates syntax structure and common errors
- Generates detailed reports with error locations
- Supports metadata extraction from diagrams

#### 3. Metadata Validator
**`validate-metadata.py`** - YAML front matter validation
```bash
# Validate document metadata
python3 scripts/validate-metadata.py

# Verbose output
python3 scripts/validate-metadata.py --verbose

# Generate metadata templates
python3 scripts/validate-metadata.py --generate-templates

# NPM script
npm run docs:metadata
```

**Validation Rules:**
- **Viewpoints**: Requires `title`, `viewpoint`, `description`, `stakeholders`
- **Perspectives**: Requires `title`, `perspective`, `description`, `quality_attributes`
- **Templates**: Requires `title`, `type`, `description`, `usage`
- **General**: Requires `title`, `description`

**Features:**
- Validates YAML front matter syntax
- Checks required fields by document type
- Validates viewpoint/perspective values against standard lists
- Generates metadata templates for missing documents
- Cross-references related documents

#### 4. Translation Quality Checker
**`check-translation-quality.sh`** - Enhanced translation synchronization
```bash
# Check translation quality
bash scripts/check-translation-quality.sh

# NPM script
npm run docs:translation
```

**Features:**
- Validates Chinese-English document pairs
- Checks terminology consistency using `.terminology.json`
- Validates Viewpoints & Perspectives structure
- Supports new documentation organization

### Test and Validation Scripts

#### System Test Runner
**`test-documentation-quality.sh`** - Comprehensive system testing
```bash
# Test all quality check components
bash scripts/test-documentation-quality.sh
```

**Features:**
- Tests all quality check scripts
- Creates test files with known issues
- Validates script functionality
- Generates test reports

## Configuration Files

### Markdown Linting
**`.markdownlint.json`** - Markdown syntax rules
- Line length: 120 characters
- Allows HTML elements: `br`, `sub`, `sup`, `kbd`, `details`, `summary`
- Ordered list style enforcement
- Heading structure validation

### Terminology Dictionary
**`docs/.terminology.json`** - Translation terminology
- Rozanski & Woods viewpoints and perspectives
- DDD strategic and tactical patterns
- Architecture and design terminology
- Stakeholder and implementation terms

## NPM Scripts

Add to your workflow:
```bash
# Individual checks
npm run docs:quality      # Comprehensive quality check
npm run docs:links        # Link validation (internal)
npm run docs:links:external # Link validation (external)
npm run docs:diagrams     # Diagram validation
npm run docs:metadata     # Metadata validation
npm run docs:translation  # Translation quality

# Combined validation
npm run docs:validate     # Run all checks
```

## GitHub Actions Integration

**`.github/workflows/documentation-quality.yml`** - Automated CI/CD checks

**Triggers:**
- Push to `main`/`develop` branches
- Pull requests affecting documentation
- Manual workflow dispatch

**Features:**
- Runs all quality checks automatically
- Uploads reports as artifacts
- Comments on PRs with quality summary
- Supports external link checking option

## Reports and Output

### Report Structure
```
build/reports/documentation-quality/
├── reports-summaries/frontend/documentation-quality-summary.md     # Main summary report
├── markdown-lint-report.txt             # Markdown syntax issues
├── link-check-report.txt                # Link validation results
├── advanced-link-check.json             # Detailed link analysis
├── advanced-link-check.md               # Link check summary
├── diagram-validation-report.json       # Diagram validation data
├── reports-summaries/diagrams/diagram-validation-report.md         # Diagram validation summary
├── metadata-validation-report.json      # Metadata validation data
├── metadata-validation-report.md        # Metadata validation summary
└── translation-sync-report.txt          # Translation quality results
```

### Report Contents
- **Pass/Fail Status**: Clear indicators for each check
- **Error Details**: Specific file locations and error messages
- **Recommendations**: Actionable steps to fix issues
- **Statistics**: Coverage percentages and success rates
- **Trends**: Historical comparison when available

## Prerequisites

### Required Tools
- **Node.js 18+**: For link checking and npm scripts
- **Python 3.8+**: For diagram and metadata validation
- **Bash**: For shell scripts (macOS/Linux)

### Python Dependencies
```bash
# Install PyYAML for metadata validation
pip3 install pyyaml

# Or use system package manager
brew install python-yq  # macOS
```

### Node.js Dependencies
```bash
# Install markdownlint globally
npm install -g markdownlint-cli

# Or install project dependencies
npm ci
```

## Usage Examples

### Daily Development Workflow
```bash
# Quick quality check before commit
npm run docs:quality

# Fix any issues found
# Re-run specific checks
npm run docs:links
npm run docs:metadata
```

### Pre-Release Validation
```bash
# Comprehensive validation including external links
npm run docs:links:external
npm run docs:validate

# Review reports
open build/reports/documentation-quality/reports-summaries/frontend/documentation-quality-summary.md
```

### Continuous Integration
```bash
# In CI/CD pipeline
npm ci
npm run docs:validate

# Upload reports for review
# Fail build if critical issues found
```

## Troubleshooting

### Common Issues

**1. PyYAML Not Available**
```bash
# Install PyYAML
pip3 install --user pyyaml
# Or use system package manager
brew install python-yq
```

**2. Markdownlint Not Found**
```bash
# Install globally
npm install -g markdownlint-cli
# Or use npx
npx markdownlint docs/**/*.md
```

**3. Permission Denied**
```bash
# Make scripts executable
chmod +x scripts/*.sh scripts/*.js scripts/*.py
```

**4. External Link Timeouts**
```bash
# Skip external links for faster checking
node scripts/check-links-advanced.js  # Internal only
```

### Performance Optimization

**For Large Documentation Sets:**
- Use internal-only link checking for daily development
- Run external link checking in CI/CD only
- Use `--verbose` flag to monitor progress
- Consider parallel execution for multiple directories

**Memory Usage:**
- Diagram validation: ~50MB for 100+ diagrams
- Link checking: ~100MB for 300+ documents
- Metadata validation: ~20MB for typical documentation

## Integration with Development Workflow

### Pre-Commit Hooks
```bash
# Add to .git/hooks/pre-commit
#!/bin/bash
npm run docs:quality
if [ $? -ne 0 ]; then
    echo "Documentation quality checks failed. Please fix issues before committing."
    exit 1
fi
```

### IDE Integration
- Configure markdownlint extension for real-time syntax checking
- Set up file watchers for automatic validation
- Use task runners for integrated quality checks

### Documentation Review Process
1. **Author**: Run `npm run docs:quality` before creating PR
2. **Reviewer**: Check quality reports in PR comments
3. **Maintainer**: Ensure all quality checks pass before merge
4. **Release**: Run comprehensive validation including external links

This comprehensive quality check system ensures consistent, high-quality documentation that meets the requirements of the Viewpoints & Perspectives restructure specification.