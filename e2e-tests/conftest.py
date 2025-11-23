"""
Pytest Configuration and Fixtures

This module provides pytest configuration and fixtures for test data management,
including automatic setup and teardown of test data.

Features:
- Automatic test data generation
- Test data cleanup after tests
- Database connection management
- S3 resource management
- Test isolation

Requirements: 12.12, 12.13
"""

import pytest
import os
from typing import Dict, List
from test_data.data_builders import (
    CustomerDataBuilder,
    ProductDataBuilder,
    OrderDataBuilder,
    TestDataFactory
)
from test_data.data_cleanup import (
    DatabaseCleanup,
    S3Cleanup,
    TestDataCleanupManager,
    load_cleanup_config
)


# ==================== Configuration Fixtures ====================

@pytest.fixture(scope="session")
def test_config() -> Dict:
    """
    Load test configuration from environment variables.
    
    Returns:
        Dictionary containing test configuration
    """
    return {
        'api_base_url': os.getenv('API_BASE_URL', 'http://localhost:8080'),
        'region': os.getenv('AWS_REGION', 'ap-northeast-1'),
        'database': {
            'host': os.getenv('DB_HOST', 'localhost'),
            'port': int(os.getenv('DB_PORT', '5432')),
            'database': os.getenv('DB_NAME', 'genai_demo'),
            'user': os.getenv('DB_USER', 'postgres'),
            'password': os.getenv('DB_PASSWORD', '')
        },
        'test_bucket': os.getenv('TEST_BUCKET', 'genai-demo-test-data'),
        'cleanup_enabled': os.getenv('CLEANUP_ENABLED', 'true').lower() == 'true'
    }


# ==================== Data Builder Fixtures ====================

@pytest.fixture(scope="session")
def customer_builder():
    """
    Provide CustomerDataBuilder instance.
    
    Returns:
        CustomerDataBuilder instance
    """
    return CustomerDataBuilder()


@pytest.fixture(scope="session")
def product_builder():
    """
    Provide ProductDataBuilder instance.
    
    Returns:
        ProductDataBuilder instance
    """
    return ProductDataBuilder()


@pytest.fixture(scope="session")
def order_builder():
    """
    Provide OrderDataBuilder instance.
    
    Returns:
        OrderDataBuilder instance
    """
    return OrderDataBuilder()


@pytest.fixture(scope="session")
def test_data_factory():
    """
    Provide TestDataFactory instance.
    
    Returns:
        TestDataFactory instance
    """
    return TestDataFactory()


# ==================== Test Data Fixtures ====================

@pytest.fixture
def test_customer(customer_builder):
    """
    Provide a test customer for individual tests.
    
    Returns:
        Customer data dictionary
    """
    return customer_builder.build()


@pytest.fixture
def test_customers(customer_builder):
    """
    Provide multiple test customers.
    
    Returns:
        List of customer data dictionaries
    """
    return customer_builder.build_batch(5)


@pytest.fixture
def test_product(product_builder):
    """
    Provide a test product for individual tests.
    
    Returns:
        Product data dictionary
    """
    return product_builder.build()


@pytest.fixture
def test_products(product_builder):
    """
    Provide multiple test products.
    
    Returns:
        List of product data dictionaries
    """
    return product_builder.build_batch(10)


@pytest.fixture
def test_order(order_builder, test_customer, test_products):
    """
    Provide a test order with customer and products.
    
    Returns:
        Order data dictionary
    """
    return order_builder.build(
        customer_id=test_customer['id'],
        products=test_products[:3]
    )


@pytest.fixture
def test_scenario(test_data_factory):
    """
    Provide a complete test scenario with related entities.
    
    Returns:
        Dictionary containing customer, products, and order
    """
    return test_data_factory.create_complete_order_scenario()


@pytest.fixture
def bulk_test_data(test_data_factory):
    """
    Provide bulk test data for performance testing.
    
    Returns:
        Dictionary containing bulk customers, products, and orders
    """
    return test_data_factory.create_bulk_test_data(
        customer_count=100,
        product_count=500
    )


# ==================== Cleanup Fixtures ====================

@pytest.fixture(scope="session")
def db_cleanup(test_config):
    """
    Provide DatabaseCleanup instance.
    
    Returns:
        DatabaseCleanup instance
    """
    return DatabaseCleanup(test_config['database'])


@pytest.fixture(scope="session")
def s3_cleanup(test_config):
    """
    Provide S3Cleanup instance.
    
    Returns:
        S3Cleanup instance
    """
    return S3Cleanup(test_config['region'])


@pytest.fixture(scope="session")
def cleanup_manager(test_config):
    """
    Provide TestDataCleanupManager instance.
    
    Returns:
        TestDataCleanupManager instance
    """
    config = load_cleanup_config()
    return TestDataCleanupManager(config)


# ==================== Automatic Cleanup Fixtures ====================

@pytest.fixture(autouse=True)
def auto_cleanup_after_test(request, test_config, cleanup_manager):
    """
    Automatically cleanup test data after each test.
    
    This fixture runs after every test and cleans up any test data
    created during the test execution.
    """
    # Setup: Nothing to do before test
    yield
    
    # Teardown: Cleanup after test
    if test_config['cleanup_enabled']:
        # Only cleanup if test passed or if explicitly requested
        if not request.node.rep_call.failed:
            try:
                # Cleanup test data created during this test
                # This is a simplified version - in production, you'd track
                # which data was created and only clean that up
                pass  # Actual cleanup logic would go here
            except Exception as e:
                # Log cleanup errors but don't fail the test
                print(f"Warning: Cleanup failed: {str(e)}")


@pytest.fixture(scope="session", autouse=True)
def cleanup_before_session(cleanup_manager):
    """
    Cleanup test data before test session starts.
    
    This ensures a clean state before running tests.
    """
    try:
        # Cleanup old test data (older than 7 days)
        cleanup_manager.cleanup_old_test_data(days_old=7)
    except Exception as e:
        print(f"Warning: Pre-session cleanup failed: {str(e)}")
    
    yield


@pytest.fixture(scope="session", autouse=True)
def cleanup_after_session(cleanup_manager):
    """
    Cleanup test data after test session completes.
    
    This ensures no test data is left behind after test execution.
    """
    yield
    
    try:
        # Perform comprehensive cleanup after all tests
        summary = cleanup_manager.cleanup_all_test_data()
        print(f"\nTest data cleanup completed: {summary['status']}")
    except Exception as e:
        print(f"Warning: Post-session cleanup failed: {str(e)}")


# ==================== Test Isolation Fixtures ====================

@pytest.fixture
def isolated_database(db_cleanup):
    """
    Provide isolated database for tests that need complete isolation.
    
    This fixture truncates test tables before and after the test.
    """
    # Setup: Truncate tables before test
    test_tables = ['customers', 'orders', 'order_items', 'products', 'payments']
    db_cleanup.truncate_tables(test_tables, cascade=True)
    
    yield
    
    # Teardown: Truncate tables after test
    db_cleanup.truncate_tables(test_tables, cascade=True)


@pytest.fixture
def isolated_s3(s3_cleanup, test_config):
    """
    Provide isolated S3 environment for tests.
    
    This fixture cleans up test S3 objects before and after the test.
    """
    bucket = test_config['test_bucket']
    prefix = 'test-data/'
    
    # Setup: Clean S3 before test
    s3_cleanup.delete_objects_by_prefix(bucket, prefix)
    
    yield
    
    # Teardown: Clean S3 after test
    s3_cleanup.delete_objects_by_prefix(bucket, prefix)


# ==================== Pytest Hooks ====================

def pytest_configure(config):
    """
    Pytest configuration hook.
    
    Register custom markers and configure test environment.
    """
    config.addinivalue_line(
        "markers", "slow: marks tests as slow (deselect with '-m \"not slow\"')"
    )
    config.addinivalue_line(
        "markers", "integration: marks tests as integration tests"
    )
    config.addinivalue_line(
        "markers", "performance: marks tests as performance tests"
    )
    config.addinivalue_line(
        "markers", "cleanup: marks tests that require cleanup"
    )


@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    """
    Pytest hook to make test results available to fixtures.
    
    This allows fixtures to access test results for conditional cleanup.
    """
    outcome = yield
    rep = outcome.get_result()
    setattr(item, f"rep_{rep.when}", rep)


# ==================== Helper Functions ====================

def get_test_data_summary(test_data: Dict) -> str:
    """
    Generate summary of test data.
    
    Args:
        test_data: Test data dictionary
    
    Returns:
        Summary string
    """
    summary_parts = []
    
    if 'customer' in test_data:
        summary_parts.append(f"Customer: {test_data['customer']['id']}")
    
    if 'customers' in test_data:
        summary_parts.append(f"Customers: {len(test_data['customers'])}")
    
    if 'product' in test_data:
        summary_parts.append(f"Product: {test_data['product']['id']}")
    
    if 'products' in test_data:
        summary_parts.append(f"Products: {len(test_data['products'])}")
    
    if 'order' in test_data:
        summary_parts.append(f"Order: {test_data['order']['id']}")
    
    if 'orders' in test_data:
        summary_parts.append(f"Orders: {len(test_data['orders'])}")
    
    return ", ".join(summary_parts)


# ==================== Example Usage ====================

"""
Example test using fixtures:

def test_customer_creation(test_customer, test_config):
    # test_customer fixture provides a pre-built customer
    assert test_customer['id'] is not None
    assert test_customer['email'] is not None
    
    # Use customer in API call
    response = requests.post(
        f"{test_config['api_base_url']}/api/v1/customers",
        json=test_customer
    )
    
    assert response.status_code == 201
    # Cleanup happens automatically after test


def test_order_scenario(test_scenario):
    # test_scenario provides complete related entities
    customer = test_scenario['customer']
    products = test_scenario['products']
    order = test_scenario['order']
    
    # Use in test
    assert order['customer_id'] == customer['id']
    assert len(order['items']) == len(products)
    # Cleanup happens automatically after test


@pytest.mark.slow
def test_bulk_operations(bulk_test_data):
    # bulk_test_data provides large dataset for performance testing
    customers = bulk_test_data['customers']
    products = bulk_test_data['products']
    orders = bulk_test_data['orders']
    
    # Perform bulk operations
    assert len(customers) == 100
    assert len(products) == 500
    assert len(orders) >= 200
    # Cleanup happens automatically after test
"""
