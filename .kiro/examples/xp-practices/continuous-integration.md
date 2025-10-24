# Continuous Integration Guide

## Overview

Practical guide for implementing Continuous Integration (CI) in our project.

**Related Standards**: [Development Standards](../../steering/development-standards.md)

---

## What is Continuous Integration?

**Continuous Integration (CI)** is the practice of frequently integrating code changes into a shared repository, with automated builds and tests.

### Core Principles

1. **Integrate frequently** - Multiple times per day
2. **Automate the build** - One command to build
3. **Test automatically** - Every commit triggers tests
4. **Fix broken builds immediately** - Top priority
5. **Keep builds fast** - < 10 minutes

---

## CI Workflow

```
Developer → Commit → Push → CI Server → Build → Test → Deploy
                                ↓
                            Notify Team
```

### Daily Workflow

```bash
# 1. Pull latest changes
git pull origin main

# 2. Create feature branch
git checkout -b feature/order-submission

# 3. Write test (Red)
# Write failing test

# 4. Implement (Green)
# Make test pass

# 5. Refactor
# Improve code quality

# 6. Run local tests
./gradlew test

# 7. Commit frequently
git add .
git commit -m "feat: add order submission validation"

# 8. Push to trigger CI
git push origin feature/order-submission

# 9. CI runs automatically
# - Build
# - Test
# - Code quality checks
# - Security scan

# 10. Create PR when ready
# CI runs again on PR

# 11. Merge after approval
# CI runs on main branch
```

---

## Build Automation

### Single Command Build

```bash
# ✅ GOOD: One command builds everything
./gradlew clean build

# This should:
# - Compile code
# - Run all tests
# - Generate reports
# - Create artifacts
```

### Gradle Build Configuration

```gradle
// build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.5'
    id 'jacoco'
    id 'pmd'
    id 'checkstyle'
}

// Fast feedback tasks
tasks.register('quickTest', Test) {
    description = 'Fast unit tests for daily development'
    useJUnitPlatform {
        excludeTags 'integration', 'slow'
    }
    maxHeapSize = '2g'
    maxParallelForks = Runtime.runtime.availableProcessors()
}

// Pre-commit verification
tasks.register('preCommit') {
    dependsOn 'quickTest', 'checkstyleMain', 'pmdMain'
    description = 'Run before committing code'
}

// Full CI build
tasks.register('ciBuild') {
    dependsOn 'clean', 'build', 'test', 'jacocoTestReport', 'archUnit'
    description = 'Complete CI build with all checks'
}
```

---

## Automated Testing

### Test Pyramid in CI

```
┌─────────────────┐
│   E2E Tests     │  5%  - Slow, comprehensive
│   (< 3s each)   │
├─────────────────┤
│ Integration     │  15% - Medium speed
│ Tests           │
│ (< 500ms each)  │
├─────────────────┤
│   Unit Tests    │  80% - Fast, focused
│   (< 50ms each) │
└─────────────────┘
```

### CI Test Execution

```bash
# Stage 1: Fast feedback (< 2 min)
./gradlew quickTest

# Stage 2: Integration tests (< 5 min)
./gradlew integrationTest

# Stage 3: E2E tests (< 10 min)
./gradlew e2eTest

# Stage 4: Quality checks
./gradlew jacocoTestReport pmdMain checkstyleMain
```

---

## GitHub Actions CI Pipeline

### Basic CI Workflow

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Build with Gradle
      run: ./gradlew build
    
    - name: Run tests
      run: ./gradlew test
    
    - name: Generate test report
      run: ./gradlew jacocoTestReport
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        files: ./build/reports/jacoco/test/jacocoTestReport.xml
```

### Multi-Stage CI Pipeline

```yaml
# .github/workflows/ci-advanced.yml
name: Advanced CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  # Stage 1: Fast feedback
  quick-tests:
    runs-on: ubuntu-latest
    timeout-minutes: 5
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle
    - name: Quick tests
      run: ./gradlew quickTest
  
  # Stage 2: Code quality
  code-quality:
    runs-on: ubuntu-latest
    needs: quick-tests
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle
    - name: Code quality checks
      run: ./gradlew checkstyleMain pmdMain spotbugsMain
  
  # Stage 3: Integration tests
  integration-tests:
    runs-on: ubuntu-latest
    needs: quick-tests
    timeout-minutes: 10
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle
    - name: Integration tests
      run: ./gradlew integrationTest
  
  # Stage 4: Security scan
  security-scan:
    runs-on: ubuntu-latest
    needs: quick-tests
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle
    - name: Security scan
      run: ./gradlew dependencyCheckAnalyze
  
  # Stage 5: E2E tests (only on main)
  e2e-tests:
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    needs: [integration-tests, code-quality]
    timeout-minutes: 15
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle
    - name: E2E tests
      run: ./gradlew e2eTest
```

---

## Build Notifications

### Slack Integration

```yaml
# .github/workflows/ci.yml
jobs:
  build:
    # ... build steps ...
    
    - name: Notify Slack on failure
      if: failure()
      uses: slackapi/slack-github-action@v1
      with:
        payload: |
          {
            "text": "❌ Build failed on ${{ github.ref }}",
            "blocks": [
              {
                "type": "section",
                "text": {
                  "type": "mrkdwn",
                  "text": "*Build Failed*\nBranch: ${{ github.ref }}\nCommit: ${{ github.sha }}\nAuthor: ${{ github.actor }}"
                }
              }
            ]
          }
      env:
        SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

---

## Best Practices

### 1. Commit Frequently

```bash
# ✅ GOOD: Small, frequent commits
git commit -m "feat: add email validation"
git commit -m "test: add test for empty email"
git commit -m "refactor: extract validation to value object"

# ❌ BAD: Large, infrequent commits
git commit -m "feat: complete entire order module"
# (100+ files changed, 5000+ lines)
```

### 2. Fix Broken Builds Immediately

```
Priority when build breaks:
1. Stop new work
2. Fix the build
3. Verify fix works
4. Resume normal work

If fix takes > 30 minutes:
- Revert the breaking commit
- Fix offline
- Re-commit when ready
```

### 3. Keep Builds Fast

```
Target build times:
- Quick tests: < 2 minutes
- Full build: < 10 minutes
- E2E tests: < 15 minutes

Strategies:
- Parallel test execution
- Test categorization
- Incremental builds
- Build caching
```

### 4. Never Commit on Broken Build

```bash
# ✅ GOOD: Check build status first
./gradlew test
# All tests pass
git commit -m "feat: add feature"

# ❌ BAD: Commit without testing
git commit -m "feat: add feature"
# Build breaks for everyone
```

---

## Continuous Deployment

### Deployment Pipeline

```yaml
# .github/workflows/cd.yml
name: CD

on:
  push:
    branches: [ main ]

jobs:
  deploy-staging:
    runs-on: ubuntu-latest
    environment: staging
    steps:
    - uses: actions/checkout@v3
    - name: Deploy to staging
      run: |
        ./gradlew build
        ./deploy-staging.sh
    
    - name: Run smoke tests
      run: ./gradlew smokeTest -Denv=staging
  
  deploy-production:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment: production
    steps:
    - uses: actions/checkout@v3
    - name: Deploy to production
      run: |
        ./gradlew build
        ./deploy-production.sh
    
    - name: Run smoke tests
      run: ./gradlew smokeTest -Denv=production
```

---

## Monitoring CI Health

### Key Metrics

```
Build Success Rate:     > 95%
Average Build Time:     < 10 minutes
Time to Fix Build:      < 30 minutes
Test Coverage:          > 80%
Failed Test Rate:       < 1%
```

### CI Dashboard

```
┌─────────────────────────────────┐
│ CI Health Dashboard             │
├─────────────────────────────────┤
│ Build Status:        ✅ Passing │
│ Last Build:          2 min ago  │
│ Success Rate:        98%        │
│ Avg Build Time:      8m 32s     │
│ Test Coverage:       85%        │
│ Failed Tests:        0          │
└─────────────────────────────────┘
```

---

## Troubleshooting

### Build Fails Locally But Passes in CI

```bash
# Check environment differences
# - Java version
# - Gradle version
# - Environment variables
# - File permissions

# Clean and rebuild
./gradlew clean build --no-build-cache
```

### Flaky Tests

```bash
# Identify flaky tests
./gradlew test --rerun-tasks

# Fix flaky tests:
# - Remove time dependencies
# - Fix race conditions
# - Improve test isolation
# - Use proper test data setup
```

### Slow Builds

```bash
# Profile build
./gradlew build --profile

# Optimize:
# - Enable parallel execution
# - Use build cache
# - Optimize test execution
# - Split into stages
```

---

## Summary

Effective CI requires:

1. **Frequent integration** - Multiple commits per day
2. **Automated builds** - One command to build and test
3. **Fast feedback** - Builds complete in < 10 minutes
4. **Immediate fixes** - Broken builds are top priority
5. **Team discipline** - Everyone follows the process

Remember: **CI is a practice, not just a tool** - it requires team commitment.

---

**Related Documentation**:
- [Development Standards](../../steering/development-standards.md)
- [Testing Strategy](../../steering/testing-strategy.md)
- [Simple Design Examples](simple-design-examples.md)
