# IDE Configuration Standards

## Overview

This document defines the IDE configuration standards for Kiro IDE (VS Code-based) to enable automatic code quality checks and fixes.

**Purpose**: Configure IDE for automatic code quality enforcement and quick fixes.
**Target IDE**: Kiro IDE (VS Code-based)

---

## 🚨 CRITICAL REMINDER - DOCUMENTATION DATES 🚨

**Last Updated**: 2025-11-22

---

## Required Extensions

### Core Java Extensions

Install these extensions for full Java development support:

```bash
# Language Support
- redhat.java                          # Java Language Support
- vscjava.vscode-java-debug            # Java Debugging
- vscjava.vscode-java-test             # Java Test Runner
- vscjava.vscode-java-dependency       # Java Dependency Viewer

# Build Tools
- vscjava.vscode-gradle                # Gradle Support
- vscjava.vscode-maven                 # Maven Support (if needed)

# Code Quality
- shengchen.vscode-checkstyle          # Checkstyle Integration
- sonarsource.sonarlint-vscode         # SonarLint (Code Quality & Security)

# Spring Boot
- vmware.vscode-spring-boot            # Spring Boot Tools
- vscjava.vscode-spring-initializr     # Spring Initializr
- vscjava.vscode-spring-boot-dashboard # Spring Boot Dashboard

# Testing
- cucumber.cucumber-official           # Cucumber/Gherkin Support

# Utilities
- editorconfig.editorconfig            # EditorConfig Support
- streetsidesoftware.code-spell-checker # Spell Checker
```

### Installation

Extensions are listed in `.vscode/extensions.json`. Kiro IDE will prompt to install them automatically.

---

## Automatic Checks and Fixes

### 1. Save Actions (Automatic)

When you save a file, the following actions run automatically:

- ✅ **Organize Imports**: Remove unused imports, sort imports
- ✅ **Fix All**: Apply all available quick fixes
- ✅ **Format Code**: Apply code formatting
- ✅ **Trim Trailing Whitespace**: Remove trailing spaces
- ✅ **Insert Final Newline**: Ensure file ends with newline

**Configuration**: `.vscode/settings.json`

```json
{
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.organizeImports": "explicit",
    "source.fixAll": "explicit"
  },
  "files.trimTrailingWhitespace": true,
  "files.insertFinalNewline": true
}
```

### 2. Import Organization

**Automatic Rules**:
- Remove unused imports
- Sort imports alphabetically
- Group imports by package
- Use star imports when > 5 classes from same package

**Configuration**:
```json
{
  "java.saveActions.organizeImports": true,
  "java.sources.organizeImports.starThreshold": 5,
  "java.sources.organizeImports.staticStarThreshold": 3
}
```

### 3. Code Formatting

**Format**: Google Java Style (configurable)

**Automatic Formatting**:
- On save
- On paste (optional)
- Manual: `Shift + Alt + F` (Windows/Linux) or `Shift + Option + F` (Mac)

**Configuration**:
```json
{
  "java.format.settings.profile": "GoogleStyle",
  "[java]": {
    "editor.defaultFormatter": "redhat.java",
    "editor.tabSize": 4,
    "editor.insertSpaces": true
  }
}
```

---

## Code Quality Checks

### 1. Checkstyle Integration

**What it checks**:
- Code style violations
- Naming conventions
- Unused imports
- Magic numbers
- Method length
- Complexity

**Configuration**: `config/checkstyle/checkstyle.xml`

**How to use**:
1. Violations appear as warnings in Problems panel
2. Hover over warning to see details
3. Click lightbulb icon for quick fixes (if available)

**Manual check**:
```bash
./gradlew checkstyleMain
./gradlew checkstyleTest
```

### 2. SonarLint Integration

**What it checks**:
- Code smells
- Bugs
- Security vulnerabilities
- Code duplications
- Complexity issues

**Real-time feedback**:
- Underlines issues in editor
- Shows in Problems panel
- Provides detailed explanations
- Suggests fixes

**How to use**:
1. Issues appear immediately as you type
2. Hover for detailed explanation
3. Click "Show Rule Description" for more info
4. Apply suggested fix if available

### 3. Java Language Server Checks

**Built-in checks**:
- ✅ Syntax errors
- ✅ Type errors
- ✅ Unused variables
- ✅ Unused methods
- ✅ Unused fields
- ✅ Null pointer warnings
- ✅ Resource leak warnings
- ✅ Deprecated API usage

**Automatic fixes available**:
- Remove unused imports
- Remove unused variables
- Add missing imports
- Implement missing methods
- Add null checks
- Convert to try-with-resources

---

## Quick Fixes

### How to Apply Quick Fixes

1. **Automatic (on save)**: Most fixes apply automatically
2. **Manual (lightbulb icon)**:
   - Click lightbulb icon next to warning/error
   - Or press `Ctrl + .` (Windows/Linux) or `Cmd + .` (Mac)
   - Select fix from menu

### Common Quick Fixes

| Issue | Quick Fix |
|-------|-----------|
| Unused import | Remove import |
| Missing import | Add import |
| Unused variable | Remove variable or prefix with `_` |
| Unused method | Remove method or suppress warning |
| Magic number | Extract to constant |
| Long method | Extract method |
| Duplicate code | Extract method |
| Null pointer risk | Add null check |
| Resource leak | Convert to try-with-resources |

---

## Problems Panel

### View Problems

- **Open**: `View → Problems` or `Ctrl + Shift + M`
- **Filter**: By severity (Error, Warning, Info)
- **Group**: By file or by type

### Problem Severity

- 🔴 **Error**: Must fix (prevents compilation)
- 🟡 **Warning**: Should fix (code quality issue)
- 🔵 **Info**: Consider fixing (suggestion)

### Batch Fixes

Some issues can be fixed in batch:
1. Right-click in Problems panel
2. Select "Fix All" or "Fix All in File"

---

## Code Style Configuration

### EditorConfig

**File**: `.editorconfig`

**What it controls**:
- Indentation (spaces vs tabs)
- Indent size
- Line endings (LF vs CRLF)
- Trailing whitespace
- Final newline
- Max line length

**Applies to**:
- Java files: 4 spaces, max 120 chars
- YAML/JSON: 2 spaces
- Markdown: No trailing whitespace trim

### Java Formatting Rules

**Key rules**:
- Indent: 4 spaces (no tabs)
- Max line length: 120 characters
- Braces: Same line (K&R style)
- Blank lines: 1 between methods
- Import order: Alphabetical, grouped

---

## Gradle Integration

### Static Analysis Tasks

Run these tasks to check code quality:

```bash
# Checkstyle
./gradlew checkstyleMain        # Check main code
./gradlew checkstyleTest        # Check test code

# PMD (if configured)
./gradlew pmdMain               # Check for code smells

# SpotBugs (if configured)
./gradlew spotbugsMain          # Find potential bugs

# All checks
./gradlew check                 # Run all quality checks
```

### Reports

After running checks, view reports:
- Checkstyle: `build/reports/checkstyle/`
- PMD: `build/reports/pmd/`
- SpotBugs: `build/reports/spotbugs/`

---

## Keyboard Shortcuts

### Essential Shortcuts

| Action | Windows/Linux | Mac |
|--------|---------------|-----|
| Format Document | `Shift + Alt + F` | `Shift + Option + F` |
| Quick Fix | `Ctrl + .` | `Cmd + .` |
| Organize Imports | `Shift + Alt + O` | `Shift + Option + O` |
| Show Problems | `Ctrl + Shift + M` | `Cmd + Shift + M` |
| Go to Definition | `F12` | `F12` |
| Find References | `Shift + F12` | `Shift + F12` |
| Rename Symbol | `F2` | `F2` |
| Show Hover | `Ctrl + K Ctrl + I` | `Cmd + K Cmd + I` |

---

## Troubleshooting

### Extension Not Working

1. **Check extension is installed**:
   - `View → Extensions`
   - Search for extension name
   - Click "Install" if not installed

2. **Reload window**:
   - `Ctrl + Shift + P` → "Reload Window"

3. **Check Java installation**:
   ```bash
   java -version  # Should be Java 21
   ```

4. **Clean and rebuild**:
   ```bash
   ./gradlew clean build
   ```

### Checkstyle Not Running

1. **Check configuration file exists**:
   ```bash
   ls config/checkstyle/checkstyle.xml
   ```

2. **Check settings.json**:
   ```json
   {
     "java.checkstyle.configuration": "${workspaceFolder}/config/checkstyle/checkstyle.xml"
   }
   ```

3. **Reload Checkstyle**:
   - `Ctrl + Shift + P` → "Checkstyle: Reload Configuration"

### SonarLint Not Showing Issues

1. **Check extension is active**:
   - Look for SonarLint icon in status bar

2. **Check file is in workspace**:
   - SonarLint only analyzes files in workspace

3. **Check language is supported**:
   - Java files should have `.java` extension

---

## Best Practices

### Daily Development

1. ✅ **Save frequently**: Automatic fixes apply on save
2. ✅ **Fix warnings immediately**: Don't accumulate technical debt
3. ✅ **Use quick fixes**: Faster than manual fixes
4. ✅ **Check Problems panel**: Before committing code
5. ✅ **Run Gradle checks**: Before pushing to remote

### Before Committing

```bash
# 1. Format all code
# (Already done on save)

# 2. Run all checks
./gradlew check

# 3. Run tests
./gradlew test

# 4. Check Problems panel
# Should have 0 errors, minimal warnings
```

### Code Review

Reviewers should check:
- [ ] No Checkstyle violations
- [ ] No SonarLint critical issues
- [ ] No unused imports/variables
- [ ] Proper formatting
- [ ] All tests pass

---

## Configuration Files

### Project Structure

```text
.
├── .vscode/
│   ├── settings.json          # IDE settings
│   └── extensions.json        # Recommended extensions
├── .editorconfig              # Code style rules
├── config/
│   └── checkstyle/
│       └── checkstyle.xml     # Checkstyle rules
└── build.gradle               # Gradle configuration
```

### Customization

To customize rules:

1. **Checkstyle**: Edit `config/checkstyle/checkstyle.xml`
2. **SonarLint**: Edit `.vscode/settings.json` → `sonarlint.rules`
3. **Formatting**: Edit `.vscode/settings.json` → `java.format.*`
4. **EditorConfig**: Edit `.editorconfig`

---

## Integration with CI/CD

### GitHub Actions

The same checks run in CI/CD:

```yaml
- name: Run Checkstyle
  run: ./gradlew checkstyleMain checkstyleTest

- name: Run Tests
  run: ./gradlew test

- name: Run All Checks
  run: ./gradlew check
```

**Important**: Fix issues locally before pushing to avoid CI failures.

---

## Related Documentation

- **Development Standards**: [development-standards.md](development-standards.md)
- **Code Quality Checklist**: [code-quality-checklist.md](code-quality-checklist.md)
- **Code Review Standards**: [code-review-standards.md](code-review-standards.md)

---

## Quick Reference

| Feature | How to Use | When |
|---------|-----------|------|
| Auto-format | Save file | Every save |
| Organize imports | Save file | Every save |
| Quick fix | `Ctrl + .` | When you see warning |
| View problems | `Ctrl + Shift + M` | Before commit |
| Run checks | `./gradlew check` | Before push |

---

**Document Version**: 1.0
**Last Updated**: 2025-11-22
**Owner**: Development Team
