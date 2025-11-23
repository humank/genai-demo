#!/usr/bin/env python3
"""
Load Balancing Test

This test validates load balancing mechanisms across multiple regions in an Active-Active architecture.
It verifies that traffic is properly distributed based on geographic location, region health, and capacity.

Test Scenarios:
1. Geographic routing - users routed to nearest region
2. Weighted routing - traffic distribution based on capacity
3. Health-based routing - traffic avoids unhealthy regions
4. Capacity-based routing - traffic shifts when region reaches capacity
"""

import asyncio
import time
import logging
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass
from collections import defaultdict

import boto3
import requests
from concurrent.futures import ThreadPoolExecutor

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@dataclass
class LoadBalancingConfig:
    """Configuration for load balancing tests"""
    regions: List[str] = None
    global_endpoint: str = "https://api.example.com"
    target_distribution: Dict[str, float] = None  # Expected traffic percentage per region
    max_latency_ms: int = 200  # P95 target
    concurrent_users: int = 1000
    test_duration_seconds: int = 60
    
    def __post_init__(self):
        if self.regions is None:
            self.regions = ["us-east-1", "us-west-2", "eu-west-1"]
        if self.target_distribution is None:
            # Equal distribution by default
            self.target_distribution = {region: 100.0 / len(self.regions) for region in self.regions}


@dataclass
class LoadBalancingTestResult:
    """Result of a load balancing test"""
    test_name: str
    success: bool
    actual_distribution: Dict[str, float]
    avg_latency_ms: float
    p95_latency_ms: float
    error_rate: float
    error_message: Optional[str] = None
    details: Optional[Dict] = None


class LoadBalancingTest:
    """Test suite for load balancing"""
    
    def __init__(self, config: LoadBalancingConfig):
        self.config = config
        self.results: List[LoadBalancingTestResult] = []
        self.route53_client = boto3.client('route53')
        self.cloudwatch_client = boto3.client('cloudwatch')
        
    async def test_geographic_routing(self) -> LoadBalancingTestResult:
        """
        Test: Geographic routing to nearest region
        
        Validates that users are routed to their geographically nearest region
        for optimal latency.
        """
        test_name = "geographic_routing"
        logger.info(f"Starting test: {test_name}")
        
        try:
            # Simulate requests from different geographic locations
            test_locations = [
                ("us-east", "us-east-1"),
                ("us-west", "us-west-2"),
                ("europe", "eu-west-1")
            ]
            
            routing_results = []
            latencies = []
            
            for location, expected_region in test_locations:
                # Simulate 100 requests from each location
                for _ in range(100):
                    start_time = time.time()
                    actual_region, success = await self._make_request_from_location(location)
                    latency = (time.time() - start_time) * 1000
                    
                    routing_results.append((location, expected_region, actual_region, success))
                    if success:
                        latencies.append(latency)
            
            # Calculate routing accuracy
            correct_routes = sum(1 for _, expected, actual, success in routing_results 
                               if success and expected == actual)
            total_requests = len(routing_results)
            routing_accuracy = (correct_routes / total_requests) * 100
            
            # Calculate latency metrics
            avg_latency = sum(latencies) / len(latencies) if latencies else 0
            p95_latency = sorted(latencies)[int(len(latencies) * 0.95)] if latencies else 0
            
            # Success criteria: >95% routing accuracy and P95 latency < 200ms
            success = routing_accuracy >= 95.0 and p95_latency <= self.config.max_latency_ms
            
            return LoadBalancingTestResult(
                test_name=test_name,
                success=success,
                actual_distribution=self._calculate_distribution(routing_results),
                avg_latency_ms=avg_latency,
                p95_latency_ms=p95_latency,
                error_rate=(total_requests - len(latencies)) / total_requests * 100,
                details={
                    "routing_accuracy": routing_accuracy,
                    "total_requests": total_requests,
                    "correct_routes": correct_routes,
                    "test_locations": len(test_locations)
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed with exception: {str(e)}")
            return LoadBalancingTestResult(
                test_name=test_name,
                success=False,
                actual_distribution={},
                avg_latency_ms=0,
                p95_latency_ms=0,
                error_rate=100.0,
                error_message=str(e)
            )
    
    async def test_weighted_routing(self) -> LoadBalancingTestResult:
        """
        Test: Weighted routing based on region capacity
        
        Validates that traffic is distributed according to configured weights
        (e.g., 40% to us-east-1, 30% to us-west-2, 30% to eu-west-1).
        """
        test_name = "weighted_routing"
        logger.info(f"Starting test: {test_name}")
        
        try:
            # Configure weighted routing
            weights = {
                "us-east-1": 40.0,
                "us-west-2": 30.0,
                "eu-west-1": 30.0
            }
            
            await self._configure_weighted_routing(weights)
            
            # Generate load
            num_requests = 1000
            region_counts = defaultdict(int)
            latencies = []
            errors = 0
            
            for _ in range(num_requests):
                start_time = time.time()
                region, success = await self._make_request()
                latency = (time.time() - start_time) * 1000
                
                if success and region:
                    region_counts[region] += 1
                    latencies.append(latency)
                else:
                    errors += 1
            
            # Calculate actual distribution
            actual_distribution = {
                region: (count / num_requests) * 100
                for region, count in region_counts.items()
            }
            
            # Check if distribution matches weights (within 5% tolerance)
            distribution_match = all(
                abs(actual_distribution.get(region, 0) - weight) <= 5.0
                for region, weight in weights.items()
            )
            
            # Calculate latency metrics
            avg_latency = sum(latencies) / len(latencies) if latencies else 0
            p95_latency = sorted(latencies)[int(len(latencies) * 0.95)] if latencies else 0
            error_rate = (errors / num_requests) * 100
            
            success = distribution_match and error_rate < 1.0
            
            return LoadBalancingTestResult(
                test_name=test_name,
                success=success,
                actual_distribution=actual_distribution,
                avg_latency_ms=avg_latency,
                p95_latency_ms=p95_latency,
                error_rate=error_rate,
                details={
                    "target_weights": weights,
                    "distribution_match": distribution_match,
                    "total_requests": num_requests
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed with exception: {str(e)}")
            return LoadBalancingTestResult(
                test_name=test_name,
                success=False,
                actual_distribution={},
                avg_latency_ms=0,
                p95_latency_ms=0,
                error_rate=100.0,
                error_message=str(e)
            )
    
    async def test_health_based_routing(self) -> LoadBalancingTestResult:
        """
        Test: Health-based routing avoids unhealthy regions
        
        Validates that traffic is automatically routed away from unhealthy regions
        and redistributed to healthy regions.
        """
        test_name = "health_based_routing"
        logger.info(f"Starting test: {test_name}")
        
        try:
            # Mark one region as unhealthy
            unhealthy_region = "us-east-1"
            await self._mark_region_unhealthy(unhealthy_region)
            
            # Wait for health check propagation
            await asyncio.sleep(30)
            
            # Generate load
            num_requests = 500
            region_counts = defaultdict(int)
            latencies = []
            errors = 0
            
            for _ in range(num_requests):
                start_time = time.time()
                region, success = await self._make_request()
                latency = (time.time() - start_time) * 1000
                
                if success and region:
                    region_counts[region] += 1
                    latencies.append(latency)
                else:
                    errors += 1
            
            # Verify no traffic went to unhealthy region
            unhealthy_traffic = region_counts.get(unhealthy_region, 0)
            traffic_avoided = unhealthy_traffic == 0
            
            # Calculate distribution among healthy regions
            healthy_regions = [r for r in self.config.regions if r != unhealthy_region]
            actual_distribution = {
                region: (region_counts.get(region, 0) / num_requests) * 100
                for region in healthy_regions
            }
            
            # Calculate latency metrics
            avg_latency = sum(latencies) / len(latencies) if latencies else 0
            p95_latency = sorted(latencies)[int(len(latencies) * 0.95)] if latencies else 0
            error_rate = (errors / num_requests) * 100
            
            success = traffic_avoided and error_rate < 1.0
            
            return LoadBalancingTestResult(
                test_name=test_name,
                success=success,
                actual_distribution=actual_distribution,
                avg_latency_ms=avg_latency,
                p95_latency_ms=p95_latency,
                error_rate=error_rate,
                details={
                    "unhealthy_region": unhealthy_region,
                    "traffic_to_unhealthy": unhealthy_traffic,
                    "traffic_avoided": traffic_avoided,
                    "healthy_regions": healthy_regions
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed with exception: {str(e)}")
            return LoadBalancingTestResult(
                test_name=test_name,
                success=False,
                actual_distribution={},
                avg_latency_ms=0,
                p95_latency_ms=0,
                error_rate=100.0,
                error_message=str(e)
            )
        finally:
            # Restore region health
            await self._mark_region_healthy(unhealthy_region)
    
    async def test_capacity_based_routing(self) -> LoadBalancingTestResult:
        """
        Test: Capacity-based routing shifts traffic when region reaches capacity
        
        Validates that when a region reaches its capacity threshold (e.g., 80% CPU),
        new traffic is routed to other regions.
        """
        test_name = "capacity_based_routing"
        logger.info(f"Starting test: {test_name}")
        
        try:
            # Simulate high load on one region
            overloaded_region = "us-east-1"
            await self._simulate_high_load(overloaded_region)
            
            # Wait for capacity monitoring to detect overload
            await asyncio.sleep(30)
            
            # Generate additional load
            num_requests = 500
            region_counts = defaultdict(int)
            latencies = []
            errors = 0
            
            for _ in range(num_requests):
                start_time = time.time()
                region, success = await self._make_request()
                latency = (time.time() - start_time) * 1000
                
                if success and region:
                    region_counts[region] += 1
                    latencies.append(latency)
                else:
                    errors += 1
            
            # Calculate distribution
            actual_distribution = {
                region: (region_counts.get(region, 0) / num_requests) * 100
                for region in self.config.regions
            }
            
            # Verify reduced traffic to overloaded region
            overloaded_traffic = actual_distribution.get(overloaded_region, 0)
            traffic_shifted = overloaded_traffic < 20.0  # Less than 20% traffic
            
            # Calculate latency metrics
            avg_latency = sum(latencies) / len(latencies) if latencies else 0
            p95_latency = sorted(latencies)[int(len(latencies) * 0.95)] if latencies else 0
            error_rate = (errors / num_requests) * 100
            
            # Success: traffic shifted away and latency maintained
            success = traffic_shifted and p95_latency <= self.config.max_latency_ms
            
            return LoadBalancingTestResult(
                test_name=test_name,
                success=success,
                actual_distribution=actual_distribution,
                avg_latency_ms=avg_latency,
                p95_latency_ms=p95_latency,
                error_rate=error_rate,
                details={
                    "overloaded_region": overloaded_region,
                    "traffic_to_overloaded": overloaded_traffic,
                    "traffic_shifted": traffic_shifted
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed with exception: {str(e)}")
            return LoadBalancingTestResult(
                test_name=test_name,
                success=False,
                actual_distribution={},
                avg_latency_ms=0,
                p95_latency_ms=0,
                error_rate=100.0,
                error_message=str(e)
            )
        finally:
            # Remove simulated load
            await self._remove_simulated_load(overloaded_region)
    
    # Helper methods
    
    async def _make_request(self) -> Tuple[Optional[str], bool]:
        """Make a request to the global endpoint and return the region that handled it"""
        try:
            response = requests.get(
                f"{self.config.global_endpoint}/api/v1/health",
                timeout=5
            )
            if response.status_code == 200:
                # Extract region from response headers or body
                region = response.headers.get('X-Region', 'unknown')
                return region, True
            return None, False
        except Exception as e:
            logger.debug(f"Request failed: {str(e)}")
            return None, False
    
    async def _make_request_from_location(self, location: str) -> Tuple[Optional[str], bool]:
        """Simulate request from specific geographic location"""
        # In real implementation, this would use different source IPs or headers
        # to simulate requests from different locations
        return await self._make_request()
    
    async def _configure_weighted_routing(self, weights: Dict[str, float]):
        """Configure weighted routing in Route53"""
        logger.info(f"Configuring weighted routing: {weights}")
        # Implementation would update Route53 weighted routing policy
        pass
    
    async def _mark_region_unhealthy(self, region: str):
        """Mark a region as unhealthy in Route53 health checks"""
        logger.info(f"Marking region {region} as unhealthy")
        # Implementation would update Route53 health check status
        pass
    
    async def _mark_region_healthy(self, region: str):
        """Mark a region as healthy in Route53 health checks"""
        logger.info(f"Marking region {region} as healthy")
        # Implementation would restore Route53 health check status
        pass
    
    async def _simulate_high_load(self, region: str):
        """Simulate high load on a region"""
        logger.info(f"Simulating high load on region {region}")
        # Implementation would trigger high CPU/memory usage
        pass
    
    async def _remove_simulated_load(self, region: str):
        """Remove simulated load from a region"""
        logger.info(f"Removing simulated load from region {region}")
        # Implementation would stop load simulation
        pass
    
    def _calculate_distribution(self, routing_results: List[Tuple]) -> Dict[str, float]:
        """Calculate traffic distribution from routing results"""
        region_counts = defaultdict(int)
        total = len(routing_results)
        
        for _, _, actual_region, success in routing_results:
            if success and actual_region:
                region_counts[actual_region] += 1
        
        return {
            region: (count / total) * 100
            for region, count in region_counts.items()
        }
    
    async def run_all_tests(self) -> List[LoadBalancingTestResult]:
        """Run all load balancing tests"""
        logger.info("Starting load balancing test suite")
        
        tests = [
            self.test_geographic_routing(),
            self.test_weighted_routing(),
            self.test_health_based_routing(),
            self.test_capacity_based_routing()
        ]
        
        self.results = await asyncio.gather(*tests)
        
        # Print summary
        self._print_summary()
        
        return self.results
    
    def _print_summary(self):
        """Print test results summary"""
        logger.info("\n" + "="*80)
        logger.info("Load Balancing Test Results")
        logger.info("="*80)
        
        for result in self.results:
            status = "✓ PASS" if result.success else "✗ FAIL"
            logger.info(f"\n{status} - {result.test_name}")
            logger.info(f"  Avg Latency: {result.avg_latency_ms:.2f}ms")
            logger.info(f"  P95 Latency: {result.p95_latency_ms:.2f}ms")
            logger.info(f"  Error Rate: {result.error_rate:.2f}%")
            logger.info(f"  Traffic Distribution:")
            for region, percentage in result.actual_distribution.items():
                logger.info(f"    {region}: {percentage:.1f}%")
            
            if result.error_message:
                logger.info(f"  Error: {result.error_message}")
        
        passed = sum(1 for r in self.results if r.success)
        total = len(self.results)
        logger.info(f"\n{'='*80}")
        logger.info(f"Summary: {passed}/{total} tests passed")
        logger.info("="*80 + "\n")


async def main():
    """Main entry point for load balancing tests"""
    config = LoadBalancingConfig()
    test_suite = LoadBalancingTest(config)
    
    results = await test_suite.run_all_tests()
    
    # Exit with appropriate code
    all_passed = all(r.success for r in results)
    exit(0 if all_passed else 1)


if __name__ == "__main__":
    asyncio.run(main())
