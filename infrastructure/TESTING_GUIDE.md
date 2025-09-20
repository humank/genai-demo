# CDK Infrastructure Testing Guide

## Overview

This document provides comprehensive guidance for testing the consolidated CDK infrastructure.

## Test Structure

### Test Categories

1. **Unit Tests** (`test/unit/`) - Fast, isolated tests for individual components (26 tests)
2. **Integration Tests** (`test/integration/`) - Tests for stack interactions and full deployment (8 tests)
3. **Consolidated Tests** (`test/consolidated-stack.test.ts`) - Main test suite for all stacks (18 tests)
4. **Compliance Tests** (`test/cdk-nag-suppressions.test.ts`) - CDK Nag compliance validation (4 tests)
5. **Stack Tests** (`test/*-stack.test.ts`) - Individual stack validation tests (47 tests)

### Test Files

```
test/
├── unit/                           # Unit tests
│   ├── network-stack.test.ts      # Network stack unit tests
│   ├── network-stack.unit.test.ts # Additional network tests
│   └── security-stack.test.ts     # Security stack unit tests
├── integration/                    # Integration tests
│   └── deployment.test.ts         # Full deployment tests
├── consolidated-stack.test.ts      # Main consolidated test suite
├── cdk-nag-suppressions.test.ts   # CDK Nag compliance tests
├── certificate-stack.test.ts      # Certificate stack tests
├── msk-stack.test.ts              # MSK stack tests
├── network-stack.test.ts          # Network stack integration tests
├── rds-stack.test.ts              # RDS stack tests
├── security-stack.test.ts         # Security stack integration tests
└── setup.ts                       # Test configuration
```

## Running Tests

### All Tests

```bash
npm test
```

### Specific Test Categories

```bash
# Unit tests only
npm test -- --testPathPatterns="unit/"

# Integration tests only
npm test -- --testPathPatterns="integration/"

# Consolidated tests only
npm test -- --testPathPatterns="consolidated-stack.test.ts"

# CDK Nag compliance tests
npm test -- --testPathPatterns="cdk-nag-suppressions.test.ts"
```

### Individual Test Files

```bash
# Network stack tests
npm test -- --testPathPatterns="network-stack.test.ts"

# Security stack tests
npm test -- --testPathPatterns="security-stack.test.ts"

# Full deployment tests
npm test -- --testPathPatterns="deployment.test.ts"
```

## Test Configuration

### Jest Configuration

- **Timeout**: 60 seconds for CDK synthesis operations
- **Max Workers**: 1 (sequential execution to avoid CDK conflicts)
- **Setup**: Automatic environment configuration and warning suppression

### Environment Variables

Tests automatically set:

- `CDK_DEFAULT_REGION=us-east-1`
- `CDK_DEFAULT_ACCOUNT=123456789012`
- `JSII_DEPRECATED=quiet`

## CDK Synthesis Testing

### Without CDK Nag (Recommended for Development)

```bash
npx cdk synth --context enableCdkNag=false
```

### With CDK Nag (Security Compliance)

```bash
npx cdk synth
```

### Specific Stack

```bash
npx cdk synth genai-demo-development-NetworkStack --context enableCdkNag=false
```

## Test Coverage

### Current Test Coverage

- **NetworkStack**: ✅ VPC, Subnets, Security Groups, Route Tables, Outputs (15 tests)
- **SecurityStack**: ✅ KMS Key, IAM Role, Policies, Outputs (8 tests)
- **CoreInfrastructureStack**: ✅ ALB, Target Groups, Listeners (3 tests)
- **AlertingStack**: ✅ SNS Topics, Subscriptions (2 tests)
- **ObservabilityStack**: ✅ CloudWatch Logs, Dashboard (2 tests)
- **AnalyticsStack**: ✅ S3, Kinesis Firehose, Lambda, Glue (3 tests)
- **Integration Tests**: ✅ Full deployment, Cross-stack references (8 tests)
- **Compliance Tests**: ✅ CDK Nag suppressions, Security validation (4 tests)

### Integration Tests

- **Full Deployment**: ✅ All stacks with dependencies
- **Cross-Stack References**: ✅ VPC, Security Groups, KMS Keys
- **Optional Components**: ✅ Analytics stack enable/disable

## CDK Nag Compliance

### Known Suppressions

The following CDK Nag rules are suppressed with justification:

1. **AwsSolutions-VPC7**: VPC Flow Logs optional for development
2. **AwsSolutions-EC23**: ALB requires internet access on ports 80/443
3. **AwsSolutions-IAM4**: CloudWatch managed policy required
4. **AwsSolutions-IAM5**: KMS wildcard permissions required

### Running Compliance Tests

```bash
npm test -- --testPathPatterns="cdk-nag-suppressions.test.ts"
```

## Troubleshooting

### Common Issues

1. **Multiple Synthesis Error**
   - **Cause**: CDK App synthesized multiple times in same test
   - **Solution**: Create new App instance for each test

2. **Resource Count Mismatch**
   - **Cause**: CDK creates additional resources automatically
   - **Solution**: Check actual template and update expected counts

3. **Template Property Mismatch**
   - **Cause**: CDK generates different property structure than expected
   - **Solution**: Use `Template.fromStack(stack).toJSON()` to inspect

### Debugging Tests

```bash
# Run with verbose output
npm test -- --verbose

# Run specific test with debugging
npm test -- --testPathPatterns="network-stack.test.ts" --verbose

# Check CDK template output
npx cdk synth --context enableCdkNag=false > template.yaml
```

### Test Performance

- **Unit Tests**: < 1 second each
- **Integration Tests**: 2-5 seconds each
- **Full Test Suite**: ~16 seconds (103 tests)
- **Parallel Execution**: Optimized with maxWorkers=1 for CDK synthesis

## Best Practices

### Writing Tests

1. **Use Descriptive Names**: Test names should clearly describe what is being tested
2. **Test One Thing**: Each test should verify a single aspect
3. **Use Proper Setup**: Create fresh CDK App for each test suite
4. **Check Resources**: Verify both existence and configuration
5. **Test Outputs**: Ensure stack outputs are correctly configured

### Test Organization

1. **Group Related Tests**: Use `describe` blocks for logical grouping
2. **Share Setup**: Use `beforeEach` for common test setup
3. **Clean Isolation**: Avoid test interdependencies
4. **Document Expectations**: Comment complex test logic

### Performance Optimization

1. **Minimize Synthesis**: Reuse templates within test suites
2. **Parallel Execution**: Use separate test files for independent tests
3. **Mock External Dependencies**: Use mocks for external services
4. **Selective Testing**: Run only relevant tests during development

## Continuous Integration

### GitHub Actions Integration

```yaml
- name: Run CDK Tests
  run: |
    cd infrastructure
    npm ci
    npm test
    npm run synth -- --context enableCdkNag=false
```

### Pre-commit Hooks

```bash
# Run tests before commit
npm test -- --testPathPatterns="consolidated-stack.test.ts"
```

## Future Enhancements

### Planned Test Additions

1. **Performance Tests**: Resource creation time benchmarks
2. **Security Tests**: Automated security scanning
3. **Cost Tests**: Resource cost estimation validation
4. **Multi-Region Tests**: Cross-region deployment validation

### Test Automation

1. **Snapshot Testing**: Template change detection
2. **Property Testing**: Random input validation
3. **Load Testing**: Large-scale deployment simulation
4. **Regression Testing**: Automated change impact analysis

## Resources

- [AWS CDK Testing Guide](https://docs.aws.amazon.com/cdk/v2/guide/testing.html)
- [CDK Assertions Library](https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.assertions-readme.html)
- [CDK Nag Documentation](https://github.com/cdklabs/cdk-nag)
- [Jest Testing Framework](https://jestjs.io/docs/getting-started)
