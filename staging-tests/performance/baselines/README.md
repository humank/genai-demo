# Performance Baselines

This directory contains performance baselines for different test scenarios. Baselines are used to detect performance regressions and track performance trends over time.

## Overview

Performance baselines provide reference points for comparing current test results against historical performance. They help identify:

- Performance regressions (degradation from baseline)
- Performance improvements (better than baseline)
- Performance stability (consistency with baseline)
- Long-term performance trends

## Baseline Files

Each test scenario has its own baseline file:

- `normal-load_baseline.json` - Normal load test baseline
- `peak-load_baseline.json` - Peak load test baseline  
- `stress-test_baseline.json` - Stress test baseline
- `endurance-test_baseline.json` - Endurance test baseline
- `spike-test_baseline.json` - Spike test baseline

## Baseline Structure

Each baseline file contains:

```json
{
  "test_scenario": "normal-load",
  "created_at": "2025-01-21T10:30:00Z",
  "updated_at": "2025-01-21T15:45:00Z",
  "version": "1.2",
  "data_points": 10,
  "baseline_metrics": {
    "mean_response_time": {
      "value": 850.5,
      "unit": "ms",
      "confidence_interval": [820.2, 880.8],
      "std_deviation": 45.3
    },
    "p95_response_time": {
      "value": 1250.0,
      "unit": "ms", 
      "confidence_interval": [1200.0, 1300.0],
      "std_deviation": 75.5
    },
    "throughput": {
      "value": 125.8,
      "unit": "req/s",
      "confidence_interval": [120.0, 131.6],
      "std_deviation": 8.2
    },
    "success_rate": {
      "value": 99.2,
      "unit": "%",
      "confidence_interval": [98.8, 99.6],
      "std_deviation": 0.4
    }
  },
  "regression_thresholds": {
    "critical": {
      "response_time_increase_percent": 50,
      "throughput_decrease_percent": 30,
      "success_rate_decrease_percent": 5
    },
    "high": {
      "response_time_increase_percent": 30,
      "throughput_decrease_percent": 20,
      "success_rate_decrease_percent": 3
    }
  },
  "metadata": {
    "calculation_method": "percentile",
    "percentile": 75,
    "outliers_removed": 2,
    "source_files": [
      "results/20250121_103000/normal-load-results.json",
      "results/20250121_113000/normal-load-results.json"
    ]
  }
}
```

## Creating Baselines

### Automatic Creation

Baselines are automatically created when you run performance tests:

```bash
# Run test with baseline creation
./scripts/run-performance-tests.sh --scenario normal --baseline

# Create baseline from existing results
python scripts/performance-baseline.py create \
  --test-name normal-load \
  --results-file results/normal-load-results.json \
  --description "Initial baseline for normal load"
```

### Manual Creation

You can manually create baselines from multiple test results:

```bash
# Create baseline from multiple result files
python scripts/performance-baseline.py create \
  --test-name normal-load \
  --results-files results/run1.json results/run2.json results/run3.json \
  --description "Baseline from 3 test runs"
```

## Updating Baselines

### Rolling Average Update

Update baseline using rolling average of recent results:

```bash
python scripts/performance-baseline.py update \
  --test-name normal-load \
  --results-file results/latest-results.json \
  --strategy rolling_average
```

### Weighted Average Update

Update baseline using weighted average:

```bash
python scripts/performance-baseline.py update \
  --test-name normal-load \
  --results-file results/latest-results.json \
  --strategy weighted_average \
  --new-weight 0.3
```

### Replace Baseline

Completely replace existing baseline:

```bash
python scripts/performance-baseline.py update \
  --test-name normal-load \
  --results-file results/latest-results.json \
  --strategy replace
```

## Regression Detection

### Compare Against Baseline

```bash
# Compare current results against baseline
python scripts/performance-baseline.py compare \
  --test-name normal-load \
  --results-file results/current-results.json \
  --output regression-analysis.json
```

### Automated Regression Detection

Regression detection runs automatically during performance tests:

```bash
# Run test with regression detection
./scripts/run-performance-tests.sh --scenario normal --baseline --alerts
```

## Regression Thresholds

Different severity levels for performance regressions:

### Critical Regressions
- Response time increase > 50%
- Throughput decrease > 30% 
- Success rate decrease > 5%

### High Regressions
- Response time increase > 30%
- Throughput decrease > 20%
- Success rate decrease > 3%

### Medium Regressions  
- Response time increase > 20%
- Throughput decrease > 15%
- Success rate decrease > 2%

### Low Regressions
- Response time increase > 10%
- Throughput decrease > 10%
- Success rate decrease > 1%

## Baseline Management

### List Baselines

```bash
# List all baselines
python scripts/performance-baseline.py list

# List baselines for specific scenario
python scripts/performance-baseline.py list --test-name normal-load
```

### Baseline Information

```bash
# Get baseline details
python scripts/performance-baseline.py info --test-name normal-load
```

### Backup Baselines

```bash
# Backup all baselines
python scripts/performance-baseline.py backup

# Backup specific baseline
python scripts/performance-baseline.py backup --test-name normal-load
```

### Restore Baselines

```bash
# Restore from backup
python scripts/performance-baseline.py restore --backup-file backup/normal-load_baseline_20250121.json
```

## Configuration

Baseline behavior is configured in `baseline-config.yml`:

### Key Configuration Options

- **Calculation Method**: mean, median, percentile
- **Outlier Detection**: IQR, Z-score, isolation forest
- **Update Strategy**: rolling_average, weighted_average, replace
- **Regression Thresholds**: Per severity level
- **Retention Policy**: How long to keep baselines

### Test Scenario Specific Config

Each test scenario can have custom configuration:

```yaml
test_scenarios:
  normal_load:
    baseline_creation:
      min_test_runs: 3
      calculation_method: "median"
    regression_detection:
      thresholds:
        critical:
          response_time_increase_percent: 40
```

## Best Practices

### Baseline Creation

1. **Sufficient Data**: Use at least 3-5 test runs for baseline creation
2. **Stable Environment**: Create baselines in consistent test environment
3. **Representative Load**: Ensure test scenarios represent real usage
4. **Clean Data**: Remove outliers and failed test runs

### Baseline Maintenance

1. **Regular Updates**: Update baselines after significant improvements
2. **Version Control**: Keep baseline versions for rollback capability
3. **Documentation**: Document baseline changes and rationale
4. **Validation**: Validate baselines after major system changes

### Regression Detection

1. **Appropriate Thresholds**: Set thresholds based on business requirements
2. **Statistical Significance**: Use statistical tests for regression confirmation
3. **Context Awareness**: Consider deployment and environment changes
4. **Trend Analysis**: Look at trends, not just individual regressions

## Troubleshooting

### Common Issues

#### Baseline Creation Fails

```bash
# Check if enough data points
python scripts/performance-baseline.py info --test-name normal-load

# Verify result file format
python scripts/performance-baseline.py validate --results-file results/test-results.json
```

#### High Baseline Variability

```bash
# Check coefficient of variation
python scripts/performance-baseline.py analyze --test-name normal-load

# Review outlier detection settings in baseline-config.yml
```

#### False Positive Regressions

```bash
# Adjust regression thresholds in baseline-config.yml
# Enable statistical significance testing
# Increase consecutive regression requirement
```

### Debug Mode

Enable debug mode for detailed logging:

```bash
python scripts/performance-baseline.py --debug compare \
  --test-name normal-load \
  --results-file results/current-results.json
```

## Integration

### CI/CD Pipeline

```yaml
# GitHub Actions example
- name: Performance Test with Baseline
  run: |
    ./scripts/run-performance-tests.sh --scenario normal --baseline
    
- name: Check for Regressions
  run: |
    python scripts/performance-baseline.py compare \
      --test-name normal-load \
      --results-file results/normal-load-results.json \
      --fail-on-regression
```

### Monitoring Integration

```bash
# Send baseline metrics to monitoring system
python scripts/performance-baseline.py export \
  --test-name normal-load \
  --format prometheus \
  --output metrics.txt
```

## File Naming Convention

- Baseline files: `{test_scenario}_baseline.json`
- Backup files: `{test_scenario}_baseline_{timestamp}.json`
- Version files: `{test_scenario}_baseline_v{version}.json`

## Directory Structure

```
baselines/
├── README.md                           # This file
├── normal-load_baseline.json           # Normal load baseline
├── peak-load_baseline.json             # Peak load baseline
├── stress-test_baseline.json           # Stress test baseline
├── endurance-test_baseline.json        # Endurance test baseline
├── spike-test_baseline.json            # Spike test baseline
├── backup/                             # Baseline backups
│   ├── normal-load_baseline_20250121.json
│   └── peak-load_baseline_20250120.json
└── versions/                           # Baseline versions
    ├── normal-load_baseline_v1.0.json
    └── normal-load_baseline_v1.1.json
```

This baseline system ensures consistent performance monitoring and helps maintain application performance quality over time.