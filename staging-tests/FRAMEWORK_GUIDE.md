# Staging Test Framework Guide

## Overview

This comprehensive staging test framework provides a robust foundation for testing applications in staging environments. It includes:

- **Base test classes** with authentication and retry logic
- **Performance metrics** collection and reporting
- **AWS service integration** for CloudWatch, X-Ray, and other services
- **Flexible configuration** via environment variables
- **Pytest integration** with custom fixtures and markers

## Quick Start

### 1. Installation

```bash
# Navigate to staging-tests directory
cd staging-tests

# Install dependencies
pip install -r requirements.txt

# Copy environment configuration
cp .env.example .env

# Edit .env with your staging environment details
nano .env
```

### 2. Configuration

Update `.env` file with your staging environment configuration:

```bash
# API Configuration
STAGING_API_URL=https://api-staging.your-domain.com
AUTH_TOKEN=your_bearer_token_here

# AWS Configuration
AWS_REGION=ap-northeast-1
AWS_PROFILE=staging  # Optional

# Database Configuration
DB_HOST=your-aurora-endpoint.rds.amazonaws.com
DB_PORT=5432
DB_NAME=staging_db
DB_USER=staging_user
DB_PASSWORD=your_password
```

### 3. Running Tests

```bash
# Run all tests
pytest

# Run specific test suite
pytest examples/test_example_api.py

# Run tests with specific markers
pytest -m smoke  # Run smoke tests only
pytest -m "api and not slow"  # Run API tests excluding slow ones

# Run tests with verbose output
pytest -v

# Run tests in parallel
pytest -n auto  # Use all available CPU cores

# Generate HTML report
pytest --html=reports/test_report.html --self-contained-html
```

## Framework Components

### Base Test Classes

#### BaseStagingApiTest

Base class for API testing with built-in features:

- **Authentication**: Supports Bearer, Basic, and API Key authentication
- **Retry Logic**: Automatic retry with exponential backoff for transient failures
- **Metrics Tracking**: Collects performance metrics for all API calls
- **Error Handling**: Distinguishes between retryable and non-retryable errors

**Example Usage:**

```python
from base_staging_test import BaseStagingApiTest

class TestMyApi(BaseStagingApiTest):
    def test_create_resource(self):
        with self.track_test_metrics('test_create_resource'):
            # Make API request with automatic retry
            response = self.post('/api/resources', data={'name': 'Test'})
            
            # Assert response
            self.assert_response_success(response, 201)
            self.assert_response_contains(response, 'id')
```

#### BaseStagingIntegrationTest

Extends `BaseStagingApiTest` with AWS service integration:

- **CloudWatch**: Publish custom metrics and query logs
- **X-Ray**: Distributed tracing integration
- **RDS**: Database service integration
- **ElastiCache**: Cache service integration
- **MSK**: Kafka service integration

**Example Usage:**

```python
from base_staging_test import BaseStagingIntegrationTest

class TestAwsIntegration(BaseStagingIntegrationTest):
    def test_publish_metrics(self):
        with self.track_test_metrics('test_publish_metrics'):
            # Publish metric to CloudWatch
            self.publish_cloudwatch_metric(
                namespace='MyApp',
                metric_name='TestMetric',
                value=100.0,
                unit='Count'
            )
```

### Configuration Management

#### StagingEnvironmentConfig

Centralized configuration class that loads settings from environment variables:

```python
from base_staging_test import StagingEnvironmentConfig

# Load configuration from environment
config = StagingEnvironmentConfig()

# Access configuration values
print(config.api_base_url)
print(config.aws_region)
print(config.db_host)
```

### Retry Logic

#### @retry_on_failure Decorator

Decorator for adding retry logic to any function:

```python
from base_staging_test import retry_on_failure, RetryableError

@retry_on_failure(max_retries=3, delay=1.0, backoff=2.0)
def flaky_operation():
    # This will be retried up to 3 times with exponential backoff
    if random.random() < 0.5:
        raise RetryableError("Temporary failure")
    return "Success"
```

### Performance Metrics

#### TestMetrics

Automatic collection of performance metrics:

- Test execution duration
- API call count and durations
- Success/failure status
- Error messages

**Example:**

```python
class TestPerformance(BaseStagingApiTest):
    def test_with_metrics(self):
        with self.track_test_metrics('test_with_metrics'):
            # All API calls are automatically tracked
            self.get('/api/resource/1')
            self.get('/api/resource/2')
            self.get('/api/resource/3')
        
        # Metrics are automatically saved at test cleanup
```

**Generated Metrics Report:**

```json
{
  "generated_at": "2025-10-09T10:30:00",
  "total_tests": 5,
  "successful_tests": 4,
  "failed_tests": 1,
  "total_duration_ms": 1234.56,
  "total_api_calls": 15,
  "tests": [
    {
      "test_name": "test_with_metrics",
      "duration_ms": 234.56,
      "success": true,
      "api_calls": 3,
      "avg_api_call_duration_ms": 78.19
    }
  ]
}
```

## Test Organization

### Test Markers

Use pytest markers to categorize tests:

```python
@pytest.mark.smoke
def test_health_check():
    """Quick smoke test"""
    pass

@pytest.mark.integration
@pytest.mark.database
def test_database_connection():
    """Database integration test"""
    pass

@pytest.mark.performance
@pytest.mark.slow
def test_load_handling():
    """Performance test (slow)"""
    pass
```

### Test Structure

Organize tests by functionality:

```
staging-tests/
├── base_staging_test.py          # Base test classes
├── examples/                      # Example tests
│   └── test_example_api.py
├── integration/                   # Integration tests
│   ├── database/
│   ├── cache/
│   └── messaging/
├── performance/                   # Performance tests
├── security/                      # Security tests
└── cross-region/                  # Cross-region tests
```

## Best Practices

### 1. Use Context Managers for Metrics

Always use `track_test_metrics` context manager:

```python
def test_example(self):
    with self.track_test_metrics('test_example'):
        # Test code here
        pass
```

### 2. Clean Up Test Data

Always clean up test data in finally blocks or fixtures:

```python
def test_create_resource(self):
    resource_id = None
    try:
        response = self.post('/api/resources', data={'name': 'Test'})
        resource_id = response.json()['id']
        # Test assertions
    finally:
        if resource_id:
            self.delete(f'/api/resources/{resource_id}')
```

### 3. Use Fixtures for Common Setup

```python
@pytest.fixture(autouse=True)
def setup_test_data(self):
    """Setup test data before each test"""
    self.test_data = {'name': 'Test', 'value': 123}
    yield
    # Cleanup after test
```

### 4. Handle Retryable vs Non-Retryable Errors

```python
from base_staging_test import RetryableError, NonRetryableError

def make_request(self):
    response = self.get('/api/resource')
    
    if response.status_code >= 500:
        # Server errors are retryable
        raise RetryableError("Server error")
    elif response.status_code == 400:
        # Client errors are not retryable
        raise NonRetryableError("Bad request")
```

### 5. Use Assertions Helpers

```python
def test_api_response(self):
    response = self.get('/api/resource')
    
    # Use built-in assertion helpers
    self.assert_response_success(response, 200)
    self.assert_response_contains(response, 'id')
    self.assert_response_contains(response, 'name', 'Expected Name')
```

## Advanced Features

### Parallel Test Execution

Run tests in parallel for faster execution:

```bash
# Use all available CPU cores
pytest -n auto

# Use specific number of workers
pytest -n 4
```

### Test Timeouts

Configure timeouts for slow tests:

```python
@pytest.mark.timeout(60)  # 60 second timeout
def test_slow_operation(self):
    pass
```

### Custom Fixtures

Create custom fixtures for common test scenarios:

```python
@pytest.fixture(scope='session')
def test_customer():
    """Create a test customer for the entire test session"""
    client = BaseStagingApiTest()
    response = client.post('/api/customers', data={'name': 'Test Customer'})
    customer_id = response.json()['id']
    
    yield customer_id
    
    # Cleanup after all tests
    client.delete(f'/api/customers/{customer_id}')
```

### Conditional Test Execution

Skip tests based on environment:

```python
@pytest.mark.skipif(
    os.getenv('ENABLE_CROSS_REGION_TESTS') != 'true',
    reason="Cross-region tests disabled"
)
def test_cross_region_replication(self):
    pass
```

## Troubleshooting

### Common Issues

#### 1. Authentication Failures

```bash
# Verify authentication configuration
echo $AUTH_TOKEN

# Test authentication manually
curl -H "Authorization: Bearer $AUTH_TOKEN" $STAGING_API_URL/health
```

#### 2. Connection Timeouts

```bash
# Increase timeout in .env
API_TIMEOUT=60

# Or in test code
response = self.get('/api/resource', timeout=60)
```

#### 3. AWS Credentials

```bash
# Verify AWS credentials
aws sts get-caller-identity --profile staging

# Or use environment variables
export AWS_ACCESS_KEY_ID=your_key
export AWS_SECRET_ACCESS_KEY=your_secret
```

### Debug Mode

Enable verbose logging:

```bash
# Run tests with debug logging
pytest -v --log-cli-level=DEBUG

# Or set in environment
export LOG_LEVEL=DEBUG
```

### Test Reports

Generate detailed test reports:

```bash
# HTML report
pytest --html=reports/test_report.html --self-contained-html

# JUnit XML report (for CI/CD)
pytest --junitxml=reports/junit.xml

# Coverage report
pytest --cov=. --cov-report=html
```

## Integration with CI/CD

### GitHub Actions Example

```yaml
name: Staging Tests

on:
  push:
    branches: [main, develop]
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.11'
      
      - name: Install dependencies
        run: |
          cd staging-tests
          pip install -r requirements.txt
      
      - name: Run tests
        env:
          STAGING_API_URL: ${{ secrets.STAGING_API_URL }}
          AUTH_TOKEN: ${{ secrets.AUTH_TOKEN }}
          AWS_REGION: ap-northeast-1
        run: |
          cd staging-tests
          pytest -v --html=reports/test_report.html
      
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: staging-tests/reports/
```

## Contributing

### Adding New Test Suites

1. Create a new test file following naming convention: `test_*.py`
2. Inherit from `BaseStagingApiTest` or `BaseStagingIntegrationTest`
3. Add appropriate pytest markers
4. Include docstrings for all test methods
5. Clean up test data in teardown

### Adding New Features

1. Update `base_staging_test.py` with new functionality
2. Add tests for new features in `examples/`
3. Update this guide with usage examples
4. Update `requirements.txt` if new dependencies are needed

## Support

For questions or issues:

1. Check this guide and example tests
2. Review test execution logs
3. Check environment configuration
4. Contact the development team

## References

- [Pytest Documentation](https://docs.pytest.org/)
- [Requests Library](https://requests.readthedocs.io/)
- [Boto3 Documentation](https://boto3.amazonaws.com/v1/documentation/api/latest/index.html)
- [AWS SDK for Python](https://aws.amazon.com/sdk-for-python/)
