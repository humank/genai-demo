"""
Test Data Builders using Faker

This module provides comprehensive test data builders for generating realistic
test data for customers, orders, products, and other domain entities.

Features:
- Realistic data generation using Faker
- Customizable data patterns
- Support for different locales
- Relationship management between entities
- Data consistency validation

Requirements: 12.12, 12.13
Implementation: Python using Faker library
"""

from faker import Faker
from typing import Dict, List, Optional, Any
from datetime import datetime, timedelta
from decimal import Decimal
import random
import uuid


class BaseDataBuilder:
    """Base class for all data builders with common functionality."""
    
    def __init__(self, locale: str = 'en_US'):
        """
        Initialize data builder with specified locale.
        
        Args:
            locale: Faker locale for data generation (default: en_US)
        """
        self.faker = Faker(locale)
        Faker.seed(0)  # For reproducible data in tests
    
    def generate_id(self, prefix: str = '') -> str:
        """Generate unique ID with optional prefix."""
        return f"{prefix}{uuid.uuid4().hex[:12]}" if prefix else uuid.uuid4().hex[:12]
    
    def random_date_between(self, start_date: datetime, end_date: datetime) -> datetime:
        """Generate random date between two dates."""
        time_between = end_date - start_date
        days_between = time_between.days
        random_days = random.randint(0, days_between)
        return start_date + timedelta(days=random_days)


class CustomerDataBuilder(BaseDataBuilder):
    """
    Builder for generating customer test data.
    
    Generates realistic customer data including:
    - Personal information
    - Contact details
    - Membership levels
    - Registration dates
    """
    
    def __init__(self, locale: str = 'en_US'):
        super().__init__(locale)
        self.membership_levels = ['STANDARD', 'SILVER', 'GOLD', 'PLATINUM']
        self.customer_statuses = ['ACTIVE', 'INACTIVE', 'SUSPENDED']
    
    def build(self, **overrides) -> Dict[str, Any]:
        """
        Build a customer data dictionary.
        
        Args:
            **overrides: Override specific fields
        
        Returns:
            Dictionary containing customer data
        """
        customer_data = {
            'id': self.generate_id('CUST-'),
            'name': self.faker.name(),
            'email': self.faker.email(),
            'phone': self.faker.phone_number(),
            'date_of_birth': self.faker.date_of_birth(minimum_age=18, maximum_age=80).isoformat(),
            'membership_level': random.choice(self.membership_levels),
            'status': 'ACTIVE',
            'registration_date': self.faker.date_time_between(start_date='-2y', end_date='now').isoformat(),
            'address': {
                'street': self.faker.street_address(),
                'city': self.faker.city(),
                'state': self.faker.state(),
                'postal_code': self.faker.postcode(),
                'country': self.faker.country_code()
            },
            'preferences': {
                'newsletter': random.choice([True, False]),
                'sms_notifications': random.choice([True, False]),
                'language': random.choice(['en', 'zh-TW', 'ja'])
            },
            'metadata': {
                'created_at': datetime.utcnow().isoformat(),
                'updated_at': datetime.utcnow().isoformat(),
                'version': 1
            }
        }
        
        # Apply overrides
        customer_data.update(overrides)
        
        return customer_data
    
    def build_batch(self, count: int, **overrides) -> List[Dict[str, Any]]:
        """
        Build multiple customer records.
        
        Args:
            count: Number of customers to generate
            **overrides: Override specific fields for all customers
        
        Returns:
            List of customer data dictionaries
        """
        return [self.build(**overrides) for _ in range(count)]
    
    def build_premium_customer(self, **overrides) -> Dict[str, Any]:
        """Build a premium customer with PLATINUM membership."""
        premium_overrides = {
            'membership_level': 'PLATINUM',
            'status': 'ACTIVE'
        }
        premium_overrides.update(overrides)
        return self.build(**premium_overrides)
    
    def build_inactive_customer(self, **overrides) -> Dict[str, Any]:
        """Build an inactive customer."""
        inactive_overrides = {
            'status': 'INACTIVE',
            'membership_level': 'STANDARD'
        }
        inactive_overrides.update(overrides)
        return self.build(**inactive_overrides)


class ProductDataBuilder(BaseDataBuilder):
    """
    Builder for generating product test data.
    
    Generates realistic product data including:
    - Product details
    - Pricing information
    - Inventory levels
    - Categories
    """
    
    def __init__(self, locale: str = 'en_US'):
        super().__init__(locale)
        self.categories = ['Electronics', 'Clothing', 'Food', 'Books', 'Home', 'Sports']
        self.product_statuses = ['AVAILABLE', 'OUT_OF_STOCK', 'DISCONTINUED']
    
    def build(self, **overrides) -> Dict[str, Any]:
        """
        Build a product data dictionary.
        
        Args:
            **overrides: Override specific fields
        
        Returns:
            Dictionary containing product data
        """
        price = Decimal(str(random.uniform(10.0, 1000.0))).quantize(Decimal('0.01'))
        
        product_data = {
            'id': self.generate_id('PROD-'),
            'name': self.faker.catch_phrase(),
            'description': self.faker.text(max_nb_chars=200),
            'sku': self.faker.ean13(),
            'category': random.choice(self.categories),
            'price': float(price),
            'currency': 'USD',
            'stock_quantity': random.randint(0, 1000),
            'status': 'AVAILABLE',
            'dimensions': {
                'weight': round(random.uniform(0.1, 50.0), 2),
                'length': round(random.uniform(5.0, 100.0), 2),
                'width': round(random.uniform(5.0, 100.0), 2),
                'height': round(random.uniform(5.0, 100.0), 2),
                'unit': 'cm'
            },
            'images': [
                self.faker.image_url(),
                self.faker.image_url()
            ],
            'tags': [self.faker.word() for _ in range(random.randint(2, 5))],
            'metadata': {
                'created_at': datetime.utcnow().isoformat(),
                'updated_at': datetime.utcnow().isoformat(),
                'version': 1
            }
        }
        
        # Apply overrides
        product_data.update(overrides)
        
        return product_data
    
    def build_batch(self, count: int, **overrides) -> List[Dict[str, Any]]:
        """Build multiple product records."""
        return [self.build(**overrides) for _ in range(count)]
    
    def build_out_of_stock_product(self, **overrides) -> Dict[str, Any]:
        """Build an out-of-stock product."""
        out_of_stock_overrides = {
            'stock_quantity': 0,
            'status': 'OUT_OF_STOCK'
        }
        out_of_stock_overrides.update(overrides)
        return self.build(**out_of_stock_overrides)


class OrderDataBuilder(BaseDataBuilder):
    """
    Builder for generating order test data.
    
    Generates realistic order data including:
    - Order details
    - Line items
    - Payment information
    - Shipping details
    """
    
    def __init__(self, locale: str = 'en_US'):
        super().__init__(locale)
        self.order_statuses = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED']
        self.payment_methods = ['CREDIT_CARD', 'DEBIT_CARD', 'PAYPAL', 'BANK_TRANSFER']
        self.shipping_methods = ['STANDARD', 'EXPRESS', 'OVERNIGHT']
    
    def build(self, customer_id: Optional[str] = None, products: Optional[List[Dict]] = None, **overrides) -> Dict[str, Any]:
        """
        Build an order data dictionary.
        
        Args:
            customer_id: Customer ID for the order
            products: List of products to include in order
            **overrides: Override specific fields
        
        Returns:
            Dictionary containing order data
        """
        # Generate order items
        if products is None:
            product_builder = ProductDataBuilder(self.faker.locale)
            products = product_builder.build_batch(random.randint(1, 5))
        
        order_items = []
        subtotal = Decimal('0.00')
        
        for product in products:
            quantity = random.randint(1, 5)
            unit_price = Decimal(str(product['price']))
            item_total = unit_price * quantity
            subtotal += item_total
            
            order_items.append({
                'product_id': product['id'],
                'product_name': product['name'],
                'quantity': quantity,
                'unit_price': float(unit_price),
                'total_price': float(item_total)
            })
        
        tax = subtotal * Decimal('0.08')  # 8% tax
        shipping = Decimal('10.00')
        total = subtotal + tax + shipping
        
        order_data = {
            'id': self.generate_id('ORD-'),
            'customer_id': customer_id or self.generate_id('CUST-'),
            'order_number': f"ORD-{datetime.utcnow().strftime('%Y%m%d')}-{random.randint(1000, 9999)}",
            'status': 'PENDING',
            'order_date': datetime.utcnow().isoformat(),
            'items': order_items,
            'pricing': {
                'subtotal': float(subtotal),
                'tax': float(tax),
                'shipping': float(shipping),
                'total': float(total),
                'currency': 'USD'
            },
            'payment': {
                'method': random.choice(self.payment_methods),
                'status': 'PENDING',
                'transaction_id': self.generate_id('TXN-')
            },
            'shipping': {
                'method': random.choice(self.shipping_methods),
                'address': {
                    'street': self.faker.street_address(),
                    'city': self.faker.city(),
                    'state': self.faker.state(),
                    'postal_code': self.faker.postcode(),
                    'country': self.faker.country_code()
                },
                'tracking_number': None,
                'estimated_delivery': (datetime.utcnow() + timedelta(days=random.randint(3, 7))).isoformat()
            },
            'metadata': {
                'created_at': datetime.utcnow().isoformat(),
                'updated_at': datetime.utcnow().isoformat(),
                'version': 1
            }
        }
        
        # Apply overrides
        order_data.update(overrides)
        
        return order_data
    
    def build_batch(self, count: int, customer_id: Optional[str] = None, **overrides) -> List[Dict[str, Any]]:
        """Build multiple order records."""
        return [self.build(customer_id=customer_id, **overrides) for _ in range(count)]
    
    def build_completed_order(self, customer_id: Optional[str] = None, **overrides) -> Dict[str, Any]:
        """Build a completed order."""
        completed_overrides = {
            'status': 'DELIVERED',
            'payment': {
                'method': 'CREDIT_CARD',
                'status': 'COMPLETED',
                'transaction_id': self.generate_id('TXN-')
            }
        }
        completed_overrides.update(overrides)
        return self.build(customer_id=customer_id, **completed_overrides)


class TestDataFactory:
    """
    Factory for creating related test data entities.
    
    Provides high-level methods for creating complete test scenarios
    with properly related entities.
    """
    
    def __init__(self, locale: str = 'en_US'):
        """Initialize factory with specified locale."""
        self.customer_builder = CustomerDataBuilder(locale)
        self.product_builder = ProductDataBuilder(locale)
        self.order_builder = OrderDataBuilder(locale)
    
    def create_customer_with_orders(self, order_count: int = 3) -> Dict[str, Any]:
        """
        Create a customer with multiple orders.
        
        Args:
            order_count: Number of orders to create
        
        Returns:
            Dictionary containing customer and their orders
        """
        customer = self.customer_builder.build()
        products = self.product_builder.build_batch(10)
        orders = self.order_builder.build_batch(
            order_count,
            customer_id=customer['id']
        )
        
        return {
            'customer': customer,
            'orders': orders,
            'products': products
        }
    
    def create_complete_order_scenario(self) -> Dict[str, Any]:
        """
        Create a complete order scenario with customer, products, and order.
        
        Returns:
            Dictionary containing all related entities
        """
        customer = self.customer_builder.build()
        products = self.product_builder.build_batch(5)
        order = self.order_builder.build(
            customer_id=customer['id'],
            products=products
        )
        
        return {
            'customer': customer,
            'products': products,
            'order': order
        }
    
    def create_bulk_test_data(self, customer_count: int = 100, product_count: int = 500) -> Dict[str, Any]:
        """
        Create bulk test data for performance testing.
        
        Args:
            customer_count: Number of customers to create
            product_count: Number of products to create
        
        Returns:
            Dictionary containing bulk test data
        """
        customers = self.customer_builder.build_batch(customer_count)
        products = self.product_builder.build_batch(product_count)
        
        # Create orders for random customers
        orders = []
        for _ in range(customer_count * 2):  # 2 orders per customer on average
            customer = random.choice(customers)
            order_products = random.sample(products, random.randint(1, 5))
            order = self.order_builder.build(
                customer_id=customer['id'],
                products=order_products
            )
            orders.append(order)
        
        return {
            'customers': customers,
            'products': products,
            'orders': orders
        }


# Convenience functions for quick data generation
def create_customer(**overrides) -> Dict[str, Any]:
    """Quick function to create a single customer."""
    return CustomerDataBuilder().build(**overrides)


def create_product(**overrides) -> Dict[str, Any]:
    """Quick function to create a single product."""
    return ProductDataBuilder().build(**overrides)


def create_order(customer_id: Optional[str] = None, **overrides) -> Dict[str, Any]:
    """Quick function to create a single order."""
    return OrderDataBuilder().build(customer_id=customer_id, **overrides)


def create_test_scenario() -> Dict[str, Any]:
    """Quick function to create a complete test scenario."""
    return TestDataFactory().create_complete_order_scenario()


if __name__ == "__main__":
    # Example usage
    factory = TestDataFactory()
    
    # Create single entities
    customer = create_customer()
    product = create_product()
    order = create_order(customer_id=customer['id'])
    
    print("Sample Customer:", customer)
    print("\nSample Product:", product)
    print("\nSample Order:", order)
    
    # Create complete scenario
    scenario = create_test_scenario()
    print("\nComplete Scenario:", scenario.keys())
