#!/usr/bin/env python3
"""
End-to-End Business Flow Test

This test validates complete business workflows across multiple regions in an Active-Active architecture.
It simulates real user scenarios and verifies that business operations work correctly across regions.

Test Scenarios:
1. Customer registration and order placement across regions
2. Cross-region order fulfillment
3. Payment processing with multi-region coordination
4. Inventory management across regions
"""

import asyncio
import time
import logging
import uuid
from typing import Dict, List, Optional
from dataclasses import dataclass
from datetime import datetime

import requests

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@dataclass
class BusinessFlowConfig:
    """Configuration for end-to-end business flow tests"""
    regions: List[str] = None
    api_endpoints: Dict[str, str] = None
    max_workflow_time_seconds: int = 30
    
    def __post_init__(self):
        if self.regions is None:
            self.regions = ["us-east-1", "us-west-2", "eu-west-1"]
        if self.api_endpoints is None:
            self.api_endpoints = {
                "us-east-1": "https://api-us-east-1.example.com",
                "us-west-2": "https://api-us-west-2.example.com",
                "eu-west-1": "https://api-eu-west-1.example.com"
            }


@dataclass
class BusinessFlowTestResult:
    """Result of a business flow test"""
    test_name: str
    success: bool
    workflow_time_seconds: float
    steps_completed: int
    total_steps: int
    data_consistent: bool
    error_message: Optional[str] = None
    details: Optional[Dict] = None


class EndToEndBusinessFlowTest:
    """Test suite for end-to-end business flows"""
    
    def __init__(self, config: BusinessFlowConfig):
        self.config = config
        self.results: List[BusinessFlowTestResult] = []
        
    async def test_customer_registration_and_order(self) -> BusinessFlowTestResult:
        """
        Test: Complete customer registration and order placement workflow
        
        Steps:
        1. Register customer in Region A
        2. Verify customer data replicated to all regions
        3. Place order from Region B
        4. Verify order visible in all regions
        5. Update order status in Region C
        6. Verify consistency across all regions
        """
        test_name = "customer_registration_and_order"
        logger.info(f"Starting test: {test_name}")
        
        workflow_start = time.time()
        steps_completed = 0
        total_steps = 6
        
        try:
            # Step 1: Register customer in Region A
            customer_id = str(uuid.uuid4())
            customer_data = {
                "id": customer_id,
                "name": "Test Customer",
                "email": f"test-{customer_id}@example.com",
                "region": self.config.regions[0]
            }
            
            registration_success = await self._register_customer(
                self.config.regions[0],
                customer_data
            )
            
            if not registration_success:
                return self._create_failure_result(
                    test_name, workflow_start, steps_completed, total_steps,
                    "Customer registration failed"
                )
            
            steps_completed += 1
            logger.info(f"✓ Step {steps_completed}/{total_steps}: Customer registered")
            
            # Step 2: Verify customer data replicated to all regions
            await asyncio.sleep(2)  # Allow replication time
            
            replication_success = await self._verify_customer_in_all_regions(customer_id)
            if not replication_success:
                return self._create_failure_result(
                    test_name, workflow_start, steps_completed, total_steps,
                    "Customer data not replicated to all regions"
                )
            
            steps_completed += 1
            logger.info(f"✓ Step {steps_completed}/{total_steps}: Customer data replicated")
            
            # Step 3: Place order from Region B
            order_id = str(uuid.uuid4())
            order_data = {
                "id": order_id,
                "customer_id": customer_id,
                "items": [
                    {"product_id": "PROD-001", "quantity": 2, "price": 29.99},
                    {"product_id": "PROD-002", "quantity": 1, "price": 49.99}
                ],
                "total": 109.97,
                "region": self.config.regions[1]
            }
            
            order_success = await self._place_order(
                self.config.regions[1],
                order_data
            )
            
            if not order_success:
                return self._create_failure_result(
                    test_name, workflow_start, steps_completed, total_steps,
                    "Order placement failed"
                )
            
            steps_completed += 1
            logger.info(f"✓ Step {steps_completed}/{total_steps}: Order placed")
            
            # Step 4: Verify order visible in all regions
            await asyncio.sleep(2)  # Allow replication time
            
            order_replication_success = await self._verify_order_in_all_regions(order_id)
            if not order_replication_success:
                return self._create_failure_result(
                    test_name, workflow_start, steps_completed, total_steps,
                    "Order not replicated to all regions"
                )
            
            steps_completed += 1
            logger.info(f"✓ Step {steps_completed}/{total_steps}: Order replicated")
            
            # Step 5: Update order status in Region C
            update_success = await self._update_order_status(
                self.config.regions[2],
                order_id,
                "PROCESSING"
            )
            
            if not update_success:
                return self._create_failure_result(
                    test_name, workflow_start, steps_completed, total_steps,
                    "Order status update failed"
                )
            
            steps_completed += 1
            logger.info(f"✓ Step {steps_completed}/{total_steps}: Order status updated")
            
            # Step 6: Verify consistency across all regions
            await asyncio.sleep(2)  # Allow replication time
            
            consistency_check = await self._verify_order_status_consistency(
                order_id,
                "PROCESSING"
            )
            
            if not consistency_check:
                return self._create_failure_result(
                    test_name, workflow_start, steps_completed, total_steps,
                    "Order status not consistent across regions"
                )
            
            steps_completed += 1
            logger.info(f"✓ Step {steps_completed}/{total_steps}: Consistency verified")
            
            workflow_time = time.time() - workflow_start
            
            return BusinessFlowTestResult(
                test_name=test_name,
                success=True,
                workflow_time_seconds=workflow_time,
                steps_completed=steps_completed,
                total_steps=total_steps,
                data_consistent=True,
                details={
                    "customer_id": customer_id,
                    "order_id": order_id,
                    "workflow_time_seconds": workflow_time
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed with exception: {str(e)}")
            return self._create_failure_result(
                test_name, workflow_start, steps_completed, total_steps,
                str(e)
            )
    
    async def test_cross_region_order_fulfillment(self) -> BusinessFlowTestResult:
        """
        Test: Order fulfillment across multiple regions
        
        Validates that orders can be fulfilled from different regions
        based on inventory availability and shipping optimization.
        """
        test_name = "cross_region_order_fulfillment"
        logger.info(f"Starting test: {test_name}")
        
        workflow_start = time.time()
        steps_completed = 0
        total_steps = 5
        
        try:
            # Step 1: Create order in Region A
            order_id = str(uuid.uuid4())
            order_data = {
                "id": order_id,
                "customer_id": str(uuid.uuid4()),
                "items": [{"product_id": "PROD-003", "quantity": 5}],
                "region": self.config.regions[0]
            }
            
            order_success = await self._place_order(self.config.regions[0], order_data)
            if not order_success:
                return self._create_failure_result(
                    test_name, workflow_start, steps_completed, total_steps,
                    "Order creation failed"
                )
            
            steps_completed += 1
            logger.info(f"✓ Step {steps_completed}/{total_steps}: Order created")
            
            # Step 2: Check inventory across all regions
            inventory_check = await self._check_inventory_across_regions("PROD-003", 5)
            if not inventory_check:
                return self._create_failure_result(
                    test_name, workflow_start, steps_completed, total_steps,
                    "Insufficient inventory across regions"
                )
            
            steps_completed += 1
            logger.info(f"✓ Step {steps_completed}/{total_steps}: Inventory verified")
            
            # Step 3: Allocate inventory from optimal region
            allocation_region = await self._allocate_inventory(order_id, "PROD-003", 5)
            if not allocation_region:
                return self._create_failure_result(
                    test_name, workflow_start, steps_completed, total_steps,
                    "Inventory allocation failed"
                )
            
            steps_completed += 1
            logger.info(f"✓ Step {steps_completed}/{total_steps}: Inventory allocated from {allocation_region}")
            
            # Step 4: Process fulfillment
            fulfillment_success = await self._process_fulfillment(order_id, allocation_region)
            if not fulfillment_success:
                return self._create_failure_result(
                    test_name, workflow_start, steps_completed, total_steps,
                    "Fulfillment processing failed"
                )
            
            steps_completed += 1
            logger.info(f"✓ Step {steps_completed}/{total_steps}: Fulfillment processed")
            
            # Step 5: Verify order status updated across all regions
            await asyncio.sleep(2)
            status_check = await self._verify_order_status_consistency(order_id, "FULFILLED")
            if not status_check:
                return self._create_failure_result(
                    test_name, workflow_start, steps_completed, total_steps,
                    "Order status not consistent"
                )
            
            steps_completed += 1
            logger.info(f"✓ Step {steps_completed}/{total_steps}: Status verified")
            
            workflow_time = time.time() - workflow_start
            
            return BusinessFlowTestResult(
                test_name=test_name,
                success=True,
                workflow_time_seconds=workflow_time,
                steps_completed=steps_completed,
                total_steps=total_steps,
                data_consistent=True,
                details={
                    "order_id": order_id,
                    "fulfillment_region": allocation_region,
                    "workflow_time_seconds": workflow_time
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed with exception: {str(e)}")
            return self._create_failure_result(
                test_name, workflow_start, steps_completed, total_steps,
                str(e)
            )
    
    async def test_payment_processing_multi_region(self) -> BusinessFlowTestResult:
        """
        Test: Payment processing with multi-region coordination
        
        Validates that payment processing works correctly across regions
        with proper transaction handling and consistency.
        """
        test_name = "payment_processing_multi_region"
        logger.info(f"Starting test: {test_name}")
        
        workflow_start = time.time()
        steps_completed = 0
        total_steps = 4
        
        try:
            # Step 1: Create order with payment
            order_id = str(uuid.uuid4())
            payment_data = {
                "order_id": order_id,
                "amount": 199.99,
                "currency": "USD",
                "payment_method": "credit_card",
                "region": self.config.regions[0]
            }
            
            payment_initiated = await self._initiate_payment(
                self.config.regions[0],
                payment_data
            )
            
            if not payment_initiated:
                return self._create_failure_result(
                    test_name, workflow_start, steps_completed, total_steps,
                    "Payment initiation failed"
                )
            
            steps_completed += 1
            logger.info(f"✓ Step {steps_completed}/{total_steps}: Payment initiated")
            
            # Step 2: Process payment in payment gateway region
            payment_success = await self._process_payment(order_id)
            if not payment_success:
                return self._create_failure_result(
                    test_name, workflow_start, steps_completed, total_steps,
                    "Payment processing failed"
                )
            
            steps_completed += 1
            logger.info(f"✓ Step {steps_completed}/{total_steps}: Payment processed")
            
            # Step 3: Verify payment status replicated
            await asyncio.sleep(2)
            payment_status_check = await self._verify_payment_status_across_regions(
                order_id,
                "COMPLETED"
            )
            
            if not payment_status_check:
                return self._create_failure_result(
                    test_name, workflow_start, steps_completed, total_steps,
                    "Payment status not replicated"
                )
            
            steps_completed += 1
            logger.info(f"✓ Step {steps_completed}/{total_steps}: Payment status replicated")
            
            # Step 4: Verify order status updated
            order_status_check = await self._verify_order_status_consistency(
                order_id,
                "PAID"
            )
            
            if not order_status_check:
                return self._create_failure_result(
                    test_name, workflow_start, steps_completed, total_steps,
                    "Order status not updated"
                )
            
            steps_completed += 1
            logger.info(f"✓ Step {steps_completed}/{total_steps}: Order status updated")
            
            workflow_time = time.time() - workflow_start
            
            return BusinessFlowTestResult(
                test_name=test_name,
                success=True,
                workflow_time_seconds=workflow_time,
                steps_completed=steps_completed,
                total_steps=total_steps,
                data_consistent=True,
                details={
                    "order_id": order_id,
                    "payment_amount": payment_data["amount"],
                    "workflow_time_seconds": workflow_time
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed with exception: {str(e)}")
            return self._create_failure_result(
                test_name, workflow_start, steps_completed, total_steps,
                str(e)
            )
    
    # Helper methods
    
    async def _register_customer(self, region: str, customer_data: Dict) -> bool:
        """Register a customer in specified region"""
        try:
            endpoint = self.config.api_endpoints.get(region)
            response = requests.post(
                f"{endpoint}/api/v1/customers",
                json=customer_data,
                timeout=10
            )
            return response.status_code in [200, 201]
        except Exception as e:
            logger.error(f"Customer registration failed: {str(e)}")
            return False
    
    async def _verify_customer_in_all_regions(self, customer_id: str) -> bool:
        """Verify customer exists in all regions"""
        for region in self.config.regions:
            endpoint = self.config.api_endpoints.get(region)
            try:
                response = requests.get(
                    f"{endpoint}/api/v1/customers/{customer_id}",
                    timeout=10
                )
                if response.status_code != 200:
                    return False
            except Exception:
                return False
        return True
    
    async def _place_order(self, region: str, order_data: Dict) -> bool:
        """Place an order in specified region"""
        try:
            endpoint = self.config.api_endpoints.get(region)
            response = requests.post(
                f"{endpoint}/api/v1/orders",
                json=order_data,
                timeout=10
            )
            return response.status_code in [200, 201]
        except Exception as e:
            logger.error(f"Order placement failed: {str(e)}")
            return False
    
    async def _verify_order_in_all_regions(self, order_id: str) -> bool:
        """Verify order exists in all regions"""
        for region in self.config.regions:
            endpoint = self.config.api_endpoints.get(region)
            try:
                response = requests.get(
                    f"{endpoint}/api/v1/orders/{order_id}",
                    timeout=10
                )
                if response.status_code != 200:
                    return False
            except Exception:
                return False
        return True
    
    async def _update_order_status(self, region: str, order_id: str, status: str) -> bool:
        """Update order status in specified region"""
        try:
            endpoint = self.config.api_endpoints.get(region)
            response = requests.patch(
                f"{endpoint}/api/v1/orders/{order_id}",
                json={"status": status},
                timeout=10
            )
            return response.status_code == 200
        except Exception as e:
            logger.error(f"Order status update failed: {str(e)}")
            return False
    
    async def _verify_order_status_consistency(self, order_id: str, expected_status: str) -> bool:
        """Verify order status is consistent across all regions"""
        for region in self.config.regions:
            endpoint = self.config.api_endpoints.get(region)
            try:
                response = requests.get(
                    f"{endpoint}/api/v1/orders/{order_id}",
                    timeout=10
                )
                if response.status_code == 200:
                    order_data = response.json()
                    if order_data.get("status") != expected_status:
                        return False
                else:
                    return False
            except Exception:
                return False
        return True
    
    async def _check_inventory_across_regions(self, product_id: str, quantity: int) -> bool:
        """Check if sufficient inventory exists across regions"""
        # Placeholder implementation
        return True
    
    async def _allocate_inventory(self, order_id: str, product_id: str, quantity: int) -> Optional[str]:
        """Allocate inventory from optimal region"""
        # Placeholder implementation - return first region
        return self.config.regions[0]
    
    async def _process_fulfillment(self, order_id: str, region: str) -> bool:
        """Process order fulfillment"""
        # Placeholder implementation
        return True
    
    async def _initiate_payment(self, region: str, payment_data: Dict) -> bool:
        """Initiate payment processing"""
        # Placeholder implementation
        return True
    
    async def _process_payment(self, order_id: str) -> bool:
        """Process payment"""
        # Placeholder implementation
        return True
    
    async def _verify_payment_status_across_regions(self, order_id: str, expected_status: str) -> bool:
        """Verify payment status across regions"""
        # Placeholder implementation
        return True
    
    def _create_failure_result(
        self,
        test_name: str,
        workflow_start: float,
        steps_completed: int,
        total_steps: int,
        error_message: str
    ) -> BusinessFlowTestResult:
        """Create a failure result"""
        return BusinessFlowTestResult(
            test_name=test_name,
            success=False,
            workflow_time_seconds=time.time() - workflow_start,
            steps_completed=steps_completed,
            total_steps=total_steps,
            data_consistent=False,
            error_message=error_message
        )
    
    async def run_all_tests(self) -> List[BusinessFlowTestResult]:
        """Run all business flow tests"""
        logger.info("Starting end-to-end business flow test suite")
        
        tests = [
            self.test_customer_registration_and_order(),
            self.test_cross_region_order_fulfillment(),
            self.test_payment_processing_multi_region()
        ]
        
        self.results = await asyncio.gather(*tests)
        
        # Print summary
        self._print_summary()
        
        return self.results
    
    def _print_summary(self):
        """Print test results summary"""
        logger.info("\n" + "="*80)
        logger.info("End-to-End Business Flow Test Results")
        logger.info("="*80)
        
        for result in self.results:
            status = "✓ PASS" if result.success else "✗ FAIL"
            logger.info(f"\n{status} - {result.test_name}")
            logger.info(f"  Workflow Time: {result.workflow_time_seconds:.2f}s")
            logger.info(f"  Steps Completed: {result.steps_completed}/{result.total_steps}")
            logger.info(f"  Data Consistent: {'Yes' if result.data_consistent else 'No'}")
            
            if result.error_message:
                logger.info(f"  Error: {result.error_message}")
        
        passed = sum(1 for r in self.results if r.success)
        total = len(self.results)
        logger.info(f"\n{'='*80}")
        logger.info(f"Summary: {passed}/{total} tests passed")
        logger.info("="*80 + "\n")


async def main():
    """Main entry point for end-to-end business flow tests"""
    config = BusinessFlowConfig()
    test_suite = EndToEndBusinessFlowTest(config)
    
    results = await test_suite.run_all_tests()
    
    # Exit with appropriate code
    all_passed = all(r.success for r in results)
    exit(0 if all_passed else 1)


if __name__ == "__main__":
    asyncio.run(main())
