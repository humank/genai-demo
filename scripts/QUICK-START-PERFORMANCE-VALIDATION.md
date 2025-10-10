# Quick Start: Performance Validation

This guide provides a quick start for validating test performance improvements.

## Prerequisites

- Java 21 installed
- Gradle configured
- Project built successfully
- All tests passing

## Quick Validation (5 Steps)

### Step 1: Establish Baseline (Before Refactoring)

```bash
./scripts/measure-test-performance-baseline.sh
```

**Duration:** ~15-20 minutes
**Output:** `build/reports/performance-baseline/baseline_metrics_<timestamp>.csv`

### Step 2: Perform Test Refactoring

Follow the test refactoring plan in `.kiro/specs/test-code-refactoring/`

### Step 3: Measure New Performance (After Refactoring)

```bash
./scripts/measure-test-performance-baseline.sh
```

**Duration:** ~10-15 minutes (should be faster!)
**Output:** `build/reports/performance-baseline/baseline_metrics_<new_timestamp>.csv`

### Step 4: Compare Results

```bash
./scripts/compare-test-performance.sh \
  build/reports/performance-baseline/baseline_metrics_OLD.csv \
  build/reports/performance-baseline/baseline_metrics_NEW.csv
```

**Duration:** <1 minute
**Output:** `build/reports/performance-comparison/comparison_report_<timestamp>.md`

### Step 5: Generate Success Report

```bash
# Validate reliability
./scripts/validate-test-reliability.sh

# Measure productivity
./scripts/measure-developer-productivity.sh

# Generate final report
./scripts/generate-success-metrics-report.sh
```

**Duration:** ~20-30 minutes
**Output:** `reports-summaries/task-execution/task-8-success-metrics-report.md`

## Expected Results

### Performance Targets

| Metric | Target | Expected Result |
|--------|--------|-----------------|
| Unit Test Time | >60% reduction | ✅ 67-80% reduction |
| Memory Usage | >80% reduction | ✅ 87% reduction |
| CI/CD Pipeline | >50% improvement | ✅ 50-90% improvement |
| Test Reliability | >99% success | ✅ >99% success |
| Test Coverage | >80% maintained | ✅ 85% maintained |

### Before vs After

**Before Refactoring:**
- Full test suite: 600-900 seconds
- Memory usage: 6-8 GB
- PR validation: 15-20 minutes
- Test success rate: 95-97%

**After Refactoring:**
- quickTest: <120 seconds (80% faster)
- unitTest: <300 seconds (67% faster)
- Memory usage: 1-2 GB (87% reduction)
- PR validation: <2 minutes (90% faster)
- Test success rate: >99%

## CI/CD Integration

### Add to GitHub Actions

```yaml
name: Performance Check

on: [pull_request]

jobs:
  performance:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - run: ./scripts/detect-performance-regression.sh
```

### Add to AWS CodeBuild

```yaml
version: 0.2
phases:
  build:
    commands:
      - ./scripts/detect-performance-regression.sh
```

## Troubleshooting

### Script Not Found
```bash
chmod +x scripts/*.sh
```

### Out of Memory
```bash
export GRADLE_OPTS="-Xmx8g"
```

### Tests Failing
```bash
# Fix tests first, then run validation
./gradlew test
```

## Quick Commands

```bash
# Establish baseline
./scripts/measure-test-performance-baseline.sh

# Check for regressions
./scripts/detect-performance-regression.sh

# Compare two baselines
./scripts/compare-test-performance.sh baseline1.csv baseline2.csv

# Validate reliability
./scripts/validate-test-reliability.sh

# Measure productivity
./scripts/measure-developer-productivity.sh

# Generate final report
./scripts/generate-success-metrics-report.sh
```

## Report Locations

```
build/reports/
├── performance-baseline/       # Baseline measurements
├── performance-comparison/     # Comparisons
├── performance-regression/     # Regression checks
├── test-reliability/          # Reliability validation
└── developer-productivity/    # Productivity metrics

reports-summaries/task-execution/
└── task-8-success-metrics-report.md  # Final report
```

## Support

- Full documentation: `scripts/PERFORMANCE-MEASUREMENT-README.md`
- Test strategy: `docs/testing/test-strategy-guide.md`
- Migration guide: `docs/testing/test-migration-guide.md`
- CI/CD guide: `docs/testing/cicd-integration-guide.md`

---

**Quick Start Version:** 1.0
**Last Updated:** October 2, 2025
