"""
Example API Test Suite

This module demonstrates how to use the BaseStagingApiTest class
for creating comprehensive API tests with retry logic, metrics tracking,
and error handling.

Requirements: 12.1, 12.6
"""

import pytest
import sys
import os

# Add parent directory to path to import base_staging_test
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from base_staging_test import (
    BaseStagingApiTest,
    BaseStagingIntegrationTest,
    StagingEnvironmentConfig,
    retry_on_failure,
    RetryableError,
    NonRetryableError
)


class TestHealthCheckApi(BaseStagingApiTest):
    """Test suite for health check endpoints"""
    
    @pytest.mark.smoke
    @pytest.mark.api
    def test_health_check_endpoint(self):
        """Test that health check endpoint returns healthy status"""
        with self.track_test_metrics('test_health_check_endpoint'):
            # Make request to health endpoint
            response = self.get('/health')
            
            # Assert response is successful
            self.assert_response_success(response, 200)
            
            # Assert response contains expected fields
            self.assert_response_contains(response, 'status', 'healthy')
            
            # Verify response time is acceptable
            if self.current_test_metrics and self.current_test_metrics.api_call_durations:
                last_duration = self.current_test_metrics.api_call_durations[-1]
                self.assert_response_time(last_duration, 1000)  # Max 1 second
    
    @pytest.mark.smoke
    @pytest.mark.api
    def test_readiness_check(self):
        """Test that readiness check endpoint returns ready status"""
        with self.track_test_metrics('test_readiness_check'):
            response = self.get('/health/ready')
            self.assert_response_success(response, 200)
            self.assert_response_contains(response, 'ready', True)
    
    @pytest.mark.smoke
    @pytest.mark.api
    def test_liveness_check(self):
        """Test that liveness check endpoint returns alive status"""
        with self.track_test_metrics('test_liveness_check'):
            response = self.get('/health/live')
            self.assert_response_success(response, 200)
            self.assert_response_contains(response, 'alive', True)


class TestCustomerApi(BaseStagingApiTest):
    """Test suite for customer API endpoints"""
    
    @pytest.fixture(autouse=True)
    def setup_test_data(self):
        """Setup test data before each test"""
        self.test_customer_data = {
            'name': 'Test Customer',
            'email': 'test@example.com',
            'phone': '+1234567890'
        }
        yield
        # Cleanup after test if needed
    
    @pytest.mark.api
    @pytest.mark.integration
    def test_create_customer(self):
        """Test creating a new customer"""
        with self.track_test_metrics('test_create_customer'):
            # Create customer
            response = self.post('/api/v1/customers', data=self.test_customer_data)
            
            # Assert creation was successful
            self.assert_response_success(response, 201)
            self.assert_response_contains(response, 'id')
            self.assert_response_contains(response, 'name', self.test_customer_data['name'])
            
            # Store customer ID for cleanup
            customer_id = response.json()['id']
            
            # Verify customer can be retrieved
            get_response = self.get(f'/api/v1/customers/{customer_id}')
            self.assert_response_success(get_response, 200)
            
            # Cleanup: Delete test customer
            delete_response = self.delete(f'/api/v1/customers/{customer_id}')
            self.assert_response_success(delete_response, 204)
    
    @pytest.mark.api
    @pytest.mark.integration
    def test_get_customer_by_id(self):
        """Test retrieving a customer by ID"""
        with self.track_test_metrics('test_get_customer_by_id'):
            # First create a customer
            create_response = self.post('/api/v1/customers', data=self.test_customer_data)
            self.assert_response_success(create_response, 201)
            customer_id = create_response.json()['id']
            
            try:
                # Get customer by ID
                response = self.get(f'/api/v1/customers/{customer_id}')
                self.assert_response_success(response, 200)
                
                # Verify customer data
                customer = response.json()
                assert customer['id'] == customer_id
                assert customer['name'] == self.test_customer_data['name']
                assert customer['email'] == self.test_customer_data['email']
            finally:
                # Cleanup
                self.delete(f'/api/v1/customers/{customer_id}')
    
    @pytest.mark.api
    @pytest.mark.integration
    def test_update_customer(self):
        """Test updating a customer"""
        with self.track_test_metrics('test_update_customer'):
            # Create customer
            create_response = self.post('/api/v1/customers', data=self.test_customer_data)
            customer_id = create_response.json()['id']
            
            try:
                # Update customer
                updated_data = {
                    'name': 'Updated Customer Name',
                    'email': 'updated@example.com'
                }
                update_response = self.put(
                    f'/api/v1/customers/{customer_id}',
                    data=updated_data
                )
                self.assert_response_success(update_response, 200)
                
                # Verify update
                get_response = self.get(f'/api/v1/customers/{customer_id}')
                updated_customer = get_response.json()
                assert updated_customer['name'] == updated_data['name']
                assert updated_customer['email'] == updated_data['email']
            finally:
                # Cleanup
                self.delete(f'/api/v1/customers/{customer_id}')
    
    @pytest.mark.api
    @pytest.mark.integration
    def test_delete_customer(self):
        """Test deleting a customer"""
        with self.track_test_metrics('test_delete_customer'):
            # Create customer
            create_response = self.post('/api/v1/customers', data=self.test_customer_data)
            customer_id = create_response.json()['id']
            
            # Delete customer
            delete_response = self.delete(f'/api/v1/customers/{customer_id}')
            self.assert_response_success(delete_response, 204)
            
            # Verify customer is deleted (should return 404)
            get_response = self.get(f'/api/v1/customers/{customer_id}')
            assert get_response.status_code == 404
    
    @pytest.mark.api
    @pytest.mark.integration
    def test_list_customers(self):
        """Test listing customers with pagination"""
        with self.track_test_metrics('test_list_customers'):
            # Create multiple customers
            customer_ids = []
            for i in range(3):
                data = self.test_customer_data.copy()
                data['email'] = f'test{i}@example.com'
                response = self.post('/api/v1/customers', data=data)
                customer_ids.append(response.json()['id'])
            
            try:
                # List customers
                response = self.get('/api/v1/customers', params={'page': 1, 'size': 10})
                self.assert_response_success(response, 200)
                
                # Verify response structure
                data = response.json()
                assert 'items' in data
                assert 'total' in data
                assert 'page' in data
                assert 'size' in data
                
                # Verify our test customers are in the list
                customer_emails = [c['email'] for c in data['items']]
                for i in range(3):
                    assert f'test{i}@example.com' in customer_emails
            finally:
                # Cleanup
                for customer_id in customer_ids:
                    self.delete(f'/api/v1/customers/{customer_id}')


class TestRetryLogic(BaseStagingApiTest):
    """Test suite for retry logic and error handling"""
    
    @pytest.mark.unit
    def test_retry_on_server_error(self):
        """Test that server errors trigger retry logic"""
        # This test would require mocking the API response
        # In a real scenario, you would use pytest-mock or responses library
        pass
    
    @pytest.mark.unit
    def test_no_retry_on_client_error(self):
        """Test that client errors do not trigger retry logic"""
        # This test would require mocking the API response
        pass
    
    @pytest.mark.unit
    def test_exponential_backoff(self):
        """Test that retry logic uses exponential backoff"""
        # This test would verify the timing between retries
        pass


class TestPerformanceMetrics(BaseStagingApiTest):
    """Test suite for performance metrics collection"""
    
    @pytest.mark.performance
    def test_metrics_collection(self):
        """Test that performance metrics are collected correctly"""
        with self.track_test_metrics('test_metrics_collection'):
            # Make multiple API calls
            for i in range(5):
                self.get('/health')
            
            # Verify metrics were collected
            assert self.current_test_metrics is not None
            assert self.current_test_metrics.api_calls == 5
            assert len(self.current_test_metrics.api_call_durations) == 5
    
    @pytest.mark.performance
    def test_metrics_report_generation(self):
        """Test that metrics report can be generated"""
        with self.track_test_metrics('test_metrics_report_generation'):
            self.get('/health')
        
        # Generate report
        report_path = self.save_metrics_report('test_report.json')
        assert os.path.exists(report_path)
        
        # Verify report content
        import json
        with open(report_path, 'r') as f:
            report = json.load(f)
        
        assert 'total_tests' in report
        assert 'successful_tests' in report
        assert 'tests' in report
        assert len(report['tests']) > 0


class TestAwsIntegration(BaseStagingIntegrationTest):
    """Test suite for AWS service integration"""
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    def test_cloudwatch_metric_publishing(self):
        """Test publishing custom metrics to CloudWatch"""
        with self.track_test_metrics('test_cloudwatch_metric_publishing'):
            # Publish a test metric
            self.publish_cloudwatch_metric(
                namespace='StagingTests',
                metric_name='TestMetric',
                value=100.0,
                unit='Count',
                dimensions=[
                    {'Name': 'Environment', 'Value': 'Staging'},
                    {'Name': 'TestSuite', 'Value': 'ExampleTests'}
                ]
            )
            
            # Note: In a real test, you would verify the metric was published
            # by querying CloudWatch after a short delay
    
    @pytest.mark.integration
    @pytest.mark.monitoring
    @pytest.mark.slow
    def test_cloudwatch_logs_query(self):
        """Test querying CloudWatch Logs Insights"""
        with self.track_test_metrics('test_cloudwatch_logs_query'):
            # Query recent logs
            results = self.query_cloudwatch_logs(
                log_group='/aws/staging/application',
                query='fields @timestamp, @message | sort @timestamp desc | limit 10'
            )
            
            # Verify results
            assert isinstance(results, list)
            # Note: Results may be empty if no logs exist


# Pytest configuration for this test module
def pytest_configure(config):
    """Configure pytest for this test module"""
    config.addinivalue_line(
        "markers", "example: Example tests demonstrating framework usage"
    )


if __name__ == '__main__':
    # Run tests with pytest
    pytest.main([__file__, '-v', '--tb=short'])
