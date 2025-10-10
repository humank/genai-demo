"""
Base Staging Test Framework

This module provides the foundation for all staging environment tests including:
- Base test classes with authentication and retry logic
- Common test utilities and fixtures
- Environment configuration management
- Error handling and logging
- Performance metrics collection

Requirements: 12.1, 12.6
Implementation: Pure Python using pytest, requests, and boto3
"""

import pytest
import requests
import boto3
import time
import logging
import os
import json
from typing import Dict, Any, Optional, List, Callable
from dataclasses import dataclass, asdict, field
from contextlib import contextmanager
from functools import wraps
from datetime import datetime, timedelta
import hashlib

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@dataclass
class StagingEnvironmentConfig:
    """Configuration for staging environment"""
    # API Configuration
    api_base_url: str = os.getenv('STAGING_API_URL', 'https://api-staging.example.com')
    api_timeout: int = int(os.getenv('API_TIMEOUT', '30'))
    api_retry_count: int = int(os.getenv('API_RETRY_COUNT', '3'))
    api_retry_delay: float = float(os.getenv('API_RETRY_DELAY', '1.0'))
    
    # Authentication Configuration
    auth_type: str = os.getenv('AUTH_TYPE', 'bearer')  # bearer, basic, api_key
    auth_token: Optional[str] = os.getenv('AUTH_TOKEN')
    auth_username: Optional[str] = os.getenv('AUTH_USERNAME')
    auth_password: Optional[str] = os.getenv('AUTH_PASSWORD')
    auth_api_key: Optional[str] = os.getenv('AUTH_API_KEY')
    
    # AWS Configuration
    aws_region: str = os.getenv('AWS_REGION', 'ap-northeast-1')
    aws_profile: Optional[str] = os.getenv('AWS_PROFILE')
    
    # Test Configuration
    test_data_dir: str = os.getenv('TEST_DATA_DIR', './test-data')
    test_report_dir: str = os.getenv('TEST_REPORT_DIR', './reports')
    enable_performance_metrics: bool = os.getenv('ENABLE_PERFORMANCE_METRICS', 'true').lower() == 'true'
    
    # Database Configuration
    db_host: str = os.getenv('DB_HOST', 'localhost')
    db_port: int = int(os.getenv('DB_PORT', '5432'))
    db_name: str = os.getenv('DB_NAME', 'staging_db')
    db_user: str = os.getenv('DB_USER', 'staging_user')
    db_password: str = os.getenv('DB_PASSWORD', 'staging_password')
    
    # Cache Configuration
    redis_host: str = os.getenv('REDIS_HOST', 'localhost')
    redis_port: int = int(os.getenv('REDIS_PORT', '6379'))
    redis_password: Optional[str] = os.getenv('REDIS_PASSWORD')
    
    # Messaging Configuration
    kafka_brokers: str = os.getenv('KAFKA_BROKERS', 'localhost:9092')
    
    def __post_init__(self):
        """Validate configuration after initialization"""
        if not self.api_base_url:
            raise ValueError("API base URL is required")
        
        # Ensure directories exist
        os.makedirs(self.test_data_dir, exist_ok=True)
        os.makedirs(self.test_report_dir, exist_ok=True)


@dataclass
class TestMetrics:
    """Performance metrics for test execution"""
    test_name: str
    start_time: datetime
    end_time: Optional[datetime] = None
    duration_ms: float = 0.0
    success: bool = False
    error_message: Optional[str] = None
    api_calls: int = 0
    api_call_durations: List[float] = field(default_factory=list)
    
    def complete(self, success: bool, error_message: Optional[str] = None):
        """Mark test as complete and calculate duration"""
        self.end_time = datetime.now()
        self.duration_ms = (self.end_time - self.start_time).total_seconds() * 1000
        self.success = success
        self.error_message = error_message
    
    def add_api_call(self, duration_ms: float):
        """Record an API call duration"""
        self.api_calls += 1
        self.api_call_durations.append(duration_ms)
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert metrics to dictionary"""
        return {
            'test_name': self.test_name,
            'start_time': self.start_time.isoformat(),
            'end_time': self.end_time.isoformat() if self.end_time else None,
            'duration_ms': self.duration_ms,
            'success': self.success,
            'error_message': self.error_message,
            'api_calls': self.api_calls,
            'avg_api_call_duration_ms': sum(self.api_call_durations) / len(self.api_call_durations) if self.api_call_durations else 0,
            'max_api_call_duration_ms': max(self.api_call_durations) if self.api_call_durations else 0,
            'min_api_call_duration_ms': min(self.api_call_durations) if self.api_call_durations else 0
        }


class RetryableError(Exception):
    """Exception that indicates an operation should be retried"""
    pass


class NonRetryableError(Exception):
    """Exception that indicates an operation should not be retried"""
    pass


def retry_on_failure(max_retries: int = 3, delay: float = 1.0, backoff: float = 2.0):
    """
    Decorator for retrying functions on failure with exponential backoff
    
    Args:
        max_retries: Maximum number of retry attempts
        delay: Initial delay between retries in seconds
        backoff: Multiplier for delay after each retry
    """
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            current_delay = delay
            last_exception = None
            
            for attempt in range(max_retries + 1):
                try:
                    return func(*args, **kwargs)
                except NonRetryableError:
                    # Don't retry on non-retryable errors
                    raise
                except Exception as e:
                    last_exception = e
                    if attempt < max_retries:
                        logger.warning(
                            f"Attempt {attempt + 1}/{max_retries + 1} failed for {func.__name__}: {str(e)}. "
                            f"Retrying in {current_delay}s..."
                        )
                        time.sleep(current_delay)
                        current_delay *= backoff
                    else:
                        logger.error(f"All {max_retries + 1} attempts failed for {func.__name__}")
            
            raise last_exception
        
        return wrapper
    return decorator


class BaseStagingApiTest:
    """
    Base class for all staging API tests
    
    Provides:
    - Authentication handling
    - Retry logic with exponential backoff
    - Error handling and logging
    - Performance metrics collection
    - Common test utilities
    """
    
    def __init__(self, config: Optional[StagingEnvironmentConfig] = None):
        """Initialize base test with configuration"""
        self.config = config or StagingEnvironmentConfig()
        self.session = requests.Session()
        self.metrics: List[TestMetrics] = []
        self.current_test_metrics: Optional[TestMetrics] = None
        
        # Configure session with authentication
        self._configure_authentication()
        
        # Configure session defaults
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'User-Agent': 'StagingTestFramework/1.0'
        })
    
    def _configure_authentication(self):
        """Configure authentication for API requests"""
        if self.config.auth_type == 'bearer' and self.config.auth_token:
            self.session.headers['Authorization'] = f'Bearer {self.config.auth_token}'
        elif self.config.auth_type == 'basic' and self.config.auth_username and self.config.auth_password:
            self.session.auth = (self.config.auth_username, self.config.auth_password)
        elif self.config.auth_type == 'api_key' and self.config.auth_api_key:
            self.session.headers['X-API-Key'] = self.config.auth_api_key
        else:
            logger.warning("No authentication configured")
    
    @contextmanager
    def track_test_metrics(self, test_name: str):
        """Context manager for tracking test metrics"""
        metrics = TestMetrics(test_name=test_name, start_time=datetime.now())
        self.current_test_metrics = metrics
        
        try:
            yield metrics
            metrics.complete(success=True)
        except Exception as e:
            metrics.complete(success=False, error_message=str(e))
            raise
        finally:
            self.metrics.append(metrics)
            self.current_test_metrics = None
    
    @retry_on_failure(max_retries=3, delay=1.0, backoff=2.0)
    def make_api_request(
        self,
        method: str,
        endpoint: str,
        data: Optional[Dict[str, Any]] = None,
        params: Optional[Dict[str, Any]] = None,
        headers: Optional[Dict[str, str]] = None,
        timeout: Optional[int] = None
    ) -> requests.Response:
        """
        Make an API request with retry logic
        
        Args:
            method: HTTP method (GET, POST, PUT, DELETE, etc.)
            endpoint: API endpoint (relative to base URL)
            data: Request body data
            params: Query parameters
            headers: Additional headers
            timeout: Request timeout in seconds
        
        Returns:
            Response object
        
        Raises:
            RetryableError: For errors that should be retried
            NonRetryableError: For errors that should not be retried
        """
        url = f"{self.config.api_base_url.rstrip('/')}/{endpoint.lstrip('/')}"
        timeout = timeout or self.config.api_timeout
        
        # Merge headers
        request_headers = self.session.headers.copy()
        if headers:
            request_headers.update(headers)
        
        start_time = time.time()
        
        try:
            response = self.session.request(
                method=method.upper(),
                url=url,
                json=data,
                params=params,
                headers=request_headers,
                timeout=timeout
            )
            
            duration_ms = (time.time() - start_time) * 1000
            
            # Track API call metrics
            if self.current_test_metrics:
                self.current_test_metrics.add_api_call(duration_ms)
            
            logger.info(
                f"{method.upper()} {url} - Status: {response.status_code} - Duration: {duration_ms:.2f}ms"
            )
            
            # Determine if error is retryable
            if response.status_code >= 500:
                # Server errors are retryable
                raise RetryableError(f"Server error: {response.status_code} - {response.text}")
            elif response.status_code == 429:
                # Rate limiting is retryable
                raise RetryableError(f"Rate limited: {response.status_code}")
            elif response.status_code >= 400:
                # Client errors are not retryable
                raise NonRetryableError(f"Client error: {response.status_code} - {response.text}")
            
            return response
            
        except requests.exceptions.Timeout:
            raise RetryableError(f"Request timeout after {timeout}s")
        except requests.exceptions.ConnectionError as e:
            raise RetryableError(f"Connection error: {str(e)}")
        except requests.exceptions.RequestException as e:
            raise NonRetryableError(f"Request failed: {str(e)}")
    
    def get(self, endpoint: str, **kwargs) -> requests.Response:
        """Make a GET request"""
        return self.make_api_request('GET', endpoint, **kwargs)
    
    def post(self, endpoint: str, data: Optional[Dict[str, Any]] = None, **kwargs) -> requests.Response:
        """Make a POST request"""
        return self.make_api_request('POST', endpoint, data=data, **kwargs)
    
    def put(self, endpoint: str, data: Optional[Dict[str, Any]] = None, **kwargs) -> requests.Response:
        """Make a PUT request"""
        return self.make_api_request('PUT', endpoint, data=data, **kwargs)
    
    def delete(self, endpoint: str, **kwargs) -> requests.Response:
        """Make a DELETE request"""
        return self.make_api_request('DELETE', endpoint, **kwargs)
    
    def patch(self, endpoint: str, data: Optional[Dict[str, Any]] = None, **kwargs) -> requests.Response:
        """Make a PATCH request"""
        return self.make_api_request('PATCH', endpoint, data=data, **kwargs)
    
    def assert_response_success(self, response: requests.Response, expected_status: int = 200):
        """Assert that response is successful"""
        assert response.status_code == expected_status, \
            f"Expected status {expected_status}, got {response.status_code}: {response.text}"
    
    def assert_response_contains(self, response: requests.Response, key: str, value: Any = None):
        """Assert that response contains a specific key and optionally a value"""
        data = response.json()
        assert key in data, f"Response does not contain key '{key}': {data}"
        
        if value is not None:
            assert data[key] == value, f"Expected {key}={value}, got {data[key]}"
    
    def assert_response_time(self, duration_ms: float, max_duration_ms: float):
        """Assert that response time is within acceptable limits"""
        assert duration_ms <= max_duration_ms, \
            f"Response time {duration_ms:.2f}ms exceeds maximum {max_duration_ms}ms"
    
    def wait_for_condition(
        self,
        condition: Callable[[], bool],
        timeout: int = 30,
        interval: float = 1.0,
        error_message: str = "Condition not met within timeout"
    ):
        """
        Wait for a condition to become true
        
        Args:
            condition: Callable that returns True when condition is met
            timeout: Maximum time to wait in seconds
            interval: Time between checks in seconds
            error_message: Error message if timeout is reached
        """
        start_time = time.time()
        
        while time.time() - start_time < timeout:
            if condition():
                return
            time.sleep(interval)
        
        raise TimeoutError(error_message)
    
    def generate_test_data(self, template: Dict[str, Any], **overrides) -> Dict[str, Any]:
        """
        Generate test data from a template with overrides
        
        Args:
            template: Base template for test data
            **overrides: Values to override in template
        
        Returns:
            Generated test data
        """
        data = template.copy()
        data.update(overrides)
        return data
    
    def save_metrics_report(self, filename: Optional[str] = None):
        """Save test metrics to a JSON report"""
        if not filename:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename = f"test_metrics_{timestamp}.json"
        
        filepath = os.path.join(self.config.test_report_dir, filename)
        
        report = {
            'generated_at': datetime.now().isoformat(),
            'total_tests': len(self.metrics),
            'successful_tests': sum(1 for m in self.metrics if m.success),
            'failed_tests': sum(1 for m in self.metrics if not m.success),
            'total_duration_ms': sum(m.duration_ms for m in self.metrics),
            'total_api_calls': sum(m.api_calls for m in self.metrics),
            'tests': [m.to_dict() for m in self.metrics]
        }
        
        with open(filepath, 'w') as f:
            json.dump(report, f, indent=2)
        
        logger.info(f"Metrics report saved to {filepath}")
        return filepath
    
    def cleanup(self):
        """Cleanup resources after tests"""
        if self.session:
            self.session.close()
        
        if self.config.enable_performance_metrics and self.metrics:
            self.save_metrics_report()


class BaseStagingIntegrationTest(BaseStagingApiTest):
    """
    Base class for integration tests that require AWS services
    
    Extends BaseStagingApiTest with AWS service integration
    """
    
    def __init__(self, config: Optional[StagingEnvironmentConfig] = None):
        """Initialize base integration test"""
        super().__init__(config)
        
        # Initialize AWS clients
        self.aws_session = self._create_aws_session()
        self.cloudwatch = self.aws_session.client('cloudwatch')
        self.xray = self.aws_session.client('xray')
        self.logs = self.aws_session.client('logs')
        self.rds = self.aws_session.client('rds')
        self.elasticache = self.aws_session.client('elasticache')
        self.kafka = self.aws_session.client('kafka')
    
    def _create_aws_session(self) -> boto3.Session:
        """Create AWS session with configured credentials"""
        session_kwargs = {'region_name': self.config.aws_region}
        
        if self.config.aws_profile:
            session_kwargs['profile_name'] = self.config.aws_profile
        
        return boto3.Session(**session_kwargs)
    
    def publish_cloudwatch_metric(
        self,
        namespace: str,
        metric_name: str,
        value: float,
        unit: str = 'None',
        dimensions: Optional[List[Dict[str, str]]] = None
    ):
        """Publish a custom metric to CloudWatch"""
        metric_data = {
            'MetricName': metric_name,
            'Value': value,
            'Unit': unit,
            'Timestamp': datetime.now()
        }
        
        if dimensions:
            metric_data['Dimensions'] = dimensions
        
        try:
            self.cloudwatch.put_metric_data(
                Namespace=namespace,
                MetricData=[metric_data]
            )
            logger.info(f"Published metric {metric_name}={value} to CloudWatch namespace {namespace}")
        except Exception as e:
            logger.error(f"Failed to publish CloudWatch metric: {str(e)}")
    
    def query_cloudwatch_logs(
        self,
        log_group: str,
        query: str,
        start_time: Optional[datetime] = None,
        end_time: Optional[datetime] = None
    ) -> List[Dict[str, Any]]:
        """Query CloudWatch Logs Insights"""
        if not start_time:
            start_time = datetime.now() - timedelta(hours=1)
        if not end_time:
            end_time = datetime.now()
        
        try:
            response = self.logs.start_query(
                logGroupName=log_group,
                startTime=int(start_time.timestamp()),
                endTime=int(end_time.timestamp()),
                queryString=query
            )
            
            query_id = response['queryId']
            
            # Wait for query to complete
            while True:
                result = self.logs.get_query_results(queryId=query_id)
                status = result['status']
                
                if status == 'Complete':
                    return result['results']
                elif status in ['Failed', 'Cancelled']:
                    raise Exception(f"Query {status.lower()}: {query}")
                
                time.sleep(1)
                
        except Exception as e:
            logger.error(f"Failed to query CloudWatch Logs: {str(e)}")
            return []


# Pytest fixtures for common test setup

@pytest.fixture(scope='session')
def staging_config():
    """Provide staging environment configuration"""
    return StagingEnvironmentConfig()


@pytest.fixture(scope='session')
def api_test_client(staging_config):
    """Provide API test client"""
    client = BaseStagingApiTest(staging_config)
    yield client
    client.cleanup()


@pytest.fixture(scope='session')
def integration_test_client(staging_config):
    """Provide integration test client with AWS services"""
    client = BaseStagingIntegrationTest(staging_config)
    yield client
    client.cleanup()


@pytest.fixture(scope='function')
def test_metrics():
    """Provide test metrics tracking"""
    metrics = []
    yield metrics
    # Metrics are automatically collected by test clients


# Example usage and test templates

class ExampleApiTest(BaseStagingApiTest):
    """Example API test demonstrating usage of base class"""
    
    def test_health_check(self):
        """Test API health check endpoint"""
        with self.track_test_metrics('test_health_check'):
            response = self.get('/health')
            self.assert_response_success(response, 200)
            self.assert_response_contains(response, 'status', 'healthy')
    
    def test_create_resource(self):
        """Test resource creation with retry logic"""
        with self.track_test_metrics('test_create_resource'):
            test_data = {
                'name': 'Test Resource',
                'description': 'Created by staging test'
            }
            
            response = self.post('/api/resources', data=test_data)
            self.assert_response_success(response, 201)
            self.assert_response_contains(response, 'id')
            
            # Verify resource was created
            resource_id = response.json()['id']
            get_response = self.get(f'/api/resources/{resource_id}')
            self.assert_response_success(get_response, 200)


if __name__ == '__main__':
    # Example: Run a simple test
    config = StagingEnvironmentConfig()
    test_client = BaseStagingApiTest(config)
    
    try:
        with test_client.track_test_metrics('example_test'):
            response = test_client.get('/health')
            print(f"Health check status: {response.status_code}")
            print(f"Response: {response.json()}")
    finally:
        test_client.cleanup()
