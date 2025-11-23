#!/usr/bin/env python3
"""
Cross-Region Latency Test

This test measures network latency between different AWS regions and validates
that cross-region communication meets performance targets.

Test Scenarios:
1. Region-to-region latency measurement
2. Database replication latency
3. API gateway latency across regions
4. CDN edge location latency
"""

import asyncio
import time
import logging
from typing import Dict, List, Tuple
from dataclasses import dataclass, field
import statistics

import boto3
import requests
import aiohttp

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@dataclass
class LatencyTestConfig:
    """Configuration for cross-region latency tests"""
    regions: List[str] = field(default_factory=lambda: [
        "us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1"
    ])
    api_endpoints: Dict[str, str] = field(default_factory=lambda: {
        "us-east-1": "https://api-us-east-1.example.com",
        "us-west-2": "https://api-us-west-2.example.com",
        "eu-west-1": "https://api-eu-west-1.example.com",
        "ap-southeast-1": "https://api-ap-southeast-1.example.com"
    })
    max_latency_same_region_ms: int = 10
    max_latency_cross_region_ms: int = 200
    max_replication_latency_ms: int = 100
    samples_per_test: int = 100


@dataclass
class LatencyTestResult:
    """Result of a latency test"""
    test_name: str
    success: bool
    latencies: Dict[str, Dict[str, float]]  # source -> target -> latency
    avg_latency_ms: float
    p50_latency_ms: float
    p95_latency_ms: float
    p99_latency_ms: float
    max_latency_ms: float
    details: Dict = field(default_factory=dict)


class CrossRegionLatencyTest:
    """Test suite for cross-region latency"""
    
    def __init__(self, config: LatencyTestConfig):
        self.config = config
        self.results: List[LatencyTestResult] = []
        
    async def test_region_to_region_latency(self) -> LatencyTestResult:
        """
        Test: Measure latency between all region pairs
        
        Measures network latency between every pair of regions to create
        a latency matrix.
        """
        test_name = "region_to_region_latency"
        logger.info(f"Starting test: {test_name}")
        
        latency_matrix = {}
        all_latencies = []
        
        try:
            # Test latency from each region to every other region
            for source_region in self.config.regions:
                latency_matrix[source_region] = {}
                
                for target_region in self.config.regions:
                    if source_region == target_region:
                        # Same region - should be very low latency
                        latencies = await self._measure_latency(
                            source_region,
                            target_region,
                            self.config.samples_per_test
                        )
                    else:
                        # Cross-region - higher latency expected
                        latencies = await self._measure_latency(
                            source_region,
                            target_region,
                            self.config.samples_per_test
                        )
                    
                    avg_latency = statistics.mean(latencies)
                    latency_matrix[source_region][target_region] = avg_latency
                    all_latencies.extend(latencies)
                    
                    logger.info(f"{source_region} -> {target_region}: {avg_latency:.2f}ms")
            
            # Calculate statistics
            avg_latency = statistics.mean(all_latencies)
            p50_latency = statistics.median(all_latencies)
            sorted_latencies = sorted(all_latencies)
            p95_latency = sorted_latencies[int(len(sorted_latencies) * 0.95)]
            p99_latency = sorted_latencies[int(len(sorted_latencies) * 0.99)]
            max_latency = max(all_latencies)
            
            # Evaluate success
            success = p95_latency <= self.config.max_latency_cross_region_ms
            
            return LatencyTestResult(
                test_name=test_name,
                success=success,
                latencies=latency_matrix,
                avg_latency_ms=avg_latency,
                p50_latency_ms=p50_latency,
                p95_latency_ms=p95_latency,
                p99_latency_ms=p99_latency,
                max_latency_ms=max_latency,
                details={
                    "total_measurements": len(all_latencies),
                    "region_pairs": len(self.config.regions) * len(self.config.regions)
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed: {str(e)}")
            return LatencyTestResult(
                test_name=test_name,
                success=False,
                latencies={},
                avg_latency_ms=0,
                p50_latency_ms=0,
                p95_latency_ms=0,
                p99_latency_ms=0,
                max_latency_ms=0,
                details={"error": str(e)}
            )
    
    async def test_database_replication_latency(self) -> LatencyTestResult:
        """
        Test: Measure Aurora Global Database replication latency
        
        Measures the time it takes for data written in one region to be
        replicated to other regions.
        """
        test_name = "database_replication_latency"
        logger.info(f"Starting test: {test_name}")
        
        replication_latencies = {}
        all_latencies = []
        
        try:
            primary_region = self.config.regions[0]
            
            for target_region in self.config.regions[1:]:
                latencies = []
                
                for i in range(self.config.samples_per_test):
                    # Write data to primary region
                    test_id = f"repl-test-{int(time.time() * 1000)}-{i}"
                    write_time = time.time()
                    
                    await self._write_test_data(primary_region, test_id)
                    
                    # Poll target region until data appears
                    replication_time = await self._wait_for_replication(
                        target_region,
                        test_id,
                        max_wait_ms=5000
                    )
                    
                    if replication_time:
                        latencies.append(replication_time)
                        all_latencies.append(replication_time)
                    
                    # Small delay between tests
                    await asyncio.sleep(0.1)
                
                avg_latency = statistics.mean(latencies) if latencies else 0
                replication_latencies[f"{primary_region}->{target_region}"] = avg_latency
                
                logger.info(f"Replication {primary_region} -> {target_region}: {avg_latency:.2f}ms")
            
            # Calculate statistics
            if all_latencies:
                avg_latency = statistics.mean(all_latencies)
                p50_latency = statistics.median(all_latencies)
                sorted_latencies = sorted(all_latencies)
                p95_latency = sorted_latencies[int(len(sorted_latencies) * 0.95)]
                p99_latency = sorted_latencies[int(len(sorted_latencies) * 0.99)]
                max_latency = max(all_latencies)
                
                # Success if P99 < 100ms
                success = p99_latency <= self.config.max_replication_latency_ms
            else:
                avg_latency = p50_latency = p95_latency = p99_latency = max_latency = 0
                success = False
            
            return LatencyTestResult(
                test_name=test_name,
                success=success,
                latencies={"replication": replication_latencies},
                avg_latency_ms=avg_latency,
                p50_latency_ms=p50_latency,
                p95_latency_ms=p95_latency,
                p99_latency_ms=p99_latency,
                max_latency_ms=max_latency,
                details={
                    "primary_region": primary_region,
                    "target_regions": self.config.regions[1:],
                    "samples_per_region": self.config.samples_per_test
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed: {str(e)}")
            return LatencyTestResult(
                test_name=test_name,
                success=False,
                latencies={},
                avg_latency_ms=0,
                p50_latency_ms=0,
                p95_latency_ms=0,
                p99_latency_ms=0,
                max_latency_ms=0,
                details={"error": str(e)}
            )
    
    async def test_api_gateway_latency(self) -> LatencyTestResult:
        """
        Test: Measure API Gateway latency across regions
        
        Measures the latency of API requests through API Gateway
        in different regions.
        """
        test_name = "api_gateway_latency"
        logger.info(f"Starting test: {test_name}")
        
        api_latencies = {}
        all_latencies = []
        
        try:
            for region in self.config.regions:
                endpoint = self.config.api_endpoints.get(region)
                if not endpoint:
                    continue
                
                latencies = []
                
                for _ in range(self.config.samples_per_test):
                    start_time = time.time()
                    
                    try:
                        async with aiohttp.ClientSession() as session:
                            async with session.get(
                                f"{endpoint}/api/v1/health",
                                timeout=aiohttp.ClientTimeout(total=10)
                            ) as response:
                                if response.status == 200:
                                    latency = (time.time() - start_time) * 1000
                                    latencies.append(latency)
                                    all_latencies.append(latency)
                    except Exception as e:
                        logger.debug(f"Request to {region} failed: {str(e)}")
                    
                    await asyncio.sleep(0.05)  # Small delay between requests
                
                if latencies:
                    avg_latency = statistics.mean(latencies)
                    api_latencies[region] = avg_latency
                    logger.info(f"API Gateway {region}: {avg_latency:.2f}ms")
            
            # Calculate statistics
            if all_latencies:
                avg_latency = statistics.mean(all_latencies)
                p50_latency = statistics.median(all_latencies)
                sorted_latencies = sorted(all_latencies)
                p95_latency = sorted_latencies[int(len(sorted_latencies) * 0.95)]
                p99_latency = sorted_latencies[int(len(sorted_latencies) * 0.99)]
                max_latency = max(all_latencies)
                
                # Success if P95 < 200ms
                success = p95_latency <= self.config.max_latency_cross_region_ms
            else:
                avg_latency = p50_latency = p95_latency = p99_latency = max_latency = 0
                success = False
            
            return LatencyTestResult(
                test_name=test_name,
                success=success,
                latencies={"api_gateway": api_latencies},
                avg_latency_ms=avg_latency,
                p50_latency_ms=p50_latency,
                p95_latency_ms=p95_latency,
                p99_latency_ms=p99_latency,
                max_latency_ms=max_latency,
                details={
                    "regions_tested": len(api_latencies),
                    "samples_per_region": self.config.samples_per_test
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed: {str(e)}")
            return LatencyTestResult(
                test_name=test_name,
                success=False,
                latencies={},
                avg_latency_ms=0,
                p50_latency_ms=0,
                p95_latency_ms=0,
                p99_latency_ms=0,
                max_latency_ms=0,
                details={"error": str(e)}
            )
    
    async def test_cdn_edge_latency(self) -> LatencyTestResult:
        """
        Test: Measure CloudFront CDN edge location latency
        
        Measures latency to CloudFront edge locations from different regions.
        """
        test_name = "cdn_edge_latency"
        logger.info(f"Starting test: {test_name}")
        
        cdn_latencies = {}
        all_latencies = []
        
        try:
            cdn_endpoint = "https://cdn.example.com"
            
            for region in self.config.regions:
                latencies = []
                
                for _ in range(self.config.samples_per_test):
                    start_time = time.time()
                    
                    try:
                        async with aiohttp.ClientSession() as session:
                            async with session.get(
                                f"{cdn_endpoint}/health",
                                timeout=aiohttp.ClientTimeout(total=10)
                            ) as response:
                                if response.status == 200:
                                    latency = (time.time() - start_time) * 1000
                                    latencies.append(latency)
                                    all_latencies.append(latency)
                    except Exception as e:
                        logger.debug(f"CDN request from {region} failed: {str(e)}")
                    
                    await asyncio.sleep(0.05)
                
                if latencies:
                    avg_latency = statistics.mean(latencies)
                    cdn_latencies[region] = avg_latency
                    logger.info(f"CDN from {region}: {avg_latency:.2f}ms")
            
            # Calculate statistics
            if all_latencies:
                avg_latency = statistics.mean(all_latencies)
                p50_latency = statistics.median(all_latencies)
                sorted_latencies = sorted(all_latencies)
                p95_latency = sorted_latencies[int(len(sorted_latencies) * 0.95)]
                p99_latency = sorted_latencies[int(len(sorted_latencies) * 0.99)]
                max_latency = max(all_latencies)
                
                # CDN should have low latency from all regions
                success = p95_latency <= 100  # 100ms target for CDN
            else:
                avg_latency = p50_latency = p95_latency = p99_latency = max_latency = 0
                success = False
            
            return LatencyTestResult(
                test_name=test_name,
                success=success,
                latencies={"cdn": cdn_latencies},
                avg_latency_ms=avg_latency,
                p50_latency_ms=p50_latency,
                p95_latency_ms=p95_latency,
                p99_latency_ms=p99_latency,
                max_latency_ms=max_latency,
                details={
                    "cdn_endpoint": cdn_endpoint,
                    "regions_tested": len(cdn_latencies)
                }
            )
            
        except Exception as e:
            logger.error(f"Test {test_name} failed: {str(e)}")
            return LatencyTestResult(
                test_name=test_name,
                success=False,
                latencies={},
                avg_latency_ms=0,
                p50_latency_ms=0,
                p95_latency_ms=0,
                p99_latency_ms=0,
                max_latency_ms=0,
                details={"error": str(e)}
            )
    
    # Helper methods
    
    async def _measure_latency(
        self,
        source_region: str,
        target_region: str,
        samples: int
    ) -> List[float]:
        """Measure latency between two regions"""
        latencies = []
        endpoint = self.config.api_endpoints.get(target_region)
        
        if not endpoint:
            return latencies
        
        for _ in range(samples):
            start_time = time.time()
            
            try:
                async with aiohttp.ClientSession() as session:
                    async with session.get(
                        f"{endpoint}/api/v1/ping",
                        timeout=aiohttp.ClientTimeout(total=5)
                    ) as response:
                        if response.status == 200:
                            latency = (time.time() - start_time) * 1000
                            latencies.append(latency)
            except Exception:
                pass
            
            await asyncio.sleep(0.01)
        
        return latencies
    
    async def _write_test_data(self, region: str, test_id: str):
        """Write test data to specified region"""
        endpoint = self.config.api_endpoints.get(region)
        if not endpoint:
            return
        
        try:
            async with aiohttp.ClientSession() as session:
                async with session.post(
                    f"{endpoint}/api/v1/test-data",
                    json={"id": test_id, "timestamp": time.time()},
                    timeout=aiohttp.ClientTimeout(total=5)
                ) as response:
                    return response.status in [200, 201]
        except Exception:
            return False
    
    async def _wait_for_replication(
        self,
        region: str,
        test_id: str,
        max_wait_ms: int = 5000
    ) -> float:
        """Wait for data to replicate and return replication time"""
        endpoint = self.config.api_endpoints.get(region)
        if not endpoint:
            return None
        
        start_time = time.time()
        max_wait_seconds = max_wait_ms / 1000
        
        while (time.time() - start_time) < max_wait_seconds:
            try:
                async with aiohttp.ClientSession() as session:
                    async with session.get(
                        f"{endpoint}/api/v1/test-data/{test_id}",
                        timeout=aiohttp.ClientTimeout(total=1)
                    ) as response:
                        if response.status == 200:
                            return (time.time() - start_time) * 1000
            except Exception:
                pass
            
            await asyncio.sleep(0.01)
        
        return None
    
    async def run_all_tests(self) -> List[LatencyTestResult]:
        """Run all latency tests"""
        logger.info("Starting cross-region latency test suite")
        
        tests = [
            self.test_region_to_region_latency(),
            self.test_database_replication_latency(),
            self.test_api_gateway_latency(),
            self.test_cdn_edge_latency()
        ]
        
        self.results = await asyncio.gather(*tests)
        
        # Print summary
        self._print_summary()
        
        return self.results
    
    def _print_summary(self):
        """Print test results summary"""
        logger.info("\n" + "="*80)
        logger.info("Cross-Region Latency Test Results")
        logger.info("="*80)
        
        for result in self.results:
            status = "✓ PASS" if result.success else "✗ FAIL"
            logger.info(f"\n{status} - {result.test_name}")
            logger.info(f"  Avg Latency: {result.avg_latency_ms:.2f}ms")
            logger.info(f"  P50 Latency: {result.p50_latency_ms:.2f}ms")
            logger.info(f"  P95 Latency: {result.p95_latency_ms:.2f}ms")
            logger.info(f"  P99 Latency: {result.p99_latency_ms:.2f}ms")
            logger.info(f"  Max Latency: {result.max_latency_ms:.2f}ms")
        
        passed = sum(1 for r in self.results if r.success)
        total = len(self.results)
        logger.info(f"\n{'='*80}")
        logger.info(f"Summary: {passed}/{total} tests passed")
        logger.info("="*80 + "\n")


async def main():
    """Main entry point for cross-region latency tests"""
    config = LatencyTestConfig()
    test_suite = CrossRegionLatencyTest(config)
    
    results = await test_suite.run_all_tests()
    
    # Exit with appropriate code
    all_passed = all(r.success for r in results)
    exit(0 if all_passed else 1)


if __name__ == "__main__":
    asyncio.run(main())
